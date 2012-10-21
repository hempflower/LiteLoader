package com.mumfrey.liteloader.core;

import java.util.concurrent.Callable;

import net.minecraft.src.CrashReport;

public class CallableLiteLoaderMods implements Callable<String>
{
	final CrashReport crashReport;
	
	public CallableLiteLoaderMods(CrashReport report)
	{
		this.crashReport = report;
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.Callable#call()
	 */
	@Override
	public String call() throws Exception
	{
		return LiteLoader.getInstance().getLoadedModsList();
	}
}
