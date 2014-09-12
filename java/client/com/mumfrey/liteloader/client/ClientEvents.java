package com.mumfrey.liteloader.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.network.INetHandler;
import net.minecraft.network.login.INetHandlerLoginClient;
import net.minecraft.network.login.server.S02PacketLoginSuccess;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.Timer;

import org.lwjgl.input.Mouse;

import com.mumfrey.liteloader.*;
import com.mumfrey.liteloader.client.gen.GenProfiler;
import com.mumfrey.liteloader.client.overlays.IMinecraft;
import com.mumfrey.liteloader.client.util.PrivateFields;
import com.mumfrey.liteloader.common.LoadingProgress;
import com.mumfrey.liteloader.core.ClientPluginChannels;
import com.mumfrey.liteloader.core.Events;
import com.mumfrey.liteloader.core.InterfaceRegistrationDelegate;
import com.mumfrey.liteloader.core.LiteLoader;
import com.mumfrey.liteloader.core.event.HandlerList;
import com.mumfrey.liteloader.launch.LoaderProperties;
import com.mumfrey.liteloader.transformers.event.EventInfo;
import com.mumfrey.liteloader.util.log.LiteLoaderLogger;

public class ClientEvents extends Events<Minecraft, IntegratedServer>
{	
	private static ClientEvents instance;

	/**
	 * Reference to the game
	 */
	protected final GameEngineClient engineClient;

	/**
	 * Reference to the minecraft timer
	 */
	private Timer minecraftTimer;
	
	/**
	 * Flags which keep track of whether hooks have been applied
	 */
	private boolean lateInitDone, profilerHooked;

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
	 * 
	 */
	private boolean wasFullScreen = false;

	/**
	 * Hash code of the current world. We don't store the world reference here because we don't want
	 * to mess with world GC by mistake
	 */
	private int worldHashCode = 0;

	private HandlerList<Tickable>             tickListeners         = new HandlerList<Tickable>(Tickable.class);
	private HandlerList<GameLoopListener>     loopListeners         = new HandlerList<GameLoopListener>(GameLoopListener.class);
	private HandlerList<RenderListener>       renderListeners       = new HandlerList<RenderListener>(RenderListener.class);
	private HandlerList<PostRenderListener>   postRenderListeners   = new HandlerList<PostRenderListener>(PostRenderListener.class);
	private HandlerList<HUDRenderListener>    hudRenderListeners    = new HandlerList<HUDRenderListener>(HUDRenderListener.class);
	private HandlerList<ChatRenderListener>   chatRenderListeners   = new HandlerList<ChatRenderListener>(ChatRenderListener.class);
	private HandlerList<ChatListener>         chatListeners         = new HandlerList<ChatListener>(ChatListener.class);
	private HandlerList<PostLoginListener>    postLoginListeners    = new HandlerList<PostLoginListener>(PostLoginListener.class);
	private HandlerList<JoinGameListener>     joinGameListeners     = new HandlerList<JoinGameListener>(JoinGameListener.class);
	private HandlerList<OutboundChatListener> outboundChatListeners = new HandlerList<OutboundChatListener>(OutboundChatListener.class);
	private HandlerList<ViewportListener>     viewportListeners     = new HandlerList<ViewportListener>(ViewportListener.class);
	private HandlerList<FrameBufferListener>  frameBufferListeners  = new HandlerList<FrameBufferListener>(FrameBufferListener.class);
	private HandlerList<InitCompleteListener> initListeners         = new HandlerList<InitCompleteListener>(InitCompleteListener.class);
	private HandlerList<ChatFilter>           chatFilters           = new HandlerList<ChatFilter>(ChatFilter.class);
	private HandlerList<PreJoinGameListener>  preJoinGameListeners  = new HandlerList<PreJoinGameListener>(PreJoinGameListener.class);
	private HandlerList<OutboundChatFilter>   outboundChatFilters   = new HandlerList<OutboundChatFilter>(OutboundChatFilter.class);

	@SuppressWarnings("cast")
	public ClientEvents(LiteLoader loader, GameEngineClient engine, LoaderProperties properties)
	{
		super(loader, engine, properties);
		
		ClientEvents.instance = this;
		
		this.engineClient = (GameEngineClient)engine;
		try
		{
			if (properties.getBooleanProperty(LoaderProperties.OPTION_GENERATE_MAPPINGS))
			{
				this.genProfiler = GenProfiler.class.newInstance();
			}
		}
		catch (Throwable th)
		{
//			th.printStackTrace();
		}
	}
	
	static ClientEvents getInstance()
	{
		return ClientEvents.instance;
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.api.InterfaceProvider#registerInterfaces(com.mumfrey.liteloader.core.InterfaceRegistrationDelegate)
	 */
	@Override
	public void registerInterfaces(InterfaceRegistrationDelegate delegate)
	{
		super.registerInterfaces(delegate);
		
		delegate.registerInterface(Tickable.class);
		delegate.registerInterface(GameLoopListener.class);
		delegate.registerInterface(RenderListener.class);
		delegate.registerInterface(PostRenderListener.class);
		delegate.registerInterface(HUDRenderListener.class);
		delegate.registerInterface(ChatRenderListener.class);
		delegate.registerInterface(ChatListener.class);
		delegate.registerInterface(PostLoginListener.class);
		delegate.registerInterface(JoinGameListener.class);
		delegate.registerInterface(OutboundChatListener.class);
		delegate.registerInterface(ViewportListener.class);
		delegate.registerInterface(FrameBufferListener.class);
		delegate.registerInterface(InitCompleteListener.class);
		delegate.registerInterface(ChatFilter.class);
		delegate.registerInterface(PreJoinGameListener.class);
		delegate.registerInterface(OutboundChatFilter.class);
	}
	
	/**
	 * Initialise hooks
	 */
	@Override
	public void initProvider()
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
					PrivateFields.minecraftProfiler.setFinal(this.engine.getClient(), this.genProfiler);
				}
			}
			catch (Exception ex)
			{
				LiteLoaderLogger.warning(ex, "Error creating hook");
				ex.printStackTrace();
			}
		}	
	}

	/**
	 * @deprecated use LiteLoader.getInterfaceManager().registerListener(listener); instead
	 * @param tickListener
	 */
	@Deprecated
	@Override
	public void addTickListener(Object tickListener)
	{
		if (tickListener instanceof Tickable)
		{
			this.addTickListener((Tickable)tickListener);
		}
	}

	/**
	 * @param tickable
	 */
	public void addTickListener(Tickable tickable)
	{
		this.tickListeners.add(tickable);
	}
	
	/**
	 * @param loopListener
	 */
	public void addLoopListener(GameLoopListener loopListener)
	{
		this.loopListeners.add(loopListener);
	}
	
	/**
	 * @param initCompleteListener
	 */
	public void addInitListener(InitCompleteListener initCompleteListener)
	{
		this.initListeners.add(initCompleteListener);
	}
	
	/**
	 * @param renderListener
	 */
	public void addRenderListener(RenderListener renderListener)
	{
		this.renderListeners.add(renderListener);
	}
	
	/**
	 * @param postRenderListener
	 */
	public void addPostRenderListener(PostRenderListener postRenderListener)
	{
		this.postRenderListeners.add(postRenderListener);
	}
	
	/**
	 * @deprecated use LiteLoader.getInterfaceManager().registerListener(listener); instead
	 * @param chatFilter
	 */
	@Deprecated
	@Override
	public void addChatFilter(Object chatFilter)
	{
		if (chatFilter instanceof ChatFilter)
		{
			this.addChatFilter((ChatFilter)chatFilter);
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
		if (chatListener instanceof ChatFilter)
		{
			LiteLoaderLogger.warning("Interface error initialising mod '%1s'. A mod implementing ChatFilter and ChatListener is not supported! Remove one of these interfaces", chatListener.getName());
		}
		else
		{
			this.chatListeners.add(chatListener);
		}
	}
	
	/**
	 * @param chatRenderListener
	 */
	public void addChatRenderListener(ChatRenderListener chatRenderListener)
	{
		this.chatRenderListeners.add(chatRenderListener);
	}
	
	/**
	 * @param hudRenderListener
	 */
	public void addHUDRenderListener(HUDRenderListener hudRenderListener)
	{
		this.hudRenderListeners.add(hudRenderListener);
	}
	
	/**
	 * @param postLoginListener
	 */
	public void addPreJoinGameListener(PostLoginListener postLoginListener)
	{
		this.postLoginListeners.add(postLoginListener);
	}
	
	/**
	 * @param joinGameListener
	 */
	public void addPreJoinGameListener(PreJoinGameListener joinGameListener)
	{
		this.preJoinGameListeners.add(joinGameListener);
	}
	
	/**
	 * @param joinGameListener
	 */
	public void addJoinGameListener(JoinGameListener joinGameListener)
	{
		this.joinGameListeners.add(joinGameListener);
	}

	/**
	 * @param outboundChatListener
	 */
	public void addOutboundChatListener(OutboundChatListener outboundChatListener)
	{
		this.outboundChatListeners.add(outboundChatListener);
	}
	
	/**
	 * @param outboundChatFilter
	 */
	public void addOutboundChatFiler(OutboundChatFilter outboundChatFilter)
	{
		this.outboundChatFilters.add(outboundChatFilter);
	}
	
	/**
	 * @param viewportListener
	 */
	public void addViewportListener(ViewportListener viewportListener)
	{
		this.viewportListeners.add(viewportListener);
	}
	
	/**
	 * @param frameBufferListener
	 */
	public void addFrameBufferListener(FrameBufferListener frameBufferListener)
	{
		this.frameBufferListeners.add(frameBufferListener);
	}

	/**
	 * Late initialisation callback
	 */
	@Override
	protected void onStartupComplete()
	{
		this.engine.refreshResources(false);
		
		if (!this.lateInitDone)
		{
			this.lateInitDone = true;
			
			for (InitCompleteListener initMod : this.initListeners)
			{
				try
				{
					LoadingProgress.setMessage("Calling late init for mod %s...", initMod.getName());
					LiteLoaderLogger.info("Calling late init for mod %s", initMod.getName());
					initMod.onInitCompleted(this.engine.getClient(), this.loader);
				}
				catch (Throwable th)
				{
					this.mods.onLateInitFailed(initMod, th);
					LiteLoaderLogger.warning(th, "Error calling late init for mod %s", initMod.getName());
				}
			}
		}
		
		this.onResize(this.engineClient.getClient());

		super.onStartupComplete();
	}

	public void onResize(Minecraft minecraft)
	{
		this.currentResolution = this.engineClient.getScaledResolution();
		this.screenWidth = this.currentResolution.getScaledWidth();
		this.screenHeight = this.currentResolution.getScaledHeight();
		
		if (this.wasFullScreen != minecraft.isFullScreen())
		{
			this.viewportListeners.all().onFullScreenToggled(minecraft.isFullScreen());
		}
		
		this.wasFullScreen = minecraft.isFullScreen();
		this.viewportListeners.all().onViewportResized(this.currentResolution, minecraft.displayWidth, minecraft.displayHeight);
	}
	
	/**
	 * Callback from the tick hook, pre render
	 */
	void onRender()
	{
		this.renderListeners.all().onRender();
	}
	
	/**
	 * Callback from the tick hook, post render entities
	 */
	void postRenderEntities()
	{
		float partialTicks = (this.minecraftTimer != null) ? this.minecraftTimer.elapsedPartialTicks : 0.0F;
		this.postRenderListeners.all().onPostRenderEntities(partialTicks);
	}
	
	/**
	 * Callback from the tick hook, post render
	 */
	void postRender()
	{
		float partialTicks = (this.minecraftTimer != null) ? this.minecraftTimer.elapsedPartialTicks : 0.0F;
		this.postRenderListeners.all().onPostRender(partialTicks);
	}
	
	/**
	 * Called immediately before the current GUI is rendered
	 */
	void preRenderGUI(int ref)
	{
		Minecraft minecraft = this.engine.getClient();
		
		if (!minecraft.skipRenderWorld && ref == (minecraft.theWorld == null ? 1 : 2))
		{
			this.renderListeners.all().onRenderGui(this.engineClient.getCurrentScreen());
		}
	}
	
	/**
	 * Called immediately after the world/camera transform is initialised
	 */
	void onSetupCameraTransform()
	{
		this.renderListeners.all().onSetupCameraTransform();
	}
	
	/**
	 * Called immediately before the chat log is rendered
	 */
	void onRenderChat()
	{
		GuiNewChat chat = this.engineClient.getChatGUI();
		this.chatRenderListeners.all().onPreRenderChat(this.screenWidth, this.screenHeight, chat);
	}
	
	/**
	 * Called immediately after the chat log is rendered
	 */
	void postRenderChat()
	{
		GuiNewChat chat = this.engineClient.getChatGUI();
		this.chatRenderListeners.all().onPostRenderChat(this.screenWidth, this.screenHeight, chat);
	}
	
	/**
	 * Callback when about to render the HUD
	 */
	void onRenderHUD()
	{
		if (!this.engineClient.hideGUI() || this.engineClient.getCurrentScreen() != null)
		{
			this.hudRenderListeners.all().onPreRenderHUD(this.screenWidth, this.screenHeight);
		}
	}
	
	/**
	 * Callback when the HUD has just been rendered
	 */
	void postRenderHUD()
	{
		if (!this.engineClient.hideGUI() || this.engineClient.getCurrentScreen() != null)
		{
			this.hudRenderListeners.all().onPostRenderHUD(this.screenWidth, this.screenHeight);
		}
	}
	
	/**
	 * Callback from the tick hook, called every frame when the timer is updated
	 */
	void onTimerUpdate()
	{
		Minecraft minecraft = this.engine.getClient();
		this.loopListeners.all().onRunGameLoop(minecraft);
	}
	
	/**
	 * Callback from the tick hook, ticks all tickable mods
	 * 
	 * @param clock True if this is a new tick (otherwise it's just a new frame)
	 */
	void onTick(boolean clock)
	{
		this.profiler.startSection("litemods");
		float partialTicks = 0.0F;
		
		// Try to get the minecraft timer object and determine the value of the
		// partialTicks
		if (clock || this.minecraftTimer == null)
		{
			this.minecraftTimer = ((IMinecraft)this.engine.getClient()).getTimer();
		}
		
		// Hooray, we got the timer reference
		if (this.minecraftTimer != null)
		{
			partialTicks = this.minecraftTimer.renderPartialTicks;
			clock = this.minecraftTimer.elapsedTicks > 0;
		}
		
		Minecraft minecraft = this.engine.getClient();
		
		// Flag indicates whether we are in game at the moment
		boolean inGame = minecraft.renderViewEntity != null && minecraft.renderViewEntity.worldObj != null;
		
		this.profiler.startSection("loader");
		super.onTick(clock, partialTicks, inGame);

		int mouseX = Mouse.getX() * this.screenWidth / minecraft.displayWidth;
		int mouseY = this.screenHeight - Mouse.getY() * this.screenHeight / minecraft.displayHeight - 1;
		this.profiler.endStartSection("postrender");
		super.onPostRender(mouseX, mouseY, partialTicks);
		this.profiler.endSection();
		
		// Iterate tickable mods
		this.tickListeners.all().onTick(minecraft, partialTicks, inGame, clock);
		
		// Detected world change
		if (minecraft.theWorld != null)
		{
			if (minecraft.theWorld.hashCode() != this.worldHashCode)
			{
				this.worldHashCode = minecraft.theWorld.hashCode();
				super.onWorldChanged(minecraft.theWorld);
			}
		}
		else
		{
			this.worldHashCode = 0;
			super.onWorldChanged(null);
		}
		
		this.profiler.endSection();
	}
	
	/**
	 * Callback from the chat hook
	 * 
	 * @param chatPacket
	 * @return
	 */
	boolean onChat(S02PacketChat chatPacket)
	{
		if (chatPacket.func_148915_c() == null)
			return true;
		
		IChatComponent chat = chatPacket.func_148915_c();
		String message = chat.getFormattedText();
		
		// Chat filters get a stab at the chat first, if any filter returns
		// false the chat is discarded
		for (ChatFilter chatFilter : this.chatFilters)
		{
			if (chatFilter.onChat(chatPacket, chat, message))
			{
				chat = chatPacket.func_148915_c();
				message = chat.getFormattedText();
			}
			else
			{
				return false;
			}
		}
		
		// Chat listeners get the chat if no filter removed it
		this.chatListeners.all().onChat(chat, message);
		
		return true;
	}

	/**
	 * @param packet
	 * @param message
	 */
	void onSendChatMessage(C01PacketChatMessage packet, String message)
	{
		this.outboundChatListeners.all().onSendChatMessage(packet, message);
	}
	
	/**
	 * @param message
	 */
	void onSendChatMessage(EventInfo<EntityClientPlayerMP> e, String message)
	{
		for (OutboundChatFilter outboundChatFilter : this.outboundChatFilters)
		{
			if (!outboundChatFilter.onSendChatMessage(message))
				e.cancel();
		}
	}

	/**
	 * @param netHandler
	 * @param loginPacket
	 */
	void onPostLogin(INetHandlerLoginClient netHandler, S02PacketLoginSuccess loginPacket)
	{
		ClientPluginChannels clientPluginChannels = LiteLoader.getClientPluginChannels();
		if (clientPluginChannels instanceof ClientPluginChannelsClient)
		{
			((ClientPluginChannelsClient)clientPluginChannels).onPostLogin(netHandler, loginPacket);
		}

		this.postLoginListeners.all().onPostLogin(netHandler, loginPacket);
	}
	
	/**
	 * Pre join game callback from the login hook
	 * 
	 * @param netHandler
	 * @param hookLogin
	 * @return
	 */
	boolean onPreJoinGame(INetHandler netHandler, S01PacketJoinGame loginPacket)
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
	@Override
	protected void onJoinGame(INetHandler netHandler, S01PacketJoinGame loginPacket)
	{
		super.onJoinGame(netHandler, loginPacket);
		
		ClientPluginChannels clientPluginChannels = LiteLoader.getClientPluginChannels();
		if (clientPluginChannels instanceof ClientPluginChannelsClient)
		{
			((ClientPluginChannelsClient)clientPluginChannels).onJoinGame(netHandler, loginPacket);
		}
		
		this.joinGameListeners.all().onJoinGame(netHandler, loginPacket);
	}

	/**
	 * @param framebuffer
	 */
	void preRenderFBO(Framebuffer framebuffer)
	{
		this.frameBufferListeners.all().preRenderFBO(framebuffer);
	}

	/**
	 * @param framebuffer
	 * @param width
	 * @param height
	 */
	void onRenderFBO(Framebuffer framebuffer, int width, int height)
	{
		this.frameBufferListeners.all().onRenderFBO(framebuffer, width, height);
	}

	/**
	 * @param framebuffer
	 */
	void postRenderFBO(Framebuffer framebuffer)
	{
		this.frameBufferListeners.all().postRenderFBO(framebuffer);
	}

	/**
	 * @param partialTicks
	 * @param timeSlice
	 */
	public void onRenderWorld(float partialTicks, long timeSlice)
	{
		this.renderListeners.all().onRenderWorld();
	}
}
