package com.mumfrey.liteloader.core.hooks;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;

import net.minecraft.src.IntHashMap;
import net.minecraft.src.NetHandler;
import net.minecraft.src.Packet;
import net.minecraft.src.Packet250CustomPayload;

import com.mumfrey.liteloader.core.PluginChannels;
import com.mumfrey.liteloader.util.PrivateFields;

public class HookPluginChannels extends Packet250CustomPayload
{
	/**
	 * True if this class was registered with the base class
	 */
	private static boolean registered = false;
	
	/**
	 * Handler module which is registered to handle inbound chat packets
	 */
	private static PluginChannels events; 
	
	/**
	 * Class which was overridden and will be instanced for new packets
	 */
	private static Class<? extends Packet> proxyClass;
	
	/**
	 * Instance of the proxy packet for this packet instance
	 */
	private Packet proxyPacket;
	
	public HookPluginChannels()
	{
		super();
		
		try
		{
			if (HookPluginChannels.proxyClass != null)
			{
				this.proxyPacket = HookPluginChannels.proxyClass.newInstance();
			}
		}
		catch (Exception ex) {}
	}
	
	public HookPluginChannels(String channel, byte[] data)
	{
		super(channel, data);

		try
		{
			if (HookPluginChannels.proxyClass != null)
			{
				this.proxyPacket = HookPluginChannels.proxyClass.newInstance();
				
				if (this.proxyPacket instanceof Packet250CustomPayload)
				{
					((Packet250CustomPayload)this.proxyPacket).channel = this.channel;
					((Packet250CustomPayload)this.proxyPacket).data = this.data;
					((Packet250CustomPayload)this.proxyPacket).length = this.length;
				}
			}
		}
		catch (Exception ex) {}
	}
	

	@Override
	public void readPacketData(DataInput datainputstream) throws IOException
	{
		if (this.proxyPacket != null)
		{
			this.proxyPacket.readPacketData(datainputstream);
			this.channel = ((Packet250CustomPayload)this.proxyPacket).channel;
			this.length = ((Packet250CustomPayload)this.proxyPacket).length;
			this.data = ((Packet250CustomPayload)this.proxyPacket).data;
		}
		else
			super.readPacketData(datainputstream);
	}

	@Override
	public void writePacketData(DataOutput dataoutputstream) throws IOException
	{
		if (this.proxyPacket != null)
			this.proxyPacket.writePacketData(dataoutputstream);
		else
			super.writePacketData(dataoutputstream);
	}

	@Override
	public void processPacket(NetHandler nethandler)
	{
		if (this.proxyPacket != null)
			this.proxyPacket.processPacket(nethandler);
		else
			super.processPacket(nethandler);

		if (HookPluginChannels.events != null)
		{
			HookPluginChannels.events.onPluginChannelMessage(this);
		}
	}

	@Override
	public int getPacketSize()
	{
		if (this.proxyPacket != null)
			return this.proxyPacket.getPacketSize();
		
		return super.getPacketSize();
	}
	
	/**
	 * Register the specified handler as the packet handler for this packet
	 * @param handler
	 */
	public static void registerPacketHandler(PluginChannels handler)
	{
		HookPluginChannels.events = handler;
	}
	
	/**
	 * Register this packet as the new packet for packet ID 250
	 */
	public static void register()
	{
		register(false);
	}
	
	/**
	 * Register this packet as the new packet for packet ID 250 and optionally force re-registration even
	 * if registration was performed already.
	 * 
	 * @param force Force registration even if registration was already performed previously.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void register(boolean force)
	{
		if (!HookPluginChannels.registered || force)
		{
			try
			{
				int packetId = 250;
				IntHashMap packetIdToClassMap = Packet.packetIdToClassMap;
				HookPluginChannels.proxyClass = (Class<? extends Packet>)packetIdToClassMap.lookup(packetId);
				
				if (HookPluginChannels.proxyClass.equals(Packet250CustomPayload.class))
				{
					HookPluginChannels.proxyClass = null;
				}
				
				packetIdToClassMap.removeObject(packetId);
				packetIdToClassMap.addKey(packetId, HookPluginChannels.class);
				
				Map packetClassToIdMap = PrivateFields.StaticFields.packetClassToIdMap.get();
				packetClassToIdMap.put(HookPluginChannels.class, Integer.valueOf(packetId));
				
				HookPluginChannels.registered = true;
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
	}
}
