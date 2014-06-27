package com.mumfrey.liteloader.api;

import com.mumfrey.liteloader.interfaces.ModularEnumerator;
import com.mumfrey.liteloader.launch.LoaderEnvironment;
import com.mumfrey.liteloader.launch.LoaderProperties;

import net.minecraft.launchwrapper.LaunchClassLoader;

/**
 * LiteLoader Extensible API - Interface for objects which can enumerate mods in places
 * 
 * EnumeratorModules plug into the LoaderEnumerator and are used to discover mod containers in various
 * locations, for example searching in a specific folder for particular files.
 * 
 * @author Adam Mummery-Smith
 */
public interface EnumeratorModule
{
	/**
	 * @param environment Loader environment
	 * @param properties Loader properties
	 */
	public abstract void init(LoaderEnvironment environment, LoaderProperties properties);

	/**
	 * @param environment Loader environment
	 * @param properties Loader properties
	 */
	public abstract void writeSettings(LoaderEnvironment environment, LoaderProperties properties);
	
	/**
	 * Find loadable mods in this enumerator's domain, the enumerator module should call back against the enumerator
	 * itself to register containers it discovers using the registerModContainer() and registerTweakContainer()
	 * callbacks.
	 * 
	 * This method is called during loader PREINIT phase so **DO NOT USE ANY GAME CLASSES HERE**!
	 * 
	 * @param enumerator
	 * @param profile
	 */
	public abstract void enumerate(ModularEnumerator enumerator, String profile);
	
	/**
	 * The enumerator module should inject (as required) any discovered containers into the classpath
	 * 
	 * This method is called during the loader INIT phase
	 * 
	 * @param enumerator
	 * @param classLoader
	 */
	public abstract void injectIntoClassLoader(ModularEnumerator enumerator, LaunchClassLoader classLoader);

	/**
	 * The enumerator module should callback against the enumerator using the registerModsFrom() callback to
	 * register mods from discovered containers
	 * 
	 * This method is called during the loader INIT phase
	 * 
	 * @param enumerator
	 * @param classLoader
	 */
	public abstract void registerMods(ModularEnumerator enumerator, LaunchClassLoader classLoader);
}