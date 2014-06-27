package com.mumfrey.liteloader.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.INetHandler;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.login.INetHandlerLoginClient;
import net.minecraft.network.login.server.S02PacketLoginSuccess;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.INetHandlerPlayServer;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S3FPacketCustomPayload;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.world.WorldSettings;

import com.mojang.authlib.GameProfile;
import com.mumfrey.liteloader.core.ClientPluginChannels;
import com.mumfrey.liteloader.core.LiteLoader;
import com.mumfrey.liteloader.core.ServerPluginChannels;
import com.mumfrey.liteloader.transformers.event.EventInfo;

/**
 * Proxy class which handles the redirected calls from the injected callbacks and routes them to the
 * relevant liteloader handler classes. We do this rather than patching a bunch of bytecode into the packet
 * classes themselves because this is easier to maintain.
 * 
 * @author Adam Mummery-Smith
 */
public class CallbackProxyClient
{
	/**
	 * Tick clock, sent as a flag to the core onTick so that mods know it's a new tick
	 */
	private static boolean clock = false;
	
	private static ClientEvents events;
	
	public static void handleLoginSuccessPacket(INetHandler netHandler, S02PacketLoginSuccess packet)
	{
		((INetHandlerLoginClient)netHandler).handleLoginSuccess(packet);
		CallbackProxyClient.events.onPostLogin((INetHandlerLoginClient)netHandler, packet);
	}
	
	/**
	 * S02PacketChat::processPacket()
	 * 
	 * @param netHandler
	 * @param packet
	 */
	public static void handleChatPacket(INetHandler netHandler, S02PacketChat packet)
	{
		if (CallbackProxyClient.events.onChat(packet))
		{
			((INetHandlerPlayClient)netHandler).handleChat(packet);
		}
	}
	
	/**
	 * S02PacketChat::processPacket()
	 * 
	 * @param netHandler
	 * @param packet
	 */
	public static void handleServerChatPacket(INetHandler netHandler, C01PacketChatMessage packet)
	{
		if (CallbackProxyClient.events.onServerChat((INetHandlerPlayServer)netHandler, packet))
		{
			((INetHandlerPlayServer)netHandler).processChatMessage(packet);
		}
	}
	
	/**
	 * S01PacketJoinGame::processPacket()
	 * 
	 * @param netHandler
	 * @param packet
	 */
	public static void handleJoinGamePacket(INetHandler netHandler, S01PacketJoinGame packet)
	{
		if (CallbackProxyClient.events.onPreJoinGame(netHandler, packet))
		{
			((INetHandlerPlayClient)netHandler).handleJoinGame(packet);
			CallbackProxyClient.events.onJoinGame(netHandler, packet);
		}
	}
	
	/**
	 * S3FPacketCustomPayload::processPacket()
	 * 
	 * @param netHandler
	 * @param packet
	 */
	public static void handleCustomPayloadPacket(INetHandler netHandler, S3FPacketCustomPayload packet)
	{
		((INetHandlerPlayClient)netHandler).handleCustomPayload(packet);;
		
		ClientPluginChannels pluginChannels = LiteLoader.getClientPluginChannels();
		pluginChannels.onPluginChannelMessage(packet);
	}
	
	/**
	 * C17PacketCustomPayload::processPacket()
	 * 
	 * @param netHandler
	 * @param packet
	 */
	public static void handleCustomPayloadPacket(INetHandler netHandler, C17PacketCustomPayload packet)
	{
		((INetHandlerPlayServer)netHandler).processVanilla250Packet(packet);;
		
		ServerPluginChannels pluginChannels = LiteLoader.getServerPluginChannels();
		pluginChannels.onPluginChannelMessage((INetHandlerPlayServer)netHandler, packet);
	}
	
	public static void onStartupComplete(int ref, Minecraft minecraft)
	{
		CallbackProxyClient.events = ClientEvents.getInstance();
		CallbackProxyClient.events.onStartupComplete();
	}
	
	public static void onTimerUpdate(int ref)
	{
		CallbackProxyClient.events.onTimerUpdate();
	}
	
	public static void onAnimateTick(int ref)
	{
		CallbackProxyClient.clock = true;
	}
	
	public static void onTick(int ref)
	{
		if (ref == 2)
		{
			CallbackProxyClient.events.onTick(CallbackProxyClient.clock);
			CallbackProxyClient.clock = false;
		}
	}
	
	public static void onRender(int ref)
	{
		CallbackProxyClient.events.onRender();
	}
	
	public static void preRenderGUI(int ref)
	{
		if (ref == 1)
		{
			CallbackProxyClient.events.preRenderGUI(ref);
		}
	}
	
	public static void onSetupCameraTransform(int ref)
	{
		CallbackProxyClient.events.onSetupCameraTransform();
	}
	
	public static void postRenderEntities(int ref)
	{
		CallbackProxyClient.events.postRenderEntities();
	}
	
	public static void postRender(int ref)
	{
		CallbackProxyClient.events.postRender();
	}
	
	public static void onRenderHUD(int ref)
	{
		CallbackProxyClient.events.onRenderHUD();
	}
	
	public static void onRenderChat(int ref)
	{
		CallbackProxyClient.events.onRenderChat();
	}
	
	public static void postRenderChat(int ref)
	{
		if (ref == 10)
		{
			CallbackProxyClient.events.postRenderChat();
		}
	}
	
	public static void postRenderHUDandGUI(int ref)
	{
		if (ref == 2)
		{
			CallbackProxyClient.events.postRenderHUD();
			CallbackProxyClient.events.preRenderGUI(ref);
		}
	}
	
	public static void IntegratedServerCtor(int ref, IntegratedServer instance, Minecraft minecraft, String folderName, String worldName, WorldSettings worldSettings)
	{
		if (ref == 0)
		{
			CallbackProxyClient.events.onStartServer(instance, folderName, worldName, worldSettings);
		}
	}
	
	public static void onInitializePlayerConnection(int ref, ServerConfigurationManager scm, NetworkManager netManager, EntityPlayerMP player)
	{
		if (ref == 0)
		{
			CallbackProxyClient.events.onInitializePlayerConnection(scm, netManager, player);
		}
	}

	public static void onPlayerLogin(int ref, ServerConfigurationManager scm, EntityPlayerMP player)
	{
		if (ref == 0)
		{
			CallbackProxyClient.events.onPlayerLogin(scm, player);
		}
	}
	
	public static void onPlayerLogout(int ref, ServerConfigurationManager scm, EntityPlayerMP player)
	{
		if (ref == 0)
		{
			CallbackProxyClient.events.onPlayerLogout(scm, player);
		}
	}
	
	public static EntityPlayerMP onSpawnPlayer(EntityPlayerMP returnValue, int ref, ServerConfigurationManager scm, GameProfile profile)
	{
		if (ref == 0)
		{
			CallbackProxyClient.events.onSpawnPlayer(scm, returnValue, profile);
		}

		return returnValue;
	}

	public static EntityPlayerMP onRespawnPlayer(EntityPlayerMP returnValue, int ref, ServerConfigurationManager scm, EntityPlayerMP oldPlayer, int dimension, boolean won)
	{
		if (ref == 0)
		{
			CallbackProxyClient.events.onRespawnPlayer(scm, returnValue, oldPlayer, dimension, won);
		}

		return returnValue;
	}
	
	public static void onOutboundChat(int ref, C01PacketChatMessage packet, String message)
	{
		if (ref == 0)
		{
			CallbackProxyClient.events.onSendChatMessage(packet, message);
		}
	}
	
	public static void onOutboundChat(EventInfo<EntityClientPlayerMP> e, String message)
	{
		CallbackProxyClient.events.onSendChatMessage(e, message);
	}

    public static void onResize(EventInfo<Minecraft> e)
    {
    	CallbackProxyClient.events.onResize(e.getSource());
    }
}
