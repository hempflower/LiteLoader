package com.mumfrey.liteloader.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;

import net.minecraft.client.Minecraft;

public class FilteringClassLoader extends URLClassLoader
{
	public FilteringClassLoader()
	{
		super(new URL[0], Minecraft.class.getClassLoader());
	}

	/* (non-Javadoc)
	 * @see java.lang.ClassLoader#loadClass(java.lang.String)
	 */
	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException
	{
		System.out.println("Parent is trying to load class " + name);
		return super.loadClass(name);
	}

	/* (non-Javadoc)
	 * @see java.lang.ClassLoader#findClass(java.lang.String)
	 */
	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException
	{
		System.out.println("findClass " + name);
		// TODO Auto-generated method stub
		return super.findClass(name);
	}

	/* (non-Javadoc)
	 * @see java.lang.ClassLoader#getResource(java.lang.String)
	 */
	@Override
	public URL getResource(String name)
	{
		System.out.println("getResource " + name);
		// TODO Auto-generated method stub
		return super.getResource(name);
	}

	/* (non-Javadoc)
	 * @see java.lang.ClassLoader#getResources(java.lang.String)
	 */
	@Override
	public Enumeration<URL> getResources(String name) throws IOException
	{
		System.out.println("getResources " + name);
		// TODO Auto-generated method stub
		return super.getResources(name);
	}

	/* (non-Javadoc)
	 * @see java.lang.ClassLoader#findResource(java.lang.String)
	 */
	@Override
	public URL findResource(String name)
	{
		System.out.println("findResource " + name);
		// TODO Auto-generated method stub
		return super.findResource(name);
	}

	/* (non-Javadoc)
	 * @see java.lang.ClassLoader#findResources(java.lang.String)
	 */
	@Override
	public Enumeration<URL> findResources(String name) throws IOException
	{
		System.out.println("findResources " + name);
		// TODO Auto-generated method stub
		return super.findResources(name);
	}

	/* (non-Javadoc)
	 * @see java.lang.ClassLoader#getResourceAsStream(java.lang.String)
	 */
	@Override
	public InputStream getResourceAsStream(String name)
	{
		System.out.println("getResourceAsStream " + name);
		// TODO Auto-generated method stub
		return super.getResourceAsStream(name);
	}

	/* (non-Javadoc)
	 * @see java.lang.ClassLoader#getPackage(java.lang.String)
	 */
	@Override
	protected Package getPackage(String name)
	{
		System.out.println("getPackage " + name);
		// TODO Auto-generated method stub
		return super.getPackage(name);
	}

	/* (non-Javadoc)
	 * @see java.lang.ClassLoader#getPackages()
	 */
	@Override
	protected Package[] getPackages()
	{
		System.out.println("getPackages");
		// TODO Auto-generated method stub
		return super.getPackages();
	}

	/* (non-Javadoc)
	 * @see java.lang.ClassLoader#findLibrary(java.lang.String)
	 */
	@Override
	protected String findLibrary(String libname)
	{
		System.out.println("findLibrary " + libname);
		// TODO Auto-generated method stub
		return super.findLibrary(libname);
	}

	/* (non-Javadoc)
	 * @see java.lang.ClassLoader#setDefaultAssertionStatus(boolean)
	 */
	@Override
	public synchronized void setDefaultAssertionStatus(boolean enabled)
	{
		System.out.println("setDefaultAssertionStatus " + enabled);
		// TODO Auto-generated method stub
		super.setDefaultAssertionStatus(enabled);
	}

	/* (non-Javadoc)
	 * @see java.lang.ClassLoader#setPackageAssertionStatus(java.lang.String, boolean)
	 */
	@Override
	public synchronized void setPackageAssertionStatus(String packageName, boolean enabled)
	{
		System.out.println("setPackageAssertionStatus " + packageName + "   " + enabled);
		// TODO Auto-generated method stub
		super.setPackageAssertionStatus(packageName, enabled);
	}

	/* (non-Javadoc)
	 * @see java.lang.ClassLoader#setClassAssertionStatus(java.lang.String, boolean)
	 */
	@Override
	public synchronized void setClassAssertionStatus(String className, boolean enabled)
	{
		System.out.println("setClassAssertionStatus " + className + "   " + enabled);
		// TODO Auto-generated method stub
		super.setClassAssertionStatus(className, enabled);
	}

	/* (non-Javadoc)
	 * @see java.lang.ClassLoader#clearAssertionStatus()
	 */
	@Override
	public synchronized void clearAssertionStatus()
	{
		System.out.println("clearAssertionStatus");
		// TODO Auto-generated method stub
		super.clearAssertionStatus();
	}
}
