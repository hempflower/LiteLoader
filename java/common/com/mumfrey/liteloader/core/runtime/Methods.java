package com.mumfrey.liteloader.core.runtime;

import com.mumfrey.liteloader.transformers.event.MethodInfo;

/**
 *
 * @author Adam Mummery-Smith
 */
public abstract class Methods
{
	public static final MethodInfo runGameLoop            = new MethodInfo(Obf.Minecraft,            Obf.runGameLoop,            Void.TYPE);
	public static final MethodInfo updateFramebufferSize  = new MethodInfo(Obf.Minecraft,            Obf.updateFramebufferSize,  Void.TYPE);
	public static final MethodInfo framebufferRender      = new MethodInfo(Obf.FrameBuffer,          Obf.framebufferRender,      Void.TYPE, Integer.TYPE, Integer.TYPE);
	public static final MethodInfo bindFramebufferTexture = new MethodInfo(Obf.FrameBuffer,          Obf.bindFramebufferTexture, Void.TYPE);
	public static final MethodInfo sendChatMessage        = new MethodInfo(Obf.EntityClientPlayerMP, Obf.sendChatMessage,        Void.TYPE, String.class);
	public static final MethodInfo renderWorld            = new MethodInfo(Obf.EntityRenderer,       Obf.renderWorld,            Void.TYPE, Float.TYPE, Long.TYPE);
	
	public static final MethodInfo startSection           = new MethodInfo(Obf.Profiler,             Obf.startSection,           Void.TYPE, String.class);
	public static final MethodInfo endSection             = new MethodInfo(Obf.Profiler,             Obf.endSection,             Void.TYPE);
	public static final MethodInfo endStartSection        = new MethodInfo(Obf.Profiler,             Obf.endStartSection,        Void.TYPE, String.class);
	
	private Methods() {}
}
