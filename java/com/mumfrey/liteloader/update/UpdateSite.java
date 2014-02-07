package com.mumfrey.liteloader.update;

import java.text.DateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.mumfrey.liteloader.util.log.LiteLoaderLogger;
import com.mumfrey.liteloader.util.net.HttpStringRetriever;

/**
 * An update site, used by liteloader to check for new versions but is also available to mods who may
 * want to use a similar system
 * 
 * @author Adam Mummery-Smith
 */
public class UpdateSite implements Comparator<Long>
{
	/**
	 * Gson instance for deserializing remote version data
	 */
	private static Gson gson = new Gson();

	/**
	 * Base URL of the remote update site
	 */
	private final String updateSiteUrl;
	
	/**
	 * Name of the json file containing the remote versions data, eg. versions.json
	 */
	private final String updateSiteJsonFileName;
	
	/**
	 * The version of minecraft being targetted
	 */
	private final String targetVersion;
	
	/**
	 * Artefact name in the form "com.somedomain.pkg:artefactname"
	 */
	private final String artefact;
	
	/**
	 * Local artefact timestamp
	 */
	private final long currentTimeStamp;
	
	/**
	 * Comparator for the timestamps 
	 */
	private final Comparator<Long> timeStampComparator;
	
	/**
	 * Threading lock object 
	 */
	private final Object lock = new Object();
	
	/**
	 * StringRetriever which will fetch the remote json file, null when not performing a fetch operation
	 */
	private HttpStringRetriever stringRetriever;
	
	/**
	 * True if the check is complete (even if it failed)
	 */
	private volatile boolean checkComplete = false;
	
	/**
	 * True if the check succeeded
	 */
	private volatile boolean checkSuccess = false;
	
	/**
	 * True if an updated version is available
	 */
	private volatile boolean updateAvailable = false;
	
	/**
	 * The version which is available
	 */
	private String availableVersion = null;
	
	/**
	 * The version which is available
	 */
	private String availableVersionDate = null;
	
	/**
	 * The URL to the available artefact
	 */
	private String availableVersionURL = null;
	
	/**
	 * Create a new UpdateSite with the specified information
	 * 
	 * @param updateSiteUrl Base URL of the update site, should include the trailing slash
	 * @param jsonFileName Name of the json file on the remote site containing the version data, eg. versions.json
	 * @param targetVersion The target minecraft version
	 * @param artefact Artefact name in the form "com.somedomain.pkg:artefactname"
	 * @param currentTimeStamp Timestamp of the current artefact
	 * @param timeStampComparator Comparator to use for comparing timestamps, if null uses built in comparator
	 */
	public UpdateSite(String updateSiteUrl, String jsonFileName, String targetVersion, String artefact, long currentTimeStamp, Comparator<Long> timeStampComparator)
	{
		this.updateSiteUrl = updateSiteUrl + (updateSiteUrl.endsWith("/") ? "" : "/");
		this.updateSiteJsonFileName = jsonFileName;
		
		this.targetVersion = targetVersion;
		this.artefact = artefact;
		this.currentTimeStamp = currentTimeStamp;
		
		this.timeStampComparator = timeStampComparator != null ? timeStampComparator : this;
	}
	
	/**
	 * Create a new UpdateSite with the specified information
	 * 
	 * @param updateSiteUrl Base URL of the update site, should include the trailing slash
	 * @param jsonFileName Name of the json file on the remote site containing the version data, eg. versions.json
	 * @param targetVersion The target minecraft version
	 * @param artefact Artefact name in the form "com.somedomain.pkg:artefactname"
	 * @param currentTimeStamp Timestamp of the current artefact
	 */
	public UpdateSite(String updateSiteUrl, String jsonFileName, String targetVersion, String artefact, long currentTimeStamp)
	{
		this(updateSiteUrl, jsonFileName, targetVersion, artefact, currentTimeStamp, null);
	}
	
	/**
	 * If an update check is not already in progress, starts an update check
	 */
	public void beginUpdateCheck()
	{
		synchronized (this.lock)
		{
			if (this.stringRetriever == null)
			{
				this.stringRetriever = new HttpStringRetriever(String.format("%s%s", this.updateSiteUrl, this.updateSiteJsonFileName));
				this.stringRetriever.start();
			}
		}
	}
	
	/**
	 * Gets whether a check is in progress
	 */
	public boolean isCheckInProgress()
	{
		this.update();
		boolean checkInProgress = false;
		
		synchronized (this.lock)
		{
			checkInProgress = this.stringRetriever != null;
		}
		
		return checkInProgress;
	}
	
	/**
	 * Gets whether a check has been completed
	 */
	public boolean isCheckComplete()
	{
		this.update();
		return this.checkComplete;
	}
	
	/**
	 * Gets whether the last check was a success
	 */
	public boolean isCheckSucceess()
	{
		this.update();
		return this.checkComplete && this.checkSuccess;
	}
	
	/**
	 * Gets whether an update is available at the remote site
	 */
	public boolean isUpdateAvailable()
	{
		this.update();
		return this.updateAvailable;
	}
	
	/**
	 * Gets the latest version available at the remote site
	 */
	public String getAvailableVersion()
	{
		this.update();
		return this.availableVersion;
	}
	
	/**
	 * Gets the latest version available at the remote site
	 */
	public String getAvailableVersionDate()
	{
		this.update();
		return this.availableVersionDate;
	}
	
	/**
	 * Gets the URL to the available version
	 */
	public String getAvailableVersionURL()
	{
		this.update();
		return this.availableVersionURL;
	}
	
	/**
	 * Check whether an in-progress check has completed and if it has parse the retrieved data
	 */
	private void update()
	{
		synchronized (this.lock)
		{
			if (this.stringRetriever != null && this.stringRetriever.isDone())
			{
				this.checkComplete = true;
				this.checkSuccess = this.stringRetriever.getSuccess();
				
				if (this.checkSuccess)
				{
					try
					{
						this.parseData(this.stringRetriever.getString());
					}
					catch (Exception ex)
					{
						this.checkSuccess = false;
						ex.printStackTrace();
					}
				}
				
				this.stringRetriever = null;
			}
		}
	}
	
	/**
	 * Parse data receieved from a query
	 * 
	 * @param json
	 */
	private void parseData(String json)
	{
		this.updateAvailable = false;
		
		try
		{
			@SuppressWarnings("serial")
			Map<String, Map<String, Map<String, Map<String, Map<String, Map<String, String>>>>>> data = UpdateSite.gson.fromJson(json, new TypeToken<Map<String, Map<String, Map<String, Map<String, Map<String, Map<String, String>>>>>>>(){}.getType());
			
			if (data.containsKey("versions"))
			{
				this.handleVersionsData(data.get("versions"));
			}
			else
			{
				LiteLoaderLogger.warning("No key 'versions' in update site JSON");
			}
		}
		catch (JsonSyntaxException ex)
		{
			LiteLoaderLogger.warning("Error parsing update site JSON: %s: %s", ex.getClass().getSimpleName(), ex.getMessage());
		}
	}

	/**
	 * @param versions
	 */
	private void handleVersionsData(Map<String, Map<String, Map<String, Map<String, Map<String, String>>>>> versions)
	{
		if (versions.containsKey(this.targetVersion))
		{
			for (Entry<String, Map<String, Map<String, Map<String, String>>>> versionDataEntry : versions.get(this.targetVersion).entrySet())
			{
				this.handleVersionData(versionDataEntry.getKey(), versionDataEntry.getValue());
			}
		}
		else
		{
			LiteLoaderLogger.warning("No version entry for current version '%s' in update site JSON", this.targetVersion);
		}
	}

	/**
	 * @param key
	 * @param value
	 */
	private void handleVersionData(String key, Map<String, Map<String, Map<String, String>>> value)
	{
		if (key.equals("artefacts"))
		{
			if (value.containsKey(this.artefact))
			{
				this.handleArtefactData(value.get(this.artefact));
			}
			else
			{
				LiteLoaderLogger.warning("No artefacts entry for specified artefact '%s' in update site JSON", this.artefact);
			}
		}
	}

	/**
	 * @param availableArtefacts
	 */
	private void handleArtefactData(Map<String, Map<String, String>> availableArtefacts)
	{
		if (availableArtefacts.containsKey("latest"))
		{
			Map<String, String> latestArtefact = availableArtefacts.get("latest");
			this.checkAndUseRemoteArtefact(latestArtefact, this.currentTimeStamp, false);
		}
		else
		{
			LiteLoaderLogger.warning("No key 'latest' in update site JSON");
			
			long bestTimeStamp = this.currentTimeStamp;
			Map<String, String> bestRemoteArtefact = null; 
			
			for (Map<String, String> remoteArtefact : availableArtefacts.values())
			{
				if (this.checkAndUseRemoteArtefact(remoteArtefact, bestTimeStamp, true))
				{
					bestTimeStamp = Long.parseLong(remoteArtefact.get("timestamp"));
					bestRemoteArtefact = remoteArtefact;
				}
			}
			
			if (bestRemoteArtefact != null)
			{
				this.availableVersion = bestRemoteArtefact.get("version");
				this.availableVersionURL = this.createArtefactURL(bestRemoteArtefact.get("file"));
				this.updateAvailable = this.compareTimeStamps(this.currentTimeStamp, bestTimeStamp);
			}
		}
	}

	/**
	 * @param artefact
	 * @param bestTimeStamp
	 * @param checkOnly
	 * @return
	 */
	private boolean checkAndUseRemoteArtefact(Map<String, String> artefact, long bestTimeStamp, boolean checkOnly)
	{
		if (artefact.containsKey("file") && artefact.containsKey("version") && artefact.containsKey("timestamp"))
		{
			Long remoteTimeStamp = Long.parseLong(artefact.get("timestamp"));
			
			if (checkOnly)
			{
				return this.compareTimeStamps(bestTimeStamp, remoteTimeStamp);
			}
			
			this.availableVersion = artefact.get("version");
			this.availableVersionDate = DateFormat.getDateTimeInstance().format(new Date(remoteTimeStamp * 1000L));
			this.availableVersionURL = this.createArtefactURL(artefact.get("file"));
			this.updateAvailable = this.compareTimeStamps(bestTimeStamp, remoteTimeStamp);
			
			return true;
		}
		
		return false;
	}

	/**
	 * @param bestTimeStamp
	 * @param remoteTimeStamp
	 * @return
	 */
	private boolean compareTimeStamps(long bestTimeStamp, Long remoteTimeStamp)
	{
		return this.timeStampComparator.compare(bestTimeStamp, remoteTimeStamp) < 0;
	}

	/**
	 * @param file
	 * @return
	 */
	private String createArtefactURL(String file)
	{
		return String.format("%s%s/%s/%s", this.updateSiteUrl, this.artefact.replace('.', '/').replace(':', '/'), this.targetVersion, file);
	}

	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(Long localTimestamp, Long remoteTimestamp)
	{
		if (localTimestamp == null && remoteTimestamp == null) return 0;
		if (localTimestamp == null) return -1;
		if (remoteTimestamp == null) return 1;
		return (int)(localTimestamp.longValue() - remoteTimestamp.longValue());
	}
}
