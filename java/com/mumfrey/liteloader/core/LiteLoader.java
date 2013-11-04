package com.mumfrey.liteloader.core;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activity.InvalidActivityException;

import org.lwjgl.input.Keyboard;

import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraft.src.CrashReport;
import net.minecraft.src.GuiControls;
import net.minecraft.src.GuiMainMenu;
import net.minecraft.src.GuiScreen;
import net.minecraft.src.KeyBinding;
import net.minecraft.src.Minecraft;
import net.minecraft.src.NetHandler;
import net.minecraft.src.Packet1Login;
import net.minecraft.src.ResourcePack;
import net.minecraft.src.SimpleReloadableResourceManager;
import net.minecraft.src.World;

import com.mumfrey.liteloader.*;
import com.mumfrey.liteloader.crashreport.CallableLiteLoaderBrand;
import com.mumfrey.liteloader.crashreport.CallableLiteLoaderMods;
import com.mumfrey.liteloader.gui.GuiControlsPaginated;
import com.mumfrey.liteloader.gui.GuiScreenModInfo;
import com.mumfrey.liteloader.modconfig.ConfigPanelManager;
import com.mumfrey.liteloader.permissions.PermissionsManagerClient;
import com.mumfrey.liteloader.util.PrivateFields;

/**
 * LiteLoader is a simple loader which loads and provides useful callbacks to
 * lightweight mods
 * 
 * @author Adam Mummery-Smith
 * @version 1.6.4_02
 */
public final class LiteLoader
{
	private static final String OPTION_MOD_INFO_SCREEN   = "modInfoScreen";
	private static final String OPTION_SOUND_MANAGER_FIX = "soundManagerFix";
	private static final String OPTION_CONTROLS_PAGES    = "controls.pages";

	/**
	 * LiteLoader is a singleton, this is the singleton instance
	 */
	private static LiteLoader instance;
	
	/**
	 * Logger for LiteLoader events
	 */
	private static final Logger logger = Logger.getLogger("liteloader");
	
	/**
	 * Tweak system class loader 
	 */
	private static LaunchClassLoader classLoader;
	
	/**
	 * List of mods passed into the command line
	 */
	private EnabledModsList enabledModsList = null;
	
	/**
	 * Mods folder which contains mods and legacy config files
	 */
	private File modsFolder;
	
	/**
	 * Base config folder which contains LiteLoader config files and versioned
	 * subfolders
	 */
	private File configBaseFolder;
	
	/**
	 * Folder containing version-independent configuration
	 */
	private File commonConfigFolder;
	
	/**
	 * Folder containing version-specific configuration
	 */
	private File versionConfigFolder;
	
	/**
	 * JSON file containing the list of enabled/disabled mods by profile
	 */
	private File enabledModsFile;
	
	/**
	 * Reference to the Minecraft game instance
	 */
	private Minecraft minecraft;
	
	/**
	 * Setting value, if true we will swap out the MC "Controls" GUI for our
	 * custom, paginated one
	 */
	private boolean paginateControls = true;
	
	/**
	 * Loader Bootstrap instance 
	 */
	private final LiteLoaderBootstrap bootstrap;
	
	/**
	 * Mod enumerator instance
	 */
	private final LiteLoaderEnumerator enumerator;
	
	/**
	 * Registered resource packs 
	 */
	private final Map<String, ResourcePack> registeredResourcePacks = new HashMap<String, ResourcePack>();
	
	/**
	 * List of loaded mods, for crash reporting
	 */
	private String loadedModsList = "none";
	
	/**
	 * Global list of mods which we can load
	 */
	private final LinkedList<LiteMod> mods = new LinkedList<LiteMod>();
	
	/**
	 * Global list of mods which we have loaded
	 */
	private final LinkedList<LiteMod> loadedMods = new LinkedList<LiteMod>();
	
	/**
	 * Mods which are loaded but disabled
	 */
	private final LinkedList<ModFile> disabledMods = new LinkedList<ModFile>();
	
	/**
	 * Event manager
	 */
	private Events events;
	
	/**
	 * Plugin channel manager 
	 */
	private final PluginChannels pluginChannels = new PluginChannels();
	
	/**
	 * Permission Manager
	 */
	private final PermissionsManagerClient permissionsManager = PermissionsManagerClient.getInstance();
	
	/**
	 * Configuration panel manager/registry
	 */
	private final ConfigPanelManager configPanelManager;
	
	/**
	 * Flag which keeps track of whether late initialisation has been done
	 */
	private boolean postInitStarted, startupComplete;

	/**
	 * True while initialising mods if we need to do a resource manager reload once the process is completed
	 */
	private boolean pendingResourceReload;
	
	/**
	 * Read from the properties file, if true we will inhibit the sound manager reload during startup to avoid getting in trouble with OpenAL
	 */
	private boolean inhibitSoundManagerReload = true;
	
	/**
	 * If inhibit is enabled, this object is used to reflectively inhibit the sound manager's reload process during startup by removing it from the reloadables list
	 */
	private SoundManagerReloadInhibitor soundManagerReloadInhibitor;

	/**
	 * File in which we will store mod key mappings
	 */
	private File keyMapSettingsFile = null;
	
	/**
	 * Properties object which stores mod key mappings
	 */
	private Properties keyMapSettings = new Properties();
	
	/**
	 * List of all registered mod keys
	 */
	private List<KeyBinding> modKeyBindings = new ArrayList<KeyBinding>();
	
	/**
	 * Map of mod key bindings to their key codes, stored so that we don't need to cast from
	 * string in the properties file every tick
	 */
	private Map<KeyBinding, Integer> storedModKeyBindings = new HashMap<KeyBinding, Integer>();
	
	/**
	 * Setting which determines whether we show the "mod info" screen tab in the main menu
	 */
	private boolean displayModInfoScreenTab = true;
	
	/**
	 * Override for the "mod info" tab setting, so that mods which want to handle the mod info themselves
	 * can temporarily disable the function without having to change the underlying property
	 */
	private boolean hideModInfoScreenTab = false;
	
	/**
	 * Active "mod info" screen, drawn as an overlay when in the main menu and made the active screen if
	 * the user clicks the tab
	 */
	private GuiScreenModInfo modInfoScreen;
	
	/**
	 * Pre-init routine, called using reflection by the tweaker
	 * 
	 * @param gameDirectory Game directory passed to the tweaker
	 * @param assetsDirectory Assets directory passed to the tweaker
	 * @param profile Launch profile name supplied with --version parameter
	 * @param modNameFilter List of mod names parsed from the command line
	 * @param classLoader LaunchClassLoader
	 */
	static final void init(LiteLoaderBootstrap bootstrap, LiteLoaderEnumerator enumerator, List<String> modNameFilter, LaunchClassLoader classLoader)
	{
		if (LiteLoader.instance == null)
		{
			LiteLoader.classLoader = classLoader;
			
			LiteLoader.instance = new LiteLoader(bootstrap, enumerator, modNameFilter);
			LiteLoader.instance.onInit();
		}
	}
	
	/**
	 * Post-init routine, initialises and loads mods enumerated in preInit
	 */
	static final void postInit()
	{
		if (LiteLoader.instance != null)
		{
			final Minecraft minecraft = Minecraft.getMinecraft();
			LiteLoader.instance.onPostInit(minecraft);
		}
	}

	/**
	 * LiteLoader constructor
	 * @param profile 
	 * @param modNameFilter 
	 */
	private LiteLoader(LiteLoaderBootstrap bootstrap, LiteLoaderEnumerator enumerator, List<String> modNameFilter)
	{
		this.bootstrap = bootstrap;
		this.enumerator = enumerator;
		
		this.setupPaths(bootstrap);
		
		this.enabledModsList = EnabledModsList.createFrom(this.enabledModsFile);
		this.enabledModsList.processModsList(bootstrap.getProfile(), modNameFilter);
		
		this.configPanelManager = new ConfigPanelManager();
	}
	
	/**
	 * Set up paths used by the loader
	 */
	private void setupPaths(LiteLoaderBootstrap bootstrap)
	{
		this.modsFolder = bootstrap.getModsFolder();
		this.configBaseFolder = bootstrap.getConfigBaseFolder();
		
		this.commonConfigFolder = new File(this.configBaseFolder, "common");
		this.versionConfigFolder = this.inflectVersionedConfigPath(LiteLoaderBootstrap.VERSION);
		
		if (!this.modsFolder.exists()) this.modsFolder.mkdirs();
		if (!this.configBaseFolder.exists()) this.configBaseFolder.mkdirs();
		if (!this.commonConfigFolder.exists()) this.commonConfigFolder.mkdirs();
		if (!this.versionConfigFolder.exists()) this.versionConfigFolder.mkdirs();
		
		this.enabledModsFile = new File(this.configBaseFolder, "liteloader.profiles.json");
		this.keyMapSettingsFile = new File(this.configBaseFolder, "liteloader.keys.properties");
	}
	
	/**
	 * @param version
	 * @return
	 */
	private File inflectVersionedConfigPath(LiteLoaderVersion version)
	{
		if (version.equals(LiteLoaderVersion.LEGACY))
		{
			return this.modsFolder;
		}
		
		return new File(this.configBaseFolder, String.format("config.%s", version.getMinecraftVersion()));
	}

	/**
	 * Set up reflection methods required by the loader
	 */
	private boolean onInit()
	{
		try
		{
			if (this.keyMapSettingsFile.exists())
			{
				try
				{
					this.keyMapSettings.load(new FileReader(this.keyMapSettingsFile));
				}
				catch (Exception ex) {}
			}
			
			this.paginateControls = this.bootstrap.getAndStoreBooleanProperty(OPTION_CONTROLS_PAGES, true);
			this.inhibitSoundManagerReload = this.bootstrap.getAndStoreBooleanProperty(OPTION_SOUND_MANAGER_FIX, true);
			this.displayModInfoScreenTab = this.bootstrap.getAndStoreBooleanProperty(OPTION_MOD_INFO_SCREEN, true);
			
			this.enumerator.discoverModClasses();
		}
		catch (Throwable th)
		{
			LiteLoader.getLogger().log(Level.SEVERE, "Error initialising LiteLoader", th);
			return false;
		}
		
		return true;
	}
	
	private void onPostInit(Minecraft minecraft)
	{
		if (this.postInitStarted) return;
		this.postInitStarted = true;

		// Cache local minecraft reference
		this.minecraft = minecraft;

		// Create the event broker
		this.events = new Events(this, this.minecraft, this.pluginChannels);
		
		// Spawn mod instances
		this.loadMods();
		
		// Initialises enumerated mods
		this.initMods();
		
		// Initialises the required hooks for loaded mods
		this.events.initHooks();
		this.startupComplete = true;
		
		this.enabledModsList.saveTo(this.enabledModsFile);
		this.bootstrap.writeProperties();
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.core.ICustomResourcePackManager#registerModResourcePack(net.minecraft.src.ResourcePack)
	 */
	public boolean registerModResourcePack(ResourcePack resourcePack)
	{
		if (!this.registeredResourcePacks.containsKey(resourcePack.getPackName()))
		{
			this.pendingResourceReload = true;

			List<ResourcePack> defaultResourcePacks = PrivateFields.defaultResourcePacks.get(this.minecraft);
			if (!defaultResourcePacks.contains(resourcePack))
			{
				defaultResourcePacks.add(resourcePack);
				this.registeredResourcePacks.put(resourcePack.getPackName(), resourcePack);
				return true;
			}
		}
		
		return false;
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.core.ICustomResourcePackManager#unRegisterModResourcePack(net.minecraft.src.ResourcePack)
	 */
	public boolean unRegisterModResourcePack(ResourcePack resourcePack)
	{
		if (this.registeredResourcePacks.containsValue(resourcePack))
		{
			this.pendingResourceReload = true;

			List<ResourcePack> defaultResourcePacks = PrivateFields.defaultResourcePacks.get(this.minecraft);
			this.registeredResourcePacks.remove(resourcePack.getPackName());
			defaultResourcePacks.remove(resourcePack);
			return true;
		}
		
		return false;
	}
	
	/**
	 * Get the singleton instance of LiteLoader, initialises the loader if
	 * necessary
	 * 
	 * @param locationProvider
	 * @return LiteLoader instance
	 */
	public static final LiteLoader getInstance()
	{
		return LiteLoader.instance;
	}
	
	/**
	 * Get the LiteLoader logger object
	 * 
	 * @return
	 */
	public static final Logger getLogger()
	{
		return LiteLoader.logger;
	}
	
	/**
	 * Get the tweak system classloader
	 * 
	 * @return
	 */
	public static LaunchClassLoader getClassLoader()
	{
		return LiteLoader.classLoader;
	}
	
	/**
	 * Get the output stream which we are using for console output
	 * 
	 * @return
	 */
	public static final PrintStream getConsoleStream()
	{
		return LiteLoaderBootstrap.getConsoleStream();
	}
	
	/**
	 * Get LiteLoader version
	 * 
	 * @return
	 */
	public static final String getVersion()
	{
		return LiteLoaderBootstrap.VERSION.getLoaderVersion();
	}
	
	/**
	 * Get the loader revision
	 * 
	 * @return
	 */
	public static final int getRevision()
	{
		return LiteLoaderBootstrap.VERSION.getLoaderRevision();
	}
	
	/**
	 * @return
	 */
	public static PermissionsManagerClient getPermissionsManager()
	{
		return LiteLoader.getInstance().permissionsManager;
	}
	
	/**
	 * Get the event manager
	 * 
	 * @return
	 */
	public static Events getEvents()
	{
		return LiteLoader.getInstance().events;
	}
	
	/**
	 * Get the plugin channel manager
	 * 
	 * @return
	 */
	public static PluginChannels getPluginChannels()
	{
		return LiteLoader.getInstance().pluginChannels;
	}

	/**
	 * Get the "mods" folder
	 */
	public static File getModsFolder()
	{
		return LiteLoader.getInstance().modsFolder;
	}
	
	/**
	 * Get the common (version-independent) config folder
	 */
	public static File getCommonConfigFolder()
	{
		return LiteLoader.getInstance().commonConfigFolder;
	}
	
	/**
	 * Get the config folder for this version
	 */
	public static File getConfigFolder()
	{
		return LiteLoader.getInstance().versionConfigFolder;
	}
	
	/**
	 * @return
	 */
	public static File getGameDirectory()
	{
		return LiteLoader.getInstance().bootstrap.getGameDirectory();
	}
	
	/**
	 * @return
	 */
	public static File getAssetsDirectory()
	{
		return LiteLoader.getInstance().bootstrap.getAssetsDirectory();
	}
	
	/**
	 * @return
	 */
	public static String getProfile()
	{
		return LiteLoader.getInstance().bootstrap.getProfile();
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
	public List<ModFile> getDisabledMods()
	{
		return Collections.unmodifiableList(this.disabledMods);
	}
	
	/**
	 * Used to get the name of the modpack being used
	 * 
	 * @return name of the modpack in use or null if no pack
	 */
	public String getBranding()
	{
		return this.bootstrap.getBranding();
	}
	
	/**
	 * Get a reference to a loaded mod, if the mod exists
	 * 
	 * @param modName Mod's name, meta name or class name
	 * @return
	 * @throws InvalidActivityException
	 */
	@SuppressWarnings("unchecked")
	public <T extends LiteMod> T getMod(String modName) throws InvalidActivityException, IllegalArgumentException
	{
		if (!this.startupComplete)
		{
			throw new InvalidActivityException("Attempted to get a reference to a mod before loader startup is complete");
		}
		
		if (modName == null)
		{
			throw new IllegalArgumentException("Attempted to get a reference to a mod without specifying a mod name");
		}
		
		for (LiteMod mod : this.mods)
		{
			String metaName = this.getModMetaName(mod.getClass());
			
			if (modName.equalsIgnoreCase(mod.getName()) || modName.equalsIgnoreCase(metaName) || modName.equalsIgnoreCase(mod.getClass().getSimpleName()))
				return (T)mod;
		}
		
		return null;
	}
	
	/**
	 * Get a reference to a loaded mod, if the mod exists
	 * 
	 * @param modName Mod's name or class name
	 * @return
	 * @throws InvalidActivityException
	 */
	@SuppressWarnings("unchecked")
	public <T extends LiteMod> T getMod(Class<T> modClass)
	{
		if (!this.startupComplete)
		{
			throw new RuntimeException("Attempted to get a reference to a mod before loader startup is complete");
		}
		
		for (LiteMod mod : this.mods)
		{
			if (mod.getClass().equals(modClass))
				return (T)mod;
		}
		
		return null;
	}
	
	/**
	 * Get whether the specified mod is installed
	 *
	 * @param modName
	 * @return
	 */
	public boolean isModInstalled(String modName)
	{
		if (!this.startupComplete || modName == null) return false;
		
		for (LiteMod mod : this.mods)
		{
			if (modName.equalsIgnoreCase(mod.getName()) || modName.equalsIgnoreCase(mod.getClass().getSimpleName())) return true;
		}
		
		return true;
	}

	/**
	 * Get a metadata value for the specified mod
	 * 
	 * @param mod
	 * @param metaDataKey
	 * @param defaultValue
	 * @return
	 * @throws InvalidActivityException Thrown by getMod if init is not complete 
	 * @throws IllegalArgumentException Thrown by getMod if argument is null
	 */
	public String getModMetaData(String mod, String metaDataKey, String defaultValue) throws InvalidActivityException, IllegalArgumentException
	{
		return this.getModMetaData(this.getMod(mod), metaDataKey, defaultValue);
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
	 * @param modClassName
	 * @param metaDataKey
	 * @param defaultValue
	 * @return
	 */
	public String getModMetaData(Class<? extends LiteMod> modClass, String metaDataKey, String defaultValue)
	{
		return this.enumerator.getModMetaData(modClass, metaDataKey, defaultValue);
	}

	/**
	 * Get the mod "name" metadata key, this is used for versioning, exclusivity, and enablement checks
	 * 
	 * @param modClass
	 * @return
	 */
	public String getModMetaName(Class<? extends LiteMod> modClass)
	{
		return this.enumerator.getModMetaName(modClass);
	}
	
	/**
	 * Get the mod "name" metadata key, this is used for versioning, exclusivity, and enablement checks
	 * 
	 * @param modClass
	 * @return
	 */
	public Class<? extends LiteMod> getModFromMetaName(String modName)
	{
		if (modName == null) return null;
		
		for (LiteMod mod : this.mods)
		{
			if (modName.equalsIgnoreCase(this.enumerator.getModMetaName(mod.getClass())))
			{
				return mod.getClass();
			}
		}
		
		return null;
	}
	
	/**
	 * @param modMetaName Mod meta name to enable
	 */
	public void enableMod(String modMetaName)
	{
		this.setModEnabled(modMetaName, true);
	}

	/**
	 * @param modMetaName Mod meta name to disable
	 */
	public void disableMod(String modMetaName)
	{
		this.setModEnabled(modMetaName, false);
	}
	
	/**
	 * @param modMetaName Mod meta name to enable/disable
	 * @param enabled
	 */
	public void setModEnabled(String modMetaName, boolean enabled)
	{
		this.enabledModsList.setEnabled(this.bootstrap.getProfile(), modMetaName, enabled);
		this.enabledModsList.saveTo(this.enabledModsFile);
	}

	/**
	 * @param modName
	 * @return
	 */
	public boolean isModEnabled(String modName)
	{
		return this.enabledModsList.isEnabled(LiteLoader.getProfile(), modName);
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
			if (modName.equalsIgnoreCase(this.enumerator.getModMetaName(mod.getClass())))
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
	private void loadMods()
	{
		if (!this.enumerator.hasModsToLoad())
		{
			LiteLoader.logInfo("Mod class discovery failed or no mod classes were found. Not loading any mods.");
			return;
		}
		
		LiteLoader.logInfo("Discovered %d total mod(s)", this.enumerator.modsToLoadCount());
		
		this.pendingResourceReload = false;
		this.soundManagerReloadInhibitor = new SoundManagerReloadInhibitor((SimpleReloadableResourceManager)this.minecraft.getResourceManager(), this.minecraft.sndManager);
		if (this.inhibitSoundManagerReload) this.soundManagerReloadInhibitor.inhibit();
		
		for (Class<? extends LiteMod> mod : this.enumerator.getModsToLoad())
		{
			try
			{
				String metaName = this.getModMetaName(mod);
				if (metaName == null || this.enabledModsList.isEnabled(this.bootstrap.getProfile(), metaName))
				{
					this.loadMod(metaName, mod);
				}
				else
				{
					LiteLoader.logInfo("Not loading mod %s, excluded by filter", metaName);
					this.disabledMods.add(this.enumerator.getModFile(mod));
				}
			}
			catch (Throwable th)
			{
				th.printStackTrace(System.out);
				LiteLoader.getLogger().log(Level.WARNING, String.format("Error loading mod from %s", mod.getName()), th);
			}
		}
	}

	/**
	 * @param metaName
	 * @param mod
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	protected void loadMod(String metaName, Class<? extends LiteMod> mod) throws InstantiationException, IllegalAccessException
	{
		LiteLoader.logInfo("Loading mod from %s", mod.getName());
		
		LiteMod newMod = mod.newInstance();
		
		this.mods.add(newMod);
		String modName = newMod.getName();
		if (modName == null && metaName != null) modName = metaName;
		LiteLoader.logInfo("Successfully added mod %s version %s", modName, newMod.getVersion());
		
		// Get the mod file and register it as a resource pack if it exists
		ModFile modFile = this.enumerator.getModFile(mod);
		if (modFile != null)
		{
			this.disabledMods.remove(modFile);
			
			LiteLoader.logInfo("Adding \"%s\" to active resource pack set", modFile.getAbsolutePath());
			if (modName != null)
			{
				modFile.initResourcePack(modName);
				
				if (modFile.hasResourcePack() && this.registerModResourcePack((ResourcePack)modFile.getResourcePack()))
				{
					LiteLoader.logInfo("Successfully added \"%s\" to active resource pack set", modFile.getAbsolutePath());
				}
			}
		}
	}

	/**
	 * Initialise the mods which were loaded
	 */
	private void initMods()
	{
		this.loadedModsList = "";
		int loadedModsCount = 0;
		
		for (Iterator<LiteMod> iter = this.mods.iterator(); iter.hasNext();)
		{
			LiteMod mod = iter.next();
			
			try
			{
				this.initMod(mod);
				loadedModsCount++;
			}
			catch (Throwable th)
			{
				LiteLoader.getLogger().log(Level.WARNING, "Error initialising mod '" + mod.getName() + "'", th);
				iter.remove();
			}
		}
		
		this.loadedModsList = String.format("%s loaded mod(s)%s", loadedModsCount, this.loadedModsList);
	}

	/**
	 * @param mod
	 */
	protected void initMod(LiteMod mod)
	{
		LiteLoader.logInfo("Initialising mod %s version %s", mod.getName(), mod.getVersion());
		
		try
		{
			this.handleModVersionUpgrade(mod);
		}
		catch (Throwable th)
		{
			LiteLoader.logWarning("Error performing settings upgrade for %s. Settings may not be properly migrated", mod.getName());
		}
		
		// initialise the mod
		mod.init(this.commonConfigFolder); 
		
		// add the mod to all relevant listener queues
		this.events.addListener(mod);
		
		// add mod to permissions manager if permissible
		this.permissionsManager.registerMod(mod);
		
		// register mod config panel if configurable
		this.configPanelManager.registerMod(mod);
		
		this.loadedMods.add(mod);
		this.loadedModsList += String.format("\n          - %s version %s", mod.getName(), mod.getVersion());
	}

	/**
	 * @param mod
	 */
	protected void handleModVersionUpgrade(LiteMod mod)
	{
		String modKey = this.getModNameForConfig(mod.getClass(), mod.getName());
		LiteLoaderVersion lastModVersion = LiteLoaderVersion.getVersionFromRevision(this.bootstrap.getLastKnownModRevision(modKey));
		
		if (LiteLoaderBootstrap.VERSION.getLoaderRevision() > lastModVersion.getLoaderRevision())
		{
			LiteLoader.logInfo("Performing config upgrade for mod %s. Upgrading %s to %s...", mod.getName(), lastModVersion, LiteLoaderBootstrap.VERSION);
			mod.upgradeSettings(LiteLoaderBootstrap.VERSION.getMinecraftVersion(), this.versionConfigFolder, this.inflectVersionedConfigPath(lastModVersion));
			
			this.bootstrap.storeLastKnownModRevision(modKey);
			LiteLoader.logInfo("Config upgrade succeeded for mod %s", mod.getName());
		}
	}
	
	/**
	 * Used by the version upgrade code, gets a version of the mod name suitable
	 * for inclusion in the properties file
	 * 
	 * @param modName
	 * @return
	 */
	private String getModNameForConfig(Class<? extends LiteMod> modClass, String modName)
	{
		if (modName == null || modName.isEmpty())
		{
			modName = modClass.getSimpleName().toLowerCase();
		}
		
		return String.format("version.%s", modName.toLowerCase().replaceAll("[^a-z0-9_\\-\\.]", ""));
	}
	
	/**
	 * Called before mod late initialisation, refresh the resources that have been added so that mods can use them
	 */
	void preInitMods()
	{
		if (this.pendingResourceReload)
		{
			this.pendingResourceReload = false;
			this.minecraft.refreshResources();
		}
	}
	
	/**
	 * Called after mod late init
	 */
	void preBeginGame()
	{
		// Set the loader branding in ClientBrandRetriever using reflection
		LiteLoaderBootstrap.setBranding("LiteLoader");
		
		if (this.soundManagerReloadInhibitor != null && this.soundManagerReloadInhibitor.isInhibited())
		{
			this.soundManagerReloadInhibitor.unInhibit(true);
		}
	}

	/**
	 * Called on login
	 * 
	 * @param netHandler
	 * @param loginPacket
	 */
	void onLogin(NetHandler netHandler, Packet1Login loginPacket)
	{
		this.permissionsManager.onLogin(netHandler, loginPacket);
	}
	
	/**
	 * Called when the world reference is changed
	 * 
	 * @param world
	 */
	void onWorldChanged(World world)
	{
		if (world != null)
		{
			// For bungeecord
			this.permissionsManager.scheduleRefresh();
		}
	}

	/**
	 * On render callback
	 */
	void onRender()
	{
		if (this.paginateControls && this.minecraft.currentScreen != null && this.minecraft.currentScreen.getClass().equals(GuiControls.class))
		{
			try
			{
				// Try to get the parent screen entry from the existing screen
				GuiScreen parentScreen = PrivateFields.guiControlsParentScreen.get((GuiControls)this.minecraft.currentScreen);
				this.minecraft.displayGuiScreen(new GuiControlsPaginated(parentScreen, this.minecraft.gameSettings));
			}
			catch (Exception ex)
			{
			}
		}
	}
	
	/**
	 * @param partialTicks
	 */
	void postRender(int mouseX, int mouseY, float partialTicks)
	{
		if (this.minecraft.currentScreen instanceof GuiMainMenu && this.displayModInfoScreenTab && !this.hideModInfoScreenTab)
		{
			// If we're at the main menu, prepare the overlay
			if (this.modInfoScreen == null || this.modInfoScreen.getMenu() != this.minecraft.currentScreen)
			{
				this.modInfoScreen = new GuiScreenModInfo(this.minecraft, (GuiMainMenu)this.minecraft.currentScreen, this, this.enabledModsList, this.configPanelManager);
			}

			this.modInfoScreen.drawScreen(mouseX, mouseY, partialTicks);
		}
		else if (this.minecraft.currentScreen != this.modInfoScreen && this.modInfoScreen != null)
		{
			// If we're in any other screen, kill the overlay
			this.modInfoScreen.release();
			this.modInfoScreen = null;
		}
		else if (this.minecraft.currentScreen instanceof GuiMainMenu && Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) && Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) && Keyboard.isKeyDown(Keyboard.KEY_TAB))
		{
			this.minecraft.displayGuiScreen(new GuiScreenModInfo(this.minecraft, (GuiMainMenu)this.minecraft.currentScreen, this, this.enabledModsList, this.configPanelManager));
		}			
	}

	/**
	 * @param partialTicks
	 * @param inGame
	 */
	void onTick(float partialTicks, boolean inGame)
	{
		// Tick the permissions manager
		this.permissionsManager.onTick(this.minecraft, partialTicks, inGame);
		
		this.checkAndStoreKeyBindings();
		
		if (this.modInfoScreen != null && this.minecraft.currentScreen != this.modInfoScreen)
		{
			this.modInfoScreen.updateScreen();
		}
	}
	
	/**
	 * Register a key for a mod
	 * 
	 * @param binding
	 */
	public void registerModKey(KeyBinding binding)
	{
		LinkedList<KeyBinding> keyBindings = new LinkedList<KeyBinding>();
		keyBindings.addAll(Arrays.asList(this.minecraft.gameSettings.keyBindings));
		
		if (!keyBindings.contains(binding))
		{
			if (this.keyMapSettings.containsKey(binding.keyDescription))
			{
				try
				{
					binding.keyCode = Integer.parseInt(this.keyMapSettings.getProperty(binding.keyDescription, String.valueOf(binding.keyCode)));
				}
				catch (NumberFormatException ex) {}
			}

			keyBindings.add(binding);
			this.minecraft.gameSettings.keyBindings = keyBindings.toArray(new KeyBinding[0]);
			this.modKeyBindings.add(binding);
			
			this.updateBinding(binding);
			this.storeBindings();
		}
	}
	
	/**
	 * Checks for changed mod keybindings and stores any that have changed 
	 */
	private void checkAndStoreKeyBindings()
	{
		boolean updated = false;
		
		for (KeyBinding binding : this.modKeyBindings)
		{
			if (binding.keyCode != this.storedModKeyBindings.get(binding))
			{
				this.updateBinding(binding);
				updated = true;
			}
		}
		
		if (updated)
			this.storeBindings();
	}
	
	/**
	 * @param binding
	 */
	private void updateBinding(KeyBinding binding)
	{
		this.keyMapSettings.setProperty(binding.keyDescription, String.valueOf(binding.keyCode));
		this.storedModKeyBindings.put(binding, Integer.valueOf(binding.keyCode));
	}

	/**
	 * Writes mod bindings to disk
	 */
	private void storeBindings()
	{
		try
		{
			this.keyMapSettings.store(new FileWriter(this.keyMapSettingsFile), "Mod key mappings for LiteLoader mods, stored here to avoid losing settings stored in options.txt");
		}
		catch (IOException ex) {}
	}
	
	/**
	 * Set the "mod info" screen tab to hidden, regardless of the property setting
	 */
	public void hideModInfoScreenTab()
	{
		this.hideModInfoScreenTab = true;
	}
	
	/**
	 * Set whether the "mod info" screen tab should be shown in the main menu
	 */
	public void setDisplayModInfoScreenTab(boolean show)
	{
		this.displayModInfoScreenTab = show;
		this.bootstrap.setBooleanProperty(OPTION_MOD_INFO_SCREEN, show);
		this.bootstrap.writeProperties();
	}
	
	/**
	 * Get whether the "mod info" screen tab is shown in the main menu
	 */
	public boolean getDisplayModInfoScreenTab()
	{
		return this.displayModInfoScreenTab;
	}
	
	private static void logInfo(String string, Object... args)
	{
		LiteLoader.logger.info(String.format(string, args));
	}

	private static void logWarning(String string, Object... args)
	{
		LiteLoader.logger.warning(String.format(string, args));
	}

	/**
	 * @param objCrashReport This is an object so that we don't need to transform the obfuscated name in the transformer
	 */
	public static void populateCrashReport(Object objCrashReport)
	{
		if (objCrashReport instanceof CrashReport)
		{
			CrashReport crashReport = (CrashReport)objCrashReport;
			crashReport.getCategory().addCrashSectionCallable("Mod Pack",        new CallableLiteLoaderBrand(crashReport));
			crashReport.getCategory().addCrashSectionCallable("LiteLoader Mods", new CallableLiteLoaderMods(crashReport));
		}
	}
}