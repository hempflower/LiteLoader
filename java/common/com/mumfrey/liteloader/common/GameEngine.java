package com.mumfrey.liteloader.common;

import java.util.List;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;

/**
 * @author Adam Mummery-Smith
 *
 * @param <TClient> Type of the client runtime, "Minecraft" on client and null on the server
 * @param <TServer> Type of the server runtime, "IntegratedServer" on the client, "MinecraftServer" on the server 
 */
public interface GameEngine<TClient, TServer extends MinecraftServer>
{
	/**
	 * True if the environment is a client environment
	 */
	public abstract boolean isClient();
	
	/**
	 * True if the current environment is a server environment, always true on dedicated and true in single player
	 */
	public abstract boolean isServer();

	/**
	 * True if the client is "in game", always true on server 
	 */
	public abstract boolean isInGame();
	
	/**
	 * True if the game loop's "isRunning" flag is true
	 */
	public abstract boolean isRunning();

	/**
	 * True if the current world is single player, always false on the server
	 */
	public abstract boolean isSinglePlayer();

	/**
	 * Get the underlying client instance, returns a dummy on the server
	 */
	public abstract TClient getClient();
	
	/**
	 * Get the underlying server instance
	 */
	public abstract TServer getServer();

	/**
	 * Get the profiler instance
	 */
	public abstract Profiler getProfiler();

	/**
	 * Get the resource manager for the current environment, returns the SimpleReloadableResourceManager on client and ModResourceManager on the server
	 */
	public abstract IResourceManager getResourceManager();

	/**
	 * @param resourcePack
	 * @return
	 */
	public abstract boolean registerResourcePack(IResourcePack resourcePack);

	/**
	 * @param resourcePack
	 * @return
	 */
	public abstract boolean unRegisterResourcePack(IResourcePack resourcePack);

	/**
	 * Refresh resource pack list
	 * 
	 * @param force
	 */
	public abstract void refreshResources(boolean force);

	/**
	 * Get the keybinding list, only supported on client will throw an exception on the server
	 */
	public abstract List<KeyBinding> getKeyBindings();

	/**
	 * Set the keybinding list, only supported on client will throw an exception on the server
	 * 
	 * @param keyBindings
	 */
	public abstract void setKeyBindings(List<KeyBinding> keyBindings);
}
