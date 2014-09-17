package com.mumfrey.liteloader.client.transformers;

import static com.mumfrey.liteloader.core.runtime.Methods.*;
import static com.mumfrey.liteloader.transformers.event.InjectionPoint.*;

import com.mumfrey.liteloader.core.runtime.Obf;
import com.mumfrey.liteloader.transformers.event.Event;
import com.mumfrey.liteloader.transformers.event.EventInjectionTransformer;
import com.mumfrey.liteloader.transformers.event.InjectionPoint;
import com.mumfrey.liteloader.transformers.event.MethodInfo;
import com.mumfrey.liteloader.transformers.event.inject.BeforeInvoke;
import com.mumfrey.liteloader.transformers.event.inject.BeforeReturn;
import com.mumfrey.liteloader.transformers.event.inject.BeforeStringInvoke;
import com.mumfrey.liteloader.transformers.event.inject.MethodHead;

public class LiteLoaderEventInjectionTransformer extends EventInjectionTransformer
{
	@Override
	protected void addEvents()
	{
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
		Event onInitializePlayerConnection     = Event.getOrCreate("onInitializePlayerConnection", false);
		Event onPlayerLogin                    = Event.getOrCreate("onPlayerLogin",                false);
		Event onPlayerLogout                   = Event.getOrCreate("onPlayerLogout",               false);
		Event onSpawnPlayer                    = Event.getOrCreate("onSpawnPlayer",                false);
		Event onRespawnPlayer                  = Event.getOrCreate("onRespawnPlayer",              false);
		Event onStartupComplete                = Event.getOrCreate("onStartupComplete",            false);
		
		InjectionPoint methodHead              = new MethodHead();
		InjectionPoint methodReturn            = new BeforeReturn();
		InjectionPoint beforeGlClear           = new BeforeInvoke(glClear);
		InjectionPoint beforeFBORender         = new BeforeInvoke(framebufferRender);
		InjectionPoint beforeRenderHUD         = new BeforeInvoke(renderGameOverlay);
		InjectionPoint beforeBindFBOTex        = new BeforeInvoke(bindFramebufferTexture);
		InjectionPoint beforeRender            = new BeforeInvoke(updateCameraAndRender);
		InjectionPoint beforeDrawChat          = new BeforeInvoke(drawChat);
		InjectionPoint beforeEndProfiler       = new BeforeInvoke(endSection);
		InjectionPoint beforeTickProfiler      = new BeforeStringInvoke("tick",         startSection);
		InjectionPoint beforePickProfiler      = new BeforeStringInvoke("pick",         endStartSection);
		InjectionPoint beforeRenderProfiler    = new BeforeStringInvoke("gameRenderer", endStartSection);
		InjectionPoint beforeFrustumProfiler   = new BeforeStringInvoke("frustrum",     endStartSection);
		InjectionPoint beforeParticlesProfiler = new BeforeStringInvoke("litParticles", endStartSection);
		
		this.add(onOutboundChat,               sendChatMessage,            (methodHead),              "onOutboundChat");
		this.add(onResize,                     updateFramebufferSize,      (methodHead),              "onResize");
		this.add(preRenderFBO,                 runGameLoop,                (beforeFBORender),         "preRenderFBO");
		this.add(renderFBO,                    framebufferRender,          (beforeBindFBOTex),        "renderFBO");
		this.add(postRenderFBO,                runGameLoop,           after(beforeFBORender),         "postRenderFBO");
		this.add(onRenderWorld,                renderWorld,                (beforePickProfiler),      "onRenderWorld");
		this.add(onTimerUpdate,                runGameLoop,                (beforeTickProfiler),      "onTimerUpdate");
		this.add(onRender,                     runGameLoop,                (beforeRenderProfiler),    "onRender");
		this.add(newTick,                      runTick,                    (methodHead),              "newTick");
		this.add(onTick,                       runGameLoop,           after(beforeRender),            "onTick");
		this.add(preRenderGUI,                 updateCameraAndRender, after(beforeGlClear),           "preRenderGUI");
		this.add(onRenderHUD,                  updateCameraAndRender,      (beforeRenderHUD),         "onRenderHUD");
		this.add(postRenderHUD,                updateCameraAndRender, after(beforeRenderHUD),         "postRenderHUD");
		this.add(onSetupCameraTransform,       renderWorld,                (beforeFrustumProfiler),   "onSetupCameraTransform");
		this.add(postRenderEntities,           renderWorld,                (beforeParticlesProfiler), "postRenderEntities");
		this.add(postRender,                   renderWorld,                (beforeEndProfiler),       "postRender");
		this.add(onRenderChat,                 renderGameOverlay,          (beforeDrawChat),          "onRenderChat");
		this.add(postRenderChat,               renderGameOverlay,     after(beforeDrawChat),          "postRenderChat");
		this.add(onCreateIntegratedServer,     integratedServerCtor,       (methodReturn),            "IntegratedServerCtor");
		this.add(onInitializePlayerConnection, initPlayerConnection,       (methodReturn),            "onInitializePlayerConnection");
		this.add(onPlayerLogin,                playerLoggedIn,             (methodReturn),            "onPlayerLogin");
		this.add(onPlayerLogout,               playerLoggedOut,            (methodReturn),            "onPlayerLogout");
		this.add(onSpawnPlayer,                spawnPlayer,                (methodReturn),            "onSpawnPlayer");
		this.add(onRespawnPlayer,              respawnPlayer,              (methodReturn),            "onRespawnPlayer");
		this.add(onStartupComplete,            startGame,                  (methodReturn),            "onStartupComplete");
	}

	protected final Event add(Event event, MethodInfo targetMethod, InjectionPoint injectionPoint, String callback)
	{
		return this.addEvent(event, targetMethod, injectionPoint).addListener(new MethodInfo(Obf.CallbackProxyClient, callback));
	}
}
