package com.mumfrey.liteloader.api;

import java.util.List;

import com.mumfrey.liteloader.core.ModInfo;
import com.mumfrey.liteloader.util.render.IconTextured;

/**
 * LiteLoader Extensible API - Branding Provider
 *
 * Decorator for ModInfo classes, to alter the appearance of ModInfo entries in the mod list
 * 
 * @author Adam Mummery-Smith
 */
public interface ModInfoDecorator extends CustomisationProvider
{
	/**
	 * Add icons to the mod list entry for this mod
	 * 
	 * @param mod
	 * @param icons
	 */
	public abstract void addIcons(ModInfo<?> mod, List<IconTextured> icons);
}
