package com.mumfrey.liteloader.server.api;

import java.util.List;

import net.minecraft.server.MinecraftServer;

import com.google.common.collect.ImmutableList;
import com.mumfrey.liteloader.api.CoreProvider;
import com.mumfrey.liteloader.api.CustomisationProvider;
import com.mumfrey.liteloader.api.InterfaceProvider;
import com.mumfrey.liteloader.api.Observer;
import com.mumfrey.liteloader.core.api.LiteLoaderCoreAPI;
import com.mumfrey.liteloader.interfaces.ObjectFactory;
import com.mumfrey.liteloader.server.DummyClient;
import com.mumfrey.liteloader.server.LiteLoaderCoreProviderServer;

/**
 *
 * @author Adam Mummery-Smith
 */
public class LiteLoaderCoreAPIServer extends LiteLoaderCoreAPI
{
	private static final String PKG_LITELOADER_SERVER = LiteLoaderCoreAPI.PKG_LITELOADER + ".server";

	private static final String[] requiredTransformers = {
		LiteLoaderCoreAPI.PKG_LITELOADER + ".launch.LiteLoaderTransformer"
	};
	
	private static final String[] requiredDownstreamTransformers = {
		LiteLoaderCoreAPIServer.PKG_LITELOADER_SERVER + ".transformers.LiteLoaderCallbackInjectionTransformer"
	};
	
	private static final String[] defaultPacketTransformers = {
		LiteLoaderCoreAPIServer.PKG_LITELOADER_SERVER + ".transformers.ServerChatPacketTransformer",
		LiteLoaderCoreAPIServer.PKG_LITELOADER_SERVER + ".transformers.ServerCustomPayloadPacketTransformer"
	};
	
	private ObjectFactory<DummyClient, MinecraftServer> objectFactory;
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.api.LiteAPI#getRequiredTransformers()
	 */
	@Override
	public String[] getRequiredTransformers()
	{
		return LiteLoaderCoreAPIServer.requiredTransformers;
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.api.LiteAPI#getRequiredDownstreamTransformers()
	 */
	@Override
	public String[] getRequiredDownstreamTransformers()
	{
		return LiteLoaderCoreAPIServer.requiredDownstreamTransformers;
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.api.LiteAPI#getPacketTransformers()
	 */
	@Override
	public String[] getPacketTransformers()
	{
		return LiteLoaderCoreAPIServer.defaultPacketTransformers;
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.api.LiteAPI#getCustomisationProviders()
	 */
	@Override
	public List<CustomisationProvider> getCustomisationProviders()
	{
		return null;
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.api.LiteAPI#getCoreProviders()
	 */
	@Override
	public List<CoreProvider> getCoreProviders()
	{
		return ImmutableList.<CoreProvider>of
		(
			new LiteLoaderCoreProviderServer(this.properties)
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
			objectFactory.getServerPluginChannels()
		);
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.api.LiteAPI#getObservers()
	 */
	@Override
	public List<Observer> getObservers()
	{
		return null;
	}
	
	@Override
	public ObjectFactory<?, ?> getObjectFactory()
	{
		if (this.objectFactory == null)
		{
			this.objectFactory = new ObjectFactoryServer(this.environment, this.properties);
		}	
		
		return this.objectFactory;
	}
}
