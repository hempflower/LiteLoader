package com.mumfrey.liteloader.client.gui.modlist;

import static com.mumfrey.liteloader.gl.GLClippingPlanes.*;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.resources.I18n;

import com.google.common.base.Strings;
import com.mumfrey.liteloader.client.gui.GuiSimpleScrollBar;
import com.mumfrey.liteloader.core.ModInfo;

public class GuiModInfoPanel extends Gui
{
	private static final int TITLE_COLOUR       = GuiModListPanel.WHITE;
	private static final int AUTHORS_COLOUR     = GuiModListPanel.WHITE;
	private static final int DIVIDER_COLOUR     = GuiModListPanel.GREY;
	private static final int DESCRIPTION_COLOUR = GuiModListPanel.WHITE;
	
	private final ModListEntry owner;
	
	private final FontRenderer fontRenderer;
	
	private final int brandColour;
	
	private final ModInfo<?> modInfo;

	private GuiSimpleScrollBar scrollBar = new GuiSimpleScrollBar();

	private boolean mouseOverPanel, mouseOverScrollBar;

	public GuiModInfoPanel(ModListEntry owner, FontRenderer fontRenderer, int brandColour, ModInfo<?> modInfo)
	{
		this.owner = owner;
		this.fontRenderer = fontRenderer;
		this.brandColour = brandColour;
		this.modInfo = modInfo;
	}

	public void draw(int mouseX, int mouseY, float partialTicks, int xPosition, int yPosition, int width, int height)
	{
		int bottom = height + yPosition;
		int yPos = yPosition + 2;
		
		this.mouseOverPanel = this.isMouseOver(mouseX, mouseY, xPosition, yPos, width, height);

		this.fontRenderer.drawString(this.owner.getTitleText(), xPosition + 5, yPos, GuiModInfoPanel.TITLE_COLOUR); yPos += 10;
		this.fontRenderer.drawString(this.owner.getVersionText(), xPosition + 5, yPos, GuiModListPanel.VERSION_TEXT_COLOUR); yPos += 10;

		drawRect(xPosition + 5, yPos, xPosition + width, yPos + 1, GuiModInfoPanel.DIVIDER_COLOUR); yPos += 4; // divider

		this.fontRenderer.drawString(I18n.format("gui.about.authors") + ": \2477" + this.modInfo.getAuthor(), xPosition + 5, yPos, GuiModInfoPanel.AUTHORS_COLOUR); yPos += 10;
		if (!Strings.isNullOrEmpty(this.modInfo.getURL()))
		{
			this.fontRenderer.drawString(this.modInfo.getURL(), xPosition + 5, yPos, GuiModListPanel.BLEND_2THRDS & this.brandColour); yPos += 10;
		}

		drawRect(xPosition + 5, yPos, xPosition + width, yPos + 1, GuiModInfoPanel.DIVIDER_COLOUR); yPos += 4; // divider
		drawRect(xPosition + 5, bottom - 1, xPosition + width, bottom, GuiModInfoPanel.DIVIDER_COLOUR); // divider
		
		int scrollHeight = bottom - yPos - 3;
		int totalHeight = this.fontRenderer.splitStringWidth(this.modInfo.getDescription(), width - 11);
		
		this.scrollBar.setMaxValue(totalHeight - scrollHeight);
		this.scrollBar.drawScrollBar(mouseX, mouseY, partialTicks, xPosition + width - 5, yPos, 5, scrollHeight, totalHeight);
		
		this.mouseOverScrollBar = this.isMouseOver(mouseX, mouseY, xPosition + width - 5, yPos, 5, scrollHeight);

		glEnableClipping(-1, -1, yPos, bottom - 3);
		this.fontRenderer.drawSplitString(this.modInfo.getDescription(), xPosition + 5, yPos - this.scrollBar.getValue(), width - 11, GuiModInfoPanel.DESCRIPTION_COLOUR);
	}
	
	private boolean isMouseOver(int mouseX, int mouseY, int x, int y, int width, int height)
	{
		return mouseX > x && mouseX < x + width && mouseY > y && mouseY < y + height;
	}
	
	public void mousePressed()
	{
		if (this.mouseOverScrollBar)
		{
			this.scrollBar.setDragging(true);
		}
	}

	public void mouseReleased()
	{
		this.scrollBar.setDragging(false);
	}
	
	public boolean mouseWheelScrolled(int mouseWheelDelta)
	{
		if (this.mouseOverPanel)
		{
			this.scrollBar.offsetValue(-mouseWheelDelta / 8);
			return true;
		}
		
		return false;
	}
}
