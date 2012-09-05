package com.mumfrey.liteloader.util;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.src.IntHashMap;
import net.minecraft.src.KeyBinding;
import net.minecraft.src.Packet;
import net.minecraft.src.Packet250CustomPayload;
import net.minecraft.src.Render;
import net.minecraft.src.RenderManager;
import net.minecraft.src.Tessellator;

import com.mumfrey.liteloader.core.LiteLoader;

public abstract class ModUtilities
{
    /**
     * Add a renderer map entry for the specified entity class
     * 
     * @param entityClass
     * @param renderer
     */
    public static void addRenderer(Class entityClass, Render renderer)
    {
    	Map entityRenderMap = PrivateFields.entityRenderMap.Get(RenderManager.instance);
    	entityRenderMap.put(entityClass, renderer);
    	renderer.setRenderManager(RenderManager.instance);
    }

	/**
	 * Register a packet override
	 * 
	 * @param packetId
	 * @param newPacket
	 */
	public static boolean registerPacketOverride(int packetId, Class newPacket)
	{
		try
		{
	    	IntHashMap packetIdToClassMap = Packet.packetIdToClassMap;
	    	PrivateFields.StaticFields.packetClassToIdMap.Get();
			Map packetClassToIdMap = PrivateFields.StaticFields.packetClassToIdMap.Get();
			
		    packetIdToClassMap.removeObject(packetId);
		    packetIdToClassMap.addKey(packetId, newPacket);
	        packetClassToIdMap.put(newPacket, Integer.valueOf(packetId));
			
			return true;
		}
		catch (Exception ex)
		{
			LiteLoader.logger.warning("Error registering packet override for packet id " + packetId + ": " + ex.getMessage());
			return false;
		}
	}
	
	/**
	 * Send a plugin channel (custom payload) packet to the server
	 * 
	 * @param channel Channel to send the data
	 * @param data
	 */
	public static void sendPluginChannelMessage(String channel, byte[] data)
	{
		if (channel == null || channel.length() > 16)
			throw new RuntimeException("Invalid channel name specified"); 
		
		try
		{
			Minecraft minecraft = Minecraft.getMinecraft();
			
			if (minecraft.thePlayer != null)
			{
				Packet250CustomPayload payload = new Packet250CustomPayload(channel, data);
				minecraft.thePlayer.sendQueue.addToSendQueue(payload);
			}
		}
		catch (Exception ex) {}
	}

	/**
	 * Abstraction helper function
	 * 
	 * @param fieldName Name of field to get, returned unmodified if in debug mode
	 * @return Obfuscated field name if present
	 */
	public static String getObfuscatedFieldName(String fieldName, String obfuscatedFieldName)
	{
		return (!net.minecraft.src.Tessellator.instance.getClass().getSimpleName().equals("Tessellator")) ? obfuscatedFieldName : fieldName;
	}

	/**
	 * Registers a keybind with the game settings class so that it is configurable in the "controls" screen
	 * 
	 * @param newBinding key binding to add
	 */
	public static void registerKey(KeyBinding newBinding)
	{
		Minecraft mc = Minecraft.getMinecraft();
		
		if (mc == null || mc.gameSettings == null) return;
		
	    LinkedList<KeyBinding> keyBindings = new LinkedList<KeyBinding>();
	    keyBindings.addAll(Arrays.asList(mc.gameSettings.keyBindings));
	    
	    if (!keyBindings.contains(newBinding))
	    {
	    	keyBindings.add(newBinding);
	    	mc.gameSettings.keyBindings = keyBindings.toArray(new KeyBinding[0]);
	    }
	}

	/**
	 * Unregisters a registered keybind with the game settings class, thus removing it from the "controls" screen
	 * 
	 * @param removeBinding
	 */
	public static void unRegisterKey(KeyBinding removeBinding)
	{
		Minecraft mc = Minecraft.getMinecraft();
		
		if (mc == null || mc.gameSettings == null) return;
	
		LinkedList<KeyBinding> keyBindings = new LinkedList<KeyBinding>();
	    keyBindings.addAll(Arrays.asList(mc.gameSettings.keyBindings));
	    
	    if (keyBindings.contains(removeBinding))
	    {
	        keyBindings.remove(removeBinding);
	        mc.gameSettings.keyBindings = keyBindings.toArray(new KeyBinding[0]);
	    }
	}
}
