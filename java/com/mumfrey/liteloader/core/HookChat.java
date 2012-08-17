package com.mumfrey.liteloader.core;

import net.minecraft.src.NetHandler;
import net.minecraft.src.Packet3Chat;

public class HookChat extends Packet3Chat
{
	public static LiteLoader loader;
	
	public HookChat()
	{
		super();
	}
	
	public HookChat(String par1Str)
	{
		super(par1Str);
	}
	
	public HookChat(String par1Str, boolean par2)
	{
		super(par1Str, par2);
	}

	/* (non-Javadoc)
	 * @see net.minecraft.src.Packet3Chat#processPacket(net.minecraft.src.NetHandler)
	 */
	@Override
	public void processPacket(NetHandler par1NetHandler)
	{
		if (loader == null || loader.onChat(this))
		{
			super.processPacket(par1NetHandler);
		}
	}
}
