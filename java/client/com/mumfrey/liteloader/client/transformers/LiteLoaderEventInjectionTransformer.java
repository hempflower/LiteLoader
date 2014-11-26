package com.mumfrey.liteloader.client.transformers;

import static com.mumfrey.liteloader.core.runtime.Methods.*;
import static com.mumfrey.liteloader.transformers.event.InjectionPoint.*;

import com.mumfrey.liteloader.common.transformers.LiteLoaderEventTransformer;
import com.mumfrey.liteloader.core.runtime.Obf;
import com.mumfrey.liteloader.transformers.event.Event;
import com.mumfrey.liteloader.transformers.event.InjectionPoint;
import com.mumfrey.liteloader.transformers.event.inject.BeforeInvoke;
import com.mumfrey.liteloader.transformers.event.inject.BeforeNew;
import com.mumfrey.liteloader.transformers.event.inject.BeforeReturn;
import com.mumfrey.liteloader.transformers.event.inject.BeforeStringInvoke;
import com.mumfrey.liteloader.transformers.event.inject.MethodHead;

/**
 * Injector for LiteLoader's main events
 *
 * @author Adam Mummery-Smith
 */
public class LiteLoaderEventInjectionTransformer extends LiteLoaderEventTransformer
{
	@Override
	protected Obf getProxy()
	{
		return Obf.CallbackProxyClient;
	}
	
	@Override
	protected void addEvents()
	{
		super.addEvents();

		// Event declaraions
		Event onOutboundChat                   = Event.getOrCreate("onOutboundChat",               true);
		Event onResize                         = Event.getOrCreate("updateFramebufferSize",        false);
		Event preRenderFBO                     = Event.getOrCreate("preRenderFBO",                 false);
		Event renderFBO                        = Event.getOrCreate("renderFBO",                    false);
		Event postRenderFBO                    = Event.getOrCreate("postRenderFBO",                false);
		Event onRenderWorld                    = Event.getOrCreate("onRenderWorld",                false);
		Event onTimerUpdate                    = Event.getOrCreate("onTimerUpdate",                false);
		Event onRender                         = Event.getOrCreate("onRender",                     false);
		Event newTick                          = Event.getOrCreate("newTick",                      false);
		Event onTick                           = Event.getOrCreate("onTick",                       false);
		Event preRenderGUI                     = Event.getOrCreate("preRenderGUI",                 false);
		Event onRenderHUD                      = Event.getOrCreate("onRenderHUD",                  false);
		Event postRenderHUD                    = Event.getOrCreate("postRenderHUD",                false);
		Event onSetupCameraTransform           = Event.getOrCreate("onSetupCameraTransform",       false);
		Event postRenderEntities               = Event.getOrCreate("postRenderEntities",           false);
		Event postRender                       = Event.getOrCreate("postRender",                   false);
		Event onRenderChat                     = Event.getOrCreate("onRenderChat",                 false);
		Event postRenderChat                   = Event.getOrCreate("postRenderChat",               false);
		Event onCreateIntegratedServer         = Event.getOrCreate("onCreateIntegratedServer",     false);
		Event onStartupComplete                = Event.getOrCreate("onStartupComplete",            false);
		Event onSessionProfileBad              = Event.getOrCreate("onSessionProfileBad",          true);
		Event onSaveScreenshot                 = Event.getOrCreate("onSaveScreenshot",             true);
		Event onRenderEntity                   = Event.getOrCreate("onRenderEntity",               false);
		Event onPostRenderEntity               = Event.getOrCreate("onPostRenderEntity",           false);
		Event onJoinRealm                      = Event.getOrCreate("onJoinRealm",                  false);
		
		// Injection Points
		InjectionPoint methodHead              = new MethodHead();
		InjectionPoint methodReturn            = new BeforeReturn();
		InjectionPoint beforeGlClear           = new BeforeInvoke(glClear);
		InjectionPoint beforeFBORender         = new BeforeInvoke(framebufferRender);
		InjectionPoint beforeRenderHUD         = new BeforeInvoke(renderGameOverlay);
		InjectionPoint beforeBindFBOTex        = new BeforeInvoke(bindFramebufferTexture);
		InjectionPoint beforeRender            = new BeforeInvoke(updateCameraAndRender);
		InjectionPoint beforeDrawChat          = new BeforeInvoke(drawChat);
		InjectionPoint beforeEndProfiler       = new BeforeInvoke(endSection);
		InjectionPoint beforeIsFBOEnabled      = new BeforeInvoke(isFramebufferEnabled);
		InjectionPoint beforeRenderEntity      = new BeforeInvoke(doRender).setCaptureLocals(true);
		InjectionPoint beforeStopRealsmFetcher = new BeforeInvoke(realmsStopFetcher).setCaptureLocals(true);
		InjectionPoint beforeTickProfiler      = new BeforeStringInvoke("tick",         startSection);
		InjectionPoint beforeCenterProfiler    = new BeforeStringInvoke("center",       startSection);
		InjectionPoint beforeRenderProfiler    = new BeforeStringInvoke("gameRenderer", endStartSection);
		InjectionPoint beforeFrustumProfiler   = new BeforeStringInvoke("frustum",      endStartSection);
		InjectionPoint beforeParticlesProfiler = new BeforeStringInvoke("litParticles", endStartSection);
		InjectionPoint beforeNewGameProfile    = new BeforeNew(1, Obf.GameProfile);
		
		// Hooks
		this.add(onOutboundChat,               sendChatMessage,            (methodHead),              "onOutboundChat");
		this.add(onResize,                     updateFramebufferSize,      (methodHead),              "onResize");
		this.add(preRenderFBO,                 runGameLoop,                (beforeFBORender),         "preRenderFBO");
		this.add(renderFBO,                    framebufferRenderExt,       (beforeBindFBOTex),        "renderFBO");
		this.add(postRenderFBO,                runGameLoop,           after(beforeFBORender),         "postRenderFBO");
		this.add(onRenderWorld,                renderWorld,                (beforeCenterProfiler),    "onRenderWorld");
		this.add(onTimerUpdate,                runGameLoop,                (beforeTickProfiler),      "onTimerUpdate");
		this.add(onRender,                     runGameLoop,                (beforeRenderProfiler),    "onRender");
		this.add(newTick,                      runTick,                    (methodHead),              "newTick");
		this.add(onTick,                       runGameLoop,           after(beforeRender),            "onTick");
		this.add(preRenderGUI,                 updateCameraAndRender, after(beforeGlClear),           "preRenderGUI");
		this.add(onRenderHUD,                  updateCameraAndRender,      (beforeRenderHUD),         "onRenderHUD");
		this.add(postRenderHUD,                updateCameraAndRender, after(beforeRenderHUD),         "postRenderHUD");
		this.add(onSetupCameraTransform,       renderWorldPass,            (beforeFrustumProfiler),   "onSetupCameraTransform");
		this.add(postRenderEntities,           renderWorldPass,            (beforeParticlesProfiler), "postRenderEntities");
		this.add(postRender,                   renderWorld,                (beforeEndProfiler),       "postRender");
		this.add(onRenderChat,                 renderGameOverlay,          (beforeDrawChat),          "onRenderChat");
		this.add(postRenderChat,               renderGameOverlay,     after(beforeDrawChat),          "postRenderChat");
		this.add(onCreateIntegratedServer,     integratedServerCtor,       (methodReturn),            "IntegratedServerCtor");
		this.add(onStartupComplete,            startGame,                  (methodReturn),            "onStartupComplete");
		this.add(onSaveScreenshot,             saveScreenshot,             (beforeIsFBOEnabled),      "onSaveScreenshot");
		this.add(onRenderEntity,               doRenderEntity,             (beforeRenderEntity),      "onRenderEntity");
		this.add(onPostRenderEntity,           doRenderEntity,        after(beforeRenderEntity),      "onPostRenderEntity");
		
		// Compatibility handlers
		this.add(onSessionProfileBad,          getProfile,                 (beforeNewGameProfile),    "generateOfflineUUID");
		
		// Protocol handlers
		this.add(onJoinRealm,                  realmsPlay,                 (beforeStopRealsmFetcher), "onJoinRealm", Obf.PacketEventsClient);
	}
}
