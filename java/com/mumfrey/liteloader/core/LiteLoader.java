package com.mumfrey.liteloader.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import net.minecraft.client.Minecraft;
import net.minecraft.src.ConsoleLogManager;
import net.minecraft.src.IntHashMap;
import net.minecraft.src.NetHandler;
import net.minecraft.src.Packet;
import net.minecraft.src.Packet1Login;
import net.minecraft.src.Packet3Chat;
import net.minecraft.src.Timer;

import com.mumfrey.liteloader.ChatFilter;
import com.mumfrey.liteloader.ChatListener;
import com.mumfrey.liteloader.LiteMod;
import com.mumfrey.liteloader.LoginListener;
import com.mumfrey.liteloader.Tickable;

/**
 * LiteLoader is a simple loader which provides tick events to loaded mods 
 *
 * @author Adam Mummery-Smith
 * @version 1.3.2_01
 */
public final class LiteLoader implements FilenameFilter
{
	/**
	 * Minecraft version that we will load mods for, this will be compared
	 * against the version.txt value in mod files to prevent outdated mods being
	 * loaded!!!
	 */
	private static final String MINECRAFT_VERSION = "1.3.1";
	
	/**
	 * LiteLoader is a singleton, this is the singleton instance
	 */
	private static LiteLoader instance;
	
	/**
	 * Logger for LiteLoader events
	 */
	private static Logger logger = Logger.getLogger("liteloader");
	
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
	 * Global list of mods which we have loaded
	 */
	private LinkedList<LiteMod> mods = new LinkedList<LiteMod>();
	
	/**
	 * List of mods which implement Tickable interface and will receive tick
	 * events
	 */
	private LinkedList<Tickable> tickMods = new LinkedList<Tickable>();
	
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
	 * Reference to the addUrl method on URLClassLoader
	 */
	private Method mAddUrl;
	
	/**
	 * Get the singleton instance of LiteLoader, initialises the loader if necessary
	 * 
	 * @return LiteLoader instance
	 */
	public static final LiteLoader getInstance()
	{
		if (instance == null)
		{
			instance = new LiteLoader();
		}
		
		return instance;
	}
	
	/**
	 * LiteLoader constructor
	 */
	private LiteLoader()
	{
		// Set up loader, initialises any reflection methods needed
		prepareLoader();
		
		logger.info("Liteloader for " + MINECRAFT_VERSION + " starting up...");

		// Examines the class path and mods folder and locates loadable mods
		prepareMods();
		
		// Initialises enumerated mods
		initMods();
		
		// Initialises the required hooks for loaded mods
		initHooks();
	}
	
	/**
	 * Set up reflection methods required by the loader
	 */
	private void prepareLoader()
	{
		try
		{
			// addURL method is used by the class loader to 
			mAddUrl = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
			mAddUrl.setAccessible(true);

			ConsoleLogManager.func_73699_a();
			Formatter minecraftLogFormatter = ConsoleLogManager.loggerLogManager.getHandlers()[0].getFormatter();
			
			logger.setUseParentHandlers(false);
			
			StreamHandler consoleHandler = new ConsoleHandler();
			consoleHandler.setFormatter(minecraftLogFormatter);
			logger.addHandler(consoleHandler);
			
			FileHandler logFileHandler = new FileHandler(new File(Minecraft.getMinecraftDir(), "LiteLoader.txt").getAbsolutePath());
			logFileHandler.setFormatter(minecraftLogFormatter);
			logger.addHandler(logFileHandler);

		}
		catch (Exception ex)
		{
			// TODO Auto-generated catch block
			ex.printStackTrace();
		}
		
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
			logger.info("Loading mods from class path");
			
			String classPathSeparator = System.getProperty("path.separator");
			String[] classPathEntries = System.getProperty("java.class.path").split(classPathSeparator);
			modsToLoad = findModClasses(classPathEntries, modFiles);
		}
		catch (Exception ex)
		{
			// TODO Auto-generated catch block
			ex.printStackTrace();
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
					if (strVersion.equals(MINECRAFT_VERSION) && addURLToClassPath(modFile.toURI().toURL()))
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
		
		// Search through the class path and find mod classes
		for (String classPathPart : classPathEntries)
		{
			File packagePath = new File(classPathPart);
			LinkedList<Class> modClasses = getSubclassesFor(packagePath, Minecraft.class.getClassLoader(), LiteMod.class, "LiteMod");
			
			for (Class mod : modClasses)
			{
				modsToLoad.put(mod.getSimpleName(), mod);
			}
		}
		
		// Search through mod files and find mod classes
		for (File modFile : modFiles)
		{
			LinkedList<Class> modClasses = getSubclassesFor(modFile, Minecraft.class.getClassLoader(), LiteMod.class, "LiteMod");
			
			for (Class mod : modClasses)
			{
				modsToLoad.put(mod.getSimpleName(), mod);
			}
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
		if (modsToLoad == null) return;
		
		logger.info("Loading mods");
		
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
				logger.warning(th.getMessage());
			}
		}
	}
	
	/**
	 * Initialise the mods which were loaded
	 */
	private void initMods()
	{
		for (Iterator<LiteMod> iter = mods.iterator(); iter.hasNext();)
		{
			LiteMod mod = iter.next();
			
			try
			{
				mod.init();
				
				if (mod instanceof Tickable)
				{
					tickMods.add((Tickable)mod);
				}
				
				if (mod instanceof ChatFilter)
				{
					chatFilters.add((ChatFilter)mod);
				}
				
				if (mod instanceof ChatListener && !(mod instanceof ChatFilter))
				{
					chatListeners.add((ChatListener)mod);
				}
				
				if (mod instanceof LoginListener)
				{
					loginListeners.add((LoginListener)mod);
				}
			}
			catch (Throwable th)
			{
				logger.warning("Error initialising mod '" + mod.getName() + "': " + th.getMessage());
				iter.remove();
			}
		}
	}
	
	/**
	 * Initialise mod hooks
	 */
	private void initHooks()
	{
		try
		{
			// Chat hook
			if (chatListeners.size() > 0 || chatFilters.size() > 0)
			{
				registerPacketOverride(3, HookChat.class);
				HookChat.loader = this;
			}
			
			// Login hook
			if (loginListeners.size() > 0)
			{
				registerPacketOverride(1, HookLogin.class);
				HookLogin.loader = this;
			}
			
			// Tick hook
			if (tickMods.size() > 0)
			{
				Field modifiers = Field.class.getDeclaredField("modifiers");
				modifiers.setAccessible(true);
				
				Field profiler = Minecraft.class.getDeclaredField(getObfuscatedFieldName("mcProfiler", "I"));
				modifiers.setInt(profiler, profiler.getModifiers() & ~Modifier.FINAL);
				profiler.setAccessible(true);
				profiler.set(minecraft, new LiteLoaderHook(this, logger));
			}
		}
		catch (Exception ex)
		{
			logger.warning("Error creating hooks: " + ex.getMessage());
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
			logger.warning(th.getMessage());
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
			logger.warning(th.getMessage());
		}
	}
	
	/**
	 * Register a packet override
	 * 
	 * @param packetId
	 * @param newPacket
	 */
	private static boolean registerPacketOverride(int packetId, Class newPacket)
	{
		try
		{
			IntHashMap packetIdToClassMap = Packet.packetIdToClassMap;
			Field fPacketClassToIdMap = Packet.class.getDeclaredField(getObfuscatedFieldName("packetClassToIdMap", "a"));
			fPacketClassToIdMap.setAccessible(true);
			Map packetClassToIdMap = (Map)fPacketClassToIdMap.get(null);
			
			packetIdToClassMap.removeObject(packetId);
			packetIdToClassMap.addKey(packetId, newPacket);
			packetClassToIdMap.put(newPacket, Integer.valueOf(packetId));
			
			return true;
		}
		catch (Exception ex)
		{
			logger.warning("Error registering packet override for packet id " + packetId + ": " + ex.getMessage());
			return false;
		}
	}
	
	/**
	 * Abstraction helper function
	 * 
	 * @param fieldName Name of field to get, returned unmodified if in debug mode
	 * @return Obfuscated field name if present
	 */
	private static String getObfuscatedFieldName(String fieldName, String obfuscatedFieldName)
	{
		return (!net.minecraft.src.Tessellator.instance.getClass().getSimpleName().equals("Tessellator")) ? obfuscatedFieldName : fieldName;
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
			logger.warning("Error adding class path entry: " + th.getMessage());
		}
		
		return false;
	}
	
	/**
	 * Callback from the tick hook, ticks all tickable mods
	 * 
	 * @param tick True if this is a new tick (otherwise it's just a new frame)
	 */
	public void onTick(boolean tick)
	{
		float partialTicks = 0.0F;
		
		// Try to get the minecraft timer object and determine the value of the partialTicks
		if (tick || minecraftTimer == null)
		{
			try
			{
				Field fTimer = Minecraft.class.getDeclaredField(getObfuscatedFieldName("Timer", "T"));
				fTimer.setAccessible(true);
				minecraftTimer = (Timer)fTimer.get(minecraft);
				
			}
			catch (Exception ex) {}
			
			// Hooray, we got the timer reference
			if (minecraftTimer != null)
			{
				partialTicks = minecraftTimer.elapsedPartialTicks;
			}
		}
		
		// Flag indicates whether we are in game at the moment
		boolean inGame = minecraft.renderViewEntity != null && minecraft.renderViewEntity.worldObj != null;
		
		// Iterate tickable mods
		for (Tickable tickable : tickMods)
		{
			tickable.onTick(minecraft, partialTicks, inGame, tick);
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
	 * Callback from the login hook
	 * 
	 * @param netHandler
	 * @param loginPacket
	 */
	public void onConnectToServer(NetHandler netHandler, Packet1Login loginPacket)
	{
		for (LoginListener loginListener : loginListeners)
			loginListener.onLogin(netHandler, loginPacket);
	}
}
