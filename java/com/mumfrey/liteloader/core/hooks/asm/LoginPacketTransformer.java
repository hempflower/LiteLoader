package com.mumfrey.liteloader.core.hooks.asm;

/**
 * Transformer for Packet 1 (login)
 *
 * @author Adam Mummery-Smith
 */
public class LoginPacketTransformer extends PacketTransformer
{
	private static boolean injected = false;
	
	public LoginPacketTransformer()
	{
		// TODO Obfuscation 1.6.4
		super("net.minecraft.src.Packet1Login", "ep", "handleLoginPacket");
	}
	
	@Override
	protected void notifyInjected()
	{
		LoginPacketTransformer.injected = true;
	}
	
	public static boolean isInjected()
	{
		return LoginPacketTransformer.injected;
	}
}