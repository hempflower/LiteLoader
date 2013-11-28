package com.mumfrey.liteloader.debug;
import java.io.File;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

import net.minecraft.launchwrapper.Launch;

import com.mumfrey.liteloader.launch.LiteLoaderTweaker;
import com.mumfrey.liteloader.util.log.LiteLoaderLogFormatter;

/**
 * Wrapper class for LaunchWrapper Main class, which logs into minecraft.net first so that online shizzle can be tested
 * 
 * @author Adam Mummery-Smith
 * @version 0.6
 */
public abstract class Start
{
	private static Logger logger = Logger.getLogger("liteloader");
	
	/**
	 * Entry point.
	 * 
	 * @param args
	 */
	public static void main(String[] args)
	{
		Start.prepareLogger();
		
		String usernameFromCmdLine = (args.length > 0) ? args[0] : null;
		String passwordFromCmdLine = (args.length > 1) ? args[1] : null;
		
		File loginJson = new File(new File(System.getProperty("user.dir")), ".auth.json");
		LoginManager loginManager = new LoginManager(loginJson);
		loginManager.login(usernameFromCmdLine, passwordFromCmdLine, 5);

		Start.logger.info(String.format("Launching game as %s", loginManager.getProfileName()));
		
		File gameDir = new File(System.getProperty("user.dir"));
		File assetsDir = new File(gameDir, "assets");
		
		args = new String[] {
			"--tweakClass", LiteLoaderTweaker.class.getName(),
			"--username",   loginManager.getProfileName(),
			"--session",    loginManager.getAuthenticatedToken(),
			"--version",    "mcp",
			"--gameDir",    gameDir.getAbsolutePath(),
			"--assetsDir",  assetsDir.getAbsolutePath()
		};
		
		Launch.main(args);
	}
	
	private static void prepareLogger()
	{
		for (Handler handler : Start.logger.getParent().getHandlers())
		{
			if (handler instanceof ConsoleHandler)
				handler.setFormatter(new LiteLoaderLogFormatter());
		}
	}
}
