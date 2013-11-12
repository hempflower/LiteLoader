package com.mumfrey.liteloader.core.hooks.asm;

import com.mumfrey.liteloader.core.Events;
import com.mumfrey.liteloader.core.LiteLoader;
import com.mumfrey.liteloader.core.PluginChannels;

import net.minecraft.src.NetHandler;
import net.minecraft.src.Packet1Login;
import net.minecraft.src.Packet250CustomPayload;
import net.minecraft.src.Packet3Chat;

/**
 * Proxy class which handles the redirected calls from the injected packet hooks and routes them to the
 * relevant liteloader handler classes. We do this rather than patching a bunch of bytecode into the packet
 * classes themselves because this is easier to maintain.
 * 
 * @author Adam Mummery-Smith
 */
public class ASMHookProxy
{
	/**
	 * Packet3Chat::processPacket()
	 * 
	 * @param netHandler
	 * @param packet
	 */
	public void handleChatPacket(NetHandler netHandler, Packet3Chat packet)
	{
		Events events = LiteLoader.getEvents();
		if (events.onChat(packet))
		{
			netHandler.handleChat(packet);
		}
	}
	
	/**
	 * Packet3Chat::processPacket()
	 * 
	 * @param netHandler
	 * @param packet
	 */
	public void handleLoginPacket(NetHandler netHandler, Packet1Login packet)
	{
		Events events = LiteLoader.getEvents();
		if (events.onPreLogin(netHandler, packet))
		{
			netHandler.handleLogin(packet);
			events.onConnectToServer(netHandler, packet);
		}
	}
	
	/**
	 * Packet3Chat::processPacket()
	 * 
	 * @param netHandler
	 * @param packet
	 */
	public void handleCustomPayloadPacket(NetHandler netHandler, Packet250CustomPayload packet)
	{
		netHandler.handleCustomPayload(packet);
		
		PluginChannels pluginChannels = LiteLoader.getPluginChannels();
		pluginChannels.onPluginChannelMessage(packet);
	}
}
