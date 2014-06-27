package com.mumfrey.liteloader.server.api;

import net.minecraft.server.MinecraftServer;

import com.mumfrey.liteloader.common.GameEngine;
import com.mumfrey.liteloader.core.ClientPluginChannels;
import com.mumfrey.liteloader.core.Events;
import com.mumfrey.liteloader.core.LiteLoader;
import com.mumfrey.liteloader.core.ServerPluginChannels;
import com.mumfrey.liteloader.interfaces.PanelManager;
import com.mumfrey.liteloader.interfaces.ObjectFactory;
import com.mumfrey.liteloader.launch.LoaderEnvironment;
import com.mumfrey.liteloader.launch.LoaderProperties;
import com.mumfrey.liteloader.permissions.PermissionsManagerClient;
import com.mumfrey.liteloader.permissions.PermissionsManagerServer;
import com.mumfrey.liteloader.server.DummyClient;
import com.mumfrey.liteloader.server.GameEngineServer;
import com.mumfrey.liteloader.server.ServerEvents;

class ObjectFactoryServer implements ObjectFactory<DummyClient, MinecraftServer>
{
	private LoaderEnvironment environment;
	
	private LoaderProperties properties;
	
	private ServerEvents serverEvents;

	private GameEngineServer engine;
	
	private ServerPluginChannels serverPluginChannels;

	ObjectFactoryServer(LoaderEnvironment environment, LoaderProperties properties)
	{
		this.environment = environment;
		this.properties = properties;
	}
	
	@Override
	public Events<DummyClient, MinecraftServer> getEventBroker()
	{
		if (this.serverEvents == null)
		{
			this.serverEvents = new ServerEvents(LiteLoader.getInstance(), this.getGameEngine(), this.properties);
		}
		
		return this.serverEvents;
	}
	
	@Override
	public GameEngine<DummyClient, MinecraftServer> getGameEngine()
	{
		if (this.engine == null)
		{
			this.engine = new GameEngineServer(this.environment);
		}	
		
		return this.engine;
	}
	
	@Override
	public PanelManager<Object> getModPanelManager()
	{
		return null;
	}
	
	@Override
	public ClientPluginChannels getClientPluginChannels()
	{
		return null;
	}
	
	@Override
	public ServerPluginChannels getServerPluginChannels()
	{
		if (this.serverPluginChannels == null)
		{
			this.serverPluginChannels = new ServerPluginChannels();
		}	

		return this.serverPluginChannels;
	}
	
	@Override
	public PermissionsManagerClient getClientPermissionManager()
	{
		return null;
	}
	
	@Override
	public PermissionsManagerServer getServerPermissionManager()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void preBeginGame()
	{
	}
}
