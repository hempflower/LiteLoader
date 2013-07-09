package com.mumfrey.liteloader;

import net.minecraft.src.ChatMessageComponent;

/**
 * Interface for mods which receive inbound chat
 *
 * @author Adam Mummery-Smith
 */
public interface ChatListener extends LiteMod
{
	/**
	 * Handle an inbound message
	 * 
	 * @param chat ChatMessageComponent parsed from the chat packet
	 * @param message Chat message parsed from the chat message component
	 */
	public abstract void onChat(ChatMessageComponent chat, String message);
}
