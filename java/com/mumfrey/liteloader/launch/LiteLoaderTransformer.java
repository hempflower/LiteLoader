package com.mumfrey.liteloader.launch;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.LaunchClassLoader;

public class LiteLoaderTransformer implements IClassTransformer
{
	private static final String classMappingRenderLightningBolt = "net.minecraft.src.RenderLightningBolt";
	
	// TODO Obfuscation 1.6.4
	private static final String classMappingRenderLightningBoltObf = "bha";

	private static Logger logger = Logger.getLogger("liteloader");
	
	public static LaunchClassLoader launchClassLoader;

	public static List<String> modsToLoad;
	
	public static File gameDirectory;
	
	public static File assetsDirectory;
	
	public static String profile;
	
	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass)
	{
		if (classMappingRenderLightningBolt.equals(name) || classMappingRenderLightningBoltObf.equals(name))
		{
			logger.info("Beginning LiteLoader Init...");
			
			try
			{
				Class<?> loaderClass = Class.forName("com.mumfrey.liteloader.core.LiteLoader", false, LiteLoaderTransformer.launchClassLoader);
				Method mInit = loaderClass.getDeclaredMethod("init", File.class, File.class, String.class, List.class, LaunchClassLoader.class);
				mInit.setAccessible(true);
				mInit.invoke(null, LiteLoaderTransformer.gameDirectory, LiteLoaderTransformer.assetsDirectory, LiteLoaderTransformer.profile, LiteLoaderTransformer.modsToLoad, LiteLoaderTransformer.launchClassLoader);
			}
			catch (Throwable th)
			{
				logger.log(Level.SEVERE, String.format("Error initialising LiteLoader: %s", th.getMessage()), th);
			}
		}
		
		return basicClass;
	}
}
