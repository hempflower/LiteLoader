package com.mumfrey.liteloader.core;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Serialisable (via GSON) object which stores list of enabled/disabled mods for each profile
 *
 * @author Adam Mummery-Smith
 */
public class EnabledModsList
{
	@SuppressWarnings("unused")
	private static transient final long serialVersionUID = -6449451105617763769L;

	/**
	 * Gson object for serialisation/deserialisation
	 */
	private static transient Gson gson = new GsonBuilder().setPrettyPrinting().create();
	
	/**
	 * This is the node which gets serialised
	 */
	private TreeMap<String, TreeMap<String, Boolean>> mods;
	
	/**
	 * By default, when we discover a mod which is NOT in the list for the current profile, we will ENABLE the
	 * mod and add it to the list. However, when we receive a list of mods on the command line, we instead want
	 * to DISABLE any additional unlisted mods, we also don't want to save the mods list because the command line
	 * is supposed to be an override rather than a new mask. These two values provide this behaviour. 
	 */
	private transient Boolean defaultEnabledValue = Boolean.TRUE;
	private transient boolean allowSave = true;
	
	private EnabledModsList()
	{
		// Private because we are always instanced by the static createFrom() method below
	}
	
	/**
	 * Check whether a particular mod is enabled
	 * 
	 * @param profileName
	 * @param name
	 * @return
	 */
	public boolean isEnabled(String profileName, String name)
	{
		Map<String, Boolean> profile = this.getProfile(profileName);
		name = name.toLowerCase().trim();
		
		if (!profile.containsKey(name))
		{
			profile.put(name, this.defaultEnabledValue);
		}
		
		return profile.get(name);
	}

	/**
	 * Set the enablement state of a mod in the specified profile
	 * 
	 * @param profileName
	 * @param name
	 * @param enabled
	 */
	public void setEnabled(String profileName, String name, boolean enabled)
	{
		Map<String, Boolean> profile = this.getProfile(profileName);
		profile.put(name.toLowerCase().trim(), Boolean.valueOf(enabled));

		this.allowSave = true;
	}

	/**
	 * Reads the mods list passed in on the command line
	 * 
	 * @param profileName
	 * @param modNameFilter
	 */
	public void processModsList(String profileName, List<String> modNameFilter)
	{
		Map<String, Boolean> profile = this.getProfile(profileName);
		
		try
		{
			if (modNameFilter != null)
			{
				for (String modName : profile.keySet())
				{
					profile.put(modName, Boolean.FALSE);
				}

				this.defaultEnabledValue = Boolean.FALSE;
				this.allowSave = false;
				
				for (String filterEntry : modNameFilter)
				{
					profile.put(filterEntry.toLowerCase().trim(), Boolean.TRUE);
				}
			}
		}
		catch (Exception ex)
		{
			this.defaultEnabledValue = Boolean.TRUE;
			this.allowSave = true;
		}
	}

	/**
	 * Internal method which returns the map for the specified profile
	 * 
	 * @param profileName
	 * @return
	 */
	private Map<String, Boolean> getProfile(String profileName)
	{
		if (profileName == null) profileName = "default";
		if (this.mods == null) this.mods = new TreeMap<String, TreeMap<String,Boolean>>();
		
		if (!this.mods.containsKey(profileName))
		{
			this.mods.put(profileName, new TreeMap<String, Boolean>());
		}
		
		return this.mods.get(profileName);
	}
	
	/**
	 * Factory method which tries to deserialise the enablement list from the file or if failing creates
	 * and returns a new instance.
	 * 
	 * @param file
	 * @return
	 */
	public static EnabledModsList createFrom(File file)
	{
		if (file.exists())
		{
			FileReader reader = null;
			
			try
			{
				reader = new FileReader(file);
				EnabledModsList instance = gson.fromJson(reader, EnabledModsList.class);
				return instance;
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
			finally
			{
				try
				{
					if (reader != null)
						reader.close();
				}
				catch (IOException ex)
				{
					ex.printStackTrace();
				}
			}
		}
		
		return new EnabledModsList();
	}
	
	/**
	 * Save the enablement list to the specified file
	 * 
	 * @param file
	 */
	public void saveTo(File file)
	{
		if (!this.allowSave) return;
		
		FileWriter writer = null;
		
		try
		{
			writer = new FileWriter(file);
			gson.toJson(this, writer);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		finally
		{
			try
			{
				if (writer != null)
					writer.close();
			}
			catch (IOException ex)
			{
				ex.printStackTrace();
			}
		}
	}
}
