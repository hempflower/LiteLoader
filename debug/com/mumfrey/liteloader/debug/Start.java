package com.mumfrey.liteloader.debug;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.launchwrapper.Launch;

import com.mumfrey.liteloader.launch.LiteLoaderTweaker;
import com.mumfrey.liteloader.util.log.LiteLoaderLogger;

/**
 * Wrapper class for LaunchWrapper Main class, which logs into minecraft.net first so that online shizzle can be tested
 * 
 * @author Adam Mummery-Smith
 */
public abstract class Start
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
		
		String usernameFromCmdLine = null;
		String passwordFromCmdLine = null;
		
		if (argsList.size() > 0 && !argsList.get(0).startsWith("-"))
		{
			usernameFromCmdLine = argsList.remove(0); 

			if (argsList.size() > 0 && !argsList.get(0).startsWith("-"))
				passwordFromCmdLine = argsList.remove(0); 
		}
		
		File loginJson = new File(new File(System.getProperty("user.dir")), ".auth.json");
		LoginManager loginManager = new LoginManager(loginJson);
		loginManager.login(usernameFromCmdLine, passwordFromCmdLine, 5);

		LiteLoaderLogger.info("Launching game as %s", loginManager.getProfileName());
		
		File gameDir = new File(System.getProperty("user.dir"));
		File assetsDir = new File(gameDir, "assets");

		argsList.add("--tweakClass");     argsList.add(LiteLoaderTweaker.class.getName());
		argsList.add("--username");       argsList.add(loginManager.getProfileName());
		argsList.add("--uuid");           argsList.add(loginManager.getUUID());
		argsList.add("--accessToken");    argsList.add(loginManager.getAuthenticatedToken());
		argsList.add("--userType");       argsList.add(loginManager.getUserType());
		argsList.add("--userProperties"); argsList.add(loginManager.getUserProperties());
		argsList.add("--version");        argsList.add("mcp");
		argsList.add("--gameDir");        argsList.add(gameDir.getAbsolutePath());
		argsList.add("--assetIndex");     argsList.add(LiteLoaderTweaker.VERSION);
		argsList.add("--assetsDir");      argsList.add(assetsDir.getAbsolutePath());
		
		Launch.main(argsList.toArray(args));
	}
}
