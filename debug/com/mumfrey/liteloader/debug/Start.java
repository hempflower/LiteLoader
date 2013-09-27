package com.mumfrey.liteloader.debug;
import java.io.File;
import java.io.IOException;
import java.net.Proxy;
import java.net.URL;

import net.minecraft.hopper.Util;
import net.minecraft.launcher.authentication.yggdrasil.AuthenticationResponse;
import net.minecraft.launchwrapper.Launch;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;
import com.mumfrey.liteloader.launch.LiteLoaderTweaker;

/**
 * Wrapper class for LaunchWrapper Main class, which logs into minecraft.net first so that online shizzle can be tested
 * 
 * @author Adam Mummery-Smith
 * @version 0.5
 */
public abstract class Start
{
	/**
	 * Username specified on the command line
	 */
	private static String userName = "";
	
	/**
	 * Session ID retrieved during login ("-" means no session, eg. offline)
	 */
	private static String sessionId = "-";
	
	/**
	 * Entry point. Validates the parameters and performs the login
	 * 
	 * @param args
	 */
	public static void main(String[] args)
	{
		// Check we have enough arguments
		if (args.length < 2)
		{
			Start.showError("Invalid parameters specified for start, use: <username> <password> to log in to minecraft.net");
			userName = args.length < 1 ? System.getProperty("user.name") : args[0];
		}
		else
		{
			// Assign username as the first argument
			userName = args[0];			
			
			// Perform the login if the second parameter is not "-" (indicating offline)
			if (!args[1].equals("-") && Start.login(args[0], args[1], 13, true))
			{
				Start.showMessage(String.format("Successfully logged in as %s with session ID %s", userName, sessionId));
				args[0] = userName;
				args[1] = sessionId;
			}
		}

		Start.showMessage(String.format("Launching game as %s", userName));
		
		File gameDir = new File(System.getProperty("user.dir"));
		File assetsDir = new File(gameDir, "assets");
		
		args = new String[] {
			"--tweakClass", LiteLoaderTweaker.class.getName(),
			"--username",   userName,
			"--session",    sessionId,
			"--version",    "mcp",
			"--gameDir",    gameDir.getAbsolutePath(),
			"--assetsDir",  assetsDir.getAbsolutePath()
		};
		
		Launch.main(args);
	}
	
	private static boolean login(String user, String password, int masqueradeLauncherVersion, boolean validateCertificate)
	{
		try
		{
			AuthenticationResponse response = Start.authRequest(user, password);
			
			if (response != null)
			{
				userName = response.getSelectedProfile().getName();
				sessionId = String.format("token:%s:%s", response.getAccessToken(), response.getSelectedProfile().getId());
				return true;
			}
		}
		catch (Exception ex) {}

		return false;
	}

	protected static AuthenticationResponse authRequest(String user, String password) throws IOException
	{
		Gson gson = new Gson();
		AuthenticationRequest request = new AuthenticationRequest(user, password);
		URL authUrl = new URL("https://authserver.mojang.com/authenticate");
		String json = Util.performPost(authUrl, gson.toJson(request), Proxy.NO_PROXY, "application/json", true);
		AuthenticationResponse result = gson.fromJson(json, AuthenticationResponse.class);
		if (result != null)
		{
			if (StringUtils.isBlank(result.getError()))
				return result;
			
			Start.showError(result.getErrorMessage());
		}

		return null;
	}

	/**
	 * Show a message on stdout
	 * 
	 * @param message
	 */
	private static void showMessage(String message)
	{
		System.out.println("[START] [INFO] " + message);
	}

	/**
	 * Show a message on stderr
	 * 
	 * @param message
	 */
	private static void showError(String message)
	{
		System.err.println("[START] [ERROR] " + message);
	}
}
