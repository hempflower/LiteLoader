package com.mumfrey.liteloader.client.api;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.server.integrated.IntegratedServer;

import com.google.common.collect.ImmutableList;
import com.mumfrey.liteloader.api.CoreProvider;
import com.mumfrey.liteloader.api.CustomisationProvider;
import com.mumfrey.liteloader.api.InterfaceProvider;
import com.mumfrey.liteloader.api.Observer;
import com.mumfrey.liteloader.client.LiteLoaderCoreProviderClient;
import com.mumfrey.liteloader.core.LiteLoader;
import com.mumfrey.liteloader.core.api.LiteLoaderCoreAPI;
import com.mumfrey.liteloader.interfaces.ObjectFactory;
import com.mumfrey.liteloader.messaging.MessageBus;

/**
 * Client side of the core API
 *
 * @author Adam Mummery-Smith
 */
public class LiteLoaderCoreAPIClient extends LiteLoaderCoreAPI
{
	private static final String PKG_LITELOADER_CLIENT = LiteLoaderCoreAPI.PKG_LITELOADER + ".client";

	private static final String[] requiredTransformers = {
		LiteLoaderCoreAPI.PKG_LITELOADER + ".transformers.event.EventProxyTransformer",
		LiteLoaderCoreAPI.PKG_LITELOADER + ".launch.LiteLoaderTransformer",
		LiteLoaderCoreAPIClient.PKG_LITELOADER_CLIENT + ".transformers.CrashReportTransformer"
	};
	
	private static final String[] requiredDownstreamTransformers = {
		LiteLoaderCoreAPIClient.PKG_LITELOADER_CLIENT + ".transformers.LiteLoaderEventInjectionTransformer",
		LiteLoaderCoreAPIClient.PKG_LITELOADER_CLIENT + ".transformers.MinecraftOverlayTransformer"
	};
	
	private static final String[] defaultPacketTransformers = {
		LiteLoaderCoreAPIClient.PKG_LITELOADER_CLIENT + ".transformers.LoginSuccessPacketTransformer",
		LiteLoaderCoreAPIClient.PKG_LITELOADER_CLIENT + ".transformers.ChatPacketTransformer",
		LiteLoaderCoreAPIClient.PKG_LITELOADER_CLIENT + ".transformers.JoinGamePacketTransformer",
		LiteLoaderCoreAPIClient.PKG_LITELOADER_CLIENT + ".transformers.CustomPayloadPacketTransformer",
		LiteLoaderCoreAPIClient.PKG_LITELOADER_CLIENT + ".transformers.ServerChatPacketTransformer",
		LiteLoaderCoreAPIClient.PKG_LITELOADER_CLIENT + ".transformers.ServerCustomPayloadPacketTransformer"
	};
	
	private ObjectFactory<Minecraft, IntegratedServer> objectFactory;
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.api.LiteAPI#getRequiredTransformers()
	 */
	@Override
	public String[] getRequiredTransformers()
	{
		return LiteLoaderCoreAPIClient.requiredTransformers;
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.api.LiteAPI#getRequiredDownstreamTransformers()
	 */
	@Override
	public String[] getRequiredDownstreamTransformers()
	{
		return LiteLoaderCoreAPIClient.requiredDownstreamTransformers;
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.api.LiteAPI#getPacketTransformers()
	 */
	@Override
	public String[] getPacketTransformers()
	{
		return LiteLoaderCoreAPIClient.defaultPacketTransformers;
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.api.LiteAPI#getCustomisationProviders()
	 */
	@Override
	public List<CustomisationProvider> getCustomisationProviders()
	{
		return ImmutableList.<CustomisationProvider>of
		(
			new LiteLoaderBrandingProvider(),
			new LiteLoaderModInfoDecorator()
		);
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.api.LiteAPI#getCoreProviders()
	 */
	@Override
	public List<CoreProvider> getCoreProviders()
	{
		return ImmutableList.<CoreProvider>of
		(
			new LiteLoaderCoreProviderClient(this.properties),
			LiteLoader.getInput()
		);
	}


	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.api.LiteAPI#getInterfaceProviders()
	 */
	@Override
	public List<InterfaceProvider> getInterfaceProviders()
	{
		ObjectFactory<?, ?> objectFactory = this.getObjectFactory();
		
		return ImmutableList.<InterfaceProvider>of
		(
			objectFactory.getEventBroker(),
			objectFactory.getClientPluginChannels(),
			objectFactory.getServerPluginChannels(),
			MessageBus.getInstance()
		);
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.api.LiteAPI#getObservers()
	 */
	@Override
	public List<Observer> getObservers()
	{
		return ImmutableList.<Observer>of
		(
			this.getObjectFactory().getPanelManager()
		);
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.core.api.LiteLoaderCoreAPI#getObjectFactory()
	 */
	@Override
	public ObjectFactory<?, ?> getObjectFactory()
	{
		if (this.objectFactory == null)
		{
			this.objectFactory = new ObjectFactoryClient(this.environment, this.properties);
		}	
		
		return this.objectFactory;
	}
}
