package com.mumfrey.liteloader.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.minecraft.launchwrapper.LaunchClassLoader;
import joptsimple.internal.Strings;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.mumfrey.liteloader.launch.LiteLoaderTweaker;
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

	private static final Logger logger = Logger.getLogger("liteloader");

	/**
	 * Gson parser for JSON
	 */
	protected static Gson gson = new Gson();
	
	/**
	 * True if the metadata information is parsed successfully, the mod will be added
	 */
	protected boolean valid = false;
	
	/**
	 * Name of the mod specified in the JSON file, this can be any string but should be the same between mod versions
	 */
	protected String modName;
	
	/**
	 * Loader version
	 */
	protected String targetVersion;
	
	/**
	 * Name of the tweak class
	 */
	protected String tweakClassName;
	
	/**
	 * Name of the class transof
	 */
	protected String classTransformerClassName;
	
	/**
	 * File time stamp, used as sorting criteria when no revision information is found
	 */
	protected long timeStamp;
	
	/**
	 * Revision number from the json file
	 */
	protected float revision = 0.0F;
	
	/**
	 * True if the revision number was successfully read, used as a semaphore so that we know when revision is a valid number
	 */
	protected boolean hasRevision = false;
	
	/**
	 * Resource pack we have registered with minecraft
	 */
	protected Object resourcePack = null;
	
	/**
	 * ALL of the parsed metadata from the file, associated with the mod later on for retrieval via the loader
	 */
	protected Map<String, String> metaData = new HashMap<String, String>();

	/**
	 * True once this file has been injected into the class path 
	 */
	private boolean injected;
	
	/**
	 * @param file
	 * @param strVersion
	 */
	ModFile(File file, String strVersion)
	{
		super(file.getAbsolutePath());
		
		this.timeStamp = this.lastModified();
		this.parseVersionFile(strVersion);
	}

	@SuppressWarnings("unchecked")
	protected void parseVersionFile(String strVersionData)
	{
		if (Strings.isNullOrEmpty(strVersionData)) return;
		
		try
		{
			this.metaData = ModFile.gson.fromJson(strVersionData, HashMap.class);
		}
		catch (JsonSyntaxException jsx)
		{
			ModFile.logger.warning("Error reading litemod.json in " + this.getAbsolutePath() + ", JSON syntax exception: " + jsx.getMessage());
			return;
		}
		
		this.modName = this.metaData.get("name");
		this.targetVersion = this.metaData.get("mcversion");
		if (this.targetVersion == null)
		{
			ModFile.logger.warning("Mod in " + this.getAbsolutePath() + " has no loader version number reading litemod.json");
			return;
		}
		
		try
		{
			this.revision = Float.parseFloat(this.metaData.get("revision"));
			this.hasRevision = true;
		}
		catch (NullPointerException ex) {}
		catch (Exception ex)
		{
			ModFile.logger.warning("Mod in " + this.getAbsolutePath() + " has an invalid revision number reading litemod.json");
		}

		this.valid = true;
		
		if (this.modName == null)
		{
			this.modName = this.getDefaultName();
		}
		
		this.tweakClassName = this.metaData.get("tweakClass");
		this.classTransformerClassName = this.metaData.get("classTransformerClass");
	}

	protected String getDefaultName()
	{
		return this.getName().replaceAll("[^a-zA-Z]", "");
	}
	
	public String getModName()
	{
		return this.modName;
	}
	
	public boolean isValid()
	{
		return this.valid;
	}
	
	public String getVersion()
	{
		return this.targetVersion;
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
	
	public boolean hasTweakClass()
	{
		return this.tweakClassName != null;
	}
	
	public String getTweakClassName()
	{
		return this.tweakClassName;
	}
	
	public boolean hasClassTransformer()
	{
		return this.classTransformerClassName != null;
	}
	
	public String getClassTransformerClassName()
	{
		return this.classTransformerClassName;
	}
	
	public boolean isInjected()
	{
		return this.injected;
	}
	
	public boolean injectIntoClassPath(LaunchClassLoader classLoader, boolean injectIntoParent) throws MalformedURLException
	{
		if (!this.injected)
		{
			if (injectIntoParent)
			{
				LiteLoaderTweaker.addURLToParentClassLoader(this.toURI().toURL());
			}
			
			classLoader.addURL(this.toURI().toURL());
			this.injected = true;
			return true;
		}
		
		return false;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getResourcePack()
	{
		return (T)this.resourcePack;
	}
	
	/**
	 * Initialise the mod resource pack
	 * 
	 * @param name
	 */
	public void initResourcePack(String name)
	{
		if (this.resourcePack == null)
		{
			ModFile.logger.info(String.format("Setting up \"%s\" as mod resource pack with identifier \"%s\"", this.getName(), name));
			this.resourcePack = new ModResourcePack(name, this);
		}
	}
	
	/**
	 * Registers this file as a minecraft resource pack 
	 * 
	 * @param name
	 * @return true if the pack was added
	 */
	public boolean hasResourcePack()
	{
		return (this.resourcePack != null);
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


	/**
	 * @param zip
	 * @param entry
	 * @return
	 * @throws IOException
	 */
	public static String zipEntryToString(ZipFile zip, ZipEntry entry) throws IOException
	{
		BufferedReader reader = null; 
		StringBuilder sb = new StringBuilder();
		
		try
		{
			InputStream stream = zip.getInputStream(entry);
			reader = new BufferedReader(new InputStreamReader(stream));

			String versionFileLine;
			while ((versionFileLine = reader.readLine()) != null)
				sb.append(versionFileLine);
		}
		finally
		{
			if (reader != null) reader.close();
		}
		
		return sb.toString();
	}
}
