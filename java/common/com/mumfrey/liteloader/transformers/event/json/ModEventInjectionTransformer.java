package com.mumfrey.liteloader.transformers.event.json;

import java.util.Map.Entry;

import com.mumfrey.liteloader.transformers.ClassTransformer;
import com.mumfrey.liteloader.transformers.event.Event;
import com.mumfrey.liteloader.transformers.event.EventInjectionTransformer;
import com.mumfrey.liteloader.transformers.event.InjectionPoint;
import com.mumfrey.liteloader.transformers.event.MethodInfo;
import com.mumfrey.liteloader.util.log.LiteLoaderLogger;

/**
 * Event transformer which manages injections of mod events specified via events.json in the mod container
 *
 * @author Adam Mummery-Smith
 */
public class ModEventInjectionTransformer extends EventInjectionTransformer
{
	@Override
	protected void addEvents()
	{
		for (Entry<String, String> eventsDefinition : ModEvents.getEvents().entrySet())
		{
			String identifier = eventsDefinition.getKey();
			String json = eventsDefinition.getValue();
			
			this.addEvents(identifier, json);
		}
	}

	/**
	 * @param identifier
	 * @param json
	 */
	private void addEvents(String identifier, String json)
	{
		JsonEvents events = null;
		
		try
		{
			LiteLoaderLogger.info("Parsing events for mod with id %s", identifier);
			events = JsonEvents.parse(json);
			events.register(this);
		}
		catch (InvalidEventJsonException ex)
		{
			LiteLoaderLogger.debug(ClassTransformer.HORIZONTAL_RULE);
			LiteLoaderLogger.debug(ex.getMessage());
			LiteLoaderLogger.debug(ClassTransformer.HORIZONTAL_RULE);
			LiteLoaderLogger.debug(json);
			LiteLoaderLogger.debug(ClassTransformer.HORIZONTAL_RULE);
			LiteLoaderLogger.severe(ex, "Invalid JSON event declarations for mod with id %s", identifier);
		}
		catch (Throwable ex)
		{
			LiteLoaderLogger.severe(ex, "Error whilst parsing event declarations for mod with id %s", identifier);
		}
		
		try
		{
			if (events != null)
			{
				LiteLoaderLogger.info("Registering events for mod with id %s", identifier);
				events.register(this);
			}
		}
		catch (Throwable ex)
		{
			LiteLoaderLogger.severe(ex, "Error whilst parsing event declarations for mod with id %s", identifier);
		}
	}
	
	protected Event registerEvent(Event event, MethodInfo targetMethod, InjectionPoint injectionPoint)
	{
		return super.addEvent(event, targetMethod, injectionPoint);
	}
	
	protected void registerAccessor(String interfaceName)
	{
		super.addAccessor(interfaceName);
	}
}
