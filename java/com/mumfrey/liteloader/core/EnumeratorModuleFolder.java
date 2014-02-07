package com.mumfrey.liteloader.core;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.minecraft.launchwrapper.LaunchClassLoader;

import com.mumfrey.liteloader.util.log.LiteLoaderLogger;

/**
 * Enumerator module which searches for mods and tweaks in a folder
 * 
 * @author Adam Mummery-Smith
 */
public class EnumeratorModuleFolder implements FilenameFilter, EnumeratorModule<File>
{
	private static final String OPTION_SEARCH_ZIPFILES  = "search.zipfiles";
	private static final String OPTION_SEARCH_JARFILES  = "search.jarfiles";

	/**
	 * Ordered sets used to sort mods by version/revision  
	 */
	private final Map<String, TreeSet<LoadableMod<File>>> versionOrderingSets = new HashMap<String, TreeSet<LoadableMod<File>>>();
	
	/**
	 * Mods to add once init is completed
	 */
	private final List<LoadableMod<File>> loadableMods = new ArrayList<LoadableMod<File>>();

	private File directory;

	private boolean readZipFiles;
	private boolean readJarFiles;
	private boolean loadTweaks;

	/**
	 * True if this is a general, unversioned folder and the enumerator should only add files which have valid version metadata
	 */
	private final boolean requireMetaData;

	public EnumeratorModuleFolder(File directory, boolean loadTweaks, boolean requireMetaData)
	{
		this.directory       = directory;
		this.loadTweaks      = loadTweaks;
		this.requireMetaData = requireMetaData;
	}
	
	@Override
	public void init(PluggableEnumerator enumerator)
	{
		this.readZipFiles = enumerator.getAndStoreBooleanProperty(OPTION_SEARCH_ZIPFILES,  false);
		this.readJarFiles = enumerator.getAndStoreBooleanProperty(OPTION_SEARCH_JARFILES,  true);
	}
	
	/**
	 * Write settings
	 */
	@Override
	public void writeSettings(PluggableEnumerator enumerator)
	{
		enumerator.setBooleanProperty(OPTION_SEARCH_ZIPFILES, this.readZipFiles);
		enumerator.setBooleanProperty(OPTION_SEARCH_JARFILES, this.readJarFiles);
	}
	
	@Override
	public String toString()
	{
		return this.directory.getAbsolutePath();
	}
	
	/**
	 * @return
	 */
	public File getDirectory()
	{
		return this.directory;
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.core.Enumerator#getLoadableMods()
	 */
	public List<LoadableMod<File>> getLoadableMods()
	{
		return this.loadableMods;
	}

	/**
	 * For FilenameFilter interface
	 * 
	 * @see java.io.FilenameFilter#accept(java.io.File, java.lang.String)
	 */
	@Override
	public boolean accept(File dir, String fileName)
	{
		fileName = fileName.toLowerCase();
		return                       fileName.endsWith(".litemod")
			|| (this.readZipFiles && fileName.endsWith(".zip"))
			|| (this.readJarFiles && fileName.endsWith(".jar"));
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.core.Enumerator#enumerate(com.mumfrey.liteloader.core.EnabledModsList, java.lang.String)
	 */
	@Override
	public void enumerate(PluggableEnumerator enumerator, EnabledModsList enabledModsList, String profile)
	{
		if (this.directory.exists() && this.directory.isDirectory())
		{
			LiteLoaderLogger.info("Discovering valid mod files in folder %s", this.directory.getPath());

			this.findValidFiles(enumerator, enabledModsList, profile);
			this.sortAndAllocateFiles(enumerator, enabledModsList, profile);
			this.versionOrderingSets.clear();
		}
	}
	
	/**
	 * @param enabledModsList
	 * @param profile
	 */
	private void findValidFiles(PluggableEnumerator enumerator, EnabledModsList enabledModsList, String profile)
	{
		for (File candidateFile : this.directory.listFiles(this))
		{
			ZipFile candidateZip = null;
			
			try
			{
				candidateZip = new ZipFile(candidateFile);
				ZipEntry versionEntry = candidateZip.getEntry(LoadableMod.METADATA_FILENAME);
				ZipEntry legacyVersionEntry = candidateZip.getEntry(LoadableMod.LEGACY_METADATA_FILENAME);

				// Check for a version file
				if (versionEntry != null)
				{
					String strVersion = null;
					try
					{
						strVersion = LoadableModFile.zipEntryToString(candidateZip, versionEntry);
					}
					catch (IOException ex)
					{
						LiteLoaderLogger.warning("Error reading version data from %s", candidateZip.getName());
					}
					
					if (strVersion != null)
					{
						this.addModFile(candidateFile, strVersion);
					}
				}
				else if (legacyVersionEntry != null)
				{
					LiteLoaderLogger.warning("%s is no longer supported, ignoring outdated mod file: %s", LoadableMod.LEGACY_METADATA_FILENAME, candidateFile.getAbsolutePath());
				}
				else if (!this.requireMetaData && this.loadTweaks && this.readJarFiles && candidateFile.getName().toLowerCase().endsWith(".jar"))
				{
					LoadableFile container = new LoadableFile(candidateFile);
					enumerator.registerTweakContainer(container);
				}
			}
			catch (Exception ex)
			{
				LiteLoaderLogger.info("Error enumerating '%s': Invalid zip file or error reading file", candidateFile.getAbsolutePath());
			}
			finally
			{
				if (candidateZip != null)
				{
					try
					{
						candidateZip.close();
					}
					catch (IOException ex) {}
				}
			}
		}
	}

	/**
	 * @param candidateFile
	 * @param strVersion
	 */
	private void addModFile(File candidateFile, String strVersion)
	{
		LoadableModFile modFile = new LoadableModFile(candidateFile, strVersion);
		
		if (modFile.hasValidMetaData())
		{
			// Only add the mod if the version matches, we add candidates to the versionOrderingSets in
			// order to determine the most recent version available.
			if (LiteLoaderVersion.CURRENT.isVersionSupported(modFile.getTargetVersion()))
			{
				if (!this.versionOrderingSets.containsKey(modFile.getName()))
				{
					this.versionOrderingSets.put(modFile.getModName(), new TreeSet<LoadableMod<File>>());
				}
				
				LiteLoaderLogger.info("Considering valid mod file: %s", modFile.getAbsolutePath());
				this.versionOrderingSets.get(modFile.getModName()).add(modFile);
			}
			else
			{
				LiteLoaderLogger.info("Not adding invalid or outdated mod file: %s", candidateFile.getAbsolutePath());
			}
		}
	}

	/**
	 * @param enumerator 
	 * @param enabledModsList 
	 * @param enabledModsList
	 * @param profile 
	 * @param profile
	 */
	@SuppressWarnings("unchecked")
	private void sortAndAllocateFiles(PluggableEnumerator enumerator, EnabledModsList enabledModsList, String profile)
	{
		// Copy the first entry in every version set into the modfiles list
		for (Entry<String, TreeSet<LoadableMod<File>>> modFileEntry : this.versionOrderingSets.entrySet())
		{
			LoadableMod<File> newestVersion = modFileEntry.getValue().iterator().next();

			if (enumerator.isContainerEnabled(newestVersion))
			{
				LiteLoaderLogger.info("Adding newest valid mod file '%s' at revision %.4f", newestVersion.getLocation(), newestVersion.getRevision());
				this.loadableMods.add(newestVersion);
			}
			else
			{
				LiteLoaderLogger.info("Not adding valid mod file '%s', the specified mod is disabled or missing a required dependency", newestVersion.getLocation());
			}
			
			if (this.loadTweaks)
			{
				try
				{
					if (newestVersion instanceof TweakContainer)
					{
						enumerator.registerTweakContainer((TweakContainer<File>)newestVersion);
					}
				}
				catch (Throwable th)
				{
					LiteLoaderLogger.warning("Error adding tweaks from '%s'", newestVersion.getLocation());
				}
			}
		}
	}
	
	@Override
	public void injectIntoClassLoader(PluggableEnumerator enumerator, LaunchClassLoader classLoader, EnabledModsList enabledModsList, String profile)
	{
		LiteLoaderLogger.info("Injecting external mods into class path...");
		
		for (LoadableMod<?> loadableMod : this.loadableMods)
		{
			try
			{
				if (loadableMod.injectIntoClassPath(classLoader, false))
				{
					LiteLoaderLogger.info("Successfully injected mod file '%s' into classpath", loadableMod.getLocation());
				}
			}
			catch (MalformedURLException ex)
			{
				LiteLoaderLogger.warning("Error injecting '%s' into classPath. The mod will not be loaded", loadableMod.getLocation());
			}
		}
	}
	
	@Override
	public void registerMods(PluggableEnumerator enumerator, LaunchClassLoader classLoader)
	{
		LiteLoaderLogger.info("Discovering mods in valid mod files...");

		for (LoadableMod<?> modFile : this.loadableMods)
		{
			LiteLoaderLogger.info("Searching %s...", modFile.getLocation());
			enumerator.registerModsFrom(modFile, true);
		}
	}
}
