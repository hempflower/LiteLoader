/*
 * This file is part of LiteLoader.
 * Copyright (C) 2012-16 Adam Mummery-Smith
 * All Rights Reserved.
 */
package com.mumfrey.liteloader.client.gui;

import com.mumfrey.liteloader.client.api.LiteLoaderBrandingProvider;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;

public class GuiHoverLabel extends GuiButton
{
    private FontRenderer fontRenderer;
    private int colour;
    private int hoverColour;

    public GuiHoverLabel(int id, int xPosition, int yPosition, FontRenderer fontRenderer, String displayText)
    {
        this(id, xPosition, yPosition, fontRenderer, displayText, LiteLoaderBrandingProvider.BRANDING_COLOUR);
    }

    public GuiHoverLabel(int id, int xPosition, int yPosition, FontRenderer fontRenderer, String displayText, int colour)
    {
        this(id, xPosition, yPosition, fontRenderer, displayText, colour, 0xFFFFFFAA);
    }

    public GuiHoverLabel(int id, int xPosition, int yPosition, FontRenderer fontRenderer, String displayText, int colour, int hoverColour)
    {
        super(id, xPosition, yPosition, GuiHoverLabel.getStringWidth(fontRenderer, displayText), 8, displayText);

        this.fontRenderer = fontRenderer;
        this.colour = colour;
        this.hoverColour = hoverColour;
    }

    @Override
    public void drawButton(Minecraft minecraft, int mouseX, int mouseY, float partialTicks)
    {
        if (this.visible)
        {
            this.hovered = mouseX >= this.x
                    && mouseY >= this.y
                    && mouseX < this.x + this.width
                    && mouseY < this.y + this.height;
            this.fontRenderer.drawString(this.displayString, this.x, this.y, this.hovered ? this.hoverColour : this.colour);
        }
        else
        {
            this.hovered = false;
        }
    }

    private static int getStringWidth(FontRenderer fontRenderer, String text)
    {
        return fontRenderer.getStringWidth(text);
    }
}
