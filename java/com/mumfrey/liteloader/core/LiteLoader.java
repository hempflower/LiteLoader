package com.mumfrey.liteloader.core;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.logging.*;
import java.util.logging.Formatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import com.mumfrey.liteloader.*;
import com.mumfrey.liteloader.util.ModUtilities;
import com.mumfrey.liteloader.util.PrivateFields;
import com.sun.corba.se.impl.ior.ByteBuffer;

import net.minecraft.client.Minecraft;
import net.minecraft.src.*;
import net.minecraft.src.Timer;

/**
 * LiteLoader is a simple loader which provides tick events to loaded mods 
 *
 * @author Adam Mummery-Smith
 * @version 1.4.4
 */
@SuppressWarnings("rawtypes")
public final class LiteLoader implements FilenameFilter
{
	/**
	 * Liteloader version 
	 */
	private static final String LOADER_VERSION = "1.4.4";
	
	/**
	 * Loader revision, can be used by mods to determine whether the loader is sufficiently up-to-date 
	 */
	private static final int LOADER_REVISION = 6;
	
	/**
	 * Minecraft versions that we will load mods for, this will be compared
	 * against the version.txt value in mod files to prevent outdated mods being
	 * loaded!!!
	 */
	private static final String[] SUPPORTED_VERSIONS = { "1.4.4" };
	
	/**
	 * LiteLoader is a singleton, this is the singleton instance
	 */
	private static LiteLoader instance;
	
	/**
	 * Logger for LiteLoader events
	 */
	public static Logger logger = Logger.getLogger("liteloader");
	
	/**
	 * "mods" folder which contains mods and config files
	 */
	private File modsFolder;
	
	/**
	 * Reference to the Minecraft game instance
	 */
	private Minecraft minecraft = Minecraft.getMinecraft();
	
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
		if (loaderStartupDone) return;
		loaderStartupDone = true;
		
		// Set up base class overrides
		prepareClassOverrides();
		
		// Set up loader, initialises any reflection methods needed
		if (prepareLoader())
		{
			logger.info("LiteLoader " + LOADER_VERSION + " starting up...");
			logger.info(String.format("Java reports OS=\"%s\"", System.getProperty("os.name").toLowerCase()));
	
			// Examines the class path and mods folder and locates loadable mods
			prepareMods();
			
			// Initialises enumerated mods
			initMods();
			
			// Initialises the required hooks for loaded mods
			initHooks();
			
			loaderStartupComplete = true;
		}
	}
	
	/**
	 * Do dirty non-base-clean overrides
	 */
	private void prepareClassOverrides()
	{
		registerBaseClassOverride(ModUtilities.getObfuscatedFieldName("net.minecraft.src.CallableJVMFlags", "g"), "g");
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
	@SuppressWarnings("unchecked")
	private boolean prepareLoader()
	{
		try
		{
			// addURL method is used by the class loader to 
			mAddUrl = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
			mAddUrl.setAccessible(true);
			
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
			
			StreamHandler consoleHandler = new ConsoleHandler();
			if (minecraftLogFormatter != null) consoleHandler.setFormatter(minecraftLogFormatter);
			logger.addHandler(consoleHandler);
			
			FileHandler logFileHandler = new FileHandler(new File(Minecraft.getMinecraftDir(), "LiteLoader.txt").getAbsolutePath());
			if (minecraftLogFormatter != null) logFileHandler.setFormatter(minecraftLogFormatter);
			logger.addHandler(logFileHandler);
		}
		catch (Throwable th)
		{
			logger.log(Level.SEVERE, "Error initialising LiteLoader", th);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Get the "mods" folder
	 */
	public File getModsFolder()
	{
		if (modsFolder == null)
		{
			modsFolder = new File(Minecraft.getMinecraftDir(), "mods");
			
			if (!modsFolder.exists() || !modsFolder.isDirectory())
			{
				try
				{
					// Attempt to create the "mods" folder if it does not already exist
					modsFolder.mkdirs();
				}
				catch (Exception ex) {}
			}
		}
		
		return modsFolder;
	}
	
	/**
	 * Used for crash reporting
	 * 
	 * @return List of loaded mods as a string
	 */
	public String getLoadedModsList()
	{
		return loadedModsList;
	}
	
	/**
	 * Enumerate the java class path and "mods" folder to find mod classes, then load the classes
	 */
	private void prepareMods()
	{
		// List of mod files in the "mods" folder
		LinkedList<File> modFiles = new LinkedList<File>();
		
		// Find and enumerate the "mods" folder
		File modFolder = getModsFolder();
		if (modFolder.exists() && modFolder.isDirectory())
		{
			logger.info("Mods folder found, searching " + modFolder.getPath());
			findModFiles(modFolder, modFiles);
			logger.info("Found " + modFiles.size() + " mod file(s)");
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
			
			logger.info("Loading mods from class path...");

			modsToLoad = findModClasses(classPathEntries, modFiles);

			logger.info("Mod class discovery completed");
		}
		catch (Throwable th)
		{
			logger.log(Level.WARNING, "Mod class discovery failed", th);
			return;
		}
		
		loadMods(modsToLoad);
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
	private HashMap<String, Class> findModClasses(String[] classPathEntries, LinkedList<File> modFiles)
	{
		// To try to avoid loading the same mod multiple times if it appears in more than one entry in the class path, we index
		// the mods by name and hopefully match only a single instance of a particular mod
		HashMap<String, Class> modsToLoad = new HashMap<String, Class>();

		try
		{
			logger.info("Searching protection domain code source...");
			
			File packagePath = new File(LiteLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			LinkedList<Class> modClasses = getSubclassesFor(packagePath, Minecraft.class.getClassLoader(), LiteMod.class, "LiteMod");
			
			for (Class mod : modClasses)
			{
				modsToLoad.put(mod.getSimpleName(), mod);
			}

			if (modClasses.size() > 0) logger.info(String.format("Found %s potential matches", modClasses.size()));
		}
		catch (Throwable th)
		{
			logger.warning("Error loading from local class path: " + th.getMessage());
		}

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
				mods.add(newMod);
				
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
		loadedModsList = "";
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
					addTickListener((Tickable)mod);
				}

				if (mod instanceof InitCompleteListener)
				{
					addInitListener((InitCompleteListener)mod);
				}
				
				if (mod instanceof RenderListener)
				{
					addRenderListener((RenderListener)mod);
				}
				
				if (mod instanceof ChatFilter)
				{
					addChatFilter((ChatFilter)mod);
				}
				
				if (mod instanceof ChatListener && !(mod instanceof ChatFilter))
				{
					addChatListener((ChatListener)mod);
				}
				
				if (mod instanceof PreLoginListener)
				{
					addPreLoginListener((PreLoginListener)mod);
				}
				
				if (mod instanceof LoginListener)
				{
					addLoginListener((LoginListener)mod);
				}
				
				if (mod instanceof PluginChannelListener)
				{
					addPluginChannelListener((PluginChannelListener)mod);
				}
				
				loadedModsList += String.format("\n    - %s version %s", mod.getName(), mod.getVersion());
				loadedModsCount++;
			}
			catch (Throwable th)
			{
				logger.log(Level.WARNING, "Error initialising mod '" + mod.getName(), th);
				iter.remove();
			}
		}
		
		loadedModsList = String.format("%s loaded mod(s)%s", loadedModsCount, loadedModsList);
	}

	/**
	 * Initialise mod hooks
	 */
	private void initHooks()
	{
		try
		{
			// Chat hook
			if ((chatListeners.size() > 0 || chatFilters.size() > 0) && !chatHooked)
			{
				chatHooked = true;
				HookChat.Register();
				HookChat.RegisterPacketHandler(this);
			}
			
			// Login hook
			if ((preLoginListeners.size() > 0 || loginListeners.size() > 0) && !loginHooked)
			{
				loginHooked = true;
				ModUtilities.registerPacketOverride(1, HookLogin.class);
				HookLogin.loader = this;
			}
			
			// Plugin channels hook
			if (pluginChannelListeners.size() > 0 && !pluginChannelHooked)
			{
				pluginChannelHooked = true;
				HookPluginChannels.Register();
				HookPluginChannels.RegisterPacketHandler(this);
			}
			
			// Tick hook
			if (!tickHooked)
			{
				tickHooked = true;
				PrivateFields.minecraftProfiler.SetFinal(minecraft, new HookProfiler(this, logger));
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
		if (!tickListeners.contains(tickable))
		{
			tickListeners.add(tickable);
			if (loaderStartupComplete) initHooks();
		}
	}
	
	/**
	 * @param initCompleteListener
	 */
	public void addInitListener(InitCompleteListener initCompleteListener)
	{
		if (!initListeners.contains(initCompleteListener))
		{
			initListeners.add(initCompleteListener);
			if (loaderStartupComplete) initHooks();
		}
	}

	/**
	 * @param tickable
	 */
	public void addRenderListener(RenderListener tickable)
	{
		if (!renderListeners.contains(tickable))
		{
			renderListeners.add(tickable);
			if (loaderStartupComplete) initHooks();
		}
	}

	/**
	 * @param chatFilter
	 */
	public void addChatFilter(ChatFilter chatFilter)
	{
		if (!chatFilters.contains(chatFilter))
		{
			chatFilters.add(chatFilter);
			if (loaderStartupComplete) initHooks();
		}
	}

	/**
	 * @param chatListener
	 */
	public void addChatListener(ChatListener chatListener)
	{
		if (!chatListeners.contains(chatListener))
		{
			chatListeners.add(chatListener);
			if (loaderStartupComplete) initHooks();
		}
	}

	/**
	 * @param loginListener
	 */
	public void addPreLoginListener(PreLoginListener loginListener)
	{
		if (!preLoginListeners.contains(loginListener))
		{
			preLoginListeners.add(loginListener);
			if (loaderStartupComplete) initHooks();
		}
	}

	/**
	 * @param loginListener
	 */
	public void addLoginListener(LoginListener loginListener)
	{
		if (!loginListeners.contains(loginListener))
		{
			loginListeners.add(loginListener);
			if (loaderStartupComplete) initHooks();
		}
	}

	/**
	 * @param pluginChannelListener
	 */
	public void addPluginChannelListener(PluginChannelListener pluginChannelListener)
	{
		if (!pluginChannelListeners.contains(pluginChannelListener))
		{
			pluginChannelListeners.add(pluginChannelListener);
			if (loaderStartupComplete) initHooks();
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
		enumerateDirectory(prefix, superClass, classloader, classes, packagePath, "");
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
	private static void enumerateDirectory(String prefix, Class superClass, ClassLoader classloader, LinkedList<Class> classes, File packagePath, String packageName)
	{
		File[] classFiles = packagePath.listFiles();
		
		for (File classFile : classFiles)
		{
			if (classFile.isDirectory())
			{
				enumerateDirectory(prefix, superClass, classloader, classes, classFile, packageName + classFile.getName() + ".");
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
			if (Minecraft.class.getClassLoader() instanceof URLClassLoader && mAddUrl != null && mAddUrl.isAccessible())
			{
				URLClassLoader classLoader = (URLClassLoader)Minecraft.class.getClassLoader();
				mAddUrl.invoke(classLoader, classUrl);
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
		if (!lateInitDone)
		{
			lateInitDone = true;
			
			for (InitCompleteListener initMod : initListeners)
			{
				try
				{
					logger.info("Calling late init for mod " + initMod.getName());
					initMod.onInitCompleted(minecraft, this);
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
		for (RenderListener renderListener : renderListeners)
			renderListener.onRender();
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
		if (tick || minecraftTimer == null)
		{
			minecraftTimer = PrivateFields.minecraftTimer.Get(minecraft);
		}
			
		// Hooray, we got the timer reference
		if (minecraftTimer != null)
		{
			partialTicks = minecraftTimer.elapsedPartialTicks;
			tick = minecraftTimer.elapsedTicks > 0;
		}
	
		// Flag indicates whether we are in game at the moment
		boolean inGame = minecraft.renderViewEntity != null && minecraft.renderViewEntity.worldObj != null;
		
		// Iterate tickable mods
		for (Tickable tickable : tickListeners)
		{
			profiler.startSection(tickable.getClass().getSimpleName());
			tickable.onTick(minecraft, partialTicks, inGame, tick);
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
		for (ChatFilter chatFilter : chatFilters)
			if (!chatFilter.onChat(chatPacket))
				return false;
		
		// Chat listeners get the chat if no filter removed it
		for (ChatListener chatListener : chatListeners)
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
		
		for (PreLoginListener loginListener : preLoginListeners)
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
		for (LoginListener loginListener : loginListeners)
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
		if (hookPluginChannels != null && hookPluginChannels.channel != null && pluginChannels.containsKey(hookPluginChannels.channel))
		{
			for (PluginChannelListener pluginChannelListener : pluginChannels.get(hookPluginChannels.channel))
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
		pluginChannels.clear();
		
		// Enumerate mods for plugin channels
		for (PluginChannelListener pluginChannelListener : pluginChannelListeners)
		{
			List<String> channels = pluginChannelListener.getChannels();
			
			if (channels != null)
			{
				for (String channel : channels)
				{
					if (channel.length() > 16 || channel.toUpperCase().equals("REGISTER") || channel.toUpperCase().equals("UNREGISTER"))
						continue;
					
					if (!pluginChannels.containsKey(channel))
					{
						pluginChannels.put(channel, new LinkedList<PluginChannelListener>());
					}
					
					pluginChannels.get(channel).add(pluginChannelListener);
				}
			}
		}

		// If any mods have registered channels, send the REGISTER packet
		if (pluginChannels.keySet().size() > 0)
		{
			StringBuilder channelList = new StringBuilder();
			boolean separator = false;
			
			for (String channel : pluginChannels.keySet())
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