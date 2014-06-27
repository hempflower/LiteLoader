package com.mumfrey.liteloader.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;

import org.lwjgl.input.Keyboard;

import com.mumfrey.liteloader.client.gui.GuiLiteLoaderPanel;
import com.mumfrey.liteloader.common.GameEngine;
import com.mumfrey.liteloader.core.LiteLoaderMods;
import com.mumfrey.liteloader.interfaces.PanelManager;
import com.mumfrey.liteloader.launch.LoaderEnvironment;
import com.mumfrey.liteloader.launch.LoaderProperties;
import com.mumfrey.liteloader.modconfig.ConfigManager;

/**
 * Observer which handles the display of the mod panel
 * 
 * @author Adam Mummery-Smith
 */
public class LiteLoaderPanelManager implements PanelManager<GuiScreen>
{
	private static final String OPTION_MOD_INFO_SCREEN = "modInfoScreen";
	private static final String OPTION_NO_HIDE_TAB = "tabAlwaysExpanded"; 

	private final LoaderEnvironment environment;
	
	/**
	 * Loader Properties adapter 
	 */
	private final LoaderProperties properties;

	private LiteLoaderMods mods;

	private ConfigManager configManager;

	private Minecraft minecraft;

	/**
	 * Setting which determines whether we show the "mod info" screen tab in the main menu
	 */
	private boolean displayModInfoScreenTab = true;
	
	/**
	 * Don't hide t
	 */
	private boolean tabAlwaysExpanded = false;
	
	/**
	 * Override for the "mod info" tab setting, so that mods which want to handle the mod info themselves
	 * can temporarily disable the function without having to change the underlying property
	 */
	private boolean hideModInfoScreenTab = false;
	
	/**
	 * Active "mod info" screen, drawn as an overlay when in the main menu and made the active screen if
	 * the user clicks the tab
	 */
	private GuiLiteLoaderPanel panelHost;
	
	/**
	 * @param environment
	 * @param properties
	 */
	@SuppressWarnings("unchecked")
	public LiteLoaderPanelManager(GameEngine<?, ?> engine, LoaderEnvironment environment, LoaderProperties properties)
	{
		this.environment = environment;
		this.properties  = properties;
		this.minecraft   = ((GameEngine<Minecraft, ?>)engine).getClient();
		
		this.displayModInfoScreenTab = this.properties.getAndStoreBooleanProperty(LiteLoaderPanelManager.OPTION_MOD_INFO_SCREEN, true);
		this.tabAlwaysExpanded = this.properties.getAndStoreBooleanProperty(LiteLoaderPanelManager.OPTION_NO_HIDE_TAB, false);
	}
	
	@Override
	public void init(LiteLoaderMods mods, ConfigManager configManager)
	{
		this.mods          = mods;
		this.configManager = configManager;
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.api.TickObserver#onTick(boolean, float, boolean)
	 */
	@Override
	public void onTick(boolean clock, float partialTicks, boolean inGame)
	{
		if (clock && this.panelHost != null && this.minecraft.currentScreen != this.panelHost)
		{
			this.panelHost.updateScreen();
		}
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.api.PostRenderObserver#onPostRender(int, int, float)
	 */
	@Override
	public void onPostRender(int mouseX, int mouseY, float partialTicks)
	{
		if (this.mods == null) return;
		
		boolean tabHidden = this.hideModInfoScreenTab && this.minecraft.currentScreen instanceof GuiMainMenu;
		
		if (this.isPanelSupportedOnScreen(this.minecraft.currentScreen) && ((this.displayModInfoScreenTab && !tabHidden) || (this.panelHost != null && this.panelHost.isOpen())))
		{
			// If we're at the main menu, prepare the overlay
			if (this.panelHost == null || this.panelHost.getScreen() != this.minecraft.currentScreen)
			{
				this.panelHost = new GuiLiteLoaderPanel(this.minecraft, this.minecraft.currentScreen, this.mods, this.environment, this.configManager, !tabHidden);
			}

			this.minecraft.entityRenderer.setupOverlayRendering();
			this.panelHost.drawScreen(mouseX, mouseY, partialTicks, this.tabAlwaysExpanded);
		}
		else if (this.minecraft.currentScreen != this.panelHost && this.panelHost != null)
		{
			// If we're in any other screen, kill the overlay
			this.panelHost.release();
			this.panelHost = null;
		}
		else if (this.isPanelSupportedOnScreen(this.minecraft.currentScreen) && Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) && Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) && Keyboard.isKeyDown(Keyboard.KEY_TAB))
		{
			this.displayLiteLoaderPanel(this.minecraft.currentScreen);
		}
	}
	
	/**
	 * Set the "mod info" screen tab to hidden, regardless of the property setting
	 */
	@Override
	public void hideTab()
	{
		this.hideModInfoScreenTab = true;
	}
	
	/**
	 * Set whether the "mod info" screen tab should be shown in the main menu
	 */
	@Override
	public void setTabVisible(boolean show)
	{
		this.displayModInfoScreenTab = show;
		this.properties.setBooleanProperty(LiteLoaderPanelManager.OPTION_MOD_INFO_SCREEN, show);
		this.properties.writeProperties();
	}
	
	/**
	 * Get whether the "mod info" screen tab is shown in the main menu
	 */
	@Override
	public boolean isTabVisible()
	{
		return this.displayModInfoScreenTab;
	}
	
	@Override
	public void setTabAlwaysExpanded(boolean expand)
	{
		this.tabAlwaysExpanded = expand;
		this.properties.setBooleanProperty(LiteLoaderPanelManager.OPTION_NO_HIDE_TAB, expand);
		this.properties.writeProperties();
	}
	
	@Override
	public boolean isTabAlwaysExpanded()
	{
		return this.tabAlwaysExpanded;
	}
	
	/**
	 * Display the liteloader panel over the specified GUI
	 * 
	 * @param parentScreen
	 */
	@Override
	public void displayLiteLoaderPanel(GuiScreen parentScreen)
	{
		if (this.isPanelSupportedOnScreen(parentScreen))
		{
			this.panelHost = new GuiLiteLoaderPanel(this.minecraft, parentScreen, this.mods, this.environment, this.configManager, !this.hideModInfoScreenTab);
			this.minecraft.displayGuiScreen(this.panelHost);
		}
	}

	private boolean isPanelSupportedOnScreen(GuiScreen guiScreen)
	{
		return (
			guiScreen instanceof GuiMainMenu ||
			guiScreen instanceof GuiIngameMenu ||
			guiScreen instanceof GuiOptions
		);
	}
}
