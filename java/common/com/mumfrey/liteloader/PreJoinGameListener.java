package com.mumfrey.liteloader;

import net.minecraft.network.INetHandler;
import net.minecraft.network.play.server.S01PacketJoinGame;


/**
 * Interface for mods which wish to be notified when the player connects to a server (or local game)
 *
 * @author Adam Mummery-Smith
 */
public interface PreJoinGameListener extends LiteMod
{
	/**
	 * Called on login
	 * 
	 * @param netHandler Net handler
	 * @param joinGamePacket Join game packet
	 * 
	 * @return true to cancel the event
	 * @deprecated this event's return code is not compatible with other events expressing the same pattern, 
	 *     it will be replaced in the next release with a method whose return value is boolean to NOT cancel
	 */
	@Deprecated
	public abstract boolean onPreJoinGame(INetHandler netHandler, S01PacketJoinGame joinGamePacket);
}
