package com.mumfrey.liteloader;

import java.util.List;

/**
 * Interface for mods which want to use plugin channels
 *
 * @author Adam Mummery-Smith
 */
public interface PluginChannelListener extends LoginListener
{
	/**
	 * Return a list of the plugin channels the mod wants to register
	 * 
	 * @return
	 */
	public abstract List<String> getChannels();
	
	/**
	 * Called when a custom payload packet arrives on a channel this mod has registered
	 * 
	 * @param channel Channel on which the custom payload was received
	 * @param length Length of the custom payload data
	 * @param data Custom payload data
	 */
	public abstract void onCustomPayload(String channel, int length, byte[] data);
}
