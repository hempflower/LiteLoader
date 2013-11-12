package com.mumfrey.liteloader.core.hooks.asm;

/**
 * Transformer for Packet 3 (chat)
 *
 * @author Adam Mummery-Smith
 */
public class ChatPacketTransformer extends PacketTransformer
{
	private static boolean injected = false;
	
	public ChatPacketTransformer()
	{
		// TODO Obfuscation 1.6.4
		super("net.minecraft.src.Packet3Chat", "dm", "handleChatPacket");
	}
	
	@Override
	protected void notifyInjected()
	{
		ChatPacketTransformer.injected = true;
	}
	
	public static boolean isInjected()
	{
		return ChatPacketTransformer.injected;
	}
}
