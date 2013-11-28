package com.mumfrey.liteloader.launch;

import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Stack;

import net.minecraft.launchwrapper.LaunchClassLoader;
import sun.misc.URLClassPath;

/**
 * Nasty horrible reflection hack to inject a classpath entry at the top of the classpath stack
 * 
 * @author Adam Mummery-Smith
 */
public abstract class ClassPathInjector
{
	private static Field ucp;
	private static Field classPathURLs;
	private static Field classPathPath;
	
	static
	{
		try
		{
			ucp = URLClassLoader.class.getDeclaredField("ucp");
			ucp.setAccessible(true);
			classPathURLs = URLClassPath.class.getDeclaredField("urls");
			classPathURLs.setAccessible(true);
			classPathPath = URLClassPath.class.getDeclaredField("path");
			classPathPath.setAccessible(true);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	/**
	 * Injects a URL into the classpath at the TOP of the stack
	 * 
	 * @param classLoader
	 * @param url
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void injectIntoClassPath(URLClassLoader classLoader, URL url)
	{
		try
		{
			URLClassPath classPath = (URLClassPath)ucp.get(classLoader);
			
			Stack urls = (Stack)classPathURLs.get(classPath);
			ArrayList path = (ArrayList)classPathPath.get(classPath);
			
			synchronized (urls)
			{
				if (!path.contains(url))
				{
					urls.add(url);
					path.add(0, url);
				}
			}
		}
		catch (Exception ex) {}
		
		if (classLoader instanceof LaunchClassLoader)
		{
			((LaunchClassLoader)classLoader).addURL(url);
		}
	}
}
