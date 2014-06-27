package com.mumfrey.liteloader.server;

import net.minecraft.server.MinecraftServer;

import com.mumfrey.liteloader.common.GameEngine;
import com.mumfrey.liteloader.core.Events;
import com.mumfrey.liteloader.core.InterfaceRegistrationDelegate;
import com.mumfrey.liteloader.core.LiteLoader;
import com.mumfrey.liteloader.launch.LoaderProperties;

public class ServerEvents extends Events<DummyClient, MinecraftServer>
{
	private static ServerEvents instance;
	
	private boolean lateInitDone;

	public ServerEvents(LiteLoader loader, GameEngine<DummyClient, MinecraftServer> engine, LoaderProperties properties)
	{
		super(loader, engine, properties);
		
		ServerEvents.instance = this;
	}

	public static ServerEvents getInstance()
	{
		return ServerEvents.instance;
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.api.InterfaceProvider#registerInterfaces(com.mumfrey.liteloader.core.InterfaceRegistrationDelegate)
	 */
	@Override
	public void registerInterfaces(InterfaceRegistrationDelegate delegate)
	{
//		delegate.registerInterface(Tickable.class);
//		delegate.registerInterface(GameLoopListener.class);
//		delegate.registerInterface(InitCompleteListener.class);
//		delegate.registerInterface(RenderListener.class);
//		delegate.registerInterface(PostRenderListener.class);
//		delegate.registerInterface(ChatFilter.class);
//		delegate.registerInterface(ChatListener.class);
//		delegate.registerInterface(ChatRenderListener.class);
//		delegate.registerInterface(HUDRenderListener.class);
//		delegate.registerInterface(PreJoinGameListener.class);
//		delegate.registerInterface(JoinGameListener.class);
//		delegate.registerInterface(OutboundChatListener.class);
	}

	@Override
	public void initProvider()
	{
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
			
//			for (InitCompleteListener initMod : this.initListeners)
//			{
//				try
//				{
//					LoadingProgress.setMessage("Calling late init for mod %s...", initMod.getName());
//					LiteLoaderLogger.info("Calling late init for mod %s", initMod.getName());
//					initMod.onInitCompleted(this.engine.getClient(), this.loader);
//				}
//				catch (Throwable th)
//				{
//					LiteLoaderLogger.warning(th, "Error initialising mod %s", initMod.getName());
//				}
//			}
		}

		super.onStartupComplete();
	}
}
