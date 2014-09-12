package com.mumfrey.liteloader.launch;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

import com.google.common.base.Throwables;
import com.mumfrey.liteloader.util.SortableValue;
import com.mumfrey.liteloader.util.log.LiteLoaderLogger;

/**
 * LiteLoader tweak class
 * 
 * @author Adam Mummery-Smith
 */
public class LiteLoaderTweaker implements ITweaker
{
	public static final int ENV_TYPE_CLIENT = 0;
	public static final int ENV_TYPE_DEDICATEDSERVER = 1;
	
	public static final String VERSION = "1.7.10";

	protected static final String bootstrapClassName = "com.mumfrey.liteloader.core.LiteLoaderBootstrap";
	protected static final String genTransformerClassName = "com.mumfrey.liteloader.client.gen.GenProfilerTransformer";

	/**
	 * Loader startup state
	 * 
	 * @author Adam Mummery-Smith
	 */
	enum StartupState
	{
		PREPARE,
		PREINIT,
		BEGINGAME,
		INIT,
		POSTINIT,
		DONE;
		
		/**
		 * Current state
		 */
		private static StartupState currentState = StartupState.PREPARE.gotoState();

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
						throw new IllegalStateException(String.format("Cannot go to state <%s> as %s %s", this.name(), otherState, otherState.getNextState() == this ? "" : "and expects \""  + otherState.getNextState().name() + "\" instead"), LiteLoaderLogger.getLastThrowable());
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
			return String.format("<%s> is %s %s", this.name(), this.inState ? "[ACTIVE]" : "[INACTIVE]", this.completed ? "and [COMPLETED]" : "but [INCOMPLETE]");
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
	
	protected URL jarUrl;
	
	protected int tweakOrder = 0;
	protected Set<String> allCascadingTweaks = new HashSet<String>();
	protected Set<SortableValue<String>> sortedCascadingTweaks = new TreeSet<SortableValue<String>>();

	protected boolean isPrimary;
	
	protected StartupEnvironment env;
	
	/**
	 * Loader bootstrap object
	 */
	protected LoaderBootstrap bootstrap;
	
	protected LoaderProperties properties;
	
	/**
	 * Transformer manager
	 */
	protected ClassTransformerManager transformerManager;
	
	@Override
	public void acceptOptions(List<String> args, File gameDirectory, File assetsDirectory, String profile)
	{
		Launch.classLoader.addClassLoaderExclusion("com.google.common.");
		LiteLoaderTweaker.instance = this;
		
		this.prepare(args, gameDirectory, assetsDirectory, profile);
		
		this.preInit();
	}

	/**
	 * @param gameDirectory
	 * @param assetsDirectory
	 * @param profile
	 * @param apisToLoad
	 */
	private void prepare(List<String> args, File gameDirectory, File assetsDirectory, String profile)
	{
		LiteLoaderLogger.info("Bootstrapping LiteLoader " + LiteLoaderTweaker.VERSION);
		
		try
		{
			this.initEnvironment(args, gameDirectory, assetsDirectory, profile);

			this.bootstrap = this.spawnBootstrap(LiteLoaderTweaker.bootstrapClassName, Launch.classLoader);
			this.properties = this.bootstrap.getProperties();
			
			this.transformerManager = new ClassTransformerManager(this.bootstrap.getRequiredTransformers());
			this.transformerManager.injectTransformers(this.bootstrap.getPacketTransformers());
			
			StartupState.PREPARE.completed();
		}
		catch (Throwable th)
		{
			LiteLoaderLogger.severe(th, "Error during LiteLoader PREPARE: %s %s", th.getClass().getName(), th.getMessage());
		}
	}

	/**
	 * Do the first stage of loader startup, which enumerates mod sources and finds tweakers
	 */
	private void preInit()
	{
		StartupState.PREINIT.gotoState();

		try
		{
			this.bootstrap.preInit(Launch.classLoader, true, this.env.getModFilterList());
			
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

	protected void initEnvironment(List<String> args, File gameDirectory, File assetsDirectory, String profile)
	{
		this.env = new StartupEnvironment(args, gameDirectory, assetsDirectory, profile)
		{
			@Override
			public void registerCoreAPIs(List<String> apisToLoad)
			{
				apisToLoad.add(0, "com.mumfrey.liteloader.client.api.LiteLoaderCoreAPIClient");
			}

			@Override
			public int getEnvironmentTypeId()
			{
				return LiteLoaderTweaker.ENV_TYPE_CLIENT;
			}
		};

		URL[] urls = Launch.classLoader.getURLs();
		this.jarUrl = urls[urls.length - 1]; // probably?
	}

	@Override
	public void injectIntoClassLoader(LaunchClassLoader classLoader)
	{
		classLoader.addClassLoaderExclusion("com.mumfrey.liteloader.core.runtime.Obf");

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
		return this.env.getLaunchArguments();
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

	protected LoaderBootstrap spawnBootstrap(String bootstrapClassName, ClassLoader classLoader)
	{
		if (!StartupState.PREPARE.isInState())
		{
			throw new IllegalStateException("spawnBootstrap is not valid outside PREPARE");
		}
		
		try
		{
			@SuppressWarnings("unchecked")
			Class<? extends LoaderBootstrap> bootstrapClass = (Class<? extends LoaderBootstrap>)Class.forName(bootstrapClassName, false, classLoader);
			Constructor<? extends LoaderBootstrap> bootstrapCtor = bootstrapClass.getDeclaredConstructor(StartupEnvironment.class);
			bootstrapCtor.setAccessible(true);
			
			return bootstrapCtor.newInstance(this.env);
		}
		catch (Throwable th)
		{
			Throwables.propagate(th);
		}
		
		return null;
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