package com.mumfrey.liteloader.core;

import java.io.File;

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

	public ClassPathMod(File file, String name, String version)
	{
		super(file, "");

		this.modName = name;
		this.version = version;
	}

	@Override
	protected void parseVersionFile(String strVersionData)
	{
		// Nope
	}
	
	@Override
	public boolean registerAsResourcePack(String name)
	{
		if (this.resourcePack == null)
		{
			if (this.isDirectory())
			{
				LiteLoader.getLogger().info(String.format("Registering \"%s/%s\" as mod resource pack with identifier \"%s\"", this.getParentFile().getName(), this.getName(), name));
				this.resourcePack = new ModResourcePackDir(name, this);
			}
			else
			{
				LiteLoader.getLogger().info(String.format("Registering \"%s\" as mod resource pack with identifier \"%s\"", this.getName(), name));
				this.resourcePack = new ModResourcePack(name, this);
			}
			
			return LiteLoader.getInstance().registerModResourcePack(this.resourcePack);
		}
		
		return false;
	}
}
