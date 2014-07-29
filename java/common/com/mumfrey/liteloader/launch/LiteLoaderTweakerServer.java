package com.mumfrey.liteloader.launch;

import java.io.File;
import java.net.URL;
import java.util.List;

import net.minecraft.launchwrapper.Launch;

public class LiteLoaderTweakerServer extends LiteLoaderTweaker
{
	@Override
	protected void initEnvironment(List<String> args, File gameDirectory, File assetsDirectory, String profile)
	{
		this.env = new StartupEnvironment(args, gameDirectory, assetsDirectory, profile)
		{
			@Override
			public void registerCoreAPIs(List<String> apisToLoad)
			{
				apisToLoad.add(0, "com.mumfrey.liteloader.server.api.LiteLoaderCoreAPIServer");
			}

			@Override
			public int getEnvironmentTypeId()
			{
				return LiteLoaderTweaker.ENV_TYPE_DEDICATEDSERVER;
			}
		};

		URL[] urls = Launch.classLoader.getURLs();
		this.jarUrl = urls[urls.length - 1]; // probably?
	}

	@Override
	public String getLaunchTarget()
	{
		super.getLaunchTarget();
		
		return "net.minecraft.server.MinecraftServer";
	}
}
