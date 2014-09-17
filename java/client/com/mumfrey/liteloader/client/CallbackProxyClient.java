package com.mumfrey.liteloader.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.world.WorldSettings;

import com.mojang.authlib.GameProfile;
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
