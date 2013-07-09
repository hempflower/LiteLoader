package com.mumfrey.liteloader;

import net.minecraft.src.Minecraft;

/**
 * Interface for mods which want a frame notification every single game loop
 * 
 * @author Adam Mummery-Smith
 */
public interface GameLoopListener
{
	/**
	 * Called every frame, before the world is ticked
	 * 
	 * @param minecraft
	 */
	public abstract void onRunGameLoop(Minecraft minecraft);
}
