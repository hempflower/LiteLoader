package com.mumfrey.liteloader.gui;

import static org.lwjgl.opengl.GL11.*;

import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.resources.I18n;

import com.mumfrey.liteloader.LiteMod;
import com.mumfrey.liteloader.core.EnabledModsList;
import com.mumfrey.liteloader.core.LiteLoader;
import com.mumfrey.liteloader.core.LiteLoaderEnumerator;
import com.mumfrey.liteloader.core.Loadable;
import com.mumfrey.liteloader.core.LoadableMod;
import com.mumfrey.liteloader.core.TweakContainer;

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
	 * For text display
	 */
	private FontRenderer fontRenderer;
	
	private LiteMod modInstance;
	
	private Class<? extends LiteMod> modClass;

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
	
	/**
	 * True if the mod is missing a dependency which has caused it not to load
	 */
	private Set<String> missingDependencies;
	
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
	
	/**
	 * True if this is not a mod but an external jar 
	 */
	private boolean external;
	
	private boolean providesTweak, providesTransformer;

	/**
	 * Scroll bar control for the mod info
	 */
	private GuiSimpleScrollBar scrollBar = new GuiSimpleScrollBar();

	/**
	 * Mod list entry for an ACTIVE mod
	 * 
	 * @param loader
	 * @param enabledMods
	 * @param fontRenderer
	 * @param modInstance
	 */
	GuiModListEntry(LiteLoader loader, EnabledModsList enabledMods, FontRenderer fontRenderer, LiteMod modInstance)
	{
		this.fontRenderer    = fontRenderer;
		this.modInstance     = modInstance;
		this.modClass        = modInstance.getClass();
		this.identifier      = loader.getModIdentifier(this.modClass);
		this.name            = modInstance.getName();
		this.version         = modInstance.getVersion();
		this.enabled         = true;
		this.canBeToggled    = this.identifier != null && enabledMods.saveAllowed();
		this.willBeEnabled   = true;
		
		LoadableMod<?> modContainer = loader.getModContainer(this.modClass);
		
		this.author          = modContainer.getAuthor();
		this.url             = modContainer.getMetaValue("url", null);
		this.description     = modContainer.getDescription(LiteLoaderEnumerator.getModClassName(modInstance));
		
		if (modContainer instanceof TweakContainer)
		{
			this.providesTweak = ((TweakContainer<?>)modContainer).hasTweakClass();
			this.providesTransformer = ((TweakContainer<?>)modContainer).hasClassTransformers();
		}
	}
	
	/**
	 * Mod list entry for a currently disabled mod
	 * 
	 * @param loader
	 * @param enabledMods
	 * @param fontRenderer
	 * @param modContainer
	 */
	GuiModListEntry(LiteLoader loader, EnabledModsList enabledMods, FontRenderer fontRenderer, Loadable<?> modContainer)
	{
		this.fontRenderer    = fontRenderer;
		this.identifier      = modContainer.getIdentifier().toLowerCase();
		this.name            = modContainer.getDisplayName();
		this.version         = modContainer.getVersion();
		this.author          = modContainer.getAuthor();
		this.enabled         = modContainer.isEnabled(enabledMods, LiteLoader.getProfile());
		this.canBeToggled    = modContainer.isToggleable() && enabledMods.saveAllowed();
		this.willBeEnabled   = enabledMods.isEnabled(LiteLoader.getProfile(), this.identifier);
		this.external        = modContainer.isExternalJar();
		this.description     = modContainer.getDescription(null);
		
		if (modContainer instanceof LoadableMod<?>)
		{
			LoadableMod<?> loadableMod = (LoadableMod<?>)modContainer;
			
			this.url                   = loadableMod.getMetaValue("url", null);
			this.missingDependencies   = loadableMod.getMissingDependencies();
			this.isMissingDependencies = this.missingDependencies.size() > 0;
			
			if (this.isMissingDependencies)
			{
				this.enabled = false;
				this.description = I18n.format("gui.description.missingdeps") + "\n" + this.missingDependencies.toString();
			}
		}
		
		if (modContainer instanceof TweakContainer)
		{
			TweakContainer<?> tweakContainer = (TweakContainer<?>)modContainer;
			
			this.providesTweak       = tweakContainer.hasTweakClass();
			this.providesTransformer = tweakContainer.hasClassTransformers();
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
		int colour1 = selected ? (this.external ? 0xB047d1aa : 0xB04785D1) : 0xB0000000;
		drawGradientRect(xPosition, yPosition, xPosition + width, yPosition + PANEL_HEIGHT, colour1, 0xB0333333);
		
		this.fontRenderer.drawString(this.name, xPosition + 5, yPosition + 2, this.isMissingDependencies ? 0xFFFFAA00 : (this.enabled ? (this.external ? 0xFF47d1aa : 0xFFFFFFFF) : 0xFF999999));
		this.fontRenderer.drawString(I18n.format("gui.about.versiontext", this.version), xPosition + 5, yPosition + 12, 0xFF999999);
		
		String status = this.external ? I18n.format("gui.status.loaded") : I18n.format("gui.status.active");
		
		if (this.isMissingDependencies)
		{
			status = "\247e" + I18n.format("gui.status.missingdeps");
			if (this.canBeToggled && !this.willBeEnabled) status = "\247c" + I18n.format("gui.status.pending.disabled");
		}
		else if (this.canBeToggled)
		{
			if (!this.enabled && !this.willBeEnabled) status = "\2477" + I18n.format("gui.status.disabled");
			if (!this.enabled &&  this.willBeEnabled) status = "\247a" + I18n.format("gui.status.pending.enabled"); 
			if ( this.enabled && !this.willBeEnabled) status = "\247c" + I18n.format("gui.status.pending.disabled");
		}
		
		this.fontRenderer.drawString(status, xPosition + 5, yPosition + 22, this.external ? 0xB047d1aa : 0xFF4785D1);
		
		this.mouseOverListEntry = this.isMouseOver(mouseX, mouseY, xPosition, yPosition, width, PANEL_HEIGHT); 
		drawRect(xPosition, yPosition, xPosition + 1, yPosition + PANEL_HEIGHT, this.mouseOverListEntry ? 0xFFFFFFFF : 0xFF999999);
		
		return PANEL_HEIGHT + PANEL_SPACING;
	}

	public int postRenderListEntry(int mouseX, int mouseY, float partialTicks, int xPosition, int yPosition, int width, boolean selected)
	{
		int iconX = xPosition + width - 14;
		if (this.providesTweak)       iconX = this.drawPropertyIcon(iconX, yPosition + PANEL_HEIGHT - 14, mouseX, mouseY, 158, 80, I18n.format("gui.mod.providestweak"));
		if (this.providesTransformer) iconX = this.drawPropertyIcon(iconX, yPosition + PANEL_HEIGHT - 14, mouseX, mouseY, 170, 80, I18n.format("gui.mod.providestransformer"));
		
		return PANEL_HEIGHT + PANEL_SPACING;
	}

	/**
	 * @param iconX
	 * @param yPosition
	 * @param mouseX
	 * @param mouseY
	 * @param u
	 * @param v
	 * @param tooltip
	 * @return
	 */
	protected int drawPropertyIcon(int iconX, int yPosition, int mouseX, int mouseY, int u, int v, String tooltipText)
	{
		glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		Minecraft.getMinecraft().getTextureManager().bindTexture(GuiScreenModInfo.aboutTextureResource);
		this.drawTexturedModalRect(iconX, yPosition, u, v, 12, 12);

		if (mouseX >= iconX && mouseX <= iconX + 12 && mouseY >= yPosition && mouseY <= yPosition + 12)
		{
			GuiScreenModInfo.drawTooltip(this.fontRenderer, tooltipText, mouseX, mouseY, 4096, 4096, 0xFFFFFFFF, 0x80000000);
		}
		
		return iconX - 14;
	}
	
	public int getHeight()
	{
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
	public void drawInfo(int mouseX, int mouseY, float partialTicks, int xPosition, int yPosition, int width, int height)
	{
		int bottom = height + yPosition;
		yPosition += 2;
		
		this.mouseOverInfo = this.isMouseOver(mouseX, mouseY, xPosition, yPosition, width, height);

		this.fontRenderer.drawString(this.name, xPosition + 5, yPosition, 0xFFFFFFFF); yPosition += 10;
		this.fontRenderer.drawString(I18n.format("gui.about.versiontext", this.version), xPosition + 5, yPosition, 0xFF999999); yPosition += 10;

		drawRect(xPosition + 5, yPosition, xPosition + width, yPosition + 1, 0xFF999999); yPosition += 4;

		this.fontRenderer.drawString(I18n.format("gui.about.authors") + ": \2477" + this.author, xPosition + 5, yPosition, 0xFFFFFFFF); yPosition += 10;
		if (this.url != null)
		{
			this.fontRenderer.drawString(this.url, xPosition + 5, yPosition, 0xB04785D1); yPosition += 10;
		}

		drawRect(xPosition + 5, yPosition, xPosition + width, yPosition + 1, 0xFF999999); yPosition += 4;
		drawRect(xPosition + 5, bottom - 1, xPosition + width, bottom, 0xFF999999);
		
		int scrollHeight = bottom - yPosition - 3;
		int totalHeight = this.fontRenderer.splitStringWidth(this.description, width - 11);
		
		this.scrollBar.setMaxValue(totalHeight - scrollHeight);
		this.scrollBar.drawScrollBar(mouseX, mouseY, partialTicks, xPosition + width - 5, yPosition, 5, scrollHeight, totalHeight);
		
		this.mouseOverScrollBar = this.isMouseOver(mouseX, mouseY, xPosition + width - 5, yPosition, 5, scrollHeight);

		GuiScreenModInfo.glEnableClipping(-1, -1, yPosition, bottom - 3);
		this.fontRenderer.drawSplitString(this.description, xPosition + 5, yPosition - this.scrollBar.getValue(), width - 11, 0xFFFFFFFF);
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
			LiteLoader.getInstance().setModEnabled(this.identifier, this.willBeEnabled);
		}
	}
	
	public String getKey()
	{
		return this.identifier + Integer.toHexString(this.hashCode());
	}
	
	public LiteMod getModInstance()
	{
		return this.modInstance;
	}
	
	public Class<? extends LiteMod> getModClass()
	{
		return this.modClass;
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
	
	public boolean mouseWasOverListEntry()
	{
		return this.mouseOverListEntry;
	}

	public boolean mouseWheelScrolled(int mouseWheelDelta)
	{
		if (this.mouseOverInfo)
		{
			this.scrollBar.offsetValue(-mouseWheelDelta);
			return true;
		}
		
		return false;
	}
}
