package com.mumfrey.liteloader.launch;

import java.lang.reflect.Field;
import java.util.*;
import java.util.Map.Entry;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraft.launchwrapper.LogWrapper;

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
	private Set<String> pendingTransformers = new HashSet<String>();
	
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
	 * Transformers successfully injected by us
	 */
	private final Set<String> injectedTransformers = new HashSet<String>();
	
	/**
	 * Catalogue of transformer startup failures
	 */
	private final Map<String, List<Throwable>> transformerStartupErrors = new HashMap<String, List<Throwable>>();
	
	private Logger attachedLog;
	
	private String pendingTransformer;
	
	class ThrowableObserver extends AbstractAppender
	{
		public ThrowableObserver()
		{
			super("Throwable Observer", null, null);
			this.start();
		}
		
		@Override
		public void append(LogEvent event)
		{
			ClassTransformerManager.this.observeThrowable(event.getThrown());
		}
	}
	
	/**
	 * @param requiredTransformers
	 */
	public ClassTransformerManager(List<String> requiredTransformers)
	{
		this.requiredTransformers = requiredTransformers;
		
		this.appendObserver();
	}

	private void appendObserver()
	{
		try
		{
			Field fLogger = LogWrapper.class.getDeclaredField("myLog");
			fLogger.setAccessible(true);
			this.attachedLog = (Logger)fLogger.get(LogWrapper.log);
			if (this.attachedLog instanceof org.apache.logging.log4j.core.Logger)
			{
				((org.apache.logging.log4j.core.Logger)this.attachedLog).addAppender(new ThrowableObserver());
			}
		}
		catch (Exception ex)
		{
			LiteLoaderLogger.warning("Failed to append ThrowableObserver to LogWrapper, transformer startup exceptions may not be logged");
		}
	}

	/**
	 * @param transformerClass
	 * @return
	 */
	public boolean injectTransformer(String transformerClass)
	{
		if (!this.gameStarted)
		{
			this.pendingTransformers.add(transformerClass);
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
			this.pendingTransformers.addAll(transformerClasses);
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
			this.pendingTransformers.addAll(Arrays.asList(transformerClasses));
			return true;
		}
		
		return false;
	}
	
	/**
	 * @param classLoader
	 */
	void injectUpstreamTransformers(LaunchClassLoader classLoader)
	{
		this.sieveAndSortPacketTransformers(classLoader, this.pendingTransformers);
		
		for (String requiredTransformerClassName : this.requiredTransformers)
		{
			LiteLoaderLogger.info("Injecting required class transformer '%s'", requiredTransformerClassName);
			this.injectTransformer(classLoader, requiredTransformerClassName);
		}
		
		for (Entry<String, TreeSet<SortableValue<String>>> packetClassTransformers : this.packetTransformers.entrySet())
		{
			for (SortableValue<String> transformerInfo : packetClassTransformers.getValue())
			{
				String packetClass = packetClassTransformers.getKey();
				String transformerClassName = transformerInfo.getValue();
				if (packetClass.lastIndexOf('.') != -1) packetClass = packetClass.substring(packetClass.lastIndexOf('.') + 1);
				LiteLoaderLogger.info("Injecting packet class transformer '%s' for packet class '%s' with priority %d", transformerClassName, packetClass, transformerInfo.getPriority());
				this.injectTransformer(classLoader, transformerClassName);
			}
		}
		
		// inject any transformers received after this point directly into the downstreamTransformers set
		this.pendingTransformers = this.downstreamTransformers;
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
			this.injectTransformer(classLoader, transformerClassName);
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
	@SuppressWarnings({ "unchecked", "deprecation" })
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

	private synchronized void injectTransformer(LaunchClassLoader classLoader, String transformerClassName)
	{
		// Assign pendingTransformer so that logged errors during transformer init can be put in the map
		this.pendingTransformer = transformerClassName;
		
		// Register the transformer
		classLoader.registerTransformer(transformerClassName);
		
		// Unassign pending transformer now init is completed
		this.pendingTransformer = null;
		
		// Check whether the transformer was successfully injected, look for it in the transformer list
		if (this.findTransformer(classLoader, transformerClassName) != null)
		{
			this.injectedTransformers.add(transformerClassName);
		}
	}

	public void observeThrowable(Throwable th)
	{
		if (th != null && this.pendingTransformer != null)
		{
			List<Throwable> transformerErrors = this.transformerStartupErrors.get(this.pendingTransformer);
			if (transformerErrors == null)
			{
				transformerErrors = new ArrayList<Throwable>();
				this.transformerStartupErrors.put(this.pendingTransformer, transformerErrors);
			}
			transformerErrors.add(th);
		}
	}

	private IClassTransformer findTransformer(LaunchClassLoader classLoader, String transformerClassName)
	{
		for (IClassTransformer transformer : classLoader.getTransformers())
		{
			if (transformer.getClass().getName().equals(transformerClassName))
				return transformer;
		}
		
		return null;
	}
	
	public Set<String> getInjectedTransformers()
	{
		return Collections.unmodifiableSet(this.injectedTransformers);
	}
	
	public List<Throwable> getTransformerStartupErrors(String transformerClassName)
	{
		List<Throwable> errorList = this.transformerStartupErrors.get(transformerClassName);
		return errorList != null ? Collections.unmodifiableList(errorList) : null;
	}
}
