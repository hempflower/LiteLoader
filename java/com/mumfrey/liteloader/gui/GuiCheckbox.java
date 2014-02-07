package com.mumfrey.liteloader.gui;

import static org.lwjgl.opengl.GL11.*;
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
	public void drawButton(Minecraft minecraft, int mouseX, int mouseY)
	{
		if (this.field_146125_m) // drawButton
		{
			minecraft.getTextureManager().bindTexture(GuiScreenModInfo.aboutTextureResource);
			glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
			this.field_146123_n = mouseX >= this.field_146128_h && mouseY >= this.field_146129_i && mouseX < this.field_146128_h + this.field_146120_f && mouseY < this.field_146129_i + this.field_146121_g;
			
			this.drawTexturedModalRect(this.field_146128_h, this.field_146129_i, this.checked ? 134 : 122, 80, 12, 12);
			this.mouseDragged(minecraft, mouseX, mouseY);
			
			int colour = 0xE0E0E0;
			if (!this.enabled) colour = 0xA0A0A0;
			else if (this.field_146123_n) colour = 0xFFFFA0;
			
			this.drawString(minecraft.fontRenderer, this.displayString, this.field_146128_h + 16, this.field_146129_i + 2, colour);
		}
	}
}
