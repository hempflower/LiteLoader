package com.mumfrey.liteloader;

/**
 * Interface for mods which want callbacks when the HUD is rendered
 * 
 * @author Adam Mummery-Smith
 */
public interface HUDRenderListener
{
	public abstract void onPreRenderHUD(int screenWidth, int screenHeight);
	
	public abstract void onPostRenderHUD(int screenWidth, int screenHeight);
}
