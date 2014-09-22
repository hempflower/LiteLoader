package com.mumfrey.liteloader.client.gui;

import static com.mumfrey.liteloader.client.util.GLClippingPlanes.*;
import static org.lwjgl.opengl.GL11.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.resources.I18n;

import com.google.common.base.Strings;
import com.mumfrey.liteloader.LiteMod;
import com.mumfrey.liteloader.api.ModInfoDecorator;
import com.mumfrey.liteloader.core.LiteLoaderMods;
import com.mumfrey.liteloader.core.ModInfo;
import com.mumfrey.liteloader.interfaces.Loadable;
import com.mumfrey.liteloader.interfaces.LoadableMod;
import com.mumfrey.liteloader.launch.LoaderEnvironment;
import com.mumfrey.liteloader.util.render.IconClickable;
import com.mumfrey.liteloader.util.render.IconTextured;

/**
 * Represents a mod in the mod info screen, keeps track of mod information and provides methods
 * for displaying the mod in the mod list and drawing the selected mod info
 *
 * @author Adam Mummery-Smith
 */
public class GuiModListEntry extends Gui
{
	private static final int BLACK                     = 0xFF000000;
	private static final int DARK_GREY                 = 0xB0333333;
	private static final int GREY                      = 0xFF999999;
	private static final int WHITE                     = 0xFFFFFFFF;
	
	private static final int BLEND_2THRDS              = 0xB0FFFFFF;
	private static final int BLEND_HALF                = 0x80FFFFFF;
	
	private static final int API_COLOUR                = 0xFFAA00AA;
	private static final int EXTERNAL_ENTRY_COLOUR     = 0xFF47D1AA;
	private static final int MISSING_DEPENDENCY_COLOUR = 0xFFFFAA00;
	private static final int ERROR_COLOUR              = 0xFFFF5555;
	private static final int ERROR_GRADIENT_COLOUR     = 0xFFAA0000;
	private static final int ERROR_GRADIENT_COLOUR2    = 0xFF550000;
	
	private static final int TITLE_COLOUR              = GuiModListEntry.WHITE;
	private static final int VERSION_TEXT_COLOUR       = GuiModListEntry.GREY;
	private static final int GRADIENT_COLOUR2          = GuiModListEntry.BLEND_2THRDS & GuiModListEntry.DARK_GREY;
	private static final int HANGER_COLOUR             = GuiModListEntry.GREY;
	private static final int HANGER_COLOUR_MOUSEOVER   = GuiModListEntry.WHITE;
	private static final int AUTHORS_COLOUR            = GuiModListEntry.WHITE;
	private static final int DIVIDER_COLOUR            = GuiModListEntry.GREY;
	private static final int DESCRIPTION_COLOUR        = GuiModListEntry.WHITE;

	private static final int PANEL_HEIGHT              = 32;
	private static final int PANEL_SPACING             = 4;
	
	/**
	 * For text display
	 */
	private FontRenderer fontRenderer;
	
	private final int brandColour;
	
	private final List<ModInfoDecorator> decorators;
	
	private final LiteLoaderMods mods;

	private ModInfo<?> modInfo;
	
	/**
	 * The identifier of the mod, used as the enablement/disablement key
	 */
	private String identifier;
	
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
	private String author = I18n.format("gui.unknown");
	
	/**
	 * Mod URL, from the metadata
	 */
	private String url = null;
	
	/**
	 * Mod description, from metadata
	 */
	private String description = "";
	
	/**
	 * Whether the mod is currently active
	 */
	private boolean enabled;
	
	private boolean isMissingDependencies;
	
	private boolean isMissingAPIs;
	
	private boolean isErrored;
	
	/**
	 * True if the mod is missing a dependency which has caused it not to load
	 */
	private Set<String> missingDependencies;
	
	/**
	 * True if the mod is missing an API which has caused it not to load
	 */
	private Set<String> missingAPIs;
	
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
	private boolean mouseOverListEntry, mouseOverInfo, mouseOverScrollBar;
	
	private IconClickable mouseOverIcon = null;
	
	/**
	 * True if this is not a mod but an external jar 
	 */
	private boolean external;
	
	private List<IconTextured> modIcons = new ArrayList<IconTextured>();

	/**
	 * Scroll bar control for the mod info
	 */
	private GuiSimpleScrollBar scrollBar = new GuiSimpleScrollBar();
	
	/**
	 * Mod list entry for an ACTIVE mod
	 * @param fontRenderer
	 * @param modInfo
	 * @param enabledMods
	 */
	GuiModListEntry(LiteLoaderMods mods, LoaderEnvironment environment, FontRenderer fontRenderer, int brandColour, List<ModInfoDecorator> decorators, ModInfo<?> modInfo)
	{
		this.mods          = mods;
		this.fontRenderer  = fontRenderer;
		this.brandColour   = brandColour;
		this.decorators    = decorators;
		this.modInfo       = modInfo;
		
		this.identifier    = modInfo.getIdentifier();
		this.name          = modInfo.getDisplayName();
		this.version       = modInfo.getVersion();
		this.author        = modInfo.getAuthor();
		this.enabled       = modInfo.isActive();
		this.canBeToggled  = modInfo.isToggleable() && mods.getEnabledModsList().saveAllowed();
		this.willBeEnabled = mods.isModEnabled(this.identifier);;
		this.external      = modInfo.getContainer().isExternalJar();
		this.description   = modInfo.getDescription();
		this.url           = modInfo.getURL();
		this.isErrored     = modInfo.getStartupErrors() != null && modInfo.getStartupErrors().size() > 0;
		
		if (!modInfo.isActive())
		{
			this.enabled = modInfo.getContainer().isEnabled(environment);

			Loadable<?> modContainer = modInfo.getContainer();
			if (modContainer instanceof LoadableMod<?>)
			{
				LoadableMod<?> loadableMod = (LoadableMod<?>)modContainer;
				
				this.missingDependencies   = loadableMod.getMissingDependencies();
				this.missingAPIs           = loadableMod.getMissingAPIs();
				this.isMissingDependencies = this.missingDependencies.size() > 0;
				this.isMissingAPIs         = this.missingAPIs.size() > 0;
			}
		}
		
		for (ModInfoDecorator decorator : this.decorators)
		{
			decorator.addIcons(modInfo, this.modIcons);
		}
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
		int gradientColour = this.getGradientColour(selected);
		int titleColour    = this.getTitleColour(selected);
		int statusColour   = this.getStatusColour(selected);
		
		this.drawGradientRect(xPosition, yPosition, xPosition + width, yPosition + GuiModListEntry.PANEL_HEIGHT, gradientColour, GuiModListEntry.GRADIENT_COLOUR2);
		
		String titleText = this.getTitleText();
		String versionText = this.getVersionText();
		String statusText = this.getStatusText();

		for (ModInfoDecorator decorator : this.decorators)
		{
			String newStatusText = decorator.modifyStatusText(this.modInfo, statusText);
			if (newStatusText != null) statusText = newStatusText;
		}
			
		this.fontRenderer.drawString(titleText,   xPosition + 5, yPosition + 2,  titleColour);
		this.fontRenderer.drawString(versionText, xPosition + 5, yPosition + 12, GuiModListEntry.VERSION_TEXT_COLOUR);
		this.fontRenderer.drawString(statusText,  xPosition + 5, yPosition + 22, statusColour);
		
		this.mouseOverListEntry = this.isMouseOver(mouseX, mouseY, xPosition, yPosition, width, PANEL_HEIGHT); 
		drawRect(xPosition, yPosition, xPosition + 1, yPosition + PANEL_HEIGHT, this.mouseOverListEntry ? GuiModListEntry.HANGER_COLOUR_MOUSEOVER : GuiModListEntry.HANGER_COLOUR);
		
		for (ModInfoDecorator decorator : this.decorators)
		{
			decorator.onDrawListEntry(mouseX, mouseY, partialTicks, xPosition, yPosition, width, GuiModListEntry.PANEL_HEIGHT, selected, this.modInfo, gradientColour, titleColour, statusColour);
		}
		
		return GuiModListEntry.PANEL_HEIGHT + GuiModListEntry.PANEL_SPACING;
	}

	public int postRenderListEntry(int mouseX, int mouseY, float partialTicks, int xPosition, int yPosition, int width, boolean selected)
	{
		xPosition += (width - 14);
		yPosition += (GuiModListEntry.PANEL_HEIGHT - 14);
		
		this.mouseOverIcon = null;
		
		for (IconTextured icon : this.modIcons)
		{
			xPosition = this.drawPropertyIcon(xPosition, yPosition, icon, mouseX, mouseY);
		}
		
		return GuiModListEntry.PANEL_HEIGHT + GuiModListEntry.PANEL_SPACING;
	}

	protected int drawPropertyIcon(int xPosition, int yPosition, IconTextured icon, int mouseX, int mouseY)
	{
		glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		Minecraft.getMinecraft().getTextureManager().bindTexture(icon.getTextureResource());
		
		glEnable(GL_BLEND);
		this.drawTexturedModalRect(xPosition, yPosition, icon.getUPos(), icon.getVPos(), icon.getIconWidth(), icon.getIconHeight());
		glDisable(GL_BLEND);

		if (mouseX >= xPosition && mouseX <= xPosition + 12 && mouseY >= yPosition && mouseY <= yPosition + 12)
		{
			String tooltipText = icon.getDisplayText();
			if (tooltipText != null)
			{
				glDisableClipping();
				GuiLiteLoaderPanel.drawTooltip(this.fontRenderer, tooltipText, mouseX, mouseY, 4096, 4096, GuiModListEntry.WHITE, GuiModListEntry.BLEND_HALF & GuiModListEntry.BLACK);
				glEnableClipping();
			}
			
			if (icon instanceof IconClickable) this.mouseOverIcon = (IconClickable)icon;
		}
		
		return xPosition - 14;
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
	public void drawInfo(final int mouseX, final int mouseY, final float partialTicks, final int xPosition, final int yPosition, final int width, final int height)
	{
		int bottom = height + yPosition;
		int yPos = yPosition + 2;
		
		this.mouseOverInfo = this.isMouseOver(mouseX, mouseY, xPosition, yPos, width, height);

		this.fontRenderer.drawString(this.getTitleText(),   xPosition + 5, yPos, GuiModListEntry.TITLE_COLOUR); yPos += 10;
		this.fontRenderer.drawString(this.getVersionText(), xPosition + 5, yPos, GuiModListEntry.VERSION_TEXT_COLOUR); yPos += 10;

		drawRect(xPosition + 5, yPos, xPosition + width, yPos + 1, GuiModListEntry.DIVIDER_COLOUR); yPos += 4; // divider

		this.fontRenderer.drawString(I18n.format("gui.about.authors") + ": \2477" + this.author, xPosition + 5, yPos, GuiModListEntry.AUTHORS_COLOUR); yPos += 10;
		if (!Strings.isNullOrEmpty(this.url))
		{
			this.fontRenderer.drawString(this.url, xPosition + 5, yPos, GuiModListEntry.BLEND_2THRDS & this.brandColour); yPos += 10;
		}

		drawRect(xPosition + 5, yPos, xPosition + width, yPos + 1, GuiModListEntry.DIVIDER_COLOUR); yPos += 4; // divider
		drawRect(xPosition + 5, bottom - 1, xPosition + width, bottom, GuiModListEntry.DIVIDER_COLOUR); // divider
		
		int scrollHeight = bottom - yPos - 3;
		int totalHeight = this.fontRenderer.splitStringWidth(this.description, width - 11);
		
		this.scrollBar.setMaxValue(totalHeight - scrollHeight);
		this.scrollBar.drawScrollBar(mouseX, mouseY, partialTicks, xPosition + width - 5, yPos, 5, scrollHeight, totalHeight);
		
		this.mouseOverScrollBar = this.isMouseOver(mouseX, mouseY, xPosition + width - 5, yPos, 5, scrollHeight);

		glEnableClipping(-1, -1, yPos, bottom - 3);
		this.fontRenderer.drawSplitString(this.description, xPosition + 5, yPos - this.scrollBar.getValue(), width - 11, GuiModListEntry.DESCRIPTION_COLOUR);
	}

	/**
	 * @return
	 */
	protected String getTitleText()
	{
		return this.name;
	}

	/**
	 * @return
	 */
	protected String getVersionText()
	{
		return I18n.format("gui.about.versiontext", this.version);
	}

	/**
	 * @return
	 */
	protected String getStatusText()
	{
		String statusText = this.external ? I18n.format("gui.status.loaded") : I18n.format("gui.status.active");
		
		if (this.isMissingAPIs)
		{
			statusText = "\2475" + I18n.format("gui.status.missingapis");
			if (this.canBeToggled && !this.willBeEnabled) statusText = "\247c" + I18n.format("gui.status.pending.disabled");
		}
		else if (this.isMissingDependencies)
		{
			statusText = "\247e" + I18n.format("gui.status.missingdeps");
			if (this.canBeToggled && !this.willBeEnabled) statusText = "\247c" + I18n.format("gui.status.pending.disabled");
		}
		else if (this.isErrored)
		{
			statusText = "\247c" + I18n.format("gui.status.startuperror");
		}
		else if (this.canBeToggled)
		{
			if (!this.enabled && !this.willBeEnabled) statusText = "\2477" + I18n.format("gui.status.disabled");
			if (!this.enabled &&  this.willBeEnabled) statusText = "\247a" + I18n.format("gui.status.pending.enabled"); 
			if ( this.enabled && !this.willBeEnabled) statusText = "\247c" + I18n.format("gui.status.pending.disabled");
		}
		
		return statusText;
	}

	/**
	 * @param external
	 * @param selected
	 * @return
	 */
	protected int getGradientColour(boolean selected)
	{
		return GuiModListEntry.BLEND_2THRDS & (this.isErrored ? (selected ? GuiModListEntry.ERROR_GRADIENT_COLOUR : GuiModListEntry.ERROR_GRADIENT_COLOUR2) : (selected ? (this.external ? GuiModListEntry.EXTERNAL_ENTRY_COLOUR : this.brandColour) : GuiModListEntry.BLACK));
	}

	/**
	 * @param missingDependencies
	 * @param enabled
	 * @param external
	 * @param selected
	 * @return
	 */
	protected int getTitleColour(boolean selected)
	{
		if (this.isMissingDependencies) return GuiModListEntry.MISSING_DEPENDENCY_COLOUR;
		if (this.isMissingAPIs) return GuiModListEntry.API_COLOUR;
		if (this.isErrored) return GuiModListEntry.ERROR_COLOUR;
		if (!this.enabled) return GuiModListEntry.GREY;
		return this.external ? GuiModListEntry.EXTERNAL_ENTRY_COLOUR : GuiModListEntry.WHITE;
	}

	/**
	 * @param external
	 * @param selected
	 * @return
	 */
	protected int getStatusColour(boolean selected)
	{
		return this.external ? GuiModListEntry.EXTERNAL_ENTRY_COLOUR : this.brandColour;
	}

	public int getHeight()
	{
		return GuiModListEntry.PANEL_HEIGHT + GuiModListEntry.PANEL_SPACING;
	}

	/**
	 * @param mouseX
	 * @param mouseY
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 * @return
	 */
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

	/**
	 * Toggle the enablement status of this mod, if supported
	 */
	public void toggleEnabled()
	{
		if (this.canBeToggled)
		{
			this.willBeEnabled = !this.willBeEnabled;
			this.mods.setModEnabled(this.identifier, this.willBeEnabled);
		}
	}
	
	public String getKey()
	{
		return (this.isErrored ? "0000" : "") + this.identifier + Integer.toHexString(this.hashCode());
	}
	
	public LiteMod getModInstance()
	{
		return this.modInfo.getMod();
	}
	
	public Class<? extends LiteMod> getModClass()
	{
		return this.modInfo.getModClass();
	}
	
	public String getName()
	{
		return getTitleText();
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
	
	public boolean isMouseOverIcon()
	{
		return this.mouseOverListEntry && this.mouseOverIcon != null;
	}

	public boolean isMouseOver()
	{
		return this.mouseOverListEntry;
	}
	
	public void iconClick(Object source)
	{
		if (this.mouseOverIcon != null)
		{
			this.mouseOverIcon.onClicked(source, this);
		}
	}
		
	public boolean mouseWheelScrolled(int mouseWheelDelta)
	{
		if (this.mouseOverInfo)
		{
			this.scrollBar.offsetValue(-mouseWheelDelta / 8);
			return true;
		}
		
		return false;
	}
}
