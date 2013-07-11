package com.mumfrey.liteloader.permissions;

import java.util.List;

import com.mumfrey.liteloader.Permissible;

import net.minecraft.src.Minecraft;
import net.minecraft.src.NetHandler;
import net.minecraft.src.Packet1Login;

/**
 * Interface for permissions manager implementations
 * 
 * @author Adam Mummery-Smith
 */
public interface PermissionsManager
{
	/**
	 * Get the underlying permissions node for this manager for the specified mod
	 * 
	 * @param modName Mod to fetch permissions for
	 * @return
	 */
	public abstract Permissions getPermissions(Permissible mod);
	
	/**
	 * Get the time the permissions for the specified mod were last updated
	 * 
	 * @param mod Mod to check for
	 * @return Timestamp when the permissions were last updated
	 */
	public abstract Long getPermissionUpdateTime(Permissible mod);
	
	/**
	 * Handler for login event, should be called when connecting to a new server. Clears the replicated
	 * permissions ready to receive new permissions from the server
	 * 
	 * @param netHandler
	 * @param loginPacket
	 */
	public abstract void onLogin(NetHandler netHandler, Packet1Login loginPacket);

	/**
	 * Handler for tick event
	 * 
	 * @param minecraft
	 * @param partialTicks
	 * @param inGame
	 */
	public abstract void onTick(Minecraft minecraft, float partialTicks, boolean inGame);

	/**
	 * Handler for custom payload
	 * 
	 * @param channel
	 * @param length
	 * @param data
	 */
	public abstract void onCustomPayload(String channel, int length, byte[] data);
	
	/**
	 * LiteLoader support, gets the list of plugin channels to listen on
	 * 
	 * @return
	 */
	public abstract List<String> getChannels();
	
	/**
	 * Register a new event listener, the registered object will receive callbacks for permissions events
	 * 
	 * @param permissible
	 */
	public abstract void registerPermissible(Permissible permissible);
	
	/**
	 * Perform any necessary validation to check for a tamper condition, can and should be called from as
	 * many places as possible 
	 */
	public abstract void tamperCheck();
}
