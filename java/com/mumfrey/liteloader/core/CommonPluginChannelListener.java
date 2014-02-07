package com.mumfrey.liteloader.core;

import java.util.List;

import com.mumfrey.liteloader.LiteMod;

/**
 * Common interface for the client/server plugin channel listeners. DO NOT IMPLEMENT THIS INTERFACE DIRECTLY, nothing will happen!
 * 
 * @author Adam Mummery-Smith
 */
public interface CommonPluginChannelListener extends LiteMod
{
	/**
	 * Return a list of the plugin channels the mod wants to register
	 * 
	 * @return
	 */
	public abstract List<String> getChannels();
}
