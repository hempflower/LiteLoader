package com.mumfrey.liteloader.launch;

import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.minecraft.launchwrapper.LaunchClassLoader;
import sun.misc.URLClassPath;

/**
 * Nasty horrible reflection hack to inject a classpath entry at positons in classpath other than at the bottom
 * 
 * @author Adam Mummery-Smith
 */
public abstract class ClassPathInjector
{
	private static Logger logger = Logger.getLogger("liteloader");
	
	/**
	 * URLClassLoader::ucp -> instance of URLClassPath
	 */
	private static Field ucp;
	
	/**
	 * URLClassLoader::urls -> instance of Stack<URL> 
	 */
	private static Field classPathURLs;
	
	/**
	 * URLClassLoader::path -> instance of ArrayList<URL> 
	 */
	private static Field classPathPath;
	
	private static boolean canInject;
	
	static
	{
		try
		{
			ClassPathInjector.ucp = URLClassLoader.class.getDeclaredField("ucp");
			ClassPathInjector.ucp.setAccessible(true);
			ClassPathInjector.classPathURLs = URLClassPath.class.getDeclaredField("urls");
			ClassPathInjector.classPathURLs.setAccessible(true);
			ClassPathInjector.classPathPath = URLClassPath.class.getDeclaredField("path");
			ClassPathInjector.classPathPath.setAccessible(true);
			ClassPathInjector.canInject = true;
		}
		catch (Throwable th)
		{
			ClassPathInjector.logger.log(Level.SEVERE, "ClassPathInjector: Error initialising ClassPathInjector, special class path injection disabled", th);
			th.printStackTrace();
		}
	}
	
	/**
	 * Injects a URL into the classpath at the TOP of the stack
	 * 
	 * @param classLoader
	 * @param url
	 */
	public static void injectIntoClassPath(URLClassLoader classLoader, URL url)
	{
		ClassPathInjector.injectIntoClassPath(classLoader, url, null);
	}
	
	/**
	 * Injects a URL into the classpath at the TOP of the stack
	 * 
	 * @param classLoader
	 * @param url
	 * @param above
	 */
	@SuppressWarnings({ "unchecked" })
	public static void injectIntoClassPath(URLClassLoader classLoader, URL url, URL above)
	{
		if (ClassPathInjector.canInject)
		{
			ClassPathInjector.logger.info(String.format("ClassPathInjector: attempting to inject %s into %s", url, classLoader.getClass().getSimpleName()));
			
			try
			{
				URLClassPath classPath = (URLClassPath)ClassPathInjector.ucp.get(classLoader);
				
				Stack<URL> urls = (Stack<URL>)ClassPathInjector.classPathURLs.get(classPath);
				ArrayList<URL> path = (ArrayList<URL>)ClassPathInjector.classPathPath.get(classPath);
				
				synchronized (urls)
				{
					if (!path.contains(url))
					{
						urls.add(url);
						
						if (above == null)
						{
							path.add(0, url);
						}
						else
						{
							for (int pos = path.size() - 1; pos > 0; pos--)
							{
								if (above.equals(path.get(pos)))
									path.add(pos, url);
							}
						}
					}
				}
			}
			catch (Exception ex)
			{
				ClassPathInjector.logger.warning(String.format("ClassPathInjector: failed to inject %s", url));
			}
		}
		
		if (classLoader instanceof LaunchClassLoader)
		{
			((LaunchClassLoader)classLoader).addURL(url);
		}
	}

	/**
	 * @param classLoader
	 * @param url
	 * @param above
	 */
	public static void injectIntoClassPath(LaunchClassLoader classLoader, URL url, String above)
	{
		above = above.trim().toLowerCase();
		if (above.length() < 1) return;
		
		for (URL classPathUrl : classLoader.getURLs())
		{
			if (classPathUrl.toString().toLowerCase().contains(above))
			{
				ClassPathInjector.injectIntoClassPath(classLoader, url, classPathUrl);
				return;
			}
		}
	}
}
