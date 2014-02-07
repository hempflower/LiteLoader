package com.mumfrey.liteloader.core.transformers;

import com.mumfrey.liteloader.core.runtime.Obf;
import com.mumfrey.liteloader.transformers.PacketTransformer;

/**
 * Transformer for S02PacketChat
 *
 * @author Adam Mummery-Smith
 */
public class ServerChatPacketTransformer extends PacketTransformer
{
	private static boolean injected = false;
	
	public ServerChatPacketTransformer()
	{
		super(Obf.C01PacketChatMessage, Obf.InjectedCallbackProxy.name, "handleServerChatPacket", 1000);
	}

	@Override
	protected void notifyInjectionFailed()
	{
	}
	
	@Override
	protected void notifyInjected()
	{
		ServerChatPacketTransformer.injected = true;
	}
	
	public static boolean isInjected()
	{
		return ServerChatPacketTransformer.injected;
	}
}
