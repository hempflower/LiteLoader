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
		InjectionPoint head = new MethodHead();
		
		Event sendChatMessage = Event.getOrCreate("sendChatMessage", true);
		MethodInfo sendChatMessageTarget = new MethodInfo(Obf.EntityClientPlayerMP, Obf.sendChatMessage, Void.TYPE, String.class);
		this.addEvent(sendChatMessage, sendChatMessageTarget, head).addListener(new MethodInfo(Obf.CallbackProxyClient, "onOutboundChat"));
		
		Event updateFramebufferSize = Event.getOrCreate("updateFramebufferSize", false);
		MethodInfo updateFramebufferSizeTarget = new MethodInfo(Obf.Minecraft, Obf.updateFramebufferSize, Void.TYPE);
		this.addEvent(updateFramebufferSize, updateFramebufferSizeTarget, head).addListener(new MethodInfo(Obf.CallbackProxyClient, "onResize"));
		
		MethodInfo framebufferRender = new MethodInfo(Obf.FrameBuffer, Obf.framebufferRender, "(II)V");
		BeforeInvoke beforeFramebufferRender = new BeforeInvoke(framebufferRender);
		
		Event preRenderFBO = Event.getOrCreate("preRenderFBO", false);
		MethodInfo runGameLoop = new MethodInfo(Obf.Minecraft, Obf.runGameLoop, Void.TYPE);
		this.addEvent(preRenderFBO, runGameLoop, beforeFramebufferRender).addListener(new MethodInfo(Obf.CallbackProxyClient, "preRenderFBO"));
		
		Event renderFBO = Event.getOrCreate("renderFBO", false);
		this.addEvent(renderFBO, framebufferRender, new BeforeInvoke(new MethodInfo(Obf.FrameBuffer, Obf.bindFramebufferTexture, Void.TYPE))).addListener(new MethodInfo(Obf.CallbackProxyClient, "renderFBO"));
		
		Event postRenderFBO = Event.getOrCreate("postRenderFBO", false);
		this.addEvent(postRenderFBO, runGameLoop, InjectionPoint.after(beforeFramebufferRender)).addListener(new MethodInfo(Obf.CallbackProxyClient, "postRenderFBO"));;
	}
}
