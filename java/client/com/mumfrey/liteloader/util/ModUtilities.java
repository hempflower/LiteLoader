package com.mumfrey.liteloader.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ObjectIntIdentityMap;
import net.minecraft.util.RegistryNamespaced;
import net.minecraft.util.RegistrySimple;

import com.mumfrey.liteloader.client.util.PrivateFields;
import com.mumfrey.liteloader.core.LiteLoader;
import com.mumfrey.liteloader.core.runtime.Obf;
import com.mumfrey.liteloader.util.log.LiteLoaderLogger;

/**
 * A small collection of useful functions for mods
 * 
 * @author Adam Mummery-Smith
 */
public abstract class ModUtilities
{
	/**
	 * True if FML is being used, in which case we use searge names instead of raw field/method names
	 */
	private static boolean fmlDetected = false;
	
	private static boolean seargeNames = false;
	
	static
	{
		// Check for FML
		ModUtilities.fmlDetected = ModUtilities.fmlIsPresent();

		try
		{
			Minecraft.class.getDeclaredField("running");
		}
		catch (SecurityException ex)
		{
		}
		catch (NoSuchFieldException ex)
		{
			ModUtilities.seargeNames = true;
		}
	}

	/**
	 * @return
	 */
	public static boolean fmlIsPresent()
	{
//		if (ClientBrandRetriever.getClientModName().contains("fml")) return true;

		for (IClassTransformer transformer : Launch.classLoader.getTransformers())
			if (transformer.getClass().getName().contains("fml")) return true;

		return false;
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
		if (entityRenderMap != null)
		{
			entityRenderMap.put(entityClass, renderer);
			renderer.setRenderManager(RenderManager.instance);
		}
		else
		{
			LiteLoaderLogger.warning("Attempted to set renderer %s for entity class %s but the operation failed", renderer.getClass().getSimpleName(), entityClass.getSimpleName());
		}
	}
	
	@SuppressWarnings("unchecked")
	public static void addRenderer(Class<? extends TileEntity> tileEntityClass, TileEntitySpecialRenderer renderer)
	{
        TileEntityRendererDispatcher tileEntityRenderer = TileEntityRendererDispatcher.instance;
        
        try
        {
        	Map<Class<? extends TileEntity>, TileEntitySpecialRenderer> specialRendererMap = PrivateFields.specialRendererMap.get(tileEntityRenderer);
			specialRendererMap.put(tileEntityClass, renderer);
			renderer.func_147497_a(tileEntityRenderer); // setDispatcher
		}
        catch (Exception ex)
        {
			LiteLoaderLogger.warning("Attempted to set renderer %s for tile entity class %s but the operation failed", renderer.getClass().getSimpleName(), tileEntityClass.getSimpleName());
        }
	}
	
	/**
	 * Add a block to the blocks registry
	 * 
	 * @param blockId Block ID to insert
	 * @param blockName Block identifier
	 * @param block Block to register
	 * @param force Force insertion even if the operation is blocked by FMl
	 */
	public static void addBlock(int blockId, String blockName, Block block, boolean force)
	{
		Block existingBlock = Block.blockRegistry.getObject(blockName);
		
		try
		{
			Block.blockRegistry.addObject(blockId, blockName, block);
		}
		catch (IllegalArgumentException ex)
		{
			if (!force) throw new IllegalArgumentException("Could not register block '" + blockName + "', the operation was blocked by FML.", ex);
			
			ModUtilities.removeObjectFromRegistry(Block.blockRegistry, blockName);
			Block.blockRegistry.addObject(blockId, blockName, block);
		}
		
		if (existingBlock != null)
		{
			try
			{
				for (Field field : Blocks.class.getDeclaredFields())
				{
					field.setAccessible(true);
					if (field.isAccessible() && Block.class.isAssignableFrom(field.getType()))
					{
						Block fieldValue = (Block)field.get(null);
						if (fieldValue == existingBlock)
						{
							ModUtilities.setFinalStaticField(field, block);
						}
					}
				}
			}
			catch (Exception ex) { ex.printStackTrace(); }
		}
	}
	
	/**
	 * Add an item to the items registry
	 * 
	 * @param itemId Item ID to insert
	 * @param itemName Item identifier
	 * @param item Item to register
	 * @param force Force insertion even if the operation is blocked by FMl
	 */
	public static void addItem(int itemId, String itemName, Item item, boolean force)
	{
		Item existingItem = Item.itemRegistry.getObject(itemName);
		
		try
		{
			Item.itemRegistry.addObject(itemId, itemName, item);
		}
		catch (IllegalArgumentException ex)
		{
			if (!force) throw new IllegalArgumentException("Could not register item '" + itemName + "', the operation was blocked by FML.", ex);
			
			ModUtilities.removeObjectFromRegistry(Block.blockRegistry, itemName);
			Item.itemRegistry.addObject(itemId, itemName, item);
		}
		
		if (existingItem != null)
		{
			try
			{
				for (Field field : Items.class.getDeclaredFields())
				{
					field.setAccessible(true);
					if (field.isAccessible() && Item.class.isAssignableFrom(field.getType()))
					{
						Item fieldValue = (Item)field.get(null);
						if (fieldValue == existingItem)
						{
							ModUtilities.setFinalStaticField(field, item);
						}
					}
				}
			}
			catch (Exception ex) {}
		}
	}
	
	@SuppressWarnings("unchecked")
	public static void addTileEntity(String entityName, Class<? extends TileEntity> tileEntityClass)
	{
        try
		{
			Map<String, Class<? extends TileEntity>> nameToClassMap = PrivateFields.tileEntityNameToClassMap.get(null);
			Map<Class<? extends TileEntity>, String> classToNameMap = PrivateFields.tileEntityClassToNameMap.get(null);
			
			nameToClassMap.put(entityName, tileEntityClass);
			classToNameMap.put(tileEntityClass, entityName);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	/**
	 * Abstraction helper function
	 * 
	 * @param fieldName Name of field to get, returned unmodified if in debug mode
	 * @return Obfuscated field name if present
	 */
	public static String getObfuscatedFieldName(String fieldName, String obfuscatedFieldName, String seargeFieldName)
	{
		boolean deobfuscated = Tessellator.class.getSimpleName().equals("Tessellator");
		return deobfuscated ? (ModUtilities.seargeNames ? seargeFieldName : fieldName) : (ModUtilities.fmlDetected ? seargeFieldName : obfuscatedFieldName);
	}
	
	/**
	 * Abstraction helper function
	 * 
	 * @param fieldName Name of field to get, returned unmodified if in debug mode
	 * @return Obfuscated field name if present
	 */
	public static String getObfuscatedFieldName(Obf obf)
	{
		boolean deobfuscated = Tessellator.class.getSimpleName().equals("Tessellator");
		return deobfuscated ? (ModUtilities.seargeNames ? obf.srg : obf.name) : (ModUtilities.fmlDetected ? obf.srg : obf.obf);
	}

	/**
	 * Registers a keybind with the game settings class so that it is configurable in the "controls" screen
	 * 
	 * @param newBinding key binding to add
	 * @deprecated Deprecated : use LiteLoader.getInput().registerKeyBinding() instead
	 */
	@Deprecated
	public static void registerKey(KeyBinding newBinding)
	{
		LiteLoader.getInput().registerKeyBinding(newBinding);
	}
	
	/**
	 * Unregisters a registered keybind with the game settings class, thus removing it from the "controls" screen
	 * 
	 * @param removeBinding
	 * @deprecated Deprecated : use LiteLoader.getInput().unRegisterKeyBinding() instead
	 */
	@Deprecated
	public static void unRegisterKey(KeyBinding removeBinding)
	{
		LiteLoader.getInput().unRegisterKeyBinding(removeBinding);
	}
	
	@SuppressWarnings("unchecked")
	private static <K, V> V removeObjectFromRegistry(RegistrySimple<K, V> registry, K key)
	{
		if (registry == null) return null;
		
		ObjectIntIdentityMap<V> underlyingIntegerMap = null;
		
		if (registry instanceof RegistryNamespaced)
		{
			RegistryNamespaced<V> rns = (RegistryNamespaced<V>)registry;
			underlyingIntegerMap = PrivateFields.underlyingIntegerMap.get(rns); 
		}
		
		Map<K, V> registryObjects = PrivateFields.registryObjects.get(registry);
		if (registryObjects != null)
		{
			V existingValue = registryObjects.get(key);
			if (existingValue != null)
			{
				registryObjects.remove(key);
				
				if (underlyingIntegerMap != null)
				{
					IdentityHashMap<V, Integer> identityMap = PrivateFields.identityMap.get(underlyingIntegerMap);
					List<V> objectList = PrivateFields.objectList.get(underlyingIntegerMap);
					if (identityMap != null) identityMap.remove(existingValue);
					if (objectList != null) objectList.remove(existingValue);
				}
				
				return existingValue;
			}
		}
		
		return null;
	}
	
	private static void setFinalStaticField(Field field, Object value) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
	{
		Field modifiers = Field.class.getDeclaredField("modifiers");
		modifiers.setAccessible(true);
		modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
		field.set(null, value);
	}
}
