package com.mumfrey.liteloader.interfaces;

import java.io.File;

import com.mumfrey.liteloader.api.EnumeratorModule;
import com.mumfrey.liteloader.core.ModInfo;

/**
 * Interface for the mod enumerator
 *
 * @author Adam Mummery-Smith
 */
public interface ModularEnumerator
{
	/**
	 * Register a pluggable module into the enumerator
	 * 
	 * @param module
	 */
	public abstract void registerModule(EnumeratorModule module);
	
	/**
	 * @param container
	 */
	public abstract boolean registerModContainer(LoadableMod<?> container);

	/**
	 * @param container
	 */
	public abstract boolean registerTweakContainer(TweakContainer<File> container);
	
	/**
	 * @param container
	 * @param registerContainer
	 */
	public abstract void registerModsFrom(LoadableMod<?> container, boolean registerContainer);

	/**
	 * @param mod
	 */
	public abstract void registerMod(ModInfo<LoadableMod<?>> mod);
}