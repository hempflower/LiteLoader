package com.mumfrey.liteloader.client.transformers;

import com.mumfrey.liteloader.core.runtime.Obf;
import com.mumfrey.liteloader.transformers.event.Event;
import com.mumfrey.liteloader.transformers.event.EventInjectionTransformer;
import com.mumfrey.liteloader.transformers.event.InjectionPoint;
import com.mumfrey.liteloader.transformers.event.MethodInfo;
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
	}
}
