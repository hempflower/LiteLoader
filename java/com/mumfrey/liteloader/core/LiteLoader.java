package com.mumfrey.liteloader.core;

import java.io.*;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javax.activity.InvalidActivityException;

import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraft.src.CrashReport;
import net.minecraft.src.GuiControls;
import net.minecraft.src.GuiScreen;
import net.minecraft.src.KeyBinding;
import net.minecraft.src.Minecraft;
import net.minecraft.src.NetHandler;
import net.minecraft.src.Packet1Login;
import net.minecraft.src.ResourcePack;
import net.minecraft.src.SimpleReloadableResourceManager;

import com.mumfrey.liteloader.*;
import com.mumfrey.liteloader.gui.GuiControlsPaginated;
import com.mumfrey.liteloader.launch.LiteLoaderTweaker;
import com.mumfrey.liteloader.log.LiteLoaderLogFormatter;
import com.mumfrey.liteloader.permissions.PermissionsManagerClient;
import com.mumfrey.liteloader.util.PrivateFields;

/**
 * LiteLoader is a simple loader which loads and provides useful callbacks to
 * lightweight mods
 * 
 * @author Adam Mummery-Smith
 * @version 1.6.4_01
 */
public final class LiteLoader implements FilenameFilter
{
	/**
	 * Liteloader version
	 */
	private static final LiteLoaderVersion VERSION = LiteLoaderVersion.MC_1_6_4_R1;
	
	/**
	 * Maximum recursion depth for mod discovery
	 */
	private static final int MAX_DISCOVERY_DEPTH = 16;
	
	/**
	 * LiteLoader is a singleton, this is the singleton instance
	 */
	private static LiteLoader instance;
	
	/**
	 * Logger for LiteLoader events
	 */
	private static final Logger logger = Logger.getLogger("liteloader");
	
	/**
	 * Use stdout rather than stderr
	 */
	private static boolean useStdOut = false;
	
	/**
	 * Game dir from launcher
	 */
	private static File gameDirectory;
	
	/**
	 * Assets dir from launcher
	 */
	private static File assetsDirectory;
	
	/**
	 * Profile name from launcher
	 */
	private static String profile = "";
	
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
	 * File to write log entries to
	 */
	private File logFile;
	
	/**
	 * Reference to the Minecraft game instance
	 */
	private Minecraft minecraft;
	
	/**
	 * File containing the properties
	 */
	private File propertiesFile;
	
	/**
	 * Internal properties loaded from inside the jar
	 */
	private Properties internalProperties = new Properties();
	
	/**
	 * LiteLoader properties
	 */
	private Properties localProperties = new Properties();
	
	/**
	 * Pack brand from properties, used to put the modpack/compilation name in
	 * crash reports
	 */
	private String branding = null;
	
	/**
	 * Setting value, if true we will swap out the MC "Controls" GUI for our
	 * custom, paginated one
	 */
	private boolean paginateControls = true;
	
	/**
	 * Classes to load, mapped by class name 
	 */
	private final Map<String, Class<? extends LiteMod>> modsToLoad = new HashMap<String, Class<? extends LiteMod>>();
	
	/**
	 * Mod metadata from version file 
	 */
	private final Map<String, ModFile> modFiles = new HashMap<String, ModFile>();
	
	/**
	 * Registered resource packs 
	 */
	private final Map<String, ResourcePack> registeredResourcePacks = new HashMap<String, ResourcePack>();
	
	/**
	 * List of loaded mods, for crash reporting
	 */
	private String loadedModsList = "none";
	
	/**
	 * Global list of mods which we can loaded
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
	 * Flag which keeps track of whether late initialisation has been done
	 */
	private boolean preInitStarted, preInitCompleted, postInitStarted, startupComplete;

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
	 * True if liteloader should also search files ending with .zip
	 */
	private boolean readZipFiles = false;
	
	/**
	 * True if the loader is allowed to load tweak classes from mod files 
	 */
	private final boolean loadTweaks;

	private boolean searchModsFolder = true;
	private boolean searchProtectionDomain = true;
	private boolean searchClassPath = true;
	
	/**
	 * Pre-init routine, called using reflection by the tweaker
	 * 
	 * @param gameDirectory Game directory passed to the tweaker
	 * @param assetsDirectory Assets directory passed to the tweaker
	 * @param profile Launch profile name supplied with --version parameter
	 * @param modNameFilter List of mod names parsed from the command line
	 * @param classLoader LaunchClassLoader
	 */
	@SuppressWarnings("unused")
	private static final void preInit(File gameDirectory, File assetsDirectory, String profile, List<String> modNameFilter, LaunchClassLoader classLoader, boolean loadTweaks)
	{
		if (LiteLoader.instance == null)
		{
			LiteLoader.gameDirectory = gameDirectory;
			LiteLoader.assetsDirectory = assetsDirectory;
			LiteLoader.profile = profile;
			LiteLoader.classLoader = classLoader;
			
			LiteLoader.instance = new LiteLoader(profile, modNameFilter, loadTweaks);
			LiteLoader.instance.onPreInit();
		}
	}
	
	/**
	 * Post-init routine, initialises and loads mods enumerated in preInit
	 */
	@SuppressWarnings("unused")
	private static final void postInit()
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
	private LiteLoader(String profile, List<String> modNameFilter, boolean loadTweaks)
	{
		// This valus is passed through from the tweaker, if we are being called pre-startup then this will be true
		// since we are starting up early enough to load tweaks. At the moment we can't do this because if we start
		// before a name transformer then all hell breaks loose later on. Need to split the mod enumeration into a separate
		// class before this is likely to work as intended.
		this.loadTweaks = loadTweaks;

		this.initPaths();
		
		this.enabledModsList = EnabledModsList.createFrom(this.enabledModsFile);
		this.enabledModsList.processModsList(profile, modNameFilter);
	}
	
	/**
	 * Set up paths used by the loader
	 */
	private void initPaths()
	{
		this.modsFolder = new File(LiteLoader.gameDirectory, "mods");
		this.configBaseFolder = new File(LiteLoader.gameDirectory, "liteconfig");
		
		this.commonConfigFolder = new File(this.configBaseFolder, "common");
		this.versionConfigFolder = this.inflectVersionedConfigPath(LiteLoader.VERSION);
		
		if (!this.modsFolder.exists()) this.modsFolder.mkdirs();
		if (!this.configBaseFolder.exists()) this.configBaseFolder.mkdirs();
		if (!this.commonConfigFolder.exists()) this.commonConfigFolder.mkdirs();
		if (!this.versionConfigFolder.exists()) this.versionConfigFolder.mkdirs();
		
		this.propertiesFile = new File(this.configBaseFolder, "liteloader.properties");
		this.enabledModsFile = new File(this.configBaseFolder, "liteloader.profiles.json");
		this.logFile = new File(this.configBaseFolder, "liteloader.log");
		this.keyMapSettingsFile = new File(this.configBaseFolder, "liteloader.keys.properties");
	}
	
	/**
	 * @param version
	 * @return
	 */
	protected File inflectVersionedConfigPath(LiteLoaderVersion version)
	{
		if (version.equals(LiteLoaderVersion.LEGACY))
		{
			return this.modsFolder;
		}
		
		return new File(this.configBaseFolder, String.format("config.%s", version.getMinecraftVersion()));
	}
	
	/**
	 * Loader initialisation
	 */
	private void onPreInit()
	{
		if (this.preInitStarted) return;
		this.preInitStarted = true;
		
		// Set up loader, initialises any reflection methods needed
		if (this.prepareLoader())
		{
			LiteLoader.logInfo("LiteLoader %s starting up...", VERSION.getLoaderVersion());
			
			// Print the branding version if any was provided
			if (this.branding != null)
			{
				LiteLoader.logInfo("Active Pack: %s", this.branding);
			}
			
			LiteLoader.logInfo("Java reports OS=\"%s\"", System.getProperty("os.name").toLowerCase());
			
			// Read the discovery settings from the properties 
			this.prepareDiscoverySettings();
			
			// Examines the class path and mods folder and locates loadable mods
			this.discoverMods();

			// Set the loader branding in ClientBrandRetriever using reflection
			this.setBranding("LiteLoader");
			
			LiteLoader.logInfo("LiteLoader PreInit completed");
			this.preInitCompleted = true;
		}
	}
	
	private void onPostInit(Minecraft minecraft)
	{
		if (!this.preInitCompleted || this.postInitStarted) return;
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
		this.writeProperties();
	}

	/**
	 * Set up reflection methods required by the loader
	 */
	private boolean prepareLoader()
	{
		try
		{
			// Prepare the properties
			this.prepareProperties();
			
			// Prepare the log writer
			this.prepareLogger();
			
			this.paginateControls = this.localProperties.getProperty("controls.pages", "true").equalsIgnoreCase("true");
			this.localProperties.setProperty("controls.pages", String.valueOf(this.paginateControls));
			
			this.inhibitSoundManagerReload = this.localProperties.getProperty("soundManagerFix", "true").equalsIgnoreCase("true");
			this.localProperties.setProperty("soundManagerFix", String.valueOf(this.inhibitSoundManagerReload));
			
			this.branding = this.internalProperties.getProperty("brand", null);
			if (this.branding != null && this.branding.length() < 1)
				this.branding = null;
			
			// Save appropriate branding in the local properties file
			if (this.branding != null)
				this.localProperties.setProperty("brand", this.branding);
			else
				this.localProperties.remove("brand");
			
		}
		catch (Throwable th)
		{
			LiteLoader.getLogger().log(Level.SEVERE, "Error initialising LiteLoader", th);
			return false;
		}
		
		return true;
	}
	
	/**
	 * @throws SecurityException
	 * @throws IOException
	 */
	private void prepareLogger() throws SecurityException, IOException
	{
		Formatter logFormatter = new LiteLoaderLogFormatter();
		
		LiteLoader.logger.setUseParentHandlers(false);
		LiteLoader.useStdOut = System.getProperty("liteloader.log", "stderr").equalsIgnoreCase("stdout") || this.localProperties.getProperty("log", "stderr").equalsIgnoreCase("stdout");
		
		StreamHandler consoleHandler = useStdOut ? new com.mumfrey.liteloader.util.log.ConsoleHandler() : new java.util.logging.ConsoleHandler();
		consoleHandler.setFormatter(logFormatter);
		LiteLoader.logger.addHandler(consoleHandler);
		
		FileHandler logFileHandler = new FileHandler(this.logFile.getAbsolutePath());
		logFileHandler.setFormatter(logFormatter);
		LiteLoader.logger.addHandler(logFileHandler);
	}
	
	/**
	 * Prepare the loader properties
	 */
	private void prepareProperties()
	{
		try
		{
			InputStream propertiesStream = LiteLoader.class.getResourceAsStream("/liteloader.properties");
			
			if (propertiesStream != null)
			{
				this.internalProperties.load(propertiesStream);
				propertiesStream.close();
			}
		}
		catch (Throwable th)
		{
			this.internalProperties = new Properties();
		}
		
		try
		{
			this.localProperties = new Properties(this.internalProperties);
			InputStream localPropertiesStream = this.getLocalPropertiesStream();
			
			if (localPropertiesStream != null)
			{
				this.localProperties.load(localPropertiesStream);
				localPropertiesStream.close();
			}
		}
		catch (Throwable th)
		{
			this.localProperties = new Properties(this.internalProperties);
		}

		if (this.keyMapSettingsFile.exists())
		{
			try
			{
				this.keyMapSettings.load(new FileReader(this.keyMapSettingsFile));
			}
			catch (Exception ex) {}
		}
	}

	/**
	 * 
	 */
	public void prepareDiscoverySettings()
	{
		this.readZipFiles           = this.localProperties.getProperty("search.zips",     "false").equalsIgnoreCase("true");
		this.searchModsFolder       = this.localProperties.getProperty("search.mods",      "true").equalsIgnoreCase("true");
		this.searchProtectionDomain = this.localProperties.getProperty("search.jar",       "true").equalsIgnoreCase("true");
		this.searchClassPath        = this.localProperties.getProperty("search.classpath", "true").equalsIgnoreCase("true");
		
		if (!this.searchModsFolder && !this.searchProtectionDomain && !this.searchClassPath)
		{
			LiteLoader.logWarning("Invalid configuration, no search locations defined. Enabling all search locations.");
			
			this.searchModsFolder       = true;
			this.searchProtectionDomain = true;
			this.searchClassPath        = true;
		}
		
		this.localProperties.setProperty("search.zips",      String.valueOf(this.readZipFiles));
		this.localProperties.setProperty("search.mods",      String.valueOf(this.searchModsFolder));
		this.localProperties.setProperty("search.jar",       String.valueOf(this.searchProtectionDomain));
		this.localProperties.setProperty("search.classpath", String.valueOf(this.searchClassPath));
	}

	/**
	 * Get the properties stream either from the jar or from the properties file
	 * in the minecraft folder
	 * 
	 * @return
	 * @throws FileNotFoundException
	 */
	private InputStream getLocalPropertiesStream() throws FileNotFoundException
	{
		if (this.propertiesFile.exists())
		{
			return new FileInputStream(this.propertiesFile);
		}
		
		// Otherwise read settings from the config
		return LiteLoader.class.getResourceAsStream("/liteloader.properties");
	}
	
	/**
	 * Write current properties to the properties file
	 */
	private void writeProperties()
	{
		try
		{
			this.localProperties.store(new FileWriter(this.propertiesFile), String.format("Properties for LiteLoader %s", VERSION));
		}
		catch (Throwable th)
		{
			LiteLoader.getLogger().log(Level.WARNING, "Error writing liteloader properties", th);
		}
	}
	
	/**
	 * Register a 
	 * 
	 * @param name
	 * @param resourcePack
	 * @return
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
	
	/**
	 * @param name
	 * @return
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
		return LiteLoader.useStdOut ? System.out : System.err;
	}
	
	/**
	 * Get LiteLoader version
	 * 
	 * @return
	 */
	public static final String getVersion()
	{
		return LiteLoader.VERSION.getLoaderVersion();
	}
	
	/**
	 * Get the loader revision
	 * 
	 * @return
	 */
	public static final int getRevision()
	{
		return LiteLoader.VERSION.getLoaderRevision();
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
		return LiteLoader.gameDirectory;
	}
	
	/**
	 * @return
	 */
	public static File getAssetsDirectory()
	{
		return LiteLoader.assetsDirectory;
	}
	
	/**
	 * @return
	 */
	public static String getProfile()
	{
		return LiteLoader.profile;
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
		return this.branding;
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
	 * Store current revision for mod in the config file
	 * 
	 * @param modKey
	 */
	private void storeLastKnownModRevision(String modKey)
	{
		if (this.localProperties != null)
		{
			this.localProperties.setProperty(modKey, String.valueOf(LiteLoader.VERSION.getLoaderRevision()));
			this.writeProperties();
		}
	}
	
	/**
	 * Get last know revision for mod from the config file 
	 * 
	 * @param modKey
	 * @return
	 */
	private int getLastKnownModRevision(String modKey)
	{
		if (this.localProperties != null)
		{
			String storedRevision = this.localProperties.getProperty(modKey, "0");
			return Integer.parseInt(storedRevision);
		}
		
		return 0;
	}
	
	/**
	 * Get a reference to a loaded mod, if the mod exists
	 * 
	 * @param modName Mod's name or class name
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
			if (modName.equalsIgnoreCase(mod.getName()) || modName.equalsIgnoreCase(mod.getClass().getSimpleName()))
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
	 * @throws InvalidActivityException
	 * @throws IllegalArgumentException
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
		return this.getModMetaData(mod.getClass(), metaDataKey, defaultValue);
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
		ModFile modFile = this.getModFile(modClass);
		return modFile != null ? modFile.getMetaValue(metaDataKey, defaultValue) : defaultValue;
	}
	
	/**
	 * @param mod
	 * @return
	 */
	private ModFile getModFile(Class<? extends LiteMod> modClass)
	{
		return this.modFiles.get(modClass.getSimpleName());
	}

	/**
	 * Get the mod "name" metadata key, this is used for versioning, exclusivity, and enablement checks
	 * 
	 * @param modClass
	 * @return
	 */
	public String getModMetaName(Class<? extends LiteMod> modClass)
	{
		String modClassName = modClass.getSimpleName();
		if (!this.modFiles.containsKey(modClassName)) return null;
		return this.modFiles.get(modClassName).getModName().toLowerCase();
	}

	/**
	 * Enumerate the java class path and "mods" folder to find mod classes, then
	 * load the classes
	 */
	private void discoverMods()
	{
		// List of mod files in the "mods" folder
		List<ModFile> modFiles = new LinkedList<ModFile>();
		
		if (this.searchModsFolder)
		{
			// Find and enumerate the "mods" folder
			File modFolder = LiteLoader.getModsFolder();
			if (modFolder.exists() && modFolder.isDirectory())
			{
				LiteLoader.logInfo("Mods folder found, searching %s", modFolder.getPath());
				this.findModFiles(modFolder, modFiles);
				LiteLoader.logInfo("Found %d mod file(s)", modFiles.size());
			}
		}
		
		try
		{
			LiteLoader.logInfo("Enumerating class path...");
			
			String classPath = System.getProperty("java.class.path");
			String classPathSeparator = System.getProperty("path.separator");
			String[] classPathEntries = classPath.split(classPathSeparator);
			
			LiteLoader.logInfo("Class path separator=\"%s\"", classPathSeparator);
			LiteLoader.logInfo("Class path entries=(\n   classpathEntry=%s\n)", classPath.replace(classPathSeparator, "\n   classpathEntry="));
			
			if (this.searchProtectionDomain || this.searchClassPath)
				LiteLoader.logInfo("Discovering mods on class path...");
			
			this.findModClasses(modFiles, classPathEntries);
			
			LiteLoader.logInfo("Mod class discovery completed");
		}
		catch (Throwable th)
		{
			LiteLoader.getLogger().log(Level.WARNING, "Mod class discovery failed", th);
			return;
		}
	}
	
	/**
	 * Find mod files in the "mods" folder
	 * 
	 * @param modFolder Folder to search
	 * @param modFiles List of mod files to load
	 */
	protected void findModFiles(File modFolder, List<ModFile> modFiles)
	{
		Map<String, TreeSet<ModFile>> versionOrderingSets = new HashMap<String, TreeSet<ModFile>>();
		
		for (File modFile : modFolder.listFiles(this))
		{
			try
			{
				String strVersion = null;
				
				// Check for a version file
				ZipFile modZip = new ZipFile(modFile);
				ZipEntry version = modZip.getEntry("litemod.json");

				// Not supporting this past 1.6.2
//				if (version == null)
//				{
//					version = modZip.getEntry("version.txt");
//				}
				
				if (version != null)
				{
					BufferedReader versionReader = null; 
					StringBuilder versionBuilder = new StringBuilder();
					
					try
					{
						// Read the version string
						InputStream versionStream = modZip.getInputStream(version);
						versionReader = new BufferedReader(new InputStreamReader(versionStream));

						String versionFileLine;
						while ((versionFileLine = versionReader.readLine()) != null)
							versionBuilder.append(versionFileLine);
						
						strVersion = versionBuilder.toString();
					}
					catch (Exception ex)
					{
						LiteLoader.logWarning("Error reading version data from %s", modFile.getName());
					}
					finally
					{
						if (versionReader != null) versionReader.close();
					}
					
					if (strVersion != null)
					{
						ModFile modFileInfo = new ModFile(modFile, strVersion);
						
						if (modFileInfo.isValid())
						{
							// Only add the mod if the version matches and we were able
							// to successfully add it to the class path
							if (LiteLoader.VERSION.isVersionSupported(modFileInfo.getVersion()))
							{
//								if (!modFileInfo.isJson())
//								{
//									LiteLoader.logWarning("Missing or invalid litemod.json reading mod file: %s", modFile.getAbsolutePath());
//								}
								
								if (!versionOrderingSets.containsKey(modFileInfo.getName()))
								{
									versionOrderingSets.put(modFileInfo.getModName(), new TreeSet<ModFile>());
								}
								
								LiteLoader.logInfo("Considering valid mod file: %s", modFileInfo.getAbsolutePath());
								versionOrderingSets.get(modFileInfo.getModName()).add(modFileInfo);
							}
							else
							{
								LiteLoader.logInfo("Not adding invalid or outdated mod file: %s", modFile.getAbsolutePath());
							}
						}
					}
				}
				else
				{
					ZipEntry legacyVersion = modZip.getEntry("version.txt");
					if (legacyVersion != null)
					{
						LiteLoader.logWarning("version.txt is no longer supported, ignoring outdated mod file: %s", modFile.getAbsolutePath());
					}
				}
				
				modZip.close();
			}
			catch (Exception ex)
			{
				ex.printStackTrace(System.err);
				LiteLoader.logInfo("Error enumerating '%s': Invalid zip file or error reading file", modFile.getAbsolutePath());
			}
		}

		// Copy the first entry in every version set into the modfiles list
		for (Entry<String, TreeSet<ModFile>> modFileEntry : versionOrderingSets.entrySet())
		{
			ModFile newestVersion = modFileEntry.getValue().iterator().next();

			try
			{
				LiteLoader.logInfo("Adding newest valid mod file '%s' at revision %.4f: ", newestVersion.getAbsolutePath(), newestVersion.getRevision());

				LiteLoader.classLoader.addURL(newestVersion.toURI().toURL());
				modFiles.add(newestVersion);
			}
			catch (Exception ex)
			{
				LiteLoader.logWarning("Error injecting '%s' into classPath. The mod will not be loaded", newestVersion.getAbsolutePath());
			}
			
			// Tweak load functionality, currently not used
			if (this.loadTweaks)
			{
				try
				{
					this.addTweaksFrom(newestVersion);
				}
				catch (Throwable th)
				{
					LiteLoader.logWarning("Error adding tweaks from '%s'", newestVersion.getAbsolutePath());
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.FilenameFilter#accept(java.io.File, java.lang.String)
	 */
	@Override
	public boolean accept(File dir, String fileName)
	{
		fileName = fileName.toLowerCase();
		return fileName.endsWith(".litemod") || (this.readZipFiles && (fileName.endsWith(".zip") || fileName.endsWith(".jar")));
	}

	/**
	 * @param modFile
	 * @throws IOException
	 */
	private void addTweaksFrom(ModFile modFile)
	{
		JarFile jar = null;
		
		LiteLoader.logInfo("Adding tweaks from file '%s'", modFile.getName());
		try
		{
			jar = new JarFile(modFile);
			Attributes manifestAttributes = jar.getManifest().getMainAttributes();
			
			String tweakClass = manifestAttributes.getValue("TweakClass");
			if (tweakClass != null)
			{
				LiteLoader.logInfo("Mod file '%s' provides tweakClass '%s', adding to Launch queue", modFile.getName(), tweakClass);
				
				if (LiteLoaderTweaker.addTweaker(modFile.toURI().toURL(), tweakClass))
				{
					LiteLoader.logInfo("tweakClass '%s' was successfully added", tweakClass);
				}
			}
			
			String classPath = manifestAttributes.getValue("Class-Path");
			if (classPath != null)
			{
				String[] classPathEntries = classPath.split(" ");
				for (String classPathEntry : classPathEntries)
				{
					File classPathJar = new File(LiteLoader.gameDirectory, classPathEntry);
					URL jarUrl = classPathJar.toURI().toURL();
					
					LiteLoader.logInfo("Adding Class-Path entry: %s", classPathEntry); 
					LiteLoaderTweaker.addURLToParentClassLoader(jarUrl);
					LiteLoader.classLoader.addURL(jarUrl);
				}
			}
		}
		catch (Exception ex)
		{
			LiteLoader.logWarning("Error parsing tweak class manifest entry in '%s'", modFile.getAbsolutePath());
		}
		finally
		{
			try
			{
				if (jar != null) jar.close();
			}
			catch (IOException ex) {}
		}
	}
	
	/**
	 * Find mod classes in the class path and enumerated mod files list
	 * @param classPathEntries Java class path split into string entries
	 */
	private void findModClasses(List<ModFile> modFiles, String[] classPathEntries)
	{
		if (this.searchProtectionDomain)
		{
			try
			{
				this.searchProtectionDomain();
			}
			catch (Throwable th)
			{
				LiteLoader.logWarning("Error loading from local class path: %s", th.getMessage());
			}
		}
		
		if (this.searchClassPath)
		{
			// Search through the class path and find mod classes
			this.searchClassPath(classPathEntries);
		}
		
		// Search through mod files and find mod classes
		this.searchModFiles(modFiles);
	}

	/**
	 * @param modsToLoad
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 * @throws UnsupportedEncodingException
	 */
	@SuppressWarnings("unchecked")
	private void searchProtectionDomain() throws MalformedURLException, URISyntaxException, UnsupportedEncodingException
	{
		LiteLoader.logInfo("Searching protection domain code source...");
		
		File packagePath = null;
		
		URL protectionDomainLocation = LiteLoader.class.getProtectionDomain().getCodeSource().getLocation();
		if (protectionDomainLocation != null)
		{
			if (protectionDomainLocation.toString().indexOf('!') > -1 && protectionDomainLocation.toString().startsWith("jar:"))
			{
				protectionDomainLocation = new URL(protectionDomainLocation.toString().substring(4, protectionDomainLocation.toString().indexOf('!')));
			}
			
			packagePath = new File(protectionDomainLocation.toURI());
		}
		else
		{
			// Fix (?) for forge and other mods which screw up the
			// protection domain
			String reflectionClassPath = LiteLoader.class.getResource("/com/mumfrey/liteloader/core/LiteLoader.class").getPath();
			
			if (reflectionClassPath.indexOf('!') > -1)
			{
				reflectionClassPath = URLDecoder.decode(reflectionClassPath, "UTF-8");
				packagePath = new File(reflectionClassPath.substring(5, reflectionClassPath.indexOf('!')));
			}
		}
		
		if (packagePath != null)
		{
			LinkedList<Class<?>> modClasses = getSubclassesFor(packagePath, LiteLoader.classLoader, LiteMod.class, "LiteMod");
			
			for (Class<?> mod : modClasses)
			{
				if (this.modsToLoad.containsKey(mod.getSimpleName()))
				{
					LiteLoader.logWarning("Mod name collision for mod with class '%s', maybe you have more than one copy?", mod.getSimpleName());
				}
				
				this.modsToLoad.put(mod.getSimpleName(), (Class<? extends LiteMod>)mod);
			}
			
			if (modClasses.size() > 0)
				LiteLoader.logInfo("Found %s potential matches", modClasses.size());
		}
	}

	/**
	 * @param classPathEntries
	 * @param modsToLoad
	 */
	@SuppressWarnings("unchecked")
	private void searchClassPath(String[] classPathEntries)
	{
		for (String classPathPart : classPathEntries)
		{
			LiteLoader.logInfo("Searching %s...", classPathPart);
			
			File packagePath = new File(classPathPart);
			LinkedList<Class<?>> modClasses = getSubclassesFor(packagePath, LiteLoader.classLoader, LiteMod.class, "LiteMod");
			
			for (Class<?> mod : modClasses)
			{
				if (this.modsToLoad.containsKey(mod.getSimpleName()))
				{
					LiteLoader.logWarning("Mod name collision for mod with class '%s', maybe you have more than one copy?", mod.getSimpleName());
				}
				
				this.modsToLoad.put(mod.getSimpleName(), (Class<? extends LiteMod>)mod);
				this.modFiles.put(mod.getSimpleName(), new ClassPathMod(packagePath, mod.getSimpleName().substring(7), LiteLoader.getVersion()));
			}
			
			if (modClasses.size() > 0)
				LiteLoader.logInfo("Found %s potential matches", modClasses.size());
		}
	}

	/**
	 * @param modFiles
	 * @param modsToLoad
	 */
	@SuppressWarnings("unchecked")
	private void searchModFiles(List<ModFile> modFiles)
	{
		for (ModFile modFile : modFiles)
		{
			LiteLoader.logInfo("Searching %s...", modFile.getAbsolutePath());
			
			LinkedList<Class<?>> modClasses = LiteLoader.getSubclassesFor(modFile, LiteLoader.classLoader, LiteMod.class, "LiteMod");
			
			for (Class<?> mod : modClasses)
			{
				if (this.modsToLoad.containsKey(mod.getSimpleName()))
				{
					LiteLoader.logWarning("Mod name collision for mod with class '%s', maybe you have more than one copy?", mod.getSimpleName());
				}
				
				this.modsToLoad.put(mod.getSimpleName(), (Class<? extends LiteMod>)mod);
				this.modFiles.put(mod.getSimpleName(), modFile);
			}
			
			if (modClasses.size() > 0)
				LiteLoader.logInfo("Found %s potential matches", modClasses.size());
		}
	}
	
	/**
	 * Create mod instances from the enumerated classes
	 * 
	 * @param modsToLoad List of mods to load
	 */
	private void loadMods()
	{
		if (this.modsToLoad == null)
		{
			LiteLoader.logInfo("Mod class discovery failed. Not loading any mods!");
			return;
		}
		
		LiteLoader.logInfo("Discovered %d total mod(s)", this.modsToLoad.size());
		
		this.pendingResourceReload = false;
		this.soundManagerReloadInhibitor = new SoundManagerReloadInhibitor((SimpleReloadableResourceManager)this.minecraft.getResourceManager(), this.minecraft.sndManager);
		if (this.inhibitSoundManagerReload) this.soundManagerReloadInhibitor.inhibit();
		
		for (Class<? extends LiteMod> mod : this.modsToLoad.values())
		{
			try
			{
				String metaName = this.getModMetaName(mod);
				if (metaName == null || this.enabledModsList.isEnabled(LiteLoader.profile, metaName))
				{
					LiteLoader.logInfo("Loading mod from %s", mod.getName());
					
					LiteMod newMod = mod.newInstance();
					
					this.mods.add(newMod);
					String modName = newMod.getName();
					if (modName == null && metaName != null) modName = metaName;
					LiteLoader.logInfo("Successfully added mod %s version %s", modName, newMod.getVersion());
					
					// Get the mod file and register it as a resource pack if it exists
					ModFile modFile = this.getModFile(mod);
					if (modFile != null)
					{
						this.disabledMods.remove(modFile);
						
						if (modName != null && modFile.registerAsResourcePack(modName))
						{
							LiteLoader.logInfo("Successfully added \"%s\" to active resource pack set", modFile.getAbsolutePath());
						}
					}
				}
				else
				{
					LiteLoader.logInfo("Not loading mod %s, excluded by filter", metaName);
					this.disabledMods.add(this.getModFile(mod));
				}
			}
			catch (Throwable th)
			{
				LiteLoader.getLogger().log(Level.WARNING, String.format("Error loading mod from %s", mod.getName()), th);
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
			String modName = mod.getName();
			
			try
			{
				LiteLoader.logInfo("Initialising mod %s version %s", modName, mod.getVersion());
				
				try
				{
					String modKey = this.getModNameForConfig(mod.getClass(), modName);
					LiteLoaderVersion lastModVersion = LiteLoaderVersion.getVersionFromRevision(this.getLastKnownModRevision(modKey));
					
					if (LiteLoader.VERSION.getLoaderRevision() > lastModVersion.getLoaderRevision())
					{
						LiteLoader.logInfo("Performing config upgrade for mod %s. Upgrading %s to %s...", modName, lastModVersion, LiteLoader.VERSION);
						mod.upgradeSettings(VERSION.getMinecraftVersion(), this.versionConfigFolder, this.inflectVersionedConfigPath(lastModVersion));
						
						this.storeLastKnownModRevision(modKey);
						LiteLoader.logInfo("Config upgrade succeeded for mod %s", modName);
					}
				}
				catch (Throwable th)
				{
					LiteLoader.logWarning("Error performing settings upgrade for %s. Settings may not be properly migrated", modName);
				}
				
				// pre-1.6.4_01 this was being called with the wrong path, I hope this doesn't break anything
				mod.init(this.commonConfigFolder); 
				
				this.events.addListener(mod);
				
				if (mod instanceof Permissible)
				{
					this.permissionsManager.registerPermissible((Permissible)mod);
				}
				
				this.loadedMods.add(mod);
				this.loadedModsList += String.format("\n          - %s version %s", modName, mod.getVersion());
				loadedModsCount++;
			}
			catch (Throwable th)
			{
				LiteLoader.getLogger().log(Level.WARNING, "Error initialising mod '" + modName, th);
				iter.remove();
			}
		}
		
		this.loadedModsList = String.format("%s loaded mod(s)%s", loadedModsCount, this.loadedModsList);
	}
	
	/**
	 * Enumerate classes on the classpath which are subclasses of the specified
	 * class
	 * 
	 * @param superClass
	 * @return
	 */
	private static LinkedList<Class<?>> getSubclassesFor(File packagePath, ClassLoader classloader, Class<?> superClass, String prefix)
	{
		LinkedList<Class<?>> classes = new LinkedList<Class<?>>();
		
		try
		{
			if (packagePath.isDirectory())
			{
				enumerateDirectory(prefix, superClass, classloader, classes, packagePath);
			}
			else if (packagePath.isFile() && (packagePath.getName().endsWith(".jar") || packagePath.getName().endsWith(".zip") || packagePath.getName().endsWith(".litemod")))
			{
				enumerateCompressedPackage(prefix, superClass, classloader, classes, packagePath);
			}
		}
		catch (Throwable th)
		{
			LiteLoader.getLogger().log(Level.WARNING, "Enumeration error", th);
		}
		
		return classes;
	}
	
	/**
	 * @param superClass
	 * @param classloader
	 * @param classes
	 * @param packagePath
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private static void enumerateCompressedPackage(String prefix, Class<?> superClass, ClassLoader classloader, LinkedList<Class<?>> classes, File packagePath) throws FileNotFoundException, IOException
	{
		FileInputStream fileinputstream = new FileInputStream(packagePath);
		ZipInputStream zipinputstream = new ZipInputStream(fileinputstream);
		
		ZipEntry zipentry = null;
		
		do
		{
			zipentry = zipinputstream.getNextEntry();
			
			if (zipentry != null && zipentry.getName().endsWith(".class"))
			{
				String classFileName = zipentry.getName();
				String className = classFileName.lastIndexOf('/') > -1 ? classFileName.substring(classFileName.lastIndexOf('/') + 1) : classFileName;
				
				if (prefix == null || className.startsWith(prefix))
				{
					try
					{
						String fullClassName = classFileName.substring(0, classFileName.length() - 6).replaceAll("/", ".");
						checkAndAddClass(classloader, superClass, classes, fullClassName);
					}
					catch (Exception ex)
					{
					}
				}
			}
		} while (zipentry != null);
		
		fileinputstream.close();
	}
	
	/**
	 * Recursive function to enumerate classes inside a classpath folder
	 * 
	 * @param superClass
	 * @param classloader
	 * @param classes
	 * @param packagePath
	 * @param packageName
	 */
	private static void enumerateDirectory(String prefix, Class<?> superClass, ClassLoader classloader, LinkedList<Class<?>> classes, File packagePath)
	{
		enumerateDirectory(prefix, superClass, classloader, classes, packagePath, "", 0);
	}
	
	/**
	 * Recursive function to enumerate classes inside a classpath folder
	 * 
	 * @param superClass
	 * @param classloader
	 * @param classes
	 * @param packagePath
	 * @param packageName
	 */
	private static void enumerateDirectory(String prefix, Class<?> superClass, ClassLoader classloader, LinkedList<Class<?>> classes, File packagePath, String packageName, int depth)
	{
		// Prevent crash due to broken recursion
		if (depth > MAX_DISCOVERY_DEPTH)
			return;
		
		File[] classFiles = packagePath.listFiles();
		
		for (File classFile : classFiles)
		{
			if (classFile.isDirectory())
			{
				enumerateDirectory(prefix, superClass, classloader, classes, classFile, packageName + classFile.getName() + ".", depth + 1);
			}
			else
			{
				if (classFile.getName().endsWith(".class") && (prefix == null || classFile.getName().startsWith(prefix)))
				{
					String classFileName = classFile.getName();
					String className = packageName + classFileName.substring(0, classFileName.length() - 6);
					checkAndAddClass(classloader, superClass, classes, className);
				}
			}
		}
	}
	
	/**
	 * @param classloader
	 * @param superClass
	 * @param classes
	 * @param className
	 */
	private static void checkAndAddClass(ClassLoader classloader, Class<?> superClass, LinkedList<Class<?>> classes, String className)
	{
		if (className.indexOf('$') > -1)
			return;
		
		try
		{
			Class<?> subClass = classloader.loadClass(className);
			
			if (subClass != null && !superClass.equals(subClass) && superClass.isAssignableFrom(subClass) && !subClass.isInterface() && !classes.contains(subClass))
			{
				classes.add(subClass);
			}
		}
		catch (Throwable th)
		{
			LiteLoader.getLogger().log(Level.WARNING, "checkAndAddClass error", th);
		}
	}

	public void refreshResources()
	{
		if (this.pendingResourceReload)
		{
			this.pendingResourceReload = false;
			this.minecraft.refreshResources();
		}
	}
	
	public void onInit()
	{
		if (this.soundManagerReloadInhibitor != null && this.soundManagerReloadInhibitor.isInhibited())
		{
			this.soundManagerReloadInhibitor.unInhibit(true);
		}
	}

	public void onLogin(NetHandler netHandler, Packet1Login loginPacket)
	{
		this.permissionsManager.onLogin(netHandler, loginPacket);
	}

	public void onRender()
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

	public void onTick(float partialTicks, boolean inGame)
	{
		// Tick the permissions manager
		this.permissionsManager.onTick(this.minecraft, partialTicks, inGame);
		
		this.checkAndStoreKeyBindings();
	}
	
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
	protected void storeBindings()
	{
		try
		{
			this.keyMapSettings.store(new FileWriter(this.keyMapSettingsFile), "Mod key mappings for LiteLoader mods, stored here to avoid losing settings stored in options.txt");
		}
		catch (IOException ex) {}
	}
	
	/**
	 * Set the brand in ClientBrandRetriever to the specified brand 
	 * 
	 * @param brand
	 */
	private void setBranding(String brand)
	{
		try
		{
			String oldBrand = ClientBrandRetriever.getClientModName();
			
			if (oldBrand.equals("vanilla"))
			{
				char[] newValue = brand.toCharArray();
				
				Field stringValue = String.class.getDeclaredField("value");
				stringValue.setAccessible(true);
				stringValue.set(oldBrand, newValue);
				
				try
				{
					Field stringCount = String.class.getDeclaredField("count");
					stringCount.setAccessible(true);
					stringCount.set(oldBrand, newValue.length);
				}
				catch (NoSuchFieldException ex) {} // java 1.7 doesn't have this member
			}
		}
		catch (Exception ex)
		{
			LiteLoader.getLogger().log(Level.WARNING, "Setting branding failed", ex);
		}
	}

	private static void logInfo(String string, Object... args)
	{
		LiteLoader.getLogger().info(String.format(string, args));
	}
	
	private static void logWarning(String string, Object... args)
	{
		LiteLoader.getLogger().warning(String.format(string, args));
	}

	public static void populateCrashReport(CrashReport par1CrashReport)
	{
		par1CrashReport.getCategory().addCrashSectionCallable("Mod Pack",       new CallableLiteLoaderBrand(par1CrashReport));
		par1CrashReport.getCategory().addCrashSectionCallable("LiteLoader Mods", new CallableLiteLoaderMods(par1CrashReport));
	}
	
	// -----------------------------------------------------------------------------------------------------------
	// TODO Remove delegates below after 1.6.4
	// -----------------------------------------------------------------------------------------------------------
	
	/**
	 * Delegate to PluginChannels.sendMessage. Deprecated and will be removed
	 * 
	 * @param channel Channel to send data to
	 * @param data Data to send
	 * 
	 * @deprecated User PluginChannels.sendMessage(channel, data) instead.
	 */
	@Deprecated
	public void sendPluginChannelMessage(String channel, byte[] data)
	{
		PluginChannels.sendMessage(channel, data);
	}

	@Deprecated
	public void addTickListener(Tickable tickable)
	{
		this.events.addTickListener(tickable);
	}
	
	@Deprecated
	public void addLoopListener(GameLoopListener loopListener)
	{
		this.events.addLoopListener(loopListener);
	}
	
	@Deprecated
	public void addInitListener(InitCompleteListener initCompleteListener)
	{
		this.events.addInitListener(initCompleteListener);
	}
	
	@Deprecated
	public void addRenderListener(RenderListener renderListener)
	{
		this.events.addRenderListener(renderListener);
	}
	
	@Deprecated
	public void addPostRenderListener(PostRenderListener postRenderListener)
	{
		this.events.addPostRenderListener(postRenderListener);
	}
	
	@Deprecated
	public void addChatFilter(ChatFilter chatFilter)
	{
		this.events.addChatFilter(chatFilter);
	}
	
	@Deprecated
	public void addChatListener(ChatListener chatListener)
	{
		this.events.addChatListener(chatListener);
	}
	
	@Deprecated
	public void addChatRenderListener(ChatRenderListener chatRenderListener)
	{
		this.events.addChatRenderListener(chatRenderListener);
	}
	
	@Deprecated
	public void addPreLoginListener(PreLoginListener loginListener)
	{
		this.events.addPreLoginListener(loginListener);
	}

	@Deprecated
	public void addLoginListener(LoginListener loginListener)
	{
		this.events.addLoginListener(loginListener);
	}
}