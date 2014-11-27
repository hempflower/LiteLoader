package com.mumfrey.liteloader.client;

import java.io.File;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.management.ItemInWorldManager;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.Session;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;

import com.mojang.authlib.GameProfile;
import com.mumfrey.liteloader.core.LiteLoaderEventBroker.InteractType;
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
	private static LiteLoaderEventBrokerClient eventBroker;

	private static boolean fboEnabled;

	private static boolean renderingFBO;
	
	public static void onStartupComplete(EventInfo<Minecraft> e)
	{
		CallbackProxyClient.eventBroker = LiteLoaderEventBrokerClient.getInstance();
		
		if (CallbackProxyClient.eventBroker == null)
		{
			throw new RuntimeException("LiteLoader failed to start up properly. The game is in an unstable state and must shut down now. Check the developer log for startup errors");
		}
		
		CallbackProxyClient.eventBroker.onStartupComplete();
	}
	
	public static void onTimerUpdate(EventInfo<Minecraft> e)
	{
		CallbackProxyClient.eventBroker.onTimerUpdate();
	}
	
	public static void newTick(EventInfo<Minecraft> e)
	{
	}
	
	public static void onTick(EventInfo<Minecraft> e)
	{
		CallbackProxyClient.eventBroker.onTick();
	}
	
	public static void onRender(EventInfo<Minecraft> e)
	{
		CallbackProxyClient.eventBroker.onRender();
	}
	
	public static void preRenderGUI(EventInfo<EntityRenderer> e, float partialTicks)
	{
		CallbackProxyClient.eventBroker.preRenderGUI(partialTicks);
	}
	
	public static void onSetupCameraTransform(EventInfo<EntityRenderer> e, int pass, float partialTicks, long timeSlice)
	{
		CallbackProxyClient.eventBroker.onSetupCameraTransform(partialTicks, timeSlice);
	}
	
	public static void postRenderEntities(EventInfo<EntityRenderer> e, int pass, float partialTicks, long timeSlice)
	{
		CallbackProxyClient.eventBroker.postRenderEntities(partialTicks, timeSlice);
	}
	
	public static void postRender(EventInfo<EntityRenderer> e, float partialTicks, long timeSlice)
	{
		CallbackProxyClient.eventBroker.postRender(partialTicks, timeSlice);
	}
	
	public static void onRenderHUD(EventInfo<EntityRenderer> e, float partialTicks)
	{
		CallbackProxyClient.eventBroker.onRenderHUD(partialTicks);
	}
	
	public static void onRenderChat(EventInfo<GuiIngame> e, float partialTicks)
	{
		CallbackProxyClient.eventBroker.onRenderChat(e.getSource().getChatGUI(), partialTicks);
	}
	
	public static void postRenderChat(EventInfo<GuiIngame> e, float partialTicks)
	{
		CallbackProxyClient.eventBroker.postRenderChat(e.getSource().getChatGUI(), partialTicks);
	}
	
	public static void postRenderHUD(EventInfo<EntityRenderer> e, float partialTicks)
	{
		CallbackProxyClient.eventBroker.postRenderHUD(partialTicks);
	}
	
	public static void IntegratedServerCtor(EventInfo<IntegratedServer> e, Minecraft minecraft, String folderName, String worldName, WorldSettings worldSettings)
	{
		CallbackProxyClient.eventBroker.onStartServer(e.getSource(), folderName, worldName, worldSettings);
	}
	
	public static void onInitializePlayerConnection(EventInfo<ServerConfigurationManager> e, NetworkManager netManager, EntityPlayerMP player)
	{
		CallbackProxyClient.eventBroker.onInitializePlayerConnection(e.getSource(), netManager, player);
	}

	public static void onPlayerLogin(EventInfo<ServerConfigurationManager> e, EntityPlayerMP player)
	{
		CallbackProxyClient.eventBroker.onPlayerLogin(e.getSource(), player);
	}
	
	public static void onPlayerLogout(EventInfo<ServerConfigurationManager> e, EntityPlayerMP player)
	{
		CallbackProxyClient.eventBroker.onPlayerLogout(e.getSource(), player);
	}
	
	public static void onSpawnPlayer(ReturnEventInfo<ServerConfigurationManager, EntityPlayerMP> e, GameProfile profile)
	{
		CallbackProxyClient.eventBroker.onSpawnPlayer(e.getSource(), e.getReturnValue(), profile);
	}

	public static void onRespawnPlayer(ReturnEventInfo<ServerConfigurationManager, EntityPlayerMP> e, EntityPlayerMP oldPlayer, int dimension, boolean won)
	{
		CallbackProxyClient.eventBroker.onRespawnPlayer(e.getSource(), e.getReturnValue(), oldPlayer, dimension, won);
	}
	
	public static void onOutboundChat(EventInfo<EntityPlayerSP> e, String message)
	{
		CallbackProxyClient.eventBroker.onSendChatMessage(e, message);
	}
	
	public static void onResize(EventInfo<Minecraft> e)
	{
		if (CallbackProxyClient.eventBroker == null) return;
		CallbackProxyClient.eventBroker.onResize(e.getSource());
	}
	
	public static void preRenderFBO(EventInfo<Minecraft> e)
	{
		if (CallbackProxyClient.eventBroker == null) return;
		CallbackProxyClient.fboEnabled = OpenGlHelper.isFramebufferEnabled();
		
		if (CallbackProxyClient.fboEnabled)
		{
			CallbackProxyClient.renderingFBO = true;
			CallbackProxyClient.eventBroker.preRenderFBO(e.getSource().getFramebuffer());
		}
	}
	
	public static void postRenderFBO(EventInfo<Minecraft> e)
	{
		if (CallbackProxyClient.eventBroker == null) return;
		CallbackProxyClient.renderingFBO = false;

		if (CallbackProxyClient.fboEnabled)
		{
			CallbackProxyClient.eventBroker.postRenderFBO(e.getSource().getFramebuffer());
		}
	}
	
	public static void renderFBO(EventInfo<Framebuffer> e, int width, int height, boolean flag)
	{
		if (CallbackProxyClient.eventBroker == null) return;
		if (CallbackProxyClient.renderingFBO)
		{
			CallbackProxyClient.eventBroker.onRenderFBO(e.getSource(), width, height);
		}
		
		CallbackProxyClient.renderingFBO = false;
	}
	
	public static void onRenderWorld(EventInfo<EntityRenderer> e, float partialTicks, long timeSlice)
	{
		CallbackProxyClient.eventBroker.onRenderWorld(partialTicks, timeSlice);
	}
	
	public static void onSaveScreenshot(ReturnEventInfo<ScreenShotHelper, IChatComponent> e, File gameDir, String name, int width, int height, Framebuffer fbo)
	{
		CallbackProxyClient.eventBroker.onScreenshot(e, name, width, height, fbo);
	}
	
	public static void onRenderEntity(ReturnEventInfo<RenderManager, Boolean> e, Entity entity, double xPos, double yPos, double zPos, float yaw, float partialTicks, boolean hideBoundingBox, Render render)
	{
		CallbackProxyClient.eventBroker.onRenderEntity(e.getSource(), entity, xPos, yPos, zPos, yaw, partialTicks, render);
	}
	
	public static void onPostRenderEntity(ReturnEventInfo<RenderManager, Boolean> e, Entity entity, double xPos, double yPos, double zPos, float yaw, float partialTicks, boolean hideBoundingBox, Render render)
	{
		CallbackProxyClient.eventBroker.onPostRenderEntity(e.getSource(), entity, xPos, yPos, zPos, yaw, partialTicks, render);
	}
	
	public static void onServerTick(EventInfo<MinecraftServer> e)
	{
		CallbackProxyClient.eventBroker.onServerTick(e.getSource());
	}
	
	public static void onPlaceBlock(EventInfo<NetHandlerPlayServer> e, C08PacketPlayerBlockPlacement packet)
	{
		NetHandlerPlayServer netHandler = e.getSource();
		
		EntityPlayerMP playerMP = netHandler.playerEntity;
		BlockPos pos = packet.getPosition();
		EnumFacing facing = EnumFacing.getFront(packet.getPlacedBlockDirection());
		if (!CallbackProxyClient.eventBroker.onPlayerInteract(InteractType.PLACE_BLOCK_MAYBE, playerMP, pos, facing))
		{
			S23PacketBlockChange cancellation = new S23PacketBlockChange(playerMP.worldObj, pos.offset(facing));
			netHandler.playerEntity.playerNetServerHandler.sendPacket(cancellation);
			playerMP.sendContainerToPlayer(playerMP.inventoryContainer);
			e.cancel();
		}
	}
	
	public static void onClickedAir(EventInfo<NetHandlerPlayServer> e, C0APacketAnimation packet)
	{
		NetHandlerPlayServer netHandler = e.getSource();
		if (!CallbackProxyClient.eventBroker.onPlayerInteract(InteractType.LEFT_CLICK, netHandler.playerEntity, null, EnumFacing.SOUTH))
		{
			e.cancel();
		}
	}

	public static void onPlayerDigging(EventInfo<NetHandlerPlayServer> e, C07PacketPlayerDigging packet)
	{
		if (packet.getStatus() == C07PacketPlayerDigging.Action.START_DESTROY_BLOCK)
		{
			NetHandlerPlayServer netHandler = e.getSource();
			BlockPos pos = packet.func_179715_a();
			EntityPlayerMP playerMP = netHandler.playerEntity;
			if (!CallbackProxyClient.eventBroker.onPlayerInteract(InteractType.DIG_BLOCK_MAYBE, playerMP, pos, EnumFacing.SOUTH))
			{
				S23PacketBlockChange cancellation = new S23PacketBlockChange(playerMP.worldObj, pos);
				netHandler.playerEntity.playerNetServerHandler.sendPacket(cancellation);
				e.cancel();
			}
		}
	}
	
	public static void onUseItem(ReturnEventInfo<ItemInWorldManager, Boolean> e, EntityPlayer player, World world, ItemStack itemStack, BlockPos pos, EnumFacing side, float par8, float par9, float par10)
	{
		if (player instanceof EntityPlayerMP)
		{
			EntityPlayerMP playerMP = (EntityPlayerMP)player;
			
			if (!CallbackProxyClient.eventBroker.onPlayerInteract(InteractType.PLACE_BLOCK_MAYBE, playerMP, pos, side))
			{
				System.err.println(pos);
				S23PacketBlockChange cancellation = new S23PacketBlockChange(playerMP.worldObj, pos);
				playerMP.playerNetServerHandler.sendPacket(cancellation);
				e.setReturnValue(false);
			}
		}
	}
	
	public static void onBlockClicked(EventInfo<ItemInWorldManager> e, BlockPos pos, EnumFacing side)
	{
		ItemInWorldManager manager = e.getSource();
		
		if (!CallbackProxyClient.eventBroker.onPlayerInteract(InteractType.LEFT_CLICK_BLOCK, manager.thisPlayerMP, pos, side))
		{
			S23PacketBlockChange cancellation = new S23PacketBlockChange(manager.theWorld, pos);
			manager.thisPlayerMP.playerNetServerHandler.sendPacket(cancellation);
			e.cancel();
		}
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
