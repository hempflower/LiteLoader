/*
 * This file is part of LiteLoader.
 * Copyright (C) 2012-16 Adam Mummery-Smith
 * All Rights Reserved.
 */
package com.mumfrey.liteloader.client.gui;

import static com.mumfrey.liteloader.gl.GL.*;

import com.mumfrey.liteloader.client.api.LiteLoaderBrandingProvider;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

/**
 * Super-simple implementation of a checkbox control
 * 
 * @author Adam Mummery-Smith
 */
public class GuiCheckbox extends GuiButton
{
    public boolean checked;

    public GuiCheckbox(int controlId, int xPosition, int yPosition, String displayString)
    {
        super(controlId, xPosition, yPosition, Minecraft.getMinecraft().fontRenderer.getStringWidth(displayString) + 16, 12, displayString);
    }

    @Override
    public void drawButton(Minecraft minecraft, int mouseX, int mouseY, float partialTicks)
    {
        if (this.visible)
        {
            minecraft.getTextureManager().bindTexture(LiteLoaderBrandingProvider.ABOUT_TEXTURE);
            glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            this.hovered = mouseX >= this.x
                    && mouseY >= this.y
                    && mouseX < this.x + this.width
                    && mouseY < this.y + this.height;

            this.drawTexturedModalRect(this.x, this.y, this.checked ? 134 : 122, 80, 12, 12);
            this.mouseDragged(minecraft, mouseX, mouseY);

            int colour = 0xE0E0E0;
            if (!this.enabled)
            {
                colour = 0xA0A0A0;
            }
            else if (this.hovered)
            {
                colour = 0xFFFFA0;
            }

            this.drawString(minecraft.fontRenderer, this.displayString, this.x + 16, this.y + 2, colour);
        }
    }
}
