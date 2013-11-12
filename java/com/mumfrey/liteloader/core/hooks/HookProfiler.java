package com.mumfrey.liteloader.core.hooks;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

import com.mumfrey.liteloader.core.Events;
import com.mumfrey.liteloader.core.LiteLoader;
import com.mumfrey.liteloader.core.exceptions.ProfilerCrossThreadAccessException;
import com.mumfrey.liteloader.core.exceptions.ProfilerStackCorruptionException;

import net.minecraft.src.Minecraft;
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
	 * Cross-thread sanity check 
	 */
	private final Thread minecraftThread;
	
	/**
	 * Logger instance
	 */
	private Logger logger;
	
	/**
	 * Event manager instance which will receive callbacks
	 */
	private Events events;
	
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
	 * @param events LiteLoader object which will get callbacks
	 * @param logger Logger instance
	 */
	public HookProfiler(Events events)
	{
		this.mc = Minecraft.getMinecraft();

		this.events = events;
		this.logger = LiteLoader.getLogger();
		
		// Detect optifine (duh!)
		this.detectOptifine();
		
		this.minecraftThread = Thread.currentThread();
	}

	/**
	 * Try to detect optifine using reflection
	 * 
	 * @param logger
	 */
	private void detectOptifine()
	{
		try
		{
			this.ofProfiler = GameSettings.class.getDeclaredField("ofProfiler");
		}
		catch (SecurityException ex) {}
		catch (NoSuchFieldException ex)
		{
			this.logger.info("Optifine not detected");
		}
		finally
		{
			if (this.ofProfiler != null)
			{
				this.logger.info(String.format("Optifine version %s detected, enabling compatibility check", this.getOptifineVersion()));
			}
		}
	}
	
	/**
	 * Try to get the optifine version using reflection
	 * 
	 * @return
	 */
	private String getOptifineVersion()
	{
		try
		{
			Class<?> config = Class.forName("Config");
			
			if (config != null)
			{
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
		if (Thread.currentThread() != this.minecraftThread)
		{
			this.logger.severe("Profiler cross thread access detected, this indicates an error with one of your mods.");
			throw new ProfilerCrossThreadAccessException(Thread.currentThread().getName());
		}
		
		if (!this.initDone)
		{
			this.initDone = true;
			this.events.preBeginGame();
		}
		
		if ("gameRenderer".equals(sectionName) && "root".equals(this.sectionStack.getLast()))
		{
			this.events.onRender();
		}
		else if ("frustrum".equals(sectionName) && "level".equals(this.sectionStack.getLast()))
		{
			this.events.onSetupCameraTransform();
		}
		else if ("litParticles".equals(sectionName))
		{
			this.events.postRenderEntities();
		}
		else if ("tick".equals(sectionName) && "root".equals(this.sectionStack.getLast()))
		{
			this.events.onTimerUpdate();
		}
		else if ("chat".equals(sectionName))
		{
			this.events.onRenderChat();
		}
		else if ("gui".equals(sectionName) && "gameRenderer".equals(this.sectionStack.getLast()))
		{
			this.events.onRenderHUD();
		}
		
		if ("animateTick".equals(sectionName)) this.tick = true;
		this.sectionStack.add(sectionName);
		super.startSection(sectionName);

		if (this.ofProfiler != null)
		{
			try
			{
				this.ofProfiler.set(this.mc.gameSettings, true);
			}
			catch (IllegalArgumentException ex)
			{
				this.ofProfiler = null;
			}
			catch (IllegalAccessException ex)
			{
				this.ofProfiler = null;
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see net.minecraft.src.Profiler#endSection()
	 */
	@Override
	public void endSection()
	{
		if (Thread.currentThread() != this.minecraftThread)
		{
			this.logger.severe("Profiler cross thread access detected, this indicates an error with one of your mods.");
			throw new ProfilerCrossThreadAccessException(Thread.currentThread().getName());
		}
		
		super.endSection();
		
		try
		{
			String endingSection = this.sectionStack.size() > 0 ? this.sectionStack.removeLast() : null;
			String nextSection = this.sectionStack.size() > 0 ? this.sectionStack.getLast() : null;
			
			if ("gameRenderer".equals(endingSection) && "root".equals(nextSection))
			{
				super.startSection("litetick");
				
				this.events.onTick(this, this.tick);
				this.tick = false;
				
				super.endSection();
			}
			else
			{
				if ("gui".equals(endingSection) && "gameRenderer".equals(nextSection) && this.mc.theWorld != null)
				{
					this.events.postRenderHUD();
					this.events.preRenderGUI();
				}
				else if ("mouse".equals(endingSection) && "gameRenderer".equals(nextSection) && (this.mc.skipRenderWorld || this.mc.theWorld == null))
				{
					this.events.preRenderGUI();
				}
				else if ("hand".equals(endingSection) && "level".equals(nextSection))
				{
					this.events.postRender();
				}
				else if ("chat".equals(endingSection))
				{
					this.events.postRenderChat();
				}
			}
		}
		catch (NoSuchElementException ex)
		{
			this.logger.severe("Corrupted Profiler stack detected, this indicates an error with one of your mods.");
			throw new ProfilerStackCorruptionException("Corrupted Profiler stack detected");
		}
	}
}