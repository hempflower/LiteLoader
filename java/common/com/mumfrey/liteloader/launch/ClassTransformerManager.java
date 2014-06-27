package com.mumfrey.liteloader.launch;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.LaunchClassLoader;

import com.mumfrey.liteloader.transformers.PacketTransformer;
import com.mumfrey.liteloader.util.SortableValue;
import com.mumfrey.liteloader.util.log.LiteLoaderLogger;

/**
 * Manages injection of required and optional transformers
 * 
 * @author Adam Mummery-Smith
 */
public class ClassTransformerManager
{
	/**
	 * Once the game is started we can no longer inject transformers
	 */
	private boolean gameStarted;
	
	/**
	 * Transformers to inject
	 */
	private Set<String> injectedTransformers = new HashSet<String>();
	
	/**
	 * Transformers to inject after preInit but before the game starts, necessary for anything that needs to be downstream of forge
	 */
	private Set<String> downstreamTransformers = new HashSet<String>();
	
	/**
	 * Packet transformers, seived from the injectedTransformers list
	 */
	private Map<String, TreeSet<SortableValue<String>>> packetTransformers = new HashMap<String, TreeSet<SortableValue<String>>>();
	
	/**
	 * Transformers passed into the constructor which are required and must be injected upstream
	 */
	private final List<String> requiredTransformers;
	
	/**
	 * @param requiredTransformers
	 */
	public ClassTransformerManager(List<String> requiredTransformers)
	{
		this.requiredTransformers = requiredTransformers;
	}

	/**
	 * @param transformerClass
	 * @return
	 */
	public boolean injectTransformer(String transformerClass)
	{
		if (!this.gameStarted)
		{
			this.injectedTransformers.add(transformerClass);
			return true;
		}
		
		return false;
	}
	
	/**
	 * @param transformerClasses
	 * @return
	 */
	public boolean injectTransformers(Collection<String> transformerClasses)
	{
		if (!this.gameStarted)
		{
			this.injectedTransformers.addAll(transformerClasses);
			return true;
		}
		
		return false;
	}
	
	/**
	 * @param transformerClasses
	 * @return
	 */
	public boolean injectTransformers(String[] transformerClasses)
	{
		if (!this.gameStarted)
		{
			this.injectedTransformers.addAll(Arrays.asList(transformerClasses));
			return true;
		}
		
		return false;
	}
	
	/**
	 * @param classLoader
	 */
	void injectUpstreamTransformers(LaunchClassLoader classLoader)
	{
		this.sieveAndSortPacketTransformers(classLoader, this.injectedTransformers);
		
		for (String requiredTransformerClassName : this.requiredTransformers)
		{
			LiteLoaderLogger.info("Injecting required class transformer '%s'", requiredTransformerClassName);
			classLoader.registerTransformer(requiredTransformerClassName);
		}
		
		for (Entry<String, TreeSet<SortableValue<String>>> packetClassTransformers : this.packetTransformers.entrySet())
		{
			for (SortableValue<String> transformerInfo : packetClassTransformers.getValue())
			{
				String packetClass = packetClassTransformers.getKey();
				if (packetClass.lastIndexOf('.') != -1) packetClass = packetClass.substring(packetClass.lastIndexOf('.') + 1);
				LiteLoaderLogger.info("Injecting packet class transformer '%s' for packet class '%s' with priority %d", transformerInfo.getValue(), packetClass, transformerInfo.getPriority());
				classLoader.registerTransformer(transformerInfo.getValue());
			}
		}
		
		// inject any transformers received after this point directly into the downstreamTransformers set
		this.injectedTransformers = this.downstreamTransformers;
	}

	/**
	 * @param classLoader
	 */
	void injectDownstreamTransformers(LaunchClassLoader classLoader)
	{
		if (this.downstreamTransformers.size() > 0)
			LiteLoaderLogger.info("Injecting downstream transformers");

		for (String transformerClassName : this.downstreamTransformers)
		{
			LiteLoaderLogger.info("Injecting additional class transformer class '%s'", transformerClassName);
			classLoader.registerTransformer(transformerClassName);
		}
		
		this.downstreamTransformers.clear();
		this.gameStarted = true;
	}

	/**
	 * Sieves packet transformers from the injected transformers list and pokes the rest into the downstreamTransformers set
	 * 
	 * @param classLoader
	 * @param transformers
	 */
	@SuppressWarnings("unchecked")
	private void sieveAndSortPacketTransformers(LaunchClassLoader classLoader, Set<String> transformers)
	{
		LiteLoaderLogger.info("Sorting registered packet transformers by priority");
		int registeredTransformers = 0;
		
		NonDelegatingClassLoader tempLoader = new NonDelegatingClassLoader(classLoader.getURLs(), this.getClass().getClassLoader());
		tempLoader.addDelegatedClassName("com.mumfrey.liteloader.core.transformers.PacketTransformer");
		tempLoader.addDelegatedClassName("com.mumfrey.liteloader.core.runtime.Obf");
		tempLoader.addDelegatedClassName("net.minecraft.launchwrapper.IClassTransformer");
		tempLoader.addDelegatedPackage("org.objectweb.asm.");

		Iterator<String> iter = transformers.iterator();
		while (iter.hasNext())
		{
			String transformerClassName = iter.next();
			try
			{
				Class<IClassTransformer> transformerClass = (Class<IClassTransformer>)tempLoader.addAndLoadClass(transformerClassName);
				
				if (PacketTransformer.class.isAssignableFrom(transformerClass))
				{
					if (tempLoader.isValid())
					{
						PacketTransformer transformer = (PacketTransformer)transformerClass.newInstance();
						String packetClass = transformer.getPacketClass();
						if (!this.packetTransformers.containsKey(packetClass))
							this.packetTransformers.put(packetClass, new TreeSet<SortableValue<String>>());
						this.packetTransformers.get(packetClass).add(transformer.getInfo(transformerClassName));
						registeredTransformers++;
						iter.remove();
					}
					else
					{
						LiteLoaderLogger.warning("Packet transformer class '%s' references class '%s' which is not allowed. Packet transformers must not contain references to other classes", transformerClassName, tempLoader.getInvalidClassName()); 
						iter.remove();
					}
				}
			}
			catch (NoClassDefFoundError err)
			{
				LiteLoaderLogger.warning(err, "Packet transformer class '%s' references a missing class. This probably means it is out of date or missing a dependency.", transformerClassName); 
				err.printStackTrace();
				iter.remove();
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
		
		this.downstreamTransformers.addAll(transformers);
		transformers.clear();
		
		LiteLoaderLogger.info("Added %d packet transformer classes to the transformer list", registeredTransformers);
	}
}
