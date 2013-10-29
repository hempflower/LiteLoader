package com.mumfrey.liteloader.core;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.minecraft.launchwrapper.LaunchClassLoader;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.mumfrey.liteloader.resources.ModResourcePack;
import com.mumfrey.liteloader.resources.ModResourcePackDir;

/**
 * Mod file reference for a file loaded from class path
 *
 * @author Adam Mummery-Smith
 */
public class ClassPathMod extends ModFile
{
	private static final long serialVersionUID = -4759310661966590773L;
	
	private static final Logger logger = Logger.getLogger("liteloader");

	ClassPathMod(File file, String fallbackName)
	{
		super(file, ClassPathMod.getVersionMetaDataString(file));

		if (this.modName == null) this.modName = fallbackName;
		if (this.targetVersion == null) this.targetVersion = LiteLoaderBootstrap.VERSION.getMinecraftVersion();
	}
	
	@Override
	protected String getDefaultName()
	{
		return null;
	}
	
	@Override
	public void initResourcePack(String name)
	{
		if (this.resourcePack == null)
		{
			if (this.isDirectory())
			{
				ClassPathMod.logger.info(String.format("Setting up \"%s/%s\" as mod resource pack with identifier \"%s\"", this.getParentFile().getName(), this.getName(), name));
				this.resourcePack = new ModResourcePackDir(name, this);
			}
			else
			{
				ClassPathMod.logger.info(String.format("Setting up \"%s\" as mod resource pack with identifier \"%s\"", this.getName(), name));
				this.resourcePack = new ModResourcePack(name, this);
			}
		}
	}
	
	@Override
	public boolean injectIntoClassPath(LaunchClassLoader classLoader, boolean injectIntoParent) throws MalformedURLException
	{
		// Can't inject a class path entry into the class path!
		return false;
	}
	
	@Override
	public boolean isInjected()
	{
		return true;
	}
	
	private static String getVersionMetaDataString(File file)
	{
		try
		{
			if (file.isDirectory())
			{
				File versionMetaFile = new File(file, "litemod.json");
				if (versionMetaFile.exists())
				{
					return Files.toString(versionMetaFile, Charsets.UTF_8);
				}
			}
			else
			{
				String strVersion = null;
				ZipFile modZip = new ZipFile(file);
				ZipEntry versionEntry = modZip.getEntry("litemod.json");
				if (versionEntry != null)
				{
					try
					{
						strVersion = ModFile.zipEntryToString(modZip, versionEntry);
					}
					catch (IOException ex) {}
				}
				
				modZip.close();
				return strVersion;
			}
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}
		
		return null;
	}
}
