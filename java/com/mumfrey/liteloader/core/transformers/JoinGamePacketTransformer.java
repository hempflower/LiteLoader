package com.mumfrey.liteloader.core.transformers;

import com.mumfrey.liteloader.core.runtime.Obf;
import com.mumfrey.liteloader.transformers.PacketTransformer;

/**
 * Transformer for S01PacketJoinGame
 *
 * @author Adam Mummery-Smith
 */
public class JoinGamePacketTransformer extends PacketTransformer
{
	private static boolean injected = false;
	
	public JoinGamePacketTransformer()
	{
		super(Obf.S01PacketJoinGame, Obf.InjectedCallbackProxy.name, "handleJoinGamePacket", 1000);
	}
	
	@Override
	protected void notifyInjectionFailed()
	{
	}
	
	@Override
	protected void notifyInjected()
	{
		JoinGamePacketTransformer.injected = true;
	}
	
	public static boolean isInjected()
	{
		return JoinGamePacketTransformer.injected;
	}
}