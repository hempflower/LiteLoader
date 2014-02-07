package com.mumfrey.liteloader;

import net.minecraft.network.INetHandler;
import net.minecraft.network.play.server.S01PacketJoinGame;


/**
 * Interface for mods which wish to be notified when the player connects to a server (or local game)
 *
 * @author Adam Mummery-Smith
 */
public interface JoinGameListener extends LiteMod
{
	/**
	 * Called on join game
	 * 
	 * @param netHandler Net handler
	 * @param joinGamePacket Join game packet
	 */
	public abstract void onJoinGame(INetHandler netHandler, S01PacketJoinGame joinGamePacket);
}
