package com.mumfrey.liteloader.gui;

import static org.lwjgl.opengl.GL11.*;

import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.mumfrey.liteloader.LiteMod;
import com.mumfrey.liteloader.core.EnabledModsList;
import com.mumfrey.liteloader.core.LiteLoader;
import com.mumfrey.liteloader.core.LiteLoaderVersion;
import com.mumfrey.liteloader.core.Loadable;
import com.mumfrey.liteloader.modconfig.ConfigManager;
import com.mumfrey.liteloader.modconfig.ConfigPanel;

/**
 * GUI screen which displays info about loaded mods and also allows them to be enabled and
 * disabled. An instance of this class is created every time the main menu is displayed and is
 * drawn as an overlay until the tab is clicked, at which point it becomes the active GUI screen
 * and draws the parent main menu screen as its background to give the appearance of being
 * overlaid on the main menu. 
 *
 * @author Adam Mummery-Smith
 */
public class GuiScreenModInfo extends GuiScreen
{
	private static final int LEFT_EDGE       = 80;
	private static final int MARGIN          = 12;
	private static final int TAB_WIDTH       = 20;
	private static final int TAB_HEIGHT      = 40;
	private static final int TAB_TOP         = 20;
	private static final int PANEL_TOP       = 83;
	private static final int PANEL_BOTTOM    = 26;
	private static final int SCROLLBAR_WIDTH = 5;
	
	private static final double TWEEN_RATE = 0.08;

	/**
	 * Used for clipping
	 */
	private static DoubleBuffer doubleBuffer = BufferUtils.createByteBuffer(64).asDoubleBuffer();
	
	// Texture resources for the "about mods" screen
	public static ResourceLocation aboutTextureResource = new ResourceLocation("liteloader", "textures/gui/about.png");

	/**
	 * Reference to the main menu which this screen is either overlaying or using as its background
	 */
	private GuiScreen mainMenu;
	
	/**
	 * Tick number (update counter) used for tweening
	 */
	private long tickNumber;
	
	/**
	 * Last tick number, for tweening
	 */
	private double lastTick;
	
	/**
	 * Current tween percentage (0.0 -> 1.0)
	 */
	private double tweenAmount = 0.0;
	
	/**
	 * Since we don't get real mouse events we have to simulate them by tracking the mouse state
	 */
	private boolean mouseDown, toggled;
	
	/**
	 * Timer used to handle double-clicking on a mod
	 */
	private int doubleClickTime = 0;
	
	/**
	 * Hover opacity for the tab
	 */
	private float tabOpacity = 0.0F;
	
	private boolean hideTab = true;
	
	/**
	 * List of enumerated mods
	 */
	private List<GuiModListEntry> mods = new ArrayList<GuiModListEntry>();
	
	/**
	 * Currently selected mod
	 */
	private GuiModListEntry selectedMod = null;
	
	/**
	 * Text to display under the header
	 */
	private String activeModText, versionText;
	
	/**
	 * Height of all the items in the list
	 */
	private int listHeight = 100;
	
	/**
	 * Scroll bar control for the mods list
	 */
	private GuiSimpleScrollBar scrollBar = new GuiSimpleScrollBar();
	
	/**
	 * Enable / disable button
	 */
	private GuiButton btnToggle;
	
	/**
	 * Config button 
	 */
	private GuiButton btnConfig;
	
	/**
	 * Enable the mod info tab checkbox
	 */
	private GuiCheckbox chkEnabled;
	
	private ConfigManager configManager;
	
	/**
	 * Configuration panel
	 */
	private ModInfoScreenPanel currentPanel;
	
	/**
	 * @param minecraft
	 * @param mainMenu
	 * @param loader
	 * @param enabledModsList
	 */
	public GuiScreenModInfo(Minecraft minecraft, GuiScreen mainMenu, LiteLoader loader, EnabledModsList enabledModsList, ConfigManager configManager, boolean hideTab)
	{
		this.mc = minecraft;
		this.fontRendererObj = minecraft.fontRenderer;
		this.mainMenu = mainMenu;
		this.configManager = configManager;
		this.hideTab = hideTab;
		
		this.versionText = I18n.format("gui.about.versiontext", LiteLoader.getVersion());

		this.populateModList(loader, enabledModsList);
	}
	
	/**
	 * Populate the mods list
	 * 
	 * @param loader
	 * @param enabledModsList
	 */
	private void populateModList(LiteLoader loader, EnabledModsList enabledModsList)
	{
		this.activeModText = I18n.format("gui.about.modsloaded", loader.getLoadedMods().size());
		
		// Add mods to this treeset first, in order to sort them
		Map<String, GuiModListEntry> sortedMods = new TreeMap<String, GuiModListEntry>();
		
		// Active mods
		for (LiteMod mod : loader.getLoadedMods())
		{
			GuiModListEntry modListEntry = new GuiModListEntry(loader, enabledModsList, this.mc.fontRenderer, mod);
			sortedMods.put(modListEntry.getKey(), modListEntry);
		}
		
		// Disabled mods
		for (Loadable<?> disabledMod : loader.getDisabledMods())
		{
			GuiModListEntry modListEntry = new GuiModListEntry(loader, enabledModsList, this.mc.fontRenderer, disabledMod);
			sortedMods.put(modListEntry.getKey(), modListEntry);
		}

		// Injected tweaks
		for (Loadable<?> injectedTweak : loader.getInjectedTweaks())
		{
			GuiModListEntry modListEntry = new GuiModListEntry(loader, enabledModsList, this.mc.fontRenderer, injectedTweak);
			sortedMods.put(modListEntry.getKey(), modListEntry);
		}
		
		// Add the sorted mods to the mods list
		this.mods.addAll(sortedMods.values());
		
		// Select the first mod in the list
		if (this.mods.size() > 0)
			this.selectedMod = this.mods.get(0);
	}

	/**
	 * Release references prior to being disposed
	 */
	public void release()
	{
		this.mainMenu = null;
	}

	/**
	 * Get the parent menu
	 */
	public GuiScreen getScreen()
	{
		return this.mainMenu;
	}

	/**
	 * @return
	 */
	public boolean isTweeningOrOpen()
	{
		return this.tweenAmount > 0.0;
	}

	/* (non-Javadoc)
	 * @see net.minecraft.client.gui.GuiScreen#initGui()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void initGui()
	{
		if (this.currentPanel != null)
		{
			this.currentPanel.setSize(this.width - LEFT_EDGE, this.height);
		}
		
		int rightPanelLeftEdge = LEFT_EDGE + MARGIN + 4 + (this.width - LEFT_EDGE - MARGIN - MARGIN - 4) / 2;
		
		this.buttonList.clear();
		this.buttonList.add(this.btnToggle = new GuiButton(0, rightPanelLeftEdge, this.height - PANEL_BOTTOM - 24, 90, 20, I18n.format("gui.enablemod")));
		this.buttonList.add(this.btnConfig = new GuiButton(1, rightPanelLeftEdge + 92, this.height - PANEL_BOTTOM - 24, 69, 20, I18n.format("gui.modsettings")));
		if (!this.hideTab)
		{
			this.buttonList.add(this.chkEnabled = new GuiCheckbox(2, LEFT_EDGE + MARGIN, this.height - PANEL_BOTTOM + 9, I18n.format("gui.about.showtabmessage")));
		}
		
		this.buttonList.add(new GuiHoverLabel(3, LEFT_EDGE + MARGIN + 38 + this.fontRendererObj.getStringWidth(this.versionText) + 6, 50, this.fontRendererObj, I18n.format("gui.about.checkupdates")));
		
		this.selectMod(this.selectedMod);

		Keyboard.enableRepeatEvents(true);
	}
	
	@Override
	public void onGuiClosed()
	{
		Keyboard.enableRepeatEvents(false);
	}
	
	/* (non-Javadoc)
	 * @see net.minecraft.client.gui.GuiScreen#setWorldAndResolution(net.minecraft.client.Minecraft, int, int)
	 */
	@Override
	public void setWorldAndResolution(Minecraft minecraft, int width, int height)
	{
		if (this.mc.currentScreen == this)
		{
			// Set res in parent screen if we are the active GUI
			this.mainMenu.setWorldAndResolution(minecraft, width, height);
		}
		
		super.setWorldAndResolution(minecraft, width, height);
	}
	
	/* (non-Javadoc)
	 * @see net.minecraft.client.gui.GuiScreen#updateScreen()
	 */
	@Override
	public void updateScreen()
	{
		if (this.currentPanel != null)
		{
			this.currentPanel.onTick();
		}
		
		this.tickNumber++;
		
		if (this.mc.currentScreen == this)
		{
			this.mc.currentScreen = this.mainMenu;
			this.mainMenu.updateScreen();
			this.mc.currentScreen = this;
			if (this.chkEnabled != null) this.chkEnabled.checked = LiteLoader.getInstance().getDisplayModInfoScreenTab();
		}
		
		if (this.toggled)
		{
			this.onToggled();
		}
		
		if (this.doubleClickTime > 0)
			this.doubleClickTime--;
	}
	
	/* (non-Javadoc)
	 * @see net.minecraft.client.gui.GuiScreen#drawScreen(int, int, float)
	 */
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks)
	{
		boolean active = this.mc.currentScreen == this;
		
		if (active)
		{
			// Draw the parent screen as our background if we are the active screen
			glClear(GL_DEPTH_BUFFER_BIT);
			this.mainMenu.drawScreen(-10, -10, partialTicks);
			glClear(GL_DEPTH_BUFFER_BIT);
		}
		else
		{
			// If this is not the active screen, copy the width and height from the parent GUI
			this.width = this.mainMenu.width;
			this.height = this.mainMenu.height;
		}

		// Calculate the current tween position
		float xOffset = (this.width - LEFT_EDGE) * this.calcTween(partialTicks, active) + 16.0F + (this.tabOpacity * -32.0F);
		int offsetMouseX = mouseX - (int)xOffset;
		
		// Handle mouse stuff here since we won't get mouse events when not the active GUI
		boolean mouseOverTab = !this.hideTab && (offsetMouseX > LEFT_EDGE - TAB_WIDTH && offsetMouseX < LEFT_EDGE && mouseY > TAB_TOP && mouseY < TAB_TOP + TAB_HEIGHT);
		this.handleMouseClick(offsetMouseX, mouseY, partialTicks, active, mouseOverTab);
		
		// Calculate the tab opacity, not framerate adjusted because we don't really care
		this.tabOpacity = mouseOverTab || this.isTweeningOrOpen() ? 0.5F : Math.max(0.0F, this.tabOpacity - partialTicks * 0.1F);
		
		// Draw the panel contents
		this.drawPanel(offsetMouseX, mouseY, partialTicks, active, xOffset);
		
		if (mouseOverTab && this.tweenAmount < 0.01)
		{
			GuiScreenModInfo.drawTooltip(this.fontRendererObj, LiteLoader.getVersionDisplayString(), mouseX, mouseY, this.width, this.height, 0xFFFFFF, 0xB0000000);
			GuiScreenModInfo.drawTooltip(this.fontRendererObj, this.activeModText, mouseX, mouseY + 13, this.width, this.height, 0xCCCCCC, 0xB0000000);
		}
	}

	/**
	 * @param mouseX
	 * @param mouseY
	 * @param partialTicks
	 * @param active
	 * @param xOffset
	 */
	private void drawPanel(int mouseX, int mouseY, float partialTicks, boolean active, float xOffset)
	{
		glPushMatrix();
		glTranslatef(xOffset, 0.0F, 0.0F);
		
		// Draw the background and left edge
		drawRect(LEFT_EDGE, 0, this.width, this.height, 0xB0000000);
		
		if (!this.hideTab)
		{
			drawRect(LEFT_EDGE, 0, LEFT_EDGE + 1, TAB_TOP, 0xFFFFFFFF);
			drawRect(LEFT_EDGE, TAB_TOP + TAB_HEIGHT, LEFT_EDGE + 1, this.height, 0xFFFFFFFF);
			
			this.mc.getTextureManager().bindTexture(aboutTextureResource);
			glDrawTexturedRect(LEFT_EDGE - TAB_WIDTH, TAB_TOP, TAB_WIDTH + 1, TAB_HEIGHT, 80, 80, 122, 160, 0.5F + this.tabOpacity);
		}
		else
		{
			drawRect(LEFT_EDGE, 0, LEFT_EDGE + 1, this.height, 0xFFFFFFFF);
			this.mc.getTextureManager().bindTexture(aboutTextureResource);
		}

		// Only draw the panel contents if we are actually open
		if (this.isTweeningOrOpen())
		{
			if (this.currentPanel != null && this.currentPanel.isCloseRequested())
			{
				this.closeCurrentPanel();
			}

			if (this.currentPanel != null)
			{
				this.drawCurrentPanel(mouseX, mouseY, partialTicks);
			}
			else
			{
				this.drawInfoPanel(mouseX, mouseY, partialTicks, LEFT_EDGE, PANEL_BOTTOM);
				
				int innerWidth = this.width - LEFT_EDGE - MARGIN - MARGIN - 4;
				int panelWidth = innerWidth / 2;
				int panelHeight = this.height - PANEL_BOTTOM - PANEL_TOP;
				
				this.drawModsList(mouseX, mouseY, partialTicks, panelWidth, panelHeight);
				this.drawSelectedMod(mouseX, mouseY, partialTicks, panelWidth, panelHeight);
				
				// Draw other controls inside the transform so that they slide properly
				super.drawScreen(mouseX, mouseY, partialTicks);
			}
		}
		else if (this.currentPanel != null)
		{
			this.closeCurrentPanel();
		}
		
		glPopMatrix();
	}

	/**
	 * @param mouseX
	 * @param mouseY
	 * @param partialTicks
	 */
	private void drawCurrentPanel(int mouseX, int mouseY, float partialTicks)
	{
		glPushMatrix();
		glTranslatef(LEFT_EDGE, 0, 0);
		
		this.currentPanel.draw(mouseX - LEFT_EDGE, mouseY, partialTicks);
		
		glPopMatrix();
	}

	/**
	 * @param mouseX
	 * @param mouseY
	 * @param partialTicks
	 */
	protected void drawInfoPanel(int mouseX, int mouseY, float partialTicks, int left, int bottom)
	{
		int right = this.width - MARGIN - LEFT_EDGE + left;
		
		// Draw the header pieces
		glDrawTexturedRect(left + MARGIN, 12, 128, 40, 0, 0, 256, 80, 1.0F); // liteloader logo
		glDrawTexturedRect(right - 32, 12, 32, 45, 0, 80, 64, 170, 1.0F); // chicken
		
		// Draw header text
		this.fontRendererObj.drawString(this.versionText, left + MARGIN + 38, 50, 0xFFFFFFFF);
		this.fontRendererObj.drawString(this.activeModText, left + MARGIN + 38, 60, 0xFFAAAAAA);
		
		// Draw top and bottom horizontal rules
		drawRect(left + MARGIN, 80, right, 81, 0xFF999999);
		drawRect(left + MARGIN, this.height - bottom + 2, right, this.height - bottom + 3, 0xFF999999);
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
		this.scrollBar.drawScrollBar(mouseX, mouseY, partialTicks, LEFT_EDGE + MARGIN + width - SCROLLBAR_WIDTH, PANEL_TOP, SCROLLBAR_WIDTH, height, this.listHeight);

		// clip outside of scroll area
		glEnableClipping(LEFT_EDGE + MARGIN, LEFT_EDGE + MARGIN + width - SCROLLBAR_WIDTH - 1, PANEL_TOP, this.height - PANEL_BOTTOM);
		
		// handle scrolling
		glPushMatrix();
		glTranslatef(0.0F, PANEL_TOP - this.scrollBar.getValue(), 0.0F);
		
		mouseY -= (PANEL_TOP - this.scrollBar.getValue());
		
		int yPos = 0;
		for (GuiModListEntry mod : this.mods)
		{
			// drawListEntry returns a value indicating the height of the item drawn
			yPos += mod.drawListEntry(mouseX, mouseY, partialTicks, LEFT_EDGE + MARGIN, yPos, width - 6, mod == this.selectedMod);
		}
		
		yPos = 0;
		for (GuiModListEntry mod : this.mods)
		{
			yPos += mod.postRenderListEntry(mouseX, mouseY, partialTicks, LEFT_EDGE + MARGIN, yPos, width - 6, mod == this.selectedMod);
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
			int left = LEFT_EDGE + MARGIN + width;
			int right = this.width - MARGIN;
			
			int spaceForButtons = this.btnConfig.field_146125_m || this.btnToggle.field_146125_m ? 28 : 0;
			glEnableClipping(left, right, PANEL_TOP, this.height - PANEL_BOTTOM - spaceForButtons);
			this.selectedMod.drawInfo(mouseX, mouseY, partialTicks, left, PANEL_TOP, right - left, height - spaceForButtons);
			glDisableClipping();
		}
	}

	/**
	 * @param mod
	 * @return
	 */
	private void selectMod(GuiModListEntry mod)
	{
		if (this.selectedMod != null)
		{
			this.selectedMod.mouseReleased();
		}
		
		this.selectedMod = mod;
		this.btnToggle.field_146125_m = false;
		this.btnConfig.field_146125_m = false;
		
		if (this.selectedMod != null && this.selectedMod.canBeToggled())
		{
			this.btnToggle.field_146125_m = true;
			this.btnToggle.displayString = this.selectedMod.willBeEnabled() ? I18n.format("gui.disablemod") : I18n.format("gui.enablemod");
			
			this.btnConfig.field_146125_m = this.configManager.hasPanel(this.selectedMod.getModClass());
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
	
	/* (non-Javadoc)
	 * @see net.minecraft.client.gui.GuiScreen#actionPerformed(net.minecraft.client.gui.GuiButton)
	 */
	@Override
	protected void actionPerformed(GuiButton button)
	{
		if (button.id == 0)
		{
			this.toggleSelectedMod();
		}
		
		if (button.id == 1)
		{
			this.openConfigPanel();
		}
		
		if (button.id == 2 && this.chkEnabled != null)
		{
			this.chkEnabled.checked = !this.chkEnabled.checked;
			LiteLoader.getInstance().setDisplayModInfoScreenTab(this.chkEnabled.checked);
			
			if (!this.chkEnabled.checked)
			{
				this.chkEnabled.displayString = I18n.format("gui.about.showtabmessage") + I18n.format("gui.about.keystrokehint");
			}
		}
		
		if (button.id == 3)
		{
			this.setCurrentPanel(new GuiCheckUpdatePanel(this.mc, LiteLoaderVersion.getUpdateSite(), "LiteLoader"));
		}
	}
	
	/* (non-Javadoc)
	 * @see net.minecraft.client.gui.GuiScreen#keyTyped(char, int)
	 */
	@Override
	protected void keyTyped(char keyChar, int keyCode)
	{
		if (this.currentPanel != null)
		{
			this.currentPanel.keyPressed(keyChar, keyCode);
			return;
		}
		
		if (keyCode == Keyboard.KEY_ESCAPE)
		{
			this.onToggled();
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
			this.setCurrentPanel(new GuiLiteLoaderLog(this.mc));
		}
		else if (keyCode == Keyboard.KEY_F1)
		{
			this.setCurrentPanel(new GuiAboutPanel(this.mc, this));
		}
	}
	
	private void scrollSelectedModIntoView()
	{
		if (this.selectedMod == null) return;
		
		int yPos = 0;
		for (GuiModListEntry mod : this.mods)
		{
			if (mod == this.selectedMod) break;
			yPos += mod.getHeight();
		}
		
		// Mod is above the top of the visible window
		if (yPos < this.scrollBar.getValue())
		{
			this.scrollBar.setValue(yPos);
			return;
		}
		
		int panelHeight = this.height - PANEL_BOTTOM - PANEL_TOP;
		int modHeight = this.selectedMod.getHeight();
		
		// Mod is below the bottom of the visible window
		if (yPos - this.scrollBar.getValue() + modHeight > panelHeight)
		{
			this.scrollBar.setValue(yPos - panelHeight + modHeight);
		}
	}

	/* (non-Javadoc)
	 * @see net.minecraft.client.gui.GuiScreen#mouseClicked(int, int, int)
	 */
	@Override
	protected void mouseClicked(int mouseX, int mouseY, int button)
	{
		if (this.currentPanel != null)
		{
			this.currentPanel.mousePressed(mouseX - LEFT_EDGE, mouseY, button);
			return;
		}
		
		if (button == 0)
		{
			if (this.scrollBar.wasMouseOver())
			{
				this.scrollBar.setDragging(true);
			}
			
			if (mouseY > PANEL_TOP && mouseY < this.height - PANEL_BOTTOM)
			{
				GuiModListEntry lastSelectedMod = this.selectedMod;
				
				for (GuiModListEntry mod : this.mods)
				{
					if (mod.mouseWasOverListEntry())
					{
						this.selectMod(mod);
						
						// handle double-click
						if (mod == lastSelectedMod && this.doubleClickTime > 0 && this.btnConfig.field_146125_m)
						{
							this.actionPerformed(this.btnConfig);
						}
						
						this.doubleClickTime = 5;
					}
				}
				
				if (this.selectedMod != null && this.selectedMod == lastSelectedMod)
				{
					this.selectedMod.mousePressed();
				}
			}
		}
		
		super.mouseClicked(mouseX, mouseY, button);
	}
	
	/* (non-Javadoc)
	 * @see net.minecraft.client.gui.GuiScreen#mouseMovedOrUp(int, int, int)
	 */
	@Override
	protected void mouseMovedOrUp(int mouseX, int mouseY, int button)
	{
		if (this.currentPanel != null)
		{
			if (button == -1)
				this.currentPanel.mouseMoved(mouseX - LEFT_EDGE, mouseY);
			else
				this.currentPanel.mouseReleased(mouseX - LEFT_EDGE, mouseY, button);
			
			return;
		}
		
		if (button == 0)
		{
			this.scrollBar.setDragging(false);
			
			if (this.selectedMod != null)
			{
				this.selectedMod.mouseReleased();
			}
		}
		
		super.mouseMovedOrUp(mouseX, mouseY, button);
	}
	
	/* (non-Javadoc)
	 * @see net.minecraft.client.gui.GuiScreen#handleMouseInput()
	 */
	@Override
	public void handleMouseInput()
	{
		int mouseWheelDelta = Mouse.getEventDWheel();
		if (mouseWheelDelta != 0)
		{
			if (this.currentPanel != null)
				this.currentPanel.mouseWheelScrolled(mouseWheelDelta);
			else
				mouseWheelScrolled(mouseWheelDelta / 8);
		}
		
		super.handleMouseInput();
	}

	/**
	 * @param mouseWheelDelta
	 */
	private void mouseWheelScrolled(int mouseWheelDelta)
	{
		if (this.selectedMod == null || !this.selectedMod.mouseWheelScrolled(mouseWheelDelta))
		{
			this.scrollBar.offsetValue(-mouseWheelDelta);
		}
	}

	/**
	 * @param mouseX
	 * @param active
	 * @param mouseOverTab
	 */
	public void handleMouseClick(int mouseX, int mouseY, float partialTicks, boolean active, boolean mouseOverTab)
	{
		boolean mouseDown = Mouse.isButtonDown(0);
		if (((active && mouseX < LEFT_EDGE && this.tweenAmount > 0.75) || mouseOverTab) && !this.mouseDown && mouseDown)
		{
			this.mouseDown = true;
			this.toggled = true;
		}
		else if (this.mouseDown && !mouseDown)
		{
			this.mouseDown = false;
		}
	}

	/**
	 * @param partialTicks
	 * @param active
	 * @return
	 */
	private float calcTween(float partialTicks, boolean active)
	{
		double tickValue = this.tickNumber + partialTicks;
		
		if (active && this.tweenAmount < 1.0)
		{
			this.tweenAmount = Math.min(1.0, this.tweenAmount + ((tickValue - this.lastTick) * TWEEN_RATE));
		}
		else if (!active && this.isTweeningOrOpen())
		{
			this.tweenAmount = Math.max(0.0, this.tweenAmount - ((tickValue - this.lastTick) * TWEEN_RATE));
		}
		
		this.lastTick = tickValue;
		return 1.0F - (float)Math.sin(this.tweenAmount * 0.5 * Math.PI);
	}

	/**
	 * Called when the tab is clicked
	 */
	private void onToggled()
	{
		this.toggled = false;
		this.mc.displayGuiScreen(this.mc.currentScreen == this ? this.mainMenu : this);
	}
	
	/**
	 * Callback for the "config" button, display the config panel for the currently selected mod
	 */
	private void openConfigPanel()
	{
		if (this.selectedMod != null && this.selectedMod.getModClass() != null)
		{
			ConfigPanel panel = this.configManager.getPanel(this.selectedMod.getModClass());
			if (panel != null)
			{
				this.setCurrentPanel(new GuiConfigPanelContainer(this.mc, panel, this.selectedMod.getModInstance()));
			}
		}
	}

	/**
	 * @param newPanel
	 */
	private void setCurrentPanel(ModInfoScreenPanel newPanel)
	{
		this.closeCurrentPanel();
		
		this.currentPanel = newPanel;
		this.currentPanel.setSize(this.width - LEFT_EDGE, this.height);
		this.currentPanel.onShown();
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.modconfig.ConfigPanelHost#close()
	 */
	private void closeCurrentPanel()
	{
		if (this.currentPanel != null)
		{
			this.currentPanel.onHidden();
			this.currentPanel = null;
		}
	}
	
	public final static boolean isSupportedOnScreen(GuiScreen guiScreen)
	{
		return (
			guiScreen instanceof GuiMainMenu ||
			guiScreen instanceof GuiIngameMenu ||
			guiScreen instanceof GuiOptions
		);
	}
	
	/**
	 * Draw a tooltip at the specified location and clip to screenWidth and screenHeight
	 * 
	 * @param fontRenderer
	 * @param tooltipText
	 * @param mouseX
	 * @param mouseY
	 * @param screenWidth
	 * @param screenHeight
	 * @param colour
	 * @param backgroundColour
	 */
	protected static void drawTooltip(FontRenderer fontRenderer, String tooltipText, int mouseX, int mouseY, int screenWidth, int screenHeight, int colour, int backgroundColour)
	{
		int textSize = fontRenderer.getStringWidth(tooltipText);
		mouseX = Math.max(0, Math.min(screenWidth - 4, mouseX - 4));
		mouseY = Math.max(0, Math.min(screenHeight - 16, mouseY));
		drawRect(mouseX - textSize - 2, mouseY, mouseX + 2, mouseY + 12, backgroundColour);
		fontRenderer.drawStringWithShadow(tooltipText, mouseX - textSize, mouseY + 2, colour);
	}

	
	/**
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 * @param u
	 * @param v
	 * @param u2
	 * @param v2
	 * @param alpha
	 */
	static void glDrawTexturedRect(int x, int y, int width, int height, int u, int v, int u2, int v2, float alpha)
	{
		glDisable(GL_LIGHTING);
		glEnable(GL_BLEND);
		glAlphaFunc(GL_GREATER, 0.0F);
		glEnable(GL_TEXTURE_2D);
		glColor4f(1.0F, 1.0F, 1.0F, alpha);
		
		float texMapScale = 0.00390625F; // 256px
		
		Tessellator tessellator = Tessellator.instance;
		tessellator.startDrawingQuads();
		tessellator.addVertexWithUV(x + 0,     y + height, 0, u  * texMapScale, v2 * texMapScale);
		tessellator.addVertexWithUV(x + width, y + height, 0, u2 * texMapScale, v2 * texMapScale);
		tessellator.addVertexWithUV(x + width, y + 0,      0, u2 * texMapScale, v  * texMapScale);
		tessellator.addVertexWithUV(x + 0,     y + 0,      0, u  * texMapScale, v  * texMapScale);
		tessellator.draw();
		
		glDisable(GL_BLEND);
		glAlphaFunc(GL_GREATER, 0.01F);
	}
	
	/**
	 * Enable OpenGL clipping planes (uses planes 2, 3, 4 and 5)
	 * 
	 * @param xLeft Left edge clip or -1 to not use this plane
	 * @param xRight Right edge clip or -1 to not use this plane
	 * @param yTop Top edge clip or -1 to not use this plane
	 * @param yBottom Bottom edge clip or -1 to not use this plane
	 */
	final static void glEnableClipping(int xLeft, int xRight, int yTop, int yBottom)
	{
		// Apply left edge clipping if specified
		if (xLeft != -1)
		{
			doubleBuffer.clear();
			doubleBuffer.put(1).put(0).put(0).put(-xLeft).flip();
			glClipPlane(GL_CLIP_PLANE2, doubleBuffer);
			glEnable(GL_CLIP_PLANE2);
		}
		
		// Apply right edge clipping if specified
		if (xRight != -1)
		{
			doubleBuffer.clear();
			doubleBuffer.put(-1).put(0).put(0).put(xRight).flip();
			glClipPlane(GL_CLIP_PLANE3, doubleBuffer);
			glEnable(GL_CLIP_PLANE3);
		}
		
		// Apply top edge clipping if specified
		if (yTop != -1)
		{
			doubleBuffer.clear();
			doubleBuffer.put(0).put(1).put(0).put(-yTop).flip();
			glClipPlane(GL_CLIP_PLANE4, doubleBuffer);
			glEnable(GL_CLIP_PLANE4);
		}
		
		// Apply bottom edge clipping if specified
		if (yBottom != -1)
		{
			doubleBuffer.clear();
			doubleBuffer.put(0).put(-1).put(0).put(yBottom).flip();
			glClipPlane(GL_CLIP_PLANE5, doubleBuffer);
			glEnable(GL_CLIP_PLANE5);
		}
	}
	
	/**
	 * Disable OpenGL clipping planes (uses planes 2, 3, 4 and 5)
	 */
	final static void glDisableClipping()
	{
		glDisable(GL_CLIP_PLANE5);
		glDisable(GL_CLIP_PLANE4);
		glDisable(GL_CLIP_PLANE3);
		glDisable(GL_CLIP_PLANE2);
	}
}