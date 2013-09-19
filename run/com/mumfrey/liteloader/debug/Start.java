package com.mumfrey.liteloader.debug;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.PublicKey;
import java.security.cert.Certificate;

import javax.net.ssl.HttpsURLConnection;

import com.mumfrey.liteloader.launch.LiteLoaderTweaker;

import net.minecraft.launchwrapper.Launch;

/**
 * Wrapper class for LaunchWrapper Main class, which logs into minecraft.net first so that online shizzle can be tested
 * 
 * @author Adam Mummery-Smith
 * @version 0.4
 */
public abstract class Start
{
	/**
	 * Username specified on the command line
	 */
	public static String userName = "";
	
	/**
	 * Latest minecraft version as recieved from minecraft.net
	 */
	public static String latestVersion = "";
	
	/**
	 * Download ticket issued to us by minecraft.net
	 */
	public static String downloadTicket = "";
	
	/**
	 * Session ID retrieved during login ("-" means no session, eg. offline)
	 */
	public static String sessionId = "-";
	
	/**
	 * Encoded certificate data in case the user forgets to extract minecraft.key !
	 */
	private static int[] certificateData = {
		0x30, 0x82, 0x01, 0x22, 0x30, 0x0d, 0x06, 0x09, 0x2a, 0x86, 0x48, 0x86, 0xf7, 0x0d, 0x01, 0x01, 0x01, 0x05,
		0x00, 0x03, 0x82, 0x01, 0x0f, 0x00, 0x30, 0x82, 0x01, 0x0a, 0x02, 0x82, 0x01, 0x01, 0x00, 0xc6, 0xf3, 0xde,
		0xfd, 0xef, 0xa9, 0x62, 0x9c, 0xe0, 0x51, 0x28, 0x9b, 0xfb, 0x46, 0xbc, 0xf4, 0x1a, 0x03, 0x0d, 0x6f, 0x69,
		0xf1, 0x98, 0x2a, 0xa6, 0x35, 0xb9, 0x62, 0xd6, 0x36, 0x6b, 0x64, 0x53, 0x87, 0x93, 0xd6, 0xa9, 0xd3, 0x46,
		0xd7, 0x93, 0x4f, 0x1d, 0x8e, 0x50, 0x50, 0x7d, 0x51, 0x80, 0xd1, 0x29, 0x8f, 0x83, 0x37, 0xb6, 0x67, 0xfd,
		0xde, 0xa3, 0xdf, 0x02, 0xda, 0x52, 0xba, 0x1c, 0x3a, 0x3e, 0x6f, 0xe4, 0xb6, 0xc8, 0x55, 0x75, 0x91, 0xf8,
		0x6b, 0x2a, 0x51, 0x5a, 0xad, 0xf6, 0x26, 0x9b, 0xff, 0xcf, 0x67, 0x15, 0xa0, 0xb9, 0x5a, 0xb8, 0xca, 0xaf,
		0x8f, 0xef, 0x9a, 0x15, 0x45, 0x9f, 0x87, 0xd0, 0x82, 0x89, 0x55, 0x45, 0x91, 0x7e, 0x90, 0x03, 0x84, 0x45,
		0x6b, 0xdf, 0xeb, 0xa4, 0x95, 0x71, 0x74, 0xbd, 0x0f, 0x8b, 0xf7, 0xa8, 0xc4, 0xfa, 0xd5, 0x7d, 0x6f, 0xff,
		0x01, 0xc0, 0x4a, 0x64, 0xd2, 0x73, 0x02, 0xf1, 0x4f, 0x72, 0x87, 0x48, 0x80, 0xa2, 0x0c, 0x9c, 0x3c, 0xd5,
		0xad, 0xbe, 0xfb, 0xf0, 0x38, 0x34, 0xaf, 0x25, 0x10, 0xef, 0x96, 0xaf, 0x8c, 0x3d, 0xfa, 0x48, 0x54, 0x5f,
		0xe4, 0x11, 0x43, 0xa2, 0x74, 0xe9, 0xc4, 0x28, 0xa9, 0x06, 0x3d, 0xcc, 0xbd, 0xc0, 0xbe, 0x48, 0xb4, 0x22,
		0xd6, 0xd2, 0x34, 0xee, 0x2f, 0x07, 0x76, 0xe7, 0x33, 0x9f, 0x0d, 0xe5, 0x9e, 0x34, 0x8a, 0xc6, 0xec, 0x2b,
		0x75, 0x15, 0x3a, 0x2f, 0xa8, 0xa6, 0x9a, 0x77, 0x68, 0x17, 0xf2, 0x90, 0x65, 0x5b, 0xef, 0x52, 0x33, 0xaa,
		0x4b, 0x05, 0xf3, 0x08, 0x80, 0x0e, 0xdf, 0x0d, 0xfb, 0x8b, 0x67, 0x0e, 0x17, 0x54, 0x25, 0x9f, 0x75, 0xa9,
		0xf8, 0x66, 0x28, 0xeb, 0x70, 0x31, 0x49, 0xac, 0xe3, 0x9d, 0xb1, 0x10, 0xc8, 0xfd, 0xfd, 0x8d, 0x23, 0x6c,
		0xef, 0x02, 0x03, 0x01, 0x00, 0x01
	};
	
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
			if (!args[1].equals("-") && login(args[0], args[1], 13, true))
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

	/**
	 * Do login to minecraft.net. Based on the login method in the Launcher
	 * 
	 * @param user Username
	 * @param password Password
	 * @param masqueradeLauncher Version Launcher version to masquerade as 
	 * @param validateCertificate True to validate the minecraft.net certificate. False if you can't be bothered or don't care :) 
	 * @return True if the login succeeded
	 */
	private static boolean login(String user, String password, int masqueradeLauncherVersion, boolean validateCertificate)
	{
		try
		{
			String parameters = String.format("user=%s&password=%s&version=%s",URLEncoder.encode(user, "UTF-8"), URLEncoder.encode(password, "UTF-8"), masqueradeLauncherVersion);
		 
			Start.showMessage("Attempting to login to minecraft.net...");
			String result = excutePost("https://login.minecraft.net/", parameters, validateCertificate);
			
			if (result == null)
			{
				Start.showError("Can't connect to minecraft.net");
				return false;
			}
			
			if (!result.contains(":"))
			{
				Start.showError(result);
				return false;
			}
			
			try
			{
				String values[] = result.split(":");
				
				latestVersion  = values[0].trim();
				downloadTicket = values[1].trim();
				userName	   = values[2].trim();
				sessionId	  = values[3].trim();
				
				return true;
			}
			catch(Exception e)
			{
				e.printStackTrace();
				return false;
			}
		}
		catch (UnsupportedEncodingException e)
		{
			Start.showError("Error encoding POST data, check your parameters");
			return false;
		}
	}
	
	/**
	 * Execute a HTTPS POST. Based on code in the Minecraft launcher
	 * 
	 * @param targetURL POST url
	 * @param postData POST query string
	 * @param validateCertificate True if the certificate should be validated against the local cache
	 * @return Response data or null if the query fails
	 */
	private static String excutePost(String targetURL, String postData, boolean validateCertificate)
	{
		HttpsURLConnection https = null;
		String responseData;
		
		try
		{
			URL url = new URL(targetURL);
			
			// Play that funky music white boy
			https = (HttpsURLConnection)url.openConnection();
			https.setRequestMethod("POST");
			https.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			https.setRequestProperty("Content-Length", Integer.toString(postData.getBytes().length));
			https.setRequestProperty("Content-Language", "en-US");
			https.setUseCaches(false);
			https.setDoInput(true);
			https.setDoOutput(true);
			https.setConnectTimeout(1000);
			https.connect();
			
			// Validate the cert we received with the HTTPS response, if the user wants to
			if (validateCertificate)
			{
				Certificate certificates[] = https.getServerCertificates();
				
				byte bytes[] = new byte[294];
				
				try
				{
					DataInputStream keyReaderStream = new DataInputStream(Start.class.getResourceAsStream("/minecraft.key"));
					keyReaderStream.readFully(bytes);
					keyReaderStream.close();
				}
				catch (NullPointerException ex)
				{
					// Error loading from the key file (probably the user forgot to put minecraft.key in the bin folder)
					Start.showError("Missing minecraft.key, reverting to internal cache");
					
					for (int certIndex = 0; certIndex < 294; certIndex++)
						bytes[certIndex] = (byte)(certificateData[certIndex] & 0xFF);
				}
				
				PublicKey publicKey = certificates[0].getPublicKey();
				byte data[] = publicKey.getEncoded();
				
				for (int i = 0; i < data.length; i++)
				{
					if(data[i] != bytes[i])
					{
						Start.showError("Public key mismatch on " + targetURL);
						return null;
					}
				}
				
				Start.showMessage("Certificate validated ok for " + targetURL);
			}
			
			// Write ze postdata to ze server
			DataOutputStream postDataStream = new DataOutputStream(https.getOutputStream());
			postDataStream.writeBytes(postData);
			postDataStream.flush();
			postDataStream.close();
			
			// Get and concat the response
			BufferedReader reader = new BufferedReader(new InputStreamReader(https.getInputStream()));
			StringBuffer responseBuffer = new StringBuffer();
			
			String readLine;
			while((readLine = reader.readLine()) != null) 
			{
				responseBuffer.append(readLine);
				responseBuffer.append('\r');
			}

			reader.close();
			responseData = responseBuffer.toString();
			
			https.disconnect();
			https = null;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			
			if(https != null)
				https.disconnect();
			
			return null;
		}
		finally
		{
			if(https != null)
			{
				https.disconnect();
			}
		}
		
		return responseData;
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
