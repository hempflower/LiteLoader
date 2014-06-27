package com.mumfrey.liteloader.client.transformers;

import com.mumfrey.liteloader.core.runtime.Obf;
import com.mumfrey.liteloader.transformers.PacketTransformer;

/**
 * Transformer for S3FPacketCustomPayload
 *
 * @author Adam Mummery-Smith
 */
public class CustomPayloadPacketTransformer extends PacketTransformer
{
	private static boolean injected = false;
	
	public CustomPayloadPacketTransformer()
	{
		super(Obf.S3FPacketCustomPayload, Obf.CallbackProxyClient.name, "handleCustomPayloadPacket", 1000);
	}

	@Override
	protected void notifyInjectionFailed()
	{
	}
	
	@Override
	protected void notifyInjected()
	{
		CustomPayloadPacketTransformer.injected = true;
	}
	
	public static boolean isInjected()
	{
		return CustomPayloadPacketTransformer.injected;
	}
}
