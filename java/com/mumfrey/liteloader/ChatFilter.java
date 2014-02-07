package com.mumfrey.liteloader;

import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.IChatComponent;


/**
 * Interface for mods which can filter inbound chat
 *
 * @author Adam Mummery-Smith
 */
public interface ChatFilter extends LiteMod
{
	/**
	 * Chat filter function, return false to filter this packet, true to pass the packet
	 * 
	 * @param chatPacket Chat packet to examine
	 * @param chat ChatMessageComponent parsed from the chat packet
	 * @param message Chat message parsed from the chat message component
	 * @return True to keep the packet, false to discard
	 */
	public abstract boolean onChat(S02PacketChat chatPacket, IChatComponent chat, String message);
}
