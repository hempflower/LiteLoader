package com.mumfrey.liteloader.modconfig;

import java.util.HashMap;
import java.util.Map;

import com.mumfrey.liteloader.Configurable;
import com.mumfrey.liteloader.LiteMod;

/**
 * Registry where we keep the mod config panel classes
 *
 * @author Adam Mummery-Smith
 */
public class ConfigPanelManager
{
	/**
	 * Mod config panel classes
	 */
	private Map<Class<? extends LiteMod>, Class<? extends ConfigPanel>> configPanels = new HashMap<Class<? extends LiteMod>, Class<? extends ConfigPanel>>();
	
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
}
