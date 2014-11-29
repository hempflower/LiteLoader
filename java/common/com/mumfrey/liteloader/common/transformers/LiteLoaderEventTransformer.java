package com.mumfrey.liteloader.common.transformers;

import static com.mumfrey.liteloader.core.runtime.Methods.*;
import static com.mumfrey.liteloader.transformers.event.InjectionPoint.*;

import org.objectweb.asm.Opcodes;

import com.mumfrey.liteloader.core.runtime.Obf;
import com.mumfrey.liteloader.transformers.event.Event;
import com.mumfrey.liteloader.transformers.event.EventInjectionTransformer;
import com.mumfrey.liteloader.transformers.event.InjectionPoint;
import com.mumfrey.liteloader.transformers.event.MethodInfo;
import com.mumfrey.liteloader.transformers.event.inject.BeforeFieldAccess;
import com.mumfrey.liteloader.transformers.event.inject.BeforeInvoke;
import com.mumfrey.liteloader.transformers.event.inject.BeforeNew;
import com.mumfrey.liteloader.transformers.event.inject.BeforeReturn;
import com.mumfrey.liteloader.transformers.event.inject.MethodHead;

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
		// Event declarations
		Event onInitializePlayerConnection     = Event.getOrCreate("onInitializePlayerConnection", false);
		Event onPlayerLogin                    = Event.getOrCreate("onPlayerLogin",                false);
		Event onPlayerLogout                   = Event.getOrCreate("onPlayerLogout",               false);
		Event onSpawnPlayer                    = Event.getOrCreate("onSpawnPlayer",                false);
		Event onRespawnPlayer                  = Event.getOrCreate("onRespawnPlayer",              false);
		Event onServerTick                     = Event.getOrCreate("onServerTick",                 false);
		Event onBlockClickedEvent              = Event.getOrCreate("onBlockClicked",               true);
		Event onActivateBlockOrUseItem         = Event.getOrCreate("onActivateBlockOrUseItem",     true);
		Event onPlayerDigging                  = Event.getOrCreate("onPlayerDigging",              true);
		Event onPlaceBlock                     = Event.getOrCreate("onPlaceBlock",                 true);
		Event onClickedAir                     = Event.getOrCreate("onClickedAir",                 true);
		Event onSessionProfileBad              = Event.getOrCreate("onSessionProfileBad",          true);
		Event onPlayerMoved                    = Event.getOrCreate("onPlayerMoved",                true);
		
		// Injection Points
		InjectionPoint methodHead              = new MethodHead();
		InjectionPoint methodReturn            = new BeforeReturn();
		InjectionPoint beforeNewGameProfile    = new BeforeNew(1, Obf.GameProfile);
		InjectionPoint beforeThreadMarshall    = new BeforeInvoke(checkThreadAndEnqueue);
		InjectionPoint beforeGetPosY           = new BeforeFieldAccess(Opcodes.GETFIELD, Obf.entityPosY, Obf.EntityPlayerMP, 4).setCaptureLocals(true);
		
		// Hooks
		this.add(onInitializePlayerConnection, initPlayerConnection,        (methodReturn),          "onInitializePlayerConnection");
		this.add(onPlayerLogin,                playerLoggedIn,              (methodReturn),          "onPlayerLogin");
		this.add(onPlayerLogout,               playerLoggedOut,             (methodReturn),          "onPlayerLogout");
		this.add(onSpawnPlayer,                spawnPlayer,                 (methodReturn),          "onSpawnPlayer");
		this.add(onRespawnPlayer,              respawnPlayer,               (methodReturn),          "onRespawnPlayer");
		this.add(onServerTick,                 serverJobs,                  (methodHead),            "onServerTick");
		this.add(onBlockClickedEvent,          onBlockClicked,              (methodHead),            "onBlockClicked");
		this.add(onActivateBlockOrUseItem,     activateBlockOrUseItem,      (methodHead),            "onUseItem");
		this.add(onPlaceBlock,                 processBlockPlacement,  after(beforeThreadMarshall) , "onPlaceBlock");
		this.add(onClickedAir,                 handleAnimation,        after(beforeThreadMarshall),  "onClickedAir");
		this.add(onPlayerDigging,              processPlayerDigging,   after(beforeThreadMarshall),  "onPlayerDigging");
		this.add(onPlayerMoved,                processPlayer,               (beforeGetPosY),         "onPlayerMoved");

		// Compatibility handlers
		this.add(onSessionProfileBad,          getProfile,                  (beforeNewGameProfile),  "generateOfflineUUID");
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
