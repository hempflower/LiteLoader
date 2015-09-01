package com.mumfrey.liteloader.client;

import java.io.File;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.Session;
import net.minecraft.world.WorldSettings;

import com.mojang.authlib.GameProfile;
import com.mumfrey.liteloader.core.CallbackProxyCommon;
import com.mumfrey.liteloader.transformers.event.EventInfo;
import com.mumfrey.liteloader.transformers.event.ReturnEventInfo;

/**
 * Proxy class which handles the redirected calls from the injected callbacks and routes them to the
 * relevant liteloader handler classes. We do this rather than patching a bunch of bytecode into the packet
 * classes themselves because this is easier to maintain.
 * 
 * @author Adam Mummery-Smith
 */
public abstract class CallbackProxyClient extends CallbackProxyCommon
{
	private static LiteLoaderEventBrokerClient clientEventBroker;

	private static boolean fboEnabled;

	private static boolean renderingFBO;
	
	private CallbackProxyClient() {}
	
	public static void onStartupComplete(EventInfo<Minecraft> e)
	{
		CallbackProxyCommon.onStartupComplete();
		
		CallbackProxyClient.clientEventBroker = LiteLoaderEventBrokerClient.getInstance();
		
		if (CallbackProxyClient.clientEventBroker == null)
		{
			throw new RuntimeException("LiteLoader failed to start up properly. The game is in an unstable state and must shut down now. Check the developer log for startup errors");
		}
		
		CallbackProxyClient.clientEventBroker.onStartupComplete();
	}
	
	public static void onTimerUpdate(EventInfo<Minecraft> e)
	{
		CallbackProxyClient.clientEventBroker.onTimerUpdate();
	}
	
	public static void newTick(EventInfo<Minecraft> e)
	{
	}
	
	public static void onTick(EventInfo<Minecraft> e)
	{
		CallbackProxyClient.clientEventBroker.onTick();
	}
	
	public static void onRender(EventInfo<Minecraft> e)
	{
		CallbackProxyClient.clientEventBroker.onRender();
	}
	
	public static void preRenderGUI(EventInfo<EntityRenderer> e, float partialTicks)
	{
		CallbackProxyClient.clientEventBroker.preRenderGUI(partialTicks);
	}
	
	public static void onSetupCameraTransform(EventInfo<EntityRenderer> e, int pass, float partialTicks, long timeSlice)
	{
		CallbackProxyClient.clientEventBroker.onSetupCameraTransform(pass, partialTicks, timeSlice);
	}
	
	public static void postRenderEntities(EventInfo<EntityRenderer> e, int pass, float partialTicks, long timeSlice)
	{
		CallbackProxyClient.clientEventBroker.postRenderEntities(partialTicks, timeSlice);
	}
	
	public static void postRender(EventInfo<EntityRenderer> e, float partialTicks, long timeSlice)
	{
		CallbackProxyClient.clientEventBroker.postRender(partialTicks, timeSlice);
	}
	
	public static void onRenderHUD(EventInfo<EntityRenderer> e, float partialTicks)
	{
		CallbackProxyClient.clientEventBroker.onRenderHUD(partialTicks);
	}
	
	public static void onRenderChat(EventInfo<GuiIngame> e, float partialTicks)
	{
		CallbackProxyClient.clientEventBroker.onRenderChat(e.getSource().getChatGUI(), partialTicks);
	}
	
	public static void postRenderChat(EventInfo<GuiIngame> e, float partialTicks)
	{
		CallbackProxyClient.clientEventBroker.postRenderChat(e.getSource().getChatGUI(), partialTicks);
	}
	
	public static void postRenderHUD(EventInfo<EntityRenderer> e, float partialTicks)
	{
		CallbackProxyClient.clientEventBroker.postRenderHUD(partialTicks);
	}
	
	public static void IntegratedServerCtor(EventInfo<IntegratedServer> e, Minecraft minecraft, String folderName, String worldName, WorldSettings worldSettings)
	{
		CallbackProxyClient.clientEventBroker.onStartServer(e.getSource(), folderName, worldName, worldSettings);
	}
	
	public static void onOutboundChat(EventInfo<EntityPlayerSP> e, String message)
	{
		CallbackProxyClient.clientEventBroker.onSendChatMessage(e, message);
	}
	
	public static void onResize(EventInfo<Minecraft> e)
	{
		if (CallbackProxyClient.clientEventBroker == null) return;
		CallbackProxyClient.clientEventBroker.onResize(e.getSource());
	}
	
	public static void preRenderFBO(EventInfo<Minecraft> e)
	{
		if (CallbackProxyClient.clientEventBroker == null) return;
		CallbackProxyClient.fboEnabled = OpenGlHelper.isFramebufferEnabled();
		
		if (CallbackProxyClient.fboEnabled)
		{
			CallbackProxyClient.renderingFBO = true;
			CallbackProxyClient.clientEventBroker.preRenderFBO(e.getSource().getFramebuffer());
		}
	}
	
	public static void postRenderFBO(EventInfo<Minecraft> e)
	{
		if (CallbackProxyClient.clientEventBroker == null) return;
		CallbackProxyClient.renderingFBO = false;

		if (CallbackProxyClient.fboEnabled)
		{
			CallbackProxyClient.clientEventBroker.postRenderFBO(e.getSource().getFramebuffer());
		}
	}
	
	public static void renderFBO(EventInfo<Framebuffer> e, int width, int height, boolean flag)
	{
		if (CallbackProxyClient.clientEventBroker == null) return;
		if (CallbackProxyClient.renderingFBO)
		{
			CallbackProxyClient.clientEventBroker.onRenderFBO(e.getSource(), width, height);
		}
		
		CallbackProxyClient.renderingFBO = false;
	}
	
	public static void onRenderWorld(EventInfo<EntityRenderer> e, float partialTicks, long timeSlice)
	{
		CallbackProxyClient.clientEventBroker.onRenderWorld(partialTicks, timeSlice);
	}
	
	public static void onRenderSky(EventInfo<EntityRenderer> e, int pass, float partialTicks, long timeSlice)
	{
		CallbackProxyClient.clientEventBroker.onRenderSky(partialTicks, pass, timeSlice);
	}
	
	public static void onRenderClouds(EventInfo<EntityRenderer> e, RenderGlobal renderGlobalIn, float partialTicks, int pass)
	{
		CallbackProxyClient.clientEventBroker.onRenderClouds(partialTicks, pass, renderGlobalIn);
	}
	
	public static void onRenderTerrain(EventInfo<EntityRenderer> e, int pass, float partialTicks, long timeSlice)
	{
		CallbackProxyClient.clientEventBroker.onRenderTerrain(partialTicks, pass, timeSlice);
	}
	
	public static void onSaveScreenshot(ReturnEventInfo<ScreenShotHelper, IChatComponent> e, File gameDir, String name, int width, int height, Framebuffer fbo)
	{
		CallbackProxyClient.clientEventBroker.onScreenshot(e, name, width, height, fbo);
	}
	
	public static void onRenderEntity(ReturnEventInfo<RenderManager, Boolean> e, Entity entity, double xPos, double yPos, double zPos, float yaw, float partialTicks, boolean hideBoundingBox, Render render)
	{
		CallbackProxyClient.clientEventBroker.onRenderEntity(e.getSource(), entity, xPos, yPos, zPos, yaw, partialTicks, render);
	}
	
	public static void onPostRenderEntity(ReturnEventInfo<RenderManager, Boolean> e, Entity entity, double xPos, double yPos, double zPos, float yaw, float partialTicks, boolean hideBoundingBox, Render render)
	{
		CallbackProxyClient.clientEventBroker.onPostRenderEntity(e.getSource(), entity, xPos, yPos, zPos, yaw, partialTicks, render);
	}
	
	/**
	 * Compatiblbe behaviour with FML, this method is called to generate a consistent offline UUID between client and server
	 * for a given username.
	 */
	public static void generateOfflineUUID(ReturnEventInfo<Session, GameProfile> e)
	{
		Session session = e.getSource();
		UUID uuid = EntityPlayer.getUUID(new GameProfile((UUID)null, session.getUsername()));
		e.setReturnValue(new GameProfile(uuid, session.getUsername()));
	}
}
