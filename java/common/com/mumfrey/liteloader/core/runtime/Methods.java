package com.mumfrey.liteloader.core.runtime;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.mumfrey.liteloader.transformers.event.MethodInfo;

/**
 *
 * @author Adam Mummery-Smith
 */
public abstract class Methods
{
	// Client & General
	public static final MethodInfo startGame              = new MethodInfo(Obf.Minecraft,                  Obf.startGame,                    Void.TYPE);
	public static final MethodInfo runGameLoop            = new MethodInfo(Obf.Minecraft,                  Obf.runGameLoop,                  Void.TYPE);
	public static final MethodInfo runTick                = new MethodInfo(Obf.Minecraft,                  Obf.runTick,                      Void.TYPE);
	public static final MethodInfo updateFramebufferSize  = new MethodInfo(Obf.Minecraft,                  Obf.updateFramebufferSize,        Void.TYPE);
	public static final MethodInfo framebufferRender      = new MethodInfo(Obf.FrameBuffer,                Obf.framebufferRender,            Void.TYPE, Integer.TYPE, Integer.TYPE);
	public static final MethodInfo framebufferRenderExt   = new MethodInfo(Obf.FrameBuffer,                Obf.framebufferRenderExt,         Void.TYPE, Integer.TYPE, Integer.TYPE, Boolean.TYPE);
	public static final MethodInfo bindFramebufferTexture = new MethodInfo(Obf.FrameBuffer,                Obf.bindFramebufferTexture,       Void.TYPE);
	public static final MethodInfo sendChatMessage        = new MethodInfo(Obf.EntityPlayerSP,             Obf.sendChatMessage,              Void.TYPE, String.class);
	public static final MethodInfo renderWorld            = new MethodInfo(Obf.EntityRenderer,             Obf.renderWorld,                  Void.TYPE, Float.TYPE, Long.TYPE);
	public static final MethodInfo renderWorldPass        = new MethodInfo(Obf.EntityRenderer,             Obf.renderWorldPass,              Void.TYPE, Integer.TYPE, Float.TYPE, Long.TYPE);
	public static final MethodInfo updateCameraAndRender  = new MethodInfo(Obf.EntityRenderer,             Obf.updateCameraAndRender,        Void.TYPE, Float.TYPE);
	public static final MethodInfo renderGameOverlay      = new MethodInfo(Obf.GuiIngame,                  Obf.renderGameOverlay,            Void.TYPE, Float.TYPE);
	public static final MethodInfo drawChat               = new MethodInfo(Obf.GuiNewChat,                 Obf.drawChat,                     Void.TYPE, Integer.TYPE);
	public static final MethodInfo integratedServerCtor   = new MethodInfo(Obf.IntegratedServer,           Obf.constructor,                  Void.TYPE, Obf.Minecraft, String.class, String.class, Obf.WorldSettings);
	public static final MethodInfo initPlayerConnection   = new MethodInfo(Obf.ServerConfigurationManager, Obf.initializeConnectionToPlayer, Void.TYPE, Obf.NetworkManager, Obf.EntityPlayerMP);
	public static final MethodInfo playerLoggedIn         = new MethodInfo(Obf.ServerConfigurationManager, Obf.playerLoggedIn,               Void.TYPE, Obf.EntityPlayerMP);
	public static final MethodInfo playerLoggedOut        = new MethodInfo(Obf.ServerConfigurationManager, Obf.playerLoggedOut,              Void.TYPE, Obf.EntityPlayerMP);
	public static final MethodInfo spawnPlayer            = new MethodInfo(Obf.ServerConfigurationManager, Obf.spawnPlayer,                  Obf.EntityPlayerMP, Obf.GameProfile);
	public static final MethodInfo respawnPlayer          = new MethodInfo(Obf.ServerConfigurationManager, Obf.respawnPlayer,                Obf.EntityPlayerMP, Obf.EntityPlayerMP, Integer.TYPE, Boolean.TYPE);
	public static final MethodInfo glClear                = new MethodInfo(Obf.GlStateManager,             Obf.clear,                        Void.TYPE, Integer.TYPE);
	public static final MethodInfo getProfile             = new MethodInfo(Obf.Session,                    Obf.getProfile,                   Obf.GameProfile);
	public static final MethodInfo saveScreenshot         = new MethodInfo(Obf.ScreenShotHelper,           Obf.saveScreenshot,               Obf.IChatComponent, File.class, String.class, Integer.TYPE, Integer.TYPE, Obf.FrameBuffer);
	public static final MethodInfo isFramebufferEnabled   = new MethodInfo(Obf.OpenGlHelper,               Obf.isFramebufferEnabled,         Boolean.TYPE);
	public static final MethodInfo doRenderEntity         = new MethodInfo(Obf.RenderManager,              Obf.doRenderEntity,               Boolean.TYPE, Obf.Entity, Double.TYPE, Double.TYPE, Double.TYPE, Float.TYPE, Float.TYPE, Boolean.TYPE);
	public static final MethodInfo doRender               = new MethodInfo(Obf.Render,                     Obf.doRender,                     Void.TYPE, Obf.Entity, Double.TYPE, Double.TYPE, Double.TYPE, Float.TYPE, Float.TYPE);
	public static final MethodInfo doRenderShadowAndFire  = new MethodInfo(Obf.Render,                     Obf.doRenderShadowAndFire,        Void.TYPE, Obf.Entity, Double.TYPE, Double.TYPE, Double.TYPE, Float.TYPE, Float.TYPE);
	public static final MethodInfo realmsPlay             = new MethodInfo(Obf.RealmsMainScreen,           "play",                           Void.TYPE, Long.TYPE);
	public static final MethodInfo realmsStopFetcher      = new MethodInfo(Obf.RealmsMainScreen,           "stopRealmsFetcherAndPinger",     Void.TYPE);
	public static final MethodInfo onBlockClicked         = new MethodInfo(Obf.ItemInWorldManager,         Obf.onBlockClicked,               Void.TYPE, Obf.BlockPos, Obf.EnumFacing);
	public static final MethodInfo activateBlockOrUseItem = new MethodInfo(Obf.ItemInWorldManager,         Obf.activateBlockOrUseItem,       Boolean.TYPE, Obf.EntityPlayer, Obf.World, Obf.ItemStack, Obf.BlockPos, Obf.EnumFacing, Float.TYPE, Float.TYPE, Float.TYPE);
	public static final MethodInfo processBlockPlacement  = new MethodInfo(Obf.NetHandlerPlayServer,       Obf.processPlayerBlockPlacement,  Void.TYPE, Packets.C08PacketPlayerBlockPlacement);
	public static final MethodInfo handleAnimation        = new MethodInfo(Obf.NetHandlerPlayServer,       Obf.handleAnimation,              Void.TYPE, Packets.C0APacketAnimation);
	public static final MethodInfo processPlayerDigging   = new MethodInfo(Obf.NetHandlerPlayServer,       Obf.processPlayerDigging,         Void.TYPE, Packets.C07PacketPlayerDigging);
	public static final MethodInfo serverJobs             = new MethodInfo(Obf.MinecraftServer,            Obf.updateTimeLightAndEntities,   Void.TYPE);
	public static final MethodInfo checkThreadAndEnqueue  = new MethodInfo(Obf.PacketThreadUtil,           Obf.checkThreadAndEnqueue);
	public static final MethodInfo processPlayer          = new MethodInfo(Obf.NetHandlerPlayServer,       Obf.processPlayer,                Void.TYPE, Packets.C03PacketPlayer);
	
	// Profiler
	public static final MethodInfo startSection           = new MethodInfo(Obf.Profiler,                   Obf.startSection,                 Void.TYPE, String.class);
	public static final MethodInfo endSection             = new MethodInfo(Obf.Profiler,                   Obf.endSection,                   Void.TYPE);
	public static final MethodInfo endStartSection        = new MethodInfo(Obf.Profiler,                   Obf.endStartSection,              Void.TYPE, String.class);

	// Dedicated Server
	public static final MethodInfo startServer            = new MethodInfo(Obf.DedicatedServer,            Obf.startServer,                  Boolean.TYPE);
	public static final MethodInfo startServerThread      = new MethodInfo(Obf.MinecraftServer,            Obf.startServerThread,            Void.TYPE);
	
	private Methods() {}
	
	private static final Map<String, MethodInfo> methodMap = new HashMap<String, MethodInfo>();
	
	static
	{
		try
		{
			for (Field fd : Methods.class.getFields())
			{
				if (fd.getType().equals(MethodInfo.class))
				{
					Methods.methodMap.put(fd.getName(), (MethodInfo)fd.get(null));
				}
			}
		}
		catch (IllegalAccessException ex) {}
	}

	public static MethodInfo getByName(String name)
	{
		return Methods.methodMap.get(name);
	}
}
