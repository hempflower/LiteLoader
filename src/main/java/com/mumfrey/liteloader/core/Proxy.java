/*
 * This file is part of LiteLoader.
 * Copyright (C) 2012-16 Adam Mummery-Smith
 * All Rights Reserved.
 */
package com.mumfrey.liteloader.core;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.authlib.GameProfile;
import com.mumfrey.liteloader.core.LiteLoaderEventBroker.InteractType;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.client.CPacketAnimation;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.network.play.client.CPacketPlayerDigging.Action;
import net.minecraft.network.play.client.CPacketPlayerTryUseItem;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public abstract class Proxy
{
    private static LiteLoaderEventBroker<?, ?> broker;

    protected Proxy() {}

    protected static void onStartupComplete()
    {
        Proxy.broker = LiteLoaderEventBroker.broker;

        if (Proxy.broker == null)
        {
            throw new RuntimeException("LiteLoader failed to start up properly."
                    + " The game is in an unstable state and must shut down now. Check the developer log for startup errors");
        }
    }

    public static void onInitializePlayerConnection(PlayerList source, NetworkManager netManager, EntityPlayerMP player)
    {
        Proxy.broker.onInitializePlayerConnection(source, netManager, player);
    }

    public static void onPlayerLogin(PlayerList source, EntityPlayerMP player)
    {
        Proxy.broker.onPlayerLogin(source, player);
    }

    public static void onPlayerLogout(PlayerList source, EntityPlayerMP player)
    {
        Proxy.broker.onPlayerLogout(source, player);
    }

    public static void onSpawnPlayer(CallbackInfoReturnable<EntityPlayerMP> cir, PlayerList source, GameProfile profile)
    {
        Proxy.broker.onSpawnPlayer(source, cir.getReturnValue(), profile);
    }

    public static void onRespawnPlayer(CallbackInfoReturnable<EntityPlayerMP> cir, PlayerList source, EntityPlayerMP oldPlayer,
            int dimension, boolean won)
    {
        Proxy.broker.onRespawnPlayer(source, cir.getReturnValue(), oldPlayer, dimension, won);
    }

    public static void onServerTick(MinecraftServer mcServer)
    {
        Proxy.broker.onServerTick(mcServer);
    }

    public static void onPlaceBlock(CallbackInfo ci, NetHandlerPlayServer netHandler, CPacketPlayerTryUseItem packet)
    {
        // Potentially not needed any more
//        if (!Proxy.broker.onPlaceBlock(netHandler, netHandler.playerEntity, packet.getPosition(),
//                EnumFacing.getFront(packet.getPlacedBlockDirection())))
//        {
//            ci.cancel();
//        }
    }

    public static void onClickedAir(CallbackInfo ci, NetHandlerPlayServer netHandler, CPacketAnimation packet)
    {
        if (!Proxy.broker.onClickedAir(InteractType.LEFT_CLICK, netHandler.playerEntity, packet.getHand()))
        {
            ci.cancel();
        }
    }

    public static void onPlayerDigging(CallbackInfo ci, NetHandlerPlayServer netHandler, CPacketPlayerDigging packet)
    {
        Action action = packet.getAction();
        EntityPlayerMP player = netHandler.playerEntity;
        if (action == Action.START_DESTROY_BLOCK)
        {
            if (!Proxy.broker.onPlayerDigging(InteractType.DIG_BLOCK_MAYBE, player, netHandler, packet.getPosition()))
            {
                ci.cancel();
            }
        }
        else if (action == Action.ABORT_DESTROY_BLOCK || action == Action.STOP_DESTROY_BLOCK)
        {
            Proxy.broker.onPlayerDigging(InteractType.DIG_BLOCK_END, player, netHandler, packet.getPosition());
        }
        else if (action == Action.SWAP_HELD_ITEMS)
        {
            if (!Proxy.broker.onPlayerSwapItems(player))
            {
                ci.cancel();
            }
        }
    }

    public static void onRightClickBlock(CallbackInfoReturnable<EnumActionResult> ci, EntityPlayer player, World world, ItemStack stack,
            EnumHand hand, BlockPos pos, EnumFacing side, float offsetX, float offsetY, float offsetZ)
    {
        if (!(player instanceof EntityPlayerMP))
        {
            return;
        }

        if (!Proxy.broker.onUseItem((EntityPlayerMP)player, hand, stack, pos, side))
        {
            ci.setReturnValue(EnumActionResult.FAIL);
        }
    }

    public static void postRightClickBlock(CallbackInfoReturnable<EnumActionResult> cir, EntityPlayer player, World world, ItemStack stack,
            EnumHand hand, BlockPos pos, EnumFacing side, float offsetX, float offsetY, float offsetZ)
    {
        if (!(player instanceof EntityPlayerMP))
        {
            return;
        }
        
        System.err.printf("@@ postRightClickBlock: %s\n", cir.getReturnValue());
    }

    public static void onRightClick(CallbackInfoReturnable<EnumActionResult> cir, EntityPlayer player, World worldIn, ItemStack stack, EnumHand hand)
    {
        if (!(player instanceof EntityPlayerMP))
        {
            return;
        }

        if (!Proxy.broker.onClickedAir(InteractType.RIGHT_CLICK, (EntityPlayerMP)player, hand))
        {
            cir.setReturnValue(EnumActionResult.FAIL);
        }
    }

    public static void onBlockClicked(CallbackInfo ci, PlayerInteractionManager manager, BlockPos pos, EnumFacing side)
    {
        if (!Proxy.broker.onBlockClicked(pos, side, manager))
        {
            ci.cancel();
        }
    }

    public static void onPlayerMoved(CallbackInfo ci, NetHandlerPlayServer netHandler, CPacketPlayer packet, WorldServer world, double oldPosX,
            double oldPosY, double oldPosZ)
    {
        if (!Proxy.broker.onPlayerMove(netHandler, packet, netHandler.playerEntity, world))
        {
            ci.cancel();
        }
    }

    public static void onPlayerMoved(CallbackInfo ci, NetHandlerPlayServer netHandler, CPacketPlayer packet, WorldServer world)
    {
        if (!Proxy.broker.onPlayerMove(netHandler, packet, netHandler.playerEntity, world))
        {
            ci.cancel();
        }
    }
}
