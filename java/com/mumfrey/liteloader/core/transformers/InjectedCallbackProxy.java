package com.mumfrey.liteloader.core.transformers;

import net.minecraft.client.Minecraft;
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
import com.mumfrey.liteloader.core.Events;
import com.mumfrey.liteloader.core.LiteLoader;
import com.mumfrey.liteloader.core.ServerPluginChannels;

/**
 * Proxy class which handles the redirected calls from the injected packet hooks and routes them to the
 * relevant liteloader handler classes. We do this rather than patching a bunch of bytecode into the packet
 * classes themselves because this is easier to maintain.
 * 
 * @author Adam Mummery-Smith
 */
public class InjectedCallbackProxy
{
	/**
	 * Initialisation done
	 */
	private static boolean initDone = false;
	
	/**
	 * Tick clock, sent as a flag to the core onTick so that mods know it's a new tick
	 */
	private static boolean clock = false;
	
	private static Events events;
	
	public static void handleLoginSuccessPacket(INetHandler netHandler, S02PacketLoginSuccess packet)
	{
		((INetHandlerLoginClient)netHandler).handleLoginSuccess(packet);
		InjectedCallbackProxy.events.onPostLogin((INetHandlerLoginClient)netHandler, packet);
	}
	
	/**
	 * S02PacketChat::processPacket()
	 * 
	 * @param netHandler
	 * @param packet
	 */
	public static void handleChatPacket(INetHandler netHandler, S02PacketChat packet)
	{
//		Events events = LiteLoader.getEvents();
		if (InjectedCallbackProxy.events.onChat(packet))
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
//		Events events = LiteLoader.getEvents();
		if (InjectedCallbackProxy.events.onServerChat((INetHandlerPlayServer)netHandler, packet))
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
//		Events events = LiteLoader.getEvents();
		if (InjectedCallbackProxy.events.onPreJoinGame(netHandler, packet))
		{
			((INetHandlerPlayClient)netHandler).handleJoinGame(packet);
			InjectedCallbackProxy.events.onJoinGame(netHandler, packet);
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
	
	public static void onTimerUpdate(int ref)
	{
		if (!InjectedCallbackProxy.initDone)
		{
			InjectedCallbackProxy.initDone = true;
			InjectedCallbackProxy.events = LiteLoader.getEvents();
			InjectedCallbackProxy.events.preBeginGame();
		}
		
		InjectedCallbackProxy.events.onTimerUpdate();
	}
	
	public static void onAnimateTick(int ref)
	{
		InjectedCallbackProxy.clock = true;
	}
	
	public static void onTick(int ref)
	{
		if (ref == 2)
		{
			InjectedCallbackProxy.events.onTick(InjectedCallbackProxy.clock);
		}
	}
	
	public static void onRender(int ref)
	{
		InjectedCallbackProxy.events.onRender();
	}
	
	public static void preRenderGUI(int ref)
	{
		if (ref == 1)
		{
			InjectedCallbackProxy.events.preRenderGUI(ref);
		}
	}
	
	public static void onSetupCameraTransform(int ref)
	{
		InjectedCallbackProxy.events.onSetupCameraTransform();
	}
	
	public static void postRenderEntities(int ref)
	{
		InjectedCallbackProxy.events.postRenderEntities();
	}
	
	public static void postRender(int ref)
	{
		InjectedCallbackProxy.events.postRender();
	}
	
	public static void onRenderHUD(int ref)
	{
		InjectedCallbackProxy.events.onRenderHUD();
	}
	
	public static void onRenderChat(int ref)
	{
		InjectedCallbackProxy.events.onRenderChat();
	}
	
	public static void postRenderChat(int ref)
	{
		if (ref == 10)
		{
			InjectedCallbackProxy.events.postRenderChat();
		}
	}
	
	public static void postRenderHUDandGUI(int ref)
	{
		if (ref == 2)
		{
			InjectedCallbackProxy.events.postRenderHUD();
			InjectedCallbackProxy.events.preRenderGUI(ref);
		}
	}
	
	public static void IntegratedServerCtor(int ref, IntegratedServer instance, Minecraft minecraft, String folderName, String worldName, WorldSettings worldSettings)
	{
		if (ref == 0)
		{
			InjectedCallbackProxy.events.onStartIntegratedServer(instance, folderName, worldName, worldSettings);
		}
	}
	
	public static void onInitializePlayerConnection(int ref, ServerConfigurationManager scm, NetworkManager netManager, EntityPlayerMP player)
	{
		if (ref == 0)
		{
			InjectedCallbackProxy.events.onInitializePlayerConnection(scm, netManager, player);
		}
	}

	public static void onPlayerLogin(int ref, ServerConfigurationManager scm, EntityPlayerMP player)
	{
		if (ref == 0)
		{
			InjectedCallbackProxy.events.onPlayerLogin(scm, player);
		}
	}
	
	public static void onPlayerLogout(int ref, ServerConfigurationManager scm, EntityPlayerMP player)
	{
		if (ref == 0)
		{
			InjectedCallbackProxy.events.onPlayerLogout(scm, player);
		}
	}
	
	public static EntityPlayerMP onSpawnPlayer(EntityPlayerMP returnValue, int ref, ServerConfigurationManager scm, GameProfile profile)
	{
		if (ref == 0)
		{
			InjectedCallbackProxy.events.onSpawnPlayer(scm, returnValue, profile);
		}

		return returnValue;
	}

	public static EntityPlayerMP onRespawnPlayer(EntityPlayerMP returnValue, int ref, ServerConfigurationManager scm, EntityPlayerMP oldPlayer, int dimension, boolean won)
	{
		if (ref == 0)
		{
			InjectedCallbackProxy.events.onRespawnPlayer(scm, returnValue, oldPlayer, dimension, won);
		}

		return returnValue;
	}
	
	public static void onOutboundChat(int ref, C01PacketChatMessage packet, String message)
	{
		if (ref == 0)
		{
			InjectedCallbackProxy.events.onSendChatMessage(packet, message);
		}
	}
}
