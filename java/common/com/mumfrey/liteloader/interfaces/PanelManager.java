package com.mumfrey.liteloader.interfaces;

import com.mumfrey.liteloader.api.PostRenderObserver;
import com.mumfrey.liteloader.api.TickObserver;
import com.mumfrey.liteloader.core.LiteLoaderMods;
import com.mumfrey.liteloader.modconfig.ConfigManager;

/**
 * Interface for the liteloader panel manager, abstracted because we don't have the class GuiScreen on the server
 * 
 * @author Adam Mummery-Smith
 *
 * @param <TParentScreen> GuiScreen class, must be generic because we don't have GuiScreen on the server side
 */
public interface PanelManager<TParentScreen> extends TickObserver, PostRenderObserver
{
	/**
	 * @param mods
	 * @param configManager
	 */
	public abstract void init(LiteLoaderMods mods, ConfigManager configManager);

	/**
	 * 
	 */
	public abstract void hideTab();

	/**
	 * @param show
	 */
	public abstract void setTabVisible(boolean show);

	/**
	 * @return
	 */
	public abstract boolean isTabVisible();

	/**
	 * @param show
	 */
	public abstract void setTabAlwaysExpanded(boolean expand);
	
	/**
	 * @return
	 */
	public abstract boolean isTabAlwaysExpanded();

	/**
	 * @param parentScreen
	 */
	public abstract void displayLiteLoaderPanel(TParentScreen parentScreen);
	
	/**
	 * @return
	 */
	public abstract int getStartupErrorCount();
}
