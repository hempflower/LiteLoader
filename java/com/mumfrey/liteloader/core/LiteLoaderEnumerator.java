package com.mumfrey.liteloader.core;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

import com.mumfrey.liteloader.LiteMod;
import com.mumfrey.liteloader.core.exceptions.OutdatedLoaderException;
import com.mumfrey.liteloader.launch.LiteLoaderTweaker;
import com.mumfrey.liteloader.util.log.LiteLoaderLogger;

/**
 * The enumerator performs all mod discovery functions for LiteLoader, this includes locating mod files to load
 * as well as searching for mod classes within the class path and discovered mod files.
 *
 * @author Adam Mummery-Smith
 */
public class LiteLoaderEnumerator implements PluggableEnumerator
{
	private static final String OPTION_SEARCH_MODS      = "search.mods";
	private static final String OPTION_SEARCH_JAR       = "search.jar";
	private static final String OPTION_SEARCH_CLASSPATH = "search.classpath";

	/**
	 * Reference to the bootstrap agent 
	 */
	private final LiteLoaderBootstrap bootstrap;

	/**
	 * Reference to the launch classloader
	 */
	private final LaunchClassLoader classLoader;

	/**
	 * 
	 */
	private final EnabledModsList enabledModsList;
	
	/**
	 * Classes to load, mapped by class name 
	 */
	private final Map<String, Class<? extends LiteMod>> modsToLoad = new HashMap<String, Class<? extends LiteMod>>();
	
	/**
	 * Mod containers which are disabled 
	 */
	private final Map<String, LoadableMod<?>> disabledMods = new HashMap<String, LoadableMod<?>>();

	/**
	 * Mapping of identifiers to mod containers 
	 */
	private final Map<String, LoadableMod<?>> containers = new HashMap<String, LoadableMod<?>>();
	
	/**
	 * Mapping of mods to mod containers 
	 */
	private final Map<String, LoadableMod<?>> modContainers = new HashMap<String, LoadableMod<?>>();
	
	/**
	 * Tweaks to inject 
	 */
	private final List<TweakContainer<File>> tweakContainers = new ArrayList<TweakContainer<File>>();
	
	/**
	 * Other tweak-containing jars which we have injected 
	 */
	private final List<Loadable<File>> injectedTweaks = new ArrayList<Loadable<File>>();
	
	/**
	 * 
	 */
	private final List<EnumeratorModule<?>> modules = new ArrayList<EnumeratorModule<?>>();
	
	private boolean searchModsFolder = true;
	private boolean searchProtectionDomain = true;
	private boolean searchClassPath = true;
	
	/**
	 * @param classLoader
	 * @param enabledModsList 
	 * @param loadTweaks
	 * @param properties
	 * @param gameFolder
	 */
	public LiteLoaderEnumerator(LiteLoaderBootstrap bootstrap, LaunchClassLoader classLoader, EnabledModsList enabledModsList, boolean loadTweaks)
	{
		this.bootstrap       = bootstrap;
		this.classLoader     = classLoader;
		this.enabledModsList = enabledModsList;
		
		this.initModules(loadTweaks);
		
		// Initialise the shared mod list if we haven't already
        this.getSharedModList();
	}

	/**
	 * Initialise the discovery modules
	 * 
	 * @param loadTweaks 
	 */
	private void initModules(boolean loadTweaks)
	{
		// Read the discovery settings from the properties 
		this.readSettings();
		
		if (this.searchClassPath)
		{
			this.registerModule(new EnumeratorModuleClassPath(loadTweaks));
		}
		
		if (this.searchProtectionDomain)
		{
			LiteLoaderLogger.info("Protection domain searching is no longer required or supported, protection domain search has been disabled");
			this.searchProtectionDomain = false;
//			this.registerModule(new EnumeratorModuleProtectionDomain(loadTweaks));
		}
		
		if (this.searchModsFolder)
		{
			File modsFolder = this.bootstrap.getModsFolder();
			this.registerModule(new EnumeratorModuleFolder(modsFolder, loadTweaks, true));
			
			File versionedModsFolder = this.bootstrap.getVersionedModsFolder();
			this.registerModule(new EnumeratorModuleFolder(versionedModsFolder, loadTweaks, false));
		}
		
		this.writeSettings();
	}

	/**
	 * Initialise the "shared" mod list if it's not already been created
	 * @return 
	 */
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
	
	/**
	 * Get the discovery settings from the properties file
	 */
	private void readSettings()
	{
		this.searchModsFolder       = this.bootstrap.getAndStoreBooleanProperty(OPTION_SEARCH_MODS,      true);
		this.searchProtectionDomain = this.bootstrap.getAndStoreBooleanProperty(OPTION_SEARCH_JAR,       false);
		this.searchClassPath        = this.bootstrap.getAndStoreBooleanProperty(OPTION_SEARCH_CLASSPATH, true);
		
		if (!this.searchModsFolder && !this.searchProtectionDomain && !this.searchClassPath)
		{
			LiteLoaderLogger.warning("Invalid configuration, no search locations defined. Enabling all search locations.");
			
			this.searchModsFolder       = true;
			this.searchClassPath        = true;
		}
	}

	/**
	 * Write settings
	 */
	private void writeSettings()
	{
		this.bootstrap.setBooleanProperty(OPTION_SEARCH_MODS,      this.searchModsFolder);
		this.bootstrap.setBooleanProperty(OPTION_SEARCH_JAR,       this.searchProtectionDomain);
		this.bootstrap.setBooleanProperty(OPTION_SEARCH_CLASSPATH, this.searchClassPath);
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.core.PluggableEnumerator#registerModule(com.mumfrey.liteloader.core.EnumeratorModule)
	 */
	@Override
	public void registerModule(EnumeratorModule<?> module)
	{
		if (module != null && !this.modules.contains(module))
		{
			LiteLoaderLogger.info("Registering %s: %s", module.getClass().getSimpleName(), module);
			this.modules.add(module);
			module.init(this);
		}
	}
	
	/**
	 * @param propertyName
	 * @param defaultValue
	 * @return
	 */
	@Override
	public boolean getAndStoreBooleanProperty(String propertyName, boolean defaultValue)
	{
		return this.bootstrap.getAndStoreBooleanProperty(propertyName, defaultValue);
	}

	/**
	 * @param propertyName
	 * @param value
	 */
	@Override
	public void setBooleanProperty(String propertyName, boolean value)
	{
		this.bootstrap.setBooleanProperty(propertyName, value);
	}

	/**
	 * Get the list of all enumerated mod classes to load
	 */
	public Collection<Class<? extends LiteMod>> getModsToLoad()
	{
		return this.modsToLoad.values();
	}
	
	/**
	 * @return
	 */
	public Collection<LoadableMod<?>> getDisabledMods()
	{
		return this.disabledMods.values();
	}
	
	/**
	 * Get the list of injected tweak containers
	 */
	public List<Loadable<File>> getInjectedTweaks()
	{
		return this.injectedTweaks;
	}

	/**
	 * Get the number of mods to load
	 */
	public int modsToLoadCount()
	{
		return this.modsToLoad.size();
	}
	
	/**
	 * @return
	 */
	public boolean hasModsToLoad()
	{
		return this.modsToLoad.size() > 0;
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
		return this.getModContainer(modClass).getMetaValue(metaDataKey, defaultValue);
	}
	
	/**
	 * @param mod
	 * @return
	 */
	public LoadableMod<?> getContainer(String identifier)
	{
		return this.containers.get(identifier);
	}
	
	/**
	 * @param mod
	 * @return
	 */
	public LoadableMod<?> getModContainer(Class<? extends LiteMod> modClass)
	{
		return this.modContainers.containsKey(modClass.getSimpleName()) ? this.modContainers.get(modClass.getSimpleName()) : LoadableMod.NONE;
	}

	/**
	 * Get the mod identifier (metadata key), this is used for versioning, exclusivity, and enablement checks
	 * 
	 * @param modClass
	 * @return
	 */
	public String getModIdentifier(Class<? extends LiteMod> modClass)
	{
		String modClassName = modClass.getSimpleName();
		if (!this.modContainers.containsKey(modClassName)) return LiteLoaderEnumerator.getModClassName(modClass);
		return this.modContainers.get(modClassName).getIdentifier();
	}

	/**
	 * Enumerate the "mods" folder to find mod files
	 */
	protected void discoverMods()
	{
		for (EnumeratorModule<?> module : this.modules)
		{
			module.enumerate(this, this.enabledModsList, this.bootstrap.getProfile());
		}
		
		for (TweakContainer<File> tweakContainer : this.tweakContainers)
		{
			this.addTweaksFrom(tweakContainer);
		}
	}
	
	/**
	 * Enumerate class path and discovered mod files to find mod classes
	 */
	protected void discoverModClasses()
	{
		try
		{
			for (EnumeratorModule<?> module : this.modules)
			{
				module.injectIntoClassLoader(this, this.classLoader, this.enabledModsList, this.bootstrap.getProfile());
			}

			for (EnumeratorModule<?> module : this.modules)
			{
				module.registerMods(this, this.classLoader);
			}

			LiteLoaderLogger.info("Mod class discovery completed");
		}
		catch (Throwable th)
		{
			LiteLoaderLogger.warning(th, "Mod class discovery failed");
			return;
		}
	}

	@Override
	public boolean isContainerEnabled(LoadableMod<?> container)
	{
		if (container != null)
		{
			if (container.isEnabled(this.enabledModsList, this.bootstrap.getProfile()) && this.checkDependencies(container))
			{
				this.containers.put(container.getIdentifier(), container);
			}
			else
			{
				this.disabledMods.put(container.getIdentifier(), container);
				return false;
			}
		}

		return true;
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.core.PluggableEnumerator#addTweaksFrom(com.mumfrey.liteloader.core.TweakContainer)
	 */
	@Override
	public void registerTweakContainer(TweakContainer<File> container)
	{
		if (!container.isEnabled(this.enabledModsList, this.bootstrap.getProfile()))
		{
			LiteLoaderLogger.info("Mod %s is disabled for profile %s, not injecting tranformers", container.getIdentifier(), this.bootstrap.getProfile());
			return;
		}

		this.tweakContainers.add(container);
	}

	/**
	 * @param tweakContainer
	 */
	private void addTweaksFrom(TweakContainer<File> tweakContainer)
	{
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
			if (LiteLoaderTweaker.addCascadedTweaker(tweakClass, tweakPriority))
			{
				LiteLoaderLogger.info("tweakClass '%s' was successfully added", tweakClass);
				container.injectIntoClassPath(this.classLoader, true);
				
				if (container.isExternalJar())
				{
					this.injectedTweaks.add(container);
				}
				
				String[] classPathEntries = container.getClassPathEntries();
				if (classPathEntries != null)
				{
					for (String classPathEntry : classPathEntries)
					{
						try
						{
							File classPathJar = new File(this.bootstrap.getGameDirectory(), classPathEntry);
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
				if (LiteLoaderTweaker.getTransformerManager().injectTransformer(classTransformerClass))
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
		LinkedList<Class<? extends LiteMod>> modClasses = LiteLoaderEnumerator.<LiteMod>getSubclassesFor(container, this.classLoader, LiteMod.class, PluggableEnumerator.MOD_CLASS_PREFIX);
		for (Class<? extends LiteMod> mod : modClasses)
		{
			this.registerMod(mod, registerContainer ? container : null);
		}
		
		if (modClasses.size() > 0)
		{
			LiteLoaderLogger.info("Found %s potential matches", modClasses.size());
		}
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.core.PluggableEnumerator#addMod(java.lang.Class, com.mumfrey.liteloader.core.LoadableMod)
	 */
	@Override
	public void registerMod(Class<? extends LiteMod> mod, LoadableMod<?> container)
	{
		if (this.modsToLoad.containsKey(mod.getSimpleName()))
		{
			LiteLoaderLogger.warning("Mod name collision for mod with class '%s', maybe you have more than one copy?", mod.getSimpleName());
		}
		
		this.modsToLoad.put(mod.getSimpleName(), mod);
		if (container != null)
		{
			this.modContainers.put(mod.getSimpleName(), container);
			container.addContainedMod(LiteLoaderEnumerator.getModClassName(mod));
		}
	}

	/**
	 * Enumerate classes on the classpath which are subclasses of the specified
	 * class
	 * 
	 * @param superClass
	 * @return
	 */
	private static <T> LinkedList<Class<? extends T>> getSubclassesFor(LoadableMod<?> container, ClassLoader classloader, Class<T> superClass, String prefix)
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
					if (prefix == null || className.startsWith(prefix))
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
				}
			}
			
			LiteLoaderLogger.warning(th, "checkAndAddClass error");
		}
	}

	@Override
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
				
				LoadableMod<?> dependencyContainer = this.getContainer(dependency);
				if (dependencyContainer != null)
				{
					if (this.enabledModsList.isEnabled(this.bootstrap.getProfile(), dependency))
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
}
