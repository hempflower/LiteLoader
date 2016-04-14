package com.mumfrey.liteloader.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

import com.mumfrey.liteloader.client.ducks.IMutableRegistry;
import com.mumfrey.liteloader.client.ducks.IRenderManager;
import com.mumfrey.liteloader.client.ducks.ITileEntityRendererDispatcher;
import com.mumfrey.liteloader.client.overlays.IMinecraft;
import com.mumfrey.liteloader.client.util.PrivateFieldsClient;
import com.mumfrey.liteloader.util.log.LiteLoaderLogger;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

/**
 * A small collection of useful functions for mods
 * 
 * @author Adam Mummery-Smith
 */
public abstract class ModUtilities
{
    /**
     * @return true if FML is present in the current environment
     */
    public static boolean fmlIsPresent()
    {
        return ObfuscationUtilities.fmlIsPresent();
    }

    public static void setWindowSize(int width, int height)
    {
        try
        {
            Minecraft mc = Minecraft.getMinecraft();
            Display.setDisplayMode(new DisplayMode(width, height));
            ((IMinecraft)mc).onResizeWindow(width, height);
            Display.setVSyncEnabled(mc.gameSettings.enableVsync);
        }
        catch (LWJGLException ex)
        {
            ex.printStackTrace();
        }
    }

    /**
     * Add a renderer map entry for the specified entity class
     * 
     * @param entityClass
     * @param renderer
     */
    public static <T extends Entity> void addRenderer(Class<T> entityClass, Render<T> renderer)
    {
        RenderManager renderManager = Minecraft.getMinecraft().getRenderManager();

        Map<Class<? extends Entity>, Render<? extends Entity>> entityRenderMap = ((IRenderManager)renderManager).getRenderMap();
        if (entityRenderMap != null)
        {
            entityRenderMap.put(entityClass, renderer);
        }
        else
        {
            LiteLoaderLogger.warning("Attempted to set renderer %s for entity class %s but the operation failed",
                    renderer.getClass().getSimpleName(), entityClass.getSimpleName());
        }
    }

    public static <T extends TileEntity> void addRenderer(Class<T> tileEntityClass, TileEntitySpecialRenderer<T> renderer)
    {
        TileEntityRendererDispatcher tileEntityRenderer = TileEntityRendererDispatcher.instance;

        try
        {
            Map<Class<? extends TileEntity>, TileEntitySpecialRenderer<? extends TileEntity>> specialRendererMap
                    = ((ITileEntityRendererDispatcher)tileEntityRenderer).getSpecialRenderMap();
            specialRendererMap.put(tileEntityClass, renderer);
            renderer.setRendererDispatcher(tileEntityRenderer);
        }
        catch (Exception ex)
        {
            LiteLoaderLogger.warning("Attempted to set renderer %s for tile entity class %s but the operation failed",
                    renderer.getClass().getSimpleName(), tileEntityClass.getSimpleName());
        }
    }

    /**
     * Add a block to the blocks registry
     * 
     * @param blockId Block ID to insert
     * @param blockName Block identifier
     * @param block Block to register
     * @param force Force insertion even if the operation is blocked by FMl
     * 
     * @deprecated Register blocks directly with the registry
     */
    @SuppressWarnings("unchecked")
    @Deprecated
    public static void addBlock(int blockId, ResourceLocation blockName, Block block, boolean force)
    {
        boolean exists = Block.REGISTRY.containsKey(blockName);
        Block existingBlock = Block.REGISTRY.getObject(blockName);
        
        try
        {
            Block.REGISTRY.register(blockId, blockName, block);
        }
        catch (IllegalArgumentException ex)
        {
            if (!force) throw new IllegalArgumentException("Could not register block '" + blockName + "', the operation was blocked by FML.", ex);

            if (Block.REGISTRY instanceof IMutableRegistry)
            {
                ((IMutableRegistry<ResourceLocation, Block>)Block.REGISTRY).removeObjectFromRegistry(blockName);
                Block.REGISTRY.register(blockId, blockName, block);
            }
        }

        if (exists)
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
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Add an item to the items registry
     * 
     * @param itemId Item ID to insert
     * @param itemName Item identifier
     * @param item Item to register
     * @param force Force insertion even if the operation is blocked by FMl
     * 
     * @deprecated Register items directly with the registry
     */
    @SuppressWarnings("unchecked")
    @Deprecated
    public static void addItem(int itemId, ResourceLocation itemName, Item item, boolean force)
    {
        boolean exists = Item.REGISTRY.containsKey(itemName);
        Item existingItem = Item.REGISTRY.getObject(itemName);
        
        try
        {
            Item.REGISTRY.register(itemId, itemName, item);
        }
        catch (IllegalArgumentException ex)
        {
            if (!force) throw new IllegalArgumentException("Could not register item '" + itemName + "', the operation was blocked by FML.", ex);

            if (Block.REGISTRY instanceof IMutableRegistry)
            {
                ((IMutableRegistry<ResourceLocation, Block>)Item.REGISTRY).removeObjectFromRegistry(itemName);
                Item.REGISTRY.register(itemId, itemName, item);
            }
        }

        if (exists)
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
            Map<String, Class<? extends TileEntity>> nameToClassMap = PrivateFieldsClient.tileEntityNameToClassMap.get(null);
            Map<Class<? extends TileEntity>, String> classToNameMap = PrivateFieldsClient.tileEntityClassToNameMap.get(null);
            nameToClassMap.put(entityName, tileEntityClass);
            classToNameMap.put(tileEntityClass, entityName);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private static void setFinalStaticField(Field field, Object value)
            throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
    {
        Field modifiers = Field.class.getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(null, value);
    }
}
