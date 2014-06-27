package com.mumfrey.liteloader.server.transformers;

import com.mumfrey.liteloader.core.runtime.Obf;
import com.mumfrey.liteloader.transformers.PacketTransformer;

/**
 * Transformer for C17PacketCustomPayload
 *
 * @author Adam Mummery-Smith
 */
public class ServerCustomPayloadPacketTransformer extends PacketTransformer
{
	private static boolean injected = false;
	
	public ServerCustomPayloadPacketTransformer()
	{
		super(Obf.C17PacketCustomPayload, Obf.CallbackProxyServer.name, "handleCustomPayloadPacket", 1000);
	}

	@Override
	protected void notifyInjectionFailed()
	{
	}
	
	@Override
	protected void notifyInjected()
	{
		ServerCustomPayloadPacketTransformer.injected = true;
	}
	
	public static boolean isInjected()
	{
		return ServerCustomPayloadPacketTransformer.injected;
	}
}
