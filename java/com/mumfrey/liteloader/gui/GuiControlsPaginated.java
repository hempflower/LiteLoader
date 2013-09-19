package com.mumfrey.liteloader.gui;

import java.util.List;

import net.minecraft.src.GameSettings;
import net.minecraft.src.GuiButton;
import net.minecraft.src.GuiScreen;
import net.minecraft.src.GuiSmallButton;
import net.minecraft.src.I18n;
import net.minecraft.src.KeyBinding;

/**
 * Extended "Controls" screen with pages
 * 
 * @author Adam Mummery-Smith
 */
public class GuiControlsPaginated extends GuiScreen
{
	/**
	 * Pagination variables
	 */
	protected int controlsPerPage, startIndex, endIndex;
	
	/**
	 * Parent screen which will be displayed when this screen is closed
	 */
	protected GuiScreen parentScreen;
	
	/**
	 * Game settings
	 */
	protected GameSettings gameSettings;
	
	/**
	 * Additional buttons
	 */
	protected GuiButton btnNext, btnPrevious;
	
	/**
	 * Title to display
	 */
	protected String screenTitle = "Controls";
	
	/**
	 * ID of the button currently being edited
	 */
	protected int activeButtonId = -1;
	
	public GuiControlsPaginated(GuiScreen parentScreen, GameSettings gameSettings)
	{
		this.parentScreen = parentScreen;
		this.gameSettings = gameSettings;
		
		// Pagination defaults
		this.controlsPerPage = 14; 
		this.endIndex = this.gameSettings.keyBindings.length - (this.gameSettings.keyBindings.length % this.controlsPerPage == 0 ? this.controlsPerPage : this.gameSettings.keyBindings.length % this.controlsPerPage); 
	}
	
	@SuppressWarnings("unchecked")
	protected List<GuiButton> getLegacyControlList()
	{
		return this.buttonList;
	}
	
	protected final int getHeight()
	{
		return this.height;
	}
	
	protected final int getWidth()
	{
		return this.width;
	}
	
	public GuiScreen getParentScreen()
	{
		return this.parentScreen;
	}

	/**
	 * Initialise this GUI, called when the GUI is created  
	 */
	@Override
	public void initGui()
	{
		this.getLegacyControlList().clear();
		
		int oldControlsPerPage = this.controlsPerPage;
		this.controlsPerPage = ((this.getHeight() - 70) / 24) * 2;
		this.endIndex = this.gameSettings.keyBindings.length - (this.gameSettings.keyBindings.length % this.controlsPerPage == 0 ? this.controlsPerPage : this.gameSettings.keyBindings.length % this.controlsPerPage);
		if (oldControlsPerPage != this.controlsPerPage) this.startIndex = 0;
		
		for (int controlId = 0; controlId < this.gameSettings.keyBindings.length; controlId++)
		{
			boolean buttonVisible = controlId >= this.startIndex && controlId < this.startIndex + this.controlsPerPage;
			int left = buttonVisible ? this.getWidth() / 2 - 155 : this.getWidth() + 10000;
			int top = this.getHeight() / 6 + 24 * ((controlId - this.startIndex) >> 1);
			this.getLegacyControlList().add(new GuiSmallButton(controlId, left + ((controlId - this.startIndex) % 2) * 160, top, 70, 20, this.gameSettings.getOptionDisplayString(controlId)));
		}
		
		int buttonY = this.getHeight() / 6 + (this.controlsPerPage >> 1) * 24;
		
		// Only bother paginating if there are too many controls to display
		if (this.gameSettings.keyBindings.length > this.controlsPerPage)
		{
			this.getLegacyControlList().add(this.btnNext = new GuiButton(201, this.getWidth() / 2 - 51, buttonY, 50, 20, ">>"));
			this.getLegacyControlList().add(this.btnPrevious = new GuiButton(202, this.getWidth() / 2 - 103, buttonY, 50, 20, "<<"));
			this.getLegacyControlList().add(new GuiButton(200, this.getWidth() / 2 + 1, buttonY, 100, 20, I18n.getString("gui.done"))); 
			
			this.btnNext.enabled = this.startIndex < this.endIndex;
			this.btnPrevious.enabled = this.startIndex > 0;
		}
		else
		{
			this.getLegacyControlList().add(new GuiButton(200, this.getWidth() / 2 - 100, buttonY, I18n.getString("gui.done")));
		}
		
		this.screenTitle = I18n.getString("controls.title");
	}
	
	/**
	 * @param controlId
	 * @return
	 */
	protected String getKeybindDescription(int controlId)
	{
		return this.gameSettings.getKeyBindingDescription(controlId);
	}

	/**
	 * A button was clicked, deal with it
	 * 
	 * @param guibutton Button which was clicked
	 */
	@Override
	protected void actionPerformed(GuiButton guibutton)
	{
		// Update the button labels with the appropriate key names
		for(int i = 0; i < this.gameSettings.keyBindings.length; i++)
		{
			this.getLegacyControlList().get(i).displayString = this.gameSettings.getOptionDisplayString(i);
		}
		
		if (guibutton.id == 200) // Done button
		{
			this.mc.displayGuiScreen(this.parentScreen);
		}
		else if (guibutton.id == 201) // Next button
		{
			this.startIndex += this.controlsPerPage;
			this.startIndex = Math.min(this.endIndex, this.startIndex);
			this.initGui();
		}
		else if (guibutton.id == 202) // Previous button
		{
			this.startIndex -= this.controlsPerPage;
			this.startIndex = Math.max(0, this.startIndex);
			this.initGui();
		}
		else
		{
			this.activeButtonId = guibutton.id;
			guibutton.displayString = String.format("> %s <", this.gameSettings.getOptionDisplayString(guibutton.id));
		}
	}
	
	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton)
	{
		if (this.activeButtonId >= 0)
		{
			this.gameSettings.setKeyBinding(this.activeButtonId, -100 + mouseButton);
			this.getLegacyControlList().get(this.activeButtonId).displayString = this.gameSettings.getOptionDisplayString(this.activeButtonId);
			this.activeButtonId = -1;
			KeyBinding.resetKeyBindingArrayAndHash();
		}
		else
		{
			super.mouseClicked(mouseX, mouseY, mouseButton);
		}
	}
	
	@Override
	protected void keyTyped(char keyChar, int keyCode)
	{
		if (this.activeButtonId >= 0)
		{
			this.gameSettings.setKeyBinding(this.activeButtonId, keyCode);
			this.getLegacyControlList().get(this.activeButtonId).displayString = this.gameSettings.getOptionDisplayString(this.activeButtonId);
			this.activeButtonId = -1;
			KeyBinding.resetKeyBindingArrayAndHash();
		}
		else
		{
			super.keyTyped(keyChar, keyCode);
		}
	}
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTick)
	{
		this.drawDefaultBackground();
		this.drawCenteredString(this.fontRenderer, this.screenTitle, this.width / 2, 20, 0xffffff);
		
		// Draw key labels
		for (int controlId = 0; controlId < this.gameSettings.keyBindings.length; controlId++)
		{
			boolean conflict = false;
			
			for (int id = 0; id < this.gameSettings.keyBindings.length; id++)
			{
				if (id != controlId && this.gameSettings.keyBindings[controlId].keyCode == this.gameSettings.keyBindings[id].keyCode)
				{
					conflict = true;
					break;
				}
			}
			
			if (this.activeButtonId == controlId)
			{
				this.getLegacyControlList().get(controlId).displayString = "\247f> \247e??? \247f<";
			}
			else if (conflict)
			{
				this.getLegacyControlList().get(controlId).displayString = "\247c" + this.gameSettings.getOptionDisplayString(controlId);
			}
			else
			{
				this.getLegacyControlList().get(controlId).displayString = this.gameSettings.getOptionDisplayString(controlId);
			}
			
			int left = (controlId >= this.startIndex && controlId < this.startIndex + this.controlsPerPage) ? this.getWidth() / 2 - 155 : this.getWidth() + 10000;
			this.drawString(this.fontRenderer, this.getKeybindDescription(controlId), left + ((controlId - this.startIndex) % 2) * 160 + 70 + 6, this.getHeight() / 6 + 24 * ((controlId - this.startIndex) >> 1) + 7, 0xFFFFFF);
		}
		
		super.drawScreen(mouseX, mouseY, partialTick);
	}
}