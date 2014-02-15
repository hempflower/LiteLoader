package com.mumfrey.liteloader.core;

import java.io.File;
import java.io.PrintStream;
import java.util.*;

import javax.activity.InvalidActivityException;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.crash.CrashReport;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraft.network.INetHandler;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.world.World;

import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

import com.mumfrey.liteloader.LiteMod;
import com.mumfrey.liteloader.core.overlays.IMinecraft;
import com.mumfrey.liteloader.crashreport.CallableLaunchWrapper;
import com.mumfrey.liteloader.crashreport.CallableLiteLoaderBrand;
import com.mumfrey.liteloader.crashreport.CallableLiteLoaderMods;
import com.mumfrey.liteloader.gui.GuiScreenModInfo;
import com.mumfrey.liteloader.modconfig.ConfigManager;
import com.mumfrey.liteloader.modconfig.Exposable;
import com.mumfrey.liteloader.permissions.PermissionsManagerClient;
import com.mumfrey.liteloader.util.Input;
import com.mumfrey.liteloader.resources.InternalResourcePack;
import com.mumfrey.liteloader.util.log.LiteLoaderLogger;

/**
 * LiteLoader is a simple loader which loads and provides useful callbacks to
 * lightweight mods
 * 
 * @author Adam Mummery-Smith
 * @version 1.7.2_04
 */
public final class LiteLoader
{
	public static final String MOD_SYSTEM = "liteloader";
	
	private static final String OPTION_MOD_INFO_SCREEN   = "modInfoScreen";
	private static final String OPTION_SOUND_MANAGER_FIX = "soundManagerFix";
	private static final String OPTION_GENERATE_MAPPINGS = "genMappings";

	/**
	 * LiteLoader is a singleton, this is the singleton instance
	 */
	private static LiteLoader instance;
	
	/**
	 * Logger for LiteLoader events
	 */
	private static final Logger logger = LiteLoaderLogger.getLogger();
	
	/**
	 * Tweak system class loader 
	 */
	private static LaunchClassLoader classLoader;
	
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
	 * Reference to the Minecraft game instance
	 */
	private Minecraft minecraft;
	
	/**
	 * Loader Bootstrap instance 
	 */
	private final LiteLoaderBootstrap bootstrap;
	
	/**
	 * Mod enumerator instance
	 */
	private final LiteLoaderEnumerator enumerator;
	
	/**
	 * List of mods passed into the command line
	 */
	private final EnabledModsList enabledModsList;
	
	/**
	 * Registered resource packs 
	 */
	private final Map<String, IResourcePack> registeredResourcePacks = new HashMap<String, IResourcePack>();
	
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
	private final LinkedList<Loadable<?>> disabledMods = new LinkedList<Loadable<?>>();
	
	/**
	 * Event manager
	 */
	private Events events;
	
	/**
	 * Plugin channel manager 
	 */
	private final ClientPluginChannels clientPluginChannels = new ClientPluginChannels();
	
	/**
	 * Server channel manager 
	 */
	private final ServerPluginChannels serverPluginChannels = new ServerPluginChannels();
	
	/**
	 * Permission Manager
	 */
	private final PermissionsManagerClient permissionsManager = PermissionsManagerClient.getInstance();
	
	/**
	 * Mod configuration manager
	 */
	private final ConfigManager configManager;
	
	/**
	 * Flag which keeps track of whether late initialisation has completed
	 */
	private boolean startupComplete;

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
	private SoundHandlerReloadInhibitor soundHandlerReloadInhibitor;
	
	/**
	 * 
	 */
	private Input input;

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
	 * LiteLoader constructor
	 * @param profile 
	 * @param modNameFilter 
	 */
	private LiteLoader(LiteLoaderBootstrap bootstrap, LiteLoaderEnumerator enumerator, EnabledModsList enabledModsList)
	{
		this.bootstrap = bootstrap;
		this.enumerator = enumerator;
		this.enabledModsList = enabledModsList;
		
		this.setupPaths(bootstrap);
		
		this.configManager = new ConfigManager();
		this.input = new Input(new File(this.commonConfigFolder, "liteloader.keys.properties"));
	}
	
	/**
	 * Set up paths used by the loader
	 */
	private void setupPaths(LiteLoaderBootstrap bootstrap)
	{
		this.modsFolder = bootstrap.getModsFolder();
		this.configBaseFolder = bootstrap.getConfigBaseFolder();
		
		this.commonConfigFolder = new File(this.configBaseFolder, "common");
		this.versionConfigFolder = this.inflectVersionedConfigPath(LiteLoaderVersion.CURRENT);
		
		if (!this.modsFolder.exists()) this.modsFolder.mkdirs();
		if (!this.configBaseFolder.exists()) this.configBaseFolder.mkdirs();
		if (!this.commonConfigFolder.exists()) this.commonConfigFolder.mkdirs();
		if (!this.versionConfigFolder.exists()) this.versionConfigFolder.mkdirs();
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
	void init()
	{
		try
		{
			this.input.init();
			
			this.inhibitSoundManagerReload = this.bootstrap.getAndStoreBooleanProperty(OPTION_SOUND_MANAGER_FIX, true);
			this.displayModInfoScreenTab = this.bootstrap.getAndStoreBooleanProperty(OPTION_MOD_INFO_SCREEN, true);
			
			this.enumerator.discoverModClasses();
			this.disabledMods.addAll(this.enumerator.getDisabledMods());
		}
		catch (Throwable th)
		{
			LiteLoaderLogger.severe("Error initialising LiteLoader", th);
		}
	}
	
	void postInit()
	{
		// Cache local minecraft reference
		this.minecraft = Minecraft.getMinecraft();
		
		// Add self as a resource pack for texture/lang resources
		this.registerModResourcePack(new InternalResourcePack("LiteLoader", LiteLoader.class, "liteloader"));
		
		// Create the event broker
		this.events = new Events(this, this.minecraft, this.clientPluginChannels, this.serverPluginChannels, this.bootstrap.getBooleanProperty(OPTION_GENERATE_MAPPINGS));
		
		// Spawn mod instances
		this.loadMods();
		
		// Initialises enumerated mods
		this.initMods();
		
		this.updateSharedModList();
		
		// Initialises the required hooks for loaded mods
		this.events.initHooks();
		this.startupComplete = true;
		
		// Save stuff
		this.enabledModsList.save();
		this.bootstrap.writeProperties();
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.core.ICustomResourcePackManager#registerModResourcePack(net.minecraft.client.resources.ResourcePack)
	 */
	public boolean registerModResourcePack(IResourcePack resourcePack)
	{
		if (!this.registeredResourcePacks.containsKey(resourcePack.getPackName()))
		{
			this.pendingResourceReload = true;

			List<IResourcePack> defaultResourcePacks = ((IMinecraft)this.minecraft).getDefaultResourcePacks();
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
	 * @see com.mumfrey.liteloader.core.ICustomResourcePackManager#unRegisterModResourcePack(net.minecraft.client.resources.ResourcePack)
	 */
	public boolean unRegisterModResourcePack(IResourcePack resourcePack)
	{
		if (this.registeredResourcePacks.containsValue(resourcePack))
		{
			this.pendingResourceReload = true;

			List<IResourcePack> defaultResourcePacks = ((IMinecraft)this.minecraft).getDefaultResourcePacks();
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
	 * @deprecated use LiteLoaderLogger instead
	 */
	@Deprecated
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
	 * @return System.err
	 * @deprecated use log4j instead
	 */
	@Deprecated
	public static final PrintStream getConsoleStream()
	{
		return System.err;
	}
	
	/**
	 * Get LiteLoader version
	 * 
	 * @return
	 */
	public static final String getVersion()
	{
		return LiteLoaderVersion.CURRENT.getLoaderVersion();
	}
	
	/**
	 * Get LiteLoader version
	 * 
	 * @return
	 */
	public static final String getVersionDisplayString()
	{
		return String.format("LiteLoader %s", LiteLoaderVersion.CURRENT.getLoaderVersion());
	}
	
	/**
	 * Get the loader revision
	 * 
	 * @return
	 */
	public static final int getRevision()
	{
		return LiteLoaderVersion.CURRENT.getLoaderRevision();
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
	 * @deprecated use LiteLoader.getClientPluginChannels()
	 */
	@Deprecated
	public static ClientPluginChannels getPluginChannels()
	{
		return LiteLoader.getInstance().clientPluginChannels;
	}

	/**
	 * Get the client-side plugin channel manager
	 * 
	 * @return
	 */
	public static ClientPluginChannels getClientPluginChannels()
	{
		return LiteLoader.getInstance().clientPluginChannels;
	}
	
	/**
	 * Get the server-side plugin channel manager
	 * 
	 * @return
	 */
	public static ServerPluginChannels getServerPluginChannels()
	{
		return LiteLoader.getInstance().serverPluginChannels;
	}
	
	/**
	 * Get the input manager
	 */
	public static Input getInput()
	{
		return LiteLoader.getInstance().input;
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
	 * Used to get the name of the modpack being used
	 * 
	 * @return name of the modpack in use or null if no pack
	 */
	public static String getBranding()
	{
		return LiteLoader.getInstance().bootstrap.getBranding();
	}
	
	/**
	 * @return
	 */
	public static boolean isDevelopmentEnvironment()
	{
		return "true".equals(System.getProperty("mcpenv"));
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
	 * Get a reference to a loaded mod, if the mod exists
	 * 
	 * @param modName Mod's name, identifier or class name
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
			Class<? extends LiteMod> modClass = mod.getClass();
			String modId = this.getModIdentifier(modClass);
			
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
		return this.enumerator.getModIdentifier(modClass);
	}
	
	/**
	 * Get the mod identifier, this is used for versioning, exclusivity, and enablement checks
	 * 
	 * @param modClass
	 * @return
	 */
	public String getModIdentifier(LiteMod mod)
	{
		return mod == null ? null : this.enumerator.getModIdentifier(mod.getClass());
	}
	
	/**
	 * Get the container (mod file, classpath jar or folder) for the specified mod
	 * 
	 * @param modClass
	 * @return
	 */
	public LoadableMod<?> getModContainer(Class<? extends LiteMod> modClass)
	{
		return this.enumerator.getModContainer(modClass);
	}
	
	/**
	 * Get the container (mod file, classpath jar or folder) for the specified mod
	 * 
	 * @param modClass
	 * @return
	 */
	public LoadableMod<?> getModContainer(LiteMod mod)
	{
		return mod == null ? null : this.enumerator.getModContainer(mod.getClass());
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
		
		for (LiteMod mod : this.mods)
		{
			if (identifier.equalsIgnoreCase(this.enumerator.getModIdentifier(mod.getClass())))
			{
				return mod.getClass();
			}
		}
		
		return null;
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
		this.enabledModsList.setEnabled(this.bootstrap.getProfile(), identifier, enabled);
		this.enabledModsList.save();
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
			if (modName.equalsIgnoreCase(this.enumerator.getModIdentifier(mod.getClass())))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * @param exposable
	 */
	public void writeConfig(Exposable exposable)
	{
		this.configManager.invalidateConfig(exposable);
	}
	
	/**
	 * Register an arbitrary Exposable
	 * 
	 * @param exposable Exposable object to register
	 * @param fileName Override config file name to use (leave null to use value from ExposableConfig specified value)
	 */
	public void registerExposable(Exposable exposable, String fileName)
	{
		this.configManager.registerExposable(exposable, fileName, true);
		this.configManager.initConfig(exposable);
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
			LiteLoaderLogger.info("Mod class discovery failed or no mod classes were found. Not loading any mods.");
			return;
		}
		
		LiteLoaderLogger.info("Discovered %d total mod(s)", this.enumerator.modsToLoadCount());
		
		this.pendingResourceReload = false;
		this.soundHandlerReloadInhibitor = new SoundHandlerReloadInhibitor((SimpleReloadableResourceManager)this.minecraft.getResourceManager(), this.minecraft.getSoundHandler());
		if (this.inhibitSoundManagerReload) this.soundHandlerReloadInhibitor.inhibit();
		
		for (Class<? extends LiteMod> mod : this.enumerator.getModsToLoad())
		{
			LoadableMod<?> container = this.enumerator.getModContainer(mod);

			try
			{
				String identifier = this.getModIdentifier(mod);
				if (identifier == null || this.enabledModsList.isEnabled(this.bootstrap.getProfile(), identifier))
				{
					if (this.enumerator.checkDependencies(container))
					{
						this.loadMod(identifier, mod, container);
					}
					else
					{
						LiteLoaderLogger.info("Not loading mod %s, the mod was missing a required dependency", identifier);
						if (container != LoadableMod.NONE && !this.disabledMods.contains(container)) this.disabledMods.add(container);
					}
				}
				else
				{
					LiteLoaderLogger.info("Not loading mod %s, excluded by filter", identifier);
					if (container != LoadableMod.NONE && !this.disabledMods.contains(container)) this.disabledMods.add(container);
				}
			}
			catch (Throwable th)
			{
				th.printStackTrace();
				LiteLoaderLogger.warning(th, "Error loading mod from %s", mod.getName());
				if (container != LoadableMod.NONE && !this.disabledMods.contains(container)) this.disabledMods.add(container);
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
	protected void loadMod(String identifier, Class<? extends LiteMod> mod, LoadableMod<?> container) throws InstantiationException, IllegalAccessException
	{
		LiteLoaderLogger.info("Loading mod from %s", mod.getName());
		
		LiteMod newMod = mod.newInstance();
		
		this.mods.add(newMod);
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
				
				if (container.hasResourcePack() && this.registerModResourcePack((IResourcePack)container.getResourcePack()))
				{
					LiteLoaderLogger.info("Successfully added \"%s\" to active resource pack set", container.getLocation());
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
				LiteLoaderLogger.warning(th, "Error initialising mod '%s'", mod.getName());
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
		LiteLoaderLogger.info("Initialising mod %s version %s", mod.getName(), mod.getVersion());
		
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
		
		// initialise the mod
		mod.init(this.commonConfigFolder); 
		
		// add the mod to all relevant listener queues
		this.events.addListener(mod);
		
		// add mod to permissions manager if permissible
		this.permissionsManager.registerMod(mod);
		
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
		
		if (LiteLoaderVersion.CURRENT.getLoaderRevision() > lastModVersion.getLoaderRevision())
		{
			LiteLoaderLogger.info("Performing config upgrade for mod %s. Upgrading %s to %s...", mod.getName(), lastModVersion, LiteLoaderVersion.CURRENT);
			
			// Migrate versioned config if any is present
			this.configManager.migrateModConfig(mod, this.versionConfigFolder, this.inflectVersionedConfigPath(lastModVersion));
			
			// Let the mod upgrade
			mod.upgradeSettings(LiteLoaderVersion.CURRENT.getMinecraftVersion(), this.versionConfigFolder, this.inflectVersionedConfigPath(lastModVersion));
			
			this.bootstrap.storeLastKnownModRevision(modKey);
			LiteLoaderLogger.info("Config upgrade succeeded for mod %s", mod.getName());
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
		
		if (this.soundHandlerReloadInhibitor != null && this.soundHandlerReloadInhibitor.isInhibited())
		{
			this.soundHandlerReloadInhibitor.unInhibit(true);
		}
	}

	/**
	 * Called on login
	 * 
	 * @param netHandler
	 * @param loginPacket
	 */
	void onJoinGame(INetHandler netHandler, S01PacketJoinGame loginPacket)
	{
		this.permissionsManager.onJoinGame(netHandler, loginPacket);
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
	 * @param partialTicks
	 */
	void postRender(int mouseX, int mouseY, float partialTicks)
	{
		boolean tabHidden = this.hideModInfoScreenTab && this.minecraft.currentScreen instanceof GuiMainMenu;
		
		if (GuiScreenModInfo.isSupportedOnScreen(this.minecraft.currentScreen) && ((this.displayModInfoScreenTab && !tabHidden) || (this.modInfoScreen != null && this.modInfoScreen.isTweeningOrOpen())))
		{
			// If we're at the main menu, prepare the overlay
			if (this.modInfoScreen == null || this.modInfoScreen.getScreen() != this.minecraft.currentScreen)
			{
				this.modInfoScreen = new GuiScreenModInfo(this.minecraft, this.minecraft.currentScreen, this, this.enabledModsList, this.configManager, tabHidden);
			}

			this.minecraft.entityRenderer.setupOverlayRendering();
			this.modInfoScreen.drawScreen(mouseX, mouseY, partialTicks);
		}
		else if (this.minecraft.currentScreen != this.modInfoScreen && this.modInfoScreen != null)
		{
			// If we're in any other screen, kill the overlay
			this.modInfoScreen.release();
			this.modInfoScreen = null;
		}
		else if (GuiScreenModInfo.isSupportedOnScreen(this.minecraft.currentScreen) && Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) && Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) && Keyboard.isKeyDown(Keyboard.KEY_TAB))
		{
			this.displayModInfoScreen(this.minecraft.currentScreen);
		}			
	}

	/**
	 * @param partialTicks
	 * @param inGame
	 */
	void onTick(boolean clock, float partialTicks, boolean inGame)
	{
		if (clock)
		{
			// Tick the permissions manager
			this.minecraft.mcProfiler.startSection("permissionsmanager");
			this.permissionsManager.onTick(this.minecraft, partialTicks, inGame);
			
			// Tick the config manager
			this.minecraft.mcProfiler.endStartSection("configmanager");
			this.configManager.onTick();
			
			if (this.modInfoScreen != null && this.minecraft.currentScreen != this.modInfoScreen)
			{
				this.modInfoScreen.updateScreen();
			}
			
			if (!((IMinecraft)this.minecraft).isRunning())
			{
				this.onShutDown();
			}
			
			this.minecraft.mcProfiler.endSection();
		}

		this.minecraft.mcProfiler.startSection("keybindings");
		this.input.onTick(clock);
		this.minecraft.mcProfiler.endSection();
	}

	private void onShutDown()
	{
		LiteLoaderLogger.info("LiteLoader is shutting down, syncing configuration");
		
		this.input.storeBindings();
		this.configManager.syncConfig();
	}

	/**
	 * Register a key for a mod
	 * 
	 * @param binding
	 * @deprecated Deprecated : use LiteLoader.getInput().registerKeyBinding() instead
	 */
	@Deprecated
	public void registerModKey(KeyBinding binding)
	{
		this.input.registerKeyBinding(binding);
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

	/**
	 * Display the "mod info" overlay over the specified GUI
	 * 
	 * @param parentScreen
	 */
	public void displayModInfoScreen(GuiScreen parentScreen)
	{
		if (GuiScreenModInfo.isSupportedOnScreen(parentScreen))
		{
			this.modInfoScreen = new GuiScreenModInfo(this.minecraft, parentScreen, this, this.enabledModsList, this.configManager, this.hideModInfoScreenTab);
			this.minecraft.displayGuiScreen(this.modInfoScreen);
		}
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
			crashReport.getCategory().addCrashSectionCallable("LaunchWrapper",   new CallableLaunchWrapper(crashReport));
		}
	}

	static final void init(LiteLoaderBootstrap bootstrap, LiteLoaderEnumerator enumerator, EnabledModsList enabledModsList, LaunchClassLoader classLoader)
	{
		if (LiteLoader.instance == null)
		{
			LiteLoader.classLoader = classLoader;
			
			LiteLoader.instance = new LiteLoader(bootstrap, enumerator, enabledModsList);
			LiteLoader.instance.init();
		}
	}
	
	private void updateSharedModList()
	{
		Map<String, Map<String, String>> modList = this.enumerator.getSharedModList();
		if (modList == null) return;
		
		for (LiteMod mod : this.mods)
		{
			String modKey = String.format("%s:%s", LiteLoader.MOD_SYSTEM, this.getModIdentifier(mod));
			modList.put(modKey, this.packModInfoToMap(mod));
		}
	}

	private Map<String, String> packModInfoToMap(LiteMod mod)
	{
		Map<String, String> modInfo = new HashMap<String, String>();
		LoadableMod<?> container = this.getModContainer(mod);
		
		modInfo.put("modsystem",   LiteLoader.MOD_SYSTEM);
		modInfo.put("id",          this.getModIdentifier(mod));
		modInfo.put("version",     mod.getVersion());
		modInfo.put("name",        mod.getName());
		modInfo.put("url",         container.getMetaValue("url", ""));
		modInfo.put("authors",     container.getAuthor());
		modInfo.put("description", container.getDescription(LiteLoaderEnumerator.getModClassName(mod)));
		
		return modInfo;
	}
}