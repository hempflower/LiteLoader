package com.mumfrey.liteloader.gui;

import static org.lwjgl.opengl.GL11.*;
import static com.mumfrey.liteloader.gui.GuiScreenModInfo.glEnableClipping;
import static com.mumfrey.liteloader.gui.GuiScreenModInfo.glDisableClipping;

import java.util.LinkedList;
import java.util.List;

import net.minecraft.src.Gui;
import net.minecraft.src.GuiButton;
import net.minecraft.src.Minecraft;

import com.mumfrey.liteloader.LiteMod;
import com.mumfrey.liteloader.modconfig.ConfigPanel;
import com.mumfrey.liteloader.modconfig.ConfigPanelHost;

/**
 * Config panel container, this handles drawing the configuration panel chrome and also hosts the
 * configuration panels themselves to support scrolling and stuff
 *
 * @author Adam Mummery-Smith
 */
public class GuiConfigPanelContainer extends Gui implements ConfigPanelHost
{
	private static final int TOP    = 26;
	private static final int BOTTOM = 40;
	private static final int MARGIN = 12;
	
	/**
	 * Parent screen
	 */
	private GuiScreenModInfo parent;
	
	/**
	 * Minecraft
	 */
	private Minecraft mc;
	
	/**
	 * Panel we are hosting
	 */
	private ConfigPanel panel;
	
	/**
	 * Mod being configured, the panel may want a reference to it
	 */
	private LiteMod mod;
	
	/**
	 * Buttons
	 */
	private List<GuiButton> controls = new LinkedList<GuiButton>();

	/**
	 * Scroll bar for the panel
	 */
	private GuiSimpleScrollBar scrollBar = new GuiSimpleScrollBar();
	
	/**
	 * Current available width
	 */
	private int width = 0;
	
	/**
	 * Current available height
	 */
	private int height = 0;
	
	/**
	 * Current panel width (width - margins)
	 */
	private int panelWidth = 0;
	
	/**
	 * Current panel visible height (height - chrome)
	 */
	private int panelHeight = 0;
	
	/**
	 * Panel's internal height (for scrolling)
	 */
	private int totalHeight = -1;
	
	/**
	 * Panel Y position (for scroll)
	 */
	private int panelTop = TOP;
	
	/**
	 * @param parent
	 * @param minecraft
	 * @param panel
	 * @param mod
	 */
	GuiConfigPanelContainer(GuiScreenModInfo parent, Minecraft minecraft, ConfigPanel panel, LiteMod mod)
	{
		this.parent = parent;
		this.mc     = minecraft;
		this.panel  = panel;
		this.mod    = mod;
	}

	/**
	 * @return
	 */
	protected String getPanelTitle()
	{
		String panelTitle = this.panel.getPanelTitle();
		return panelTitle != null ? panelTitle : String.format("%s Settings", this.mod.getName());
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.modconfig.ConfigPanelHost#getMod()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <ModClass extends LiteMod> ModClass getMod()
	{
		return (ModClass)this.mod;
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.modconfig.ConfigPanelHost#getWidth()
	 */
	@Override
	public int getWidth()
	{
		return this.panelWidth;
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.modconfig.ConfigPanelHost#getHeight()
	 */
	@Override
	public int getHeight()
	{
		return this.panelHeight;
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.modconfig.ConfigPanelHost#close()
	 */
	@Override
	public void close()
	{
		this.parent.closeConfigPanel(this);
	}
	
	/**
	 * Callback from parent screen when window is resized
	 * 
	 * @param width
	 * @param height
	 */
	void setSize(int width, int height)
	{
		this.width = width;
		this.height = height;
		
		this.panelHeight = this.height - TOP - BOTTOM;
		this.panelWidth = this.width - (MARGIN * 2) - 6;
		
		this.panel.onPanelResize(this);
		
		this.controls.clear();
		this.controls.add(new GuiButton(0, this.width - 99 - MARGIN, this.height - BOTTOM + 9, 100, 20, "Save & Close"));
	}
	
	/**
	 * Callback from parent screen when panel is displayed
	 */
	void onShown()
	{
		this.panel.onPanelShown(this);
	}
	
	/**
	 * Callback from parent screen when panel is hidden
	 */
	void onHidden()
	{
		this.panel.onPanelHidden();
	}
	
	/**
	 * Callback from parent screen every tick
	 */
	void onTick()
	{
		this.panel.onTick(this);
	}
	
	/**
	 * Draw the panel and chrome
	 * 
	 * @param mouseX
	 * @param mouseY
	 * @param partialTicks
	 */
	void draw(int mouseX, int mouseY, float partialTicks)
	{
		// Scroll position
		this.panelTop = TOP - this.scrollBar.getValue();
	
		// Draw panel title
		this.mc.fontRenderer.drawString(this.getPanelTitle(), MARGIN, TOP - 14, 0xFFFFFFFF);
		
		// Draw top and bottom horizontal bars
		drawRect(MARGIN, TOP - 4, this.width - MARGIN, TOP - 3, 0xFF999999);
		drawRect(MARGIN, this.height - BOTTOM + 2, this.width - MARGIN, this.height - BOTTOM + 3, 0xFF999999);

		// Clip rect
		glEnableClipping(MARGIN, this.width - MARGIN - 6, TOP, this.height - BOTTOM);
		
		// Offset by scroll
		glPushMatrix();
		glTranslatef(MARGIN, this.panelTop, 0.0F);
		
		// Draw panel contents
		this.panel.drawPanel(this, mouseX - MARGIN - (this.mouseOverPanel(mouseX, mouseY) ? 0 : 99999), mouseY - this.panelTop, partialTicks);
		glClear(GL_DEPTH_BUFFER_BIT);
		
		// Disable clip rect
		glDisableClipping();
		
		// Restore transform
		glPopMatrix();
		
		// Get total scroll height from panel
		this.totalHeight = Math.max(-1, this.panel.getContentHeight());
		
		// Update and draw scroll bar
		this.scrollBar.setMaxValue(this.totalHeight - this.panelHeight);
		this.scrollBar.drawScrollBar(mouseX, mouseY, partialTicks, this.width - MARGIN - 5, TOP, 5, this.panelHeight, Math.max(this.panelHeight, this.totalHeight));
		
		// Draw other buttons
		for (GuiButton control : this.controls)
			control.drawButton(this.mc, mouseX, mouseY);
	}

	/**
	 * @param control
	 */
	private void actionPerformed(GuiButton control)
	{
		if (control.id == 0) this.close();
	}

	/**
	 * @param mouseX
	 * @param mouseY
	 * @return
	 */
	private boolean mouseOverPanel(int mouseX, int mouseY)
	{
		return mouseX > MARGIN && mouseX <= this.width - MARGIN && mouseY > TOP && mouseY <= this.height - BOTTOM;
	}

	/**
	 * @param mouseWheelDelta
	 */
	public void mouseWheelScrolled(int mouseWheelDelta)
	{
		this.scrollBar.offsetValue(-mouseWheelDelta / 8);
	}

	/**
	 * @param mouseX
	 * @param mouseY
	 * @param mouseButton
	 */
	void mousePressed(int mouseX, int mouseY, int mouseButton)
	{
		if (mouseButton == 0)
		{
			if (this.scrollBar.wasMouseOver())
				this.scrollBar.setDragging(true);
			
			for (GuiButton control : this.controls)
			{
				if (control.mousePressed(this.mc, mouseX, mouseY))
				{
					this.mc.sndManager.playSoundFX("random.click", 1.0F, 1.0F);
					this.actionPerformed(control);
				}
			}
		}
		
		if (this.mouseOverPanel(mouseX, mouseY))
		{
			this.panel.mousePressed(this, mouseX - MARGIN, mouseY - this.panelTop, mouseButton);
		}
	}
	
	/**
	 * @param mouseX
	 * @param mouseY
	 * @param mouseButton
	 */
	void mouseReleased(int mouseX, int mouseY, int mouseButton)
	{
		if (mouseButton == 0)
		{
			this.scrollBar.setDragging(false);
		}
		
		this.panel.mouseReleased(this, mouseX - MARGIN, mouseY - this.panelTop, mouseButton);
	}
	
	/**
	 * @param mouseX
	 * @param mouseY
	 */
	void mouseMoved(int mouseX, int mouseY)
	{
		this.panel.mouseMoved(this, mouseX - MARGIN, mouseY - this.panelTop);
	}
	
	/**
	 * @param keyChar
	 * @param keyCode
	 */
	void keyPressed(char keyChar, int keyCode)
	{
		this.panel.keyPressed(this, keyChar, keyCode);
	}
}
