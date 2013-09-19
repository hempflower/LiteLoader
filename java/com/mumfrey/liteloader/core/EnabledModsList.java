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

public class EnabledModsList
{
	private static transient Gson gson = new GsonBuilder().setPrettyPrinting().create();
	
	private TreeMap<String, TreeMap<String, Boolean>> mods;
	
	private transient Boolean defaultEnabledValue = Boolean.TRUE;
	
	private transient boolean allowSave = true;
	
	private EnabledModsList()
	{
	}
	
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

	public void setEnabled(String profileName, String name, boolean enabled)
	{
		Map<String, Boolean> profile = this.getProfile(profileName);
		profile.put(name.toLowerCase().trim(), Boolean.valueOf(enabled));

		this.allowSave = true;
	}

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

	private Map<String, Boolean> getProfile(String profileName)
	{
		if (this.mods == null) this.mods = new TreeMap<String, TreeMap<String,Boolean>>();
		
		if (!this.mods.containsKey(profileName))
		{
			this.mods.put(profileName, new TreeMap<String, Boolean>());
		}
		
		return this.mods.get(profileName);
	}
	
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
