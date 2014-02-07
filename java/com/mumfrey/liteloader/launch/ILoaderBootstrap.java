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
	 * @param modsToLoad
	 */
	public abstract void preInit(LaunchClassLoader classLoader, boolean loadTweaks, List<String> modsToLoad);
	
	/**
	 * Init, create the loader instance and load mods
	 * @param classLoader
	 */
	public abstract void init(LaunchClassLoader classLoader);
	
	/**
	 * Post-init, initialise loaded mods
	 */
	public abstract void postInit();

	public abstract void setBooleanProperty(String propertyName, boolean value);

	public abstract boolean getBooleanProperty(String propertyName);

	public abstract boolean getAndStoreBooleanProperty(String propertyName, boolean defaultValue);
}
