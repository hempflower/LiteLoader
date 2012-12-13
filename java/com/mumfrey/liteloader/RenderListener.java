package com.mumfrey.liteloader;

import net.minecraft.src.GuiScreen;

/**
 * Interface for objects which want a pre-render callback
 * 
 * @author Adam Mummery-Smith
 */
public interface RenderListener extends LiteMod
{
	/**
	 * Callback when a frame is rendered
	 */
	public abstract void onRender();
	
	public abstract void onRenderGui(GuiScreen currentScreen);
	
	public abstract void onRenderWorld();
}
