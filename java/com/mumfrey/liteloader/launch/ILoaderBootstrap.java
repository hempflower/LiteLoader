package com.mumfrey.liteloader.launch;

import java.util.List;

import net.minecraft.launchwrapper.LaunchClassLoader;

/**
 * Interface for the loader bootstrap, this is loaded in the parent classloader for convenience
 * otherwise it would be necessary to call the initialisation functions using reflection which
 * just gets boring very quickly.
 * 
 * @author Adam Mummery-Smith
 */
public interface ILoaderBootstrap
{
	/**
	 * Pre-init, perform mod file discovery and initial setup (eg. logger, properties)
	 * 
	 * @param classLoader
	 * @param loadTweaks
	 */
	public abstract void preInit(LaunchClassLoader classLoader, boolean loadTweaks);
	
	/**
	 * Init, create the loader instance and load mods
	 * 
	 * @param modsToLoad
	 * @param classLoader
	 */
	public abstract void init(List<String> modsToLoad, LaunchClassLoader classLoader);
	
	/**
	 * Post-init, initialise loaded mods
	 */
	public abstract void postInit();
}
