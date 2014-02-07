package com.mumfrey.liteloader.core;

import java.io.File;

import com.mumfrey.liteloader.LiteMod;

/**
 * Interface for the mod enumerator
 *
 * @author Adam Mummery-Smith
 */
public interface PluggableEnumerator
{
	public static final String MOD_CLASS_PREFIX = "LiteMod";
	
	/**
	 * @param module
	 */
	public abstract void registerModule(EnumeratorModule<?> module);
	
	/**
	 * @param container
	 * @return
	 */
	public abstract boolean isContainerEnabled(LoadableMod<?> container);

	/**
	 * @param container
	 */
	public abstract void registerTweakContainer(TweakContainer<File> container);
	
	/**
	 * @param container
	 * @param registerContainer
	 */
	public abstract void registerModsFrom(LoadableMod<?> container, boolean registerContainer);

	/**
	 * @param mod
	 * @param container
	 */
	public abstract void registerMod(Class<? extends LiteMod> mod, LoadableMod<?> container);

	/**
	 * @param propertyName
	 * @param value
	 */
	public abstract void setBooleanProperty(String propertyName, boolean value);

	/**
	 * @param propertyName
	 * @param defaultValue
	 * @return
	 */
	public abstract boolean getAndStoreBooleanProperty(String propertyName, boolean defaultValue);

	/**
	 * @param base
	 * @return
	 */
	public abstract boolean checkDependencies(LoadableMod<?> base);

	/**
	 * @param tweakContainer
	 * @return
	 */
	public abstract boolean checkDependencies(TweakContainer<File> tweakContainer);
}