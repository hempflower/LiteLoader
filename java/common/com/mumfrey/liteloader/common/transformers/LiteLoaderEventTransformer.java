package com.mumfrey.liteloader.common.transformers;

import static com.mumfrey.liteloader.core.runtime.Methods.*;

import com.mumfrey.liteloader.core.runtime.Obf;
import com.mumfrey.liteloader.transformers.event.Event;
import com.mumfrey.liteloader.transformers.event.EventInjectionTransformer;
import com.mumfrey.liteloader.transformers.event.InjectionPoint;
import com.mumfrey.liteloader.transformers.event.MethodInfo;
import com.mumfrey.liteloader.transformers.event.inject.BeforeNew;
import com.mumfrey.liteloader.transformers.event.inject.BeforeReturn;

/**
 * Injector for LiteLoader's common events
 *
 * @author Adam Mummery-Smith
 */
public abstract class LiteLoaderEventTransformer extends EventInjectionTransformer
{
	protected abstract Obf getProxy();
	
	@Override
	protected void addEvents()
	{
		// Event declaraions
		Event onInitializePlayerConnection     = Event.getOrCreate("onInitializePlayerConnection", false);
		Event onPlayerLogin                    = Event.getOrCreate("onPlayerLogin",                false);
		Event onPlayerLogout                   = Event.getOrCreate("onPlayerLogout",               false);
		Event onSpawnPlayer                    = Event.getOrCreate("onSpawnPlayer",                false);
		Event onRespawnPlayer                  = Event.getOrCreate("onRespawnPlayer",              false);
		Event onSessionProfileBad              = Event.getOrCreate("onSessionProfileBad",          true);
		
		// Injection Points
		InjectionPoint methodReturn            = new BeforeReturn();
		InjectionPoint beforeNewGameProfile    = new BeforeNew(1, Obf.GameProfile);
		
		// Hooks
		this.add(onInitializePlayerConnection, initPlayerConnection, (methodReturn),         "onInitializePlayerConnection");
		this.add(onPlayerLogin,                playerLoggedIn,       (methodReturn),         "onPlayerLogin");
		this.add(onPlayerLogout,               playerLoggedOut,      (methodReturn),         "onPlayerLogout");
		this.add(onSpawnPlayer,                spawnPlayer,          (methodReturn),         "onSpawnPlayer");
		this.add(onRespawnPlayer,              respawnPlayer,        (methodReturn),         "onRespawnPlayer");

		// Compatibility handlers
		this.add(onSessionProfileBad,          getProfile,           (beforeNewGameProfile), "generateOfflineUUID");
	}

	protected final Event add(Event event, MethodInfo targetMethod, InjectionPoint injectionPoint, String callback)
	{
		return this.add(event, targetMethod, injectionPoint, callback, this.getProxy());
	}

	protected Event add(Event event, MethodInfo targetMethod, InjectionPoint injectionPoint, String callback, Obf proxy)
	{
		return this.addEvent(event, targetMethod, injectionPoint).addListener(new MethodInfo(proxy, callback));
	}
}
