package com.mumfrey.liteloader.debug;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Proxy;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.mojang.authlib.Agent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.InvalidCredentialsException;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication;

/**
 * Manages login requests against Yggdrasil for use in MCP
 *
 * @author Adam Mummery-Smith
 */
public class LoginManager
{
	private static Logger logger = Logger.getLogger("liteloader");
	
	private static Gson gson = new GsonBuilder().setPrettyPrinting().create();
	
	private YggdrasilAuthenticationService authService;
	
	private YggdrasilUserAuthentication authentication;

	private File jsonFile;
	
	private String defaultUsername;
	
	private String defaultDisplayName = System.getProperty("user.name");
	
	private boolean offline = false;
	
	private boolean showDialog = false;
	
	public LoginManager(File jsonFile)
	{
		this.jsonFile = jsonFile;

		this.resetAuth();
		this.load();
	}

	/**
	 * 
	 */
	public void resetAuth()
	{
		this.authService = new YggdrasilAuthenticationService(Proxy.NO_PROXY, UUID.randomUUID().toString());
		this.authentication = new YggdrasilUserAuthentication(this.authService, Agent.MINECRAFT);
	}

	/**
	 * @throws JsonIOException
	 * @throws JsonSyntaxException
	 */
	private void load()
	{
		if (this.jsonFile.exists())
		{
			FileReader fileReader = null;
			
			try
			{
				fileReader = new FileReader(this.jsonFile);
				AuthData authData = LoginManager.gson.fromJson(fileReader, AuthData.class);
				
				if (authData != null)
				{
					this.logInfo("Initialising Yggdrasil authentication service with client token: %s", authData.getClientToken());
					this.authService = new YggdrasilAuthenticationService(Proxy.NO_PROXY, authData.getClientToken());
					this.authentication = new YggdrasilUserAuthentication(this.authService, Agent.MINECRAFT);
					authData.loadFromStorage(this.authentication);
					this.offline = authData.workOffline();
					this.defaultUsername = authData.getUsername();
					this.defaultDisplayName = authData.getDisplayName();
				}
			}
			catch (IOException ex) {}
			finally
			{
				try
				{
					if (fileReader != null) fileReader.close();
				}
				catch (IOException ex) {}
			}
		}
	}
	
	private void save()
	{
		FileWriter fileWriter = null;
		
		try
		{
			fileWriter = new FileWriter(this.jsonFile);
			
			AuthData authData = new AuthData(this.authService, this.authentication, this.offline, this.defaultUsername, this.defaultDisplayName);
			LoginManager.gson.toJson(authData, fileWriter);
		}
		catch (IOException ex) { ex.printStackTrace(); }
		finally
		{
			try
			{
				if (fileWriter != null) fileWriter.close();
			}
			catch (IOException ex) { ex.printStackTrace(); }
		}
		
	}

	public boolean login(String username, String password, int remainingTries)
	{
		if (this.offline || remainingTries < 1)
		{
			this.logInfo("LoginManager is set to work offline, skipping login");
			return false;
		}
		
		this.logInfo("Remaining login tries: %d", remainingTries);
		
		try
		{
			this.logInfo("Attempting login, contacting Mojang auth servers...");
			
			this.authentication.logIn();
			
			if (this.authentication.isLoggedIn())
			{
				this.logInfo("LoginManager logged in successfully. Can play online = %s", this.authentication.canPlayOnline());
				this.save();
				return true;
			}
			
			this.logInfo("LoginManager failed to log in, unspecified status.");
		}
		catch (InvalidCredentialsException ex)
		{
			this.logInfo("Authentication agent reported invalid credentials: %s", ex.getMessage());
			this.resetAuth();
			
			if (username == null)
			{
				username = this.defaultUsername;
			}

			if (this.showDialog || username == null || password == null)
			{
				LoginPanel loginPanel = LoginPanel.getLoginPanel(username, password, this.showDialog ? ex.getMessage() : null);
				if (!loginPanel.showModalDialog())
				{
					this.logInfo("User cancelled login dialog");
					this.offline = loginPanel.workOffline();
					if (this.offline) this.save();
					return false;
				}

				username = loginPanel.getUsername();
				password = loginPanel.getPassword();
				this.offline = loginPanel.workOffline();
				this.save();
			}
			
			if (!Strings.isNullOrEmpty(username) && !Strings.isNullOrEmpty(password))
			{
				this.authentication.setUsername(username);
				this.authentication.setPassword(password);
			}

			this.showDialog = true;
			this.login(username, password, --remainingTries);
		}
		catch (AuthenticationException ex)
		{
			ex.printStackTrace();
		}
		
		this.save();
		return false;
	}
	
	public String getProfileName()
	{
		GameProfile selectedProfile = this.authentication.getSelectedProfile();
		return selectedProfile != null ? selectedProfile.getName() : this.defaultDisplayName;
	}
	
	public String getAuthenticatedToken()
	{
		String accessToken = this.authentication.getAuthenticatedToken();
		return accessToken != null ? accessToken : "-";
	}
	
	private void logInfo(String message, Object... params)
	{
		LoginManager.logger.info(String.format(message, params));
	}

	class AuthData
	{
		private String clientToken;
		
		private boolean workOffline;
		
		private Map<String, String> credentials;
		
		public AuthData()
		{
		}

		public AuthData(YggdrasilAuthenticationService authService, YggdrasilUserAuthentication authentication, boolean workOffline, String defaultUserName, String defaultDisplayName)
		{
			this.clientToken = authService.getClientToken();
			this.credentials = authentication.saveForStorage();
			this.workOffline = workOffline;
			
			if (defaultUserName != null && !this.credentials.containsKey("username"))
				this.credentials.put("username", defaultUserName);
			
			if (defaultDisplayName != null && !this.credentials.containsKey("displayName"))
				this.credentials.put("displayName", defaultDisplayName);
		}

		public String getClientToken()
		{
			return this.clientToken;
		}

		public void setClientToken(String clientToken)
		{
			this.clientToken = clientToken;
		}

		public void loadFromStorage(YggdrasilUserAuthentication authentication)
		{
			authentication.loadFromStorage(this.credentials);
		}
		
		public boolean workOffline()
		{
			return this.workOffline;
		}

		public String getUsername()
		{
			return this.credentials != null ? this.credentials.get("username") : null;
		}
		
		public String getDisplayName()
		{
			return this.credentials != null && this.credentials.containsKey("displayName") ? this.credentials.get("displayName") : System.getProperty("user.name");
		}
	}
}
