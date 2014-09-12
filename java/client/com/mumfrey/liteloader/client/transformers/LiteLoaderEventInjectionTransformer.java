package com.mumfrey.liteloader.client.transformers;

import com.mumfrey.liteloader.core.runtime.Obf;
import com.mumfrey.liteloader.transformers.event.Event;
import com.mumfrey.liteloader.transformers.event.EventInjectionTransformer;
import com.mumfrey.liteloader.transformers.event.InjectionPoint;
import com.mumfrey.liteloader.transformers.event.MethodInfo;
import com.mumfrey.liteloader.transformers.event.inject.BeforeInvoke;
import com.mumfrey.liteloader.transformers.event.inject.BeforeStringInvoke;
import com.mumfrey.liteloader.transformers.event.inject.MethodHead;

import static com.mumfrey.liteloader.core.runtime.Methods.*;

public class LiteLoaderEventInjectionTransformer extends EventInjectionTransformer
{
	@Override
	protected void addEvents()
	{
		InjectionPoint methodHead         = new MethodHead();
		InjectionPoint beforeFBORender    = new BeforeInvoke(framebufferRender);
		InjectionPoint beforeBindFBOTex   = new BeforeInvoke(bindFramebufferTexture);
		InjectionPoint beforePickProfiler = new BeforeStringInvoke("pick", endStartSection);
		
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
			
		this.addEvent(Event.getOrCreate("onRenderWorld", false), renderWorld, beforePickProfiler)
			.addListener(new MethodInfo(Obf.CallbackProxyClient, "onRenderWorld"));;
	}
}
