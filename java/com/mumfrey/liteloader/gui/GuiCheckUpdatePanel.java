package com.mumfrey.liteloader.gui;

import java.net.URI;

import org.lwjgl.input.Keyboard;

import com.mumfrey.liteloader.update.UpdateSite;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;

/**
 * "Check for updates" panel which docks in the mod info screen
 *
 * @author Adam Mummery-Smith
 */
public class GuiCheckUpdatePanel extends ModInfoScreenPanel
{
	/**
	 * URI to open if a new version is available
	 */
	private static final URI DOWNLOAD_URI = URI.create("http://dl.liteloader.com");

	/**
	 * Update site to contact
	 */
	private UpdateSite updateSite;
	
	/**
	 * Panel title
	 */
	private String panelTitle;
	
	/**
	 * Buttons
	 */
	private GuiButton btnCheck, btnDownload;
	
	/**
	 * Throbber frame
	 */
	private int throb;

	public GuiCheckUpdatePanel(Minecraft minecraft, UpdateSite updateSite, String updateName)
	{
		super(minecraft);
		
		this.updateSite = updateSite;
		this.panelTitle = I18n.format("gui.updates.title", updateName);
	}
	
	@Override
	void setSize(int width, int height)
	{
		super.setSize(width, height);
		
		this.controls.add(new GuiButton(0, this.width - 99 - MARGIN, this.height - BOTTOM + 9, 100, 20, I18n.format("gui.done")));
		this.controls.add(this.btnCheck = new GuiButton(1, MARGIN + 16, TOP + 16, 100, 20, I18n.format("gui.checknow")));
		this.controls.add(this.btnDownload = new GuiButton(2, MARGIN + 16, TOP + 118, 100, 20, I18n.format("gui.downloadupdate")));
	}
	
	@Override
	void draw(int mouseX, int mouseY, float partialTicks)
	{
		FontRenderer fontRenderer = this.mc.fontRenderer;
		
		// Draw panel title
		fontRenderer.drawString(this.panelTitle, MARGIN, TOP - 14, 0xFFFFFFFF);
		
		// Draw top and bottom horizontal bars
		drawRect(MARGIN, TOP - 4, this.width - MARGIN, TOP - 3, 0xFF999999);
		drawRect(MARGIN, this.height - BOTTOM + 2, this.width - MARGIN, this.height - BOTTOM + 3, 0xFF999999);
		
		this.btnCheck.enabled = !this.updateSite.isCheckInProgress();
		this.btnDownload.field_146125_m = false;

		if (this.updateSite.isCheckInProgress())
		{
			this.drawThrobber(MARGIN, TOP + 40, this.throb);
			fontRenderer.drawString(I18n.format("gui.updates.status.checking", ""), MARGIN + 18, TOP + 44, 0xFFFFFFFF);
		}
		else if (this.updateSite.isCheckComplete())
		{
			boolean success = this.updateSite.isCheckSucceess();
			String status = success ? I18n.format("gui.updates.status.success") : I18n.format("gui.updates.status.failed");
			fontRenderer.drawString(I18n.format("gui.updates.status.checking", status), MARGIN + 18, TOP + 44, 0xFFFFFFFF);
			
			if (success)
			{
				fontRenderer.drawString(I18n.format("gui.updates.available.title"), MARGIN + 18, TOP + 70, 0xFFFFFFFF);
				if (this.updateSite.isUpdateAvailable())
				{
					this.btnDownload.field_146125_m = true;
					fontRenderer.drawString(I18n.format("gui.updates.available.newversion"), MARGIN + 18, TOP + 84, 0xFFFFFFFF);
					fontRenderer.drawString(I18n.format("gui.updates.available.version", this.updateSite.getAvailableVersion()), MARGIN + 18, TOP + 94, 0xFFFFFFFF);
					fontRenderer.drawString(I18n.format("gui.updates.available.date", this.updateSite.getAvailableVersionDate()), MARGIN + 18, TOP + 104, 0xFFFFFFFF);
				}
				else
				{
					fontRenderer.drawString(I18n.format("gui.updates.available.nonewversion"), MARGIN + 18, TOP + 84, 0xFFFFFFFF);
				}
			}
		}
		else
		{
			fontRenderer.drawString(I18n.format("gui.updates.status.idle"), MARGIN + 18, TOP + 44, 0xFFFFFFFF);
		}
		
		super.draw(mouseX, mouseY, partialTicks);
	}

	/**
	 * @param control
	 */
	@Override
	void actionPerformed(GuiButton control)
	{
		if (control.id == 0) this.close();
		if (control.id == 1) this.updateSite.beginUpdateCheck();
		if (control.id == 2)
		{
			this.openURI(GuiCheckUpdatePanel.DOWNLOAD_URI);
			this.btnDownload.enabled = false;
		}
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
		this.throb++;
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
