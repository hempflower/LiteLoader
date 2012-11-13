package com.mumfrey.liteloader;

import net.minecraft.src.NetHandler;
import net.minecraft.src.Packet1Login;

/**
 * Interface for mods which wish to be notified when the player connects to a server (or local game)
 *
 * @author Adam Mummery-Smith
 */
public interface PreLoginListener extends LiteMod
{
	/**
	 * Called on login
	 * 
	 * @param netHandler Net handler
	 * @param loginPacket Login packet
	 */
	public abstract boolean onPreLogin(NetHandler netHandler, Packet1Login loginPacket);
}
