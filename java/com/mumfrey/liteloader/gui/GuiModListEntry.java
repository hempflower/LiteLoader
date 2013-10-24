package com.mumfrey.liteloader.gui;

import com.mumfrey.liteloader.LiteMod;
import com.mumfrey.liteloader.core.ClassPathMod;
import com.mumfrey.liteloader.core.EnabledModsList;
import com.mumfrey.liteloader.core.LiteLoader;
import com.mumfrey.liteloader.core.ModFile;

import net.minecraft.src.FontRenderer;
import net.minecraft.src.Gui;

/**
 * Represents a mod in the mod info screen, keeps track of mod information and provides methods
 * for displaying the mod in the mod list and drawing the selected mod info
 *
 * @author Adam Mummery-Smith
 */
public class GuiModListEntry extends Gui
{
	private static final int PANEL_HEIGHT = 32;
	private static final int PANEL_SPACING = 4;
	
	/**
	 * Enabled mods list, keep a reference so that we can toggle mod enablement when required
	 */
	private EnabledModsList enabledModsList;
	
	/**
	 * For text display
	 */
	private FontRenderer fontRenderer;

	/**
	 * The metadata name (id) of the mod, used as the enablement/disablement key
	 */
	private String metaName;
	
	/**
	 * Display name of the mod, disabled mods use the file/folder name
	 */
	private String name;
	
	/**
	 * Mod version string
	 */
	private String version;
	
	/**
	 * Mod author, from the metadata
	 */
	private String author;
	
	/**
	 * Mod URL, from the metadata
	 */
	private String url;
	
	/**
	 * Mod description, from metadata
	 */
	private String description;
	
	/**
	 * Whether the mod is currently active
	 */
	private boolean enabled;
	
	/**
	 * Whether the mod can be toggled, not all mods support this, eg. internal mods
	 */
	private boolean canBeToggled;
	
	/**
	 * Whether the mod WILL be enabled on the next startup, if the mod is active and has been disabled this
	 * will be false, and if it's currently disabled by has been toggled then it will be true 
	 */
	private boolean willBeEnabled;
	
	/**
	 * True if the mouse was over this mod on the last render
	 */
	private boolean mouseOver;
	
	/**
	 * Mod list entry for an ACTIVE mod
	 * 
	 * @param loader
	 * @param enabledMods
	 * @param fontRenderer
	 * @param mod
	 */
	GuiModListEntry(LiteLoader loader, EnabledModsList enabledMods, FontRenderer fontRenderer, LiteMod mod)
	{
		this.enabledModsList = enabledMods;
		this.fontRenderer    = fontRenderer;
		this.metaName        = loader.getModMetaName(mod.getClass());
		this.name            = mod.getName();
		this.version         = mod.getVersion();
		this.author          = loader.getModMetaData(mod.getClass(), "author", "Unknown");
		this.url             = loader.getModMetaData(mod.getClass(), "url", null);
		this.description     = loader.getModMetaData(mod.getClass(), "description", "");
		this.enabled         = true;
		this.canBeToggled    = this.metaName != null && this.enabledModsList.saveAllowed();
		this.willBeEnabled   = true;
	}
	
	/**
	 * Mod list entry for a currently disabled mod
	 * 
	 * @param loader
	 * @param enabledMods
	 * @param fontRenderer
	 * @param file
	 */
	GuiModListEntry(LiteLoader loader, EnabledModsList enabledMods, FontRenderer fontRenderer, ModFile file)
	{
		this.enabledModsList = enabledMods;
		this.fontRenderer    = fontRenderer;
		this.metaName        = file.getModName().toLowerCase();
		this.name            = file instanceof ClassPathMod ? file.getModName() : file.getName();
		this.version         = file.getVersion();
		this.author          = file.getMetaValue("author", "Unknown");
		this.url             = file.getMetaValue("url", null);
		this.description     = file.getMetaValue("description", "");
		this.enabled         = false;
		this.canBeToggled    = this.enabledModsList.saveAllowed();
		this.willBeEnabled   = enabledMods.isEnabled(loader.getProfile(), this.metaName);
	}
	
	/**
	 * Draw this list entry as a list item
	 * 
	 * @param mouseX
	 * @param mouseY
	 * @param partialTicks
	 * @param xPosition
	 * @param yPosition
	 * @param width
	 * @param selected
	 * @return
	 */
	public int drawListEntry(int mouseX, int mouseY, float partialTicks, int xPosition, int yPosition, int width, boolean selected)
	{
		int colour1 = selected ? 0xB04785D1 : 0xB0000000;
		drawGradientRect(xPosition, yPosition, xPosition + width, yPosition + PANEL_HEIGHT, colour1, 0xB0333333);
		
		this.fontRenderer.drawString(this.name, xPosition + 5, yPosition + 2, this.enabled ? 0xFFFFFFFF : 0xFF999999);
		this.fontRenderer.drawString("Version " + this.version, xPosition + 5, yPosition + 12, 0xFF999999);
		
		String status = "Active";
		
		if (this.canBeToggled)
		{
			if (!this.enabled && !this.willBeEnabled) status = "\2477Disabled";
			if (!this.enabled &&  this.willBeEnabled) status = "\247aEnabled on next startup"; 
			if ( this.enabled && !this.willBeEnabled) status = "\247cDisabled on next startup";
		}
		
		this.fontRenderer.drawString(status, xPosition + 5, yPosition + 22, 0xFF4785D1);
		
		this.mouseOver = mouseX > xPosition && mouseX < xPosition + width && mouseY > yPosition && mouseY < yPosition + PANEL_HEIGHT; 
		drawRect(xPosition, yPosition, xPosition + 1, yPosition + PANEL_HEIGHT, this.mouseOver ? 0xFFFFFFFF : 0xFF999999);
		
		return PANEL_HEIGHT + PANEL_SPACING;
	}
	
	/**
	 * Draw this entry as the info page
	 * 
	 * @param mouseX
	 * @param mouseY
	 * @param partialTicks
	 * @param xPosition
	 * @param yPosition
	 * @param width
	 */
	public void drawInfo(int mouseX, int mouseY, float partialTicks, int xPosition, int yPosition, int width)
	{
		yPosition += 2;

		this.fontRenderer.drawString(this.name, xPosition + 5, yPosition, 0xFFFFFFFF); yPosition += 10;
		this.fontRenderer.drawString("Version " + this.version, xPosition + 5, yPosition, 0xFF999999); yPosition += 10;

		drawRect(xPosition + 5, yPosition, xPosition + width, yPosition + 1, 0xFF999999); yPosition += 4;

		this.fontRenderer.drawString("Authors: \2477" + this.author, xPosition + 5, yPosition, 0xFFFFFFFF); yPosition += 10;
		if (this.url != null)
		{
			this.fontRenderer.drawString(this.url, xPosition + 5, yPosition, 0xB04785D1); yPosition += 10;
		}

		drawRect(xPosition + 5, yPosition, xPosition + width, yPosition + 1, 0xFF999999); yPosition += 4;
		
		this.fontRenderer.drawSplitString(this.description, xPosition + 5, yPosition, width - 5, 0xFFFFFFFF);
	}

	/**
	 * Toggle the enablement status of this mod, if supported
	 */
	public void toggleEnabled()
	{
		if (this.canBeToggled)
		{
			this.willBeEnabled = !this.willBeEnabled;
			this.enabledModsList.setEnabled(LiteLoader.getProfile(), this.metaName, this.willBeEnabled);
			this.enabledModsList.save();
		}
	}
	
	public String getKey()
	{
		return this.metaName + Integer.toHexString(this.hashCode());
	}
	
	public String getName()
	{
		return this.name;
	}

	public String getVersion()
	{
		return this.version;
	}

	public String getAuthor()
	{
		return this.author;
	}

	public String getDescription()
	{
		return this.description;
	}

	public boolean isEnabled()
	{
		return this.enabled;
	}

	public boolean canBeToggled()
	{
		return this.canBeToggled;
	}

	public boolean willBeEnabled()
	{
		return this.willBeEnabled;
	}
	
	public boolean mouseWasOver()
	{
		return this.mouseOver;
	}
}
