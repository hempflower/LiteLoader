package com.mumfrey.liteloader.launch;

import java.util.List;

public class LiteLoaderTweakerServer extends LiteLoaderTweaker
{
	@Override
	protected void registerCoreAPIs(List<String> apisToLoad)
	{
		apisToLoad.add(0, "com.mumfrey.liteloader.server.api.LiteLoaderCoreAPIServer");
	}
	
	@Override
	public String getLaunchTarget()
	{
		super.getLaunchTarget();
		
		return "net.minecraft.server.MinecraftServer";
	}
	
	@Override
	protected int getEnvironmentTypeId()
	{
		return LiteLoaderTweaker.ENV_TYPE_DEDICATEDSERVER;
	}
}
