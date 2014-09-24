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
	static final int BLACK                     = 0xFF000000;
	static final int DARK_GREY                 = 0xB0333333;
	static final int GREY                      = 0xFF999999;
	static final int WHITE                     = 0xFFFFFFFF;
	
	static final int BLEND_2THRDS              = 0xB0FFFFFF;
	static final int BLEND_HALF                = 0x80FFFFFF;
	
	static final int API_COLOUR                = 0xFFAA00AA;
	static final int EXTERNAL_ENTRY_COLOUR     = 0xFF47D1AA;
	static final int MISSING_DEPENDENCY_COLOUR = 0xFFFFAA00;
	static final int ERROR_COLOUR              = 0xFFFF5555;
	static final int ERROR_GRADIENT_COLOUR     = 0xFFAA0000;
	static final int ERROR_GRADIENT_COLOUR2    = 0xFF550000;
	
	static final int VERSION_TEXT_COLOUR       = GuiModListEntry.GREY;
	static final int GRADIENT_COLOUR2          = GuiModListEntry.BLEND_2THRDS & GuiModListEntry.DARK_GREY;
	static final int HANGER_COLOUR             = GuiModListEntry.GREY;
	static final int HANGER_COLOUR_MOUSEOVER   = GuiModListEntry.WHITE;

	static final int PANEL_HEIGHT              = 32;
	static final int PANEL_SPACING             = 4;
	
	/**
	 * For text display
	 */
	private final FontRenderer fontRenderer;
	
	private final int brandColour;
	
	private final List<ModInfoDecorator> decorators;
	
	private final LiteLoaderMods mods;

	private final ModInfo<?> modInfo;
	
	private final GuiModInfoPanel infoPanel;
	
	/**
	 * Whether the mod is currently active
	 */
	private boolean isActive;
	
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
	private boolean mouseOver;
	
	private IconClickable mouseOverIcon = null;
	
	/**
	 * True if this is not a mod but an external jar 
	 */
	private boolean external;
	
	private List<IconTextured> modIcons = new ArrayList<IconTextured>();
	
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
		
		this.infoPanel     = new GuiModInfoPanel(fontRenderer, brandColour, modInfo);
		
		this.isActive      = modInfo.isActive();
		this.canBeToggled  = modInfo.isToggleable() && mods.getEnabledModsList().saveAllowed();
		this.willBeEnabled = mods.isModEnabled(this.modInfo.getIdentifier());;
		this.external      = modInfo.getContainer().isExternalJar();
		this.isErrored     = modInfo.getStartupErrors() != null && modInfo.getStartupErrors().size() > 0;
		
		if (!modInfo.isActive())
		{
			this.isActive = modInfo.getContainer().isEnabled(environment);

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
	public int draw(int mouseX, int mouseY, float partialTicks, int xPosition, int yPosition, int width, boolean selected)
	{
		int gradientColour = this.getGradientColour(selected);
		int titleColour    = this.getTitleColour(selected);
		int statusColour   = this.getStatusColour(selected);
		
		this.drawGradientRect(xPosition, yPosition, xPosition + width, yPosition + GuiModListEntry.PANEL_HEIGHT, gradientColour, GuiModListEntry.GRADIENT_COLOUR2);
		
		String titleText = this.modInfo.getDisplayName();
		String versionText = I18n.format("gui.about.versiontext", this.modInfo.getVersion());
		String statusText = this.getStatusText();

		for (ModInfoDecorator decorator : this.decorators)
		{
			String newStatusText = decorator.modifyStatusText(this.modInfo, statusText);
			if (newStatusText != null) statusText = newStatusText;
		}
			
		this.fontRenderer.drawString(titleText,   xPosition + 5, yPosition + 2,  titleColour);
		this.fontRenderer.drawString(versionText, xPosition + 5, yPosition + 12, GuiModListEntry.VERSION_TEXT_COLOUR);
		this.fontRenderer.drawString(statusText,  xPosition + 5, yPosition + 22, statusColour);
		
		this.mouseOver = this.isMouseOver(mouseX, mouseY, xPosition, yPosition, width, PANEL_HEIGHT); 
		int hangerColour = this.mouseOver ? GuiModListEntry.HANGER_COLOUR_MOUSEOVER : GuiModListEntry.HANGER_COLOUR;
		drawRect(xPosition, yPosition, xPosition + 1, yPosition + PANEL_HEIGHT, hangerColour);
		
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
			if (!this.isActive && !this.willBeEnabled) statusText = "\2477" + I18n.format("gui.status.disabled");
			if (!this.isActive &&  this.willBeEnabled) statusText = "\247a" + I18n.format("gui.status.pending.enabled"); 
			if ( this.isActive && !this.willBeEnabled) statusText = "\247c" + I18n.format("gui.status.pending.disabled");
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
		if (!this.isActive) return GuiModListEntry.GREY;
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
	
	/**
	 * Toggle the enablement status of this mod, if supported
	 */
	public void toggleEnabled()
	{
		if (this.canBeToggled)
		{
			this.willBeEnabled = !this.willBeEnabled;
			this.mods.setModEnabled(this.modInfo.getIdentifier(), this.willBeEnabled);
		}
	}
	
	public String getKey()
	{
		return (this.isErrored ? "0000" : "") + this.modInfo.getIdentifier() + Integer.toHexString(this.hashCode());
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
		return this.modInfo.getDisplayName();
	}

	public String getVersion()
	{
		return this.modInfo.getVersion();
	}

	public String getAuthor()
	{
		return this.modInfo.getAuthor();
	}

	public String getDescription()
	{
		return this.modInfo.getDescription();
	}

	public boolean isEnabled()
	{
		return this.isActive;
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
		return this.mouseOver && this.mouseOverIcon != null;
	}

	public boolean isMouseOver()
	{
		return this.mouseOver;
	}
	
	public void iconClick(Object source)
	{
		if (this.mouseOverIcon != null)
		{
			this.mouseOverIcon.onClicked(source, this);
		}
	}

	public GuiModInfoPanel getInfoPanel()
	{
		return this.infoPanel;
	}
}
