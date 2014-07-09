package com.mumfrey.liteloader.core;

import java.util.LinkedList;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.command.ICommandManager;
import net.minecraft.command.ServerCommandManager;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.INetHandler;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.INetHandlerPlayServer;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;

import com.mojang.authlib.GameProfile;
import com.mumfrey.liteloader.LiteMod;
import com.mumfrey.liteloader.PluginChannelListener;
import com.mumfrey.liteloader.ServerChatFilter;
import com.mumfrey.liteloader.ServerCommandProvider;
import com.mumfrey.liteloader.ServerPlayerListener;
import com.mumfrey.liteloader.ServerPluginChannelListener;
import com.mumfrey.liteloader.api.InterfaceProvider;
import com.mumfrey.liteloader.api.Listener;
import com.mumfrey.liteloader.common.GameEngine;
import com.mumfrey.liteloader.common.LoadingProgress;
import com.mumfrey.liteloader.launch.LoaderProperties;
import com.mumfrey.liteloader.util.log.LiteLoaderLogger;

/**
 * @author Adam Mummery-Smith
 *
 * @param <TClient> Type of the client runtime, "Minecraft" on client and null on the server
 * @param <TServer> Type of the server runtime, "IntegratedServer" on the client, "MinecraftServer" on the server 
 */
public abstract class Events<TClient, TServer extends MinecraftServer> implements InterfaceProvider, IResourceManagerReloadListener
{
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
	
	
	/**
	 * List of mods which can filter server chat
	 */
	private LinkedList<ServerChatFilter> serverChatFilters = new LinkedList<ServerChatFilter>();
	
	/**
	 * List of mods which provide server commands
	 */
	private LinkedList<ServerCommandProvider> serverCommandProviders = new LinkedList<ServerCommandProvider>();
	
	/**
	 * List of mods which monitor server player events
	 */
	private LinkedList<ServerPlayerListener> serverPlayerListeners = new LinkedList<ServerPlayerListener>();

	/**
	 * Package private ctor
	 * 
	 * @param loader
	 * @param minecraft
	 */
	public Events(LiteLoader loader, GameEngine<TClient, TServer> engine, LoaderProperties properties)
	{
		this.loader   = loader;
		this.engine   = engine;
		this.profiler = engine.getProfiler();
	}

	/**
	 * 
	 */
	protected void onStartupComplete()
	{
		LoadingProgress.setMessage("Initialising CoreProviders...");
		this.loader.onStartupComplete();
		
		LoadingProgress.setMessage("Starting Game...");
	}

	/**
	 * @param listener
	 * @deprecated Use LiteLoader.getInterfaceManager().registerListener() instead
	 */
	@Deprecated
	public void addListener(LiteMod listener)
	{
		LiteLoader.getInterfaceManager().registerListener(listener);
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
		delegate.registerInterface(ServerChatFilter.class);
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
	 * @param serverChatFilter
	 */
	public void addServerChatFilter(ServerChatFilter serverChatFilter)
	{
		if (!this.serverChatFilters.contains(serverChatFilter))
		{
			this.serverChatFilters.add(serverChatFilter);
		}
	}

	/**
	 * @param serverCommandProvider
	 */
	public void addServerCommandProvider(ServerCommandProvider serverCommandProvider)
	{
		if (!this.serverCommandProviders.contains(serverCommandProvider))
		{
			this.serverCommandProviders.add(serverCommandProvider);
		}
	}

	/**
	 * @param serverPlayerListener
	 */
	public void addServerPlayerListener(ServerPlayerListener serverPlayerListener)
	{
		if (!this.serverPlayerListeners.contains(serverPlayerListener))
		{
			this.serverPlayerListeners.add(serverPlayerListener);
		}
	}
	
	@Override
	public void onResourceManagerReload(IResourceManager resourceManager)
	{
		LoadingProgress.setMessage("Reloading Resources...");
	}
	
	/**
	 * Callback from the chat hook
	 * @param netHandler 
	 * 
	 * @param chatPacket
	 * @return
	 */
	public boolean onServerChat(INetHandlerPlayServer netHandler, C01PacketChatMessage chatPacket)
	{
		EntityPlayerMP player = netHandler instanceof NetHandlerPlayServer ? ((NetHandlerPlayServer)netHandler).playerEntity : null;
		
		for (ServerChatFilter chatFilter : this.serverChatFilters)
		{
			if (!chatFilter.onChat(player, chatPacket, chatPacket.func_149439_c()))
			{
				return false;
			}
		}

		return true;
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
			
			for (ServerCommandProvider commandProvider : this.serverCommandProviders)
				commandProvider.provideCommands(serverCommandManager);
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
		for (ServerPlayerListener serverPlayerListener : this.serverPlayerListeners)
			serverPlayerListener.onPlayerConnect(player, profile);
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
		for (ServerPlayerListener serverPlayerListener : this.serverPlayerListeners)
			serverPlayerListener.onPlayerLoggedIn(player);
	}

	/**
	 * @param scm
	 * @param player
	 * @param oldPlayer
	 * @param dimension
	 * @param copy
	 */
	public void onRespawnPlayer(ServerConfigurationManager scm, EntityPlayerMP player, EntityPlayerMP oldPlayer, int dimension, boolean won)
	{
		for (ServerPlayerListener serverPlayerListener : this.serverPlayerListeners)
			serverPlayerListener.onPlayerRespawn(player, oldPlayer, dimension, won);
	}

	/**
	 * @param scm
	 * @param player
	 */
	public void onPlayerLogout(ServerConfigurationManager scm, EntityPlayerMP player)
	{
		for (ServerPlayerListener serverPlayerListener : this.serverPlayerListeners)
			serverPlayerListener.onPlayerLogout(player);
	}

	protected void onTick(boolean clock, float partialTicks, boolean inGame)
	{
		this.loader.onTick(clock, partialTicks, inGame);
	}

	protected void onPostRender(int mouseX, int mouseY, float partialTicks)
	{
		this.loader.onPostRender(mouseX, mouseY, partialTicks);		
	}

	protected void onWorldChanged(World world)
	{
		this.loader.onWorldChanged(world);
	}

	protected void onJoinGame(INetHandler netHandler, S01PacketJoinGame loginPacket)
	{
		this.loader.onJoinGame(netHandler, loginPacket);
	}

	/**
	 * @deprecated use LiteLoader.getInterfaceManager().registerListener(listener); instead
	 * @param chatFilter
	 */
	@Deprecated
	public void addChatFilter(Object chatFilter)
	{
		
	}

	/**
	 * @deprecated use LiteLoader.getInterfaceManager().registerListener(listener); instead
	 * @param tickListener
	 */
	@Deprecated
	public void addTickListener(Object tickListener)
	{
	}
}
