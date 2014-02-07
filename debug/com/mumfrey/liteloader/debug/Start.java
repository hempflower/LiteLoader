package com.mumfrey.liteloader.debug;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.launchwrapper.Launch;

import com.mumfrey.liteloader.launch.LiteLoaderTweaker;
import com.mumfrey.liteloader.util.log.LiteLoaderLogger;

/**
 * Wrapper class for LaunchWrapper Main class, which logs into minecraft.net first so that online shizzle can be tested
 * 
 * @author Adam Mummery-Smith
 * @version 0.6.2
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
		List<String> argsList = new ArrayList<String>();

		// Detect the FML tweaker specified on the command line, this likely means someone has pulled us
		// into a Forge MCP workspace
		for (String arg : args) fmlDetected |= FML_TWEAKER_NAME.equals(arg);
		
		if (fmlDetected)
		{
			args = new String[0];
			argsList.add("--tweakClass");argsList.add(FML_TWEAKER_NAME);
		}

		String usernameFromCmdLine = (args.length > 0) ? args[0] : null;
		String passwordFromCmdLine = (args.length > 1) ? args[1] : null;
		
		File loginJson = new File(new File(System.getProperty("user.dir")), ".auth.json");
		LoginManager loginManager = new LoginManager(loginJson);
		loginManager.login(usernameFromCmdLine, passwordFromCmdLine, 5);

		LiteLoaderLogger.info("Launching game as %s", loginManager.getProfileName());
		
		File gameDir = new File(System.getProperty("user.dir"));
		File assetsDir = new File(gameDir, "assets/virtual/legacy");

		argsList.add("--tweakClass");  argsList.add(LiteLoaderTweaker.class.getName());
		argsList.add("--username");    argsList.add(loginManager.getProfileName());
		argsList.add("--uuid");        argsList.add(loginManager.getUUID());
		argsList.add("--accessToken"); argsList.add(loginManager.getAuthenticatedToken());
		argsList.add("--version");     argsList.add("mcp");
		argsList.add("--gameDir");     argsList.add(gameDir.getAbsolutePath());
		argsList.add("--assetsDir");   argsList.add(assetsDir.getAbsolutePath());
		
		Launch.main(argsList.toArray(args));
	}
}
