package com.mumfrey.liteloader.interfaces;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.mumfrey.liteloader.LiteMod;

/**
 * Interface for the enumerator
 * 
 * @author Adam Mummery-Smith
 */
public interface LoaderEnumerator extends ModularEnumerator
{
	/**
	 * Perform pre-init tasks (container discovery)
	 */
	public abstract void onPreInit();
	
	/**
	 * Perform init tasks (injection and mod discovery)
	 */
	public abstract void onInit();
	
	/**
	 * Check API requirements for the supplied container
	 * 
	 * @param container
	 * @return
	 */
	public abstract boolean checkAPIRequirements(LoadableMod<?> container);

	/**
	 * Check intra-mod dependencies for the supplied container
	 * 
	 * @param base
	 * @return
	 */
	public abstract boolean checkDependencies(LoadableMod<?> base);

	/**
	 * Inflect mod identifier from the supplied mod class
	 * 
	 * @param modClass
	 * @return
	 */
	public abstract String getIdentifier(Class<? extends LiteMod> modClass);

	/**
	 * Get the container which the specified mod is loaded from
	 * 
	 * @param modClass
	 * @return
	 */
	public abstract LoadableMod<?> getContainer(Class<? extends LiteMod> modClass);

	/**
	 * Get the container for the specified mod identifier
	 * 
	 * @param identifier
	 * @return
	 */
	public abstract LoadableMod<?> getContainer(String identifier);

	/**
	 * Get all containers identified at discover-time as disabled
	 * 
	 * @return
	 */
	public abstract Collection<LoadableMod<?>> getDisabledContainers();
	
	/**
	 * @param modClass
	 * @param metaDataKey
	 * @param defaultValue
	 * @return
	 */
	public abstract String getModMetaData(Class<? extends LiteMod> modClass, String metaDataKey, String defaultValue);

	/**
	 * @return
	 */
	public abstract int modsToLoadCount();

	/**
	 * @return
	 */
	public abstract Collection<Class<? extends LiteMod>> getModsToLoad();
	
	/**
	 * @return
	 */
	public abstract List<Loadable<File>> getInjectedTweaks();
	
	/**
	 * @return
	 */
	public abstract Map<String, Map<String, String>> getSharedModList();
}
