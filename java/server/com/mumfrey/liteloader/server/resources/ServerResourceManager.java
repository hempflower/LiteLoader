package com.mumfrey.liteloader.server.resources;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mumfrey.liteloader.common.LoadingProgress;
import com.mumfrey.liteloader.launch.LoaderEnvironment;

import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.util.ResourceLocation;

public class ServerResourceManager implements IResourceManager
{
	private final LoaderEnvironment environment;

	/**
	 * Registered resource packs 
	 */
	private final Map<String, IResourcePack> registeredResourcePacks = new HashMap<String, IResourcePack>();

	/**
	 * True while initialising mods if we need to do a resource manager reload once the process is completed
	 */
	private boolean pendingResourceReload;

	public ServerResourceManager(LoaderEnvironment environment)
	{
		this.environment = environment;
	}

	@Override
	public Set<String> getResourceDomains()
	{
		return null;
	}
	
	@Override
	public IResource getResource(ResourceLocation var1) throws IOException
	{
		return null;
	}
	
	@Override
	public List<IResource> getAllResources(ResourceLocation var1) throws IOException
	{
		return null;
	}
	
	public void refreshResources(boolean force)
	{
		if (this.pendingResourceReload || force)
		{
			LoadingProgress.setMessage("Reloading Resources...");
			this.pendingResourceReload = false;
//			this.engine.refreshResources();
		}
	}
	
	public boolean registerResourcePack(IResourcePack resourcePack)
	{
		if (!this.registeredResourcePacks.containsKey(resourcePack.getPackName()))
		{
			this.pendingResourceReload = true;
			this.registeredResourcePacks.put(resourcePack.getPackName(), resourcePack);
			return true;
		}
		
		return false;
	}
}
