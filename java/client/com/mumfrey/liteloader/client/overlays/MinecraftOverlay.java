package com.mumfrey.liteloader.client.overlays;

import java.util.List;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.Timer;

import com.google.common.collect.Lists;
import com.mumfrey.liteloader.transformers.Obfuscated;
import com.mumfrey.liteloader.transformers.Stub;

/**
 * Overlay to inject accessors into Minecraft main class
 * 
 * @author Adam Mummery-Smith
 */
public abstract class MinecraftOverlay implements IMinecraft
{
	@SuppressWarnings("unused")
	private static Minecraft __TARGET;
	
	// TODO Obfuscation 1.7.10
	// Fields
	@Obfuscated({"field_71428_T", "Q"}) private Timer timer;
	@Obfuscated({"field_71424_I", "z"}) private Profiler mcProfiler;
	@Obfuscated({"field_71425_J", "A"}) private boolean running;
	@Obfuscated({"field_110449_ao", "ap"}) private List<?> defaultResourcePacks = Lists.newArrayList();
	@Obfuscated({"field_71475_ae", "af"}) private String serverName;
	@Obfuscated({"field_71477_af", "ag"}) private int serverPort;
	
	// Methods
	@Obfuscated({"func_71370_a", "a"}) @Stub abstract void resize(int width, int height);
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.client.overlays.IMinecraft#getTimer()
	 */
	@Override
	public Timer getTimer()
	{
		return this.timer;
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.client.overlays.IMinecraft#isRunning()
	 */
	@Override
	public boolean isRunning()
	{
		return this.running;
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.client.overlays.IMinecraft#getDefaultResourcePacks()
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<IResourcePack> getDefaultResourcePacks()
	{
		return (List<IResourcePack>)this.defaultResourcePacks;
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.client.overlays.IMinecraft#setSize(int, int)
	 */
	@Override
	public void setSize(int width, int height)
	{
		try
		{
			Display.setDisplayMode(new DisplayMode(width, height));
			this.resize(width, height);
			Display.setVSyncEnabled(Minecraft.getMinecraft().gameSettings.enableVsync);
		}
		catch (LWJGLException ex)
		{
			ex.printStackTrace();
		}
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.client.overlays.IMinecraft#getServerName()
	 */
	@Override
	public String getServerName()
	{
		return this.serverName;
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.client.overlays.IMinecraft#getServerPort()
	 */
	@Override
	public int getServerPort()
	{
		return this.serverPort;
	}
}
