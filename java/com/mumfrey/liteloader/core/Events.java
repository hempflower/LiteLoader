package com.mumfrey.liteloader.core;

import java.util.LinkedList;
import java.util.logging.Level;

import net.minecraft.src.*;

import com.mumfrey.liteloader.*;
import com.mumfrey.liteloader.Tickable;
import com.mumfrey.liteloader.core.hooks.HookChat;
import com.mumfrey.liteloader.core.hooks.HookLogin;
import com.mumfrey.liteloader.core.hooks.HookProfiler;
import com.mumfrey.liteloader.util.ModUtilities;
import com.mumfrey.liteloader.util.PrivateFields;

/**
 *
 * @author Adam Mummery-Smith
 */
public class Events implements IPlayerUsage
{
	/**
	 * Reference to the loader instance
	 */
	private LiteLoader loader;
	
	/**
	 * Reference to the game
	 */
	private Minecraft minecraft;
	
	/**
	 * Plugin channel manager
	 */
	private PluginChannels pluginChannels;
	
	/**
	 * Reference to the minecraft timer
	 */
	private Timer minecraftTimer;
	
	/**
	 * Flags which keep track of whether hooks have been applied
	 */
	private boolean hookInitDone, lateInitDone, chatHooked, loginHooked, tickHooked;
	
	/**
	 * Profiler hook objects
	 */
	private HookProfiler profilerHook = new HookProfiler(this);
	
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
	 * List of mods which implement LoginListener interface and will receive
	 * client login events
	 */
	private LinkedList<LoginListener> loginListeners = new LinkedList<LoginListener>();
	
	/**
	 * List of mods which implement LoginListener interface and will receive
	 * client login events
	 */
	private LinkedList<PreLoginListener> preLoginListeners = new LinkedList<PreLoginListener>();

	public Events(LiteLoader loader, Minecraft minecraft, PluginChannels pluginChannels)
	{
		this.loader = loader;
		this.minecraft = minecraft;
		this.pluginChannels = pluginChannels;
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
				LiteLoader.getLogger().warning(String.format("Interface error initialising mod '%1s'. A mod implementing ChatFilter and ChatListener is not supported! Remove one of these interfaces", listener.getName()));
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
		
		if (listener instanceof PreLoginListener)
		{
			this.addPreLoginListener((PreLoginListener)listener);
		}
		
		if (listener instanceof LoginListener)
		{
			this.addLoginListener((LoginListener)listener);
		}
		
		if (listener instanceof PluginChannelListener)
		{
			this.pluginChannels.addPluginChannelListener((PluginChannelListener)listener);
		}
	}

	
	/**
	 * Initialise mod hooks
	 */
	public void initHooks()
	{
		try
		{
			LiteLoader.getLogger().info("Event manager is registering hooks");
			
			// Chat hook
			if ((this.chatListeners.size() > 0 || this.chatFilters.size() > 0) && !this.chatHooked)
			{
				this.chatHooked = true;
				HookChat.register();
				HookChat.registerPacketHandler(this);
			}
			
			// Login hook
			if ((this.preLoginListeners.size() > 0 || this.loginListeners.size() > 0) && !this.loginHooked)
			{
				this.loginHooked = true;
				ModUtilities.registerPacketOverride(1, HookLogin.class);
				HookLogin.events = this;
			}

			// Tick hook
			if (!this.tickHooked)
			{
				this.tickHooked = true;
				PrivateFields.minecraftProfiler.setFinal(this.minecraft, this.profilerHook);
			}
			
			// Sanity hook
			PlayerUsageSnooper snooper = this.minecraft.getPlayerUsageSnooper();
			PrivateFields.playerStatsCollector.setFinal(snooper, this);

			this.pluginChannels.initHook();
		}
		catch (Exception ex)
		{
			LiteLoader.getLogger().log(Level.WARNING, "Error creating hooks", ex);
			ex.printStackTrace();
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
			if (this.hookInitDone)
				this.initHooks();
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
			if (this.hookInitDone)
				this.initHooks();
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
	 * @param loginListener
	 */
	public void addPreLoginListener(PreLoginListener loginListener)
	{
		if (!this.preLoginListeners.contains(loginListener))
		{
			this.preLoginListeners.add(loginListener);
			if (this.hookInitDone)
				this.initHooks();
		}
	}
	
	/**
	 * @param loginListener
	 */
	public void addLoginListener(LoginListener loginListener)
	{
		if (!this.loginListeners.contains(loginListener))
		{
			this.loginListeners.add(loginListener);
			if (this.hookInitDone)
				this.initHooks();
		}
	}

	/**
	 * Late initialisation callback
	 */
	public void onInit()
	{
		this.loader.refreshResources();
		
		if (!this.lateInitDone)
		{
			this.lateInitDone = true;
			
			for (InitCompleteListener initMod : this.initListeners)
			{
				try
				{
					LiteLoader.getLogger().info("Calling late init for mod " + initMod.getName());
					initMod.onInitCompleted(this.minecraft, this.loader);
				}
				catch (Throwable th)
				{
					LiteLoader.getLogger().log(Level.WARNING, "Error initialising mod " + initMod.getName(), th);
				}
			}
		}

		this.loader.onInit();
	}
	
	/**
	 * Callback from the tick hook, pre render
	 */
	public void onRender()
	{
		this.loader.onRender();
		
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
	public void preRenderGUI()
	{
		for (RenderListener renderListener : this.renderListeners)
			renderListener.onRenderGui(this.minecraft.currentScreen);
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
		this.currentResolution = new ScaledResolution(this.minecraft.gameSettings, this.minecraft.displayWidth, this.minecraft.displayHeight);
		this.screenWidth = this.currentResolution.getScaledWidth();
		this.screenHeight = this.currentResolution.getScaledHeight();
		
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
	public void onTick(Profiler profiler, boolean clock)
	{
		float partialTicks = 0.0F;
		
		// Try to get the minecraft timer object and determine the value of the
		// partialTicks
		if (clock || this.minecraftTimer == null)
		{
			this.minecraftTimer = PrivateFields.minecraftTimer.get(this.minecraft);
		}
		
		// Hooray, we got the timer reference
		if (this.minecraftTimer != null)
		{
			partialTicks = this.minecraftTimer.renderPartialTicks;
			clock = this.minecraftTimer.elapsedTicks > 0;
		}
		
		// Flag indicates whether we are in game at the moment
		boolean inGame = this.minecraft.renderViewEntity != null && this.minecraft.renderViewEntity.worldObj != null;
		
		if (clock)
		{
			this.loader.onTick(partialTicks, inGame);
		}
		
		// Iterate tickable mods
		for (Tickable tickable : this.tickListeners)
		{
			profiler.startSection(tickable.getClass().getSimpleName());
			tickable.onTick(this.minecraft, partialTicks, inGame, clock);
			profiler.endSection();
		}
	}
	
	/**
	 * Callback from the chat hook
	 * 
	 * @param chatPacket
	 * @return
	 */
	public boolean onChat(Packet3Chat chatPacket)
	{
		if (chatPacket.message == null)
			return true;
		
		ChatMessageComponent chat = ChatMessageComponent.createFromJson(chatPacket.message);
		String message = chat.toStringWithFormatting(true);
		
		// Chat filters get a stab at the chat first, if any filter returns
		// false the chat is discarded
		for (ChatFilter chatFilter : this.chatFilters)
		{
			if (chatFilter.onChat(chatPacket, chat, message))
			{
				chat = ChatMessageComponent.createFromJson(chatPacket.message);
				message = chat.toStringWithFormatting(true);
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
	 * Pre-login callback from the login hook
	 * 
	 * @param netHandler
	 * @param hookLogin
	 * @return
	 */
	public boolean onPreLogin(NetHandler netHandler, Packet1Login loginPacket)
	{
		boolean cancelled = false;
		
		for (PreLoginListener loginListener : this.preLoginListeners)
		{
			cancelled |= !loginListener.onPreLogin(netHandler, loginPacket);
		}
		
		return !cancelled;
	}
	
	/**
	 * Callback from the login hook
	 * 
	 * @param netHandler
	 * @param loginPacket
	 */
	public void onConnectToServer(NetHandler netHandler, Packet1Login loginPacket)
	{
		this.loader.onLogin(netHandler, loginPacket);
		
		for (LoginListener loginListener : this.loginListeners)
			loginListener.onLogin(netHandler, loginPacket);
		
		this.pluginChannels.onConnectToServer(netHandler, loginPacket);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.minecraft.src.IPlayerUsage#addServerStatsToSnooper(net.minecraft.
	 * src.PlayerUsageSnooper)
	 */
	@Override
	public void addServerStatsToSnooper(PlayerUsageSnooper var1)
	{
		this.minecraft.addServerStatsToSnooper(var1);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.minecraft.src.IPlayerUsage#addServerTypeToSnooper(net.minecraft.src
	 * .PlayerUsageSnooper)
	 */
	@Override
	public void addServerTypeToSnooper(PlayerUsageSnooper var1)
	{
		this.sanityCheck();
		this.minecraft.addServerTypeToSnooper(var1);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see net.minecraft.src.IPlayerUsage#isSnooperEnabled()
	 */
	@Override
	public boolean isSnooperEnabled()
	{
		return this.minecraft.isSnooperEnabled();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see net.minecraft.src.IPlayerUsage#getLogAgent()
	 */
	@Override
	public ILogAgent getLogAgent()
	{
		return this.minecraft.getLogAgent();
	}
	
	/**
	 * Check that the profiler hook hasn't been overridden by something else
	 */
	private void sanityCheck()
	{
		if (this.tickHooked && this.minecraft.mcProfiler != this.profilerHook)
		{
			PrivateFields.minecraftProfiler.setFinal(this.minecraft, this.profilerHook);
		}
	}
}
