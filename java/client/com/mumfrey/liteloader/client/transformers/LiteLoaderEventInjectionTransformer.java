package com.mumfrey.liteloader.client.transformers;

import com.mumfrey.liteloader.core.runtime.Obf;
import com.mumfrey.liteloader.transformers.event.Event;
import com.mumfrey.liteloader.transformers.event.EventInjectionTransformer;
import com.mumfrey.liteloader.transformers.event.InjectionPoint;
import com.mumfrey.liteloader.transformers.event.MethodInfo;
import com.mumfrey.liteloader.transformers.event.inject.BeforeInvoke;
import com.mumfrey.liteloader.transformers.event.inject.MethodHead;

public class LiteLoaderEventInjectionTransformer extends EventInjectionTransformer
{
	@Override
	protected void addEvents()
	{
		MethodInfo runGameLoop            = new MethodInfo(Obf.Minecraft,            Obf.runGameLoop,            Void.TYPE);
		MethodInfo updateFramebufferSize  = new MethodInfo(Obf.Minecraft,            Obf.updateFramebufferSize,  Void.TYPE);
		MethodInfo framebufferRender      = new MethodInfo(Obf.FrameBuffer,          Obf.framebufferRender,      Void.TYPE, Integer.TYPE, Integer.TYPE);
		MethodInfo bindFramebufferTexture = new MethodInfo(Obf.FrameBuffer,          Obf.bindFramebufferTexture, Void.TYPE);
		MethodInfo sendChatMessage        = new MethodInfo(Obf.EntityClientPlayerMP, Obf.sendChatMessage,        Void.TYPE, String.class);

		InjectionPoint methodHead         = new MethodHead();
		InjectionPoint beforeFBORender    = new BeforeInvoke(framebufferRender);
		InjectionPoint beforeBindFBOTex   = new BeforeInvoke(bindFramebufferTexture);
		
		this.addEvent(Event.getOrCreate("sendChatMessage", true), sendChatMessage, methodHead)
			.addListener(new MethodInfo(Obf.CallbackProxyClient, "onOutboundChat"));
		
		this.addEvent(Event.getOrCreate("updateFramebufferSize", false), updateFramebufferSize, methodHead)
			.addListener(new MethodInfo(Obf.CallbackProxyClient, "onResize"));
		
		this.addEvent(Event.getOrCreate("preRenderFBO", false), runGameLoop, beforeFBORender)
			.addListener(new MethodInfo(Obf.CallbackProxyClient, "preRenderFBO"));
		
		this.addEvent(Event.getOrCreate("renderFBO", false), framebufferRender, beforeBindFBOTex)
			.addListener(new MethodInfo(Obf.CallbackProxyClient, "renderFBO"));
		
		this.addEvent(Event.getOrCreate("postRenderFBO", false), runGameLoop, InjectionPoint.after(beforeFBORender))
			.addListener(new MethodInfo(Obf.CallbackProxyClient, "postRenderFBO"));;
	}
}
