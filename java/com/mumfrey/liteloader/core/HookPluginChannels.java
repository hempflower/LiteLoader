package com.mumfrey.liteloader.core;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

import com.mumfrey.liteloader.util.PrivateFields;

import net.minecraft.src.*;

public class HookPluginChannels extends Packet250CustomPayload
{
	/**
	 * True if this class was registered with the base class
	 */
	private static boolean registered = false;
	
	/**
	 * Handler module which is registered to handle inbound chat packets
	 */
	private static LiteLoader packetHandler; 
	
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
			if (proxyClass != null)
			{
				proxyPacket = proxyClass.newInstance();
			}
		}
		catch (Exception ex) {}
	}
	
	public HookPluginChannels(String channel, byte[] data)
	{
		super(channel, data);

		try
		{
			if (proxyClass != null)
			{
				proxyPacket = proxyClass.newInstance();
				
				if (proxyPacket instanceof Packet250CustomPayload)
				{
					((Packet250CustomPayload)proxyPacket).channel = this.channel;
					((Packet250CustomPayload)proxyPacket).data = this.data;
					((Packet250CustomPayload)proxyPacket).length = this.length;
				}
			}
		}
		catch (Exception ex) {}
	}
	

	@Override
	public void readPacketData(DataInputStream datainputstream) throws IOException
	{
		if (proxyPacket != null)
		{
			proxyPacket.readPacketData(datainputstream);
			this.channel = ((Packet250CustomPayload)proxyPacket).channel;
			this.length = ((Packet250CustomPayload)proxyPacket).length;
			this.data = ((Packet250CustomPayload)proxyPacket).data;
		}
		else
			super.readPacketData(datainputstream);
	}

	@Override
	public void writePacketData(DataOutputStream dataoutputstream) throws IOException
	{
		if (proxyPacket != null)
			proxyPacket.writePacketData(dataoutputstream);
		else
			super.writePacketData(dataoutputstream);
	}

	@Override
	public void processPacket(NetHandler nethandler)
	{
		if (proxyPacket != null)
			proxyPacket.processPacket(nethandler);
		else
			super.processPacket(nethandler);

		if (packetHandler != null)
		{
			packetHandler.onPluginChannelMessage(this);
		}
	}

	@Override
	public int getPacketSize()
	{
		if (proxyPacket != null)
			return proxyPacket.getPacketSize();
		else
			return super.getPacketSize();
	}
	
	/**
	 * Register the specified handler as the packet handler for this packet
	 * @param handler
	 */
	public static void RegisterPacketHandler(LiteLoader handler)
	{
		packetHandler = handler;
	}
	
	/**
	 * Register this packet as the new packet for packet ID 250
	 */
    public static void Register()
    {
    	Register(false);
    }

    /**
     * Register this packet as the new packet for packet ID 250 and optionally force re-registration even
     * if registration was performed already.
     * 
     * @param force Force registration even if registration was already performed previously.
     */
    @SuppressWarnings("unchecked")
	public static void Register(boolean force)
	{
		if (!registered || force)
		{
			try
			{
			    IntHashMap packetIdToClassMap = Packet.packetIdToClassMap;
			    proxyClass = (Class<? extends Packet>)packetIdToClassMap.lookup(250);
			    
			    if (proxyClass.equals(Packet250CustomPayload.class))
			    {
			    	proxyClass = null;
			    }
			    
			    packetIdToClassMap.removeObject(250);
			    packetIdToClassMap.addKey(250, HookPluginChannels.class);

			    Map packetClassToIdMap = PrivateFields.StaticFields.packetClassToIdMap.Get();
			    packetClassToIdMap.put(HookPluginChannels.class, Integer.valueOf(250));
			    
			    registered = true;
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
	}
}
