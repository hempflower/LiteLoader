package com.mumfrey.liteloader.core;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import net.minecraft.src.Minecraft;
import net.minecraft.src.NetHandler;
import net.minecraft.src.Packet1Login;
import net.minecraft.src.Packet250CustomPayload;

import com.mumfrey.liteloader.PluginChannelListener;
import com.mumfrey.liteloader.core.hooks.HookPluginChannels;
import com.mumfrey.liteloader.permissions.PermissionsManagerClient;

/**
 * Manages plugin channel connections and subscriptions for LiteLoader
 *
 * @author Adam Mummery-Smith
 */
public class PluginChannels
{
	// reserved channel consts
	private static final String CHANNEL_REGISTER = "REGISTER";
	private static final String CHANNEL_UNREGISTER = "UNREGISTER";

	/**
	 * True if we have initialised the hook
	 */
	private boolean hookInitDone;
	
	/**
	 * Mapping of plugin channel names to listeners
	 */
	private HashMap<String, LinkedList<PluginChannelListener>> pluginChannels = new HashMap<String, LinkedList<PluginChannelListener>>();
	
	/**
	 * List of mods which implement PluginChannelListener interface
	 */
	private LinkedList<PluginChannelListener> pluginChannelListeners = new LinkedList<PluginChannelListener>();

	/**
	 * Package private
	 */
	PluginChannels() {}

	/**
	 * 
	 */
	public void initHook()
	{
		// Plugin channels hook
		if (this.pluginChannelListeners.size() > 0 && !this.hookInitDone)
		{
			HookPluginChannels.register();
			HookPluginChannels.registerPacketHandler(this);
			this.hookInitDone = true;
		}
	}
	
	
	/**
	 * @param pluginChannelListener
	 */
	public void addPluginChannelListener(PluginChannelListener pluginChannelListener)
	{
		if (!this.pluginChannelListeners.contains(pluginChannelListener))
		{
			this.pluginChannelListeners.add(pluginChannelListener);
			if (this.hookInitDone)
				this.initHook();
		}
	}

	/**
	 * @param netHandler
	 * @param loginPacket
	 */
	public void onConnectToServer(NetHandler netHandler, Packet1Login loginPacket)
	{
		this.setupPluginChannels();
	}

	/**
	 * Callback for the plugin channel hook
	 * 
	 * @param customPayload
	 */
	public void onPluginChannelMessage(Packet250CustomPayload customPayload)
	{
		if (customPayload != null && customPayload.channel != null && this.pluginChannels.containsKey(customPayload.channel))
		{
			try
			{
				PermissionsManagerClient permissionsManager = LiteLoader.getPermissionsManager();
				if (permissionsManager != null)
				{
					permissionsManager.onCustomPayload(customPayload.channel, customPayload.length, customPayload.data);
				}
			}
			catch (Exception ex) {}
			
			for (PluginChannelListener pluginChannelListener : this.pluginChannels.get(customPayload.channel))
			{
				try
				{
					pluginChannelListener.onCustomPayload(customPayload.channel, customPayload.length, customPayload.data);
				}
				catch (Exception ex) {}
			}
		}
	}
	
	/**
	 * Query loaded mods for registered channels
	 */
	protected void setupPluginChannels()
	{
		// Clear any channels from before
		this.pluginChannels.clear();
		
		// Add the permissions manager channels
		this.addPluginChannelsFor(LiteLoader.getPermissionsManager());
		
		// Enumerate mods for plugin channels
		for (PluginChannelListener pluginChannelListener : this.pluginChannelListeners)
		{
			this.addPluginChannelsFor(pluginChannelListener);
		}
		
		// If any mods have registered channels, send the REGISTER packet
		if (this.pluginChannels.keySet().size() > 0)
		{
			StringBuilder channelList = new StringBuilder();
			boolean separator = false;
			
			for (String channel : this.pluginChannels.keySet())
			{
				if (separator) channelList.append("\u0000");
				channelList.append(channel);
				separator = true;
			}
			
			byte[] registrationData = channelList.toString().getBytes(Charset.forName("UTF8"));
			PluginChannels.dispatch(new Packet250CustomPayload(CHANNEL_REGISTER, registrationData));
		}
	}
	
	/**
	 * Adds plugin channels for the specified listener to the local channels
	 * collection
	 * 
	 * @param pluginChannelListener
	 */
	private void addPluginChannelsFor(PluginChannelListener pluginChannelListener)
	{
		List<String> channels = pluginChannelListener.getChannels();
		
		if (channels != null)
		{
			for (String channel : channels)
			{
				if (channel.length() > 16 || channel.toUpperCase().equals(CHANNEL_REGISTER) || channel.toUpperCase().equals(CHANNEL_UNREGISTER))
					continue;
				
				if (!this.pluginChannels.containsKey(channel))
				{
					this.pluginChannels.put(channel, new LinkedList<PluginChannelListener>());
				}
				
				this.pluginChannels.get(channel).add(pluginChannelListener);
			}
		}
	}
	
	/**
	 * Send a message on a plugin channel
	 * 
	 * @param channel Channel to send, must not be a reserved channel name
	 * @param data
	 */
	public static void sendMessage(String channel, byte[] data)
	{
		if (channel == null || channel.length() > 16 || CHANNEL_REGISTER.equals(channel) || CHANNEL_UNREGISTER.equals(channel))
			throw new RuntimeException("Invalid channel name specified"); 
		
		Packet250CustomPayload payload = new Packet250CustomPayload(channel, data);
		PluginChannels.dispatch(payload);
	}

	/**
	 * @param channel
	 * @param data
	 */
	private static void dispatch(Packet250CustomPayload payload)
	{
		try
		{
			Minecraft minecraft = Minecraft.getMinecraft();
			
			if (minecraft.thePlayer != null && minecraft.thePlayer.sendQueue != null)
				minecraft.thePlayer.sendQueue.addToSendQueue(payload);
		}
		catch (Exception ex) {}
	}
}
