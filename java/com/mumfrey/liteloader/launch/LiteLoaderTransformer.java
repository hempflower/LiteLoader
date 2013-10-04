package com.mumfrey.liteloader.launch;

import net.minecraft.launchwrapper.IClassTransformer;

public class LiteLoaderTransformer implements IClassTransformer
{
	private static final String classMappingRenderLightningBolt = "net.minecraft.src.RenderLightningBolt";
	
	// TODO Obfuscation 1.6.4
	private static final String classMappingRenderLightningBoltObf = "bha";
	
	private static boolean postInit = false;
	
	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass)
	{
		if ((classMappingRenderLightningBolt.equals(name) || classMappingRenderLightningBoltObf.equals(name)) && !LiteLoaderTransformer.postInit)
		{
			LiteLoaderTransformer.postInit = true;
			LiteLoaderTweaker.init();
			LiteLoaderTweaker.postInit();
		}
		
		return basicClass;
	}
}
