package com.mumfrey.liteloader.gui;

import static org.lwjgl.opengl.GL11.*;

import java.awt.image.BufferedImage;
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.imageio.ImageIO;

import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.mumfrey.liteloader.LiteMod;
import com.mumfrey.liteloader.core.EnabledModsList;
import com.mumfrey.liteloader.core.LiteLoader;
import com.mumfrey.liteloader.core.ModFile;
import com.mumfrey.liteloader.modconfig.ConfigPanel;
import com.mumfrey.liteloader.modconfig.ConfigPanelManager;

import net.minecraft.src.DynamicTexture;
import net.minecraft.src.GuiButton;
import net.minecraft.src.GuiMainMenu;
import net.minecraft.src.GuiScreen;
import net.minecraft.src.Minecraft;
import net.minecraft.src.ResourceLocation;
import net.minecraft.src.Tessellator;

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
	
	// Texture resources for the "about mods" screen, we load the texture directly anyway so it won't be from an RP
	public static ResourceLocation aboutTextureResource;
	private static DynamicTexture aboutTexture;

	/**
	 * Reference to the main menu which this screen is either overlaying or using as its background
	 */
	private GuiMainMenu mainMenu;
	
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
	private String activeModText = "0 mod(s) loaded";
	
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
	
	private ConfigPanelManager configPanelManager;
	
	/**
	 * Configuration panel
	 */
	private GuiConfigPanelContainer configPanel;
	
	/**
	 * @param minecraft
	 * @param mainMenu
	 * @param loader
	 * @param enabledModsList
	 */
	public GuiScreenModInfo(Minecraft minecraft, GuiMainMenu mainMenu, LiteLoader loader, EnabledModsList enabledModsList, ConfigPanelManager configPanelManager)
	{
		this.mc = minecraft;
		this.mainMenu = mainMenu;
		this.configPanelManager = configPanelManager;
		
		// Spawn the texture resource if we haven't already
		if (aboutTexture == null)
		{
			try
			{
				BufferedImage aboutImage = ImageIO.read(GuiScreenModInfo.class.getResourceAsStream("/assets/liteloader/textures/gui/about.png"));
				aboutTexture = new DynamicTexture(aboutImage);
				aboutTextureResource = minecraft.getTextureManager().getDynamicTextureLocation("about_assets", aboutTexture);
			}
			catch (Exception ex) {}
		}
		
		this.enumerateMods(loader, enabledModsList);
	}
	
	/**
	 * Populate the mods list
	 * 
	 * @param loader
	 * @param enabledModsList
	 */
	private void enumerateMods(LiteLoader loader, EnabledModsList enabledModsList)
	{
		this.activeModText = String.format("%d mod(s) loaded", loader.getLoadedMods().size());
		
		// Add mods to this treeset first, in order to sort them
		Map<String, GuiModListEntry> sortedMods = new TreeMap<String, GuiModListEntry>();
		
		// Active mods
		for (LiteMod mod : loader.getLoadedMods())
		{
			GuiModListEntry modListEntry = new GuiModListEntry(loader, enabledModsList, this.mc.fontRenderer, mod);
			sortedMods.put(modListEntry.getKey(), modListEntry);
		}
		
		// Disabled mods
		for (ModFile disabledMod : loader.getDisabledMods())
		{
			GuiModListEntry modListEntry = new GuiModListEntry(loader, enabledModsList, this.mc.fontRenderer, disabledMod);
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
	public GuiMainMenu getMenu()
	{
		return this.mainMenu;
	}
	
	/* (non-Javadoc)
	 * @see net.minecraft.src.GuiScreen#initGui()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void initGui()
	{
		if (this.configPanel != null)
		{
			this.configPanel.setSize(this.width - LEFT_EDGE, this.height);
		}
		
		int left = LEFT_EDGE + MARGIN + 4 + (this.width - LEFT_EDGE - MARGIN - MARGIN - 4) / 2;
		
		this.buttonList.clear();
		this.buttonList.add(this.btnToggle = new GuiButton(0, left, this.height - PANEL_BOTTOM - 24, 90, 20, "Enable mod"));
		this.buttonList.add(this.btnConfig = new GuiButton(1, left + 92, this.height - PANEL_BOTTOM - 24, 69, 20, "Settings..."));
		this.buttonList.add(this.chkEnabled = new GuiCheckbox(2, LEFT_EDGE + MARGIN, this.height - PANEL_BOTTOM + 9, "Show LiteLoader tab on main menu"));
		
		this.selectMod(this.selectedMod);
	}
	
	/* (non-Javadoc)
	 * @see net.minecraft.src.GuiScreen#setWorldAndResolution(net.minecraft.src.Minecraft, int, int)
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
	 * @see net.minecraft.src.GuiScreen#updateScreen()
	 */
	@Override
	public void updateScreen()
	{
		if (this.configPanel != null)
		{
			this.configPanel.onTick();
		}
		
		this.tickNumber++;
		
		if (this.mc.currentScreen == this)
		{
			this.mainMenu.updateScreen();
			this.chkEnabled.checked = LiteLoader.getInstance().getDisplayModInfoScreenTab();
		}
		
		if (this.toggled)
		{
			this.onToggled();
		}
		
		if (this.doubleClickTime > 0)
			this.doubleClickTime--;
	}
	
	/* (non-Javadoc)
	 * @see net.minecraft.src.GuiScreen#drawScreen(int, int, float)
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
		mouseX -= (int)xOffset;
		
		// Handle mouse stuff here since we won't get mouse events when not the active GUI
		boolean mouseOverTab = mouseX > LEFT_EDGE - TAB_WIDTH && mouseX < LEFT_EDGE && mouseY > TAB_TOP && mouseY < TAB_TOP + TAB_HEIGHT;
		this.handleMouseClick(mouseX, mouseY, partialTicks, active, mouseOverTab);
		
		// Calculate the tab opacity, not framerate adjusted because we don't really care
		this.tabOpacity = mouseOverTab || this.tweenAmount > 0.0 ? 0.5F : Math.max(0.0F, this.tabOpacity - partialTicks * 0.1F);
		
		// Draw the panel contents
		this.drawPanel(mouseX, mouseY, partialTicks, active, xOffset);
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
		drawRect(LEFT_EDGE, 0, LEFT_EDGE + 1, TAB_TOP, 0xFFFFFFFF);
		drawRect(LEFT_EDGE, TAB_TOP + TAB_HEIGHT, LEFT_EDGE + 1, this.height, 0xFFFFFFFF);
		
		// Draw the tab
		this.mc.getTextureManager().bindTexture(aboutTextureResource);
		glDrawTexturedRect(LEFT_EDGE - TAB_WIDTH, TAB_TOP, TAB_WIDTH + 1, TAB_HEIGHT, 80, 80, 122, 160, 0.5F + this.tabOpacity);

		// Only draw the panel contents if we are actually open
		if (this.tweenAmount > 0.0)
		{
			if (this.configPanel != null)
			{
				this.drawConfigPanel(mouseX, mouseY, partialTicks);
			}
			else
			{
				// Draw the header pieces
				glDrawTexturedRect(LEFT_EDGE + MARGIN, 12, 128, 40, 0, 0, 256, 80, 1.0F); // liteloader logo
				glDrawTexturedRect(this.width - 32 - MARGIN, 12, 32, 45, 0, 80, 64, 170, 1.0F); // chicken
				
				// Draw header text
				this.fontRenderer.drawString("Version " + LiteLoader.getVersion(), LEFT_EDGE + MARGIN + 38, 50, 0xFFFFFFFF);
				this.fontRenderer.drawString(this.activeModText, LEFT_EDGE + MARGIN + 38, 60, 0xFFAAAAAA);
				
				// Draw top and bottom horizontal rules
				drawRect(LEFT_EDGE + MARGIN, 80, this.width - MARGIN, 81, 0xFF999999);
				drawRect(LEFT_EDGE + MARGIN, this.height - PANEL_BOTTOM + 2, this.width - MARGIN, this.height - PANEL_BOTTOM + 3, 0xFF999999);
				
				int innerWidth = this.width - LEFT_EDGE - MARGIN - MARGIN - 4;
				int panelWidth = innerWidth / 2;
				int panelHeight = this.height - PANEL_BOTTOM - PANEL_TOP;
				
				this.drawModsList(mouseX, mouseY, partialTicks, panelWidth, panelHeight);
				this.drawSelectedMod(mouseX, mouseY, partialTicks, panelWidth, panelHeight);
				
				// Draw other controls inside the transform so that they slide properly
				super.drawScreen(mouseX, mouseY, partialTicks);
			}
		}
		else if (this.configPanel != null)
		{
			this.closeConfigPanel(this.configPanel);
		}
			
		
		glPopMatrix();
	}

	/**
	 * @param mouseX
	 * @param mouseY
	 * @param partialTicks
	 */
	public void drawConfigPanel(int mouseX, int mouseY, float partialTicks)
	{
		glPushMatrix();
		glTranslatef(LEFT_EDGE, 0, 0);
		
		this.configPanel.draw(mouseX - LEFT_EDGE, mouseY, partialTicks);
		
		glPopMatrix();
	}

	/**
	 * @param mouseX
	 * @param mouseY
	 * @param partialTicks
	 * @param width
	 * @param height
	 */
	public void drawModsList(int mouseX, int mouseY, float partialTicks, int width, int height)
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
	public void drawSelectedMod(int mouseX, int mouseY, float partialTicks, int width, int height)
	{
		if (this.selectedMod != null)
		{
			int left = LEFT_EDGE + MARGIN + width;
			int right = this.width - MARGIN;

			glEnableClipping(left, right, PANEL_TOP, this.height - PANEL_BOTTOM - 28);
			this.selectedMod.drawInfo(mouseX, mouseY, partialTicks, left, PANEL_TOP, right - left);
			glDisableClipping();
		}
	}

	/**
	 * @param mod
	 * @return
	 */
	public void selectMod(GuiModListEntry mod)
	{
		this.selectedMod = mod;
		this.btnToggle.drawButton = false;
		this.btnConfig.drawButton = false;
		
		if (this.selectedMod != null && this.selectedMod.canBeToggled())
		{
			this.btnToggle.drawButton = true;
			this.btnToggle.displayString = this.selectedMod.willBeEnabled() ? "Disable mod" : "Enable mod";
			
			this.btnConfig.drawButton = this.configPanelManager.hasPanel(this.selectedMod.getModClass());
		}
	}
	
	/* (non-Javadoc)
	 * @see net.minecraft.src.GuiScreen#actionPerformed(net.minecraft.src.GuiButton)
	 */
	@Override
	protected void actionPerformed(GuiButton button)
	{
		if (button.id == 0 && this.selectedMod != null)
		{
			this.selectedMod.toggleEnabled();
			this.selectMod(this.selectedMod);
		}
		
		if (button.id == 1)
		{
			this.openConfigPanel();
		}
		
		if (button.id == 2)
		{
			this.chkEnabled.checked = !this.chkEnabled.checked;
			LiteLoader.getInstance().setDisplayModInfoScreenTab(this.chkEnabled.checked);
			
			if (!this.chkEnabled.checked)
			{
				this.chkEnabled.displayString = "Show LiteLoader tab on main menu \247e(use \2479CTRL\247e+\2479SHIFT\247e+\2479TAB\247e)";
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see net.minecraft.src.GuiScreen#keyTyped(char, int)
	 */
	@Override
	protected void keyTyped(char keyChar, int keyCode)
	{
		if (this.configPanel != null)
		{
			this.configPanel.keyPressed(keyChar, keyCode);
			return;
		}
		
		if (keyCode == Keyboard.KEY_ESCAPE)
		{
			this.onToggled();
			return;
		}
	}
	
	/* (non-Javadoc)
	 * @see net.minecraft.src.GuiScreen#mouseClicked(int, int, int)
	 */
	@Override
	protected void mouseClicked(int mouseX, int mouseY, int button)
	{
		if (this.configPanel != null)
		{
			this.configPanel.mousePressed(mouseX - LEFT_EDGE, mouseY, button);
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
					if (mod.mouseWasOver())
					{
						this.selectMod(mod);
						
						// handle double-click
						if (mod == lastSelectedMod && this.doubleClickTime > 0 && this.btnConfig.drawButton)
						{
							this.actionPerformed(this.btnConfig);
						}
						
						this.doubleClickTime = 5;
					}
				}
			}
		}
		
		super.mouseClicked(mouseX, mouseY, button);
	}
	
	/* (non-Javadoc)
	 * @see net.minecraft.src.GuiScreen#mouseMovedOrUp(int, int, int)
	 */
	@Override
	protected void mouseMovedOrUp(int mouseX, int mouseY, int button)
	{
		if (this.configPanel != null)
		{
			if (button == -1)
				this.configPanel.mouseMoved(mouseX - LEFT_EDGE, mouseY);
			else
				this.configPanel.mouseReleased(mouseX - LEFT_EDGE, mouseY, button);
			
			return;
		}
		
		if (button == 0)
		{
			this.scrollBar.setDragging(false);
		}
		
		super.mouseMovedOrUp(mouseX, mouseY, button);
	}
	
	/* (non-Javadoc)
	 * @see net.minecraft.src.GuiScreen#handleMouseInput()
	 */
	@Override
	public void handleMouseInput()
	{
		int mouseWheelDelta = Mouse.getEventDWheel();
		if (mouseWheelDelta != 0)
		{
			if (this.configPanel != null)
				this.configPanel.mouseWheelScrolled(mouseWheelDelta);
			else
				this.scrollBar.offsetValue(-mouseWheelDelta / 8);
		}
		
		super.handleMouseInput();
	}

	/**
	 * @param mouseX
	 * @param active
	 * @param mouseOverTab
	 */
	public void handleMouseClick(int mouseX, int mouseY, float partialTicks, boolean active, boolean mouseOverTab)
	{
		boolean mouseDown = Mouse.isButtonDown(0);
		if (((active && mouseX < LEFT_EDGE) || mouseOverTab) && !this.mouseDown && mouseDown)
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
		else if (!active && this.tweenAmount > 0.0)
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
			ConfigPanel panel = this.configPanelManager.getPanel(this.selectedMod.getModClass());
			if (panel != null)
			{
				if (this.configPanel != null)
				{
					this.configPanel.onHidden();
				}
				
				this.configPanel = new GuiConfigPanelContainer(this, this.mc, panel, this.selectedMod.getModInstance());
				this.configPanel.setSize(this.width - LEFT_EDGE, this.height);
				this.configPanel.onShown();
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.modconfig.ConfigPanelHost#close()
	 */
	void closeConfigPanel(GuiConfigPanelContainer container)
	{
		if (this.configPanel != null && (container == null || this.configPanel == container))
		{
			this.configPanel.onHidden();
			this.configPanel = null;
		}
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
	private static void glDrawTexturedRect(int x, int y, int width, int height, int u, int v, int u2, int v2, float alpha)
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