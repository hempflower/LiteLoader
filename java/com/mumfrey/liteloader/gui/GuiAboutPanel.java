package com.mumfrey.liteloader.gui;

import java.net.URI;

import org.lwjgl.input.Keyboard;

import com.mumfrey.liteloader.core.LiteLoader;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;

/**
 * "About LiteLoader" panel which docks in the mod info screen
 *
 * @author Adam Mummery-Smith
 */
public class GuiAboutPanel extends ModInfoScreenPanel
{
	private static final URI LITELOADER_URI = URI.create("http://www.liteloader.com/");
	private static final URI MCP_URI = URI.create("http://mcp.ocean-labs.de/");
	private static final URI TWITTER_URI = URI.create("https://twitter.com/therealeq2");

	private GuiScreenModInfo parent;

	private String versionText;
	
	public GuiAboutPanel(Minecraft minecraft, GuiScreenModInfo parent)
	{
		super(minecraft);
		this.parent = parent;
		this.versionText = "LiteLoader " + I18n.format("gui.about.versiontext", LiteLoader.getVersion());
	}
	
	@Override
	void setSize(int width, int height)
	{
		super.setSize(width, height);
		
		this.controls.add(new GuiButton(0, this.width - 99 - MARGIN, this.height - BOTTOM + 9, 100, 20, I18n.format("gui.done")));
		this.controls.add(new GuiHoverLabel(1, 50, 112, this.mc.fontRenderer, "\247n" + LITELOADER_URI.toString()));
		this.controls.add(new GuiHoverLabel(2, 50, 154, this.mc.fontRenderer, "\247n" + MCP_URI.toString()));
	}
	
	@Override
	void draw(int mouseX, int mouseY, float partialTicks)
	{
		FontRenderer fontRenderer = this.mc.fontRenderer;
		int textColour = 0xFFAAAAAA;

		this.mc.getTextureManager().bindTexture(GuiScreenModInfo.aboutTextureResource);
		GuiScreenModInfo.glDrawTexturedRect(MARGIN, 90, 32, 32, 192, 80, 192 + 64, 80 + 64, 1.0F);
		this.parent.drawInfoPanel(mouseX, mouseY, partialTicks, 0, 38);
		
		fontRenderer.drawString(this.versionText, 50, 90, 0xFFFFFFFF);
		fontRenderer.drawString("Copyright (c) 2012-2014 Adam Mummery-Smith", 50, 101, textColour);
		
		fontRenderer.drawString("Created using Mod Coder Pack", 50, 132, 0xFFFFFFFF);
		fontRenderer.drawString("MCP is (c) Copyright by the MCP Team", 50, 143, textColour);

		fontRenderer.drawString("Minecraft is Copyright (c) Mojang AB", 50, 174, textColour);
		fontRenderer.drawString("All rights reserved.", 50, 185, textColour);
		
		super.draw(mouseX, mouseY, partialTicks);
	}

	/**
	 * @param control
	 */
	@Override
	void actionPerformed(GuiButton control)
	{
		if (control.id == 0) this.close();
		if (control.id == 1) this.openURI(LITELOADER_URI);
		if (control.id == 2) this.openURI(MCP_URI);
	}
	
    private void openURI(URI uri)
    {
        try
        {
            Class<?> desktop = Class.forName("java.awt.Desktop");
            Object instance = desktop.getMethod("getDesktop").invoke(null);
            desktop.getMethod("browse", URI.class).invoke(instance, uri);
        }
        catch (Throwable th) {}
    }

	@Override
	void onTick()
	{
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
	void keyPressed(char keyChar, int keyCode)
	{
		if (keyCode == Keyboard.KEY_ESCAPE) this.close();
	}
	
	@Override
	void mousePressed(int mouseX, int mouseY, int mouseButton)
	{
		if (mouseButton == 0 && mouseX > MARGIN && mouseX < MARGIN + 32 && mouseY > 90 && mouseY < 122)
			this.openURI(TWITTER_URI);
		
		super.mousePressed(mouseX, mouseY, mouseButton);
	}
	
	@Override
	void mouseMoved(int mouseX, int mouseY)
	{
	}
	
	@Override
	void mouseReleased(int mouseX, int mouseY, int mouseButton)
	{
	}
	
	@Override
	void mouseWheelScrolled(int mouseWheelDelta)
	{
	}
}
