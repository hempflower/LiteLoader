package com.mumfrey.liteloader.core;

import java.util.List;

import com.mumfrey.liteloader.util.PrivateFields;

import net.minecraft.src.ResourceManagerReloadListener;
import net.minecraft.src.SimpleReloadableResourceManager;
import net.minecraft.src.SoundManager;

/**
 * Manager object which handles inhibiting the sound manager's reload notification at startup
 *
 * @author Adam Mummery-Smith
 */
public class SoundManagerReloadInhibitor
{
	/**
	 * Resource Manager
	 */
	private SimpleReloadableResourceManager resourceManager;
	
	/**
	 * Sound manager
	 */
	private SoundManager soundManager;
	
	/**
	 * True if inhibition is currently active
	 */
	private boolean inhibited;
	
	/**
	 * So that we can re-insert the sound manager at the same index, we store the index we remove it from
	 */
	private int storedIndex;
	
	public SoundManagerReloadInhibitor(SimpleReloadableResourceManager resourceManager, SoundManager soundManager)
	{
		this.resourceManager = resourceManager;
		this.soundManager = soundManager;
	}
	
	/**
	 * Inhibit the sound manager reload notification
	 * 
	 * @return true if inhibit was applied
	 */
	public boolean inhibit()
	{
		try
		{
			if (!this.inhibited)
			{
				List<ResourceManagerReloadListener> reloadListeners = PrivateFields.reloadListeners.get(this.resourceManager);
				if (reloadListeners != null)
				{
					this.storedIndex = reloadListeners.indexOf(this.soundManager);
					if (this.storedIndex > -1)
					{
						LiteLoader.getLogger().info("Inhibiting sound manager reload");
						reloadListeners.remove(this.soundManager);
						this.inhibited = true;
						return true;
					}
				}
			}
		}
		catch (Exception ex)
		{
			LiteLoader.getLogger().warning("Error inhibiting sound manager reload");
		}
		
		return false;
	}
	
	/**
	 * Remove the sound manager reload inhibit
	 * 
	 * @param reload True to reload the sound manager now
	 * @return true if the sound manager was successfully restored
	 */
	public boolean unInhibit(boolean reload)
	{
		try
		{
			if (this.inhibited)
			{
				List<ResourceManagerReloadListener> reloadListeners = PrivateFields.reloadListeners.get(this.resourceManager);
				if (reloadListeners != null)
				{
					if (this.storedIndex > -1)
					{
						reloadListeners.add(this.storedIndex, this.soundManager);
					}
					else
					{
						reloadListeners.add(this.soundManager);
					}

					LiteLoader.getLogger().info("Sound manager reload inhibit removed");
					
					if (reload)
					{
						LiteLoader.getLogger().info("Reloading sound manager");
						this.soundManager.onResourceManagerReload(this.resourceManager);
					}

					this.inhibited = false;
					return true;
				}
			}
		}
		catch (Exception ex)
		{
			LiteLoader.getLogger().warning("Error removing sound manager reload inhibit");
		}
		
		return false;
	}

	public boolean isInhibited()
	{
		return this.inhibited;
	}
}
