package com.mumfrey.liteloader.client.transformers;

import com.mumfrey.liteloader.core.runtime.Obf;
import com.mumfrey.liteloader.transformers.PacketTransformer;

/**
 * Transformer for S02PacketLoginSuccess
 *
 * @author Adam Mummery-Smith
 */
public class LoginSuccessPacketTransformer extends PacketTransformer
{
	private static boolean injected = false;
	
	public LoginSuccessPacketTransformer()
	{
		super(Obf.S02PacketLoginSuccess, Obf.CallbackProxyClient.name, "handleLoginSuccessPacket", 1000);
	}
	
	@Override
	protected void notifyInjectionFailed()
	{
	}
	
	@Override
	protected void notifyInjected()
	{
		LoginSuccessPacketTransformer.injected = true;
	}
	
	public static boolean isInjected()
	{
		return LoginSuccessPacketTransformer.injected;
	}
}