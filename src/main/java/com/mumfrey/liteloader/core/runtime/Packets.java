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
 * TODO Obfuscation 1.10
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

    public static Packets C00Handshake                          = new Packets("net/minecraft/network/handshake/client/C00Handshake",                     "jm",   Context.SERVER);
    public static Packets CPacketEncryptionResponse             = new Packets("net/minecraft/network/login/client/CPacketEncryptionResponse",            "jw",   Context.SERVER);
    public static Packets CPacketLoginStart                     = new Packets("net/minecraft/network/login/client/CPacketLoginStart",                    "jv",   Context.SERVER);
    public static Packets SPacketDisconnectLogin                = new Packets("net/minecraft/network/login/server/SPacketDisconnect",                    "jt",   Context.CLIENT);
    public static Packets SPacketEnableCompression              = new Packets("net/minecraft/network/login/server/SPacketEnableCompression",             "js",   Context.CLIENT);
    public static Packets SPacketEncryptionRequest              = new Packets("net/minecraft/network/login/server/SPacketEncryptionRequest",             "jr",   Context.CLIENT);
    public static Packets SPacketLoginSuccess                   = new Packets("net/minecraft/network/login/server/SPacketLoginSuccess",                  "jq",   Context.CLIENT);
    public static Packets CPacketAnimation                      = new Packets("net/minecraft/network/play/client/CPacketAnimation",                      "jh",   Context.SERVER);
    public static Packets CPacketChatMessage                    = new Packets("net/minecraft/network/play/client/CPacketChatMessage",                    "im",   Context.SERVER);
    public static Packets CPacketClickWindow                    = new Packets("net/minecraft/network/play/client/CPacketClickWindow",                    "ir",   Context.SERVER);
    public static Packets CPacketClientSettings                 = new Packets("net/minecraft/network/play/client/CPacketClientSettings",                 "io",   Context.SERVER);
    public static Packets CPacketClientStatus                   = new Packets("net/minecraft/network/play/client/CPacketClientStatus",                   "in",   Context.SERVER);
    public static Packets CPacketCloseWindow                    = new Packets("net/minecraft/network/play/client/CPacketCloseWindow",                    "is",   Context.SERVER);
    public static Packets CPacketConfirmTeleport                = new Packets("net/minecraft/network/play/client/CPacketConfirmTeleport",                "ik",   Context.SERVER);
    public static Packets CPacketConfirmTransaction             = new Packets("net/minecraft/network/play/client/CPacketConfirmTransaction",             "ip",   Context.SERVER);
    public static Packets CPacketCreativeInventoryAction        = new Packets("net/minecraft/network/play/client/CPacketCreativeInventoryAction",        "jf",   Context.SERVER);
    public static Packets CPacketCustomPayload                  = new Packets("net/minecraft/network/play/client/CPacketCustomPayload",                  "it",   Context.SERVER);
    public static Packets CPacketEnchantItem                    = new Packets("net/minecraft/network/play/client/CPacketEnchantItem",                    "iq",   Context.SERVER);
    public static Packets CPacketEntityAction                   = new Packets("net/minecraft/network/play/client/CPacketEntityAction",                   "jb",   Context.SERVER);
    public static Packets CPacketHeldItemChange                 = new Packets("net/minecraft/network/play/client/CPacketHeldItemChange",                 "je",   Context.SERVER);
    public static Packets CPacketInput                          = new Packets("net/minecraft/network/play/client/CPacketInput",                          "jc",   Context.SERVER);
    public static Packets CPacketKeepAlive                      = new Packets("net/minecraft/network/play/client/CPacketKeepAlive",                      "iv",   Context.SERVER);
    public static Packets CPacketPlayer                         = new Packets("net/minecraft/network/play/client/CPacketPlayer",                         "iw",   Context.SERVER);
    public static Packets CPacketPlayerPosition                 = new Packets("net/minecraft/network/play/client/CPacketPlayer$Position",                "iw$a", Context.SERVER);
    public static Packets CPacketPlayerPositionRotation         = new Packets("net/minecraft/network/play/client/CPacketPlayer$PositionRotation",        "iw$b", Context.SERVER);
    public static Packets CPacketPlayerRotation                 = new Packets("net/minecraft/network/play/client/CPacketPlayer$Rotation",                "iw$c", Context.SERVER);
    public static Packets CPacketPlayerAbilities                = new Packets("net/minecraft/network/play/client/CPacketPlayerAbilities",                "iz",   Context.SERVER);
    public static Packets CPacketPlayerDigging                  = new Packets("net/minecraft/network/play/client/CPacketPlayerDigging",                  "ja",   Context.SERVER);
    public static Packets CPacketPlayerTryUseItem               = new Packets("net/minecraft/network/play/client/CPacketPlayerTryUseItem",               "jk",   Context.SERVER);
    public static Packets CPacketPlayerTryUseItemOnBlock        = new Packets("net/minecraft/network/play/client/CPacketPlayerTryUseItemOnBlock",        "jj",   Context.SERVER);
    public static Packets CPacketResourcePackStatus             = new Packets("net/minecraft/network/play/client/CPacketResourcePackStatus",             "jd",   Context.SERVER);
    public static Packets CPacketSpectate                       = new Packets("net/minecraft/network/play/client/CPacketSpectate",                       "ji",   Context.SERVER);
    public static Packets CPacketSteerBoat                      = new Packets("net/minecraft/network/play/client/CPacketSteerBoat",                      "iy",   Context.SERVER);
    public static Packets CPacketTabComplete                    = new Packets("net/minecraft/network/play/client/CPacketTabComplete",                    "il",   Context.SERVER);
    public static Packets CPacketUpdateSign                     = new Packets("net/minecraft/network/play/client/CPacketUpdateSign",                     "jg",   Context.SERVER);
    public static Packets CPacketUseEntity                      = new Packets("net/minecraft/network/play/client/CPacketUseEntity",                      "iu",   Context.SERVER);
    public static Packets CPacketVehicleMove                    = new Packets("net/minecraft/network/play/client/CPacketVehicleMove",                    "ix",   Context.SERVER);
    public static Packets SPacketAnimation                      = new Packets("net/minecraft/network/play/server/SPacketAnimation",                      "ft",   Context.CLIENT);
    public static Packets SPacketBlockAction                    = new Packets("net/minecraft/network/play/server/SPacketBlockAction",                    "fx",   Context.CLIENT);
    public static Packets SPacketBlockBreakAnim                 = new Packets("net/minecraft/network/play/server/SPacketBlockBreakAnim",                 "fv",   Context.CLIENT);
    public static Packets SPacketBlockChange                    = new Packets("net/minecraft/network/play/server/SPacketBlockChange",                    "fy",   Context.CLIENT);
    public static Packets SPacketCamera                         = new Packets("net/minecraft/network/play/server/SPacketCamera",                         "hm",   Context.CLIENT);
    public static Packets SPacketChangeGameState                = new Packets("net/minecraft/network/play/server/SPacketChangeGameState",                "gr",   Context.CLIENT);
    public static Packets SPacketChat                           = new Packets("net/minecraft/network/play/server/SPacketChat",                           "gc",   Context.CLIENT);
    public static Packets SPacketChunkData                      = new Packets("net/minecraft/network/play/server/SPacketChunkData",                      "gt",   Context.CLIENT);
    public static Packets SPacketCloseWindow                    = new Packets("net/minecraft/network/play/server/SPacketCloseWindow",                    "gf",   Context.CLIENT);
    public static Packets SPacketCollectItem                    = new Packets("net/minecraft/network/play/server/SPacketCollectItem",                    "ie",   Context.CLIENT);
    public static Packets SPacketCombatEvent                    = new Packets("net/minecraft/network/play/server/SPacketCombatEvent",                    "hc",   Context.CLIENT);
    public static Packets SPacketConfirmTransaction             = new Packets("net/minecraft/network/play/server/SPacketConfirmTransaction",             "ge",   Context.CLIENT);
    public static Packets SPacketCooldown                       = new Packets("net/minecraft/network/play/server/SPacketCooldown",                       "gk",   Context.CLIENT);
    public static Packets SPacketCustomPayload                  = new Packets("net/minecraft/network/play/server/SPacketCustomPayload",                  "gl",   Context.CLIENT);
    public static Packets SPacketCustomSound                    = new Packets("net/minecraft/network/play/server/SPacketCustomSound",                    "gm",   Context.CLIENT);
    public static Packets SPacketDestroyEntities                = new Packets("net/minecraft/network/play/server/SPacketDestroyEntities",                "hg",   Context.CLIENT);
    public static Packets SPacketDisconnect                     = new Packets("net/minecraft/network/play/server/SPacketDisconnect",                     "gn",   Context.CLIENT);
    public static Packets SPacketDisplayObjective               = new Packets("net/minecraft/network/play/server/SPacketDisplayObjective",               "ho",   Context.CLIENT);
    public static Packets SPacketEffect                         = new Packets("net/minecraft/network/play/server/SPacketEffect",                         "gu",   Context.CLIENT);
    public static Packets SPacketEntity                         = new Packets("net/minecraft/network/play/server/SPacketEntity",                         "gy",   Context.CLIENT);
    public static Packets S15PacketEntityRelMove                = new Packets("net/minecraft/network/play/server/SPacketEntity$S15PacketEntityRelMove",  "gy$a", Context.CLIENT);
    public static Packets S16PacketEntityLook                   = new Packets("net/minecraft/network/play/server/SPacketEntity$S16PacketEntityLook",     "gy$c", Context.CLIENT);
    public static Packets S17PacketEntityLookMove               = new Packets("net/minecraft/network/play/server/SPacketEntity$S17PacketEntityLookMove", "gy$b", Context.CLIENT);
    public static Packets SPacketEntityAttach                   = new Packets("net/minecraft/network/play/server/SPacketEntityAttach",                   "hq",   Context.CLIENT);
    public static Packets SPacketEntityEffect                   = new Packets("net/minecraft/network/play/server/SPacketEntityEffect",                   "ii",   Context.CLIENT);
    public static Packets SPacketEntityEquipment                = new Packets("net/minecraft/network/play/server/SPacketEntityEquipment",                "hs",   Context.CLIENT);
    public static Packets SPacketEntityHeadLook                 = new Packets("net/minecraft/network/play/server/SPacketEntityHeadLook",                 "hk",   Context.CLIENT);
    public static Packets SPacketEntityMetadata                 = new Packets("net/minecraft/network/play/server/SPacketEntityMetadata",                 "hp",   Context.CLIENT);
    public static Packets SPacketEntityProperties               = new Packets("net/minecraft/network/play/server/SPacketEntityProperties",               "ih",   Context.CLIENT);
    public static Packets SPacketEntityStatus                   = new Packets("net/minecraft/network/play/server/SPacketEntityStatus",                   "go",   Context.CLIENT);
    public static Packets SPacketEntityTeleport                 = new Packets("net/minecraft/network/play/server/SPacketEntityTeleport",                 "ig",   Context.CLIENT);
    public static Packets SPacketEntityVelocity                 = new Packets("net/minecraft/network/play/server/SPacketEntityVelocity",                 "hr",   Context.CLIENT);
    public static Packets SPacketExplosion                      = new Packets("net/minecraft/network/play/server/SPacketExplosion",                      "gp",   Context.CLIENT);
    public static Packets SPacketHeldItemChange                 = new Packets("net/minecraft/network/play/server/SPacketHeldItemChange",                 "hn",   Context.CLIENT);
    public static Packets SPacketJoinGame                       = new Packets("net/minecraft/network/play/server/SPacketJoinGame",                       "gw",   Context.CLIENT);
    public static Packets SPacketKeepAlive                      = new Packets("net/minecraft/network/play/server/SPacketKeepAlive",                      "gs",   Context.CLIENT);
    public static Packets SPacketMaps                           = new Packets("net/minecraft/network/play/server/SPacketMaps",                           "gx",   Context.CLIENT);
    public static Packets SPacketMoveVehicle                    = new Packets("net/minecraft/network/play/server/SPacketMoveVehicle",                    "gz",   Context.CLIENT);
    public static Packets SPacketMultiBlockChange               = new Packets("net/minecraft/network/play/server/SPacketMultiBlockChange",               "gd",   Context.CLIENT);
    public static Packets SPacketOpenWindow                     = new Packets("net/minecraft/network/play/server/SPacketOpenWindow",                     "gg",   Context.CLIENT);
    public static Packets SPacketParticles                      = new Packets("net/minecraft/network/play/server/SPacketParticles",                      "gv",   Context.CLIENT);
    public static Packets SPacketPlayerAbilities                = new Packets("net/minecraft/network/play/server/SPacketPlayerAbilities",                "hb",   Context.CLIENT);
    public static Packets SPacketPlayerListHeaderFooter         = new Packets("net/minecraft/network/play/server/SPacketPlayerListHeaderFooter",         "id",   Context.CLIENT);
    public static Packets SPacketPlayerListItem                 = new Packets("net/minecraft/network/play/server/SPacketPlayerListItem",                 "hd",   Context.CLIENT);
    public static Packets SPacketPlayerPosLook                  = new Packets("net/minecraft/network/play/server/SPacketPlayerPosLook",                  "he",   Context.CLIENT);
    public static Packets SPacketRemoveEntityEffect             = new Packets("net/minecraft/network/play/server/SPacketRemoveEntityEffect",             "hh",   Context.CLIENT);
    public static Packets SPacketResourcePackSend               = new Packets("net/minecraft/network/play/server/SPacketResourcePackSend",               "hi",   Context.CLIENT);
    public static Packets SPacketRespawn                        = new Packets("net/minecraft/network/play/server/SPacketRespawn",                        "hj",   Context.CLIENT);
    public static Packets SPacketScoreboardObjective            = new Packets("net/minecraft/network/play/server/SPacketScoreboardObjective",            "hv",   Context.CLIENT);
    public static Packets SPacketServerDifficulty               = new Packets("net/minecraft/network/play/server/SPacketServerDifficulty",               "ga",   Context.CLIENT);
    public static Packets SPacketSetExperience                  = new Packets("net/minecraft/network/play/server/SPacketSetExperience",                  "ht",   Context.CLIENT);
    public static Packets SPacketSetPassengers                  = new Packets("net/minecraft/network/play/server/SPacketSetPassengers",                  "hw",   Context.CLIENT);
    public static Packets SPacketSetSlot                        = new Packets("net/minecraft/network/play/server/SPacketSetSlot",                        "gj",   Context.CLIENT);
    public static Packets SPacketSignEditorOpen                 = new Packets("net/minecraft/network/play/server/SPacketSignEditorOpen",                 "ha",   Context.CLIENT);
    public static Packets SPacketSoundEffect                    = new Packets("net/minecraft/network/play/server/SPacketSoundEffect",                    "ic",   Context.CLIENT);
    public static Packets SPacketSpawnExperienceOrb             = new Packets("net/minecraft/network/play/server/SPacketSpawnExperienceOrb",             "fo",   Context.CLIENT);
    public static Packets SPacketSpawnGlobalEntity              = new Packets("net/minecraft/network/play/server/SPacketSpawnGlobalEntity",              "fp",   Context.CLIENT);
    public static Packets SPacketSpawnMob                       = new Packets("net/minecraft/network/play/server/SPacketSpawnMob",                       "fq",   Context.CLIENT);
    public static Packets SPacketSpawnObject                    = new Packets("net/minecraft/network/play/server/SPacketSpawnObject",                    "fn",   Context.CLIENT);
    public static Packets SPacketSpawnPainting                  = new Packets("net/minecraft/network/play/server/SPacketSpawnPainting",                  "fr",   Context.CLIENT);
    public static Packets SPacketSpawnPlayer                    = new Packets("net/minecraft/network/play/server/SPacketSpawnPlayer",                    "fs",   Context.CLIENT);
    public static Packets SPacketSpawnPosition                  = new Packets("net/minecraft/network/play/server/SPacketSpawnPosition",                  "hz",   Context.CLIENT);
    public static Packets SPacketStatistics                     = new Packets("net/minecraft/network/play/server/SPacketStatistics",                     "fu",   Context.CLIENT);
    public static Packets SPacketTabComplete                    = new Packets("net/minecraft/network/play/server/SPacketTabComplete",                    "gb",   Context.CLIENT);
    public static Packets SPacketTeams                          = new Packets("net/minecraft/network/play/server/SPacketTeams",                          "hx",   Context.CLIENT);
    public static Packets SPacketTimeUpdate                     = new Packets("net/minecraft/network/play/server/SPacketTimeUpdate",                     "ia",   Context.CLIENT);
    public static Packets SPacketTitle                          = new Packets("net/minecraft/network/play/server/SPacketTitle",                          "ib",   Context.CLIENT);
    public static Packets SPacketUnloadChunk                    = new Packets("net/minecraft/network/play/server/SPacketUnloadChunk",                    "gq",   Context.CLIENT);
    public static Packets SPacketUpdateBossInfo                 = new Packets("net/minecraft/network/play/server/SPacketUpdateBossInfo",                 "fz",   Context.CLIENT);
    public static Packets SPacketUpdateHealth                   = new Packets("net/minecraft/network/play/server/SPacketUpdateHealth",                   "hu",   Context.CLIENT);
    public static Packets SPacketUpdateScore                    = new Packets("net/minecraft/network/play/server/SPacketUpdateScore",                    "hy",   Context.CLIENT);
    public static Packets SPacketUpdateTileEntity               = new Packets("net/minecraft/network/play/server/SPacketUpdateTileEntity",               "fw",   Context.CLIENT);
    public static Packets SPacketUseBed                         = new Packets("net/minecraft/network/play/server/SPacketUseBed",                         "hf",   Context.CLIENT);
    public static Packets SPacketWindowItems                    = new Packets("net/minecraft/network/play/server/SPacketWindowItems",                    "gh",   Context.CLIENT);
    public static Packets SPacketWindowProperty                 = new Packets("net/minecraft/network/play/server/SPacketWindowProperty",                 "gi",   Context.CLIENT);
    public static Packets SPacketWorldBorder                    = new Packets("net/minecraft/network/play/server/SPacketWorldBorder",                    "hl",   Context.CLIENT);
    public static Packets CPacketPing                           = new Packets("net/minecraft/network/status/client/CPacketPing",                         "ke",   Context.SERVER);
    public static Packets CPacketServerQuery                    = new Packets("net/minecraft/network/status/client/CPacketServerQuery",                  "kf",   Context.SERVER);
    public static Packets SPacketPong                           = new Packets("net/minecraft/network/status/server/SPacketPong",                         "ka",   Context.CLIENT);
    public static Packets SPacketServerInfo                     = new Packets("net/minecraft/network/status/server/SPacketServerInfo",                   "kb",   Context.CLIENT);

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
