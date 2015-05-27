package com.mumfrey.liteloader.core;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ItemInWorldManager;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import com.mojang.authlib.GameProfile;
import com.mumfrey.liteloader.transformers.event.EventInfo;
import com.mumfrey.liteloader.transformers.event.ReturnEventInfo;

public abstract class CallbackProxyCommon
{
	private static LiteLoaderEventBroker<?, ?> eventBroker;
	
	protected CallbackProxyCommon() {}

	protected static void onStartupComplete()
	{
		CallbackProxyCommon.eventBroker = LiteLoaderEventBroker.broker;
		
		if (CallbackProxyCommon.eventBroker == null)
		{
			throw new RuntimeException("LiteLoader failed to start up properly. The game is in an unstable state and must shut down now. Check the developer log for startup errors");
		}
	}
	
	public static void onInitializePlayerConnection(EventInfo<ServerConfigurationManager> e, NetworkManager netManager, EntityPlayerMP player)
	{
		CallbackProxyCommon.eventBroker.onInitializePlayerConnection(e.getSource(), netManager, player);
	}

	public static void onPlayerLogin(EventInfo<ServerConfigurationManager> e, EntityPlayerMP player)
	{
		CallbackProxyCommon.eventBroker.onPlayerLogin(e.getSource(), player);
	}
	
	public static void onPlayerLogout(EventInfo<ServerConfigurationManager> e, EntityPlayerMP player)
	{
		CallbackProxyCommon.eventBroker.onPlayerLogout(e.getSource(), player);
	}
	
	public static void onSpawnPlayer(ReturnEventInfo<ServerConfigurationManager, EntityPlayerMP> e, GameProfile profile)
	{
		CallbackProxyCommon.eventBroker.onSpawnPlayer(e.getSource(), e.getReturnValue(), profile);
	}

	public static void onRespawnPlayer(ReturnEventInfo<ServerConfigurationManager, EntityPlayerMP> e, EntityPlayerMP oldPlayer, int dimension, boolean won)
	{
		CallbackProxyCommon.eventBroker.onRespawnPlayer(e.getSource(), e.getReturnValue(), oldPlayer, dimension, won);
	}
	
	public static void onServerTick(EventInfo<MinecraftServer> e)
	{
		CallbackProxyCommon.eventBroker.onServerTick(e.getSource());
	}
	
	public static void onPlaceBlock(EventInfo<NetHandlerPlayServer> e, C08PacketPlayerBlockPlacement packet)
	{
		NetHandlerPlayServer netHandler = e.getSource();
		if (!CallbackProxyCommon.eventBroker.onPlaceBlock(netHandler, netHandler.playerEntity, packet.getPosition(), EnumFacing.getFront(packet.getPlacedBlockDirection())))
		{
			e.cancel();
		}
	}
	
	public static void onClickedAir(EventInfo<NetHandlerPlayServer> e, C0APacketAnimation packet)
	{
		if (!CallbackProxyCommon.eventBroker.onClickedAir(e.getSource()))
		{
			e.cancel();
		}
	}

	public static void onPlayerDigging(EventInfo<NetHandlerPlayServer> e, C07PacketPlayerDigging packet)
	{
		if (packet.getStatus() == C07PacketPlayerDigging.Action.START_DESTROY_BLOCK)
		{
			NetHandlerPlayServer netHandler = e.getSource();
			if (!CallbackProxyCommon.eventBroker.onPlayerDigging(netHandler, packet.getPosition(), netHandler.playerEntity))
			{
				e.cancel();
			}
		}
	}
	
	public static void onUseItem(ReturnEventInfo<ItemInWorldManager, Boolean> e, EntityPlayer player, World world, ItemStack itemStack, BlockPos pos, EnumFacing side, float par8, float par9, float par10)
	{
		if (!(player instanceof EntityPlayerMP))
		{
			return;
		}
			
		if (!CallbackProxyCommon.eventBroker.onUseItem(pos, side, (EntityPlayerMP)player))
		{
			e.setReturnValue(false);
		}
	}
	
	public static void onBlockClicked(EventInfo<ItemInWorldManager> e, BlockPos pos, EnumFacing side)
	{
		ItemInWorldManager manager = e.getSource();
		
		if (!CallbackProxyCommon.eventBroker.onBlockClicked(pos, side, manager))
		{
			e.cancel();
		}
	}
	
	public static void onPlayerMoved(EventInfo<NetHandlerPlayServer> e, C03PacketPlayer packet, WorldServer world, double oldPosX, double oldPosY, double oldPosZ)
	{
		NetHandlerPlayServer netHandler = e.getSource();
		if (!CallbackProxyCommon.eventBroker.onPlayerMove(netHandler, packet, netHandler.playerEntity, world))
		{
			e.cancel();
		}
	}
	
	public static void onPlayerMoved(EventInfo<NetHandlerPlayServer> e, C03PacketPlayer packet, WorldServer world, double oldPosX, double oldPosY, double oldPosZ, double deltaMoveSq, double deltaX, double deltaY, double deltaZ)
	{
		NetHandlerPlayServer netHandler = e.getSource();
		if (!CallbackProxyCommon.eventBroker.onPlayerMove(netHandler, packet, netHandler.playerEntity, world))
		{
			e.cancel();
		}
	}
}
