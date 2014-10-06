package com.mumfrey.liteloader.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.Timer;

import org.lwjgl.input.Mouse;

import com.mumfrey.liteloader.ChatRenderListener;
import com.mumfrey.liteloader.FrameBufferListener;
import com.mumfrey.liteloader.GameLoopListener;
import com.mumfrey.liteloader.HUDRenderListener;
import com.mumfrey.liteloader.InitCompleteListener;
import com.mumfrey.liteloader.OutboundChatFilter;
import com.mumfrey.liteloader.OutboundChatListener;
import com.mumfrey.liteloader.PostRenderListener;
import com.mumfrey.liteloader.RenderListener;
import com.mumfrey.liteloader.Tickable;
import com.mumfrey.liteloader.ViewportListener;
import com.mumfrey.liteloader.client.overlays.IMinecraft;
import com.mumfrey.liteloader.common.LoadingProgress;
import com.mumfrey.liteloader.core.Events;
import com.mumfrey.liteloader.core.InterfaceRegistrationDelegate;
import com.mumfrey.liteloader.core.LiteLoader;
import com.mumfrey.liteloader.core.event.HandlerList;
import com.mumfrey.liteloader.core.event.HandlerList.ReturnLogicOp;
import com.mumfrey.liteloader.interfaces.FastIterableDeque;
import com.mumfrey.liteloader.launch.LoaderProperties;
import com.mumfrey.liteloader.transformers.event.EventInfo;
import com.mumfrey.liteloader.util.log.LiteLoaderLogger;

public class EventsClient extends Events<Minecraft, IntegratedServer>
{	
	private static EventsClient instance;

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
	private boolean lateInitDone;

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

	private FastIterableDeque<Tickable>             tickListeners         = new HandlerList<Tickable>(Tickable.class);
	private FastIterableDeque<GameLoopListener>     loopListeners         = new HandlerList<GameLoopListener>(GameLoopListener.class);
	private FastIterableDeque<RenderListener>       renderListeners       = new HandlerList<RenderListener>(RenderListener.class);
	private FastIterableDeque<PostRenderListener>   postRenderListeners   = new HandlerList<PostRenderListener>(PostRenderListener.class);
	private FastIterableDeque<HUDRenderListener>    hudRenderListeners    = new HandlerList<HUDRenderListener>(HUDRenderListener.class);
	private FastIterableDeque<ChatRenderListener>   chatRenderListeners   = new HandlerList<ChatRenderListener>(ChatRenderListener.class);
	private FastIterableDeque<OutboundChatListener> outboundChatListeners = new HandlerList<OutboundChatListener>(OutboundChatListener.class);
	private FastIterableDeque<ViewportListener>     viewportListeners     = new HandlerList<ViewportListener>(ViewportListener.class);
	private FastIterableDeque<FrameBufferListener>  frameBufferListeners  = new HandlerList<FrameBufferListener>(FrameBufferListener.class);
	private FastIterableDeque<InitCompleteListener> initListeners         = new HandlerList<InitCompleteListener>(InitCompleteListener.class);
	private FastIterableDeque<OutboundChatFilter>   outboundChatFilters   = new HandlerList<OutboundChatFilter>(OutboundChatFilter.class, ReturnLogicOp.AND);

	@SuppressWarnings("cast")
	public EventsClient(LiteLoader loader, GameEngineClient engine, LoaderProperties properties)
	{
		super(loader, engine, properties);
		
		EventsClient.instance = this;
		
		this.engineClient = (GameEngineClient)engine;
	}
	
	static EventsClient getInstance()
	{
		return EventsClient.instance;
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
		delegate.registerInterface(OutboundChatListener.class);
		delegate.registerInterface(ViewportListener.class);
		delegate.registerInterface(FrameBufferListener.class);
		delegate.registerInterface(InitCompleteListener.class);
		delegate.registerInterface(OutboundChatFilter.class);
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.api.InterfaceProvider#initProvider()
	 */
	@Override
	public void initProvider()
	{
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
	 * 
	 * @param timeSlice 
	 * @param partialTicks2 
	 */
	void postRenderEntities(float partialTicks2, long timeSlice)
	{
		float partialTicks = (this.minecraftTimer != null) ? this.minecraftTimer.elapsedPartialTicks : 0.0F;
		this.postRenderListeners.all().onPostRenderEntities(partialTicks);
	}
	
	/**
	 * Callback from the tick hook, post render
	 * 
	 * @param timeSlice 
	 * @param partialTicks2 
	 */
	void postRender(float partialTicks2, long timeSlice)
	{
		float partialTicks = (this.minecraftTimer != null) ? this.minecraftTimer.elapsedPartialTicks : 0.0F;
		this.postRenderListeners.all().onPostRender(partialTicks);
	}
	
	/**
	 * Called immediately before the current GUI is rendered
	 */
	void preRenderGUI(float partialTicks)
	{
		this.renderListeners.all().onRenderGui(this.engineClient.getCurrentScreen());
	}
	
	/**
	 * Called immediately after the world/camera transform is initialised
	 * 
	 * @param timeSlice 
	 * @param partialTicks 
	 */
	void onSetupCameraTransform(float partialTicks, long timeSlice)
	{
		this.renderListeners.all().onSetupCameraTransform();
	}
	
	/**
	 * Called immediately before the chat log is rendered
	 * 
	 * @param chatGui 
	 * @param partialTicks 
	 */
	void onRenderChat(GuiNewChat chatGui, float partialTicks)
	{
		this.chatRenderListeners.all().onPreRenderChat(this.screenWidth, this.screenHeight, chatGui);
	}
	
	/**
	 * Called immediately after the chat log is rendered
	 * 
	 * @param chatGui 
	 * @param partialTicks 
	 */
	void postRenderChat(GuiNewChat chatGui, float partialTicks)
	{
		GuiNewChat chat = this.engineClient.getChatGUI();
		this.chatRenderListeners.all().onPostRenderChat(this.screenWidth, this.screenHeight, chat);
	}
	
	/**
	 * Callback when about to render the HUD
	 */
	void onRenderHUD(float partialTicks)
	{
		this.hudRenderListeners.all().onPreRenderHUD(this.screenWidth, this.screenHeight);
	}
	
	/**
	 * Callback when the HUD has just been rendered
	 */
	void postRenderHUD(float partialTicks)
	{
		this.hudRenderListeners.all().onPostRenderHUD(this.screenWidth, this.screenHeight);
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
		Entity renderViewEntity = minecraft.getRenderViewEntity(); // TODO OBF MCPTEST func_175606_aa - getRenderViewEntity
		boolean inGame = renderViewEntity != null && renderViewEntity.worldObj != null;
		
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
	void onSendChatMessage(EventInfo<EntityPlayerSP> e, String message)
	{
		if (!this.outboundChatFilters.all().onSendChatMessage(message))
		{
			e.cancel();
		}
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
