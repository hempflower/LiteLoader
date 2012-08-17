package com.mumfrey.liteloader;

/**
 * Interface for mods which receive inbound chat
 *
 * @author Adam Mummery-Smith
 */
public interface ChatListener extends LiteMod
{
	/**
	 * Handle an inbound message
	 * 
	 * @param message
	 */
	public abstract void onChat(String message);
}
