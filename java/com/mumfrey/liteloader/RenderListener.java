package com.mumfrey.liteloader;

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
	
	public abstract void onRenderWorld();
}
