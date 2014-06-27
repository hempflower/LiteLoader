package com.mumfrey.liteloader.client.transformers;

import com.mumfrey.liteloader.core.runtime.Obf;
import com.mumfrey.liteloader.transformers.PacketTransformer;

/**
 * Transformer for S02PacketChat
 *
 * @author Adam Mummery-Smith
 */
public class ChatPacketTransformer extends PacketTransformer
{
	private static boolean injected = false;
	
	public ChatPacketTransformer()
	{
		super(Obf.S02PacketChat, Obf.CallbackProxyClient.name, "handleChatPacket", 1000);
	}

	@Override
	protected void notifyInjectionFailed()
	{
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
