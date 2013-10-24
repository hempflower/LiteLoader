package com.mumfrey.liteloader.gui;

import static org.lwjgl.opengl.GL11.*;

import net.minecraft.src.GuiButton;
import net.minecraft.src.Minecraft;

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
		super(controlId, xPosition, yPosition, 200, 12, displayString);
	}
	
	@Override
	public void drawButton(Minecraft minecraft, int mouseX, int mouseY)
	{
		if (this.drawButton)
		{
			minecraft.getTextureManager().bindTexture(GuiScreenModInfo.aboutTextureResource);
			glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
			this.field_82253_i = mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;
			
			this.drawTexturedModalRect(this.xPosition, this.yPosition, this.checked ? 134 : 122, 80, 12, 12);
			this.mouseDragged(minecraft, mouseX, mouseY);
			
			int colour = 0xE0E0E0;
			if (!this.enabled) colour = 0xA0A0A0;
			else if (this.field_82253_i) colour = 0xFFFFA0;
			
			this.drawString(minecraft.fontRenderer, this.displayString, this.xPosition + 16, this.yPosition + 2, colour);
		}
	}
}
