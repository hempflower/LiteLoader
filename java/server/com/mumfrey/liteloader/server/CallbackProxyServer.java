package com.mumfrey.liteloader.server;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.INetHandler;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.INetHandlerPlayServer;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.management.ServerConfigurationManager;

import com.mojang.authlib.GameProfile;
import com.mumfrey.liteloader.core.LiteLoader;
import com.mumfrey.liteloader.core.ServerPluginChannels;
import com.mumfrey.liteloader.launch.LiteLoaderTweaker;

/**
 * Proxy class which handles the redirected calls from the injected packet hooks and routes them to the
 * relevant liteloader handler classes. We do this rather than patching a bunch of bytecode into the packet
 * classes themselves because this is easier to maintain.
 * 
 * @author Adam Mummery-Smith
 */
public class CallbackProxyServer
{
	private static ServerEvents events;
	
	public static void init(MinecraftServer server)
	{
		LiteLoaderTweaker.init();
		LiteLoaderTweaker.postInit();
	}
	
	/**
	 * S02PacketChat::processPacket()
	 * 
	 * @param netHandler
	 * @param packet
	 */
	public static void handleServerChatPacket(INetHandler netHandler, C01PacketChatMessage packet)
	{
		System.err.println("handleServerChatPacket [" + packet.func_149439_c() + "]");

		if (CallbackProxyServer.events.onServerChat((INetHandlerPlayServer)netHandler, packet))
		{
			((INetHandlerPlayServer)netHandler).func_147354_a(packet); // processChatMessage - func_147354_a @ server side
		}
	}
//	
//	/**
//	 * S3FPacketCustomPayload::processPacket()
//	 * 
//	 * @param netHandler
//	 * @param packet
//	 */
//	public static void handleCustomPayloadPacket(INetHandler netHandler, S3FPacketCustomPayload packet)
//	{
//		((INetHandlerPlayClient)netHandler).handleCustomPayload(packet);;
//		
//		@SuppressWarnings("unchecked")
//		ClientPluginChannels<S3FPacketCustomPayload> pluginChannels = (ClientPluginChannels<S3FPacketCustomPayload>)LiteLoader.getClientPluginChannels();
//		pluginChannels.onPluginChannelMessage(packet);
//	}
	
	/**
	 * C17PacketCustomPayload::processPacket()
	 * 
	 * @param netHandler
	 * @param packet
	 */
	public static void handleCustomPayloadPacket(INetHandler netHandler, C17PacketCustomPayload packet)
	{
		((INetHandlerPlayServer)netHandler).func_147349_a(packet); // processVanilla250Packet - func_147349_a @ server side
		
		System.err.println("handleCustomPayloadPacket [" + packet.func_149559_c() + "]");

		ServerPluginChannels pluginChannels = LiteLoader.getServerPluginChannels();
		pluginChannels.onPluginChannelMessage((INetHandlerPlayServer)netHandler, packet);
	}
	
	public static boolean onStartupComplete(boolean returnValue, int ref, DedicatedServer server)
	{
		System.err.println("onStartupComplete [" + returnValue + "] @" + ref);
		if (returnValue)
		{
			CallbackProxyServer.events = ServerEvents.getInstance();
			CallbackProxyServer.events.onStartupComplete();
		}
		
		return returnValue;
	}
	
//	public static void onTick(int ref)
//	{
//		if (ref == 2)
//		{
//			CallbackProxyServer.events.onTick(CallbackProxyServer.clock);
//			CallbackProxyServer.clock = false;
//		}
//	}
	
	public static void onInitializePlayerConnection(int ref, ServerConfigurationManager scm, NetworkManager netManager, EntityPlayerMP player)
	{
		if (ref == 0)
		{
			System.err.println("onInitializePlayerConnection");
			CallbackProxyServer.events.onInitializePlayerConnection(scm, netManager, player);
		}
	}

	public static void onPlayerLogin(int ref, ServerConfigurationManager scm, EntityPlayerMP player)
	{
		if (ref == 0)
		{
			System.err.println("onPlayerLogin " + player);
			CallbackProxyServer.events.onPlayerLogin(scm, player);
		}
	}
	
	public static void onPlayerLogout(int ref, ServerConfigurationManager scm, EntityPlayerMP player)
	{
		if (ref == 0)
		{
			System.err.println("onPlayerLogout " + player);
			CallbackProxyServer.events.onPlayerLogout(scm, player);
		}
	}
	
	public static EntityPlayerMP onSpawnPlayer(EntityPlayerMP returnValue, int ref, ServerConfigurationManager scm, GameProfile profile)
	{
		if (ref == 0)
		{
			System.err.println("onSpawnPlayer " + profile.getName() + " [" + profile.getId() + "]");
			CallbackProxyServer.events.onSpawnPlayer(scm, returnValue, profile);
		}

		return returnValue;
	}

	public static EntityPlayerMP onRespawnPlayer(EntityPlayerMP returnValue, int ref, ServerConfigurationManager scm, EntityPlayerMP oldPlayer, int dimension, boolean won)
	{
		if (ref == 0)
		{
			System.err.println("onRespawnPlayer " + oldPlayer);
			CallbackProxyServer.events.onRespawnPlayer(scm, returnValue, oldPlayer, dimension, won);
		}

		return returnValue;
	}
}
