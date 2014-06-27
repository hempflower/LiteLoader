package com.mumfrey.liteloader.server;

import net.minecraft.network.INetHandler;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.world.World;

import com.mumfrey.liteloader.api.CoreProvider;
import com.mumfrey.liteloader.common.GameEngine;
import com.mumfrey.liteloader.core.LiteLoader;
import com.mumfrey.liteloader.core.LiteLoaderMods;
import com.mumfrey.liteloader.launch.LoaderProperties;
import com.mumfrey.liteloader.resources.InternalResourcePack;

/**
 * CoreProvider which fixes SoundManager derping up at startup
 * 
 * @author Adam Mummery-Smith
 */
public class LiteLoaderCoreProviderServer implements CoreProvider
{
	/**
	 * Loader Properties adapter 
	 */
//	private final LoaderProperties properties;
	
	public LiteLoaderCoreProviderServer(LoaderProperties properties)
	{
//		this.properties = properties;
	}

	@Override
	public void onInit()
	{
	}
	
	@Override
	public void onPostInit(GameEngine<?, ?> engine)
	{
		// Add self as a resource pack for texture/lang resources
		LiteLoader.getInstance().registerModResourcePack(new InternalResourcePack("LiteLoader", LiteLoader.class, "liteloader"));
	}
	
	@Override
	public void onPostInitComplete(LiteLoaderMods mods)
	{
	}
	
	@Override
	public void onStartupComplete()
	{
	}
	
	@Override
	public void onJoinGame(INetHandler netHandler, S01PacketJoinGame loginPacket)
	{
	}
	
	@Override
	public void onPostRender(int mouseX, int mouseY, float partialTicks)
	{
	}
	
	@Override
	public void onTick(boolean clock, float partialTicks, boolean inGame)
	{
	}
	
	@Override
	public void onWorldChanged(World world)
	{
	}
	
	@Override
	public void onShutDown()
	{
	}
}
