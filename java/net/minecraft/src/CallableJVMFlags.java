package net.minecraft.src;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

import com.mumfrey.liteloader.core.CallableLiteLoaderBrand;
import com.mumfrey.liteloader.core.CallableLiteLoaderMods;

class CallableJVMFlags implements Callable<String>
{
	/** Reference to the CrashReport object. */
	final CrashReport theCrashReport;
	
	CallableJVMFlags(CrashReport par1CrashReport)
	{
		this.theCrashReport = par1CrashReport;
		par1CrashReport.getCategory().addCrashSectionCallable("Mod Pack", new CallableLiteLoaderBrand(par1CrashReport));
		par1CrashReport.getCategory().addCrashSectionCallable("LiteLoader Mods", new CallableLiteLoaderMods(par1CrashReport));
	}
	
	/**
	 * Returns the number of JVM Flags along with the passed JVM Flags.
	 */
	public String getJVMFlagsAsString()
	{
		RuntimeMXBean var1 = ManagementFactory.getRuntimeMXBean();
		List<String> var2 = var1.getInputArguments();
		int var3 = 0;
		StringBuilder var4 = new StringBuilder();
		Iterator<String> var5 = var2.iterator();
		
		while (var5.hasNext())
		{
			String var6 = var5.next();
			
			if (var6.startsWith("-X"))
			{
				if (var3++ > 0)
				{
					var4.append(" ");
				}
				
				var4.append(var6);
			}
		}
		
		return String.format("%d total; %s", new Object[] {Integer.valueOf(var3), var4.toString()});
	}
	
	@Override
	public String call()
	{
		return this.getJVMFlagsAsString();
	}
}
