package com.mumfrey.liteloader.core.runtime;

import com.mumfrey.liteloader.transformers.event.MethodInfo;

/**
 *
 * @author Adam Mummery-Smith
 */
public abstract class Methods
{
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
	
	public static final MethodInfo startSection           = new MethodInfo(Obf.Profiler,                   Obf.startSection,                 Void.TYPE, String.class);
	public static final MethodInfo endSection             = new MethodInfo(Obf.Profiler,                   Obf.endSection,                   Void.TYPE);
	public static final MethodInfo endStartSection        = new MethodInfo(Obf.Profiler,                   Obf.endStartSection,              Void.TYPE, String.class);
	
	private Methods() {}
}
