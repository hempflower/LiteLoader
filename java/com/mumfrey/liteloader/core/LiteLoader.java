package com.mumfrey.liteloader.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeSet;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javax.activity.InvalidActivityException;

import net.minecraft.src.*;

import com.mumfrey.liteloader.ChatFilter;
import com.mumfrey.liteloader.ChatListener;
import com.mumfrey.liteloader.ChatRenderListener;
import com.mumfrey.liteloader.GameLoopListener;
import com.mumfrey.liteloader.InitCompleteListener;
import com.mumfrey.liteloader.LiteMod;
import com.mumfrey.liteloader.LoginListener;
import com.mumfrey.liteloader.Permissible;
import com.mumfrey.liteloader.PluginChannelListener;
import com.mumfrey.liteloader.PostRenderListener;
import com.mumfrey.liteloader.PreLoginListener;
import com.mumfrey.liteloader.RenderListener;
import com.mumfrey.liteloader.Tickable;
import com.mumfrey.liteloader.gui.GuiControlsPaginated;
import com.mumfrey.liteloader.permissions.PermissionsManagerClient;
import com.mumfrey.liteloader.util.ModUtilities;
import com.mumfrey.liteloader.util.PrivateFields;

/**
 * LiteLoader is a simple loader which loads and provides useful callbacks to
 * lightweight mods
 * 
 * @author Adam Mummery-Smith
 * @version 1.6.2_01
 */
public final class LiteLoader implements FilenameFilter, IPlayerUsage
{
	/**
	 * Liteloader version
	 */
	private static final LiteLoaderVersion VERSION = LiteLoaderVersion.MC_1_6_2_R1;
	
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
	public static Logger logger = Logger.getLogger("liteloader");
	
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
	 * List of mods passed into the command line
	 */
	private static List<String> modNameFilter = null;
	
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
	private Minecraft minecraft = Minecraft.getMinecraft();
	
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
	 * Reference to the minecraft timer
	 */
	private Timer minecraftTimer;
	
	/**
	 * Classes to load, mapped by class name 
	 */
	private Map<String, Class<? extends LiteMod>> modsToLoad = new HashMap<String, Class<? extends LiteMod>>();
	
	/**
	 * Mod metadata from version file 
	 */
	private Map<String, ModFile> modFiles = new HashMap<String, ModFile>();
	
	/**
	 * Registered resource packs 
	 */
	private Map<String, ResourcePack> registeredResourcePacks = new HashMap<String, ResourcePack>();
	
	/**
	 * List of loaded mods, for crash reporting
	 */
	private String loadedModsList = "none";
	
	/**
	 * Global list of mods which we have loaded
	 */
	private LinkedList<LiteMod> mods = new LinkedList<LiteMod>();
	
	/**
	 * List of mods which implement Tickable interface and will receive tick
	 * events
	 */
	private LinkedList<Tickable> tickListeners = new LinkedList<Tickable>();
	
	/**
	 * List of mods which implement the GameLoopListener interface and will
	 * receive loop events
	 */
	private LinkedList<GameLoopListener> loopListeners = new LinkedList<GameLoopListener>();
	
	/**
	 * 
	 */
	private LinkedList<InitCompleteListener> initListeners = new LinkedList<InitCompleteListener>();
	
	/**
	 * List of mods which implement RenderListener interface and will receive
	 * render events events
	 */
	private LinkedList<RenderListener> renderListeners = new LinkedList<RenderListener>();
	
	/**
	 * List of mods which implement the PostRenderListener interface and want to
	 * render entities
	 */
	private LinkedList<PostRenderListener> postRenderListeners = new LinkedList<PostRenderListener>();
	
	/**
	 * List of mods which implement ChatRenderListener and want to know when
	 * chat is rendered
	 */
	private LinkedList<ChatRenderListener> chatRenderListeners = new LinkedList<ChatRenderListener>();
	
	/**
	 * List of mods which implement ChatListener interface and will receive chat
	 * events
	 */
	private LinkedList<ChatListener> chatListeners = new LinkedList<ChatListener>();
	
	/**
	 * List of mods which implement ChatFilter interface and will receive chat
	 * filter events
	 */
	private LinkedList<ChatFilter> chatFilters = new LinkedList<ChatFilter>();
	
	/**
	 * List of mods which implement LoginListener interface and will receive
	 * client login events
	 */
	private LinkedList<LoginListener> loginListeners = new LinkedList<LoginListener>();
	
	/**
	 * List of mods which implement LoginListener interface and will receive
	 * client login events
	 */
	private LinkedList<PreLoginListener> preLoginListeners = new LinkedList<PreLoginListener>();
	
	/**
	 * List of mods which implement PluginChannelListener interface
	 */
	private LinkedList<PluginChannelListener> pluginChannelListeners = new LinkedList<PluginChannelListener>();
	
	/**
	 * Mapping of plugin channel names to listeners
	 */
	private HashMap<String, LinkedList<PluginChannelListener>> pluginChannels = new HashMap<String, LinkedList<PluginChannelListener>>();
	
	/**
	 * Reference to the addUrl method on URLClassLoader
	 */
	private Method mAddUrl;
	
	/**
	 * Flag which keeps track of whether late initialisation has been done
	 */
	private boolean loaderStartupDone, loaderStartupComplete, lateInitDone;
	
	/**
	 * Flags which keep track of whether hooks have been applied
	 */
	private boolean chatHooked, loginHooked, pluginChannelHooked, tickHooked;
	
	/**
	 * Profiler hook objects
	 */
	private HookProfiler profilerHook = new HookProfiler(this, logger);
	
	/**
	 * ScaledResolution used by the pre-chat and post-chat render callbacks
	 */
	private ScaledResolution currentResolution;
	
	/**
	 * Permission Manager
	 */
	private static PermissionsManagerClient permissionsManager = PermissionsManagerClient.getInstance();

	public static final void init(File gameDirectory, File assetsDirectory, String profile, List<String> modNameFilter)
	{
		if (instance == null)
		{
			LiteLoader.gameDirectory = gameDirectory;
			LiteLoader.assetsDirectory = assetsDirectory;
			LiteLoader.profile = profile;
			
			try
			{
				if (modNameFilter != null)
				{
					LiteLoader.modNameFilter = new ArrayList<String>();
					for (String filterEntry : modNameFilter)
					{
						LiteLoader.modNameFilter.add(filterEntry.toLowerCase().trim());
					}
				}
			}
			catch (Exception ex)
			{
				LiteLoader.modNameFilter = null;
			}
			
			instance = new LiteLoader();
			instance.initLoader();
		}
		
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
		return instance;
	}
	
	/**
	 * Get the LiteLoader logger object
	 * 
	 * @return
	 */
	public static final Logger getLogger()
	{
		return logger;
	}
	
	/**
	 * Get the output stream which we are using for console output
	 * 
	 * @return
	 */
	public static final PrintStream getConsoleStream()
	{
		return useStdOut ? System.out : System.err;
	}
	
	/**
	 * Get LiteLoader version
	 * 
	 * @return
	 */
	public static final String getVersion()
	{
		return VERSION.getLoaderVersion();
	}
	
	/**
	 * Get the loader revision
	 * 
	 * @return
	 */
	public static final int getRevision()
	{
		return VERSION.getLoaderRevision();
	}
	
	public static final PermissionsManagerClient getPermissionsManager()
	{
		return permissionsManager;
	}
	
	/**
	 * LiteLoader constructor
	 */
	private LiteLoader()
	{
		this.initPaths();
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
		
		return new File(this.configBaseFolder, String.format("config.%s", version.getLoaderVersion()));
	}
	
	/**
	 * Loader initialisation
	 */
	private void initLoader()
	{
		if (this.loaderStartupDone) return;
		this.loaderStartupDone = true;
		
		// Set up loader, initialises any reflection methods needed
		if (this.prepareLoader())
		{
			logger.info(String.format("LiteLoader %s starting up...", VERSION));
			
			// Print the branding version if any was provided
			if (this.branding != null)
			{
				logger.info(String.format("Active Pack: %s", this.branding));
			}
			
			logger.info(String.format("Java reports OS=\"%s\"", System.getProperty("os.name").toLowerCase()));
			
			boolean searchMods = this.localProperties.getProperty("search.mods", "true").equalsIgnoreCase("true");
			boolean searchProtectionDomain = this.localProperties.getProperty("search.jar", "true").equalsIgnoreCase("true");
			boolean searchClassPath = this.localProperties.getProperty("search.classpath", "true").equalsIgnoreCase("true");
			
			if (!searchMods && !searchProtectionDomain && !searchClassPath)
			{
				logger.warning("Invalid configuration, no search locations defined. Enabling all search locations.");
				
				this.localProperties.setProperty("search.mods", "true");
				this.localProperties.setProperty("search.jar", "true");
				this.localProperties.setProperty("search.classpath", "true");
				
				searchMods = true;
				searchProtectionDomain = true;
				searchClassPath = true;
			}
			
			// Examines the class path and mods folder and locates loadable mods
			this.prepareMods(searchMods, searchProtectionDomain, searchClassPath);
			
			// Initialises enumerated mods
			this.initMods();
			
			// Initialises the required hooks for loaded mods
			this.initHooks();
			
			this.loaderStartupComplete = true;
			
			this.writeProperties();
		}
	}
	
	/**
	 * Set up reflection methods required by the loader
	 */
	private boolean prepareLoader()
	{
		try
		{
			// addURL method is used by the class loader to
			this.mAddUrl = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
			this.mAddUrl.setAccessible(true);
			
			// Prepare the properties
			this.prepareProperties();
			
			// Prepare the log writer
			this.prepareLogger();
			
			this.paginateControls = this.localProperties.getProperty("controls.pages", "true").equalsIgnoreCase("true");
			this.localProperties.setProperty("controls.pages", String.valueOf(this.paginateControls));
			
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
			logger.log(Level.SEVERE, "Error initialising LiteLoader", th);
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
		
		logger.setUseParentHandlers(false);
		this.useStdOut = System.getProperty("liteloader.log", "stderr").equalsIgnoreCase("stdout") || this.localProperties.getProperty("log", "stderr").equalsIgnoreCase("stdout");
		
		StreamHandler consoleHandler = useStdOut ? new com.mumfrey.liteloader.util.log.ConsoleHandler() : new java.util.logging.ConsoleHandler();
		consoleHandler.setFormatter(logFormatter);
		logger.addHandler(consoleHandler);
		
		FileHandler logFileHandler = new FileHandler(new File(this.configBaseFolder, "LiteLoader.txt").getAbsolutePath());
		logFileHandler.setFormatter(logFormatter);
		logger.addHandler(logFileHandler);
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
			logger.log(Level.WARNING, "Error writing liteloader properties", th);
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
		if (!this.registeredResourcePacks.containsKey(resourcePack.func_130077_b())) // TODO adamsrc -> getName()
		{
			List<ResourcePack> defaultResourcePacks = PrivateFields.defaultResourcePacks.get(this.minecraft);
			if (!defaultResourcePacks.contains(resourcePack))
			{
				defaultResourcePacks.add(resourcePack);
				this.registeredResourcePacks.put(resourcePack.func_130077_b(), resourcePack); // TODO adamsrc -> getName()
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
			List<ResourcePack> defaultResourcePacks = PrivateFields.defaultResourcePacks.get(this.minecraft);
			this.registeredResourcePacks.remove(resourcePack.func_130077_b()); // TODO adamsrc -> getName()
			defaultResourcePacks.remove(resourcePack);
			return true;
		}
		
		return false;
	}
	
	/**
	 * Get the "mods" folder
	 */
	public File getModsFolder()
	{
		return this.modsFolder;
	}
	
	/**
	 * Get the common (version-independent) config folder
	 */
	public File getCommonConfigFolder()
	{
		return this.commonConfigFolder;
	}
	
	/**
	 * Get the config folder for this version
	 */
	public File getConfigFolder()
	{
		return this.versionConfigFolder;
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
	 * Used for crash reporting
	 * 
	 * @return List of loaded mods as a string
	 */
	public String getLoadedModsList()
	{
		return this.loadedModsList;
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
		if (!this.loaderStartupComplete)
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
	 * Get whether the specified mod is installed
	 *
	 * @param modName
	 * @return
	 */
	public boolean isModInstalled(String modName)
	{
		if (!this.loaderStartupComplete || modName == null) return false;
		
		for (LiteMod mod : this.mods)
		{
			if (modName.equalsIgnoreCase(mod.getName()) || modName.equalsIgnoreCase(mod.getClass().getSimpleName())) return true;
		}
		
		return true;
	}

	/**
	 * Get metadata for the specified mod, attempts to retrieve the mod by name first
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
		
		String modClassName = mod.getClass().getSimpleName();
		if (!this.modFiles.containsKey(modClassName)) return defaultValue;
		
		ModFile modFile = this.modFiles.get(modClassName);
		return modFile.getMetaValue(metaDataKey, defaultValue);
	}
	
	/**
	 * @param mod
	 * @return
	 */
	private ModFile getModFile(LiteMod mod)
	{
		String modClassName = mod.getClass().getSimpleName();
		return this.modFiles.containsKey(modClassName) ? this.modFiles.get(modClassName) : null;
	}
	
	/**
	 * Enumerate the java class path and "mods" folder to find mod classes, then
	 * load the classes
	 */
	private void prepareMods(boolean searchMods, boolean searchProtectionDomain, boolean searchClassPath)
	{
		// List of mod files in the "mods" folder
		List<ModFile> modFiles = new LinkedList<ModFile>();
		
		if (searchMods)
		{
			// Find and enumerate the "mods" folder
			File modFolder = this.getModsFolder();
			if (modFolder.exists() && modFolder.isDirectory())
			{
				logger.info("Mods folder found, searching " + modFolder.getPath());
				this.findModFiles(modFolder, modFiles);
				logger.info("Found " + modFiles.size() + " mod file(s)");
			}
		}
		
		try
		{
			logger.info("Enumerating class path...");
			
			String classPath = System.getProperty("java.class.path");
			String classPathSeparator = System.getProperty("path.separator");
			String[] classPathEntries = classPath.split(classPathSeparator);
			
			logger.info(String.format("Class path separator=\"%s\"", classPathSeparator));
			logger.info(String.format("Class path entries=(\n   classpathEntry=%s\n)", classPath.replace(classPathSeparator, "\n   classpathEntry=")));
			
			if (searchProtectionDomain || searchClassPath)
				logger.info("Discovering mods on class path...");
			
			this.findModClasses(classPathEntries, modFiles, searchProtectionDomain, searchClassPath);
			
			logger.info("Mod class discovery completed");
		}
		catch (Throwable th)
		{
			logger.log(Level.WARNING, "Mod class discovery failed", th);
			return;
		}
		
		this.loadMods();
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
				
				if (version == null)
				{
					version = modZip.getEntry("version.txt");
				}
				
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
						logger.warning("Error reading version data from " + modFile.getName());
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
								if (!modFileInfo.isJson())
								{
									logger.warning("Missing or invalid litemod.json reading mod file: " + modFile.getAbsolutePath());
								}
								
								if (!versionOrderingSets.containsKey(modFileInfo.getName()))
								{
									versionOrderingSets.put(modFileInfo.getName(), new TreeSet<ModFile>());
								}
								
								versionOrderingSets.get(modFileInfo.getName()).add(modFileInfo);
							}
							else
							{
								logger.info("Not adding invalid or outdated mod file: " + modFile.getAbsolutePath());
							}
						}
					}
				}
				
				modZip.close();
			}
			catch (Exception ex)
			{
				ex.printStackTrace(System.err);
				logger.warning("Error enumerating '" + modFile.getAbsolutePath() + "': Invalid zip file or error reading file");
			}
		}

		// Copy the first entry in every version set into the modfiles list
		for (Entry<String, TreeSet<ModFile>> modFileEntry : versionOrderingSets.entrySet())
		{
			ModFile newestVersion = modFileEntry.getValue().iterator().next();

			try
			{
				if (this.addURLToClassPath(newestVersion.toURI().toURL()))
				{
					modFiles.add(newestVersion);
				}
			}
			catch (Exception ex)
			{
				logger.warning("Error injecting '" + newestVersion.getAbsolutePath() + "' into classPath. The mod will not be loaded");
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
		return fileName.toLowerCase().endsWith(".litemod");
	}
	
	/**
	 * Find mod classes in the class path and enumerated mod files list
	 * 
	 * @param classPathEntries Java class path split into string entries
	 */
	private void findModClasses(String[] classPathEntries, List<ModFile> modFiles, boolean searchProtectionDomain, boolean searchClassPath)
	{
		if (searchProtectionDomain)
		{
			try
			{
				this.searchProtectionDomain();
			}
			catch (Throwable th)
			{
				logger.warning("Error loading from local class path: " + th.getMessage());
			}
		}
		
		if (searchClassPath)
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
		logger.info("Searching protection domain code source...");
		
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
			LinkedList<Class<?>> modClasses = getSubclassesFor(packagePath, Minecraft.class.getClassLoader(), LiteMod.class, "LiteMod");
			
			for (Class<?> mod : modClasses)
			{
				if (this.modsToLoad.containsKey(mod.getSimpleName()))
				{
					logger.warning("Mod name collision for mod with class '" + mod.getSimpleName() + "', maybe you have more than one copy?");
				}
				
				this.modsToLoad.put(mod.getSimpleName(), (Class<? extends LiteMod>)mod);
			}
			
			if (modClasses.size() > 0)
				logger.info(String.format("Found %s potential matches", modClasses.size()));
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
			logger.info(String.format("Searching %s...", classPathPart));
			
			File packagePath = new File(classPathPart);
			LinkedList<Class<?>> modClasses = getSubclassesFor(packagePath, Minecraft.class.getClassLoader(), LiteMod.class, "LiteMod");
			
			for (Class<?> mod : modClasses)
			{
				if (this.modsToLoad.containsKey(mod.getSimpleName()))
				{
					logger.warning("Mod name collision for mod with class '" + mod.getSimpleName() + "', maybe you have more than one copy?");
				}
				
				this.modsToLoad.put(mod.getSimpleName(), (Class<? extends LiteMod>)mod);
				this.modFiles.put(mod.getSimpleName(), new ClassPathMod(packagePath, mod.getSimpleName().substring(7), LiteLoader.getVersion()));
			}
			
			if (modClasses.size() > 0)
				logger.info(String.format("Found %s potential matches", modClasses.size()));
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
			logger.info(String.format("Searching %s...", modFile.getAbsolutePath()));
			
			LinkedList<Class<?>> modClasses = getSubclassesFor(modFile, Minecraft.class.getClassLoader(), LiteMod.class, "LiteMod");
			
			for (Class<?> mod : modClasses)
			{
				if (this.modsToLoad.containsKey(mod.getSimpleName()))
				{
					logger.warning("Mod name collision for mod with class '" + mod.getSimpleName() + "', maybe you have more than one copy?");
				}
				
				this.modsToLoad.put(mod.getSimpleName(), (Class<? extends LiteMod>)mod);
				this.modFiles.put(mod.getSimpleName(), modFile);
			}
			
			if (modClasses.size() > 0)
				logger.info(String.format("Found %s potential matches", modClasses.size()));
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
			logger.info("Mod class discovery failed. Not loading any mods!");
			return;
		}
		
		logger.info("Discovered " + this.modsToLoad.size() + " total mod(s)");
		
		boolean addedResources = false;
		
		for (Class<? extends LiteMod> mod : this.modsToLoad.values())
		{
			try
			{
				logger.info("Loading mod from " + mod.getName());
				
				LiteMod newMod = mod.newInstance();
				
				if (this.shouldAddMod(newMod))
				{
					this.mods.add(newMod);
					logger.info("Successfully added mod " + newMod.getName() + " version " + newMod.getVersion());
					
					// Get the mod file and register it as a resource pack if it exists
					ModFile modFile = this.getModFile(newMod);
					if (modFile != null && modFile.registerAsResourcePack(newMod.getName()))
					{
						logger.info("Adding " + modFile.getAbsolutePath() + " to resources list");
						addedResources = true;
					}
				}
				else
				{
					logger.info("Not loading mod " + newMod.getName() + ", excluded by filter");
				}
			}
			catch (Throwable th)
			{
				logger.warning(th.toString());
				th.printStackTrace();
			}
		}
		
		if (addedResources)
		{
			this.minecraft.func_110436_a(); // TODO adamsrc -> refreshResourcePacks
		}
	}
	
	/**
	 * @param name
	 * @return
	 */
	private boolean shouldAddMod(LiteMod mod)
	{
		if (this.modNameFilter == null) return true;
		
		String modClassName = mod.getClass().getSimpleName();
		if (!this.modFiles.containsKey(modClassName)) return true;

		String metaName = this.modFiles.get(modClassName).getModName().toLowerCase();
		if (this.modNameFilter.contains(metaName))
		{
			return true;
		}
		
		return false;
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
				logger.info("Initialising mod " + modName + " version " + mod.getVersion());
				
				try
				{
					String modKey = this.getModNameForConfig(mod.getClass(), modName);
					LiteLoaderVersion lastModVersion = LiteLoaderVersion.getVersionFromRevision(this.getLastKnownModRevision(modKey));
					
					if (LiteLoader.VERSION.getLoaderRevision() > lastModVersion.getLoaderRevision())
					{
						logger.info("Performing config upgrade for mod " + modName + ". Upgrading " + lastModVersion + " to " + LiteLoader.VERSION + "...");
						mod.upgradeSettings(LiteLoader.getVersion(), this.versionConfigFolder, this.inflectVersionedConfigPath(lastModVersion));
						
						this.storeLastKnownModRevision(modKey);
						logger.info("Config upgrade succeeded for mod " + modName);
					}
				}
				catch (Throwable th)
				{
					logger.warning("Error performing settings upgrade for " + modName + ". Settings may not be properly migrated");
				}
				
				mod.init(this.modsFolder);
				
				if (mod instanceof Tickable)
				{
					this.addTickListener((Tickable)mod);
				}
				
				if (mod instanceof GameLoopListener)
				{
					this.addLoopListener((GameLoopListener)mod);
				}
				
				if (mod instanceof InitCompleteListener)
				{
					this.addInitListener((InitCompleteListener)mod);
				}
				
				if (mod instanceof RenderListener)
				{
					this.addRenderListener((RenderListener)mod);
				}
				
				if (mod instanceof PostRenderListener)
				{
					this.addPostRenderListener((PostRenderListener)mod);
				}
				
				if (mod instanceof ChatFilter)
				{
					this.addChatFilter((ChatFilter)mod);
				}
				
				if (mod instanceof ChatListener)
				{
					if (mod instanceof ChatFilter)
					{
						this.logger.warning(String.format("Interface error initialising mod '%1s'. A mod implementing ChatFilter and ChatListener is not supported! Remove one of these interfaces", modName));
					}
					else
					{
						this.addChatListener((ChatListener)mod);
					}
				}
				
				if (mod instanceof ChatRenderListener)
				{
					this.addChatRenderListener((ChatRenderListener)mod);
				}
				
				if (mod instanceof PreLoginListener)
				{
					this.addPreLoginListener((PreLoginListener)mod);
				}
				
				if (mod instanceof LoginListener)
				{
					this.addLoginListener((LoginListener)mod);
				}
				
				if (mod instanceof PluginChannelListener)
				{
					this.addPluginChannelListener((PluginChannelListener)mod);
				}
				
				if (mod instanceof Permissible)
				{
					permissionsManager.registerPermissible((Permissible)mod);
				}
				
				this.loadedModsList += String.format("\n          - %s version %s", modName, mod.getVersion());
				loadedModsCount++;
			}
			catch (Throwable th)
			{
				logger.log(Level.WARNING, "Error initialising mod '" + modName, th);
				iter.remove();
			}
		}
		
		this.loadedModsList = String.format("%s loaded mod(s)%s", loadedModsCount, this.loadedModsList);
	}
	
	/**
	 * Initialise mod hooks
	 */
	private void initHooks()
	{
		try
		{
			// Chat hook
			if ((this.chatListeners.size() > 0 || this.chatFilters.size() > 0) && !this.chatHooked)
			{
				this.chatHooked = true;
				HookChat.register();
				HookChat.registerPacketHandler(this);
			}
			
			// Login hook
			if ((this.preLoginListeners.size() > 0 || this.loginListeners.size() > 0) && !this.loginHooked)
			{
				this.loginHooked = true;
				ModUtilities.registerPacketOverride(1, HookLogin.class);
				HookLogin.loader = this;
			}
			
			// Plugin channels hook
			if (this.pluginChannelListeners.size() > 0 && !this.pluginChannelHooked)
			{
				this.pluginChannelHooked = true;
				HookPluginChannels.register();
				HookPluginChannels.registerPacketHandler(this);
			}
			
			// Tick hook
			if (!this.tickHooked)
			{
				this.tickHooked = true;
				PrivateFields.minecraftProfiler.setFinal(this.minecraft, this.profilerHook);
			}
			
			// Sanity hook
			PlayerUsageSnooper snooper = this.minecraft.getPlayerUsageSnooper();
			PrivateFields.playerStatsCollector.setFinal(snooper, this);
		}
		catch (Exception ex)
		{
			logger.log(Level.WARNING, "Error creating hooks", ex);
			ex.printStackTrace();
		}
	}
	
	/**
	 * @param tickable
	 */
	public void addTickListener(Tickable tickable)
	{
		if (!this.tickListeners.contains(tickable))
		{
			this.tickListeners.add(tickable);
			if (this.loaderStartupComplete)
				this.initHooks();
		}
	}
	
	/**
	 * @param loopListener
	 */
	public void addLoopListener(GameLoopListener loopListener)
	{
		if (!this.loopListeners.contains(loopListener))
		{
			this.loopListeners.add(loopListener);
			if (this.loaderStartupComplete)
				this.initHooks();
		}
	}
	
	/**
	 * @param initCompleteListener
	 */
	public void addInitListener(InitCompleteListener initCompleteListener)
	{
		if (!this.initListeners.contains(initCompleteListener))
		{
			this.initListeners.add(initCompleteListener);
			if (this.loaderStartupComplete)
				this.initHooks();
		}
	}
	
	/**
	 * @param tickable
	 */
	public void addRenderListener(RenderListener tickable)
	{
		if (!this.renderListeners.contains(tickable))
		{
			this.renderListeners.add(tickable);
			if (this.loaderStartupComplete)
				this.initHooks();
		}
	}
	
	/**
	 * @param tickable
	 */
	public void addPostRenderListener(PostRenderListener tickable)
	{
		if (!this.postRenderListeners.contains(tickable))
		{
			this.postRenderListeners.add(tickable);
			if (this.loaderStartupComplete)
				this.initHooks();
		}
	}
	
	/**
	 * @param chatFilter
	 */
	public void addChatFilter(ChatFilter chatFilter)
	{
		if (!this.chatFilters.contains(chatFilter))
		{
			this.chatFilters.add(chatFilter);
			if (this.loaderStartupComplete)
				this.initHooks();
		}
	}
	
	/**
	 * @param chatListener
	 */
	public void addChatListener(ChatListener chatListener)
	{
		if (!this.chatListeners.contains(chatListener))
		{
			this.chatListeners.add(chatListener);
			if (this.loaderStartupComplete)
				this.initHooks();
		}
	}
	
	/**
	 * @param chatRenderListener
	 */
	public void addChatRenderListener(ChatRenderListener chatRenderListener)
	{
		if (!this.chatRenderListeners.contains(chatRenderListener))
		{
			this.chatRenderListeners.add(chatRenderListener);
			if (this.loaderStartupComplete)
				this.initHooks();
		}
	}
	
	/**
	 * @param loginListener
	 */
	public void addPreLoginListener(PreLoginListener loginListener)
	{
		if (!this.preLoginListeners.contains(loginListener))
		{
			this.preLoginListeners.add(loginListener);
			if (this.loaderStartupComplete)
				this.initHooks();
		}
	}
	
	/**
	 * @param loginListener
	 */
	public void addLoginListener(LoginListener loginListener)
	{
		if (!this.loginListeners.contains(loginListener))
		{
			this.loginListeners.add(loginListener);
			if (this.loaderStartupComplete)
				this.initHooks();
		}
	}
	
	/**
	 * @param pluginChannelListener
	 */
	public void addPluginChannelListener(PluginChannelListener pluginChannelListener)
	{
		if (!this.pluginChannelListeners.contains(pluginChannelListener))
		{
			this.pluginChannelListeners.add(pluginChannelListener);
			if (this.loaderStartupComplete)
				this.initHooks();
		}
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
			logger.log(Level.WARNING, "Enumeration error", th);
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
			logger.log(Level.WARNING, "checkAndAddClass error", th);
		}
	}
	
	/**
	 * Add a URL to the Minecraft classloader class path
	 * 
	 * @param classUrl URL of the resource to add
	 */
	private boolean addURLToClassPath(URL classUrl)
	{
		try
		{
			if (Minecraft.class.getClassLoader() instanceof URLClassLoader && this.mAddUrl != null && this.mAddUrl.isAccessible())
			{
				URLClassLoader classLoader = (URLClassLoader)Minecraft.class.getClassLoader();
				this.mAddUrl.invoke(classLoader, classUrl);
				return true;
			}
		}
		catch (Throwable th)
		{
			logger.log(Level.WARNING, "Error adding class path entry", th);
		}
		
		return false;
	}
	
	/**
	 * Late initialisation callback
	 */
	public void onInit()
	{
		if (!this.lateInitDone)
		{
			this.lateInitDone = true;
			
			for (InitCompleteListener initMod : this.initListeners)
			{
				try
				{
					logger.info("Calling late init for mod " + initMod.getName());
					initMod.onInitCompleted(this.minecraft, this);
				}
				catch (Throwable th)
				{
					logger.log(Level.WARNING, "Error initialising mod " + initMod.getName(), th);
				}
			}
		}
	}
	
	/**
	 * Callback from the tick hook, pre render
	 */
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
		
		for (RenderListener renderListener : this.renderListeners)
			renderListener.onRender();
	}
	
	/**
	 * Callback from the tick hook, post render entities
	 */
	public void postRenderEntities()
	{
		float partialTicks = (this.minecraftTimer != null) ? this.minecraftTimer.elapsedPartialTicks : 0.0F;
		
		for (PostRenderListener renderListener : this.postRenderListeners)
			renderListener.onPostRenderEntities(partialTicks);
	}
	
	/**
	 * Callback from the tick hook, post render
	 */
	public void postRender()
	{
		float partialTicks = (this.minecraftTimer != null) ? this.minecraftTimer.elapsedPartialTicks : 0.0F;
		
		for (PostRenderListener renderListener : this.postRenderListeners)
			renderListener.onPostRender(partialTicks);
	}
	
	/**
	 * Called immediately before the current GUI is rendered
	 */
	public void onBeforeGuiRender()
	{
		for (RenderListener renderListener : this.renderListeners)
			renderListener.onRenderGui(this.minecraft.currentScreen);
	}
	
	/**
	 * Called immediately after the world/camera transform is initialised
	 */
	public void onSetupCameraTransform()
	{
		for (RenderListener renderListener : this.renderListeners)
			renderListener.onSetupCameraTransform();
	}
	
	/**
	 * Called immediately before the chat log is rendered
	 */
	public void onBeforeChatRender()
	{
		this.currentResolution = new ScaledResolution(this.minecraft.gameSettings, this.minecraft.displayWidth, this.minecraft.displayHeight);
		int screenWidth = this.currentResolution.getScaledWidth();
		int screenHeight = this.currentResolution.getScaledHeight();
		
		GuiNewChat chat = this.minecraft.ingameGUI.getChatGUI();
		
		for (ChatRenderListener chatRenderListener : this.chatRenderListeners)
			chatRenderListener.onPreRenderChat(screenWidth, screenHeight, chat);
	}
	
	/**
	 * Called immediately after the chat log is rendered
	 */
	public void onAfterChatRender()
	{
		int screenWidth = this.currentResolution.getScaledWidth();
		int screenHeight = this.currentResolution.getScaledHeight();
		
		GuiNewChat chat = this.minecraft.ingameGUI.getChatGUI();
		
		for (ChatRenderListener chatRenderListener : this.chatRenderListeners)
			chatRenderListener.onPostRenderChat(screenWidth, screenHeight, chat);
	}
	
	/**
	 * Callback from the tick hook, called every frame when the timer is updated
	 */
	public void onTimerUpdate()
	{
		for (GameLoopListener loopListener : this.loopListeners)
			loopListener.onRunGameLoop(this.minecraft);
	}
	
	/**
	 * Callback from the tick hook, ticks all tickable mods
	 * 
	 * @param tick True if this is a new tick (otherwise it's just a new frame)
	 */
	public void onTick(Profiler profiler, boolean tick)
	{
		float partialTicks = 0.0F;
		
		// Try to get the minecraft timer object and determine the value of the
		// partialTicks
		if (tick || this.minecraftTimer == null)
		{
			this.minecraftTimer = PrivateFields.minecraftTimer.get(this.minecraft);
		}
		
		// Hooray, we got the timer reference
		if (this.minecraftTimer != null)
		{
			partialTicks = this.minecraftTimer.renderPartialTicks;
			tick = this.minecraftTimer.elapsedTicks > 0;
		}
		
		// Flag indicates whether we are in game at the moment
		boolean inGame = this.minecraft.renderViewEntity != null && this.minecraft.renderViewEntity.worldObj != null;
		
		// Tick the permissions manager
		if (tick)
			permissionsManager.onTick(this.minecraft, partialTicks, inGame);
		
		// Iterate tickable mods
		for (Tickable tickable : this.tickListeners)
		{
			profiler.startSection(tickable.getClass().getSimpleName());
			tickable.onTick(this.minecraft, partialTicks, inGame, tick);
			profiler.endSection();
		}
	}
	
	/**
	 * Callback from the chat hook
	 * 
	 * @param chatPacket
	 * @return
	 */
	public boolean onChat(Packet3Chat chatPacket)
	{
		if (chatPacket.message == null)
			return true;
		
		ChatMessageComponent chat = ChatMessageComponent.func_111078_c(chatPacket.message);
		String message = chat.func_111068_a(true);
		
		// Chat filters get a stab at the chat first, if any filter returns
		// false the chat is discarded
		for (ChatFilter chatFilter : this.chatFilters)
		{
			if (chatFilter.onChat(chatPacket, chat, message))
			{
				chat = ChatMessageComponent.func_111078_c(chatPacket.message);
				message = chat.func_111068_a(true);
			}
			else
			{
				return false;
			}
		}
		
		// Chat listeners get the chat if no filter removed it
		for (ChatListener chatListener : this.chatListeners)
			chatListener.onChat(chat, message);
		
		return true;
	}
	
	/**
	 * Pre-login callback from the login hook
	 * 
	 * @param netHandler
	 * @param hookLogin
	 * @return
	 */
	public boolean onPreLogin(NetHandler netHandler, Packet1Login loginPacket)
	{
		boolean cancelled = false;
		
		for (PreLoginListener loginListener : this.preLoginListeners)
		{
			cancelled |= !loginListener.onPreLogin(netHandler, loginPacket);
		}
		
		return !cancelled;
	}
	
	/**
	 * Callback from the login hook
	 * 
	 * @param netHandler
	 * @param loginPacket
	 */
	public void onConnectToServer(NetHandler netHandler, Packet1Login loginPacket)
	{
		permissionsManager.onLogin(netHandler, loginPacket);
		
		for (LoginListener loginListener : this.loginListeners)
			loginListener.onLogin(netHandler, loginPacket);
		
		this.setupPluginChannels();
	}
	
	/**
	 * Callback for the plugin channel hook
	 * 
	 * @param hookPluginChannels
	 */
	public void onPluginChannelMessage(HookPluginChannels hookPluginChannels)
	{
		if (hookPluginChannels != null && hookPluginChannels.channel != null && this.pluginChannels.containsKey(hookPluginChannels.channel))
		{
			try
			{
				permissionsManager.onCustomPayload(hookPluginChannels.channel, hookPluginChannels.length, hookPluginChannels.data);
			}
			catch (Exception ex)
			{
			}
			
			for (PluginChannelListener pluginChannelListener : this.pluginChannels.get(hookPluginChannels.channel))
			{
				try
				{
					pluginChannelListener.onCustomPayload(hookPluginChannels.channel, hookPluginChannels.length, hookPluginChannels.data);
				}
				catch (Exception ex)
				{
				}
			}
		}
	}
	
	/**
	 * Delegate to ModUtilities.sendPluginChannelMessage
	 * 
	 * @param channel Channel to send data to
	 * @param data Data to send
	 */
	public void sendPluginChannelMessage(String channel, byte[] data)
	{
		ModUtilities.sendPluginChannelMessage(channel, data);
	}
	
	/**
	 * Query loaded mods for registered channels
	 */
	protected void setupPluginChannels()
	{
		// Clear any channels from before
		this.pluginChannels.clear();
		
		// Add the permissions manager channels
		this.addPluginChannelsFor(permissionsManager);
		
		// Enumerate mods for plugin channels
		for (PluginChannelListener pluginChannelListener : this.pluginChannelListeners)
		{
			this.addPluginChannelsFor(pluginChannelListener);
		}
		
		// If any mods have registered channels, send the REGISTER packet
		if (this.pluginChannels.keySet().size() > 0)
		{
			StringBuilder channelList = new StringBuilder();
			boolean separator = false;
			
			for (String channel : this.pluginChannels.keySet())
			{
				if (separator)
					channelList.append("\u0000");
				channelList.append(channel);
				separator = true;
			}
			
			byte[] registrationData = channelList.toString().getBytes(Charset.forName("UTF8"));
			
			this.sendPluginChannelMessage("REGISTER", registrationData);
		}
	}
	
	/**
	 * Adds plugin channels for the specified listener to the local channels
	 * collection
	 * 
	 * @param pluginChannelListener
	 */
	private void addPluginChannelsFor(PluginChannelListener pluginChannelListener)
	{
		List<String> channels = pluginChannelListener.getChannels();
		
		if (channels != null)
		{
			for (String channel : channels)
			{
				if (channel.length() > 16 || channel.toUpperCase().equals("REGISTER") || channel.toUpperCase().equals("UNREGISTER"))
					continue;
				
				if (!this.pluginChannels.containsKey(channel))
				{
					this.pluginChannels.put(channel, new LinkedList<PluginChannelListener>());
				}
				
				this.pluginChannels.get(channel).add(pluginChannelListener);
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.minecraft.src.IPlayerUsage#addServerStatsToSnooper(net.minecraft.
	 * src.PlayerUsageSnooper)
	 */
	@Override
	public void addServerStatsToSnooper(PlayerUsageSnooper var1)
	{
		this.minecraft.addServerStatsToSnooper(var1);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.minecraft.src.IPlayerUsage#addServerTypeToSnooper(net.minecraft.src
	 * .PlayerUsageSnooper)
	 */
	@Override
	public void addServerTypeToSnooper(PlayerUsageSnooper var1)
	{
		this.sanityCheck();
		this.minecraft.addServerTypeToSnooper(var1);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see net.minecraft.src.IPlayerUsage#isSnooperEnabled()
	 */
	@Override
	public boolean isSnooperEnabled()
	{
		return this.minecraft.isSnooperEnabled();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see net.minecraft.src.IPlayerUsage#getLogAgent()
	 */
	@Override
	public ILogAgent getLogAgent()
	{
		return this.minecraft.getLogAgent();
	}
	
	/**
	 * Check that the profiler hook hasn't been overridden by something else
	 */
	private void sanityCheck()
	{
		if (this.tickHooked && this.minecraft.mcProfiler != this.profilerHook)
		{
			PrivateFields.minecraftProfiler.setFinal(this.minecraft, this.profilerHook);
		}
	}
}