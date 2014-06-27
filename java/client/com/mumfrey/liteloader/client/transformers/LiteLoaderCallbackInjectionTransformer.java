package com.mumfrey.liteloader.client.transformers;

import org.objectweb.asm.Type;

import com.mumfrey.liteloader.core.runtime.Obf;
import com.mumfrey.liteloader.transformers.Callback;
import com.mumfrey.liteloader.transformers.CallbackInjectionTransformer;
import com.mumfrey.liteloader.transformers.Callback.CallbackType;

/**
 * Transformer which injects method calls in place of the old profiler hook
 * 
 * @author Adam Mummery-Smith
 */
public final class LiteLoaderCallbackInjectionTransformer extends CallbackInjectionTransformer
{
	/**
	 * Add mappings
	 */
	@Override
	protected void addCallbacks()
	{
		this.addCallbacks(Obf.MCP); // @MCPONLY
		this.addCallbacks(Obf.SRG);
		this.addCallbacks(Obf.OBF);
	}

	private void addCallbacks(int type)
	{
		this.addCallback(type, Obf.Minecraft,      Obf.runGameLoop,           "()V",     new Callback(CallbackType.PROFILER_STARTSECTION,    "onTimerUpdate",          Obf.CallbackProxyClient.ref, "tick",         type));
		this.addCallback(type, Obf.Minecraft,      Obf.runGameLoop,           "()V",     new Callback(CallbackType.PROFILER_ENDSTARTSECTION, "onRender",               Obf.CallbackProxyClient.ref, "gameRenderer", type));
		this.addCallback(type, Obf.Minecraft,      Obf.runTick,               "()V",     new Callback(CallbackType.PROFILER_ENDSTARTSECTION, "onAnimateTick",          Obf.CallbackProxyClient.ref, "animateTick",  type));
		this.addCallback(type, Obf.Minecraft,      Obf.runGameLoop,           "()V",     new Callback(CallbackType.PROFILER_ENDSECTION,      "onTick",                 Obf.CallbackProxyClient.ref, "",             type)); // ref 2
		this.addCallback(type, Obf.EntityRenderer, Obf.updateCameraAndRender, "(F)V",    new Callback(CallbackType.PROFILER_ENDSECTION,      "preRenderGUI",           Obf.CallbackProxyClient.ref, "",             type)); // ref 1
		this.addCallback(type, Obf.EntityRenderer, Obf.updateCameraAndRender, "(F)V",    new Callback(CallbackType.PROFILER_ENDSECTION,      "postRenderHUDandGUI",    Obf.CallbackProxyClient.ref, "",             type)); // ref 2
		this.addCallback(type, Obf.EntityRenderer, Obf.updateCameraAndRender, "(F)V",    new Callback(CallbackType.PROFILER_ENDSTARTSECTION, "onRenderHUD",            Obf.CallbackProxyClient.ref, "gui",          type));
		this.addCallback(type, Obf.EntityRenderer, Obf.renderWorld,           "(FJ)V",   new Callback(CallbackType.PROFILER_ENDSTARTSECTION, "onSetupCameraTransform", Obf.CallbackProxyClient.ref, "frustrum",     type));
		this.addCallback(type, Obf.EntityRenderer, Obf.renderWorld,           "(FJ)V",   new Callback(CallbackType.PROFILER_ENDSTARTSECTION, "postRenderEntities",     Obf.CallbackProxyClient.ref, "litParticles", type));
		this.addCallback(type, Obf.EntityRenderer, Obf.renderWorld,           "(FJ)V",   new Callback(CallbackType.PROFILER_ENDSECTION,      "postRender",             Obf.CallbackProxyClient.ref, "",             type));
		this.addCallback(type, Obf.GuiIngame,      Obf.renderGameOverlay,     "(FZII)V", new Callback(CallbackType.PROFILER_STARTSECTION,    "onRenderChat",           Obf.CallbackProxyClient.ref, "chat",         type));
		this.addCallback(type, Obf.GuiIngame,      Obf.renderGameOverlay,     "(FZII)V", new Callback(CallbackType.PROFILER_ENDSECTION,      "postRenderChat",         Obf.CallbackProxyClient.ref, "",             type)); // ref 10
		
		String integratedServerCtorDescriptor = Callback.generateDescriptor(type, Type.VOID_TYPE, Obf.Minecraft, String.class, String.class, Obf.WorldSettings);
		String initPlayerConnectionDescriptor = Callback.generateDescriptor(type, Type.VOID_TYPE, Obf.NetworkManager, Obf.EntityPlayerMP);
		String playerLoggedInOutDescriptor    = Callback.generateDescriptor(type, Type.VOID_TYPE, Obf.EntityPlayerMP);
		String spawnPlayerDescriptor          = Callback.generateDescriptor(type, Obf.EntityPlayerMP, Obf.GameProfile);
		String respawnPlayerDescriptor        = Callback.generateDescriptor(type, Obf.EntityPlayerMP, Obf.EntityPlayerMP, Type.INT_TYPE, Type.BOOLEAN_TYPE);
		
		this.addCallback(type, Obf.IntegratedServer,           Obf.constructor,                  integratedServerCtorDescriptor, new Callback(CallbackType.RETURN, "IntegratedServerCtor",         Obf.CallbackProxyClient.ref));
		this.addCallback(type, Obf.ServerConfigurationManager, Obf.initializeConnectionToPlayer, initPlayerConnectionDescriptor, new Callback(CallbackType.RETURN, "onInitializePlayerConnection", Obf.CallbackProxyClient.ref));
		this.addCallback(type, Obf.ServerConfigurationManager, Obf.playerLoggedIn,               playerLoggedInOutDescriptor,    new Callback(CallbackType.RETURN, "onPlayerLogin",                Obf.CallbackProxyClient.ref));
		this.addCallback(type, Obf.ServerConfigurationManager, Obf.playerLoggedOut,              playerLoggedInOutDescriptor,    new Callback(CallbackType.RETURN, "onPlayerLogout",               Obf.CallbackProxyClient.ref));
		this.addCallback(type, Obf.ServerConfigurationManager, Obf.spawnPlayer,                  spawnPlayerDescriptor,          new Callback(CallbackType.RETURN, "onSpawnPlayer",                Obf.CallbackProxyClient.ref));
		this.addCallback(type, Obf.ServerConfigurationManager, Obf.respawnPlayer,                respawnPlayerDescriptor,        new Callback(CallbackType.RETURN, "onRespawnPlayer",              Obf.CallbackProxyClient.ref));
//		this.addCallback(type, Obf.C01PacketChatMessage,       Obf.constructor,                  "(Ljava/lang/String;)V",        new Callback(CallbackType.RETURN, "onOutboundChat",               Obf.CallbackProxyClient.ref));
		this.addCallback(type, Obf.Minecraft,                  Obf.startGame,                    "()V",                          new Callback(CallbackType.RETURN, "onStartupComplete",            Obf.CallbackProxyClient.ref));
	}
	
	/**
	 * @param type
	 * @param className
	 * @param methodName
	 * @param methodSignature
	 * @param invokeMethod
	 * @param section
	 * @param callback
	 */
	private void addCallback(int type, Obf className, Obf methodName, String methodSignature, Callback callback)
	{
		this.addCallback(className.names[type], methodName.names[type], methodSignature, callback);
	}
}
