package com.mumfrey.liteloader.core.overlays;

import java.util.List;

import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.util.Timer;

/**
 * Interface containing injected accessors which are provided by MinecraftOverlay
 *
 * @author Adam Mummery-Smith
 */
public interface IMinecraft
{
	/**
	 * Get the timer instance
	 */
	public abstract Timer getTimer();

	/**
	 * Get the "running" flag
	 */
	public abstract boolean isRunning();

	/**
	 * Get the default resource packs set
	 */
	public abstract List<IResourcePack> getDefaultResourcePacks();

	/**
	 * Resize the window
	 * 
	 * @param width
	 * @param height
	 */
	public abstract void setSize(int width, int height);
	
	/**
	 * Get the current server address (from connection)
	 */
	public abstract String getServerName();
	
	/**
	 * Get the current server port (from connection)
	 */
	public abstract int getServerPort();
	
	/**
	 * @return
	 */
	public abstract ServerData getCurrentServerData();
}
