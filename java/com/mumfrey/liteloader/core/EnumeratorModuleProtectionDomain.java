package com.mumfrey.liteloader.core;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;

import net.minecraft.launchwrapper.LaunchClassLoader;

import com.mumfrey.liteloader.util.log.LiteLoaderLogger;

/**
 * Enumerator module which searches for mods on the classpath
 * 
 * @author Adam Mummery-Smith
 */
public class EnumeratorModuleProtectionDomain implements EnumeratorModule<File>
{
	private LoadableMod<File> codeSource;

	/**
	 * @param parent
	 * @param searchProtectionDomain
	 * @param searchClassPath
	 * @param loadTweaks
	 */
	public EnumeratorModuleProtectionDomain(boolean loadTweaks)
	{
		this.initPackagePath();
	}
	
	@Override
	public String toString()
	{
		return this.codeSource != null ? this.codeSource.getName() : "<None>";
	}
	
	private void initPackagePath()
	{
		try
		{
			URL protectionDomainLocation = EnumeratorModuleProtectionDomain.class.getProtectionDomain().getCodeSource().getLocation();
			if (protectionDomainLocation != null)
			{
				LiteLoaderLogger.info("Determining code source using protection domain");
				if (protectionDomainLocation.toString().indexOf('!') > -1 && protectionDomainLocation.toString().startsWith("jar:"))
				{
					LiteLoaderLogger.info("Protection domain references a jar file");
					protectionDomainLocation = new URL(protectionDomainLocation.toString().substring(4, protectionDomainLocation.toString().indexOf('!')));
				}
				
				File packagePath = new File(protectionDomainLocation.toURI());
				if (packagePath.isFile() && packagePath.getName().endsWith(".class"))
				{
					packagePath = packagePath.getParentFile();
				}
				
				this.codeSource = new LoadableModClassPath(packagePath);
			}
			else
			{
				LiteLoaderLogger.info("Determining code source using resource");
				String reflectionClassPath = EnumeratorModuleProtectionDomain.class.getResource("/net/minecraft/client/main/Main.class").getPath();
				
				if (reflectionClassPath.indexOf('!') > -1)
				{
					reflectionClassPath = URLDecoder.decode(reflectionClassPath, "UTF-8");
					this.codeSource = new LoadableModClassPath(new File(reflectionClassPath.substring(5, reflectionClassPath.indexOf('!'))));
				}
			}
		}
		catch (Throwable th)
		{
			LiteLoaderLogger.warning("Error determining local protection domain: %s", th.getMessage());
		}
	}
	
	@Override
	public void init(PluggableEnumerator enumerator)
	{
	}

	@Override
	public void writeSettings(PluggableEnumerator enumerator)
	{
	}

	@Override
	public void enumerate(PluggableEnumerator enumerator, EnabledModsList enabledModsList, String profile)
	{
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
		LiteLoaderLogger.info("Discovering mods in protection domain...");

		try
		{
			if (this.codeSource != null)
			{
				enumerator.registerModsFrom(this.codeSource, false);
			}
		}
		catch (Throwable th)
		{
			LiteLoaderLogger.warning("Error loading from local class path: %s", th.getMessage());
		}
	}
}
