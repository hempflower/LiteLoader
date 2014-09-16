package com.mumfrey.liteloader.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
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
import com.mumfrey.liteloader.transformers.event.ReturnEventInfo;

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

	private static boolean fboEnabled;

	private static boolean renderingFBO;
	
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
	
	public static void onStartupComplete(EventInfo<Minecraft> e)
	{
		CallbackProxyClient.events = ClientEvents.getInstance();
		
		if (CallbackProxyClient.events == null)
		{
			throw new RuntimeException("LiteLoader failed to start up properly. The game is in an unstable state and must shut down now. Check the developer log for startup errors");
		}
		
		CallbackProxyClient.events.onStartupComplete();
	}
	
	public static void onTimerUpdate(EventInfo<Minecraft> e)
	{
		CallbackProxyClient.events.onTimerUpdate();
	}
	
	public static void newTick(EventInfo<Minecraft> e)
	{
		CallbackProxyClient.clock = true;
	}
	
	public static void onTick(EventInfo<Minecraft> e)
	{
		CallbackProxyClient.events.onTick(CallbackProxyClient.clock);
		CallbackProxyClient.clock = false;
	}
	
	public static void onRender(EventInfo<Minecraft> e)
	{
		CallbackProxyClient.events.onRender();
	}
	
	public static void preRenderGUI(EventInfo<EntityRenderer> e, float partialTicks)
	{
		CallbackProxyClient.events.preRenderGUI(partialTicks);
	}
	
	public static void onSetupCameraTransform(EventInfo<EntityRenderer> e, float partialTicks, long timeSlice)
	{
		CallbackProxyClient.events.onSetupCameraTransform(partialTicks, timeSlice);
	}
	
	public static void postRenderEntities(EventInfo<EntityRenderer> e, float partialTicks, long timeSlice)
	{
		CallbackProxyClient.events.postRenderEntities(partialTicks, timeSlice);
	}
	
	public static void postRender(EventInfo<EntityRenderer> e, float partialTicks, long timeSlice)
	{
		CallbackProxyClient.events.postRender(partialTicks, timeSlice);
	}
	
	public static void onRenderHUD(EventInfo<EntityRenderer> e, float partialTicks)
	{
		CallbackProxyClient.events.onRenderHUD(partialTicks);
	}
	
	public static void onRenderChat(EventInfo<GuiIngame> e, float partialTicks, boolean guiActive, int mouseX, int mouseY)
	{
		CallbackProxyClient.events.onRenderChat(e.getSource().getChatGUI(), partialTicks, guiActive, mouseX, mouseY);
	}
	
	public static void postRenderChat(EventInfo<GuiIngame> e, float partialTicks, boolean guiActive, int mouseX, int mouseY)
	{
		CallbackProxyClient.events.postRenderChat(e.getSource().getChatGUI(), partialTicks, guiActive, mouseX, mouseY);
	}
	
	public static void postRenderHUD(EventInfo<EntityRenderer> e, float partialTicks)
	{
		CallbackProxyClient.events.postRenderHUD(partialTicks);
	}
	
	public static void IntegratedServerCtor(EventInfo<IntegratedServer> e, Minecraft minecraft, String folderName, String worldName, WorldSettings worldSettings)
	{
		CallbackProxyClient.events.onStartServer(e.getSource(), folderName, worldName, worldSettings);
	}
	
	public static void onInitializePlayerConnection(EventInfo<ServerConfigurationManager> e, NetworkManager netManager, EntityPlayerMP player)
	{
		CallbackProxyClient.events.onInitializePlayerConnection(e.getSource(), netManager, player);
	}

	public static void onPlayerLogin(EventInfo<ServerConfigurationManager> e, EntityPlayerMP player)
	{
		CallbackProxyClient.events.onPlayerLogin(e.getSource(), player);
	}
	
	public static void onPlayerLogout(EventInfo<ServerConfigurationManager> e, EntityPlayerMP player)
	{
		CallbackProxyClient.events.onPlayerLogout(e.getSource(), player);
	}
	
	public static void onSpawnPlayer(ReturnEventInfo<ServerConfigurationManager, EntityPlayerMP> e, GameProfile profile)
	{
		CallbackProxyClient.events.onSpawnPlayer(e.getSource(), e.getReturnValue(), profile);
	}

	public static void onRespawnPlayer(ReturnEventInfo<ServerConfigurationManager, EntityPlayerMP> e, EntityPlayerMP oldPlayer, int dimension, boolean won)
	{
		CallbackProxyClient.events.onRespawnPlayer(e.getSource(), e.getReturnValue(), oldPlayer, dimension, won);
	}
	
	public static void onOutboundChat(EventInfo<EntityClientPlayerMP> e, String message)
	{
		CallbackProxyClient.events.onSendChatMessage(e, message);
	}
	
	public static void onResize(EventInfo<Minecraft> e)
	{
		if (CallbackProxyClient.events == null) return;
		CallbackProxyClient.events.onResize(e.getSource());
	}
	
	public static void preRenderFBO(EventInfo<Minecraft> e)
	{
		if (CallbackProxyClient.events == null) return;
		CallbackProxyClient.fboEnabled = OpenGlHelper.isFramebufferEnabled();
		
		if (CallbackProxyClient.fboEnabled)
		{
			CallbackProxyClient.renderingFBO = true;
			CallbackProxyClient.events.preRenderFBO(e.getSource().getFramebuffer());
		}
	}
	
	public static void postRenderFBO(EventInfo<Minecraft> e)
	{
		if (CallbackProxyClient.events == null) return;
		CallbackProxyClient.renderingFBO = false;

		if (CallbackProxyClient.fboEnabled)
		{
			CallbackProxyClient.events.postRenderFBO(e.getSource().getFramebuffer());
		}
	}
	
	public static void renderFBO(EventInfo<Framebuffer> e, int width, int height)
	{
		if (CallbackProxyClient.events == null) return;
		if (CallbackProxyClient.renderingFBO)
		{
			CallbackProxyClient.events.onRenderFBO(e.getSource(), width, height);
		}
		
		CallbackProxyClient.renderingFBO = false;
	}
	
	public static void onRenderWorld(EventInfo<EntityRenderer> e, float partialTicks, long timeSlice)
	{
		CallbackProxyClient.events.onRenderWorld(partialTicks, timeSlice);
	}
}
