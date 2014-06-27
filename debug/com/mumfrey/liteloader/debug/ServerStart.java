package com.mumfrey.liteloader.debug;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.launchwrapper.Launch;

import com.mumfrey.liteloader.launch.LiteLoaderTweakerServer;

/**
 * Wrapper class for LaunchWrapper Main class, which logs into minecraft.net first so that online shizzle can be tested
 * 
 * @author Adam Mummery-Smith
 * @version 0.6.2
 */
public abstract class ServerStart
{
	private static final String FML_TWEAKER_NAME = "cpw.mods.fml.common.launcher.FMLTweaker";
	
	/**
	 * Entry point.
	 * 
	 * @param args
	 */
	public static void main(String[] args)
	{
		System.setProperty("mcpenv", "true");
		
		boolean fmlDetected = false;
		List<String> argsList = new ArrayList<String>(Arrays.asList(args));

		// Detect the FML tweaker specified on the command line, this likely means someone has pulled us
		// into a Forge MCP workspace
		for (String arg : argsList) fmlDetected |= FML_TWEAKER_NAME.equals(arg);
		
		if (fmlDetected)
		{
			argsList.clear();
			argsList.add("--tweakClass");argsList.add(FML_TWEAKER_NAME);
		}

		File gameDir = new File(System.getProperty("user.dir"));
		File assetsDir = new File(gameDir, "assets");

		argsList.add("--tweakClass");  argsList.add(LiteLoaderTweakerServer.class.getName());
		argsList.add("--version");     argsList.add("mcp");
		argsList.add("--gameDir");     argsList.add(gameDir.getAbsolutePath());
		argsList.add("--assetsDir");   argsList.add(assetsDir.getAbsolutePath());
		
		Launch.main(argsList.toArray(args));
	}
}
