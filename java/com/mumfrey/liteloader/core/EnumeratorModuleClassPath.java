package com.mumfrey.liteloader.core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.launchwrapper.LaunchClassLoader;

import com.mumfrey.liteloader.util.log.LiteLoaderLogger;

/**
 * Enumerator module which searches for mods on the classpath
 * 
 * @author Adam Mummery-Smith
 */
public class EnumeratorModuleClassPath implements EnumeratorModule<File>
{
	/**
	 * Array of class path entries specified to the JVM instance 
	 */
	private final String[] classPathEntries;
	
	/**
	 * URLs to add once init is completed
	 */
	private final List<LoadableMod<File>> loadableMods = new ArrayList<LoadableMod<File>>();
	
	private boolean loadTweaks;

	/**
	 * @param parent
	 * @param searchProtectionDomain
	 * @param searchClassPath
	 * @param loadTweaks
	 */
	public EnumeratorModuleClassPath(boolean loadTweaks)
	{
		// Read the JVM class path into the local array
		this.classPathEntries = this.readClassPath();
		
		this.loadTweaks = loadTweaks;
	}
	
	@Override
	public String toString()
	{
		return "<Java Class Path>";
	}
	
	@Override
	public void init(PluggableEnumerator enumerator)
	{
	}

	@Override
	public void writeSettings(PluggableEnumerator enumerator)
	{
	}

	/**
	 * Reads the class path entries that were supplied to the JVM and returns them as an array
	 */
	private String[] readClassPath()
	{
		LiteLoaderLogger.info("Enumerating class path...");
		
		String classPath = System.getProperty("java.class.path");
		String classPathSeparator = System.getProperty("path.separator");
		String[] classPathEntries = classPath.split(classPathSeparator);
		
		LiteLoaderLogger.info("Class path separator=\"%s\"", classPathSeparator);
		LiteLoaderLogger.info("Class path entries=(\n   classpathEntry=%s\n)", classPath.replace(classPathSeparator, "\n   classpathEntry="));
		return classPathEntries;
	}

	@Override
	public void enumerate(PluggableEnumerator enumerator, EnabledModsList enabledModsList, String profile)
	{
		if (this.loadTweaks)
		{
			LiteLoaderLogger.info("Discovering tweaks on class path...");
			
			for (String classPathPart : this.classPathEntries)
			{
				File packagePath = new File(classPathPart);
				if (packagePath.exists())
				{
					LoadableModClassPath classPathMod = new LoadableModClassPath(packagePath);
					if (enumerator.isContainerEnabled(classPathMod))
					{
						this.loadableMods.add(classPathMod);
						if (classPathMod.hasTweakClass() || classPathMod.hasClassTransformers())
						{
							enumerator.registerTweakContainer(classPathMod);
						}
					}
					else
					{
						LiteLoaderLogger.info("Mod %s is disabled or missing a required dependency, not injecting tranformers", classPathMod.getIdentifier());
					}
				}
			}
		}
	}
	
	@Override
	public void injectIntoClassLoader(PluggableEnumerator enumerator, LaunchClassLoader classLoader, EnabledModsList enabledModsList, String profile)
	{
	}

	/**
	 * @param classLoader
	 */
	@Override
	public void registerMods(PluggableEnumerator enumerator, LaunchClassLoader classLoader)
	{
		LiteLoaderLogger.info("Discovering mods on class path...");
		
		for (LoadableMod<File> classPathMod : this.loadableMods)
		{
			LiteLoaderLogger.info("Searching %s...", classPathMod);
			enumerator.registerModsFrom(classPathMod, true);
		}
	}
}
