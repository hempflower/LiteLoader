package com.mumfrey.liteloader.launch;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.NonOptionArgumentSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

/**
 *
 * @author Adam Mummery-Smith
 */
public class LiteLoaderTweaker implements ITweaker
{
	public static final String VERSION = "1.6.4";
	
	private static Logger logger = Logger.getLogger("liteloader");
	
	private static boolean preInit = true;
	
	private static boolean init = true;
	
	private static File gameDirectory;
	
	private static File assetsDirectory;
	
	private static String profile;
	
	private static List<String> modsToLoad;

	private static ILoaderBootstrap bootstrap;
	
	private static Set<String> modTransformers = new HashSet<String>();
	
	private List<String> singularLaunchArgs = new ArrayList<String>();
	
	private Map<String, String> launchArgs;
	
	private ArgumentAcceptingOptionSpec<String> modsOption;
	private OptionSet parsedOptions;
	
	private List<String> passThroughArgs;
	
	private static final String[] requiredTransformers = {
		"com.mumfrey.liteloader.launch.LiteLoaderTransformer",
		"com.mumfrey.liteloader.core.hooks.asm.CrashReportTransformer",
		"com.mumfrey.liteloader.core.hooks.asm.ChatPacketTransformer",
		"com.mumfrey.liteloader.core.hooks.asm.LoginPacketTransformer",
		"com.mumfrey.liteloader.core.hooks.asm.CustomPayloadPacketTransformer"
	};
	
	@SuppressWarnings("unchecked")
	@Override
	public void acceptOptions(List<String> args, File gameDirectory, File assetsDirectory, String profile)
	{
		LiteLoaderTweaker.gameDirectory = gameDirectory;
		LiteLoaderTweaker.assetsDirectory = assetsDirectory;
		LiteLoaderTweaker.profile = profile;
		
		OptionParser optionParser = new OptionParser();
		this.modsOption = optionParser.accepts("mods", "Comma-separated list of mods to load").withRequiredArg().ofType(String.class).withValuesSeparatedBy(',');
		optionParser.allowsUnrecognizedOptions();
		NonOptionArgumentSpec<String> nonOptions = optionParser.nonOptions();
		
		this.parsedOptions = optionParser.parse(args.toArray(new String[args.size()]));
		this.passThroughArgs = this.parsedOptions.valuesOf(nonOptions);
		
		this.launchArgs = (Map<String, String>)Launch.blackboard.get("launchArgs");
		if (this.launchArgs == null)
		{
			this.launchArgs = new HashMap<String, String>();			
			Launch.blackboard.put("launchArgs", this.launchArgs);
		}
		
		// Parse out the arguments ourself because joptsimple doesn't really provide a good way to
		// add arguments to the unparsed argument list after parsing
		this.parseArgs(this.passThroughArgs);
		
		// Put required arguments to the blackboard if they don't already exist there
		this.provideRequiredArgs(gameDirectory, assetsDirectory);
		
		if (this.parsedOptions.has(this.modsOption))
		{
			LiteLoaderTweaker.modsToLoad = this.modsOption.values(this.parsedOptions);
		}
		
		this.preInit();
	}

	/**
	 * @param gameDirectory
	 * @param assetsDirectory
	 */
	public void provideRequiredArgs(File gameDirectory, File assetsDirectory)
	{
		if (!this.launchArgs.containsKey("--version"))
			this.addClassifiedArg("--version", LiteLoaderTweaker.VERSION);
		
		if (!this.launchArgs.containsKey("--gameDir") && gameDirectory != null)
			this.addClassifiedArg("--gameDir", gameDirectory.getAbsolutePath());
		
		if (!this.launchArgs.containsKey("--assetsDir") && assetsDirectory != null)
			this.addClassifiedArg("--assetsDir", assetsDirectory.getAbsolutePath());
	}

	private void parseArgs(List<String> args)
	{
		String classifier = null;
		
		for (String arg : args)
		{
			if (arg.startsWith("-"))
			{
				if (classifier != null)
				{
					this.addClassifiedArg(classifier, "");
					classifier = null;
				}
				else if (arg.contains("="))
				{
					this.addClassifiedArg(arg.substring(0, arg.indexOf('=')), arg.substring(arg.indexOf('=') + 1));
				}
				else
				{
					classifier = arg;
				}
			}
			else
			{
				if (classifier != null)
				{
					this.addClassifiedArg(classifier, arg);
					classifier = null;
				}
				else
					this.singularLaunchArgs.add(arg);
			}
		}
	}

	private void addClassifiedArg(String classifiedArg, String arg)
	{
		this.launchArgs.put(classifiedArg, arg);
	}

	@Override
	public void injectIntoClassLoader(LaunchClassLoader classLoader)
	{
		for (String requiredTransformerClassName : LiteLoaderTweaker.requiredTransformers)
		{
			LiteLoaderTweaker.logger.info(String.format("Injecting required class transformer '%s'", requiredTransformerClassName));
			classLoader.registerTransformer(requiredTransformerClassName);
		}
		
		for (String transformerClassName : LiteLoaderTweaker.modTransformers)
		{
			LiteLoaderTweaker.logger.info(String.format("Injecting additional class transformer class '%s'", transformerClassName));
			classLoader.registerTransformer(transformerClassName);
		}
		
		LiteLoaderTweaker.modTransformers.clear();
	}

	@Override
	public String getLaunchTarget()
	{
		return "net.minecraft.client.main.Main";
	}

	@Override
	public String[] getLaunchArguments()
	{
		List<String> args = new ArrayList<String>();
		
		for (String singularArg : this.singularLaunchArgs)
			args.add(singularArg);
		
		for (Entry<String, String> launchArg : this.launchArgs.entrySet())
		{
			args.add(launchArg.getKey().trim());
			args.add(launchArg.getValue().trim());
		}
		
		this.singularLaunchArgs.clear();
		this.launchArgs.clear();
		
		return args.toArray(new String[args.size()]);
	}
	
	@SuppressWarnings("unchecked")
	public static boolean addTweaker(String tweakClass)
	{
		if (!LiteLoaderTweaker.preInit)
		{
			LiteLoaderTweaker.logger.warning(String.format("Failed to add tweak class %s because preInit is already complete", tweakClass));
			return false;
		}
		
		List<String> tweakers = (List<String>)Launch.blackboard.get("TweakClasses");
		if (tweakers != null)
		{
			tweakers.add(tweakClass);
			return true;
		}
		
		return false;
	}

	public static boolean addClassTransformer(String transfomerClass)
	{
		if (!LiteLoaderTweaker.preInit)
		{
			LiteLoaderTweaker.logger.warning(String.format("Failed to add transformer class %s because preInit is already complete", transfomerClass));
			return false;
		}
			
		LiteLoaderTweaker.modTransformers.add(transfomerClass);
		return true;
	}
	
	/**
	 * @param url URL to add
	 */
	public static boolean addURLToParentClassLoader(URL url)
	{
		if (LiteLoaderTweaker.preInit)
		{
			try
			{
				URLClassLoader classLoader = (URLClassLoader)Launch.class.getClassLoader();
				Method mAddUrl = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
				mAddUrl.setAccessible(true);
				mAddUrl.invoke(classLoader, url);
				
				return true;
			}
			catch (Exception ex)
			{
				LiteLoaderTweaker.logger.log(Level.WARNING, String.format("addURLToParentClassLoader failed: %s", ex.getMessage()), ex);
			}
		}
			
		return false;
	}

	@SuppressWarnings("unchecked")
	private static void spawnBootstrap() throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		Class<? extends ILoaderBootstrap> bootstrapClass = (Class<? extends ILoaderBootstrap>)Class.forName("com.mumfrey.liteloader.core.LiteLoaderBootstrap", false, Launch.classLoader);
		Constructor<? extends ILoaderBootstrap> bootstrapCtor = bootstrapClass.getDeclaredConstructor(File.class, File.class, String.class);
		bootstrapCtor.setAccessible(true);
		
		LiteLoaderTweaker.bootstrap = bootstrapCtor.newInstance(LiteLoaderTweaker.gameDirectory, LiteLoaderTweaker.assetsDirectory, LiteLoaderTweaker.profile);
	}

	/**
	 * Do the first stage of loader startup, which enumerates mod sources and finds tweakers
	 */
	private void preInit()
	{
		if (!LiteLoaderTweaker.preInit) throw new IllegalStateException("Attempt to perform LiteLoader PreInit but PreInit was already completed");
		
		try
		{
			LiteLoaderTweaker.logger.info("Bootstrapping LiteLoader " + LiteLoaderTweaker.VERSION);
			
			LiteLoaderTweaker.spawnBootstrap();
			
			LiteLoaderTweaker.logger.info("Beginning LiteLoader PreInit...");
			
			LiteLoaderTweaker.bootstrap.preInit(Launch.classLoader, true);
			LiteLoaderTweaker.preInit = false;
		}
		catch (Throwable th)
		{
			LiteLoaderTweaker.logger.log(Level.SEVERE, String.format("Error during LiteLoader PreInit: %s", th.getMessage()), th);
		}
	}

	/**
	 * Do the second stage of loader startup
	 */
	protected static void init()
	{
		if (LiteLoaderTweaker.preInit) throw new IllegalStateException("Attempt to perform LiteLoader Init but PreInit was not completed");
		LiteLoaderTweaker.init = true;
		
		try
		{
			LiteLoaderTweaker.bootstrap.init(LiteLoaderTweaker.modsToLoad, Launch.classLoader);
			LiteLoaderTweaker.init = false;
		}
		catch (Throwable th)
		{
			LiteLoaderTweaker.logger.log(Level.SEVERE, String.format("Error during LiteLoader Init: %s", th.getMessage()), th);
		}
	}
	
	/**
	 * Do the second stage of loader startup
	 */
	protected static void postInit()
	{
		if (LiteLoaderTweaker.init) throw new IllegalStateException("Attempt to perform LiteLoader PostInit but Init was not completed");

		try
		{
			LiteLoaderTweaker.bootstrap.postInit();
		}
		catch (Throwable th)
		{
			LiteLoaderTweaker.logger.log(Level.SEVERE, String.format("Error during LiteLoader PostInit: %s", th.getMessage()), th);
		}
	}

	/**
	 * Naive implementation to check whether Forge ModLoader (FML) is loaded
	 */
	public static boolean fmlIsPresent()
	{
		if (ClientBrandRetriever.getClientModName().contains("fml")) return true;
				
		for (IClassTransformer transformer : Launch.classLoader.getTransformers())
			if (transformer.getClass().getName().contains("fml")) return true;
		
		return false;
	}
}