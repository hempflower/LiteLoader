package com.mumfrey.liteloader;

/**
 * Base interface for mods
 *
 * @author Adam Mummery-Smith
 */
public interface LiteMod
{
	/**
	 * Get the mod's display name
	 * 
	 * @return display name
	 */
	public abstract String getName();
	
	/**
	 * Get the mod version string
	 * 
	 * @return
	 */
	public abstract String getVersion();
	
	/**
	 * Do startup stuff here, minecraft is not fully initialised when this function is called so mods *must not*
	 * interact with minecraft in any way here
	 */
	public abstract void init();
}
