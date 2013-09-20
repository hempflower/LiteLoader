package com.mumfrey.liteloader.debug;

import java.util.UUID;

import net.minecraft.launcher.authentication.yggdrasil.Agent;

@SuppressWarnings("unused")
public class AuthenticationRequest
{
	private Agent agent;
	private String username;
	private String password;
	private String clientToken;
	
	public AuthenticationRequest(String username, String password)
	{
		this.agent = Agent.MINECRAFT;
		this.username = username;
		this.password = password;
		this.clientToken = UUID.randomUUID().toString();
	}
}

