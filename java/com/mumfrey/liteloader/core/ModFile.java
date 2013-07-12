package com.mumfrey.liteloader.core;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.mumfrey.liteloader.resources.ModResourcePack;

/**
 * Wrapper for file which represents a mod file to load with associated version information and
 * metadata. Retrieve this from litemod.xml at enumeration time. We also override comparable to 
 * provide our own custom sorting logic based on version info.
 *
 * @author Adam Mummery-Smith
 */
public class ModFile extends File
{
	private static final long serialVersionUID = -7952147161905688459L;

	/**
	 * Gson parser for JSON
	 */
	private static Gson gson = new Gson();
	
	/**
	 * True if the metadata information is parsed successfully, the mod will be added
	 */
	private boolean valid = false;
	
	/**
	 * True if parsed from JSON, false if fallback mode using legacy version.txt
	 */
	private boolean json = false;
	
	/**
	 * Name of the mod specified in the JSON file, this can be any string but should be the same between mod versions
	 */
	private String modName;
	
	/**
	 * Loader version
	 */
	private String version;
	
	/**
	 * File time stamp, used as sorting criteria when no revision information is found
	 */
	private long timeStamp;
	
	/**
	 * Revision number from the json file
	 */
	private float revision = 0.0F;
	
	/**
	 * True if the revision number was successfully read, used as a semaphore so that we know when revision is a valid number
	 */
	private boolean hasRevision = false;
	
	/**
	 * Resource pack we have registered with minecraft
	 */
	private ModResourcePack resourcePack = null;
	
	/**
	 * ALL of the parsed metadata from the file, associated with the mod later on for retrieval via the loader
	 */
	private HashMap<String, String> metaData = new HashMap<String, String>();
	
	/**
	 * @param file
	 * @param strVersion
	 */
	public ModFile(File file, String strVersion)
	{
		super(file.getAbsolutePath());
		
		this.timeStamp = this.lastModified();
		
		this.parseVersionFile(strVersion);
	}

	@SuppressWarnings("unchecked")
	private void parseVersionFile(String strVersionData)
	{
		// Assume that it's json if the file starts with a brace
		if (strVersionData.trim().startsWith("{"))
		{
			try
			{
				this.metaData = ModFile.gson.fromJson(strVersionData, HashMap.class);
			}
			catch (JsonSyntaxException jsx)
			{
				LiteLoader.getLogger().warning("Error reading litemod.json in " + this.getName() + ", JSON syntax exception: " + jsx.getMessage());
				return;
			}
			
			this.modName = this.metaData.get("name");
			
			this.version = this.metaData.get("mcversion");
			if (this.version == null)
			{
				LiteLoader.getLogger().warning("Mod in " + this.getName() + " has no loader version number reading litemod.json");
				return;
			}
			
			try
			{
				this.revision = Float.parseFloat(this.metaData.get("revision"));
				this.hasRevision = true;
			}
			catch (Exception ex)
			{
				LiteLoader.getLogger().warning("Mod in " + this.getName() + " has an invalid revision number reading litemod.json");
			}

			this.valid = true;
			this.json = true;
		}
		else
		{
			// Legacy version.txt file
			this.version = strVersionData;
			this.valid = true;
		}
		
		if (this.modName == null)
		{
			this.modName = this.getName().replaceAll("[^a-zA-Z]", "");
		}
	}
	
	public String getModName()
	{
		return this.modName;
	}
	
	public boolean isValid()
	{
		return this.valid;
	}
	
	public boolean isJson()
	{
		return this.json;
	}
	
	public String getVersion()
	{
		return this.version;
	}
	
	public float getRevision()
	{
		return this.revision;
	}
	
	public String getMetaValue(String metaKey, String defaultValue)
	{
		return this.metaData.containsKey(metaKey) ? this.metaData.get(metaKey) : defaultValue;
	}

	public Map<String, String> getMetaData()
	{
		return this.metaData;
	}
	
	/**
	 * Registers this file as a minecraft resource pack 
	 * 
	 * @param name
	 * @return true if the pack was added
	 */
	public boolean registerAsResourcePack(String name)
	{
		if (this.resourcePack == null)
		{
			this.resourcePack = new ModResourcePack(name, this);
			return LiteLoader.getInstance().registerModResourcePack(this.resourcePack);
		}
		
		return false;
	}
	
	@Override
	public int compareTo(File other)
	{
		if (other == null || !(other instanceof ModFile)) return -1;
		
		ModFile otherMod = (ModFile)other;
		
		// If the other object has a revision, compare revisions
		if (otherMod.hasRevision)
		{
			return this.hasRevision && this.revision - otherMod.revision > 0 ? -1 : 1;
		}

		// If we have a revision and the other object doesn't, then we are higher
		if (this.hasRevision)
		{
			return -1;
		}

		// Give up and use timestamp
		return (int)(otherMod.timeStamp - this.timeStamp);
	}
}
