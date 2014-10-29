package com.mumfrey.liteloader.core;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.command.ICommandManager;
import net.minecraft.command.ServerCommandManager;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetworkManager;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;

import com.mojang.authlib.GameProfile;
import com.mumfrey.liteloader.LiteMod;
import com.mumfrey.liteloader.PluginChannelListener;
import com.mumfrey.liteloader.ServerCommandProvider;
import com.mumfrey.liteloader.ServerPlayerListener;
import com.mumfrey.liteloader.ServerPluginChannelListener;
import com.mumfrey.liteloader.api.InterfaceProvider;
import com.mumfrey.liteloader.api.Listener;
import com.mumfrey.liteloader.common.GameEngine;
import com.mumfrey.liteloader.common.LoadingProgress;
import com.mumfrey.liteloader.core.event.HandlerList;
import com.mumfrey.liteloader.interfaces.FastIterable;
import com.mumfrey.liteloader.launch.LoaderProperties;
import com.mumfrey.liteloader.util.log.LiteLoaderLogger;

/**
 * @author Adam Mummery-Smith
 *
 * @param <TClient> Type of the client runtime, "Minecraft" on client and null on the server
 * @param <TServer> Type of the server runtime, "IntegratedServer" on the client, "MinecraftServer" on the server 
 */
public abstract class LiteLoaderEventBroker<TClient, TServer extends MinecraftServer> implements InterfaceProvider, IResourceManagerReloadListener
{
	/**
	 * @author Adam Mummery-Smith
	 *
	 * @param <T>
	 */
	public static class ReturnValue<T>
	{
		private T value;
		private boolean isSet;
		
		public ReturnValue(T value)
		{
			this.value = value;
		}

		public ReturnValue()
		{
		}
		
		public boolean isSet()
		{
			return this.isSet;
		}
		
		public T get()
		{
			return this.value;
		}
		
		public void set(T value)
		{
			this.isSet = true;
			this.value = value;
		}
	}
	
	/**
	 * Reference to the loader instance
	 */
	protected final LiteLoader loader;
	
	/**
	 * Reference to the game
	 */
	protected final GameEngine<TClient, TServer> engine;
	
	/**
	 * Profiler 
	 */
	protected final Profiler profiler;

	protected LiteLoaderMods mods;
	
	/**
	 * List of mods which provide server commands
	 */
	private FastIterable<ServerCommandProvider> serverCommandProviders = new HandlerList<ServerCommandProvider>(ServerCommandProvider.class);
	
	/**
	 * List of mods which monitor server player events
	 */
	private FastIterable<ServerPlayerListener> serverPlayerListeners = new HandlerList<ServerPlayerListener>(ServerPlayerListener.class);
	
	/**
	 * ctor
	 * 
	 * @param loader
	 * @param engine
	 * @param properties
	 */
	public LiteLoaderEventBroker(LiteLoader loader, GameEngine<TClient, TServer> engine, LoaderProperties properties)
	{
		this.loader   = loader;
		this.engine   = engine;
		this.profiler = engine.getProfiler();
	}
	
	/**
	 * @param mods
	 */
	void setMods(LiteLoaderMods mods)
	{
		this.mods = mods;
	}

	/**
	 * 
	 */
	protected void onStartupComplete()
	{
		LoadingProgress.setMessage("Checking mods...");
		this.mods.onStartupComplete();
		
		LoadingProgress.setMessage("Initialising CoreProviders...");
		this.loader.onStartupComplete();
		
		LoadingProgress.setMessage("Starting Game...");
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.api.InterfaceProvider#getListenerBaseType()
	 */
	@Override
	public Class<? extends Listener> getListenerBaseType()
	{
		return LiteMod.class;
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.api.InterfaceProvider#registerInterfaces(com.mumfrey.liteloader.core.InterfaceRegistrationDelegate)
	 */
	@Override
	public void registerInterfaces(InterfaceRegistrationDelegate delegate)
	{
		delegate.registerInterface(ServerCommandProvider.class);
		delegate.registerInterface(ServerPlayerListener.class);
		delegate.registerInterface(CommonPluginChannelListener.class);
	}
	
	/**
	 * Add a listener to the relevant listener lists
	 * 
	 * @param listener
	 */
	public void addCommonPluginChannelListener(CommonPluginChannelListener listener)
	{
		if (!(listener instanceof PluginChannelListener) && !(listener instanceof ServerPluginChannelListener))
		{
			LiteLoaderLogger.warning("Interface error for mod '%1s'. Implementing CommonPluginChannelListener has no effect! Use PluginChannelListener or ServerPluginChannelListener instead", listener.getName());
		}
	}

	/**
	 * @param serverCommandProvider
	 */
	public void addServerCommandProvider(ServerCommandProvider serverCommandProvider)
	{
		this.serverCommandProviders.add(serverCommandProvider);
	}

	/**
	 * @param serverPlayerListener
	 */
	public void addServerPlayerListener(ServerPlayerListener serverPlayerListener)
	{
		this.serverPlayerListeners.add(serverPlayerListener);
	}
	
	@Override
	public void onResourceManagerReload(IResourceManager resourceManager)
	{
		LoadingProgress.setMessage("Reloading Resources...");
	}

	/**
	 * @param instance
	 * @param folderName
	 * @param worldName
	 * @param worldSettings
	 */
	public void onStartServer(MinecraftServer instance, String folderName, String worldName, WorldSettings worldSettings)
	{
		ICommandManager commandManager = instance.getCommandManager();
		
		if (commandManager instanceof ServerCommandManager)
		{
			ServerCommandManager serverCommandManager = (ServerCommandManager)commandManager;
			this.serverCommandProviders.all().provideCommands(serverCommandManager);
		}

		LiteLoader.getServerPluginChannels().onServerStartup();
	}

	/**
	 * @param scm
	 * @param player
	 * @param profile
	 */
	public void onSpawnPlayer(ServerConfigurationManager scm, EntityPlayerMP player, GameProfile profile)
	{
		this.serverPlayerListeners.all().onPlayerConnect(player, profile);
	}
	
	/**
	 * @param scm
	 * @param player
	 */
	public void onPlayerLogin(ServerConfigurationManager scm, EntityPlayerMP player)
	{
		LiteLoader.getServerPluginChannels().onPlayerJoined(player);
	}
	
	/**
	 * @param scm
	 * @param netManager
	 * @param player
	 */
	public void onInitializePlayerConnection(ServerConfigurationManager scm, NetworkManager netManager, EntityPlayerMP player)
	{
		this.serverPlayerListeners.all().onPlayerLoggedIn(player);
	}

	/**
	 * @param scm
	 * @param player
	 * @param oldPlayer
	 * @param dimension
	 * @param won
	 */
	public void onRespawnPlayer(ServerConfigurationManager scm, EntityPlayerMP player, EntityPlayerMP oldPlayer, int dimension, boolean won)
	{
		this.serverPlayerListeners.all().onPlayerRespawn(player, oldPlayer, dimension, won);
	}

	/**
	 * @param scm
	 * @param player
	 */
	public void onPlayerLogout(ServerConfigurationManager scm, EntityPlayerMP player)
	{
		this.serverPlayerListeners.all().onPlayerLogout(player);
	}

	/**
	 * @param clock
	 * @param partialTicks
	 * @param inGame
	 */
	protected void onTick(boolean clock, float partialTicks, boolean inGame)
	{
		this.loader.onTick(clock, partialTicks, inGame);
	}

	/**
	 * @param mouseX
	 * @param mouseY
	 * @param partialTicks
	 */
	protected void onPostRender(int mouseX, int mouseY, float partialTicks)
	{
		this.loader.onPostRender(mouseX, mouseY, partialTicks);		
	}

	protected void onWorldChanged(World world)
	{
		this.loader.onWorldChanged(world);
	}
}
