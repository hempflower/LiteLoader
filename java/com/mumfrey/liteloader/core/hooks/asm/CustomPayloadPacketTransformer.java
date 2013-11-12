package com.mumfrey.liteloader.core.hooks.asm;

/**
 * Transformer for Packet 250 (custom paylor)
 *
 * @author Adam Mummery-Smith
 */
public class CustomPayloadPacketTransformer extends PacketTransformer
{
	private static boolean injected = false;
	
	public CustomPayloadPacketTransformer()
	{
		// TODO Obfuscation 1.6.4
		super("net.minecraft.src.Packet250CustomPayload", "ea", "handleCustomPayloadPacket");
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
