package com.mumfrey.liteloader.launch;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
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
import java.util.TreeSet;

import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.NonOptionArgumentSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

import com.google.common.base.Throwables;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.mumfrey.liteloader.util.SortableValue;
import com.mumfrey.liteloader.util.log.LiteLoaderLogger;

/**
 * LiteLoader tweak class
 * 
 * @author Adam Mummery-Smith
 */
public class LiteLoaderTweaker implements ITweaker
{
	public static final String VERSION = "1.7.10";
	
	protected static final int ENV_TYPE_CLIENT = 0;

	protected static final int ENV_TYPE_DEDICATEDSERVER = 1;

	/**
	 * Loader startup state
	 * 
	 * @author Adam Mummery-Smith
	 */
	enum StartupState
	{
		CONSTRUCT,
		PREINIT,
		BEGINGAME,
		INIT,
		POSTINIT,
		DONE;
		
		/**
		 * Current state
		 */
		private static StartupState currentState = StartupState.CONSTRUCT.gotoState();

		/**
		 * Whether this state is active
		 */
		private boolean inState;
		
		/**
		 * Whether this state is completed (can go to next state)
		 */
		private boolean completed;
		
		/**
		 * @return
		 */
		public boolean isCompleted()
		{
			return this.completed;
		}
		
		/**
		 * @return
		 */
		public boolean isInState()
		{
			return this.inState;
		}
		
		/**
		 * Go to the next state, checks whether can move to the next state (previous state is marked completed) first
		 */
		public StartupState gotoState()
		{
			for (StartupState otherState : StartupState.values())
			{
				if (otherState.isInState() && otherState != this)
				{
					if (otherState.canGotoState(this))
						otherState.leaveState();
					else
						throw new IllegalStateException(String.format("Cannot go to state \"%s\" as %s %s", this.name(), otherState, otherState.getNextState() == this ? "" : "and expects \""  + otherState.getNextState().name() + "\" instead"), LiteLoaderLogger.getLastThrowable());
				}
			}
			
			LiteLoaderLogger.clearLastThrowable();
			StartupState.currentState = this;
			
			this.inState = true;
			this.completed = false;
			
			return this;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString()
		{
			return String.format("\"%s\" is %s %s", this.name(), this.inState ? "[ACTIVE]" : "[INACTIVE]", this.completed ? "and [COMPLETED]" : "but [INCOMPLETE]");
		}
		
		/**
		 * 
		 */
		public void leaveState()
		{
			this.inState = false;
		}
		
		/**
		 * 
		 */
		public void completed()
		{
			if (!this.inState || this.completed)
				throw new IllegalStateException("Attempted to complete state " + this.name() + " but the state is already completed or is not active", LiteLoaderLogger.getLastThrowable());
			
			this.completed = true;
		}
		
		/**
		 * @return
		 */
		private StartupState getNextState()
		{
			return this.ordinal() < StartupState.values().length - 1 ? StartupState.values()[this.ordinal() + 1] : StartupState.DONE;
		}
		
		/**
		 * @param next
		 * @return
		 */
		public boolean canGotoState(StartupState next)
		{
			if (this.inState && next == this.getNextState())
			{
				return this.completed;
			}
			
			return !this.inState;
		}
		
		/**
		 * @return
		 */
		public static StartupState getCurrent()
		{
			return StartupState.currentState;
		}
	}
	
	private static LiteLoaderTweaker instance;
	
	private URL jarUrl;
	
	private List<String> singularLaunchArgs = new ArrayList<String>();
	private Map<String, String> launchArgs;
	
	private ArgumentAcceptingOptionSpec<String> jarOption;
	private ArgumentAcceptingOptionSpec<String> modsOption;
	private ArgumentAcceptingOptionSpec<String> apisOption;
	private OptionSet parsedOptions;
	
	private int tweakOrder = 0;
	private Set<String> allCascadingTweaks = new HashSet<String>();
	private Set<SortableValue<String>> sortedCascadingTweaks = new TreeSet<SortableValue<String>>();

	private boolean isPrimary;
	
	/**
	 * Loader bootstrap object
	 */
	private LoaderBootstrap bootstrap;
	
	private LoaderProperties properties;
	
	/**
	 * Transformer manager
	 */
	private ClassTransformerManager transformerManager;
	
	private static final String bootstrapClassName = "com.mumfrey.liteloader.core.LiteLoaderBootstrap";
	
	private static final String genTransformerClassName = "com.mumfrey.liteloader.client.gen.GenProfilerTransformer";

	@Override
	public void acceptOptions(List<String> args, File gameDirectory, File assetsDirectory, String profile)
	{
		LiteLoaderTweaker.instance = this;
		
		this.initArgs(args, gameDirectory, assetsDirectory);
		
		List<String> modsToLoad = this.getModFilterList();
		List<String> apisToLoad = this.getAPIsToLoad();
		
		this.registerCoreAPIs(apisToLoad);
		
		if (this.parsedOptions.has(this.jarOption))
		{
			this.initJarUsing(this.jarOption.value(this.parsedOptions));
		}
		else
		{
			this.initJar();
		}

		LiteLoaderLogger.info("Bootstrapping LiteLoader " + LiteLoaderTweaker.VERSION);
		
		this.bootstrap = this.spawnBootstrap(LiteLoaderTweaker.bootstrapClassName, Launch.classLoader, gameDirectory, assetsDirectory, profile, apisToLoad);
		this.properties = this.bootstrap instanceof LoaderProperties ? (LoaderProperties)this.bootstrap : null;
		
		this.transformerManager = new ClassTransformerManager(this.bootstrap.getRequiredTransformers());
		this.transformerManager.injectTransformers(this.bootstrap.getPacketTransformers());
		
		StartupState.CONSTRUCT.completed();
		
		this.preInit(modsToLoad);
	}

	/**
	 * @return
	 */
	private List<String> getModFilterList()
	{
		return (this.parsedOptions.has(this.modsOption)) ? this.modsOption.values(this.parsedOptions) : null;
	}

	/**
	 * @return
	 */
	private List<String> getAPIsToLoad()
	{
		List<String> apisToLoad = new ArrayList<String>();
		this.registerCoreAPIs(apisToLoad);
		if (this.parsedOptions.has(this.apisOption))
		{
			apisToLoad.addAll(this.apisOption.values(this.parsedOptions));
		}
		
		return apisToLoad;
	}
	
	protected void registerCoreAPIs(List<String> apisToLoad)
	{
		apisToLoad.add(0, "com.mumfrey.liteloader.client.api.LiteLoaderCoreAPIClient");
	}

	/**
	 * 
	 */
	private void initJarUsing(String jarPath)
	{
		try
		{
			if (jarPath.matches("^[0-9\\.]+$")) jarPath = String.format("versions/%1$s/%1$s.jar", jarPath);
			LiteLoaderLogger.info("Version jar '%s' was specified on the command line", jarPath);
			File jarFile = new File(jarPath);
			this.jarUrl = jarFile.toURI().toURL();
			this.injectVersionJar(jarFile);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	/**
	 * @throws JsonSyntaxException
	 * @throws JsonIOException
	 */
	private void initJar() throws JsonSyntaxException, JsonIOException
	{
		String resource = "/jarfile.ref";
		InputStream refResource = LiteLoaderTweaker.class.getResourceAsStream(resource);
		File refContainer = ClassPathUtilities.getPathToResource(LiteLoaderTweaker.class, resource);
		if (refResource != null && refContainer != null)
		{
			InputStreamReader refReader = new InputStreamReader(refResource);
			
			try
			{
				@SuppressWarnings("unchecked")
				Map<String, String> refMap = new Gson().fromJson(refReader, HashMap.class);
				if (refMap.containsKey("jarfile"))
				{
					String jarPath = refMap.get("jarfile");
					LiteLoaderLogger.info("Version jar '%s' specified via jarfile.ref", jarPath);
					File jarFile = new File(refContainer.getParentFile(), jarPath);
					this.jarUrl = jarFile.toURI().toURL();
					this.injectVersionJar(jarFile);
					return;
				}
			}
			catch (IOException ex) {}
			finally
			{
				try
				{
					refReader.close();
					refResource.close();
				}
				catch (IOException ex) {}
			}
		}
			
		URL[] urls = Launch.classLoader.getURLs();
		this.jarUrl = urls[urls.length - 1]; // probably?
	}

	/**
	 * @param jarFile
	 */
	private void injectVersionJar(File jarFile)
	{
		LiteLoaderLogger.info("Injecting version jar '%s'", jarFile.getAbsolutePath());
		Launch.classLoader.addURL(this.jarUrl);
		LiteLoaderTweaker.addURLToParentClassLoader(this.jarUrl);
	}

	/**
	 * @param args
	 * @param gameDirectory
	 * @param assetsDirectory
	 */
	@SuppressWarnings("unchecked")
	public void initArgs(List<String> args, File gameDirectory, File assetsDirectory)
	{
		OptionParser optionParser = new OptionParser();
		this.jarOption = optionParser.accepts("versionJar", "Minecraft version jar to use").withRequiredArg().ofType(String.class);
		this.modsOption = optionParser.accepts("mods", "Comma-separated list of mods to load").withRequiredArg().ofType(String.class).withValuesSeparatedBy(',');
		this.apisOption = optionParser.accepts("api", "Additional API classes to load").withRequiredArg().ofType(String.class);
		optionParser.allowsUnrecognizedOptions();
		NonOptionArgumentSpec<String> nonOptions = optionParser.nonOptions();
		
		this.parsedOptions = optionParser.parse(args.toArray(new String[args.size()]));
		this.launchArgs = (Map<String, String>)Launch.blackboard.get("launchArgs");
		if (this.launchArgs == null)
		{
			this.launchArgs = new HashMap<String, String>();			
			Launch.blackboard.put("launchArgs", this.launchArgs);
		}
		
		// Parse out the arguments ourself because joptsimple doesn't really provide a good way to
		// add arguments to the unparsed argument list after parsing
		this.parseArgs(this.parsedOptions.valuesOf(nonOptions));
		
		// Put required arguments to the blackboard if they don't already exist there
		this.provideRequiredArgs(gameDirectory, assetsDirectory);
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
		classLoader.addClassLoaderExclusion("com.mumfrey.liteloader.core.runtime.");

		LiteLoaderTweaker.instance.transformerManager.injectUpstreamTransformers(classLoader);

		LoaderBootstrap bootstrap = LiteLoaderTweaker.instance.bootstrap;
		if (bootstrap instanceof LoaderProperties && ((LoaderProperties)bootstrap).getBooleanProperty(LoaderProperties.OPTION_GENERATE_MAPPINGS))
		{
			LiteLoaderLogger.info("Injecting gen trasnformer '%s'", LiteLoaderTweaker.genTransformerClassName);
			LiteLoaderTweaker.instance.transformerManager.injectTransformer(LiteLoaderTweaker.genTransformerClassName);
		}
		
		for (String transformerClassName : this.bootstrap.getRequiredDownstreamTransformers())
		{
			LiteLoaderLogger.info("Queuing required class transformer '%s'", transformerClassName);
			LiteLoaderTweaker.instance.transformerManager.injectTransformer(transformerClassName);
		}
	}
	
	@Override
	public String getLaunchTarget()
	{
		this.isPrimary = true;
		LiteLoaderTweaker.preBeginGame();
		
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
	
	public static boolean addCascadedTweaker(String tweakClass, int priority)
	{
		return LiteLoaderTweaker.instance.addTweakToSortedList(tweakClass, priority);
	}

	private boolean addTweakToSortedList(String tweakClass, int priority)
	{
		if (tweakClass != null && !this.allCascadingTweaks.contains(tweakClass))
		{
			if (this.getClass().getName().equals(tweakClass))
				return false;
			
			if (LiteLoaderTweaker.isTweakAlreadyEnqueued(tweakClass))
				return false;
			
			this.allCascadingTweaks.add(tweakClass);
			this.sortedCascadingTweaks.add(new SortableValue<String>(priority, this.tweakOrder++, tweakClass));
			return true;
		}
		
		return false;
	}

	@SuppressWarnings("unchecked")
	private void injectDiscoveredTweakClasses()
	{
		if (this.sortedCascadingTweaks.size() > 0)
		{
			if (StartupState.getCurrent() != StartupState.PREINIT || !StartupState.PREINIT.isInState())
			{
				LiteLoaderLogger.warning("Failed to inject cascaded tweak classes because preInit is already complete");
				return;
			}
			
			LiteLoaderLogger.info("Injecting cascaded tweakers...");

			List<String> tweakClasses = (List<String>)Launch.blackboard.get("TweakClasses");
			List<ITweaker> tweakers = (List<ITweaker>)Launch.blackboard.get("Tweaks");
			if (tweakClasses != null && tweakers != null)
			{
				for (SortableValue<String> tweak : this.sortedCascadingTweaks)
				{
					String tweakClass = tweak.getValue();
					LiteLoaderLogger.info("Injecting tweak class %s with priority %d", tweakClass, tweak.getPriority());
					this.injectTweakClass(tweakClass, tweakClasses, tweakers);
				}
			}
			
			// Clear sortedTweaks but not allTweaks
			this.sortedCascadingTweaks.clear();
		}
	}

	/**
	 * @param tweakClass
	 * @param tweakClasses
	 * @param tweakers
	 */
	private void injectTweakClass(String tweakClass, List<String> tweakClasses, List<ITweaker> tweakers)
	{
		if (!tweakClasses.contains(tweakClass))
		{
			for (ITweaker existingTweaker : tweakers)
			{
				if (tweakClass.equals(existingTweaker.getClass().getName()))
					return;
			}
			
			tweakClasses.add(tweakClass);
		}
	}

	/**
	 * @param url URL to add
	 */
	public static boolean addURLToParentClassLoader(URL url)
	{
		if (StartupState.getCurrent() == StartupState.PREINIT && StartupState.PREINIT.isInState())
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
				LiteLoaderLogger.warning(ex, "addURLToParentClassLoader failed: %s", ex.getMessage());
			}
		}
			
		return false;
	}

	protected LoaderBootstrap spawnBootstrap(String bootstrapClassName, ClassLoader classLoader, File gameDirectory, File assetsDirectory, String profile, List<String> apisToLoad)
	{
		if (!StartupState.CONSTRUCT.isInState())
		{
			throw new IllegalStateException("spawnBootstrap is not valid outside constructor");
		}
		
		try
		{
			@SuppressWarnings("unchecked")
			Class<? extends LoaderBootstrap> bootstrapClass = (Class<? extends LoaderBootstrap>)Class.forName(bootstrapClassName, false, classLoader);
			Constructor<? extends LoaderBootstrap> bootstrapCtor = bootstrapClass.getDeclaredConstructor(Integer.TYPE, File.class, File.class, String.class, List.class);
			bootstrapCtor.setAccessible(true);
			
			return bootstrapCtor.newInstance(this.getEnvironmentTypeId(), gameDirectory, assetsDirectory, profile, apisToLoad);
		}
		catch (Throwable th)
		{
			Throwables.propagate(th);
		}
		
		return null;
	}

	protected int getEnvironmentTypeId()
	{
		return LiteLoaderTweaker.ENV_TYPE_CLIENT;
	}

	/**
	 * Do the first stage of loader startup, which enumerates mod sources and finds tweakers
	 */
	private void preInit(List<String> modsToLoad)
	{
		StartupState.PREINIT.gotoState();

		try
		{
			this.bootstrap.preInit(Launch.classLoader, true, modsToLoad);
			
			this.injectDiscoveredTweakClasses();
			StartupState.PREINIT.completed();
		}
		catch (Throwable th)
		{
			LiteLoaderLogger.severe(th, "Error during LiteLoader PREINIT: %s %s", th.getClass().getName(), th.getMessage());
		}
	}
	
	public static void preBeginGame()
	{
		StartupState.BEGINGAME.gotoState();
		try
		{
			LiteLoaderTweaker.instance.transformerManager.injectDownstreamTransformers(Launch.classLoader);
			LiteLoaderTweaker.instance.bootstrap.preBeginGame();
			StartupState.BEGINGAME.completed();
		}
		catch (Throwable th)
		{
			LiteLoaderLogger.severe(th, "Error during LiteLoader BEGINGAME: %s %s", th.getClass().getName(), th.getMessage());
		}
	}

	/**
	 * Do the second stage of loader startup
	 */
	public static void init()
	{
		StartupState.INIT.gotoState();
		
		try
		{
			LiteLoaderTweaker.instance.bootstrap.init(Launch.classLoader);
			StartupState.INIT.completed();
		}
		catch (Throwable th)
		{
			LiteLoaderLogger.severe(th, "Error during LiteLoader INIT: %s %s", th.getClass().getName(), th.getMessage());
		}
	}
	
	/**
	 * Do the second stage of loader startup
	 */
	public static void postInit()
	{
		StartupState.POSTINIT.gotoState();

		try
		{
			LiteLoaderTweaker.instance.bootstrap.postInit();
			StartupState.POSTINIT.completed();

			StartupState.DONE.gotoState();
		}
		catch (Throwable th)
		{
			LiteLoaderLogger.severe(th, "Error during LiteLoader POSTINIT: %s %s", th.getClass().getName(), th.getMessage());
		}
	}
	
	public static URL getJarUrl()
	{
		return LiteLoaderTweaker.instance.jarUrl;
	}
	
	public static boolean isPrimary()
	{
		return LiteLoaderTweaker.instance.isPrimary;
	}

	public static ClassTransformerManager getTransformerManager()
	{
		return LiteLoaderTweaker.instance.transformerManager;
	}
	
	@SuppressWarnings("unchecked")
	private static boolean isTweakAlreadyEnqueued(String clazz)
	{
		List<String> tweakClasses = (List<String>)Launch.blackboard.get("TweakClasses");
		List<ITweaker> tweakers = (List<ITweaker>)Launch.blackboard.get("Tweaks");
		
		if (tweakClasses != null)
		{
			for (String tweakClass : tweakClasses)
			{
				if (tweakClass.equals(clazz)) return true;
			}
		}		
		
		if (tweakers != null)
		{
			for (ITweaker tweaker : tweakers)
			{
				if (tweaker.getClass().getName().equals(clazz)) return true;
			}
		}
		
		return false;
	}

	public static boolean loadingBarEnabled()
	{
		return LiteLoaderTweaker.instance.properties != null ? LiteLoaderTweaker.instance.properties.getBooleanProperty("loadingbar") : false; 
	}
}