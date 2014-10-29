package com.mumfrey.liteloader.core;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

import com.google.common.base.Throwables;
import com.mumfrey.liteloader.LiteMod;
import com.mumfrey.liteloader.api.EnumerationObserver;
import com.mumfrey.liteloader.api.EnumeratorModule;
import com.mumfrey.liteloader.api.LiteAPI;
import com.mumfrey.liteloader.api.manager.APIProvider;
import com.mumfrey.liteloader.core.event.HandlerList;
import com.mumfrey.liteloader.core.exceptions.OutdatedLoaderException;
import com.mumfrey.liteloader.interfaces.FastIterableDeque;
import com.mumfrey.liteloader.interfaces.Loadable;
import com.mumfrey.liteloader.interfaces.LoadableMod;
import com.mumfrey.liteloader.interfaces.LoaderEnumerator;
import com.mumfrey.liteloader.interfaces.TweakContainer;
import com.mumfrey.liteloader.launch.LiteLoaderTweaker;
import com.mumfrey.liteloader.launch.LoaderEnvironment;
import com.mumfrey.liteloader.launch.LoaderProperties;
import com.mumfrey.liteloader.util.log.LiteLoaderLogger;

/**
 * The enumerator performs all mod discovery functions for LiteLoader, this includes locating mod files to load
 * as well as searching for mod classes within the class path and discovered mod files.
 *
 * @author Adam Mummery-Smith
 */
public class LiteLoaderEnumerator implements LoaderEnumerator
{
	public enum EnumeratorState
	{
		INIT(null),
		DISCOVER(INIT),
		INJECT(DISCOVER),
		REGISTER(INJECT),
		FINALISED(REGISTER);
		
		private final EnumeratorState previousState;
		
		private EnumeratorState(EnumeratorState previousState)
		{
			this.previousState = previousState;
		}
		
		public boolean checkGotoState(EnumeratorState fromState)
		{
			if (fromState != this && fromState != this.previousState)
			{
				throw new IllegalStateException("Attempted to move to an invalid enumerator state " + this + ", expected to be in state " + this.previousState + " but current state is " + fromState);
			}
			
			return true;
		}
	}
	
	private final LoaderEnvironment environment;
	
	private final LoaderProperties properties;
	
	private final LiteLoaderTweaker tweaker;

	/**
	 * Reference to the launch classloader
	 */
	private final LaunchClassLoader classLoader;

	/**
	 * Classes to load, mapped by class name 
	 */
	private final Set<ModInfo<LoadableMod<?>>> modsToLoad = new LinkedHashSet<ModInfo<LoadableMod<?>>>();
	
	/**
	 * Mod containers which are disabled 
	 */
	private final Map<String, ModInfo<Loadable<?>>> disabledContainers = new HashMap<String, ModInfo<Loadable<?>>>();

	/**
	 * Mapping of identifiers to mod containers 
	 */
	private final Map<String, LoadableMod<?>> enabledContainers = new HashMap<String, LoadableMod<?>>();
	
	/**
	 * Containers which have already been checked for potential mod candidates 
	 */
	private final Set<LoadableMod<?>> enumeratedContainers = new HashSet<LoadableMod<?>>();
	
	/**
	 * Tweaks to inject 
	 */
	private final List<TweakContainer<File>> tweakContainers = new ArrayList<TweakContainer<File>>();
	
	/**
	 * Other tweak-containing jars which we have injected 
	 */
	private final List<ModInfo<Loadable<?>>> injectedTweaks = new ArrayList<ModInfo<Loadable<?>>>();
	
	/**
	 * 
	 */
	private final List<EnumeratorModule> modules = new ArrayList<EnumeratorModule>();

	private final String[] supportedPrefixes;
	
	private final FastIterableDeque<EnumerationObserver> observers = new HandlerList<EnumerationObserver>(EnumerationObserver.class);
	
	protected EnumeratorState state = EnumeratorState.INIT;
	
	/**
	 * @param environment
	 * @param properties
	 * @param classLoader
	 */
	public LiteLoaderEnumerator(LoaderEnvironment environment, LoaderProperties properties, LaunchClassLoader classLoader)
	{
		this.environment       = environment;
		this.properties        = properties;
		this.tweaker           = (LiteLoaderTweaker)environment.getTweaker();
		this.classLoader       = classLoader;
		this.supportedPrefixes = this.getSupportedPrefixes(environment);

		// Initialise observers
		this.observers.addAll(environment.getAPIAdapter().getPreInitObservers(EnumerationObserver.class));
		
		// Initialise the shared mod list if we haven't already
		this.getSharedModList();
	}

	/**
	 * @param environment
	 * @return
	 */
	private String[] getSupportedPrefixes(LoaderEnvironment environment)
	{
		List<String> prefixes = new ArrayList<String>();

		for (LiteAPI api : environment.getAPIProvider().getAPIs())
		{
			List<EnumeratorModule> apiModules = api.getEnumeratorModules();
			
			if (apiModules != null)
			{
				for (EnumeratorModule module : apiModules)
				{
					this.registerModule(module);
				}			
			}
			
			String prefix = api.getModClassPrefix();
			if (prefix != null)
			{
				LiteLoaderLogger.info("Adding supported mod class prefix '%s'", prefix);
				prefixes.add(prefix);
			}
		}
		
		return prefixes.toArray(new String[prefixes.size()]);
	}
	
	private void checkState(EnumeratorState state, String action)
	{
		if (this.state != state)
		{
			throw new IllegalStateException("Illegal enumerator state whilst performing " + action + ", expecting " + state + " but current state is " + this.state);
		}
	}
	
	private void gotoState(EnumeratorState state)
	{
		if (state.checkGotoState(this.state))
		{
			this.state = state;
		}
	}

	/**
	 * Get the loader environment
	 */
	public LoaderEnvironment getEnvironment()
	{
		return this.environment;
	}

	/**
	 * Initialise the "shared" mod list if it's not already been created
	 */
	@Override
	public Map<String, Map<String, String>> getSharedModList()
	{
		try
		{
			@SuppressWarnings("unchecked")
			Map<String, Map<String,String>> sharedModList = (Map<String, Map<String, String>>) Launch.blackboard.get("modList");
			
			if (sharedModList == null)
			{
				sharedModList = new HashMap<String, Map<String,String>>();
				Launch.blackboard.put("modList", sharedModList);
			}
			
			return sharedModList;
		}
		catch (Exception ex)
		{
			LiteLoaderLogger.warning("Shared mod list was invalid or not accessible, this isn't especially bad but something isn't quite right");
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.core.PluggableEnumerator#registerModule(com.mumfrey.liteloader.core.EnumeratorModule)
	 */
	@Override
	public void registerModule(EnumeratorModule module)
	{
		this.checkState(EnumeratorState.INIT, "registerModule");
		
		if (module != null && !this.modules.contains(module))
		{
			LiteLoaderLogger.info("Registering discovery module %s: [%s]", module.getClass().getSimpleName(), module);
			this.modules.add(module);
			module.init(this.environment, this.properties);
		}
	}
	
	/**
	 * Get the list of all enumerated mod classes to load
	 */
	@Override
	public Collection<? extends ModInfo<LoadableMod<?>>> getModsToLoad()
	{
		this.checkState(EnumeratorState.FINALISED, "getModsToLoad");
		
		return Collections.unmodifiableSet(this.modsToLoad);
	}
	
	/**
	 * Get the set of disabled containers
	 */
	@Override
	public Collection<? extends ModInfo<Loadable<?>>> getDisabledContainers()
	{
		this.checkState(EnumeratorState.FINALISED, "getDisabledContainers");
		
		return this.disabledContainers.values();
	}
	
	/**
	 * Get the list of injected tweak containers
	 */
	@Override
	public List<? extends ModInfo<Loadable<?>>> getInjectedTweaks()
	{
		this.checkState(EnumeratorState.FINALISED, "getInjectedTweaks");
		
		return this.injectedTweaks;
	}

	/**
	 * Get the number of mods to load
	 */
	@Override
	public int modsToLoadCount()
	{
		return this.modsToLoad.size();
	}

	/**
	 * Get a metadata value for the specified mod
	 * 
	 * @param modClass
	 * @param metaDataKey
	 * @param defaultValue
	 */
	@Override
	public String getModMetaData(Class<? extends LiteMod> modClass, String metaDataKey, String defaultValue)
	{
		this.checkState(EnumeratorState.FINALISED, "getModMetaData");
		
		return this.getContainerForMod(modClass).getMetaValue(metaDataKey, defaultValue);
	}
	
	/**
	 * @param identifier
	 */
	@Override
	public LoadableMod<?> getContainer(String identifier)
	{
		this.checkState(EnumeratorState.FINALISED, "getContainer");
		
		return this.getContainerById(identifier);
	}

	/**
	 * @param identifier
	 * @return
	 */
	private LoadableMod<?> getContainerById(String identifier)
	{
		LoadableMod<?> container = this.enabledContainers.get(identifier);
		return container != null ? container : LoadableMod.NONE;
	}
	
	/**
	 * @param modClass
	 */
	@Override
	public LoadableMod<?> getContainer(Class<? extends LiteMod> modClass)
	{
		this.checkState(EnumeratorState.FINALISED, "getContainer");
		
		return this.getContainerForMod(modClass);
	}

	/**
	 * @param modClass
	 * @return
	 */
	private LoadableMod<?> getContainerForMod(Class<? extends LiteMod> modClass)
	{
		for (ModInfo<LoadableMod<?>> mod : this.modsToLoad)
		{
			if (modClass.equals(mod.getModClass()))
				return mod.getContainer();
		}
		
		return LoadableMod.NONE;
	}

	/**
	 * Get the mod identifier (metadata key), this is used for versioning, exclusivity, and enablement checks
	 * 
	 * @param modClass
	 */
	@Override
	public String getIdentifier(Class<? extends LiteMod> modClass)
	{
		String modClassName = modClass.getSimpleName();

		for (ModInfo<LoadableMod<?>> mod : this.modsToLoad)
		{
			if (modClassName.equals(mod.getModClassSimpleName()))
				return mod.getIdentifier();
		}
		
		return LiteLoaderEnumerator.getModClassName(modClass);
	}
	
	@Override
	public void onPreInit()
	{
		this.discoverContainers();
		this.injectDiscoveredTweaks();
	}

	/**
	 * Call enumerator modules in order to find mod containers
	 */
	private void discoverContainers()
	{
		this.gotoState(EnumeratorState.DISCOVER);
		
		for (EnumeratorModule module : this.modules)
		{
			try
			{
				module.enumerate(this, this.environment.getProfile());
			}
			catch (Throwable th)
			{
				LiteLoaderLogger.warning(th, "Enumerator Module %s encountered an error whilst enumerating", module.getClass().getName());
			}
		}
	}
	
	private void injectDiscoveredTweaks()
	{
		this.gotoState(EnumeratorState.INJECT);
		
		for (TweakContainer<File> tweakContainer : this.tweakContainers)
		{
			this.addTweaksFrom(tweakContainer);
		}
	}
	
	/**
	 * Enumerate class path and discovered mod files to find mod classes
	 */
	@Override
	public void onInit()
	{
		try
		{
			this.gotoState(EnumeratorState.INJECT);
			
			for (EnumeratorModule module : this.modules)
			{
				try
				{
					module.injectIntoClassLoader(this, this.classLoader);
				}
				catch (Throwable th)
				{
					LiteLoaderLogger.warning(th, "Enumerator Module %s encountered an error whilst injecting", module.getClass().getName());
				}
			}

			this.gotoState(EnumeratorState.REGISTER);
			
			for (EnumeratorModule module : this.modules)
			{
				try
				{
					module.registerMods(this, this.classLoader);
				}
				catch (Throwable th)
				{
					LiteLoaderLogger.warning(th, "Enumerator Module %s encountered an error whilst registering mods", module.getClass().getName());
				}
			}

			LiteLoaderLogger.info("Mod class discovery completed");

			this.gotoState(EnumeratorState.FINALISED);
		}
		catch (IllegalStateException ex) // wut?
		{
			Throwables.propagate(ex);
		}
		catch (Throwable th)
		{
			LiteLoaderLogger.warning(th, "Mod class discovery failed");
		}
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.interfaces.PluggableEnumerator#registerModContainer(com.mumfrey.liteloader.interfaces.LoadableMod)
	 */
	@Override
	public final boolean registerModContainer(LoadableMod<?> container)
	{
		this.checkState(EnumeratorState.DISCOVER, "registerModContainer");
		
		if (container != null)
		{
			if (!container.isEnabled(this.environment))
			{
				LiteLoaderLogger.info("Container %s is disabled", container.getLocation());
				this.registerDisabledContainer(container, DisabledReason.USER_DISABLED);
				return false;
			}
			
			if (!this.checkDependencies(container))
			{
				LiteLoaderLogger.info("Container %s is missing one or more dependencies", container.getLocation());
				this.registerDisabledContainer(container, DisabledReason.MISSING_DEPENDENCY);
				return false;
			}
				
			if (!this.checkAPIRequirements(container))
			{
				LiteLoaderLogger.info("Container %s is missing one or more required APIs", container.getLocation());
				this.registerDisabledContainer(container, DisabledReason.MISSING_API);
				return false;
			}

			this.registerEnabledContainer(container);
		}

		return true;
	}

	/**
	 * @param container
	 */
	protected void registerEnabledContainer(LoadableMod<?> container)
	{
		this.checkState(EnumeratorState.DISCOVER, "registerEnabledContainer");
		
		this.disabledContainers.remove(container.getIdentifier());
		this.enabledContainers.put(container.getIdentifier(), container);
		
		this.observers.all().onRegisterEnabledContainer(this, container);
	}

	/**
	 * @param container
	 */
	protected void registerDisabledContainer(LoadableMod<?> container, DisabledReason reason)
	{
		this.checkState(EnumeratorState.DISCOVER, "registerDisabledContainer");
		
		this.enabledContainers.remove(container.getIdentifier());
		this.disabledContainers.put(container.getIdentifier(), new NonMod(container, false));

		this.observers.all().onRegisterDisabledContainer(this, container, reason);
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.core.PluggableEnumerator#addTweaksFrom(com.mumfrey.liteloader.core.TweakContainer)
	 */
	@Override
	public boolean registerTweakContainer(TweakContainer<File> container)
	{
		this.checkState(EnumeratorState.DISCOVER, "registerTweakContainer");
		
		if (!container.isEnabled(this.environment))
		{
			LiteLoaderLogger.info("Mod %s is disabled for profile %s, not injecting tranformers", container.getIdentifier(), this.environment.getProfile());
			return false;
		}

		this.tweakContainers.add(container);
		this.observers.all().onRegisterTweakContainer(this, container);
		return true;
	}

	/**
	 * @param tweakContainer
	 */
	private void addTweaksFrom(TweakContainer<File> tweakContainer)
	{
		this.checkState(EnumeratorState.INJECT, "addTweaksFrom");
		
		if (this.checkDependencies(tweakContainer))
		{
			if (tweakContainer.hasTweakClass())
			{
				this.addTweakFrom(tweakContainer);
			}
			
			if (tweakContainer.hasClassTransformers())
			{
				this.addClassTransformersFrom(tweakContainer, tweakContainer.getClassTransformerClassNames());
			}
		}
	}

	private void addTweakFrom(TweakContainer<File> container)
	{
		try
		{
			String tweakClass = container.getTweakClassName();
			int tweakPriority = container.getTweakPriority();
			LiteLoaderLogger.info("Mod file '%s' provides tweakClass '%s', adding to Launch queue with priority %d", container.getName(), tweakClass, tweakPriority);
			if (this.tweaker.addCascadedTweaker(tweakClass, tweakPriority))
			{
				LiteLoaderLogger.info("tweakClass '%s' was successfully added", tweakClass);
				container.injectIntoClassPath(this.classLoader, true);
				
				if (container.isExternalJar())
				{
					this.injectedTweaks.add(new NonMod(container, true));
				}
				
				String[] classPathEntries = container.getClassPathEntries();
				if (classPathEntries != null)
				{
					for (String classPathEntry : classPathEntries)
					{
						try
						{
							File classPathJar = new File(this.environment.getGameDirectory(), classPathEntry);
							URL classPathJarUrl = classPathJar.toURI().toURL();
							
							LiteLoaderLogger.info("Adding Class-Path entry: %s", classPathEntry); 
							LiteLoaderTweaker.addURLToParentClassLoader(classPathJarUrl);
							this.classLoader.addURL(classPathJarUrl);
						}
						catch (MalformedURLException ex) {}
					}
				}
			}
		}
		catch (MalformedURLException ex)
		{
		}
	}

	private void addClassTransformersFrom(TweakContainer<File> container, List<String> classTransformerClasses)
	{
		try
		{
			for (String classTransformerClass : classTransformerClasses)
			{
				LiteLoaderLogger.info("Mod file '%s' provides classTransformer '%s', adding to class loader", container.getName(), classTransformerClass);
				if (this.tweaker.getTransformerManager().injectTransformer(classTransformerClass))
				{
					LiteLoaderLogger.info("classTransformer '%s' was successfully added", classTransformerClass);
					container.injectIntoClassPath(this.classLoader, true);
				}
			}
		}
		catch (MalformedURLException ex)
		{
		}
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.core.PluggableEnumerator#registerMods(com.mumfrey.liteloader.core.LoadableMod, boolean)
	 */
	@Override
	public void registerModsFrom(LoadableMod<?> container, boolean registerContainer)
	{
		this.checkState(EnumeratorState.REGISTER, "registerModsFrom");
		
		if (this.disabledContainers.containsValue(container))
		{
			throw new IllegalArgumentException("Attempted to register mods from a disabled container '" + container.getName() + "'");
		}
		
		if (this.enumeratedContainers.contains(container))
		{
			// already handled this container
			return;
		}
		
		this.enumeratedContainers.add(container);
		
		LinkedList<Class<? extends LiteMod>> modClasses = LiteLoaderEnumerator.<LiteMod>getSubclassesFor(container, this.classLoader, LiteMod.class, this.supportedPrefixes);
		for (Class<? extends LiteMod> modClass : modClasses)
		{
			Mod mod = new Mod(container, modClass);
			this.registerMod(mod);
		}
		
		if (modClasses.size() > 0)
		{
			LiteLoaderLogger.info("Found %d potential matches", modClasses.size());

			this.disabledContainers.remove(container.getIdentifier());
			this.enabledContainers.put(container.getIdentifier(), container);
		}
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.interfaces.ModularEnumerator#registerMod(com.mumfrey.liteloader.interfaces.ModInfo)
	 */
	@Override
	public void registerMod(ModInfo<LoadableMod<?>> mod)
	{
		this.checkState(EnumeratorState.REGISTER, "registerMod");
		
		if (this.modsToLoad.contains(mod))
		{
			LiteLoaderLogger.warning("Mod name collision for mod with class '%s', maybe you have more than one copy?", mod.getModClassSimpleName());
		}
		
		this.modsToLoad.add(mod);
		
		this.observers.all().onModAdded(this, mod);
	}

	/**
	 * Enumerate classes on the classpath which are subclasses of the specified
	 * class
	 * 
	 * @param superClass
	 * @return
	 */
	private static <T> LinkedList<Class<? extends T>> getSubclassesFor(LoadableMod<?> container, ClassLoader classloader, Class<T> superClass, String[] supportedPrefixes)
	{
		LinkedList<Class<? extends T>> classes = new LinkedList<Class<? extends T>>();
		
		if (container != null)
		{
			try
			{
				for (String fullClassName : container.getContainedClassNames())
				{
					boolean isDefaultPackage = fullClassName.lastIndexOf('.') == -1;
					String className = isDefaultPackage ? fullClassName : fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
					if (supportedPrefixes == null || supportedPrefixes.length == 0 || LiteLoaderEnumerator.startsWithAny(className, supportedPrefixes))
					{
						LiteLoaderEnumerator.<T>checkAndAddClass(classloader, superClass, classes, fullClassName);
					}
				}
			}
			catch (OutdatedLoaderException ex)
			{
				classes.clear();
				LiteLoaderLogger.info("Error searching in '%s', missing API component '%s', your loader is probably out of date", container, ex.getMessage());
			}
			catch (Throwable th)
			{
				LiteLoaderLogger.warning(th, "Enumeration error");
			}
		}
		
		return classes;
	}

	/**
	 * @param classloader
	 * @param superClass
	 * @param classes
	 * @param className
	 * @throws OutdatedLoaderException 
	 */
	private static <T> void checkAndAddClass(ClassLoader classloader, Class<T> superClass, LinkedList<Class<? extends T>> classes, String className) throws OutdatedLoaderException
	{
		if (className.indexOf('$') > -1)
			return;
		
		try
		{
			Class<?> subClass = classloader.loadClass(className);
			
			if (subClass != null && !superClass.equals(subClass) && superClass.isAssignableFrom(subClass) && !subClass.isInterface() && !classes.contains(subClass))
			{
				@SuppressWarnings("unchecked")
				Class<? extends T> matchingClass = (Class<? extends T>)subClass;
				classes.add(matchingClass);
			}
		}
		catch (Throwable th)
		{
			if (th.getCause() != null)
			{
				String missingClassName = th.getCause().getMessage();
				if (th.getCause() instanceof NoClassDefFoundError && missingClassName != null)
				{
					if (missingClassName.startsWith("com/mumfrey/liteloader/"))
					{
						throw new OutdatedLoaderException(missingClassName.substring(missingClassName.lastIndexOf('/') + 1));
					}
					
//					String mahClassName = LiteLoaderEnumerator.getMissingAPIHandlerClass(classloader, className);
//					if (mahClassName != null)
//					{
//						LiteLoaderEnumerator.checkAndAddClass(classloader, superClass, classes, mahClassName);
//						return;
//					}
				}
			}
			
			LiteLoaderLogger.warning(th, "checkAndAddClass error while checking '%s'", className);
		}
	}

//	private static String getMissingAPIHandlerClass(ClassLoader classloader, String className)
//	{
//		String mahTypeDescriptor = Type.getDescriptor(MissingAPIHandlerClass.class);
//		
//		if (classloader instanceof LaunchClassLoader)
//		{
//			try
//			{
//				byte[] basicClass = ((LaunchClassLoader)classloader).getClassBytes(className);
//				ClassNode classNode = LiteLoaderEnumerator.readClass(basicClass);
//				
//				if (classNode.invisibleAnnotations != null)
//				{
//					for (AnnotationNode annotation : classNode.invisibleAnnotations)
//					{
//						if (mahTypeDescriptor.equals(annotation.desc))
//						{
//							return LiteLoaderEnumerator.<String>getAnnotationValue(annotation, "value");
//						}
//					}
//				}
//			}
//			catch (IOException ex)
//			{
//				ex.printStackTrace();
//			}
//		}
//		
//		return null;
//	}
//
//	/**
//	 * @param basicClass
//	 * @return
//	 */
//	private static ClassNode readClass(byte[] basicClass)
//	{
//		ClassReader classReader = new ClassReader(basicClass);
//		ClassNode classNode = new ClassNode();
//		classReader.accept(classNode, ClassReader.EXPAND_FRAMES);
//		return classNode;
//	}
//
//	/**
//	 * @param annotation
//	 * @param key
//	 * @return
//	 */
//	@SuppressWarnings("unchecked")
//	private static <T> T getAnnotationValue(AnnotationNode annotation, String key)
//	{
//		boolean getNextValue = false;
//		for (Object value : annotation.values)
//		{
//			if (getNextValue) return (T)value;
//			if (value.equals(key)) getNextValue = true;
//		}
//		return null;
//	}
	
	@Override
	public boolean checkAPIRequirements(LoadableMod<?> container)
	{
		boolean result = true;
		APIProvider apiProvider = this.environment.getAPIProvider();
		
		for (String identifier : container.getRequiredAPIs())
		{
			if (!apiProvider.isAPIAvailable(identifier))
			{
				container.registerMissingAPI(identifier);
				result = false;
			}
		}
		
		return result;
	}
	
	@SuppressWarnings("unchecked")
	public boolean checkDependencies(TweakContainer<File> tweakContainer)
	{
		if (tweakContainer instanceof LoadableMod)
		{
			return this.checkDependencies((LoadableMod<File>)tweakContainer);
		}
		
		return true;
	}

	@Override
	public boolean checkDependencies(LoadableMod<?> base)
	{
		if (base == null || !base.hasDependencies()) return true;
		
		HashSet<String> circularDependencySet = new HashSet<String>();
		circularDependencySet.add(base.getIdentifier());
		
		boolean result = this.checkDependencies(base, base, circularDependencySet);
		LiteLoaderLogger.info("Dependency check for %s %s", base.getIdentifier(), result ? "passed" : "failed");
		
		return result;
	}

	private boolean checkDependencies(LoadableMod<?> base, LoadableMod<?> container, Set<String> circularDependencySet)
	{
		if (container.getDependencies().size() == 0)
			return true;
		
		boolean result = true;
		
		for (String dependency : container.getDependencies())
		{
			if (!circularDependencySet.contains(dependency))
			{
				circularDependencySet.add(dependency);
				
				LoadableMod<?> dependencyContainer = this.getContainerById(dependency);
				if (dependencyContainer != LoadableMod.NONE)
				{
					if (this.environment.getEnabledModsList().isEnabled(this.environment.getProfile(), dependency))
					{
						result &= this.checkDependencies(base, dependencyContainer, circularDependencySet);
					}
					else
					{
//						LiteLoaderLogger.warning("Dependency %s required by %s is currently disabled", dependency, base.getIdentifier());
						base.registerMissingDependency(dependency);
						result = false;
					}
				}
				else
				{
//					LiteLoaderLogger.info("Dependency %s for %s is was not located, no container ", dependency, base.getIdentifier());
					base.registerMissingDependency(dependency);
					result = false;
				}
			}
		}
		
		return result;
	}

	public static String getModClassName(LiteMod mod)
	{
		return LiteLoaderEnumerator.getModClassName(mod.getClass());
	}

	public static String getModClassName(Class<? extends LiteMod> mod)
	{
		return mod.getSimpleName().substring(7);
	}

	private static boolean startsWithAny(String string, String[] candidates)
	{
		for (String candidate : candidates)
			if (string.startsWith(candidate)) return true;
		
		return false;
	}
}