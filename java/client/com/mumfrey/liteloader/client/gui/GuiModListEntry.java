package com.mumfrey.liteloader.client.gui;

import static org.lwjgl.opengl.GL11.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.resources.I18n;

import com.google.common.collect.ImmutableSet;
import com.mumfrey.liteloader.LiteMod;
import com.mumfrey.liteloader.client.api.LiteLoaderBrandingProvider;
import com.mumfrey.liteloader.client.util.render.IconAbsolute;
import com.mumfrey.liteloader.core.LiteLoaderEnumerator;
import com.mumfrey.liteloader.core.LiteLoaderMods;
import com.mumfrey.liteloader.interfaces.Loadable;
import com.mumfrey.liteloader.interfaces.LoadableMod;
import com.mumfrey.liteloader.interfaces.TweakContainer;
import com.mumfrey.liteloader.launch.LoaderEnvironment;

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
	
	private static final Set<String> BUILT_IN_APIS = ImmutableSet.of("liteloader");
	
	/**
	 * For text display
	 */
	private FontRenderer fontRenderer;
	
	private final int brandColour;
	
	private final LiteLoaderMods mods;

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
	
	private boolean isMissingAPIs;
	
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
	
	/**
	 * True if this is not a mod but an external jar 
	 */
	private boolean external;
	
	private List<IconAbsolute> modIcons = new ArrayList<IconAbsolute>();

	/**
	 * Scroll bar control for the mod info
	 */
	private GuiSimpleScrollBar scrollBar = new GuiSimpleScrollBar();
	
	/**
	 * Mod list entry for an ACTIVE mod
	 * @param fontRenderer
	 * @param modInstance
	 * @param enabledMods
	 */
	GuiModListEntry(LiteLoaderMods mods, LoaderEnvironment environment, FontRenderer fontRenderer, int brandColour, LiteMod modInstance)
	{
		this.mods            = mods;
		this.fontRenderer    = fontRenderer;
		this.brandColour     = brandColour;

		this.modInstance     = modInstance;
		this.modClass        = modInstance.getClass();
		this.identifier      = mods.getModIdentifier(this.modClass);
		this.name            = modInstance.getName();
		this.version         = modInstance.getVersion();
		this.enabled         = true;
		this.canBeToggled    = this.identifier != null && mods.getEnabledModsList().saveAllowed();
		this.willBeEnabled   = this.identifier == null || mods.isModEnabled(this.identifier);;
		
		LoadableMod<?> modContainer = mods.getModContainer(this.modClass);
		
		this.author          = modContainer.getAuthor();
		this.url             = modContainer.getMetaValue("url", null);
		this.description     = modContainer.getDescription(LiteLoaderEnumerator.getModClassName(modInstance));
		
		boolean providesTweak       = false;
		boolean providesTransformer = false;
		boolean usingAPI            = this.checkUsingAPI(modContainer);

		if (modContainer instanceof TweakContainer)
		{
			providesTweak       = ((TweakContainer<?>)modContainer).hasTweakClass();
			providesTransformer = ((TweakContainer<?>)modContainer).hasClassTransformers();
		}
 			
		this.initIcons(providesTweak, providesTransformer, usingAPI);
	}
	
	/**
	 * Mod list entry for a currently disabled mod
	 * @param mods
	 * @param fontRenderer
	 * @param modContainer
	 */
	GuiModListEntry(LiteLoaderMods mods, LoaderEnvironment environment, FontRenderer fontRenderer, int brandColour, Loadable<?> modContainer)
	{
		this.mods            = mods;
		this.fontRenderer    = fontRenderer;
		this.brandColour     = brandColour;
		
		this.identifier      = modContainer.getIdentifier().toLowerCase();
		this.name            = modContainer.getDisplayName();
		this.version         = modContainer.getVersion();
		this.author          = modContainer.getAuthor();
		this.enabled         = modContainer.isEnabled(environment);
		this.canBeToggled    = modContainer.isToggleable() && mods.getEnabledModsList().saveAllowed();
		this.willBeEnabled   = mods.isModEnabled(this.identifier);
		this.external        = modContainer.isExternalJar();
		this.description     = modContainer.getDescription(null);

		boolean providesTweak       = false;
		boolean providesTransformer = false;
		boolean usingAPI            = false;
		
		if (modContainer instanceof LoadableMod<?>)
		{
			LoadableMod<?> loadableMod = (LoadableMod<?>)modContainer;
			
			this.url                   = loadableMod.getMetaValue("url", null);
			this.missingDependencies   = loadableMod.getMissingDependencies();
			this.missingAPIs           = loadableMod.getMissingAPIs();
			this.isMissingDependencies = this.missingDependencies.size() > 0;
			this.isMissingAPIs         = this.missingAPIs.size() > 0;
			
			usingAPI = this.checkUsingAPI(loadableMod);
		}
		
		if (modContainer instanceof TweakContainer)
		{
			TweakContainer<?> tweakContainer = (TweakContainer<?>)modContainer;
			
			providesTweak       = tweakContainer.hasTweakClass();
			providesTransformer = tweakContainer.hasClassTransformers();
		}
		
		this.initIcons(providesTweak, providesTransformer, usingAPI);
	}

	/**
	 * @param providesTweak
	 * @param providesTransformer
	 * @param usingAPI
	 */
	protected void initIcons(boolean providesTweak, boolean providesTransformer, boolean usingAPI)
	{
		if (providesTweak)
		{
			this.modIcons.add(new IconAbsolute(LiteLoaderBrandingProvider.ABOUT_TEXTURE, I18n.format("gui.mod.providestweak"), 12, 12, 158, 80, 158 + 12, 80 + 12));
		}
		
		if (providesTransformer)
		{
			this.modIcons.add(new IconAbsolute(LiteLoaderBrandingProvider.ABOUT_TEXTURE, I18n.format("gui.mod.providestransformer"), 12, 12, 170, 80, 170 + 12, 80 + 12));
		}
		
		if (usingAPI)
		{
			this.modIcons.add(new IconAbsolute(LiteLoaderBrandingProvider.ABOUT_TEXTURE, I18n.format("gui.mod.usingapi"), 12, 12, 122, 92, 122 + 12, 92 + 12));
		}
	}

	private boolean checkUsingAPI(LoadableMod<?> loadableMod)
	{
		for (String requiredAPI : loadableMod.getRequiredAPIs())
		{
			if (!GuiModListEntry.BUILT_IN_APIS.contains(requiredAPI))
				return true;
		}
		
		return false;
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
		
		this.fontRenderer.drawString(this.getTitleText(),   xPosition + 5, yPosition + 2,  titleColour);
		this.fontRenderer.drawString(this.getVersionText(), xPosition + 5, yPosition + 12, GuiModListEntry.VERSION_TEXT_COLOUR);
		this.fontRenderer.drawString(this.getStatusText(),  xPosition + 5, yPosition + 22, statusColour);
		
		this.mouseOverListEntry = this.isMouseOver(mouseX, mouseY, xPosition, yPosition, width, PANEL_HEIGHT); 
		drawRect(xPosition, yPosition, xPosition + 1, yPosition + PANEL_HEIGHT, this.mouseOverListEntry ? GuiModListEntry.HANGER_COLOUR_MOUSEOVER : GuiModListEntry.HANGER_COLOUR);
		
		return GuiModListEntry.PANEL_HEIGHT + GuiModListEntry.PANEL_SPACING;
	}

	public int postRenderListEntry(int mouseX, int mouseY, float partialTicks, int xPosition, int yPosition, int width, boolean selected)
	{
		xPosition += (width - 14);
		yPosition += (GuiModListEntry.PANEL_HEIGHT - 14);
		
		for (IconAbsolute icon : this.modIcons)
		{
			xPosition = this.drawPropertyIcon(xPosition, yPosition, icon, mouseX, mouseY);
		}
		
		return GuiModListEntry.PANEL_HEIGHT + GuiModListEntry.PANEL_SPACING;
	}

	protected int drawPropertyIcon(int xPosition, int yPosition, IconAbsolute icon, int mouseX, int mouseY)
	{
		glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		Minecraft.getMinecraft().getTextureManager().bindTexture(icon.getTextureResource());
		
		glEnable(GL_BLEND);
		this.drawTexturedModalRect(xPosition, yPosition, icon.getUPos(), icon.getVPos(), icon.getIconWidth(), icon.getIconHeight());
		glDisable(GL_BLEND);

		if (mouseX >= xPosition && mouseX <= xPosition + 12 && mouseY >= yPosition && mouseY <= yPosition + 12)
		{
			GuiLiteLoaderPanel.drawTooltip(this.fontRenderer, icon.getIconName(), mouseX, mouseY, 4096, 4096, GuiModListEntry.WHITE, GuiModListEntry.BLEND_HALF & GuiModListEntry.BLACK);
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
		if (this.url != null)
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

		GuiLiteLoaderPanel.glEnableClipping(-1, -1, yPos, bottom - 3);
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
		return GuiModListEntry.BLEND_2THRDS & (selected ? (this.external ? GuiModListEntry.EXTERNAL_ENTRY_COLOUR : this.brandColour) : GuiModListEntry.BLACK);
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
	
	public boolean isMouseOver()
	{
		return this.mouseOverListEntry;
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
