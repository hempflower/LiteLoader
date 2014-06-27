package com.mumfrey.liteloader.core;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.INetHandlerPlayServer;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.network.play.server.S3FPacketCustomPayload;

import com.mumfrey.liteloader.ServerPluginChannelListener;
import com.mumfrey.liteloader.api.Listener;
import com.mumfrey.liteloader.core.exceptions.UnregisteredChannelException;
import com.mumfrey.liteloader.permissions.PermissionsManagerServer;
import com.mumfrey.liteloader.util.log.LiteLoaderLogger;

/**
 * Handler for server plugin channels
 *
 * @author Adam Mummery-Smith
 */
public class ServerPluginChannels extends PluginChannels<ServerPluginChannelListener>
{
	private static ServerPluginChannels instance;
	
	public ServerPluginChannels()
	{
		if (ServerPluginChannels.instance != null) throw new RuntimeException("Plugin Channels Startup Error", new InstantiationException("Only a single instance of ServerPluginChannels is allowed"));
		
		ServerPluginChannels.instance = this;
	}
	
	public static ServerPluginChannels getInstance()
	{
		return instance;
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.api.InterfaceProvider#initProvider()
	 */
	@Override
	public void initProvider()
	{
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.api.InterfaceProvider#getListenerBaseType()
	 */
	@Override
	public Class<? extends Listener> getListenerBaseType()
	{
		return Listener.class;
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.api.InterfaceProvider#registerInterfaces(com.mumfrey.liteloader.core.InterfaceRegistrationDelegate)
	 */
	@Override
	public void registerInterfaces(InterfaceRegistrationDelegate delegate)
	{
		delegate.registerInterface(ServerPluginChannelListener.class);
	}
	
	void addServerPluginChannelListener(ServerPluginChannelListener pluginChannelListener)
	{
		super.addPluginChannelListener(pluginChannelListener);
	}

	/**
	 * @param netHandler
	 * @param loginPacket
	 */
	void onServerStartup()
	{
		this.clearPluginChannels(null);
	}

	/**
	 * @param netHandler
	 * @param loginPacket
	 */
	void onPlayerJoined(EntityPlayerMP player)
	{
		this.sendRegisteredPluginChannels(player);
	}
	
	/**
	 * Callback for the plugin channel hook
	 * @param netHandler 
	 * 
	 * @param customPayload
	 */
	public void onPluginChannelMessage(INetHandlerPlayServer netHandler, C17PacketCustomPayload customPayload)
	{
		if (customPayload != null && customPayload.func_149559_c() != null)
		{
			String channel = customPayload.func_149559_c();
			byte[] data = customPayload.func_149558_e();
			
			EntityPlayerMP sender = ((NetHandlerPlayServer)netHandler).playerEntity;
			this.onPluginChannelMessage(sender, channel, data);
		}
	}

	/**
	 * @param channel
	 * @param data
	 */
	private final void onPluginChannelMessage(EntityPlayerMP sender, String channel, byte[] data)
	{
		if (PluginChannels.CHANNEL_REGISTER.equals(channel))
		{
			this.onRegisterPacketReceived(data);
		}
		else if (this.pluginChannels.containsKey(channel))
		{
			try
			{
				PermissionsManagerServer permissionsManager = LiteLoader.getServerPermissionsManager();
				if (permissionsManager != null)
				{
					permissionsManager.onCustomPayload(sender, channel, data.length, data);
				}
			}
			catch (Exception ex) {}
			
			this.onModPacketReceived(sender, channel, data, data.length);
		}
	}

	/**
	 * @param channel
	 * @param data
	 * @param length
	 */
	protected void onModPacketReceived(EntityPlayerMP sender, String channel, byte[] data, int length)
	{
		for (ServerPluginChannelListener pluginChannelListener : this.pluginChannels.get(channel))
		{
			try
			{
				pluginChannelListener.onCustomPayload(sender, channel, length, data);
				throw new RuntimeException();
			}
			catch (Exception ex)
			{
				int failCount = 1;
				if (this.faultingPluginChannelListeners.containsKey(pluginChannelListener))
					failCount = this.faultingPluginChannelListeners.get(pluginChannelListener).intValue() + 1;
				
				if (failCount >= PluginChannels.WARN_FAULT_THRESHOLD)
				{
					LiteLoaderLogger.warning("Plugin channel listener %s exceeded fault threshold on channel %s with %s", pluginChannelListener.getName(), channel, ex.getClass().getSimpleName());
					this.faultingPluginChannelListeners.remove(pluginChannelListener);
				}
				else
				{
					this.faultingPluginChannelListeners.put(pluginChannelListener, Integer.valueOf(failCount));
				}
			}
		}
	}

	protected void sendRegisteredPluginChannels(EntityPlayerMP player)
	{
		try
		{
			byte[] registrationData = this.getRegistrationData();
			if (registrationData != null)
			{
				this.sendRegistrationData(player, registrationData);
			}
		}
		catch (Exception ex)
		{
			LiteLoaderLogger.warning(ex, "Error dispatching REGISTER packet to client %s", player.getCommandSenderName());
		}
	}

	/**
	 * @param netHandler
	 * @param registrationData
	 */
	private void sendRegistrationData(EntityPlayerMP recipient, byte[] registrationData)
	{
		ServerPluginChannels.dispatch(recipient, new S3FPacketCustomPayload(CHANNEL_REGISTER, registrationData));
	}

	/**
	 * Send a message to the specified client on a plugin channel
	 * 
	 * @param recipient
	 * @param channel Channel to send, must not be a reserved channel name
	 * @param data
	 */
	public static boolean sendMessage(EntityPlayerMP recipient, String channel, byte[] data, ChannelPolicy policy)
	{
		if (ServerPluginChannels.instance != null)
		{
			return ServerPluginChannels.instance.send(recipient, channel, data, policy);
		}
		
		return false;
	}
	
	/**
	 * Send a message to the specified client on a plugin channel
	 * 
	 * @param recipient Recipient to send to
	 * @param channel Channel to send, must not be a reserved channel name
	 * @param data
	 */
	private boolean send(EntityPlayerMP recipient, String channel, byte[] data, ChannelPolicy policy)
	{
		if (recipient == null) return false;
		
		if (channel == null || channel.length() > 16 || CHANNEL_REGISTER.equals(channel) || CHANNEL_UNREGISTER.equals(channel))
			throw new RuntimeException("Invalid channel name specified"); 
		
		if (!policy.allows(this, channel))
		{
			if (policy.isSilent()) return false;
			throw new UnregisteredChannelException(channel);
		}
		
		S3FPacketCustomPayload payload = new S3FPacketCustomPayload(channel, data);
		return ServerPluginChannels.dispatch(recipient, payload);
	}
	
	/**
	 * @param channel
	 * @param data
	 */
	static boolean dispatch(EntityPlayerMP recipient, S3FPacketCustomPayload payload)
	{
		try
		{
			if (recipient != null && recipient.playerNetServerHandler != null)
			{
				recipient.playerNetServerHandler.sendPacket(payload);
				return true;
			}
		}
		catch (Exception ex) {}
		
		return false;
	}
}
