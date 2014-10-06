package com.mumfrey.liteloader.transformers.event.json;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Charsets;
import com.mumfrey.liteloader.api.EnumerationObserver;
import com.mumfrey.liteloader.core.ModInfo;
import com.mumfrey.liteloader.core.api.LoadableModFile;
import com.mumfrey.liteloader.interfaces.LoadableMod;
import com.mumfrey.liteloader.interfaces.LoaderEnumerator;
import com.mumfrey.liteloader.interfaces.TweakContainer;
import com.mumfrey.liteloader.interfaces.LoaderEnumerator.DisabledReason;
import com.mumfrey.liteloader.util.log.LiteLoaderLogger;

public class ModEvents implements EnumerationObserver
{
	private static final String DEFINITION_FILENAME = "events.json";
	
	private static Map<String, String> events = new HashMap<String, String>();
	
	@Override
	public void onRegisterEnabledContainer(LoaderEnumerator enumerator, LoadableMod<?> container)
	{
		if (container instanceof LoadableModFile)
		{
			LoadableModFile file = (LoadableModFile)container;
			if (!file.exists()) return;
			
			String json = file.getFileContents(ModEvents.DEFINITION_FILENAME, Charsets.UTF_8);
			if (json == null) return;
			
			LiteLoaderLogger.info("Registering %s for mod with id %s", ModEvents.DEFINITION_FILENAME, file.getIdentifier());
			ModEvents.events.put(file.getIdentifier(), json);
		}
	}

	@Override
	public void onRegisterDisabledContainer(LoaderEnumerator enumerator, LoadableMod<?> container, DisabledReason reason)
	{
	}

	@Override
	public void onRegisterTweakContainer(LoaderEnumerator enumerator, TweakContainer<File> container)
	{
	}
	
	@Override
	public void onModAdded(LoaderEnumerator enumerator, ModInfo<LoadableMod<?>> mod)
	{
	}
	
	static Map<String, String> getEvents()
	{
		return events;
	}
}
