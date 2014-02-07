package com.mumfrey.liteloader.crashreport;

import java.util.concurrent.Callable;

import net.minecraft.crash.CrashReport;

import com.mumfrey.liteloader.core.LiteLoader;

public class CallableLiteLoaderBrand implements Callable<String>
{
	final CrashReport crashReport;
	
	public CallableLiteLoaderBrand(CrashReport report)
	{
		this.crashReport = report;
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.Callable#call()
	 */
	@Override
	public String call() throws Exception
	{
		String brand = LiteLoader.getBranding();
		return brand == null ? "Unknown / None" : brand;
	}
}
