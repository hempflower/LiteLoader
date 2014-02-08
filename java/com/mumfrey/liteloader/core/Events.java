package com.mumfrey.liteloader.core;

import java.util.LinkedList;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.command.ICommandManager;
import net.minecraft.command.ServerCommandManager;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.INetHandler;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.login.INetHandlerLoginClient;
import net.minecraft.network.login.server.S02PacketLoginSuccess;
import net.minecraft.network.play.INetHandlerPlayServer;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.Timer;
import net.minecraft.world.WorldSettings;

import org.lwjgl.input.Mouse;

import com.mojang.authlib.GameProfile;
import com.mumfrey.liteloader.*;
import com.mumfrey.liteloader.core.gen.GenProfiler;
import com.mumfrey.liteloader.core.overlays.IMinecraft;
import com.mumfrey.liteloader.util.PrivateFields;
import com.mumfrey.liteloader.util.log.LiteLoaderLogger;

/**
 *
 * @author Adam Mummery-Smith
 */
public class Events
{
	/**
	 * Reference to the loader instance
	 */
	private final LiteLoader loader;
	
	/**
	 * Reference to the game
	 */
	private final Minecraft minecraft;
	
	/**
	 * Client plugin channel manager
	 */
	private final ClientPluginChannels clientPluginChannels;
	
	/**
	 * Server plugin channel manager
	 */
	private final ServerPluginChannels serverPluginChannels;
	
	/**
	 * Reference to the minecraft timer
	 */
	private Timer minecraftTimer;
	
	/**
	 * Flags which keep track of whether hooks have been applied
	 */
	private boolean hookInitDone, lateInitDone, profilerHooked;
	
	/**
	 * Profiler hook objects
	 */
	private Profiler genProfiler = null;
	
	/**
	 * ScaledResolution used by the pre-chat and post-chat render callbacks
	 */
	private ScaledResolution currentResolution;
	
	/**
	 * Current screen width
	 */
	private int screenWidth = 854;

	/**
	 * Current screen height
	 */
	private int screenHeight = 480;

	
	/**
	 * List of mods which implement Tickable interface and will receive tick
	 * events
	 */
	private LinkedList<Tickable> tickListeners = new LinkedList<Tickable>();
	
	/**
	 * List of mods which implement the GameLoopListener interface and will
	 * receive loop events
	 */
	private LinkedList<GameLoopListener> loopListeners = new LinkedList<GameLoopListener>();
	
	/**
	 * 
	 */
	private LinkedList<InitCompleteListener> initListeners = new LinkedList<InitCompleteListener>();
	
	/**
	 * List of mods which implement RenderListener interface and will receive
	 * render events events
	 */
	private LinkedList<RenderListener> renderListeners = new LinkedList<RenderListener>();
	
	/**
	 * List of mods which implement the PostRenderListener interface and want to
	 * render entities
	 */
	private LinkedList<PostRenderListener> postRenderListeners = new LinkedList<PostRenderListener>();
	
	/**
	 * List of mods which implement HUDRenderListener and want callbacks when HUD is rendered
	 */
	private LinkedList<HUDRenderListener> hudRenderListeners = new LinkedList<HUDRenderListener>();
	
	/**
	 * List of mods which implement ChatRenderListener and want to know when
	 * chat is rendered
	 */
	private LinkedList<ChatRenderListener> chatRenderListeners = new LinkedList<ChatRenderListener>();
	
	/**
	 * List of mods which implement ChatListener interface and will receive chat
	 * events
	 */
	private LinkedList<ChatListener> chatListeners = new LinkedList<ChatListener>();
	
	/**
	 * List of mods which implement ChatFilter interface and will receive chat
	 * filter events
	 */
	private LinkedList<ChatFilter> chatFilters = new LinkedList<ChatFilter>();
	
	/**
	 * List of mods which implement PostLoginListener and want to be notified post login
	 */
	private LinkedList<PostLoginListener> postLoginListeners = new LinkedList<PostLoginListener>();
	
	/**
	 * List of mods which implement LoginListener interface and will receive
	 * client login events
	 */
	private LinkedList<JoinGameListener> joinGameListeners = new LinkedList<JoinGameListener>();
	
	/**
	 * List of mods which implement LoginListener interface and will receive
	 * client login events
	 */
	private LinkedList<PreJoinGameListener> preJoinGameListeners = new LinkedList<PreJoinGameListener>();
	
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
	 * List of mods which monitor outbound chat
	 */
	private LinkedList<OutboundChatListener> outboundChatListeners = new LinkedList<OutboundChatListener>();
	
	/**
	 * Hash code of the current world. We don't store the world reference here because we don't want
	 * to mess with world GC by mistake
	 */
	private int worldHashCode = 0;

	/**
	 * Package private ctor
	 * 
	 * @param loader
	 * @param minecraft
	 * @param pluginChannels
	 */
	Events(LiteLoader loader, Minecraft minecraft, ClientPluginChannels pluginChannels, ServerPluginChannels serverPluginChannels, boolean genMappings)
	{
		this.loader = loader;
		this.minecraft = minecraft;
		this.clientPluginChannels = pluginChannels;
		this.serverPluginChannels = serverPluginChannels;
		try
		{
			if (genMappings)
			{
				this.genProfiler = GenProfiler.class.newInstance();
			}
		}
		catch (Throwable th)
		{
//			th.printStackTrace();
		}
	}

	/**
	 * Add a listener to the relevant listener lists
	 * 
	 * @param listener
	 */
	public void addListener(LiteMod listener)
	{
		if (listener instanceof Tickable)
		{
			this.addTickListener((Tickable)listener);
		}
		
		if (listener instanceof GameLoopListener)
		{
			this.addLoopListener((GameLoopListener)listener);
		}
		
		if (listener instanceof InitCompleteListener)
		{
			this.addInitListener((InitCompleteListener)listener);
		}
		
		if (listener instanceof RenderListener)
		{
			this.addRenderListener((RenderListener)listener);
		}
		
		if (listener instanceof PostRenderListener)
		{
			this.addPostRenderListener((PostRenderListener)listener);
		}
		
		if (listener instanceof ChatFilter)
		{
			this.addChatFilter((ChatFilter)listener);
		}
		
		if (listener instanceof ChatListener)
		{
			if (listener instanceof ChatFilter)
			{
				LiteLoaderLogger.warning("Interface error initialising mod '%1s'. A mod implementing ChatFilter and ChatListener is not supported! Remove one of these interfaces", listener.getName());
			}
			else
			{
				this.addChatListener((ChatListener)listener);
			}
		}
		
		if (listener instanceof ChatRenderListener)
		{
			this.addChatRenderListener((ChatRenderListener)listener);
		}
		
		if (listener instanceof HUDRenderListener)
		{
			this.addHUDRenderListener((HUDRenderListener)listener);
		}
		
		if (listener instanceof PreJoinGameListener)
		{
			this.addPreJoinGameListener((PreJoinGameListener)listener);
		}
		
		if (listener instanceof JoinGameListener)
		{
			this.addJoinGameListener((JoinGameListener)listener);
		}
		
		if (listener instanceof ServerChatFilter)
		{
			this.addServerChatFilter((ServerChatFilter)listener);
		}
		
		if (listener instanceof ServerCommandProvider)
		{
			this.addServerCommandProvider((ServerCommandProvider)listener);
		}
		
		if (listener instanceof ServerPlayerListener)
		{
			this.addServerPlayerListener((ServerPlayerListener)listener);
		}
		
		if (listener instanceof OutboundChatListener)
		{
			this.addOutboundChatListener((OutboundChatListener)listener);
		}
		
		this.clientPluginChannels.addListener(listener);
		this.serverPluginChannels.addListener(listener);
		
		if (listener instanceof CommonPluginChannelListener && !(listener instanceof PluginChannelListener) && !(listener instanceof ServerPluginChannelListener))
		{
			LiteLoaderLogger.warning("Interface error for mod '%1s'. Implementing CommonPluginChannelListener has no effect! Use PluginChannelListener or ServerPluginChannelListener instead", listener.getName());
		}
	}
	
	/**
	 * Initialise hooks
	 */
	public void initHooks()
	{
		if (this.genProfiler != null)
		{
			try
			{
				LiteLoaderLogger.info("Event manager is registering the mapping generator hook");
				
				// Tick hook
				if (!this.profilerHooked)
				{
					this.profilerHooked = true;
					PrivateFields.minecraftProfiler.setFinal(this.minecraft, this.genProfiler);
				}
			}
			catch (Exception ex)
			{
				LiteLoaderLogger.warning(ex, "Error creating hook");
				ex.printStackTrace();
			}
		}	
		
		this.hookInitDone = true;
	}
	
	/**
	 * @param tickable
	 */
	public void addTickListener(Tickable tickable)
	{
		if (!this.tickListeners.contains(tickable))
		{
			this.tickListeners.add(tickable);
			if (this.hookInitDone)
				this.initHooks();
		}
	}
	
	/**
	 * @param loopListener
	 */
	public void addLoopListener(GameLoopListener loopListener)
	{
		if (!this.loopListeners.contains(loopListener))
		{
			this.loopListeners.add(loopListener);
			if (this.hookInitDone)
				this.initHooks();
		}
	}
	
	/**
	 * @param initCompleteListener
	 */
	public void addInitListener(InitCompleteListener initCompleteListener)
	{
		if (!this.initListeners.contains(initCompleteListener))
		{
			this.initListeners.add(initCompleteListener);
			if (this.hookInitDone)
				this.initHooks();
		}
	}
	
	/**
	 * @param renderListener
	 */
	public void addRenderListener(RenderListener renderListener)
	{
		if (!this.renderListeners.contains(renderListener))
		{
			this.renderListeners.add(renderListener);
			if (this.hookInitDone)
				this.initHooks();
		}
	}
	
	/**
	 * @param postRenderListener
	 */
	public void addPostRenderListener(PostRenderListener postRenderListener)
	{
		if (!this.postRenderListeners.contains(postRenderListener))
		{
			this.postRenderListeners.add(postRenderListener);
			if (this.hookInitDone)
				this.initHooks();
		}
	}
	
	/**
	 * @param chatFilter
	 */
	public void addChatFilter(ChatFilter chatFilter)
	{
		if (!this.chatFilters.contains(chatFilter))
		{
			this.chatFilters.add(chatFilter);
		}
	}
	
	/**
	 * @param chatListener
	 */
	public void addChatListener(ChatListener chatListener)
	{
		if (!this.chatListeners.contains(chatListener))
		{
			this.chatListeners.add(chatListener);
		}
	}
	
	/**
	 * @param chatRenderListener
	 */
	public void addChatRenderListener(ChatRenderListener chatRenderListener)
	{
		if (!this.chatRenderListeners.contains(chatRenderListener))
		{
			this.chatRenderListeners.add(chatRenderListener);
			if (this.hookInitDone)
				this.initHooks();
		}
	}
	
	/**
	 * @param hudRenderListener
	 */
	public void addHUDRenderListener(HUDRenderListener hudRenderListener)
	{
		if (!this.hudRenderListeners.contains(hudRenderListener))
		{
			this.hudRenderListeners.add(hudRenderListener);
			if (this.hookInitDone)
				this.initHooks();
		}
	}
	
	/**
	 * @param postLoginListener
	 */
	public void addPreJoinGameListener(PostLoginListener postLoginListener)
	{
		if (!this.postLoginListeners.contains(postLoginListener))
		{
			this.postLoginListeners.add(postLoginListener);
		}
	}
	
	/**
	 * @param joinGameListener
	 */
	public void addPreJoinGameListener(PreJoinGameListener joinGameListener)
	{
		if (!this.preJoinGameListeners.contains(joinGameListener))
		{
			this.preJoinGameListeners.add(joinGameListener);
		}
	}
	
	/**
	 * @param joinGameListener
	 */
	public void addJoinGameListener(JoinGameListener joinGameListener)
	{
		if (!this.joinGameListeners.contains(joinGameListener))
		{
			this.joinGameListeners.add(joinGameListener);
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

	/**
	 * @param outboundChatListener
	 */
	private void addOutboundChatListener(OutboundChatListener outboundChatListener)
	{
		if (!this.outboundChatListeners.contains(outboundChatListener))
		{
			this.outboundChatListeners.add(outboundChatListener);
		}
	}

	/**
	 * Late initialisation callback
	 */
	public void preBeginGame()
	{
		this.loader.preInitMods();
		
		if (!this.lateInitDone)
		{
			this.lateInitDone = true;
			
			for (InitCompleteListener initMod : this.initListeners)
			{
				try
				{
					LiteLoaderLogger.info("Calling late init for mod " + initMod.getName());
					initMod.onInitCompleted(this.minecraft, this.loader);
				}
				catch (Throwable th)
				{
					LiteLoaderLogger.warning(th, "Error initialising mod %s", initMod.getName());
				}
			}
		}

		this.loader.preBeginGame();
	}
	
	/**
	 * Callback from the tick hook, pre render
	 */
	public void onRender()
	{
		this.currentResolution = new ScaledResolution(this.minecraft.gameSettings, this.minecraft.displayWidth, this.minecraft.displayHeight);
		this.screenWidth = this.currentResolution.getScaledWidth();
		this.screenHeight = this.currentResolution.getScaledHeight();
		
		for (RenderListener renderListener : this.renderListeners)
			renderListener.onRender();
	}
	
	/**
	 * Callback from the tick hook, post render entities
	 */
	public void postRenderEntities()
	{
		float partialTicks = (this.minecraftTimer != null) ? this.minecraftTimer.elapsedPartialTicks : 0.0F;
		
		for (PostRenderListener renderListener : this.postRenderListeners)
			renderListener.onPostRenderEntities(partialTicks);
	}
	
	/**
	 * Callback from the tick hook, post render
	 */
	public void postRender()
	{
		float partialTicks = (this.minecraftTimer != null) ? this.minecraftTimer.elapsedPartialTicks : 0.0F;
		
		for (PostRenderListener renderListener : this.postRenderListeners)
			renderListener.onPostRender(partialTicks);
	}
	
	/**
	 * Called immediately before the current GUI is rendered
	 */
	public void preRenderGUI(int ref)
	{
		if (!this.minecraft.skipRenderWorld && ref == (this.minecraft.theWorld == null ? 1 : 2))
		{
			for (RenderListener renderListener : this.renderListeners)
				renderListener.onRenderGui(this.minecraft.currentScreen);
		}
	}
	
	/**
	 * Called immediately after the world/camera transform is initialised
	 */
	public void onSetupCameraTransform()
	{
		for (RenderListener renderListener : this.renderListeners)
			renderListener.onSetupCameraTransform();
	}
	
	/**
	 * Called immediately before the chat log is rendered
	 */
	public void onRenderChat()
	{
		GuiNewChat chat = this.minecraft.ingameGUI.getChatGUI();
		
		for (ChatRenderListener chatRenderListener : this.chatRenderListeners)
			chatRenderListener.onPreRenderChat(this.screenWidth, this.screenHeight, chat);
	}
	
	/**
	 * Called immediately after the chat log is rendered
	 */
	public void postRenderChat()
	{
		GuiNewChat chat = this.minecraft.ingameGUI.getChatGUI();
		
		for (ChatRenderListener chatRenderListener : this.chatRenderListeners)
			chatRenderListener.onPostRenderChat(this.screenWidth, this.screenHeight, chat);
	}
	
	/**
	 * Callback when about to render the HUD
	 */
	public void onRenderHUD()
	{
		if (!this.minecraft.gameSettings.hideGUI || this.minecraft.currentScreen != null)
		{
			for (HUDRenderListener hudRenderListener : this.hudRenderListeners)
				hudRenderListener.onPreRenderHUD(this.screenWidth, this.screenHeight);
		}
	}
	
	/**
	 * Callback when the HUD has just been rendered
	 */
	public void postRenderHUD()
	{
		if (!this.minecraft.gameSettings.hideGUI || this.minecraft.currentScreen != null)
		{
			for (HUDRenderListener hudRenderListener : this.hudRenderListeners)
				hudRenderListener.onPostRenderHUD(this.screenWidth, this.screenHeight);
		}
	}
	
	/**
	 * Callback from the tick hook, called every frame when the timer is updated
	 */
	public void onTimerUpdate()
	{
		for (GameLoopListener loopListener : this.loopListeners)
			loopListener.onRunGameLoop(this.minecraft);
	}
	
	/**
	 * Callback from the tick hook, ticks all tickable mods
	 * 
	 * @param clock True if this is a new tick (otherwise it's just a new frame)
	 */
	public void onTick(boolean clock)
	{
		this.minecraft.mcProfiler.startSection("litemods");
		float partialTicks = 0.0F;
		
		// Try to get the minecraft timer object and determine the value of the
		// partialTicks
		if (clock || this.minecraftTimer == null)
		{
			this.minecraftTimer = ((IMinecraft)this.minecraft).getTimer();
		}
		
		// Hooray, we got the timer reference
		if (this.minecraftTimer != null)
		{
			partialTicks = this.minecraftTimer.renderPartialTicks;
			clock = this.minecraftTimer.elapsedTicks > 0;
		}
		
		// Flag indicates whether we are in game at the moment
		boolean inGame = this.minecraft.renderViewEntity != null && this.minecraft.renderViewEntity.worldObj != null;
		
		this.minecraft.mcProfiler.startSection("loader");
		this.loader.onTick(clock, partialTicks, inGame);

		int mouseX = Mouse.getX() * this.screenWidth / this.minecraft.displayWidth;
		int mouseY = this.screenHeight - Mouse.getY() * this.screenHeight / this.minecraft.displayHeight - 1;
		this.minecraft.mcProfiler.endStartSection("postrender");
		this.loader.postRender(mouseX, mouseY, partialTicks);
		this.minecraft.mcProfiler.endSection();
		
		// Iterate tickable mods
		for (Tickable tickable : this.tickListeners)
		{
			this.minecraft.mcProfiler.startSection(tickable.getClass().getSimpleName().toLowerCase());
			tickable.onTick(this.minecraft, partialTicks, inGame, clock);
			this.minecraft.mcProfiler.endSection();
		}
		
		// Detected world change
		if (this.minecraft.theWorld != null)
		{
			if (this.minecraft.theWorld.hashCode() != this.worldHashCode)
			{
				this.worldHashCode = this.minecraft.theWorld.hashCode();
				this.loader.onWorldChanged(this.minecraft.theWorld);
			}
		}
		else
		{
			this.worldHashCode = 0;
			this.loader.onWorldChanged(null);
		}
		
		this.minecraft.mcProfiler.endSection();
	}
	
	/**
	 * Callback from the chat hook
	 * 
	 * @param chatPacket
	 * @return
	 */
	public boolean onChat(S02PacketChat chatPacket)
	{
		if (chatPacket.func_148915_c() == null)
			return true;
		
		IChatComponent chat = chatPacket.func_148915_c();
		String message = chat.getUnformattedText();
		
		// Chat filters get a stab at the chat first, if any filter returns
		// false the chat is discarded
		for (ChatFilter chatFilter : this.chatFilters)
		{
			if (chatFilter.onChat(chatPacket, chat, message))
			{
				chat = chatPacket.func_148915_c();
				message = chat.getUnformattedText();
			}
			else
			{
				return false;
			}
		}
		
		// Chat listeners get the chat if no filter removed it
		for (ChatListener chatListener : this.chatListeners)
			chatListener.onChat(chat, message);
		
		return true;
	}

	/**
	 * @param packet
	 * @param message
	 */
	public void onSendChatMessage(C01PacketChatMessage packet, String message)
	{
		for (OutboundChatListener outboundChatListener : this.outboundChatListeners)
		{
			outboundChatListener.onSendChatMessage(packet, message);
		}
	}

	/**
	 * @param netHandler
	 * @param loginPacket
	 */
	public void onPostLogin(INetHandlerLoginClient netHandler, S02PacketLoginSuccess loginPacket)
	{
		this.clientPluginChannels.onPostLogin(netHandler, loginPacket);

		for (PostLoginListener loginListener : this.postLoginListeners)
			loginListener.onPostLogin(netHandler, loginPacket);
	}
	
	/**
	 * Pre join game callback from the login hook
	 * 
	 * @param netHandler
	 * @param hookLogin
	 * @return
	 */
	public boolean onPreJoinGame(INetHandler netHandler, S01PacketJoinGame loginPacket)
	{
		boolean cancelled = false;
		
		for (PreJoinGameListener joinGameListener : this.preJoinGameListeners)
		{
			cancelled |= !joinGameListener.onPreJoinGame(netHandler, loginPacket);
		}
		
		return !cancelled;
	}
	
	/**
	 * Callback from the join game hook
	 * 
	 * @param netHandler
	 * @param loginPacket
	 */
	public void onJoinGame(INetHandler netHandler, S01PacketJoinGame loginPacket)
	{
		this.loader.onJoinGame(netHandler, loginPacket);
		this.clientPluginChannels.onJoinGame(netHandler, loginPacket);
		
		for (JoinGameListener joinGameListener : this.joinGameListeners)
			joinGameListener.onJoinGame(netHandler, loginPacket);
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
	public void onStartIntegratedServer(IntegratedServer instance, String folderName, String worldName, WorldSettings worldSettings)
	{
		ICommandManager commandManager = instance.getCommandManager();
		
		if (commandManager instanceof ServerCommandManager)
		{
			ServerCommandManager serverCommandManager = (ServerCommandManager)commandManager;
			
			for (ServerCommandProvider commandProvider : this.serverCommandProviders)
				commandProvider.provideCommands(serverCommandManager);
		}
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
		this.serverPluginChannels.onPlayerJoined(player);
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
}
