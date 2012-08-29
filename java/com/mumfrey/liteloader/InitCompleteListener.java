package com.mumfrey.liteloader;

import net.minecraft.client.Minecraft;

import com.mumfrey.liteloader.core.LiteLoader;

public interface InitCompleteListener extends Tickable
{
	public abstract void onInitCompleted(Minecraft minecraft, LiteLoader loader);
}
