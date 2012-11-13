package com.mumfrey.liteloader.core;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.logging.Logger;

import net.minecraft.client.Minecraft;
import net.minecraft.src.GameSettings;
import net.minecraft.src.Profiler;

/**
 * Main LiteLoader tick hook 
 *
 * @author Adam Mummery-Smith
 */
public class HookProfiler extends Profiler
{
	/**
	 * Logger instance
	 */
	private Logger logger;
	
	/**
	 * LiteLoader instance which will receive callbacks
	 */
	private LiteLoader loader;
	
	/**
	 * Section list, used as a kind of stack to determine where we are in the profiler stack
	 */
	private LinkedList<String> sectionStack = new LinkedList<String>();

	/**
	 * Initialisation done
	 */
	private boolean initDone = false;
	
	/**
	 * Tick clock, sent as a flag to the core onTick so that mods know it's a new tick
	 */
	private boolean tick;
	
	/**
	 * Optifine compatibility, pointer to the "Profiler" setting so we can enable it if it's disabled
	 */
	private Field ofProfiler;
	
	/**
	 * Minecraft reference, only set if optifine compatibility is enabled 
	 */
	private Minecraft mc;
	
	/**
	 * .ctor
	 * 
	 * @param core LiteLoader object which will get callbacks
	 * @param logger Logger instance
	 */
	public HookProfiler(LiteLoader core, Logger logger)
	{
		this.loader = core;
		this.logger = logger;
		
		// Detect optifine (duh!)
		DetectOptifine();
	}

	/**
	 * Try to detect optifine using reflection
	 * 
	 * @param logger
	 */
	private void DetectOptifine()
	{
		try
		{
			ofProfiler = GameSettings.class.getDeclaredField("ofProfiler");
		}
		catch (SecurityException ex) {}
		catch (NoSuchFieldException ex)
		{
			logger.info("Optifine not detected");
		}
		finally
		{
			if (ofProfiler != null)
			{
				logger.info(String.format("Optifine version %s detected, enabling compatibility check", GetOptifineVersion()));
				mc = Minecraft.getMinecraft();
			}
		}
	}
	
	/**
	 * Try to get the optifine version using reflection
	 * 
	 * @return
	 */
	private String GetOptifineVersion()
	{
		try
		{
			Class config = Class.forName("Config");
			
			if (config != null)
			{
				@SuppressWarnings("unchecked")
				Method getVersion = config.getDeclaredMethod("getVersion");
				
				if (getVersion != null)
				{
					return (String)getVersion.invoke(null);
				}
			}
		}
		catch (Exception ex) {}
		
		return "Unknown";
	}
	
	/* (non-Javadoc)
	 * @see net.minecraft.src.Profiler#startSection(java.lang.String)
	 */
	@Override
	public void startSection(String sectionName)
	{
		if (!initDone)
		{
			initDone = true;
			loader.onInit();
		}
		
		if ("gameRenderer".equalsIgnoreCase(sectionName) && "root".equalsIgnoreCase(sectionStack.getLast()))
		{
			loader.onRender();
		}
			
		if ("animateTick".equals(sectionName)) tick = true;
		sectionStack.add(sectionName);
		super.startSection(sectionName);

		if (ofProfiler != null)
		{
			try
			{
				ofProfiler.set(mc.gameSettings, true);
			}
			catch (IllegalArgumentException ex)
			{
				ofProfiler = null;
			}
			catch (IllegalAccessException ex)
			{
				ofProfiler = null;
			}
		}
	}

	/* (non-Javadoc)
	 * @see net.minecraft.src.Profiler#endSection()
	 */
	@Override
	public void endSection()
	{
		super.endSection();
		
		String endingSection = sectionStack.removeLast();

		if ("gameRenderer".equalsIgnoreCase(endingSection) && "root".equalsIgnoreCase(sectionStack.getLast()))
		{
			super.startSection("litetick");

			loader.onTick(this, tick);
			tick = false;
			
			super.endSection();
		}
	}
}
