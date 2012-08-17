package com.mumfrey.liteloader.core;

import java.util.LinkedList;
import java.util.logging.Logger;

import net.minecraft.src.Profiler;

public class LiteLoaderHook extends Profiler
{
	private Logger logger;
	
	private LiteLoader core;
	
	private LinkedList<String> sections = new LinkedList<String>();
	
	private boolean tick;
	
	public LiteLoaderHook(LiteLoader core, Logger logger)
	{
		this.core = core;
		this.logger = logger;
	}

	/* (non-Javadoc)
	 * @see net.minecraft.src.Profiler#startSection(java.lang.String)
	 */
	@Override
	public void startSection(String sectionName)
	{
		if (sectionName.equals("animateTick")) tick = true;
		sections.add(sectionName);
		super.startSection(sectionName);
	}

	/* (non-Javadoc)
	 * @see net.minecraft.src.Profiler#endSection()
	 */
	@Override
	public void endSection()
	{
		super.endSection();

		String endingSection = sections.removeLast();

		if (endingSection.equalsIgnoreCase("gameRenderer") && sections.getLast().equalsIgnoreCase("root"))
		{
			super.startSection("litetick");

			core.onTick(tick);
			tick = false;
			
			super.endSection();
		}
	}

}
