package com.mumfrey.liteloader.client.gui;

import static com.mumfrey.liteloader.gl.GL.*;
import static com.mumfrey.liteloader.gl.GLClippingPlanes.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;

import org.lwjgl.input.Keyboard;

import com.mumfrey.liteloader.LiteMod;
import com.mumfrey.liteloader.api.ModInfoDecorator;
import com.mumfrey.liteloader.core.LiteLoaderMods;
import com.mumfrey.liteloader.core.ModInfo;
import com.mumfrey.liteloader.interfaces.Loadable;
import com.mumfrey.liteloader.interfaces.LoadableMod;
import com.mumfrey.liteloader.launch.LoaderEnvironment;
import com.mumfrey.liteloader.modconfig.ConfigManager;
import com.mumfrey.liteloader.modconfig.ConfigPanel;

/**
 * Mods panel
 * 
 * @author Adam Mummery-Smith
 */
public class GuiPanelMods extends GuiPanel
{	
	private static final int SCROLLBAR_WIDTH = 5;
	
	private final GuiLiteLoaderPanel parentScreen;

	private final ConfigManager configManager;

	/**
	 * List of enumerated mods
	 */
	private List<ModListEntry> mods = new ArrayList<ModListEntry>();
	
	/**
	 * Currently selected mod
	 */
	private ModListEntry selectedMod = null;
	
	/**
	 * Timer used to handle double-clicking on a mod
	 */
	private int doubleClickTime = 0;

	/**
	 * Enable / disable button
	 */
	private GuiButton btnToggle;
	
	/**
	 * Config button 
	 */
	private GuiButton btnConfig;
	
	/**
	 * Height of all the items in the list
	 */
	private int listHeight = 100;
	
	/**
	 * Scroll bar control for the mods list
	 */
	private GuiSimpleScrollBar scrollBar = new GuiSimpleScrollBar();

	private int brandColour;

	public GuiPanelMods(GuiLiteLoaderPanel parentScreen, Minecraft minecraft, LiteLoaderMods mods, LoaderEnvironment environment, ConfigManager configManager, int brandColour, List<ModInfoDecorator> decorators)
	{
		super(minecraft);
		
		this.parentScreen = parentScreen;
		this.configManager = configManager;
		this.brandColour = brandColour;
		
		this.populateModList(mods, environment, decorators);
	}
	
	/**
	 * Populate the mods list
	 * 
	 * @param mods
	 * @param environment
	 */
	private void populateModList(LiteLoaderMods mods, LoaderEnvironment environment, List<ModInfoDecorator> decorators)
	{
		// Add mods to this treeset first, in order to sort them
		Map<String, ModListEntry> sortedMods = new TreeMap<String, ModListEntry>();
		
		// Active mods
		for (ModInfo<LoadableMod<?>> mod : mods.getLoadedMods())
		{
			ModListEntry modListEntry = new ModListEntry(mods, environment, this.mc.fontRendererObj, this.brandColour, decorators, mod);
			sortedMods.put(modListEntry.getKey(), modListEntry);
		}
		
		// Disabled mods
		for (ModInfo<?> disabledMod : mods.getDisabledMods())
		{
			ModListEntry modListEntry = new ModListEntry(mods, environment, this.mc.fontRendererObj, this.brandColour, decorators, disabledMod);
			sortedMods.put(modListEntry.getKey(), modListEntry);
		}

		// Injected tweaks
		for (ModInfo<Loadable<?>> injectedTweak : mods.getInjectedTweaks())
		{
			ModListEntry modListEntry = new ModListEntry(mods, environment, this.mc.fontRendererObj, this.brandColour, decorators, injectedTweak);
			sortedMods.put(modListEntry.getKey(), modListEntry);
		}
		
		// Add the sorted mods to the mods list
		this.mods.addAll(sortedMods.values());
		
		// Select the first mod in the list
		if (this.mods.size() > 0)
			this.selectedMod = this.mods.get(0);
	}
	
	@Override
	boolean stealFocus()
	{
		return false;
	}

	@Override
	void setSize(int width, int height)
	{
		super.setSize(width, height);
		
		int rightPanelLeftEdge = MARGIN + 4 + (this.width - MARGIN - MARGIN - 4) / 2;
		
		this.controls.clear();
		this.controls.add(this.btnToggle = new GuiButton(0, rightPanelLeftEdge, this.height - GuiLiteLoaderPanel.PANEL_BOTTOM - 24, 90, 20, I18n.format("gui.enablemod")));
		this.controls.add(this.btnConfig = new GuiButton(1, rightPanelLeftEdge + 92, this.height - GuiLiteLoaderPanel.PANEL_BOTTOM - 24, 69, 20, I18n.format("gui.modsettings")));
		
		this.selectMod(this.selectedMod);
	}
	
	@Override
	void onTick()
	{
		if (this.doubleClickTime > 0)
			this.doubleClickTime--;
	}
	
	@Override
	void onHidden()
	{
	}
	
	@Override
	void onShown()
	{
	}
	
	@Override
	void mousePressed(int mouseX, int mouseY, int mouseButton)
	{
		if (mouseButton == 0)
		{
			if (this.scrollBar.wasMouseOver())
			{
				this.scrollBar.setDragging(true);
			}
			
			if (mouseY > GuiLiteLoaderPanel.PANEL_TOP && mouseY < this.height - GuiLiteLoaderPanel.PANEL_BOTTOM)
			{
				ModListEntry lastSelectedMod = this.selectedMod;
				
				for (ModListEntry mod : this.mods)
				{
					if (mod.getListPanel().isMouseOver())
					{
						this.selectMod(mod);
						
						if (mod.getListPanel().isMouseOverIcon())
						{
							mod.getListPanel().iconClick(this.parentScreen);
						}
						else
						{
							// handle double-click
							if (mod == lastSelectedMod && this.doubleClickTime > 0 && this.btnConfig.visible)
							{
								this.actionPerformed(this.btnConfig);
							}
						}
						
						this.doubleClickTime = 5;
					}
				}
				
				if (this.selectedMod != null && this.selectedMod == lastSelectedMod)
				{
					this.selectedMod.getInfoPanel().mousePressed();
				}
			}
		}

		super.mousePressed(mouseX, mouseY, mouseButton);
	}
	
	@Override
	void keyPressed(char keyChar, int keyCode)
	{
		if (keyCode == Keyboard.KEY_ESCAPE)
		{
			this.parentScreen.onToggled();
			return;
		}
		else if (keyCode == Keyboard.KEY_UP)
		{
			int selectedIndex = this.mods.indexOf(this.selectedMod) - 1;
			if (selectedIndex > -1) this.selectMod(this.mods.get(selectedIndex));
			this.scrollSelectedModIntoView();
		}
		else if (keyCode == Keyboard.KEY_DOWN)
		{
			int selectedIndex = this.mods.indexOf(this.selectedMod);
			if (selectedIndex > -1 && selectedIndex < this.mods.size() - 1) this.selectMod(this.mods.get(selectedIndex + 1));
			this.scrollSelectedModIntoView();
		}
		else if (keyCode == Keyboard.KEY_SPACE || keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER || keyCode == Keyboard.KEY_RIGHT)
		{
			this.toggleSelectedMod();
		}
		else if (keyCode == Keyboard.KEY_F3)
		{
			this.parentScreen.showLogPanel();
		}
		else if (keyCode == Keyboard.KEY_F1)
		{
			this.parentScreen.showAboutPanel();
		}
	}
	
	@Override
	void mouseMoved(int mouseX, int mouseY)
	{
	}
	
	@Override
	void mouseReleased(int mouseX, int mouseY, int mouseButton)
	{
		if (mouseButton == 0)
		{
			this.scrollBar.setDragging(false);
			
			if (this.selectedMod != null)
			{
				this.selectedMod.getInfoPanel().mouseReleased();
			}
		}
	}
	
	@Override
	void mouseWheelScrolled(int mouseWheelDelta)
	{
		if (this.selectedMod == null || !this.selectedMod.getInfoPanel().mouseWheelScrolled(mouseWheelDelta))
		{
			this.scrollBar.offsetValue(-mouseWheelDelta / 8);
		}
	}
	
	@Override
	void actionPerformed(GuiButton control)
	{
		if (control.id == 0)
		{
			this.toggleSelectedMod();
		}
		
		if (control.id == 1)
		{
			if (this.selectedMod != null && this.selectedMod.getModClass() != null)
			{
				ConfigPanel panel = this.configManager.getPanel(this.selectedMod.getModClass());
				LiteMod mod = this.selectedMod.getModInstance();
				this.parentScreen.openConfigPanel(panel, mod);
			}
		}
	}
	
	@Override
	void draw(int mouseX, int mouseY, float partialTicks)
	{
		this.parentScreen.drawInfoPanel(mouseX, mouseY, partialTicks, 0, GuiLiteLoaderPanel.PANEL_BOTTOM);
		
		int innerWidth = this.width - MARGIN - MARGIN - 4;
		int panelWidth = innerWidth / 2;
		int panelHeight = this.height - GuiLiteLoaderPanel.PANEL_BOTTOM - GuiLiteLoaderPanel.PANEL_TOP;
		
		this.drawModsList(mouseX, mouseY, partialTicks, panelWidth, panelHeight);
		this.drawSelectedMod(mouseX, mouseY, partialTicks, panelWidth, panelHeight);

		super.draw(mouseX, mouseY, partialTicks);
	}

	/**
	 * @param mouseX
	 * @param mouseY
	 * @param partialTicks
	 * @param width
	 * @param height
	 */
	private void drawModsList(int mouseX, int mouseY, float partialTicks, int width, int height)
	{
		this.scrollBar.drawScrollBar(mouseX, mouseY, partialTicks, MARGIN + width - SCROLLBAR_WIDTH, GuiLiteLoaderPanel.PANEL_TOP, SCROLLBAR_WIDTH, height, this.listHeight);

		// clip outside of scroll area
		glEnableClipping(MARGIN, MARGIN + width - SCROLLBAR_WIDTH - 1, GuiLiteLoaderPanel.PANEL_TOP, this.height - GuiLiteLoaderPanel.PANEL_BOTTOM);
		
		// handle scrolling
		glPushMatrix();
		glTranslatef(0.0F, GuiLiteLoaderPanel.PANEL_TOP - this.scrollBar.getValue(), 0.0F);
		
		mouseY -= (GuiLiteLoaderPanel.PANEL_TOP - this.scrollBar.getValue());
		
		int yPos = 0;
		for (ModListEntry mod : this.mods)
		{
			// drawListEntry returns a value indicating the height of the item drawn
			yPos += mod.getListPanel().draw(mouseX, mouseY, partialTicks, MARGIN, yPos, width - 6, mod == this.selectedMod);
		}
		
		yPos = 0;
		for (ModListEntry mod : this.mods)
		{
			yPos += mod.getListPanel().postRender(mouseX, mouseY, partialTicks, MARGIN, yPos, width - 6, mod == this.selectedMod);
		}
		
		glPopMatrix();
		glDisableClipping();
		
		this.listHeight = yPos;
		this.scrollBar.setMaxValue(this.listHeight - height);
	}

	/**
	 * @param mouseX
	 * @param mouseY
	 * @param partialTicks
	 * @param width
	 * @param height
	 */
	private void drawSelectedMod(int mouseX, int mouseY, float partialTicks, int width, int height)
	{
		if (this.selectedMod != null)
		{
			int left = MARGIN + width;
			int right = this.width - MARGIN;
			
			int spaceForButtons = this.btnConfig.visible || this.btnToggle.visible ? 28 : 0;
			glEnableClipping(left, right, GuiLiteLoaderPanel.PANEL_TOP, this.height - GuiLiteLoaderPanel.PANEL_BOTTOM - spaceForButtons);
			this.selectedMod.getInfoPanel().draw(mouseX, mouseY, partialTicks, left, GuiLiteLoaderPanel.PANEL_TOP, right - left, height - spaceForButtons);
			glDisableClipping();
		}
	}

	/**
	 * @param mod Mod list entry to select
	 */
	private void selectMod(ModListEntry mod)
	{
		if (this.selectedMod != null)
		{
			this.selectedMod.getInfoPanel().mouseReleased();
		}
		
		this.selectedMod = mod;
		this.btnToggle.visible = false;
		this.btnConfig.visible = false;
		
		if (this.selectedMod != null && this.selectedMod.canBeToggled())
		{
			this.btnToggle.visible = true;
			this.btnToggle.displayString = this.selectedMod.willBeEnabled() ? I18n.format("gui.disablemod") : I18n.format("gui.enablemod");
			
			this.btnConfig.visible = this.configManager.hasPanel(this.selectedMod.getModClass());
		}
	}

	/**
	 * Toggle the selected mod's enabled status
	 */
	private void toggleSelectedMod()
	{
		if (this.selectedMod != null)
		{
			this.selectedMod.toggleEnabled();
			this.selectMod(this.selectedMod);
		}
	}

	private void scrollSelectedModIntoView()
	{
		if (this.selectedMod == null) return;
		
		int yPos = 0;
		for (ModListEntry mod : this.mods)
		{
			if (mod == this.selectedMod) break;
			yPos += mod.getListPanel().getHeight();
		}
		
		// Mod is above the top of the visible window
		if (yPos < this.scrollBar.getValue())
		{
			this.scrollBar.setValue(yPos);
			return;
		}
		
		int panelHeight = this.height - GuiLiteLoaderPanel.PANEL_BOTTOM - GuiLiteLoaderPanel.PANEL_TOP;
		int modHeight = this.selectedMod.getListPanel().getHeight();
		
		// Mod is below the bottom of the visible window
		if (yPos - this.scrollBar.getValue() + modHeight > panelHeight)
		{
			this.scrollBar.setValue(yPos - panelHeight + modHeight);
		}
	}
}
