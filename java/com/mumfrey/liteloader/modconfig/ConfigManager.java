package com.mumfrey.liteloader.modconfig;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.common.base.Strings;
import com.google.common.io.Files;
import com.mumfrey.liteloader.Configurable;
import com.mumfrey.liteloader.LiteMod;
import com.mumfrey.liteloader.ExposeConfig;

/**
 * Registry where we keep the mod config panel classes
 *
 * @author Adam Mummery-Smith
 */
public class ConfigManager
{
	/**
	 * Mod config panel classes
	 */
	private Map<Class<? extends LiteMod>, Class<? extends ConfigPanel>> configPanels = new HashMap<Class<? extends LiteMod>, Class<? extends ConfigPanel>>();
	
	/**
	 * Mod config writers
	 */
	private Map<Class<? extends Exposable>, ExposableConfigWriter> configWriters = new HashMap<Class<? extends Exposable>, ExposableConfigWriter>();
	
	/**
	 * List of config writers, for faster iteration in onTick
	 */
	private List<ExposableConfigWriter> configWriterList = new LinkedList<ExposableConfigWriter>();
	
	/**
	 * Register a mod, adds the config panel class to the map if the mod implements Configurable
	 */
	public void registerMod(LiteMod mod)
	{
		if (mod instanceof Configurable)
		{
			Class<? extends ConfigPanel> panelClass = ((Configurable)mod).getConfigPanelClass();
			if (panelClass != null) this.configPanels.put(mod.getClass(), panelClass);
		}
		
		this.registerExposable(mod, null, false);
	}

	/**
	 * @param exposable
	 */
	public void registerExposable(Exposable exposable, String fileName, boolean force)
	{
		ExposeConfig exposeConfig = exposable.getClass().<ExposeConfig>getAnnotation(ExposeConfig.class);
		if (exposeConfig != null)
		{
			if (fileName == null) exposeConfig.filename();
			this.initConfigWriter(exposable, fileName, exposeConfig.strategy());
		}
		else if (force)
		{
			this.initConfigWriter(exposable, fileName, ConfigStrategy.Versioned);
		}
	}

	/**
	 * Create a config writer instance for the specified mod
	 * 
	 * @param exposable
	 * @param fileName
	 * @param strategy
	 */
	private void initConfigWriter(Exposable exposable, String fileName, ConfigStrategy strategy)
	{
		if (this.configWriters.containsKey(exposable.getClass()))
		{
			throw new IllegalArgumentException("Cannot register multiple Exposable instances of the same class or the Exposable already registered");
		}
		
		if (Strings.isNullOrEmpty(fileName))
		{
			fileName = exposable.getClass().getSimpleName().toLowerCase();
			
			if (fileName.startsWith("litemod"))
				fileName = fileName.substring(7);
		}
		
		ExposableConfigWriter configWriter = ExposableConfigWriter.create(exposable, strategy, fileName);
		if (configWriter != null)
		{
			this.configWriters.put(exposable.getClass(), configWriter);
			this.configWriterList.add(configWriter);
		}
	}

	/**
	 * Initialise the config writer for the specified mod
	 * 
	 * @param exposable
	 */
	public void initConfig(Exposable exposable)
	{
		Class<? extends Exposable> exposableClass = exposable.getClass();
		if (exposableClass != null && this.configWriters.containsKey(exposableClass))
		{
			this.configWriters.get(exposableClass).init();
		}
	}
	
	/**
	 * If the specified mod has a versioned config strategy, attempt to copy the config
	 * 
	 * @param mod
	 * @param newConfigPath
	 * @param oldConfigPath
	 */
	public void migrateModConfig(LiteMod mod, File newConfigPath, File oldConfigPath)
	{
		Class<? extends Exposable> exposableClass = mod.getClass();
		if (exposableClass != null && this.configWriters.containsKey(exposableClass))
		{
			ExposableConfigWriter writer = this.configWriters.get(exposableClass);
			if (writer.isVersioned())
			{
				File newConfigFile = writer.getConfigFile();
				File legacyConfigFile = new File(oldConfigPath, newConfigFile.getName());
				
				if (legacyConfigFile.exists() && !newConfigFile.exists())
				{
					try
					{
						Files.copy(legacyConfigFile, newConfigFile);
					}
					catch (IOException ex)
					{
						ex.printStackTrace();
					}
				}
			}
		}
	}
	
	/**
	 * Check whether a config panel is available for the specified class
	 * 
	 * @param modClass
	 * @return
	 */
	public boolean hasPanel(Class<? extends LiteMod> modClass)
	{
		return modClass != null && this.configPanels.containsKey(modClass);
	}
	
	/**
	 * Instance a new config panel for the specified mod class if one is available
	 * 
	 * @param modClass
	 * @return
	 */
	public ConfigPanel getPanel(Class<? extends LiteMod> modClass)
	{
		if (modClass != null && this.configPanels.containsKey(modClass))
		{
			try
			{
				return this.configPanels.get(modClass).newInstance();
			}
			catch (InstantiationException ex) {}
			catch (IllegalAccessException ex) {}
			
			// If instantiation fails, remove the panel
			this.configPanels.remove(modClass);
		}
		
		return null;
	}

	/**
	 * Invalidate the specified mod config, cause it to be written to disk or scheduled for writing
	 * if it has been written recent
	 * 
	 * @param exposableClass
	 */
	public void invalidateConfig(Class<? extends Exposable> exposableClass)
	{
		if (exposableClass != null && this.configWriters.containsKey(exposableClass))
		{
			this.configWriters.get(exposableClass).invalidate();
		}
	}
	
	/**
	 * Tick all of the configuration writers, handles latent writes for anti-hammer strategy
	 */
	public void onTick()
	{
		for (ExposableConfigWriter writer : this.configWriterList)
		{
			writer.onTick();
		}
	}
	
	/**
	 * Force all mod configs to be flushed to disk
	 */
	public void syncConfig()
	{
		for (ExposableConfigWriter writer : this.configWriterList)
		{
			writer.sync();
		}
	}
}
