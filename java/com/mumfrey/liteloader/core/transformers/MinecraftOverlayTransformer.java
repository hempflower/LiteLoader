package com.mumfrey.liteloader.core.transformers;

import com.mumfrey.liteloader.transformers.ClassOverlayTransformer;

public class MinecraftOverlayTransformer extends ClassOverlayTransformer
{
	private static final String overlayClassName = "com.mumfrey.liteloader.core.overlays.MinecraftOverlay";

	public MinecraftOverlayTransformer()
	{
		super(MinecraftOverlayTransformer.overlayClassName);
	}
}
