package com.mumfrey.liteloader.api;

import net.minecraft.world.World;

/**
 * LiteLoader Extensible API - WorldObserver
 * 
 * WorldObservers receive a callback when the Minecraft.theWorld reference changes, beware the value is allowed
 * to be null
 * 
 * @author Adam Mummery-Smith
 */
public interface WorldObserver extends Observer
{
	public abstract void onWorldChanged(World world);
}
