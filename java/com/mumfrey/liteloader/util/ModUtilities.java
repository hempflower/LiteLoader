package com.mumfrey.liteloader.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import com.mumfrey.liteloader.core.LiteLoader;

import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.src.Minecraft;
import net.minecraft.src.*;

public abstract class ModUtilities
{
	/**
	 * Collection of packets we have already overridden, so that duplicate registrations can generate a warning
	 */
	private static Set<Integer> overriddenPackets = new HashSet<Integer>();
	
	/**
	 * True if FML is being used, in which case we use searge names instead of raw field/method names
	 */
	private static boolean forgeModLoader = false;
	
	static
	{
		// Check for FML
		forgeModLoader = ClientBrandRetriever.getClientModName().contains("fml");
	}
	
	/**
	 * Add a renderer map entry for the specified entity class
	 * 
	 * @param entityClass
	 * @param renderer
	 */
	@SuppressWarnings("unchecked")
	public static void addRenderer(Class<? extends Entity> entityClass, Render renderer)
	{
		Map<Class<? extends Entity>, Render> entityRenderMap = PrivateFields.entityRenderMap.get(RenderManager.instance);
		entityRenderMap.put(entityClass, renderer);
		renderer.setRenderManager(RenderManager.instance);
	}
	
	/**
	 * Register a packet override
	 * 
	 * @param packetId
	 * @param newPacket
	 */
	@SuppressWarnings("unchecked")
	public static boolean registerPacketOverride(int packetId, Class<? extends Packet> newPacket)
	{
		if (overriddenPackets.contains(Integer.valueOf(packetId)))
		{
			LiteLoader.getLogger().warning(String.format("Packet with ID %s was already overridden by another mod, one or mods may not function correctly", packetId));
		}
		
		try
		{
			IntHashMap packetIdToClassMap = Packet.packetIdToClassMap;
			PrivateFields.StaticFields.packetClassToIdMap.get();
			Map<Class<? extends Packet>, Integer> packetClassToIdMap = PrivateFields.StaticFields.packetClassToIdMap.get();
			
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
	public static String getObfuscatedFieldName(String fieldName, String obfuscatedFieldName, String seargeFieldName)
	{
		if (forgeModLoader) return seargeFieldName;
		return !net.minecraft.src.Tessellator.instance.getClass().getSimpleName().equals("Tessellator") ? obfuscatedFieldName : fieldName;
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
			mc.gameSettings.loadOptions();
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
