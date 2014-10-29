package com.mumfrey.liteloader.core.api;

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

import com.mumfrey.liteloader.api.EnumeratorModule;
import com.mumfrey.liteloader.common.LoadingProgress;
import com.mumfrey.liteloader.core.LiteLoaderVersion;
import com.mumfrey.liteloader.interfaces.LoadableFile;
import com.mumfrey.liteloader.interfaces.LoadableMod;
import com.mumfrey.liteloader.interfaces.ModularEnumerator;
import com.mumfrey.liteloader.interfaces.TweakContainer;
import com.mumfrey.liteloader.launch.LoaderEnvironment;
import com.mumfrey.liteloader.launch.LoaderProperties;
import com.mumfrey.liteloader.util.log.LiteLoaderLogger;

/**
 * Enumerator module which searches for mods and tweaks in a folder
 * 
 * @author Adam Mummery-Smith
 */
public class EnumeratorModuleFolder implements FilenameFilter, EnumeratorModule
{
	/**
	 * Ordered sets used to sort mods by version/revision  
	 */
	protected final Map<String, TreeSet<LoadableMod<File>>> versionOrderingSets = new HashMap<String, TreeSet<LoadableMod<File>>>();
	
	/**
	 * Mods to add once init is completed
	 */
	protected final List<LoadableMod<File>> loadableMods = new ArrayList<LoadableMod<File>>();

	protected LiteLoaderCoreAPI coreAPI;
	
	protected File directory;

	protected boolean readZipFiles;
	protected boolean readJarFiles;
	protected boolean loadTweaks;

	/**
	 * True if this is a general, unversioned folder and the enumerator should only add files which have valid version metadata
	 */
	private final boolean requireMetaData;

	public EnumeratorModuleFolder(LiteLoaderCoreAPI coreAPI, File directory, boolean requireMetaData)
	{
		this.coreAPI         = coreAPI;
		this.directory       = directory;
		this.requireMetaData = requireMetaData;
	}
	
	@Override
	public void init(LoaderEnvironment environment, LoaderProperties properties)
	{
		this.loadTweaks = properties.loadTweaksEnabled();
		this.readZipFiles = properties.getAndStoreBooleanProperty(LoaderProperties.OPTION_SEARCH_ZIPFILES, false);
		this.readJarFiles = properties.getAndStoreBooleanProperty(LoaderProperties.OPTION_SEARCH_JARFILES, true);
		
		this.coreAPI.writeDiscoverySettings();
	}
	
	/**
	 * Write settings
	 */
	@Override
	public void writeSettings(LoaderEnvironment environment, LoaderProperties properties)
	{
		properties.setBooleanProperty(LoaderProperties.OPTION_SEARCH_ZIPFILES, this.readZipFiles);
		properties.setBooleanProperty(LoaderProperties.OPTION_SEARCH_JARFILES, this.readJarFiles);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return this.directory.getAbsolutePath();
	}
	
	/**
	 * Get the directory this module will inspect
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
		
		if (fileName.endsWith(".litemod.zip") && !this.readZipFiles)
		{
			LiteLoaderLogger.warning("Found %s with unsupported extension .litemod.zip. Please change file extension to .litemod to allow this file to be loaded!", fileName);
		}
		
		return                       fileName.endsWith(".litemod")
			|| (this.readZipFiles && fileName.endsWith(".zip"))
			|| (this.readJarFiles && fileName.endsWith(".jar"));
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.core.Enumerator#enumerate(com.mumfrey.liteloader.core.EnabledModsList, java.lang.String)
	 */
	@Override
	public void enumerate(ModularEnumerator enumerator, String profile)
	{
		if (this.directory.exists() && this.directory.isDirectory())
		{
			LiteLoaderLogger.info("Discovering valid mod files in folder %s", this.directory.getPath());

			this.findValidFiles(enumerator);
			this.sortAndAllocateFiles(enumerator);
			this.versionOrderingSets.clear();
		}
	}
	
	/**
	 */
	private void findValidFiles(ModularEnumerator enumerator)
	{
		for (File candidateFile : this.directory.listFiles(this.getFilenameFilter()))
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
	 * Get the {@link FilenameFilter} to use to filter candidate files
	 */
	protected FilenameFilter getFilenameFilter()
	{
		// Stub for subclasses
		return this;
	}

	/**
	 * @param modFile
	 */
	protected boolean isFileSupported(LoadableModFile modFile)
	{
		// Stub for subclasses
		return LiteLoaderVersion.CURRENT.isVersionSupported(modFile.getTargetVersion());
	}

	/**
	 * @param candidateFile
	 * @param strVersion
	 */
	protected void addModFile(File candidateFile, String strVersion)
	{
		LoadableModFile modFile = new LoadableModFile(candidateFile, strVersion);
		
		if (modFile.hasValidMetaData())
		{
			// Only add the mod if the version matches, we add candidates to the versionOrderingSets in
			// order to determine the most recent version available.
			if (this.isFileSupported(modFile))
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
	 */
	@SuppressWarnings("unchecked")
	protected void sortAndAllocateFiles(ModularEnumerator enumerator)
	{
		// Copy the first entry in every version set into the modfiles list
		for (Entry<String, TreeSet<LoadableMod<File>>> modFileEntry : this.versionOrderingSets.entrySet())
		{
			LoadableMod<File> newestVersion = modFileEntry.getValue().iterator().next();

			if (enumerator.registerModContainer(newestVersion))
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
	public void injectIntoClassLoader(ModularEnumerator enumerator, LaunchClassLoader classLoader)
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
	public void registerMods(ModularEnumerator enumerator, LaunchClassLoader classLoader)
	{
		LiteLoaderLogger.info("Discovering mods in valid mod files...");
		LoadingProgress.incTotalLiteLoaderProgress(this.loadableMods.size());

		for (LoadableMod<?> modFile : this.loadableMods)
		{
			LoadingProgress.incLiteLoaderProgress("Searching for mods in " + modFile.getModName() + "...");
			LiteLoaderLogger.info("Searching %s...", modFile.getLocation());
			try
			{
				enumerator.registerModsFrom(modFile, true);
			}
			catch (Exception ex)
			{
				LiteLoaderLogger.warning("Error encountered whilst searching in %s...", modFile.getLocation());
			}
		}
	}
}
