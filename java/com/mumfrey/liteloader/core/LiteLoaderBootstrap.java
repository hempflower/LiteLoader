package com.mumfrey.liteloader.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.launchwrapper.LaunchClassLoader;

import com.mumfrey.liteloader.launch.ILoaderBootstrap;
import com.mumfrey.liteloader.log.LiteLoaderLogFormatter;

/**
 * LiteLoaderBootstrap is a proxy class which handles the early part of the LiteLoader startup process which
 * used to be handled by the loader itself, this is to ensure that NONE of the Minecraft classes which by
 * necessity the Loader references get loaded before the pre-init stage has completed. This allows us to load
 * transforming tweakers in the pre-init stage without all hell breaking loose because class names have changed
 * between initialisation stages!
 * 
 * This class handles setting up requisite resources like the logger and the enumerator and passes init calls
 * through to the LiteLoader instance at the appropriate points during startup. Because this class is the first
 * part of the loader to get loaded, we also keep central references like the paths and version in here.
 *
 * @author Adam Mummery-Smith
 */
class LiteLoaderBootstrap implements ILoaderBootstrap
{
	/**
	 * Liteloader version
	 */
	public static final LiteLoaderVersion VERSION = LiteLoaderVersion.MC_1_6_4_R2;

	/**
	 * Local logger reference
	 */
	private static final Logger logger = Logger.getLogger("liteloader");

	/**
	 * True to use stdout instead of stderr
	 */
	private static boolean useStdOut;
	
	/**
	 * Base game directory, passed in from the tweaker
	 */
	private final File gameDirectory;
	
	/**
	 * Assets directory, passed in from the tweaker 
	 */
	private final File assetsDirectory;
	
	/**
	 * Active profile, passed in from the tweaker 
	 */
	private final String profile;
	
	/**
	 * "Mods" folder to use
	 */
	private final File modsFolder;
	
	/**
	 * Base "liteconfig" folder under which all other lite mod configs and liteloader configs are placed 
	 */
	private final File configBaseFolder;
	
	/**
	 * File to write log entries to
	 */
	private File logFile;

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
	 * The mod enumerator instance
	 */
	private LiteLoaderEnumerator enumerator;

	/**
	 * @param gameDirectory
	 * @param assetsDirectory
	 * @param profile
	 */
	public LiteLoaderBootstrap(File gameDirectory, File assetsDirectory, String profile)
	{
		this.gameDirectory    = gameDirectory;
		this.assetsDirectory  = assetsDirectory;
		this.profile          = profile;
		
		this.modsFolder       = new File(this.gameDirectory,    "mods");
		this.configBaseFolder = new File(this.gameDirectory,    "liteconfig");
		this.logFile          = new File(this.configBaseFolder, "liteloader.log");
		this.propertiesFile   = new File(this.configBaseFolder, "liteloader.properties");

		if (!this.modsFolder.exists()) this.modsFolder.mkdirs();
		if (!this.configBaseFolder.exists()) this.configBaseFolder.mkdirs();
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.launch.ILoaderBootstrap#preInit(net.minecraft.launchwrapper.LaunchClassLoader, boolean)
	 */
	@Override
	public void preInit(LaunchClassLoader classLoader, boolean loadTweaks)
	{
		// Set up the bootstrap
		if (!this.prepare()) return;
		
		LiteLoaderBootstrap.logInfo("LiteLoader %s starting up...", LiteLoaderBootstrap.VERSION.getLoaderVersion());
		
		// Print the branding version if any was provided
		if (this.branding != null)
		{
			LiteLoaderBootstrap.logInfo("Active Pack: %s", this.branding);
		}
		
		LiteLoaderBootstrap.logInfo("Java reports OS=\"%s\"", System.getProperty("os.name").toLowerCase());
		
		this.enumerator = new LiteLoaderEnumerator(this, classLoader, loadTweaks);
		this.enumerator.discoverMods();

		LiteLoaderBootstrap.logInfo("LiteLoader PreInit completed");
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.launch.ILoaderBootstrap#init(java.util.List, net.minecraft.launchwrapper.LaunchClassLoader)
	 */
	@Override
	public void init(List<String> modsToLoad, LaunchClassLoader classLoader)
	{
		// PreInit failed
		if (this.enumerator == null) return;
		
		try
		{
			if (LiteLoaderBootstrap.logger.getHandlers().length < 1)
				this.prepareLogger();
		}
		catch (Exception ex) {}

		LiteLoaderBootstrap.logger.info("Beginning LiteLoader Init...");
		LiteLoader.init(this, this.enumerator, modsToLoad, classLoader);
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.launch.ILoaderBootstrap#postInit()
	 */
	@Override
	public void postInit()
	{
		// PreInit failed
		if (this.enumerator == null) return;
		
		try
		{
			if (LiteLoaderBootstrap.logger.getHandlers().length < 1)
				this.prepareLogger();
		}
		catch (Exception ex) {}
		
		LiteLoaderBootstrap.logger.info("Beginning LiteLoader PostInit...");
		LiteLoader.postInit();
	}

	/**
	 * Set up reflection methods required by the loader
	 */
	private boolean prepare()
	{
		try
		{
			// Prepare the properties
			this.prepareProperties();
			
			// Prepare the log writer
			this.prepareLogger();
			
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
			LiteLoaderBootstrap.logger.log(Level.SEVERE, "Error initialising LiteLoader Bootstrap", th);
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
		
		LiteLoaderBootstrap.logger.setUseParentHandlers(false);
		LiteLoaderBootstrap.useStdOut = System.getProperty("liteloader.log", "stderr").equalsIgnoreCase("stdout") || this.localProperties.getProperty("log", "stderr").equalsIgnoreCase("stdout");
		
		StreamHandler consoleHandler = useStdOut ? new com.mumfrey.liteloader.util.log.ConsoleHandler() : new java.util.logging.ConsoleHandler();
		consoleHandler.setFormatter(logFormatter);
		LiteLoaderBootstrap.logger.addHandler(consoleHandler);
		
		FileHandler logFileHandler = new FileHandler(this.logFile.getAbsolutePath());
		logFileHandler.setFormatter(logFormatter);
		LiteLoaderBootstrap.logger.addHandler(logFileHandler);
	}
	
	/**
	 * Get the output stream which we are using for console output
	 * 
	 * @return
	 */
	public static final PrintStream getConsoleStream()
	{
		return LiteLoaderBootstrap.useStdOut ? System.out : System.err;
	}

	/**
	 * Prepare the loader properties
	 */
	private void prepareProperties()
	{
		try
		{
			InputStream propertiesStream = LiteLoaderBootstrap.class.getResourceAsStream("/liteloader.properties");
			
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
		return LiteLoaderBootstrap.class.getResourceAsStream("/liteloader.properties");
	}
	
	/**
	 * Write current properties to the properties file
	 */
	public void writeProperties()
	{
		try
		{
			this.localProperties.store(new FileWriter(this.propertiesFile), String.format("Properties for LiteLoader %s", LiteLoaderBootstrap.VERSION));
		}
		catch (Throwable th)
		{
			LiteLoaderBootstrap.logger.log(Level.WARNING, "Error writing liteloader properties", th);
		}
	}

	/**
	 * Get the game directory
	 */
	public File getGameDirectory()
	{
		return this.gameDirectory;
	}
	
	/**
	 * Get the assets directory
	 */
	public File getAssetsDirectory()
	{
		return this.assetsDirectory;
	}

	/**
	 * Get the profile directory
	 */
	public String getProfile()
	{
		return this.profile;
	}

	/**
	 * Get the mods folder
	 */
	public File getModsFolder()
	{
		return this.modsFolder;
	}

	/**
	 * Get the base "liteconfig" folder
	 */
	public File getConfigBaseFolder()
	{
		return this.configBaseFolder;
	}

	/**
	 * Get a boolean propery from the properties file and also write the new value back to the properties file
	 * 
	 * @param propertyName
	 * @param defaultValue
	 * @return
	 */
	public boolean getAndStoreBooleanProperty(String propertyName, boolean defaultValue)
	{
		boolean result = this.localProperties.getProperty(propertyName, String.valueOf(defaultValue)).equalsIgnoreCase("true");
		this.localProperties.setProperty(propertyName, String.valueOf(result));
		return result;
	}
	
	/**
	 * Set a boolean property
	 * 
	 * @param propertyName
	 * @param value
	 */
	public void setBooleanProperty(String propertyName, boolean value)
	{
		this.localProperties.setProperty(propertyName, String.valueOf(value));
	}

	/**
	 * Store current revision for mod in the config file
	 * 
	 * @param modKey
	 */
	public void storeLastKnownModRevision(String modKey)
	{
		if (this.localProperties != null)
		{
			this.localProperties.setProperty(modKey, String.valueOf(LiteLoaderBootstrap.VERSION.getLoaderRevision()));
			this.writeProperties();
		}
	}
	
	/**
	 * Get last know revision for mod from the config file 
	 * 
	 * @param modKey
	 * @return
	 */
	public int getLastKnownModRevision(String modKey)
	{
		if (this.localProperties != null)
		{
			String storedRevision = this.localProperties.getProperty(modKey, "0");
			return Integer.parseInt(storedRevision);
		}
		
		return 0;
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
	 * Set the brand in ClientBrandRetriever to the specified brand 
	 * 
	 * @param brand
	 */
	static void setBranding(String brand)
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
		catch (Throwable th)
		{
			LiteLoaderBootstrap.logger.log(Level.WARNING, "Setting branding failed", th);
		}
	}
	
	/**
	 * @param string
	 * @param args
	 */
	private static void logInfo(String string, Object... args)
	{
		LiteLoaderBootstrap.logger.info(String.format(string, args));
	}
}
