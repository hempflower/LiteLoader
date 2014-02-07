package com.mumfrey.liteloader.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import joptsimple.internal.Strings;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.mumfrey.liteloader.launch.InjectionStrategy;
import com.mumfrey.liteloader.resources.ModResourcePack;
import com.mumfrey.liteloader.util.log.LiteLoaderLogger;

/**
 * Wrapper for file which represents a mod file to load with associated version information and
 * metadata. Retrieve this from litemod.json at enumeration time. We also override comparable to 
 * provide our own custom sorting logic based on version info.
 *
 * @author Adam Mummery-Smith
 */
public class LoadableModFile extends LoadableFile implements LoadableMod<File>
{
	private static final long serialVersionUID = -7952147161905688459L;

	/**
	 * Maximum recursion depth for mod discovery
	 */
	private static final int MAX_DISCOVERY_DEPTH = 16;
	
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
	 * Name of the class transof
	 */
	protected List<String> classTransformerClassNames = new ArrayList<String>();
	
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
	 * Dependencies declared in the metadata
	 */
	private Set<String> dependencies = new HashSet<String>();

	/**
	 * Dependencies which are missing 
	 */
	private Set<String> missingDependencies = new HashSet<String>();;

	/**
	 * Classes in this container 
	 */
	protected List<String> classNames = null;
	
	/**
	 * @param file
	 * @param strVersion
	 */
	LoadableModFile(File file, String strVersion)
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
			this.metaData = LoadableModFile.gson.fromJson(strVersionData, HashMap.class);
		}
		catch (JsonSyntaxException jsx)
		{
			LiteLoaderLogger.warning("Error reading %s in %s, JSON syntax exception: %s", LoadableMod.METADATA_FILENAME, this.getAbsolutePath(), jsx.getMessage());
			return;
		}
		
		this.modName = this.getMetaValue("name", this.getDefaultName());
		this.version = this.getMetaValue("version", "Unknown");
		this.author = this.getMetaValue("author", "Unknown");
		this.targetVersion = this.metaData.get("mcversion");
		if (this.targetVersion == null)
		{
			LiteLoaderLogger.warning("Mod in %s has no loader version number reading %s" + this.getAbsolutePath(), LoadableMod.METADATA_FILENAME);
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
			LiteLoaderLogger.warning("Mod in %s has an invalid revision number reading %s", this.getAbsolutePath(), LoadableMod.METADATA_FILENAME);
		}

		this.valid = true;
		
		this.tweakClassName = this.metaData.get("tweakClass");
		this.tweakPriority = 0;
		
		for (String name : this.getMetaValues("classTransformerClasses", ","))
		{
			if (!Strings.isNullOrEmpty(name))
				this.classTransformerClassNames.add(name);
		}
		
		this.injectionStrategy = InjectionStrategy.parseStrategy(this.metaData.get("injectAt"));
		
		for (String dependency : this.getMetaValues("dependsOn", ","))
		{
			this.dependencies.add(dependency);
		}
	}

	protected String getDefaultName()
	{
		return this.getName().replaceAll("[^a-zA-Z]", "");
	}
	
	@Override
	public String getModName()
	{
		return this.modName;
	}
	
	@Override
	public String getIdentifier()
	{
		return this.modName.toLowerCase();
	}
	
	@Override
	public String getDisplayName()
	{
		return this.getName();
	}
	
	@Override
	public String getDescription(String key)
	{
		String descriptionKey = "description";
		if (key != null && key.length() > 0)
			descriptionKey += "." + key.toLowerCase();
		
		return this.getMetaValue(descriptionKey, this.getMetaValue("description", ""));
	}
	
	@Override
	public boolean isExternalJar()
	{
		return false;
	}
	
	@Override
	public boolean isToggleable()
	{
		return true;
	}
	
	@Override
	public boolean hasValidMetaData()
	{
		return this.valid;
	}
	
	@Override
	public String getTargetVersion()
	{
		return this.targetVersion;
	}
	
	@Override
	public float getRevision()
	{
		return this.revision;
	}
	
	@Override
	public String getMetaValue(String metaKey, String defaultValue)
	{
		return this.metaData.containsKey(metaKey) ? this.metaData.get(metaKey) : defaultValue;
	}
	
	public String[] getMetaValues(String metaKey, String separator)
	{
		return this.metaData.containsKey(metaKey) ? this.metaData.get(metaKey).split(separator) : new String[0];
	}

	@Override
	public Set<String> getMetaDataKeys()
	{
		return Collections.unmodifiableSet(this.metaData.keySet());
	}
	
	@Override
	public boolean hasClassTransformers()
	{
		return this.classTransformerClassNames.size() > 0;
	}
	
	@Override
	public List<String> getClassTransformerClassNames()
	{
		return this.classTransformerClassNames;
	}

	@Override
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
	@Override
	public void initResourcePack(String name)
	{
		if (this.resourcePack == null)
		{
			LiteLoaderLogger.info("Setting up \"%s\" as mod resource pack with identifier \"%s\"", this.getName(), name);
			this.resourcePack = new ModResourcePack(name, this);
		}
	}
	
	/**
	 * Registers this file as a minecraft resource pack 
	 * 
	 * @param name
	 * @return true if the pack was added
	 */
	@Override
	public boolean hasResourcePack()
	{
		return (this.resourcePack != null);
	}
	
	@Override
	public boolean hasDependencies()
	{
		return this.dependencies.size() > 0;
	}
	
	@Override
	public Set<String> getDependencies()
	{
		return this.dependencies;
	}
	
	@Override
	public void registerMissingDependency(String dependency)
	{
		this.missingDependencies.add(dependency);
	}
	
	@Override
	public Set<String> getMissingDependencies()
	{
		return this.missingDependencies;
	}

	@Override
	public List<String> getContainedClassNames()
	{
		if (this.classNames == null)
		{
			this.classNames = this.enumerateClassNames();
		}
		
		return this.classNames;
	}
	
	protected List<String> enumerateClassNames()
	{
		if (this.isDirectory())
		{
			return LoadableModFile.enumerateDirectory(new ArrayList<String>(), this, "", 0);
		}

		return LoadableModFile.enumerateZipFile(this);
	}
	
	@Override
	public void addContainedMod(String modName)
	{
	}

	@Override
	public int compareTo(File other)
	{
		if (other == null || !(other instanceof LoadableModFile)) return -1;
		
		LoadableModFile otherMod = (LoadableModFile)other;
		
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
	 * @return
	 */
	protected static List<String> enumerateZipFile(File file)
	{
		List<String> classes = new ArrayList<String>();
		
		ZipFile zipFile;
		try
		{
			zipFile = new ZipFile(file);
		}
		catch (IOException ex)
		{
			return classes;
		}
		
		@SuppressWarnings("unchecked")
		Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>)zipFile.entries();
		while (entries.hasMoreElements())
		{
			ZipEntry entry = entries.nextElement();
			String entryName = entry.getName();
			if (entry.getSize() > 0 && entryName.endsWith(".class"))
			{
				classes.add(entryName.substring(0, entryName.length() - 6).replace('/', '.'));
			}
		}
		
		try
		{
			zipFile.close();
		}
		catch (IOException ex) {}
		
		return classes;
	}
	
	/**
	 * Recursive function to enumerate classes inside a classpath folder
	 * 
	 * @param classes
	 * @param packagePath
	 * @param packageName
	 */
	protected static List<String> enumerateDirectory(List<String> classes, File packagePath, String packageName, int depth)
	{
		// Prevent crash due to broken recursion
		if (depth > MAX_DISCOVERY_DEPTH)
			return classes;
		
		File[] classFiles = packagePath.listFiles();
		
		for (File classFile : classFiles)
		{
			if (classFile.isDirectory())
			{
				LoadableModFile.enumerateDirectory(classes, classFile, packageName + classFile.getName() + ".", depth + 1);
			}
			else
			{
				if (classFile.getName().endsWith(".class"))
				{
					String classFileName = classFile.getName();
					classes.add(packageName + classFileName.substring(0, classFileName.length() - 6));
				}
			}
		}
		
		return classes;
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
