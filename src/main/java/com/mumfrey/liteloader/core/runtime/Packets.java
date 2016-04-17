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
 * TODO Obfuscation 1.9
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

    public static Packets SPacketSpawnObject                      = new Packets("net/minecraft/network/play/server/SPacketSpawnObject",                      "fj",   Context.CLIENT);
    public static Packets SPacketSpawnExperienceOrb               = new Packets("net/minecraft/network/play/server/SPacketSpawnExperienceOrb",               "fk",   Context.CLIENT);
    public static Packets SPacketSpawnGlobalEntity                = new Packets("net/minecraft/network/play/server/SPacketSpawnGlobalEntity",                "fl",   Context.CLIENT);
    public static Packets SPacketSpawnMob                         = new Packets("net/minecraft/network/play/server/SPacketSpawnMob",                         "fm",   Context.CLIENT);
    public static Packets SPacketSpawnPainting                    = new Packets("net/minecraft/network/play/server/SPacketSpawnPainting",                    "fn",   Context.CLIENT);
    public static Packets SPacketSpawnPlayer                      = new Packets("net/minecraft/network/play/server/SPacketSpawnPlayer",                      "fo",   Context.CLIENT);
    public static Packets SPacketAnimation                        = new Packets("net/minecraft/network/play/server/SPacketAnimation",                        "fp",   Context.CLIENT);
    public static Packets SPacketStatistics                       = new Packets("net/minecraft/network/play/server/SPacketStatistics",                       "fq",   Context.CLIENT);
    public static Packets SPacketBlockBreakAnim                   = new Packets("net/minecraft/network/play/server/SPacketBlockBreakAnim",                   "fr",   Context.CLIENT);
    public static Packets SPacketUpdateTileEntity                 = new Packets("net/minecraft/network/play/server/SPacketUpdateTileEntity",                 "fs",   Context.CLIENT);
    public static Packets SPacketBlockAction                      = new Packets("net/minecraft/network/play/server/SPacketBlockAction",                      "ft",   Context.CLIENT);
    public static Packets SPacketBlockChange                      = new Packets("net/minecraft/network/play/server/SPacketBlockChange",                      "fu",   Context.CLIENT);
    public static Packets SPacketUpdateEntityNBT                  = new Packets("net/minecraft/network/play/server/SPacketUpdateEntityNBT",                  "fv",   Context.CLIENT);
    public static Packets SPacketServerDifficulty                 = new Packets("net/minecraft/network/play/server/SPacketServerDifficulty",                 "fw",   Context.CLIENT);
    public static Packets SPacketTabComplete                      = new Packets("net/minecraft/network/play/server/SPacketTabComplete",                      "fx",   Context.CLIENT);
    public static Packets SPacketChat                             = new Packets("net/minecraft/network/play/server/SPacketChat",                             "fy",   Context.CLIENT);
    public static Packets SPacketMultiBlockChange                 = new Packets("net/minecraft/network/play/server/SPacketMultiBlockChange",                 "fz",   Context.CLIENT);
    public static Packets SPacketConfirmTransaction               = new Packets("net/minecraft/network/play/server/SPacketConfirmTransaction",               "ga",   Context.CLIENT);
    public static Packets SPacketCloseWindow                      = new Packets("net/minecraft/network/play/server/SPacketCloseWindow",                      "gb",   Context.CLIENT);
    public static Packets SPacketOpenWindow                       = new Packets("net/minecraft/network/play/server/SPacketOpenWindow",                       "gc",   Context.CLIENT);
    public static Packets SPacketWindowItems                      = new Packets("net/minecraft/network/play/server/SPacketWindowItems",                      "gd",   Context.CLIENT);
    public static Packets SPacketWindowProperty                   = new Packets("net/minecraft/network/play/server/SPacketWindowProperty",                   "ge",   Context.CLIENT);
    public static Packets SPacketSetSlot                          = new Packets("net/minecraft/network/play/server/SPacketSetSlot",                          "gf",   Context.CLIENT);
    public static Packets SPacketCooldown                         = new Packets("net/minecraft/network/play/server/SPacketCooldown",                         "gg",   Context.CLIENT);
    public static Packets SPacketCustomPayload                    = new Packets("net/minecraft/network/play/server/SPacketCustomPayload",                    "gh",   Context.CLIENT);
    public static Packets SPacketCustomSound                      = new Packets("net/minecraft/network/play/server/SPacketCustomSound",                      "gi",   Context.CLIENT);
    public static Packets SPacketDisconnectPlay                   = new Packets("net/minecraft/network/play/server/SPacketDisconnect",                       "gj",   Context.CLIENT);
    public static Packets SPacketEntityStatus                     = new Packets("net/minecraft/network/play/server/SPacketEntityStatus",                     "gk",   Context.CLIENT);
    public static Packets SPacketExplosion                        = new Packets("net/minecraft/network/play/server/SPacketExplosion",                        "gl",   Context.CLIENT);
    public static Packets SPacketUnloadChunk                      = new Packets("net/minecraft/network/play/server/SPacketUnloadChunk",                      "gm",   Context.CLIENT);
    public static Packets SPacketChangeGameState                  = new Packets("net/minecraft/network/play/server/SPacketChangeGameState",                  "gn",   Context.CLIENT);
    public static Packets SPacketKeepAlive                        = new Packets("net/minecraft/network/play/server/SPacketKeepAlive",                        "go",   Context.CLIENT);
    public static Packets SPacketChunkData                        = new Packets("net/minecraft/network/play/server/SPacketChunkData",                        "gp",   Context.CLIENT);
    public static Packets SPacketEffect                           = new Packets("net/minecraft/network/play/server/SPacketEffect",                           "gq",   Context.CLIENT);
    public static Packets SPacketParticles                        = new Packets("net/minecraft/network/play/server/SPacketParticles",                        "gr",   Context.CLIENT);
    public static Packets SPacketJoinGame                         = new Packets("net/minecraft/network/play/server/SPacketJoinGame",                         "gs",   Context.CLIENT);
    public static Packets SPacketMaps                             = new Packets("net/minecraft/network/play/server/SPacketMaps",                             "gt",   Context.CLIENT);
    public static Packets SPacketEntity                           = new Packets("net/minecraft/network/play/server/SPacketEntity",                           "gu",   Context.CLIENT);
    public static Packets S15PacketEntityRelMove                  = new Packets("net/minecraft/network/play/server/SPacketEntity$S15PacketEntityRelMove",    "gu$a", Context.CLIENT);
    public static Packets S17PacketEntityLookMove                 = new Packets("net/minecraft/network/play/server/SPacketEntity$S17PacketEntityLookMove",   "gu$b", Context.CLIENT);
    public static Packets S16PacketEntityLook                     = new Packets("net/minecraft/network/play/server/SPacketEntity$S16PacketEntityLook",       "gu$c", Context.CLIENT);
    public static Packets SPacketMoveVehicle                      = new Packets("net/minecraft/network/play/server/SPacketMoveVehicle",                      "gv",   Context.CLIENT);
    public static Packets SPacketSignEditorOpen                   = new Packets("net/minecraft/network/play/server/SPacketSignEditorOpen",                   "gw",   Context.CLIENT);
    public static Packets SPacketPlayerAbilities                  = new Packets("net/minecraft/network/play/server/SPacketPlayerAbilities",                  "gx",   Context.CLIENT);
    public static Packets SPacketCombatEvent                      = new Packets("net/minecraft/network/play/server/SPacketCombatEvent",                      "gy",   Context.CLIENT);
    public static Packets SPacketPlayerListItem                   = new Packets("net/minecraft/network/play/server/SPacketPlayerListItem",                   "gz",   Context.CLIENT);
    public static Packets SPacketPlayerPosLook                    = new Packets("net/minecraft/network/play/server/SPacketPlayerPosLook",                    "ha",   Context.CLIENT);
    public static Packets SPacketUseBed                           = new Packets("net/minecraft/network/play/server/SPacketUseBed",                           "hb",   Context.CLIENT);
    public static Packets SPacketDestroyEntities                  = new Packets("net/minecraft/network/play/server/SPacketDestroyEntities",                  "hc",   Context.CLIENT);
    public static Packets SPacketRemoveEntityEffect               = new Packets("net/minecraft/network/play/server/SPacketRemoveEntityEffect",               "hd",   Context.CLIENT);
    public static Packets SPacketResourcePackSend                 = new Packets("net/minecraft/network/play/server/SPacketResourcePackSend",                 "he",   Context.CLIENT);
    public static Packets SPacketRespawn                          = new Packets("net/minecraft/network/play/server/SPacketRespawn",                          "hf",   Context.CLIENT);
    public static Packets SPacketEntityHeadLook                   = new Packets("net/minecraft/network/play/server/SPacketEntityHeadLook",                   "hg",   Context.CLIENT);
    public static Packets SPacketWorldBorder                      = new Packets("net/minecraft/network/play/server/SPacketWorldBorder",                      "hh",   Context.CLIENT);
    public static Packets SPacketCamera                           = new Packets("net/minecraft/network/play/server/SPacketCamera",                           "hi",   Context.CLIENT);
    public static Packets SPacketHeldItemChange                   = new Packets("net/minecraft/network/play/server/SPacketHeldItemChange",                   "hj",   Context.CLIENT);
    public static Packets SPacketDisplayObjective                 = new Packets("net/minecraft/network/play/server/SPacketDisplayObjective",                 "hk",   Context.CLIENT);
    public static Packets SPacketEntityMetadata                   = new Packets("net/minecraft/network/play/server/SPacketEntityMetadata",                   "hl",   Context.CLIENT);
    public static Packets SPacketEntityAttach                     = new Packets("net/minecraft/network/play/server/SPacketEntityAttach",                     "hm",   Context.CLIENT);
    public static Packets SPacketEntityVelocity                   = new Packets("net/minecraft/network/play/server/SPacketEntityVelocity",                   "hn",   Context.CLIENT);
    public static Packets C00Handshake                            = new Packets("net/minecraft/network/handshake/client/C00Handshake",                       "jj",   Context.SERVER);
    public static Packets SPacketEntityEquipment                  = new Packets("net/minecraft/network/play/server/SPacketEntityEquipment",                  "ho",   Context.CLIENT);
    public static Packets SPacketSetExperience                    = new Packets("net/minecraft/network/play/server/SPacketSetExperience",                    "hp",   Context.CLIENT);
    public static Packets SPacketUpdateHealth                     = new Packets("net/minecraft/network/play/server/SPacketUpdateHealth",                     "hq",   Context.CLIENT);
    public static Packets SPacketScoreboardObjective              = new Packets("net/minecraft/network/play/server/SPacketScoreboardObjective",              "hr",   Context.CLIENT);
    public static Packets SPacketSetPassengers                    = new Packets("net/minecraft/network/play/server/SPacketSetPassengers",                    "hs",   Context.CLIENT);
    public static Packets SPacketTeams                            = new Packets("net/minecraft/network/play/server/SPacketTeams",                            "ht",   Context.CLIENT);
    public static Packets SPacketUpdateScore                      = new Packets("net/minecraft/network/play/server/SPacketUpdateScore",                      "hu",   Context.CLIENT);
    public static Packets SPacketSpawnPosition                    = new Packets("net/minecraft/network/play/server/SPacketSpawnPosition",                    "hv",   Context.CLIENT);
    public static Packets SPacketTimeUpdate                       = new Packets("net/minecraft/network/play/server/SPacketTimeUpdate",                       "hw",   Context.CLIENT);
    public static Packets SPacketTitle                            = new Packets("net/minecraft/network/play/server/SPacketTitle",                            "hx",   Context.CLIENT);
    public static Packets SPacketUpdateSign                       = new Packets("net/minecraft/network/play/server/SPacketUpdateSign",                       "hy",   Context.CLIENT);
    public static Packets SPacketSoundEffect                      = new Packets("net/minecraft/network/play/server/SPacketSoundEffect",                      "hz",   Context.CLIENT);
    public static Packets SPacketPlayerListHeaderFooter           = new Packets("net/minecraft/network/play/server/SPacketPlayerListHeaderFooter",           "ia",   Context.CLIENT);
    public static Packets SPacketCollectItem                      = new Packets("net/minecraft/network/play/server/SPacketCollectItem",                      "ib",   Context.CLIENT);
    public static Packets SPacketEntityTeleport                   = new Packets("net/minecraft/network/play/server/SPacketEntityTeleport",                   "ic",   Context.CLIENT);
    public static Packets SPacketEntityProperties                 = new Packets("net/minecraft/network/play/server/SPacketEntityProperties",                 "id",   Context.CLIENT);
    public static Packets SPacketEntityEffect                     = new Packets("net/minecraft/network/play/server/SPacketEntityEffect",                     "ie",   Context.CLIENT);
    public static Packets CPacketConfirmTeleport                  = new Packets("net/minecraft/network/play/client/CPacketConfirmTeleport",                  "ih",   Context.SERVER);
    public static Packets CPacketTabComplete                      = new Packets("net/minecraft/network/play/client/CPacketTabComplete",                      "ii",   Context.SERVER);
    public static Packets CPacketChatMessage                      = new Packets("net/minecraft/network/play/client/CPacketChatMessage",                      "ij",   Context.SERVER);
    public static Packets CPacketClientStatus                     = new Packets("net/minecraft/network/play/client/CPacketClientStatus",                     "ik",   Context.SERVER);
    public static Packets CPacketClientSettings                   = new Packets("net/minecraft/network/play/client/CPacketClientSettings",                   "il",   Context.SERVER);
    public static Packets CPacketConfirmTransaction               = new Packets("net/minecraft/network/play/client/CPacketConfirmTransaction",               "im",   Context.SERVER);
    public static Packets CPacketEnchantItem                      = new Packets("net/minecraft/network/play/client/CPacketEnchantItem",                      "in",   Context.SERVER);
    public static Packets CPacketClickWindow                      = new Packets("net/minecraft/network/play/client/CPacketClickWindow",                      "io",   Context.SERVER);
    public static Packets CPacketCloseWindow                      = new Packets("net/minecraft/network/play/client/CPacketCloseWindow",                      "ip",   Context.SERVER);
    public static Packets CPacketCustomPayload                    = new Packets("net/minecraft/network/play/client/CPacketCustomPayload",                    "iq",   Context.SERVER);
    public static Packets CPacketUseEntity                        = new Packets("net/minecraft/network/play/client/CPacketUseEntity",                        "ir",   Context.SERVER);
    public static Packets CPacketKeepAlive                        = new Packets("net/minecraft/network/play/client/CPacketKeepAlive",                        "is",   Context.SERVER);
    public static Packets CPacketPlayer                           = new Packets("net/minecraft/network/play/client/CPacketPlayer",                           "it",   Context.SERVER);
    public static Packets C04PacketPlayerPosition                 = new Packets("net/minecraft/network/play/client/CPacketPlayer$C04PacketPlayerPosition",   "it$a", Context.SERVER);
    public static Packets C06PacketPlayerPosLook                  = new Packets("net/minecraft/network/play/client/CPacketPlayer$C06PacketPlayerPosLook",    "it$b", Context.SERVER);
    public static Packets C05PacketPlayerLook                     = new Packets("net/minecraft/network/play/client/CPacketPlayer$C05PacketPlayerLook",       "it$c", Context.SERVER);
    public static Packets CPacketVehicleMove                      = new Packets("net/minecraft/network/play/client/CPacketVehicleMove",                      "iu",   Context.SERVER);
    public static Packets CPacketSteerBoat                        = new Packets("net/minecraft/network/play/client/CPacketSteerBoat",                        "iv",   Context.SERVER);
    public static Packets CPacketPlayerAbilities                  = new Packets("net/minecraft/network/play/client/CPacketPlayerAbilities",                  "iw",   Context.SERVER);
    public static Packets CPacketPlayerDigging                    = new Packets("net/minecraft/network/play/client/CPacketPlayerDigging",                    "ix",   Context.SERVER);
    public static Packets CPacketEntityAction                     = new Packets("net/minecraft/network/play/client/CPacketEntityAction",                     "iy",   Context.SERVER);
    public static Packets CPacketInput                            = new Packets("net/minecraft/network/play/client/CPacketInput",                            "iz",   Context.SERVER);
    public static Packets CPacketResourcePackStatus               = new Packets("net/minecraft/network/play/client/CPacketResourcePackStatus",               "ja",   Context.SERVER);
    public static Packets CPacketHeldItemChange                   = new Packets("net/minecraft/network/play/client/CPacketHeldItemChange",                   "jb",   Context.SERVER);
    public static Packets CPacketCreativeInventoryAction          = new Packets("net/minecraft/network/play/client/CPacketCreativeInventoryAction",          "jc",   Context.SERVER);
    public static Packets CPacketUpdateSign                       = new Packets("net/minecraft/network/play/client/CPacketUpdateSign",                       "jd",   Context.SERVER);
    public static Packets CPacketAnimation                        = new Packets("net/minecraft/network/play/client/CPacketAnimation",                        "je",   Context.SERVER);
    public static Packets CPacketSpectate                         = new Packets("net/minecraft/network/play/client/CPacketSpectate",                         "jf",   Context.SERVER);
    public static Packets CPacketPlayerTryUseItem                 = new Packets("net/minecraft/network/play/client/CPacketPlayerTryUseItem",                 "jg",   Context.SERVER);
    public static Packets CPacketPlayerBlockPlacement             = new Packets("net/minecraft/network/play/client/CPacketPlayerBlockPlacement",             "jh",   Context.SERVER);
    public static Packets SPacketLoginSuccess                     = new Packets("net/minecraft/network/login/server/SPacketLoginSuccess",                    "jn",   Context.CLIENT);
    public static Packets SPacketEncryptionRequest                = new Packets("net/minecraft/network/login/server/SPacketEncryptionRequest",               "jo",   Context.CLIENT);
    public static Packets SPacketEnableCompression                = new Packets("net/minecraft/network/login/server/SPacketEnableCompression",               "jp",   Context.CLIENT);
    public static Packets SPacketDisconnectLogin                  = new Packets("net/minecraft/network/login/server/SPacketDisconnect",                      "jq",   Context.CLIENT);
    public static Packets CPacketLoginStart                       = new Packets("net/minecraft/network/login/client/CPacketLoginStart",                      "js",   Context.SERVER);
    public static Packets CPacketEncryptionResponse               = new Packets("net/minecraft/network/login/client/CPacketEncryptionResponse",              "jt",   Context.SERVER);
    public static Packets SPacketPong                             = new Packets("net/minecraft/network/status/server/SPacketPong",                           "jx",   Context.CLIENT);
    public static Packets SPacketServerInfo                       = new Packets("net/minecraft/network/status/server/SPacketServerInfo",                     "jy",   Context.CLIENT);
    public static Packets CPacketPing                             = new Packets("net/minecraft/network/status/client/CPacketPing",                           "kb",   Context.SERVER);
    public static Packets CPacketServerQuery                      = new Packets("net/minecraft/network/status/client/CPacketServerQuery",                    "kc",   Context.SERVER);

    // CHECKSTYLE:ON

    public static final Packets[] packets = new Packets[] {
            SPacketSpawnObject,
            SPacketSpawnExperienceOrb,
            SPacketSpawnGlobalEntity,
            SPacketSpawnMob,
            SPacketSpawnPainting,
            SPacketSpawnPlayer,
            SPacketAnimation,
            SPacketStatistics,
            SPacketBlockBreakAnim,
            SPacketUpdateTileEntity,
            SPacketBlockAction,
            SPacketBlockChange,
            SPacketUpdateEntityNBT,
            SPacketServerDifficulty,
            SPacketTabComplete,
            SPacketChat,
            SPacketMultiBlockChange,
            SPacketConfirmTransaction,
            SPacketCloseWindow,
            SPacketOpenWindow,
            SPacketWindowItems,
            SPacketWindowProperty,
            SPacketSetSlot,
            SPacketCooldown,
            SPacketCustomPayload,
            SPacketCustomSound,
            SPacketDisconnectPlay,
            SPacketEntityStatus,
            SPacketExplosion,
            SPacketUnloadChunk,
            SPacketChangeGameState,
            SPacketKeepAlive,
            SPacketChunkData,
            SPacketEffect,
            SPacketParticles,
            SPacketJoinGame,
            SPacketMaps,
            SPacketEntity,
            S15PacketEntityRelMove,
            S17PacketEntityLookMove,
            S16PacketEntityLook,
            SPacketMoveVehicle,
            SPacketSignEditorOpen,
            SPacketPlayerAbilities,
            SPacketCombatEvent,
            SPacketPlayerListItem,
            SPacketPlayerPosLook,
            SPacketUseBed,
            SPacketDestroyEntities,
            SPacketRemoveEntityEffect,
            SPacketResourcePackSend,
            SPacketRespawn,
            SPacketEntityHeadLook,
            SPacketWorldBorder,
            SPacketCamera,
            SPacketHeldItemChange,
            SPacketDisplayObjective,
            SPacketEntityMetadata,
            SPacketEntityAttach,
            SPacketEntityVelocity,
            C00Handshake,
            SPacketEntityEquipment,
            SPacketSetExperience,
            SPacketUpdateHealth,
            SPacketScoreboardObjective,
            SPacketSetPassengers,
            SPacketTeams,
            SPacketUpdateScore,
            SPacketSpawnPosition,
            SPacketTimeUpdate,
            SPacketTitle,
            SPacketUpdateSign,
            SPacketSoundEffect,
            SPacketPlayerListHeaderFooter,
            SPacketCollectItem,
            SPacketEntityTeleport,
            SPacketEntityProperties,
            SPacketEntityEffect,
            CPacketConfirmTeleport,
            CPacketTabComplete,
            CPacketChatMessage,
            CPacketClientStatus,
            CPacketClientSettings,
            CPacketConfirmTransaction,
            CPacketEnchantItem,
            CPacketClickWindow,
            CPacketCloseWindow,
            CPacketCustomPayload,
            CPacketUseEntity,
            CPacketKeepAlive,
            CPacketPlayer,
            C04PacketPlayerPosition,
            C06PacketPlayerPosLook,
            C05PacketPlayerLook,
            CPacketVehicleMove,
            CPacketSteerBoat,
            CPacketPlayerAbilities,
            CPacketPlayerDigging,
            CPacketEntityAction,
            CPacketInput,
            CPacketResourcePackStatus,
            CPacketHeldItemChange,
            CPacketCreativeInventoryAction,
            CPacketUpdateSign,
            CPacketAnimation,
            CPacketSpectate,
            CPacketPlayerTryUseItem,
            CPacketPlayerBlockPlacement,
            SPacketLoginSuccess,
            SPacketEncryptionRequest,
            SPacketEnableCompression,
            SPacketDisconnectLogin,
            CPacketLoginStart,
            CPacketEncryptionResponse,
            SPacketPong,
            SPacketServerInfo,
            CPacketPing,
            CPacketServerQuery,
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
