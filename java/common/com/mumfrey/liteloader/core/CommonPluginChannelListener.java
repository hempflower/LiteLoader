package com.mumfrey.liteloader.core;

import java.util.List;

import com.mumfrey.liteloader.api.Listener;

/**
 * Common interface for the client/server plugin channel listeners. DO NOT IMPLEMENT THIS INTERFACE DIRECTLY, nothing will happen!
 * 
 * @author Adam Mummery-Smith
 */
public interface CommonPluginChannelListener extends Listener
{
	/**
	 * Return a list of the plugin channels the mod wants to register
	 * 
	 * @return
	 */
	public abstract List<String> getChannels();
}
