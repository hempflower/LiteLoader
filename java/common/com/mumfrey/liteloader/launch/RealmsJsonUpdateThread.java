package com.mumfrey.liteloader.launch;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mumfrey.liteloader.util.log.LiteLoaderLogger;
import com.mumfrey.liteloader.util.net.HttpStringRetriever;

/**
 * Attempt to keep the version JSON we were launched from in sync with the latest lib version for this version of MC
 *
 * @author Adam Mummery-Smith
 */
public class RealmsJsonUpdateThread extends Thread
{
	private static final String DOWNLOAD_BASE_URL = "http://s3.amazonaws.com/Minecraft.Download/";

	private static final String REALMS_LIB_PATH = "com.mojang:realms";

	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	
	private final File versionsDir;
	
	private final String version;
	
	private final URI uri;

	public RealmsJsonUpdateThread(File versionsDir, String version, String parentVersion)
	{
		this.versionsDir = versionsDir;
		this.version = version;
		this.uri = URI.create(String.format("%sversions/%2$s/%2$s.json", RealmsJsonUpdateThread.DOWNLOAD_BASE_URL, parentVersion));
		
		this.setName("Realms JSON update thread");
		this.setDaemon(true);
	}
	
	@Override
	public void run()
	{
		if (this.isLocalJsonUpToDate())
		{
			LiteLoaderLogger.info("Realms JSON thread is skipping update check, version JSON file was recently modified");
			return;
		}
		
		Map<String, ?> localJson = this.getLocalVersionJson();
		if (localJson == null)
		{
			LiteLoaderLogger.info("Realms JSON thread failed to locate the local version JSON for version %s. The realms library reference can not be updated", this.version);
			return;
		}
		
		Map<String, ?> remoteJson = this.getRemoteVersionJson();
		if (remoteJson == null)
		{
			LiteLoaderLogger.info("Realms JSON thread failed to fetch the vanilla JSON. The realms library reference will not be updated");
			return;
		}
			
		String realmsVersionRemote = this.getLibraryVersion(remoteJson, RealmsJsonUpdateThread.REALMS_LIB_PATH);
		if (realmsVersionRemote == null)
		{
			LiteLoaderLogger.info("Realms JSON thread failed to parse remote version JSON. The realms library reference can not be updated");
			return;
		}
		
		String realmsVersionLocal = this.getLibraryVersion(localJson, RealmsJsonUpdateThread.REALMS_LIB_PATH);
		if (realmsVersionLocal == null)
		{
			LiteLoaderLogger.info("Realms JSON thread failed to parse local version JSON. The realms library reference can not be updated");
			return;
		}
		
		if (realmsVersionRemote.equals(realmsVersionLocal))
		{
			LiteLoaderLogger.info("Realms library reference is up-to-date. Current version is %s", realmsVersionRemote);
			return;
		}

		LiteLoaderLogger.info("Realms library reference is out of date. Current version %s, found remote version %s. Attempting to update version JSON.", realmsVersionLocal, realmsVersionRemote);
		if (!this.setLibraryVersion(localJson, RealmsJsonUpdateThread.REALMS_LIB_PATH, realmsVersionRemote))
		{
			LiteLoaderLogger.warning("Could not update the realms library version in the version JSON");
			return;
		}
		
		if (!this.writeLocalVersionJson(localJson))
		{
			LiteLoaderLogger.warning("Could not write the updated version JSON");
			return;
		}

		LiteLoaderLogger.info("Realms library reference was successfully updated. Current version is now %s", realmsVersionRemote);
	}

	private boolean writeLocalVersionJson(Map<String, ?> localJson)
	{
		File versionDir = new File(this.versionsDir, this.version);
		if (!versionDir.exists() || !versionDir.isDirectory()) return false;
		
		File versionJsonFile = new File(versionDir, this.version + ".json");
		
		FileWriter writer = null;
				
		try
		{
			writer = new FileWriter(versionJsonFile);
			this.gson.toJson(localJson, writer);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		finally
		{
			IOUtils.closeQuietly(writer);
		}
		
		return true;
	}

	@SuppressWarnings("unchecked")
	private String getLibraryVersion(Map<String, ?> json, String libraryName)
	{
		for (Map<String, String> library : (List<Map<String, String>>)json.get("libraries"))
		{
			try
			{
				for (Entry<String, ?> entry : library.entrySet())
				{
					String keyName = entry.getKey();
					if ("name".equals(keyName))
					{
						String libName = entry.getValue().toString();
						if (libName.startsWith(libraryName))
						{
							return libName.substring(libraryName.length() + 1);
						}
					}
				}
			}
			catch (Exception ex) {}
		}
		
		return null;
	}

	@SuppressWarnings("unchecked")
	private boolean setLibraryVersion(Map<String, ?> json, String libraryName, String newVersion)
	{
		int libraryIndex = this.getLibraryIndex(json, libraryName);
		if (libraryIndex > -1)
		{
			List<Map<String, Object>> libraries = (List<Map<String, Object>>)json.get("libraries");
			Map<String, Object> library = libraries.get(libraryIndex);
			library.put("name", String.format("%s:%s", libraryName, newVersion));
			return true;
		}
		
		return false;
	}

	/**
	 * @param json
	 * @param libraryName
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private int getLibraryIndex(Map<String, ?> json, String libraryName)
	{
		List<Map<String, ?>> libraries = (List<Map<String, ?>>)json.get("libraries");
		for (int index = 0; index < libraries.size(); index++)
		{
			try
			{
				Map<String, ?> library = libraries.get(index);
				for (Entry<String, ?> entry : library.entrySet())
				{
					String keyName = entry.getKey();
					if ("name".equals(keyName))
					{
						String libName = entry.getValue().toString();
						if (libName.startsWith(libraryName))
						{
							return index;
						}
					}
				}
			}
			catch (Exception ex) {}
		}
		
		return -1;
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, ?> getRemoteVersionJson()
	{
		try
		{
			HttpStringRetriever http = new HttpStringRetriever();
			String json = http.fetch(this.uri.toURL());
			if (!Strings.isNullOrEmpty(json))
			{
				return this.gson.fromJson(json, Map.class);
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		return null;
	}

	@SuppressWarnings("unchecked")
	private Map<String, ?> getLocalVersionJson()
	{
		File versionDir = new File(this.versionsDir, this.version);
		if (!versionDir.exists() || !versionDir.isDirectory()) return null;
		
		File versionJsonFile = new File(versionDir, this.version + ".json");
		if (!versionJsonFile.exists()) return null;

		FileReader reader = null; 
		
		try
		{
			reader = new FileReader(versionJsonFile);
			return this.gson.fromJson(reader, Map.class);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		finally
		{
			IOUtils.closeQuietly(reader);
		}
		
		return null;
	}

	private boolean isLocalJsonUpToDate()
	{
		File versionDir = new File(this.versionsDir, this.version);
		if (!versionDir.exists() || !versionDir.isDirectory()) return false;
		
		File versionJsonFile = new File(versionDir, this.version + ".json");
		if (!versionJsonFile.exists()) return false;
		
		long deltaTime = System.currentTimeMillis() - versionJsonFile.lastModified();
		return deltaTime < 86400000;
	}
}
