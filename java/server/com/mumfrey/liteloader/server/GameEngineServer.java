package com.mumfrey.liteloader.server;

import java.util.List;

import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;

import com.mumfrey.liteloader.common.GameEngine;
import com.mumfrey.liteloader.launch.LoaderEnvironment;
import com.mumfrey.liteloader.server.resources.ServerResourceManager;

/**
 *
 * @author Adam Mummery-Smith
 */
public class GameEngineServer implements GameEngine<DummyClient, MinecraftServer>
{
	private final LoaderEnvironment environment;
	
	/**
	 * 
	 */
	private final MinecraftServer engine = MinecraftServer.getServer();
	
	/**
	 * 
	 */
	private final DummyClient client = new DummyClient();

	private ServerResourceManager resourceManager;
	
	public GameEngineServer(LoaderEnvironment environment)
	{
		this.environment = environment;
	}

	@Override
	public Profiler getProfiler()
	{
		return this.engine.theProfiler;
	}

	@Override
	public void refreshResources(boolean force)
	{
		this.getResourceManager().refreshResources(force);
	}

	@Override
	public boolean isClient()
	{
		return false;
	}

	@Override
	public boolean isServer()
	{
		return true;
	}

	@Override
	public boolean isInGame()
	{
		return true;
	}
	
	@Override
	public boolean isRunning()
	{
		return this.engine.isServerRunning();
	}
	
	@Override
	public boolean isSinglePlayer()
	{
		return false;
	}

	@Override
	public DummyClient getClient()
	{
		return this.client;
	}

	@Override
	public MinecraftServer getServer()
	{
		return this.engine;
	}
	
	@Override
	public ServerResourceManager getResourceManager()
	{
		if (this.resourceManager == null)
		{
			this.resourceManager = new ServerResourceManager(this.environment);
		}
		
		return this.resourceManager;
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.common.GameEngine#registerResourcePack(net.minecraft.client.resources.IResourcePack)
	 */
	@Override
	public boolean registerResourcePack(IResourcePack resourcePack)
	{
		return this.getResourceManager().registerResourcePack(resourcePack);
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.common.GameEngine#unRegisterResourcePack(net.minecraft.client.resources.IResourcePack)
	 */
	@Override
	public boolean unRegisterResourcePack(IResourcePack resourcePack)
	{
		return false;
	}
	
	@Override
	public List<KeyBinding> getKeyBindings()
	{
		throw new RuntimeException("Minecraft Server does not support key bindings");
	}
	
	@Override
	public void setKeyBindings(List<KeyBinding> keyBindings)
	{
		throw new RuntimeException("Minecraft Server does not support key bindings");
	}
}
