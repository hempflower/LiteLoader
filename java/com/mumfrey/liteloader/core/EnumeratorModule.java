package com.mumfrey.liteloader.core;

import net.minecraft.launchwrapper.LaunchClassLoader;

/**
 * Interface for objects which can enumerate mods in places
 * 
 * @author Adam Mummery-Smith
 *
 * @param <T>
 */
public interface EnumeratorModule<T>
{
	/**
	 * @param enumerator
	 */
	public abstract void init(PluggableEnumerator enumerator);

	/**
	 * @param enumerator
	 */
	public abstract void writeSettings(PluggableEnumerator enumerator);
	
	/**
	 * Find loadable mods in this enumerator's domain
	 * 
	 * @param enumerator
	 * @param enabledModsList
	 * @param profile
	 */
	public abstract void enumerate(PluggableEnumerator enumerator, EnabledModsList enabledModsList, String profile);
	
	/**
	 * @param enumerator
	 * @param classLoader
	 * @param enabledModsList
	 * @param profile
	 */
	public abstract void injectIntoClassLoader(PluggableEnumerator enumerator, LaunchClassLoader classLoader, EnabledModsList enabledModsList, String profile);

	/**
	 * @param enumerator
	 * @param classLoader
	 */
	public abstract void registerMods(PluggableEnumerator enumerator, LaunchClassLoader classLoader);
}