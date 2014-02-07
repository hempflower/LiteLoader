package com.mumfrey.liteloader.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerLoginClient;
import net.minecraft.network.INetHandler;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.login.INetHandlerLoginClient;
import net.minecraft.network.login.server.S02PacketLoginSuccess;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S3FPacketCustomPayload;

import com.mumfrey.liteloader.LiteMod;
import com.mumfrey.liteloader.PluginChannelListener;
import com.mumfrey.liteloader.core.exceptions.UnregisteredChannelException;
import com.mumfrey.liteloader.permissions.PermissionsManagerClient;
import com.mumfrey.liteloader.util.PrivateFields;
import com.mumfrey.liteloader.util.log.LiteLoaderLogger;

/**
 * Handler for client plugin channels
 * 
 * @author Adam Mummery-Smith
 */
public class ClientPluginChannels extends PluginChannels<PluginChannelListener>
{
	private static ClientPluginChannels instance;
	
	ClientPluginChannels()
	{
		super();
		
		ClientPluginChannels.instance = this;
	}
	
	static ClientPluginChannels getInstance()
	{
		return instance;
	}

	/**
	 * @param listener
	 */
	void addListener(LiteMod listener)
	{
		if (listener instanceof PluginChannelListener)
		{
			this.addPluginChannelListener((PluginChannelListener)listener);
		}
	}

	/**
	 * @param netHandler
	 * @param loginPacket
	 */
	void onPostLogin(INetHandlerLoginClient netHandler, S02PacketLoginSuccess loginPacket)
	{
		this.clearPluginChannels(netHandler);
	}

	/**
	 * @param netHandler
	 * @param loginPacket
	 */
	void onJoinGame(INetHandler netHandler, S01PacketJoinGame loginPacket)
	{
		this.sendRegisteredPluginChannels(netHandler);
	}
	
	/**
	 * Callback for the plugin channel hook
	 * 
	 * @param customPayload
	 */
	public void onPluginChannelMessage(S3FPacketCustomPayload customPayload)
	{
		if (customPayload != null && customPayload.func_149169_c() != null) // getChannel
		{
			String channel = customPayload.func_149169_c(); // getChannel
			byte[] data = customPayload.func_149168_d(); // getData
			
			if (PluginChannels.CHANNEL_REGISTER.equals(channel))
			{
				this.onRegisterPacketReceived(data);
			}
			else if (this.pluginChannels.containsKey(channel))
			{
				try
				{
					PermissionsManagerClient permissionsManager = LiteLoader.getPermissionsManager();
					if (permissionsManager != null)
					{
						permissionsManager.onCustomPayload(channel, data.length, data);
					}
				}
				catch (Exception ex) {}
				
				this.onModPacketReceived(channel, data, data.length);
			}
		}
	}

	/**
	 * @param channel
	 * @param data
	 * @param length
	 */
	protected void onModPacketReceived(String channel, byte[] data, int length)
	{
		for (PluginChannelListener pluginChannelListener : this.pluginChannels.get(channel))
		{
			try
			{
				pluginChannelListener.onCustomPayload(channel, length, data);
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
	
	protected void sendRegisteredPluginChannels(INetHandler netHandler)
	{
		// Add the permissions manager channels
		this.addPluginChannelsFor(LiteLoader.getPermissionsManager());
		
		try
		{
			byte[] registrationData = this.getRegistrationData();
			if (registrationData != null)
			{
				this.sendRegistrationData(netHandler, registrationData);
			}
		}
		catch (Exception ex)
		{
			LiteLoaderLogger.warning(ex, "Error dispatching REGISTER packet to server %s", ex.getClass().getSimpleName());
		}
	}

	/**
	 * @param netHandler
	 * @param registrationData
	 */
	protected void sendRegistrationData(INetHandler netHandler, byte[] registrationData)
	{
		if (netHandler instanceof INetHandlerLoginClient)
		{
			NetworkManager networkManager = PrivateFields.netManager.get(((NetHandlerLoginClient)netHandler));
			networkManager.scheduleOutboundPacket(new C17PacketCustomPayload(CHANNEL_REGISTER, registrationData));
		}
		else if (netHandler instanceof INetHandlerPlayClient)
		{
			ClientPluginChannels.dispatch(new C17PacketCustomPayload(CHANNEL_REGISTER, registrationData));
		}
	}

	/**
	 * @param channel
	 * @param data
	 */
	static boolean dispatch(C17PacketCustomPayload payload)
	{
		try
		{
			Minecraft minecraft = Minecraft.getMinecraft();
			
			if (minecraft.thePlayer != null && minecraft.thePlayer.sendQueue != null)
			{
				minecraft.thePlayer.sendQueue.addToSendQueue(payload);
				return true;
			}
		}
		catch (Exception ex) {}
		
		return false;
	}
	
	/**
	 * Send a message to the server on a plugin channel
	 * 
	 * @param channel Channel to send, must not be a reserved channel name
	 * @param data
	 */
	public static boolean sendMessage(String channel, byte[] data, ChannelPolicy policy)
	{
		if (!policy.allows(ClientPluginChannels.getInstance(), channel))
		{
			if (policy.isSilent()) return false;
			throw new UnregisteredChannelException(channel);
		}
		
		if (channel == null || channel.length() > 16 || CHANNEL_REGISTER.equals(channel) || CHANNEL_UNREGISTER.equals(channel))
			throw new RuntimeException("Invalid channel name specified"); 
		
		C17PacketCustomPayload payload = new C17PacketCustomPayload(channel, data);
		return ClientPluginChannels.dispatch(payload);
	}
}
