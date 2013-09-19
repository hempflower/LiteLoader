package com.mumfrey.liteloader.core.hooks;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;

import net.minecraft.src.IntHashMap;
import net.minecraft.src.NetHandler;
import net.minecraft.src.Packet;
import net.minecraft.src.Packet3Chat;

import com.mumfrey.liteloader.core.Events;
import com.mumfrey.liteloader.util.PrivateFields;

/**
 * Proxy packet which we will register in place of the original chat packet. The class will proxy the function calls
 * through to the replaced class via reflection if the original (replaced) class is NOT the basic Packet3Chat (this
 * is to maintain compatibility with things like WorldEditCUI. 
 * 
 * @author Adam Mummery-Smith
 *
 */
public class HookChat extends Packet3Chat
{
	/**
	 * True if this class was registered with the base class
	 */
	private static boolean registered = false;
	
	/**
	 * Handler module which is registered to handle inbound chat packets
	 */
	private static Events events; 
	
	/**
	 * Class which was overridden and will be instanced for new packets
	 */
	private static Class<? extends Packet> proxyClass;
	
	/**
	 * Instance of the proxy packet for this packet instance
	 */
	private Packet proxyPacket;
	
	/**
	 * Create a new chat packet proxy
	 */
	public HookChat()
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
	
	/**
	 * Create a new chat proxy with the specified message
	 * @param message
	 */
	public HookChat(String message)
	{
		super(message);

		try
		{
			if (proxyClass != null)
			{
				proxyPacket = proxyClass.newInstance();
				
				if (proxyPacket instanceof Packet3Chat)
				{
					((Packet3Chat)proxyPacket).message = this.message;
				}
			}
		}
		catch (Exception ex) {}
	}

	@Override
	public void readPacketData(DataInput datainputstream) throws IOException
	{
		if (proxyPacket != null)
		{
			proxyPacket.readPacketData(datainputstream);
			this.message = ((Packet3Chat)proxyPacket).message;
		}
		else
			super.readPacketData(datainputstream);
	}

	@Override
	public void writePacketData(DataOutput dataoutputstream) throws IOException
	{
		if (proxyPacket != null)
			proxyPacket.writePacketData(dataoutputstream);
		else
			super.writePacketData(dataoutputstream);
	}

	@Override
	public void processPacket(NetHandler nethandler)
	{
		if (events == null || events.onChat(this))
		{
			if (proxyPacket != null)
				proxyPacket.processPacket(nethandler);
			else
				super.processPacket(nethandler);
		}
	}

	@Override
	public int getPacketSize()
	{
		if (proxyPacket != null)
			return proxyPacket.getPacketSize();

		return super.getPacketSize();
	}
	
	/**
	 * Register the specified handler as the packet handler for this packet
	 * @param handler
	 */
	public static void registerPacketHandler(Events handler)
	{
		events = handler;
	}
	
	/**
	 * Register this packet as the new packet for packet ID 3
	 */
	public static void register()
	{
		register(false);
	}
	
	/**
	 * Register this packet as the new packet for packet ID 3 and optionally force re-registration even
	 * if registration was performed already.
	 * 
	 * @param force Force registration even if registration was already performed previously.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void register(boolean force)
	{
		if (!registered || force)
		{
			try
			{
				IntHashMap packetIdToClassMap = Packet.packetIdToClassMap;
				proxyClass = (Class<? extends Packet>)packetIdToClassMap.lookup(3);
				
				if (proxyClass.equals(Packet3Chat.class))
				{
					proxyClass = null;
				}
				
				packetIdToClassMap.removeObject(3);
				packetIdToClassMap.addKey(3, HookChat.class);
				
				Map packetClassToIdMap = PrivateFields.StaticFields.packetClassToIdMap.get();
				packetClassToIdMap.put(HookChat.class, Integer.valueOf(3));
				
				registered = true;
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
	}
}
