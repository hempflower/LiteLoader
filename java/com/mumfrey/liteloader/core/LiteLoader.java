package com.mumfrey.liteloader.core;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import net.minecraft.client.Minecraft;
import net.minecraft.src.ConsoleLogManager;
import net.minecraft.src.NetHandler;
import net.minecraft.src.Packet1Login;
import net.minecraft.src.Packet3Chat;
import net.minecraft.src.Profiler;
import net.minecraft.src.Timer;

import com.mumfrey.liteloader.ChatFilter;
import com.mumfrey.liteloader.ChatListener;
import com.mumfrey.liteloader.InitCompleteListener;
import com.mumfrey.liteloader.LiteMod;
import com.mumfrey.liteloader.LoginListener;
import com.mumfrey.liteloader.PluginChannelListener;
import com.mumfrey.liteloader.PostRenderListener;
import com.mumfrey.liteloader.PreLoginListener;
import com.mumfrey.liteloader.RenderListener;
import com.mumfrey.liteloader.Tickable;
import com.mumfrey.liteloader.util.ModUtilities;
import com.mumfrey.liteloader.util.PrivateFields;

/**
 * LiteLoader is a simple loader which provides tick events to loaded mods 
 *
 * @author Adam Mummery-Smith
 * @version 1.4.7
 */
@SuppressWarnings("rawtypes")
public final class LiteLoader implements FilenameFilter
{
	/**
	 * Liteloader version 
	 */
	private static final String LOADER_VERSION = "1.4.7";
	
	/**
	 * Loader revision, can be used by mods to determine whether the loader is sufficiently up-to-date 
	 */
	private static final int LOADER_REVISION = 7;
	
	/**
	 * Minecraft versions that we will load mods for, this will be compared
	 * against the version.txt value in mod files to prevent outdated mods being
	 * loaded!!!
	 */
	private static final String[] SUPPORTED_VERSIONS = { "1.4.6", "1.4.7" };
	
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
	 * "mods" folder which contains mods and config files
	 */
	private File modsFolder;
	
	/**
	 * Reference to the Minecraft game instance
	 */
	private Minecraft minecraft = Minecraft.getMinecraft();
	
	/**
	 * File containing the properties
	 */
	private File propertiesFile = new File(Minecraft.getMinecraftDir(), "liteloader.properties");

	/**
	 * Internal properties loaded from inside the jar
	 */
	private Properties internalProperties = new Properties();

	/**
	 * LiteLoader properties 
	 */
	private Properties localProperties = new Properties();
	
	/**
	 * Pack brand from properties, used to put the modpack/compilation name in crash reports
	 */
	private String branding = null;
	
	/**
	 * Reference to the minecraft timer
	 */
	private Timer minecraftTimer;

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
	 * 
	 */
	private LinkedList<InitCompleteListener> initListeners = new LinkedList<InitCompleteListener>();
	
	/**
	 * List of mods which implement RenderListener interface and will receive render events
	 * events
	 */
	private LinkedList<RenderListener> renderListeners = new LinkedList<RenderListener>();
	
	/**
	 * List of mods which implement the PostRenderListener interface and want to render entities
	 */
	private LinkedList<PostRenderListener> postRenderListeners = new LinkedList<PostRenderListener>();
	
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
	 * List of mods which implement LoginListener interface and will receive client login events
	 */
	private LinkedList<LoginListener> loginListeners = new LinkedList<LoginListener>();
	
	/**
	 * List of mods which implement LoginListener interface and will receive client login events
	 */
	private LinkedList<PreLoginListener> preLoginListeners = new LinkedList<PreLoginListener>();
	
	/**
	 * List of mods which implement PluginChannelListener interface
	 */
	private LinkedList<PluginChannelListener> pluginChannelListeners = new LinkedList<PluginChannelListener>();

	/**
	 * Mapping of plugin channel names to listeners 
	 */
	private HashMap<String,LinkedList<PluginChannelListener>> pluginChannels = new HashMap<String, LinkedList<PluginChannelListener>>();
	
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
	 * Get the singleton instance of LiteLoader, initialises the loader if necessary
	 * 
	 * @return LiteLoader instance
	 */
	public static final LiteLoader getInstance()
	{
		if (instance == null)
		{
			// Return immediately to stop calls to getInstance causing re-init if they arrive
			// before init is completed
			instance = new LiteLoader();
			instance.initLoader();
		}
		
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
		return LOADER_VERSION;
	}
	
	/**
	 * Get the loader revision
	 * 
	 * @return
	 */
	public static final int getRevision()
	{
		return LOADER_REVISION;
	}
	
	/**
	 * LiteLoader constructor
	 */
	private LiteLoader()
	{
	}
	
	private void initLoader()
	{
		if (this.loaderStartupDone) return;
		this.loaderStartupDone = true;
		
		// Set up base class overrides
		this.prepareClassOverrides();
		
		// Set up loader, initialises any reflection methods needed
		if (this.prepareLoader())
		{
			logger.info(String.format("LiteLoader %s starting up...", LOADER_VERSION));
			
			// Print the branding version if any was provided
			if (this.branding != null)
			{
				logger.info(String.format("Active Pack: %s", this.branding));
			}
			
			logger.info(String.format("Java reports OS=\"%s\"", System.getProperty("os.name").toLowerCase()));
			
			boolean searchMods             = this.localProperties.getProperty("search.mods",      "true").equalsIgnoreCase("true");
			boolean searchProtectionDomain = this.localProperties.getProperty("search.jar",       "true").equalsIgnoreCase("true");
			boolean searchClassPath        = this.localProperties.getProperty("search.classpath", "true").equalsIgnoreCase("true");
			
			if (!searchMods && !searchProtectionDomain && !searchClassPath)
			{
				logger.warning("Invalid configuration, no search locations defined. Enabling all search locations.");
				
				this.localProperties.setProperty("search.mods",      "true");
				this.localProperties.setProperty("search.jar",       "true");
				this.localProperties.setProperty("search.classpath", "true");
				
				searchMods             = true;
				searchProtectionDomain = true;
				searchClassPath        = true;
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
	 * Do dirty non-base-clean overrides
	 */
	private void prepareClassOverrides()
	{
		this.registerBaseClassOverride(ModUtilities.getObfuscatedFieldName("net.minecraft.src.CallableJVMFlags", "g"), "g");
	}
	
	/**
	 * Reads a base class overrride from a resource file
	 * 
	 * @param binaryClassName
	 * @param fileName
	 */
	private void registerBaseClassOverride(String binaryClassName, String fileName)
	{
		try
		{
			Method mDefineClass = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class); 
			mDefineClass.setAccessible(true);
			
			InputStream resourceInputStream = LiteLoader.class.getResourceAsStream("/classes/" + fileName + ".bin");
			
			if (resourceInputStream != null)
			{
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				
				for (int readBytes = resourceInputStream.read(); readBytes >= 0; readBytes = resourceInputStream.read())
				{
			        outputStream.write(readBytes);
				}
			 
				byte[] data = outputStream.toByteArray();

				outputStream.close();
			    resourceInputStream.close();

			    logger.info("Defining class override for " + binaryClassName);
			    mDefineClass.invoke(Minecraft.class.getClassLoader(), binaryClassName, data, 0, data.length);
			}
			else
			{
			    logger.info("Error defining class override for " + binaryClassName + ", file not found");
			}
		}
		catch (Throwable th)
		{
		    logger.log(Level.WARNING, "Error defining class override for " + binaryClassName, th);
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
			
			this.branding = this.internalProperties.getProperty("brand", null);
			if (this.branding != null && this.branding.length() < 1) this.branding = null;
			
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
	@SuppressWarnings("unchecked")
	private void prepareLogger() throws SecurityException, IOException
	{
		Formatter minecraftLogFormatter = null;
		
		try
		{
			Class<? extends Formatter> formatterClass = (Class<? extends Formatter>)Minecraft.class.getClassLoader().loadClass(ModUtilities.getObfuscatedFieldName("net.minecraft.src.ConsoleLogFormatter", "em"));
			Constructor<? extends Formatter> defaultConstructor = formatterClass.getDeclaredConstructor();
			defaultConstructor.setAccessible(true);
			minecraftLogFormatter = defaultConstructor.newInstance();
		}
		catch (Exception ex)
		{
			ConsoleLogManager.init();
			minecraftLogFormatter = ConsoleLogManager.loggerLogManager.getHandlers()[0].getFormatter();
		}
		
		logger.setUseParentHandlers(false);
		
		this.useStdOut = System.getProperty("liteloader.log", "stderr").equalsIgnoreCase("stdout") || this.localProperties.getProperty("log", "stderr").equalsIgnoreCase("stdout");
		
		StreamHandler consoleHandler = useStdOut ? new com.mumfrey.liteloader.util.log.ConsoleHandler() : new java.util.logging.ConsoleHandler();
		if (minecraftLogFormatter != null) consoleHandler.setFormatter(minecraftLogFormatter);
		logger.addHandler(consoleHandler);
		
		FileHandler logFileHandler = new FileHandler(new File(Minecraft.getMinecraftDir(), "LiteLoader.txt").getAbsolutePath());
		if (minecraftLogFormatter != null) logFileHandler.setFormatter(minecraftLogFormatter);
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
			InputStream localPropertiesStream = this.getLocalPropertiesStream();
			
			if (localPropertiesStream != null)
			{
				this.localProperties.load(localPropertiesStream);
				localPropertiesStream.close();
			}
		}
		catch (Throwable th)
		{
			this.localProperties = new Properties();
		}
	}
	
	/**
	 * Get the properties stream either from the jar or from the properties file in the minecraft folder
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
			this.localProperties.store(new FileWriter(this.propertiesFile), String.format("Properties for LiteLoader %s", LOADER_VERSION));
		}
		catch (Throwable th)
		{
			logger.log(Level.WARNING, "Error writing liteloader properties", th);
		}
	}

	/**
	 * Get the "mods" folder
	 */
	public File getModsFolder()
	{
		if (this.modsFolder == null)
		{
			this.modsFolder = new File(Minecraft.getMinecraftDir(), "mods");
			
			if (!this.modsFolder.exists() || !this.modsFolder.isDirectory())
			{
				try
				{
					// Attempt to create the "mods" folder if it does not already exist
					this.modsFolder.mkdirs();
				}
				catch (Exception ex) {}
			}
		}
		
		return this.modsFolder;
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
	 * Enumerate the java class path and "mods" folder to find mod classes, then load the classes
	 */
	private void prepareMods(boolean searchMods, boolean searchProtectionDomain, boolean searchClassPath)
	{
		// List of mod files in the "mods" folder
		LinkedList<File> modFiles = new LinkedList<File>();
		
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

		// Find and enumerate classes on the class path
		HashMap<String, Class> modsToLoad = null;
		try
		{
			logger.info("Enumerating class path...");

			String classPath = System.getProperty("java.class.path");
			String classPathSeparator = System.getProperty("path.separator");
			String[] classPathEntries = classPath.split(classPathSeparator);
			
			logger.info(String.format("Class path separator=\"%s\"", classPathSeparator));
			logger.info(String.format("Class path entries=(\n   classpathEntry=%s\n)", classPath.replace(classPathSeparator, "\n   classpathEntry=")));
			
			if (searchProtectionDomain || searchClassPath) logger.info("Discovering mods on class path...");
			modsToLoad = this.findModClasses(classPathEntries, modFiles, searchProtectionDomain, searchClassPath);

			logger.info("Mod class discovery completed");
		}
		catch (Throwable th)
		{
			logger.log(Level.WARNING, "Mod class discovery failed", th);
			return;
		}
		
		this.loadMods(modsToLoad);
	}
	
	/**
	 * Find mod files in the "mods" folder
	 * 
	 * @param modFolder Folder to search
	 * @param modFiles List of mod files to load
	 */
	protected void findModFiles(File modFolder, LinkedList<File> modFiles)
	{
		List<String> supportedVerions = Arrays.asList(SUPPORTED_VERSIONS);
		
		for (File modFile : modFolder.listFiles(this))
		{
			try
			{
				// Check for a version file
				ZipFile modZip = new ZipFile(modFile);
				ZipEntry version = modZip.getEntry("version.txt");
				
				if (version != null)
				{
					// Read the version string
					InputStream versionStream = modZip.getInputStream(version);
					BufferedReader versionReader = new BufferedReader(new InputStreamReader(versionStream));
					String strVersion = versionReader.readLine();
					versionReader.close();
					
					// Only add the mod if the version matches and we were able to successfully add it to the class path
					if (supportedVerions.contains(strVersion) && addURLToClassPath(modFile.toURI().toURL()))
					{
						modFiles.add(modFile);
					}
				}
				
				modZip.close();
			}
			catch (Exception ex)
			{
				logger.warning("Error enumerating '" + modFile.getAbsolutePath() + "': Invalid zip file or error reading file");
			}
		}
	}
	
	/* (non-Javadoc)
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
	 * @return map of classes to load
	 */
	private HashMap<String, Class> findModClasses(String[] classPathEntries, LinkedList<File> modFiles, boolean searchProtectionDomain, boolean searchClassPath)
	{
		// To try to avoid loading the same mod multiple times if it appears in more than one entry in the class path, we index
		// the mods by name and hopefully match only a single instance of a particular mod
		HashMap<String, Class> modsToLoad = new HashMap<String, Class>();

		if (searchProtectionDomain)
		{
			try
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
					// Fix (?) for forge and other mods which screw up the protection domain 
					String reflectionClassPath = LiteLoader.class.getResource("/com/mumfrey/liteloader/core/LiteLoader.class").getPath();
					
					if (reflectionClassPath.indexOf('!') > -1)
					{
						reflectionClassPath = URLDecoder.decode(reflectionClassPath, "UTF-8");
						packagePath = new File(reflectionClassPath.substring(5, reflectionClassPath.indexOf('!')));
					}
				}
				
				if (packagePath != null)
				{
					LinkedList<Class> modClasses = getSubclassesFor(packagePath, Minecraft.class.getClassLoader(), LiteMod.class, "LiteMod");
					
					for (Class mod : modClasses)
					{
						modsToLoad.put(mod.getSimpleName(), mod);
					}
					
					if (modClasses.size() > 0) logger.info(String.format("Found %s potential matches", modClasses.size()));
				}
			}
			catch (Throwable th)
			{
				logger.warning("Error loading from local class path: " + th.getMessage());
			}
		}

		if (searchClassPath)
		{
			// Search through the class path and find mod classes
			for (String classPathPart : classPathEntries)
			{
				logger.info(String.format("Searching %s...", classPathPart));
				
				File packagePath = new File(classPathPart);
				LinkedList<Class> modClasses = getSubclassesFor(packagePath, Minecraft.class.getClassLoader(), LiteMod.class, "LiteMod");
				
				for (Class mod : modClasses)
				{
					modsToLoad.put(mod.getSimpleName(), mod);
				}
				
				if (modClasses.size() > 0) logger.info(String.format("Found %s potential matches", modClasses.size()));
			}
		}
		
		// Search through mod files and find mod classes
		for (File modFile : modFiles)
		{
			logger.info(String.format("Searching %s...", modFile.getAbsolutePath()));
			
			LinkedList<Class> modClasses = getSubclassesFor(modFile, Minecraft.class.getClassLoader(), LiteMod.class, "LiteMod");
			
			for (Class mod : modClasses)
			{
				modsToLoad.put(mod.getSimpleName(), mod);
			}

			if (modClasses.size() > 0) logger.info(String.format("Found %s potential matches", modClasses.size()));
		}
		
		return modsToLoad;
	}
	
	/**
	 * Create mod instances from the enumerated classes
	 * 
	 * @param modsToLoad List of mods to load
	 */
	private void loadMods(HashMap<String, Class> modsToLoad)
	{
		if (modsToLoad == null)
		{
			logger.info("Mod class discovery failed. Not loading any mods!");
			return;
		}
		
		logger.info("Discovered " + modsToLoad.size() + " total mod(s)");
		
		for (Class mod : modsToLoad.values())
		{
			try
			{
				logger.info("Loading mod from " + mod.getName());
				
				LiteMod newMod = (LiteMod)mod.newInstance();
				this.mods.add(newMod);
				
				logger.info("Successfully added mod " + newMod.getName() + " version " + newMod.getVersion());
			}
			catch (Throwable th)
			{
				logger.warning(th.toString());
				th.printStackTrace();
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
		
		for (Iterator<LiteMod> iter = mods.iterator(); iter.hasNext();)
		{
			LiteMod mod = iter.next();
			
			try
			{
				logger.info("Initialising mod " + mod.getName() + " version " + mod.getVersion());
				
				mod.init();
				
				if (mod instanceof Tickable)
				{
					this.addTickListener((Tickable)mod);
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
				
				if (mod instanceof ChatListener && !(mod instanceof ChatFilter))
				{
					this.addChatListener((ChatListener)mod);
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
				
				this.loadedModsList += String.format("\n          - %s version %s", mod.getName(), mod.getVersion());
				loadedModsCount++;
			}
			catch (Throwable th)
			{
				logger.log(Level.WARNING, "Error initialising mod '" + mod.getName(), th);
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
				HookChat.Register();
				HookChat.RegisterPacketHandler(this);
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
				HookPluginChannels.Register();
				HookPluginChannels.RegisterPacketHandler(this);
			}
			
			// Tick hook
			if (!this.tickHooked)
			{
				this.tickHooked = true;
				PrivateFields.minecraftProfiler.SetFinal(this.minecraft, new HookProfiler(this, logger));
			}
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
			if (this.loaderStartupComplete) this.initHooks();
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
			if (this.loaderStartupComplete) this.initHooks();
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
			if (this.loaderStartupComplete) this.initHooks();
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
			if (this.loaderStartupComplete) this.initHooks();
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
			if (this.loaderStartupComplete) this.initHooks();
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
			if (this.loaderStartupComplete) this.initHooks();
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
			if (this.loaderStartupComplete) this.initHooks();
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
			if (this.loaderStartupComplete) this.initHooks();
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
			if (this.loaderStartupComplete) this.initHooks();
		}
	}

	/**
	 * Enumerate classes on the classpath which are subclasses of the specified
	 * class
	 * 
	 * @param superClass
	 * @return
	 */
	private static LinkedList<Class> getSubclassesFor(File packagePath, ClassLoader classloader, Class superClass, String prefix)
	{
		LinkedList<Class> classes = new LinkedList<Class>();
		
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
	private static void enumerateCompressedPackage(String prefix, Class superClass, ClassLoader classloader, LinkedList<Class> classes, File packagePath) throws FileNotFoundException, IOException
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
	private static void enumerateDirectory(String prefix, Class superClass, ClassLoader classloader, LinkedList<Class> classes, File packagePath)
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
	private static void enumerateDirectory(String prefix, Class superClass, ClassLoader classloader, LinkedList<Class> classes, File packagePath, String packageName, int depth)
	{
		// Prevent crash due to broken recursion
		if (depth > MAX_DISCOVERY_DEPTH) return;
		
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
	@SuppressWarnings("unchecked")
	private static void checkAndAddClass(ClassLoader classloader, Class superClass, LinkedList<Class> classes, String className)
	{
		if (className.indexOf('$') > -1)
			return;
		
		try
		{
			Class subClass = classloader.loadClass(className);
			
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
	 * Callback from the tick hook, ticks all tickable mods
	 * 
	 * @param tick True if this is a new tick (otherwise it's just a new frame)
	 */
	public void onTick(Profiler profiler, boolean tick)
	{
		float partialTicks = 0.0F;
		
		// Try to get the minecraft timer object and determine the value of the partialTicks
		if (tick || this.minecraftTimer == null)
		{
			this.minecraftTimer = PrivateFields.minecraftTimer.Get(this.minecraft);
		}
			
		// Hooray, we got the timer reference
		if (this.minecraftTimer != null)
		{
			partialTicks = this.minecraftTimer.elapsedPartialTicks;
			tick = this.minecraftTimer.elapsedTicks > 0;
		}
	
		// Flag indicates whether we are in game at the moment
		boolean inGame = this.minecraft.renderViewEntity != null && this.minecraft.renderViewEntity.worldObj != null;
		
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
		// Chat filters get a stab at the chat first, if any filter returns false the chat is discarded
		for (ChatFilter chatFilter : this.chatFilters)
			if (!chatFilter.onChat(chatPacket))
				return false;
		
		// Chat listeners get the chat if no filter removed it
		for (ChatListener chatListener : this.chatListeners)
			chatListener.onChat(chatPacket.message);
		
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
		for (LoginListener loginListener : this.loginListeners)
			loginListener.onLogin(netHandler, loginPacket);
		
		setupPluginChannels();
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
			for (PluginChannelListener pluginChannelListener : this.pluginChannels.get(hookPluginChannels.channel))
			{
				try
				{
					pluginChannelListener.onCustomPayload(hookPluginChannels.channel, hookPluginChannels.length, hookPluginChannels.data);
				}
				catch (Exception ex) {}
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
		
		// Enumerate mods for plugin channels
		for (PluginChannelListener pluginChannelListener : this.pluginChannelListeners)
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

		// If any mods have registered channels, send the REGISTER packet
		if (this.pluginChannels.keySet().size() > 0)
		{
			StringBuilder channelList = new StringBuilder();
			boolean separator = false;
			
			for (String channel : this.pluginChannels.keySet())
			{
				if (separator) channelList.append("\u0000");
				channelList.append(channel);
				separator = true;
			}
			
	        byte[] registrationData = channelList.toString().getBytes(Charset.forName("UTF8"));
        
	        sendPluginChannelMessage("REGISTER", registrationData);
		}
	}
}