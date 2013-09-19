package com.mumfrey.liteloader.core.hooks;

import com.mumfrey.liteloader.core.Events;

import net.minecraft.src.EnumGameType;
import net.minecraft.src.NetHandler;
import net.minecraft.src.Packet1Login;
import net.minecraft.src.WorldType;

public class HookLogin extends Packet1Login
{
	public static Events events;
	
	public HookLogin()
	{
		super();
	}
	
	public HookLogin(int par1, WorldType par2WorldType, EnumGameType par3EnumGameType, boolean par4, int par5, int par6, int par7, int par8)
	{
		super(par1, par2WorldType, par3EnumGameType, par4, par5, par6, par7, par8);
	}

	/* (non-Javadoc)
	 * @see net.minecraft.src.Packet1Login#processPacket(net.minecraft.src.NetHandler)
	 */
	@Override
	public void processPacket(NetHandler par1NetHandler)
	{
		if (events == null || events.onPreLogin(par1NetHandler, this))
		{
			super.processPacket(par1NetHandler);

			if (events != null) events.onConnectToServer(par1NetHandler, this);
		}
	}
}
