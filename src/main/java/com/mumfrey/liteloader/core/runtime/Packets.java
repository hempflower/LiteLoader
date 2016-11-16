/*
 * This file is part of LiteLoader.
 * Copyright (C) 2012-16 Adam Mummery-Smith
 * All Rights Reserved.
 */
package com.mumfrey.liteloader.core.runtime;

import java.util.HashMap;
import java.util.Map;

/**
 * Packet obfuscation table
 *
 * @author Adam Mummery-Smith
 * TODO Obfuscation 1.11
 */
public final class Packets extends Obf
{
    /**
     * Since we need to catch and deal with the fact that a packet is first
     * marshalled across threads via PacketThreadUtil, we will need to know
     * which owner object to check against the current thread in order to detect
     * when the packet instance is being processed by the main message loop. The
     * Context object describes in which context (client or server) that a
     * particular packet will be processed in on the <em>receiving</em> end, and
     * thus which object to check threading against.
     * 
     * @author Adam Mummery-Smith
     */
    public enum Context
    {
        CLIENT,
        SERVER
    }
    
    // CHECKSTYLE:OFF

    private static Map<String, Packets> packetMap = new HashMap<String, Packets>();

    public static Packets C00Handshake                          = new Packets("net/minecraft/network/handshake/client/C00Handshake",                     "jp",   Context.SERVER);
    public static Packets CPacketEncryptionResponse             = new Packets("net/minecraft/network/login/client/CPacketEncryptionResponse",            "jz",   Context.SERVER);
    public static Packets CPacketLoginStart                     = new Packets("net/minecraft/network/login/client/CPacketLoginStart",                    "jy",   Context.SERVER);
    public static Packets SPacketDisconnectLogin                = new Packets("net/minecraft/network/login/server/SPacketDisconnect",                    "jw",   Context.CLIENT);
    public static Packets SPacketEnableCompression              = new Packets("net/minecraft/network/login/server/SPacketEnableCompression",             "jv",   Context.CLIENT);
    public static Packets SPacketEncryptionRequest              = new Packets("net/minecraft/network/login/server/SPacketEncryptionRequest",             "ju",   Context.CLIENT);
    public static Packets SPacketLoginSuccess                   = new Packets("net/minecraft/network/login/server/SPacketLoginSuccess",                  "jt",   Context.CLIENT);
    public static Packets CPacketAnimation                      = new Packets("net/minecraft/network/play/client/CPacketAnimation",                      "jk",   Context.SERVER);
    public static Packets CPacketChatMessage                    = new Packets("net/minecraft/network/play/client/CPacketChatMessage",                    "ip",   Context.SERVER);
    public static Packets CPacketClickWindow                    = new Packets("net/minecraft/network/play/client/CPacketClickWindow",                    "iu",   Context.SERVER);
    public static Packets CPacketClientSettings                 = new Packets("net/minecraft/network/play/client/CPacketClientSettings",                 "ir",   Context.SERVER);
    public static Packets CPacketClientStatus                   = new Packets("net/minecraft/network/play/client/CPacketClientStatus",                   "iq",   Context.SERVER);
    public static Packets CPacketCloseWindow                    = new Packets("net/minecraft/network/play/client/CPacketCloseWindow",                    "iv",   Context.SERVER);
    public static Packets CPacketConfirmTeleport                = new Packets("net/minecraft/network/play/client/CPacketConfirmTeleport",                "in",   Context.SERVER);
    public static Packets CPacketConfirmTransaction             = new Packets("net/minecraft/network/play/client/CPacketConfirmTransaction",             "is",   Context.SERVER);
    public static Packets CPacketCreativeInventoryAction        = new Packets("net/minecraft/network/play/client/CPacketCreativeInventoryAction",        "ji",   Context.SERVER);
    public static Packets CPacketCustomPayload                  = new Packets("net/minecraft/network/play/client/CPacketCustomPayload",                  "iw",   Context.SERVER);
    public static Packets CPacketEnchantItem                    = new Packets("net/minecraft/network/play/client/CPacketEnchantItem",                    "it",   Context.SERVER);
    public static Packets CPacketEntityAction                   = new Packets("net/minecraft/network/play/client/CPacketEntityAction",                   "je",   Context.SERVER);
    public static Packets CPacketHeldItemChange                 = new Packets("net/minecraft/network/play/client/CPacketHeldItemChange",                 "jh",   Context.SERVER);
    public static Packets CPacketInput                          = new Packets("net/minecraft/network/play/client/CPacketInput",                          "jf",   Context.SERVER);
    public static Packets CPacketKeepAlive                      = new Packets("net/minecraft/network/play/client/CPacketKeepAlive",                      "iy",   Context.SERVER);
    public static Packets CPacketPlayer                         = new Packets("net/minecraft/network/play/client/CPacketPlayer",                         "iz",   Context.SERVER);
    public static Packets CPacketPlayerPosition                 = new Packets("net/minecraft/network/play/client/CPacketPlayer$Position",                "iz$a", Context.SERVER);
    public static Packets CPacketPlayerPositionRotation         = new Packets("net/minecraft/network/play/client/CPacketPlayer$PositionRotation",        "iz$b", Context.SERVER);
    public static Packets CPacketPlayerRotation                 = new Packets("net/minecraft/network/play/client/CPacketPlayer$Rotation",                "iz$c", Context.SERVER);
    public static Packets CPacketPlayerAbilities                = new Packets("net/minecraft/network/play/client/CPacketPlayerAbilities",                "jc",   Context.SERVER);
    public static Packets CPacketPlayerDigging                  = new Packets("net/minecraft/network/play/client/CPacketPlayerDigging",                  "jd",   Context.SERVER);
    public static Packets CPacketPlayerTryUseItem               = new Packets("net/minecraft/network/play/client/CPacketPlayerTryUseItem",               "jn",   Context.SERVER);
    public static Packets CPacketPlayerTryUseItemOnBlock        = new Packets("net/minecraft/network/play/client/CPacketPlayerTryUseItemOnBlock",        "jm",   Context.SERVER);
    public static Packets CPacketResourcePackStatus             = new Packets("net/minecraft/network/play/client/CPacketResourcePackStatus",             "jg",   Context.SERVER);
    public static Packets CPacketSpectate                       = new Packets("net/minecraft/network/play/client/CPacketSpectate",                       "jl",   Context.SERVER);
    public static Packets CPacketSteerBoat                      = new Packets("net/minecraft/network/play/client/CPacketSteerBoat",                      "jb",   Context.SERVER);
    public static Packets CPacketTabComplete                    = new Packets("net/minecraft/network/play/client/CPacketTabComplete",                    "io",   Context.SERVER);
    public static Packets CPacketUpdateSign                     = new Packets("net/minecraft/network/play/client/CPacketUpdateSign",                     "jj",   Context.SERVER);
    public static Packets CPacketUseEntity                      = new Packets("net/minecraft/network/play/client/CPacketUseEntity",                      "ix",   Context.SERVER);
    public static Packets CPacketVehicleMove                    = new Packets("net/minecraft/network/play/client/CPacketVehicleMove",                    "ja",   Context.SERVER);
    public static Packets SPacketAnimation                      = new Packets("net/minecraft/network/play/server/SPacketAnimation",                      "fw",   Context.CLIENT);
    public static Packets SPacketBlockAction                    = new Packets("net/minecraft/network/play/server/SPacketBlockAction",                    "ga",   Context.CLIENT);
    public static Packets SPacketBlockBreakAnim                 = new Packets("net/minecraft/network/play/server/SPacketBlockBreakAnim",                 "fy",   Context.CLIENT);
    public static Packets SPacketBlockChange                    = new Packets("net/minecraft/network/play/server/SPacketBlockChange",                    "gb",   Context.CLIENT);
    public static Packets SPacketCamera                         = new Packets("net/minecraft/network/play/server/SPacketCamera",                         "hp",   Context.CLIENT);
    public static Packets SPacketChangeGameState                = new Packets("net/minecraft/network/play/server/SPacketChangeGameState",                "gu",   Context.CLIENT);
    public static Packets SPacketChat                           = new Packets("net/minecraft/network/play/server/SPacketChat",                           "gf",   Context.CLIENT);
    public static Packets SPacketChunkData                      = new Packets("net/minecraft/network/play/server/SPacketChunkData",                      "gw",   Context.CLIENT);
    public static Packets SPacketCloseWindow                    = new Packets("net/minecraft/network/play/server/SPacketCloseWindow",                    "gi",   Context.CLIENT);
    public static Packets SPacketCollectItem                    = new Packets("net/minecraft/network/play/server/SPacketCollectItem",                    "ii",   Context.CLIENT);
    public static Packets SPacketCombatEvent                    = new Packets("net/minecraft/network/play/server/SPacketCombatEvent",                    "hf",   Context.CLIENT);
    public static Packets SPacketConfirmTransaction             = new Packets("net/minecraft/network/play/server/SPacketConfirmTransaction",             "gh",   Context.CLIENT);
    public static Packets SPacketCooldown                       = new Packets("net/minecraft/network/play/server/SPacketCooldown",                       "gn",   Context.CLIENT);
    public static Packets SPacketCustomPayload                  = new Packets("net/minecraft/network/play/server/SPacketCustomPayload",                  "go",   Context.CLIENT);
    public static Packets SPacketCustomSound                    = new Packets("net/minecraft/network/play/server/SPacketCustomSound",                    "gp",   Context.CLIENT);
    public static Packets SPacketDestroyEntities                = new Packets("net/minecraft/network/play/server/SPacketDestroyEntities",                "hj",   Context.CLIENT);
    public static Packets SPacketDisconnect                     = new Packets("net/minecraft/network/play/server/SPacketDisconnect",                     "gq",   Context.CLIENT);
    public static Packets SPacketDisplayObjective               = new Packets("net/minecraft/network/play/server/SPacketDisplayObjective",               "hr",   Context.CLIENT);
    public static Packets SPacketEffect                         = new Packets("net/minecraft/network/play/server/SPacketEffect",                         "gx",   Context.CLIENT);
    public static Packets SPacketEntity                         = new Packets("net/minecraft/network/play/server/SPacketEntity",                         "hb",   Context.CLIENT);
    public static Packets S15PacketEntityRelMove                = new Packets("net/minecraft/network/play/server/SPacketEntity$S15PacketEntityRelMove",  "hb$a", Context.CLIENT);
    public static Packets S16PacketEntityLook                   = new Packets("net/minecraft/network/play/server/SPacketEntity$S16PacketEntityLook",     "hb$c", Context.CLIENT);
    public static Packets S17PacketEntityLookMove               = new Packets("net/minecraft/network/play/server/SPacketEntity$S17PacketEntityLookMove", "hb$b", Context.CLIENT);
    public static Packets SPacketEntityAttach                   = new Packets("net/minecraft/network/play/server/SPacketEntityAttach",                   "ht",   Context.CLIENT);
    public static Packets SPacketEntityEffect                   = new Packets("net/minecraft/network/play/server/SPacketEntityEffect",                   "il",   Context.CLIENT);
    public static Packets SPacketEntityEquipment                = new Packets("net/minecraft/network/play/server/SPacketEntityEquipment",                "hv",   Context.CLIENT);
    public static Packets SPacketEntityHeadLook                 = new Packets("net/minecraft/network/play/server/SPacketEntityHeadLook",                 "hn",   Context.CLIENT);
    public static Packets SPacketEntityMetadata                 = new Packets("net/minecraft/network/play/server/SPacketEntityMetadata",                 "hs",   Context.CLIENT);
    public static Packets SPacketEntityProperties               = new Packets("net/minecraft/network/play/server/SPacketEntityProperties",               "ik",   Context.CLIENT);
    public static Packets SPacketEntityStatus                   = new Packets("net/minecraft/network/play/server/SPacketEntityStatus",                   "gr",   Context.CLIENT);
    public static Packets SPacketEntityTeleport                 = new Packets("net/minecraft/network/play/server/SPacketEntityTeleport",                 "ij",   Context.CLIENT);
    public static Packets SPacketEntityVelocity                 = new Packets("net/minecraft/network/play/server/SPacketEntityVelocity",                 "hu",   Context.CLIENT);
    public static Packets SPacketExplosion                      = new Packets("net/minecraft/network/play/server/SPacketExplosion",                      "gs",   Context.CLIENT);
    public static Packets SPacketHeldItemChange                 = new Packets("net/minecraft/network/play/server/SPacketHeldItemChange",                 "hq",   Context.CLIENT);
    public static Packets SPacketJoinGame                       = new Packets("net/minecraft/network/play/server/SPacketJoinGame",                       "gz",   Context.CLIENT);
    public static Packets SPacketKeepAlive                      = new Packets("net/minecraft/network/play/server/SPacketKeepAlive",                      "gv",   Context.CLIENT);
    public static Packets SPacketMaps                           = new Packets("net/minecraft/network/play/server/SPacketMaps",                           "ha",   Context.CLIENT);
    public static Packets SPacketMoveVehicle                    = new Packets("net/minecraft/network/play/server/SPacketMoveVehicle",                    "hc",   Context.CLIENT);
    public static Packets SPacketMultiBlockChange               = new Packets("net/minecraft/network/play/server/SPacketMultiBlockChange",               "gg",   Context.CLIENT);
    public static Packets SPacketOpenWindow                     = new Packets("net/minecraft/network/play/server/SPacketOpenWindow",                     "gj",   Context.CLIENT);
    public static Packets SPacketParticles                      = new Packets("net/minecraft/network/play/server/SPacketParticles",                      "gy",   Context.CLIENT);
    public static Packets SPacketPlayerAbilities                = new Packets("net/minecraft/network/play/server/SPacketPlayerAbilities",                "he",   Context.CLIENT);
    public static Packets SPacketPlayerListHeaderFooter         = new Packets("net/minecraft/network/play/server/SPacketPlayerListHeaderFooter",         "ih",   Context.CLIENT);
    public static Packets SPacketPlayerListItem                 = new Packets("net/minecraft/network/play/server/SPacketPlayerListItem",                 "hg",   Context.CLIENT);
    public static Packets SPacketPlayerPosLook                  = new Packets("net/minecraft/network/play/server/SPacketPlayerPosLook",                  "hh",   Context.CLIENT);
    public static Packets SPacketRemoveEntityEffect             = new Packets("net/minecraft/network/play/server/SPacketRemoveEntityEffect",             "hk",   Context.CLIENT);
    public static Packets SPacketResourcePackSend               = new Packets("net/minecraft/network/play/server/SPacketResourcePackSend",               "hl",   Context.CLIENT);
    public static Packets SPacketRespawn                        = new Packets("net/minecraft/network/play/server/SPacketRespawn",                        "hm",   Context.CLIENT);
    public static Packets SPacketScoreboardObjective            = new Packets("net/minecraft/network/play/server/SPacketScoreboardObjective",            "hy",   Context.CLIENT);
    public static Packets SPacketServerDifficulty               = new Packets("net/minecraft/network/play/server/SPacketServerDifficulty",               "gd",   Context.CLIENT);
    public static Packets SPacketSetExperience                  = new Packets("net/minecraft/network/play/server/SPacketSetExperience",                  "hw",   Context.CLIENT);
    public static Packets SPacketSetPassengers                  = new Packets("net/minecraft/network/play/server/SPacketSetPassengers",                  "hz",   Context.CLIENT);
    public static Packets SPacketSetSlot                        = new Packets("net/minecraft/network/play/server/SPacketSetSlot",                        "gm",   Context.CLIENT);
    public static Packets SPacketSignEditorOpen                 = new Packets("net/minecraft/network/play/server/SPacketSignEditorOpen",                 "hd",   Context.CLIENT);
    public static Packets SPacketSoundEffect                    = new Packets("net/minecraft/network/play/server/SPacketSoundEffect",                    "ig",   Context.CLIENT);
    public static Packets SPacketSpawnExperienceOrb             = new Packets("net/minecraft/network/play/server/SPacketSpawnExperienceOrb",             "fr",   Context.CLIENT);
    public static Packets SPacketSpawnGlobalEntity              = new Packets("net/minecraft/network/play/server/SPacketSpawnGlobalEntity",              "fs",   Context.CLIENT);
    public static Packets SPacketSpawnMob                       = new Packets("net/minecraft/network/play/server/SPacketSpawnMob",                       "ft",   Context.CLIENT);
    public static Packets SPacketSpawnObject                    = new Packets("net/minecraft/network/play/server/SPacketSpawnObject",                    "fq",   Context.CLIENT);
    public static Packets SPacketSpawnPainting                  = new Packets("net/minecraft/network/play/server/SPacketSpawnPainting",                  "fu",   Context.CLIENT);
    public static Packets SPacketSpawnPlayer                    = new Packets("net/minecraft/network/play/server/SPacketSpawnPlayer",                    "fv",   Context.CLIENT);
    public static Packets SPacketSpawnPosition                  = new Packets("net/minecraft/network/play/server/SPacketSpawnPosition",                  "ic",   Context.CLIENT);
    public static Packets SPacketStatistics                     = new Packets("net/minecraft/network/play/server/SPacketStatistics",                     "fx",   Context.CLIENT);
    public static Packets SPacketTabComplete                    = new Packets("net/minecraft/network/play/server/SPacketTabComplete",                    "ge",   Context.CLIENT);
    public static Packets SPacketTeams                          = new Packets("net/minecraft/network/play/server/SPacketTeams",                          "ia",   Context.CLIENT);
    public static Packets SPacketTimeUpdate                     = new Packets("net/minecraft/network/play/server/SPacketTimeUpdate",                     "id",   Context.CLIENT);
    public static Packets SPacketTitle                          = new Packets("net/minecraft/network/play/server/SPacketTitle",                          "ie",   Context.CLIENT);
    public static Packets SPacketUnloadChunk                    = new Packets("net/minecraft/network/play/server/SPacketUnloadChunk",                    "gt",   Context.CLIENT);
    public static Packets SPacketUpdateBossInfo                 = new Packets("net/minecraft/network/play/server/SPacketUpdateBossInfo",                 "gc",   Context.CLIENT);
    public static Packets SPacketUpdateHealth                   = new Packets("net/minecraft/network/play/server/SPacketUpdateHealth",                   "hx",   Context.CLIENT);
    public static Packets SPacketUpdateScore                    = new Packets("net/minecraft/network/play/server/SPacketUpdateScore",                    "ib",   Context.CLIENT);
    public static Packets SPacketUpdateTileEntity               = new Packets("net/minecraft/network/play/server/SPacketUpdateTileEntity",               "fz",   Context.CLIENT);
    public static Packets SPacketUseBed                         = new Packets("net/minecraft/network/play/server/SPacketUseBed",                         "hi",   Context.CLIENT);
    public static Packets SPacketWindowItems                    = new Packets("net/minecraft/network/play/server/SPacketWindowItems",                    "gk",   Context.CLIENT);
    public static Packets SPacketWindowProperty                 = new Packets("net/minecraft/network/play/server/SPacketWindowProperty",                 "gl",   Context.CLIENT);
    public static Packets SPacketWorldBorder                    = new Packets("net/minecraft/network/play/server/SPacketWorldBorder",                    "ho",   Context.CLIENT);
    public static Packets CPacketPing                           = new Packets("net/minecraft/network/status/client/CPacketPing",                         "kh",   Context.SERVER);
    public static Packets CPacketServerQuery                    = new Packets("net/minecraft/network/status/client/CPacketServerQuery",                  "ki",   Context.SERVER);
    public static Packets SPacketPong                           = new Packets("net/minecraft/network/status/server/SPacketPong",                         "kd",   Context.CLIENT);
    public static Packets SPacketServerInfo                     = new Packets("net/minecraft/network/status/server/SPacketServerInfo",                   "ke",   Context.CLIENT);

    // CHECKSTYLE:ON

    public static final Packets[] packets = new Packets[] {
            CPacketEncryptionResponse,
            CPacketLoginStart,
            SPacketDisconnectLogin,
            SPacketEnableCompression,
            SPacketEncryptionRequest,
            SPacketLoginSuccess,
            CPacketAnimation,
            CPacketChatMessage,
            CPacketClickWindow,
            CPacketClientSettings,
            CPacketClientStatus,
            CPacketCloseWindow,
            CPacketConfirmTeleport,
            CPacketConfirmTransaction,
            CPacketCreativeInventoryAction,
            CPacketCustomPayload,
            CPacketEnchantItem,
            CPacketEntityAction,
            CPacketHeldItemChange,
            CPacketInput,
            C00Handshake,
            CPacketKeepAlive,
            CPacketPlayer,
            CPacketPlayerPosition,
            CPacketPlayerPositionRotation,
            CPacketPlayerRotation,
            CPacketPlayerAbilities,
            CPacketPlayerDigging,
            CPacketPlayerTryUseItem,
            CPacketPlayerTryUseItemOnBlock,
            CPacketResourcePackStatus,
            CPacketSpectate,
            CPacketSteerBoat,
            CPacketTabComplete,
            CPacketUpdateSign,
            CPacketUseEntity,
            CPacketVehicleMove,
            SPacketAnimation,
            SPacketBlockAction,
            SPacketBlockBreakAnim,
            SPacketBlockChange,
            SPacketCamera,
            SPacketChangeGameState,
            SPacketChat,
            SPacketChunkData,
            SPacketCloseWindow,
            SPacketCollectItem,
            SPacketCombatEvent,
            SPacketConfirmTransaction,
            SPacketCooldown,
            SPacketCustomPayload,
            SPacketCustomSound,
            SPacketDestroyEntities,
            SPacketDisconnect,
            SPacketDisplayObjective,
            SPacketEffect,
            SPacketEntity,
            S15PacketEntityRelMove,
            S16PacketEntityLook,
            S17PacketEntityLookMove,
            SPacketEntityAttach,
            SPacketEntityEffect,
            SPacketEntityEquipment,
            SPacketEntityHeadLook,
            SPacketEntityMetadata,
            SPacketEntityProperties,
            SPacketEntityStatus,
            SPacketEntityTeleport,
            SPacketEntityVelocity,
            SPacketExplosion,
            SPacketHeldItemChange,
            SPacketJoinGame,
            SPacketKeepAlive,
            SPacketMaps,
            SPacketMoveVehicle,
            SPacketMultiBlockChange,
            SPacketOpenWindow,
            SPacketParticles,
            SPacketPlayerAbilities,
            SPacketPlayerListHeaderFooter,
            SPacketPlayerListItem,
            SPacketPlayerPosLook,
            SPacketRemoveEntityEffect,
            SPacketResourcePackSend,
            SPacketRespawn,
            SPacketScoreboardObjective,
            SPacketServerDifficulty,
            SPacketSetExperience,
            SPacketSetPassengers,
            SPacketSetSlot,
            SPacketSignEditorOpen,
            SPacketSoundEffect,
            SPacketSpawnExperienceOrb,
            SPacketSpawnGlobalEntity,
            SPacketSpawnMob,
            SPacketSpawnObject,
            SPacketSpawnPainting,
            SPacketSpawnPlayer,
            SPacketSpawnPosition,
            SPacketStatistics,
            SPacketTabComplete,
            SPacketTeams,
            SPacketTimeUpdate,
            SPacketTitle,
            SPacketUnloadChunk,
            SPacketUpdateBossInfo,
            SPacketUpdateHealth,
            SPacketUpdateScore,
            SPacketUpdateTileEntity,
            SPacketUseBed,
            SPacketWindowItems,
            SPacketWindowProperty,
            SPacketWorldBorder,
            CPacketPing,
            CPacketServerQuery,
            SPacketPong,
            SPacketServerInfo
    };

    private static int nextPacketIndex;

    private final String shortName;

    private final int index;

    private final Context context;

    private Packets(String seargeName, String obfName, Context context)
    {
        super(seargeName.replace('/', '.'), obfName);
        
        this.shortName = Packets.getShortName(seargeName);
        this.index = Packets.nextPacketIndex++;
        Packets.packetMap.put(this.shortName, this);
        this.context = context;
    }

    public int getIndex()
    {
        return this.index;
    }

    public String getShortName()
    {
        return this.shortName;
    }

    public Context getContext()
    {
        return this.context;
    }

    public static int indexOf(String packetClassName)
    {
        for (Packets packet : Packets.packets)
        {
            if (packet.name.equals(packetClassName) || packet.shortName.equals(packetClassName) || packet.obf.equals(packetClassName))
            {
                return packet.index;
            }
        }

        return -1;
    }

    public static int count()
    {
        return Packets.nextPacketIndex;
    }

    /**
     * @param name
     */
    public static Packets getByName(String name)
    {
        return Packets.packetMap.get(name);
    }

    private static String getShortName(String className)
    {
        String simpleName = className.substring(Math.max(className.lastIndexOf('/'), className.lastIndexOf('$')) + 1);
        String baseName = simpleName.replaceAll("^[CS]([0-9A-F]{2})?Packet", "");
        return baseName + "Packet";
    }
}
