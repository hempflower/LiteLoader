package com.mumfrey.liteloader.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

import net.minecraft.src.*;

/**
 * Wrapper for obf/mcp reflection-accessed private fields, mainly added to centralise the locations I have to update the obfuscated field names
 * 
 * @author Adam Mummery-Smith
 *
 * @param <P> Parent class type, the type of the class that owns the field
 * @param <T> Field type, the type of the field value
 * 
 * TODO Obfuscation - updated 1.6.4
 */
@SuppressWarnings("rawtypes")
public class PrivateFields<P, T>
{
	/**
	 * Class to which this field belongs
	 */
	public final Class<P> parentClass;

	/**
	 * MCP name for this field
	 */
	public final String mcpName;

	/**
	 * Real (obfuscated) name for this field
	 */
	public final String name;
	
	public final String seargeName;
	
	/**
	 * Name used to access the field, determined at init
	 */
	private final String fieldName;
	
	/**
	 * Creates a new private field entry
	 * 
	 * @param owner
	 * @param mcpName
	 * @param name
	 */
	private PrivateFields(Class<P> owner, String mcpName, String name, String seargeName)
	{
		this.parentClass = owner;
		this.mcpName     = mcpName;
		this.name        = name;
		this.seargeName  = seargeName;
		
		this.fieldName = ModUtilities.getObfuscatedFieldName(mcpName, name, seargeName);
	}
	
	/**
	 * Get the current value of this field on the instance class supplied
	 * 
	 * @param instance Class to get the value of
	 * @return field value or null if errors occur
	 */
	@SuppressWarnings("unchecked")
	public T get(P instance)
	{
		try
		{
			Field field = this.parentClass.getDeclaredField(this.fieldName);
			field.setAccessible(true);
			return (T)field.get(instance);
		}
		catch (Exception ex)
		{
			return null;
		}
	}

	/**
	 * Set the value of this field on the instance class supplied
	 * 
	 * @param instance Object to set the value of the field on
	 * @param value value to set
	 * @return value
	 */
	public T set(P instance, T value)
	{
		try
		{
			Field field = this.parentClass.getDeclaredField(this.fieldName);
			field.setAccessible(true);
			field.set(instance, value);
		}
		catch (Exception ex) { }
		
		return value;
	}
	
	/**
	 * Set the value of this FINAL field on the instance class supplied
	 * 
	 * @param instance Object to set the value of the field on
	 * @param value value to set
	 * @return value
	 */
	public T setFinal(P instance, T value)
	{
		try
		{
			Field modifiers = Field.class.getDeclaredField("modifiers");
			modifiers.setAccessible(true);
			
			Field field = this.parentClass.getDeclaredField(this.fieldName);
			modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
			field.setAccessible(true);
			field.set(instance, value);
		}
		catch (Exception ex) { }
		
		return value;
	}
	
	/**
	 * Static private fields
	 *
	 * @param <P> Parent class type, the type of the class that owns the field
	 * @param <T> Field type, the type of the field value
	 */
	public static final class StaticFields<P, T> extends PrivateFields<P, T>
	{
		@SuppressWarnings("synthetic-access")
		public StaticFields(Class<P> owner, String mcpName, String name, String fmlName) { super(owner, mcpName, name, fmlName); }
		public T get() { return get(null); }
		public void set(T value) { set(null, value); }
		
		public static final StaticFields<Packet, Map>           packetClassToIdMap = new StaticFields<Packet, Map>     (Packet.class,     "packetClassToIdMap", "a", "field_73291_a"); // Packet/packetClassToIdMap
		public static final StaticFields<TileEntity, Map> tileEntityNameToClassMap = new StaticFields<TileEntity, Map> (TileEntity.class, "nameToClassMap",     "a", "field_70326_a"); // TileEntity/nameToClassMap
	}

	public static final PrivateFields<Minecraft, Timer>                       minecraftTimer = new PrivateFields<Minecraft, Timer>                (Minecraft.class,          "timer",                "S",  "field_71428_T");   // Minecraft/timer
	public static final PrivateFields<Minecraft, Profiler>                 minecraftProfiler = new PrivateFields<Minecraft, Profiler>             (Minecraft.class,          "mcProfiler",           "C",  "field_71424_I");   // Minecraft/mcProfiler
	public static final PrivateFields<Minecraft, List<ResourcePack>>    defaultResourcePacks = new PrivateFields<Minecraft, List<ResourcePack>>   (Minecraft.class,          "defaultResourcePacks", "aq", "field_110449_ao"); // Minecraft/defaultResourcePacks
	public static final PrivateFields<RenderManager, Map>                    entityRenderMap = new PrivateFields<RenderManager, Map>              (RenderManager.class,      "entityRenderMap",      "q",  "field_78729_o");   // RenderManager/entityRenderMap
	public static final PrivateFields<GuiControls, GuiScreen>        guiControlsParentScreen = new PrivateFields<GuiControls, GuiScreen>          (GuiControls.class,        "parentScreen",         "b",  "field_73909_b");   // GuiControls/parentScreen
	public static final PrivateFields<PlayerUsageSnooper, IPlayerUsage> playerStatsCollector = new PrivateFields<PlayerUsageSnooper, IPlayerUsage>(PlayerUsageSnooper.class, "playerStatsCollector", "d",  "field_76478_d");   // PlayerUsageSnooper/playerStatsCollector
	public static final PrivateFields<SimpleReloadableResourceManager, List<ResourceManagerReloadListener>> reloadListeners = new PrivateFields<SimpleReloadableResourceManager, List<ResourceManagerReloadListener>>(SimpleReloadableResourceManager.class, "reloadListeners", "c", "field_110546_b");   // SimpleReloadableResourceManager/reloadListeners
}

