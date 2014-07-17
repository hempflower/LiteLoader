package com.mumfrey.liteloader.core;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.activity.InvalidActivityException;

import net.minecraft.client.resources.IResourcePack;

import com.mumfrey.liteloader.LiteMod;
import com.mumfrey.liteloader.api.ModLoadObserver;
import com.mumfrey.liteloader.common.LoadingProgress;
import com.mumfrey.liteloader.interfaces.LoaderEnumerator;
import com.mumfrey.liteloader.interfaces.Loadable;
import com.mumfrey.liteloader.interfaces.LoadableMod;
import com.mumfrey.liteloader.launch.LoaderEnvironment;
import com.mumfrey.liteloader.launch.LoaderProperties;
import com.mumfrey.liteloader.modconfig.ConfigManager;
import com.mumfrey.liteloader.modconfig.ConfigStrategy;
import com.mumfrey.liteloader.util.log.LiteLoaderLogger;

/**
 * Separated from the core loader class for encapsulation purposes
 *
 * @author Adam Mummery-Smith
 */
public class LiteLoaderMods
{
	public static final String MOD_SYSTEM = "liteloader";
	
	/**
	 * Reference to the loader 
	 */
	protected final LiteLoader loader;
	
	/**
	 * Loader environment instance 
	 */
	protected final LoaderEnvironment environment;
	
	/**
	 * Loader Properties adapter 
	 */
	private final LoaderProperties properties;

	/**
	 * Mod enumerator instance
	 */
	protected final LoaderEnumerator enumerator;

	/**
	 * Configuration manager 
	 */
	private final ConfigManager configManager;
	
	/**
	 * Mod load observers
	 */
	private List<ModLoadObserver> observers;

	/**
	 * List of loaded mods, for crash reporting
	 */
	private String loadedModsList = "none";
	
	/**
	 * Global list of mods which we can load
	 */
	protected final LinkedList<LiteMod> allMods = new LinkedList<LiteMod>();
	
	/**
	 * Global list of mods which are still waiting for initialisiation
	 */
	protected final LinkedList<LiteMod> initMods = new LinkedList<LiteMod>();
	
	/**
	 * Global list of mods which we have loaded
	 */
	protected final LinkedList<LiteMod> loadedMods = new LinkedList<LiteMod>();
	
	/**
	 * Mods which are loaded but disabled
	 */
	protected final LinkedList<Loadable<?>> disabledMods = new LinkedList<Loadable<?>>();

	LiteLoaderMods(LiteLoader loader, LoaderEnvironment environment, LoaderProperties properties, ConfigManager configManager)
	{
		this.loader           = loader;
		this.environment      = environment;
		this.enumerator       = environment.getEnumerator();
		this.properties       = properties;
		this.configManager    = configManager;
	}

	void init(List<ModLoadObserver> observers)
	{
		this.observers = observers;
		this.disabledMods.addAll(this.enumerator.getDisabledContainers());
	}

	void onPostInit()
	{
		this.updateSharedModList();
		
		this.environment.getEnabledModsList().save();
	}
	
	public EnabledModsList getEnabledModsList()
	{
		return this.environment.getEnabledModsList();
	}

	public LinkedList<LiteMod> getAllMods()
	{
		return this.allMods;
	}
	
	/**
	 * Used for crash reporting, returns a text list of all loaded mods
	 * 
	 * @return List of loaded mods as a string
	 */
	public String getLoadedModsList()
	{
		return this.loadedModsList;
	}
	
	/**
	 * Get a list containing all loaded mods
	 */
	public List<LiteMod> getLoadedMods()
	{
		return Collections.unmodifiableList(this.loadedMods);
	}
	
	/**
	 * Get a list containing all mod files which were NOT loaded
	 */
	public List<Loadable<?>> getDisabledMods()
	{
		return Collections.unmodifiableList(this.disabledMods);
	}
	
	/**
	 * Get the list of injected tweak containers
	 */
	public Collection<Loadable<File>> getInjectedTweaks()
	{
		return Collections.unmodifiableCollection(this.enumerator.getInjectedTweaks());
	}

	/**
	 * Get whether the specified mod is installed
	 *
	 * @param modName
	 * @return
	 */
	public boolean isModInstalled(String modName)
	{
		try
		{
			return this.getMod(modName) != null;
		}
		catch (IllegalArgumentException ex)
		{
			return false;
		}
	}

	/**
	 * Get a reference to a loaded mod, if the mod exists
	 * 
	 * @param modName Mod's name, identifier or class name
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T extends LiteMod> T getMod(String modName)
	{
		if (modName == null)
		{
			throw new IllegalArgumentException("Attempted to get a reference to a mod without specifying a mod name");
		}
		
		for (LiteMod mod : this.allMods)
		{
			Class<? extends LiteMod> modClass = mod.getClass();
			String modId = this.enumerator.getIdentifier(modClass);
			
			if (modName.equalsIgnoreCase(mod.getName()) || modName.equalsIgnoreCase(modId) || modName.equalsIgnoreCase(modClass.getSimpleName()))
				return (T)mod;
		}
		
		return null;
	}
	
	/**
	 * Get a reference to a loaded mod, if the mod exists
	 * 
	 * @param modName Mod's name or class name
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T extends LiteMod> T getMod(Class<T> modClass)
	{
		for (LiteMod mod : this.allMods)
		{
			if (mod.getClass().equals(modClass))
				return (T)mod;
		}
		
		return null;
	}

	/**
	 * Get the mod which matches the specified identifier
	 * 
	 * @param identifier
	 * @return
	 */
	public Class<? extends LiteMod> getModFromIdentifier(String identifier)
	{
		if (identifier == null) return null;
		
		for (LiteMod mod : this.allMods)
		{
			if (identifier.equalsIgnoreCase(this.enumerator.getIdentifier(mod.getClass())))
			{
				return mod.getClass();
			}
		}
		
		return null;
	}

	/**
	 * Get a metadata value for the specified mod
	 * 
	 * @param modNameOrId
	 * @param metaDataKey
	 * @param defaultValue
	 * @return
	 * @throws InvalidActivityException Thrown by getMod if init is not complete 
	 * @throws IllegalArgumentException Thrown by getMod if argument is null
	 */
	public String getModMetaData(String modNameOrId, String metaDataKey, String defaultValue) throws InvalidActivityException, IllegalArgumentException
	{
		return this.getModMetaData(this.getMod(modNameOrId), metaDataKey, defaultValue);
	}
	
	/**
	 * Get a metadata value for the specified mod
	 * 
	 * @param mod
	 * @param metaDataKey
	 * @param defaultValue
	 * @return
	 */
	public String getModMetaData(LiteMod mod, String metaDataKey, String defaultValue)
	{
		if (mod == null || metaDataKey == null) return defaultValue;
		return this.enumerator.getModMetaData(mod.getClass(), metaDataKey, defaultValue);
	}

	/**
	 * Get a metadata value for the specified mod
	 * 
	 * @param modClass
	 * @param metaDataKey
	 * @param defaultValue
	 * @return
	 */
	public String getModMetaData(Class<? extends LiteMod> modClass, String metaDataKey, String defaultValue)
	{
		if (modClass == null || metaDataKey == null) return defaultValue;
		return this.enumerator.getModMetaData(modClass, metaDataKey, defaultValue);
	}

	/**
	 * Get the mod identifier, this is used for versioning, exclusivity, and enablement checks
	 * 
	 * @param modClass
	 * @return
	 */
	public String getModIdentifier(Class<? extends LiteMod> modClass)
	{
		return this.enumerator.getIdentifier(modClass);
	}
	
	/**
	 * Get the mod identifier, this is used for versioning, exclusivity, and enablement checks
	 * 
	 * @param modClass
	 * @return
	 */
	public String getModIdentifier(LiteMod mod)
	{
		return mod == null ? null : this.enumerator.getIdentifier(mod.getClass());
	}
	
	/**
	 * Get the container (mod file, classpath jar or folder) for the specified mod
	 * 
	 * @param modClass
	 * @return
	 */
	public LoadableMod<?> getModContainer(Class<? extends LiteMod> modClass)
	{
		return this.enumerator.getContainer(modClass);
	}
	
	/**
	 * Get the container (mod file, classpath jar or folder) for the specified mod
	 * 
	 * @param modClass
	 * @return
	 */
	public LoadableMod<?> getModContainer(LiteMod mod)
	{
		return mod == null ? null : this.enumerator.getContainer(mod.getClass());
	}

	/**
	 * @param identifier Identifier of the mod to enable
	 */
	public void enableMod(String identifier)
	{
		this.setModEnabled(identifier, true);
	}

	/**
	 * @param identifier Identifier of the mod to disable
	 */
	public void disableMod(String identifier)
	{
		this.setModEnabled(identifier, false);
	}
	
	/**
	 * @param identifier Identifier of the mod to enable/disable
	 * @param enabled
	 */
	public void setModEnabled(String identifier, boolean enabled)
	{
		this.environment.getEnabledModsList().setEnabled(this.environment.getProfile(), identifier, enabled);
		this.environment.getEnabledModsList().save();
	}

	/**
	 * @param identifier
	 * @return
	 */
	public boolean isModEnabled(String identifier)
	{
		return this.environment.getEnabledModsList().isEnabled(LiteLoader.getProfile(), identifier);
	}

	public boolean isModEnabled(String profile, String identifier)
	{
		return this.environment.getEnabledModsList().isEnabled(profile, identifier);
	}

	/**
	 * @param modName
	 * @return
	 */
	public boolean isModActive(String modName)
	{
		if (modName == null) return false;
		
		for (LiteMod mod : this.loadedMods)
		{
			if (modName.equalsIgnoreCase(this.enumerator.getIdentifier(mod.getClass())))
			{
				return true;
			}
		}
		
		return false;
	}

	/**
	 * Create mod instances from the enumerated classes
	 * 
	 * @param modsToLoad List of mods to load
	 */
	void loadMods()
	{
		LoadingProgress.incTotalLiteLoaderProgress(this.enumerator.getModsToLoad().size());
		
		for (Class<? extends LiteMod> mod : this.enumerator.getModsToLoad())
		{
			LoadingProgress.incLiteLoaderProgress("Loading mod from %s...", mod.getName());
			LoadableMod<?> container = this.enumerator.getContainer(mod);

			try
			{
				String identifier = this.enumerator.getIdentifier(mod);
				if (identifier == null || this.environment.getEnabledModsList().isEnabled(this.environment.getProfile(), identifier))
				{
					if (!this.enumerator.checkDependencies(container))
					{
						this.onModLoadFailed(container, identifier, "the mod was missing a required dependency", null);
						continue;
					}
					
					this.loadMod(identifier, mod, container);
				}
				else
				{
					this.onModLoadFailed(container, identifier, "excluded by filter", null);
				}
			}
			catch (Throwable th)
			{
				this.onModLoadFailed(container, mod.getName(), "an error occurred", th);
			}
		}
	}

	/**
	 * @param identifier
	 * @param mod
	 * @param container 
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	void loadMod(String identifier, Class<? extends LiteMod> mod, LoadableMod<?> container) throws InstantiationException, IllegalAccessException
	{
		LiteLoaderLogger.info("Loading mod from %s", mod.getName());
		
		LiteMod newMod = mod.newInstance();
		
		this.onModLoaded(newMod);
		
		String modName = newMod.getName();
		if (modName == null && identifier != null) modName = identifier;
		LiteLoaderLogger.info("Successfully added mod %s version %s", modName, newMod.getVersion());
		
		// Register the mod as a resource pack if the container exists
		if (container != null)
		{
			LiteLoaderLogger.info("Adding \"%s\" to active resource pack set", container.getLocation());
			if (modName != null)
			{
				container.initResourcePack(modName);
				
				if (container.hasResourcePack() && LiteLoader.getGameEngine().registerResourcePack((IResourcePack)container.getResourcePack()))
				{
					LiteLoaderLogger.info("Successfully added \"%s\" to active resource pack set", container.getLocation());
				}
			}
		}
	}

	/**
	 * @param mod
	 */
	void onModLoaded(LiteMod mod)
	{
		for (ModLoadObserver observer : this.observers)
		{
			observer.onModLoaded(mod);
		}

		this.allMods.add(mod);
		this.initMods.add(mod);
		
		LoadingProgress.incTotalLiteLoaderProgress(1);
	}

	/**
	 * @param container
	 * @param identifier
	 * @param reason
	 * @param th
	 */
	void onModLoadFailed(LoadableMod<?> container, String identifier, String reason, Throwable th)
	{
		LiteLoaderLogger.warning("Not loading mod %s, %s", identifier, reason);
		
		if (container != LoadableMod.NONE && !this.disabledMods.contains(container))
		{
			this.disabledMods.add(container);
		}
		
		for (ModLoadObserver observer : this.observers)
		{
			observer.onModLoadFailed(container, identifier, reason, th);
		}
	}

	/**
	 * Initialise the mods which were loaded
	 */
	void initMods()
	{
		this.loadedModsList = "";
		int loadedModsCount = 0;
		
		while (this.initMods.size() > 0)
		{
			LiteMod mod = this.initMods.removeFirst();
			
			try
			{
				this.initMod(mod);
				loadedModsCount++;
			}
			catch (Throwable th)
			{
				LiteLoaderLogger.warning(th, "Error initialising mod '%s'", mod.getName());
				this.allMods.remove(mod);
			}
		}
		
		this.loadedModsList = String.format("%s loaded mod(s)%s", loadedModsCount, this.loadedModsList);
	}

	/**
	 * @param mod
	 */
	private void initMod(LiteMod mod)
	{
		LiteLoaderLogger.info("Initialising mod %s version %s", mod.getName(), mod.getVersion());
		LoadingProgress.incLiteLoaderProgress("Initialising mod %s version %s...", mod.getName(), mod.getVersion());
		
		this.onPreInitMod(mod);
		
		// initialise the mod
		mod.init(LiteLoader.getCommonConfigFolder());
		
		this.onPostInitMod(mod);
	}
	
	/**
	 * @param mod
	 */
	private void onPreInitMod(LiteMod mod)
	{
		for (ModLoadObserver observer : this.observers)
		{
			observer.onPreInitMod(mod);
		}
		
		// register mod config panel if configurable
		this.configManager.registerMod(mod);
		
		try
		{
			this.handleModVersionUpgrade(mod);
		}
		catch (Throwable th)
		{
			LiteLoaderLogger.warning("Error performing settings upgrade for %s. Settings may not be properly migrated", mod.getName());
		}
		
		// Init mod config if there is any
		this.configManager.initConfig(mod);
	}

	/**
	 * @param mod
	 */
	private void onPostInitMod(LiteMod mod)
	{
		for (ModLoadObserver observer : this.observers)
		{
			observer.onPostInitMod(mod);
		}

		// add the mod to all relevant listener queues
		LiteLoader.getInterfaceManager().offer(mod);

		this.loader.onPostInitMod(mod);
		
		this.loadedMods.add(mod);
		this.loadedModsList += String.format("\n          - %s version %s", mod.getName(), mod.getVersion());
	}
	
	/**
	 * @param mod
	 */
	private void handleModVersionUpgrade(LiteMod mod)
	{
		String modKey = this.getModNameForConfig(mod.getClass(), mod.getName());
		
		int currentRevision = LiteLoaderVersion.CURRENT.getLoaderRevision();
		int lastKnownRevision = this.properties.getLastKnownModRevision(modKey);
		
		LiteLoaderVersion lastModVersion = LiteLoaderVersion.getVersionFromRevision(lastKnownRevision);
		if (currentRevision > lastModVersion.getLoaderRevision())
		{
			File newConfigPath = LiteLoader.getConfigFolder();
			File oldConfigPath = this.environment.inflectVersionedConfigPath(lastModVersion);

			LiteLoaderLogger.info("Performing config upgrade for mod %s. Upgrading %s to %s...", mod.getName(), lastModVersion, LiteLoaderVersion.CURRENT);
			
			for (ModLoadObserver observer : this.observers)
			{
				observer.onMigrateModConfig(mod, newConfigPath, oldConfigPath);
			}

			// Migrate versioned config if any is present
			this.configManager.migrateModConfig(mod, newConfigPath, oldConfigPath);
			
			// Let the mod upgrade
			mod.upgradeSettings(LiteLoaderVersion.CURRENT.getMinecraftVersion(), newConfigPath, oldConfigPath);
			
			this.properties.storeLastKnownModRevision(modKey);
			LiteLoaderLogger.info("Config upgrade succeeded for mod %s", mod.getName());
		}
		else if (currentRevision < lastKnownRevision && ConfigManager.getConfigStrategy(mod) == ConfigStrategy.Unversioned)
		{
			LiteLoaderLogger.warning("Mod %s has config from unknown loader revision %d. This may cause unexpected behaviour.", mod.getName(), lastKnownRevision);
		}
	}
	
	/**
	 * Used by the version upgrade code, gets a version of the mod name suitable
	 * for inclusion in the properties file
	 * 
	 * @param modName
	 * @return
	 */
	String getModNameForConfig(Class<? extends LiteMod> modClass, String modName)
	{
		if (modName == null || modName.isEmpty())
		{
			modName = modClass.getSimpleName().toLowerCase();
		}
		
		return String.format("version.%s", modName.toLowerCase().replaceAll("[^a-z0-9_\\-\\.]", ""));
	}

	void updateSharedModList()
	{
		Map<String, Map<String, String>> modList = this.enumerator.getSharedModList();
		if (modList == null) return;
		
		for (LiteMod mod : this.allMods)
		{
			String modKey = String.format("%s:%s", LiteLoaderMods.MOD_SYSTEM, this.loader.getModIdentifier(mod));
			modList.put(modKey, this.packModInfoToMap(mod));
		}
	}

	private Map<String, String> packModInfoToMap(LiteMod mod)
	{
		Map<String, String> modInfo = new HashMap<String, String>();
		LoadableMod<?> container = this.loader.getModContainer(mod);
		
		modInfo.put("modsystem",   LiteLoaderMods.MOD_SYSTEM);
		modInfo.put("id",          this.loader.getModIdentifier(mod));
		modInfo.put("version",     mod.getVersion());
		modInfo.put("name",        mod.getName());
		modInfo.put("url",         container.getMetaValue("url", ""));
		modInfo.put("authors",     container.getAuthor());
		modInfo.put("description", container.getDescription(LiteLoaderEnumerator.getModClassName(mod)));
		
		return modInfo;
	}
}
