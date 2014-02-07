package com.mumfrey.liteloader.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

import com.mumfrey.liteloader.core.runtime.Obf;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerLoginClient;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.network.NetworkManager;
import net.minecraft.profiler.Profiler;

/**
 * Wrapper for obf/mcp reflection-accessed private fields, mainly added to centralise the locations I have to update the obfuscated field names
 * 
 * @author Adam Mummery-Smith
 *
 * @param <P> Parent class type, the type of the class that owns the field
 * @param <T> Field type, the type of the field value
 */
@SuppressWarnings("rawtypes")
public class PrivateFields<P, T>
{
	/**
	 * Class to which this field belongs
	 */
	public final Class<P> parentClass;

	/**
	 * Name used to access the field, determined at init
	 */
	private final String fieldName;
	
	private boolean errorReported = false;
	
	/**
	 * Creates a new private field entry
	 * 
	 * @param obf
	 */
	private PrivateFields(Class<P> owner, Obf obf)
	{
		this.parentClass = owner;
		this.fieldName = ModUtilities.getObfuscatedFieldName(obf);
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
			if (!this.errorReported)
			{
				this.errorReported = true;
				ex.printStackTrace();
			}
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
		catch (Exception ex)
		{
			if (!this.errorReported)
			{
				this.errorReported = true;
				ex.printStackTrace();
			}
		}
		
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
		catch (Exception ex)
		{
			if (!this.errorReported)
			{
				this.errorReported = true;
				ex.printStackTrace();
			}
		}
		
		return value;
	}

	public static final PrivateFields<Minecraft, Profiler>                 minecraftProfiler = new PrivateFields<Minecraft, Profiler>                  (Minecraft.class,             Obf.minecraftProfiler);
	public static final PrivateFields<RenderManager, Map>                    entityRenderMap = new PrivateFields<RenderManager, Map>                   (RenderManager.class,         Obf.entityRenderMap);
	public static final PrivateFields<NetHandlerLoginClient, NetworkManager>      netManager = new PrivateFields<NetHandlerLoginClient, NetworkManager>(NetHandlerLoginClient.class, Obf.netManager);
	
	public static final PrivateFields<SimpleReloadableResourceManager, List<IResourceManagerReloadListener>> reloadListeners =
			new PrivateFields<SimpleReloadableResourceManager, List<IResourceManagerReloadListener>>(SimpleReloadableResourceManager.class, Obf.reloadListeners);
}