package com.mumfrey.liteloader.core.runtime;

/**
 * Packet obfuscation table
 *
 * @author Adam Mummery-Smith
 * TODO Obfuscation 1.7.10
 */
@SuppressWarnings("hiding")
public class Packets extends Obf
{
	public static final Packets S08PacketPlayerPosLook             = new Packets("net.minecraft.network.play.server.S08PacketPlayerPosLook",                           "fu");
	public static final Packets S0EPacketSpawnObject               = new Packets("net.minecraft.network.play.server.S0EPacketSpawnObject",                             "fw");
	public static final Packets S11PacketSpawnExperienceOrb        = new Packets("net.minecraft.network.play.server.S11PacketSpawnExperienceOrb",                      "fx");
	public static final Packets S2CPacketSpawnGlobalEntity         = new Packets("net.minecraft.network.play.server.S2CPacketSpawnGlobalEntity",                       "fy");
	public static final Packets S0FPacketSpawnMob                  = new Packets("net.minecraft.network.play.server.S0FPacketSpawnMob",                                "fz");
	public static final Packets S10PacketSpawnPainting             = new Packets("net.minecraft.network.play.server.S10PacketSpawnPainting",                           "ga");
	public static final Packets S0CPacketSpawnPlayer               = new Packets("net.minecraft.network.play.server.S0CPacketSpawnPlayer",                             "gb");
	public static final Packets S0BPacketAnimation                 = new Packets("net.minecraft.network.play.server.S0BPacketAnimation",                               "gc");
	public static final Packets S37PacketStatistics                = new Packets("net.minecraft.network.play.server.S37PacketStatistics",                              "gd");
	public static final Packets S25PacketBlockBreakAnim            = new Packets("net.minecraft.network.play.server.S25PacketBlockBreakAnim",                          "ge");
	public static final Packets S35PacketUpdateTileEntity          = new Packets("net.minecraft.network.play.server.S35PacketUpdateTileEntity",                        "gf");
	public static final Packets S24PacketBlockAction               = new Packets("net.minecraft.network.play.server.S24PacketBlockAction",                             "gg");
	public static final Packets S23PacketBlockChange               = new Packets("net.minecraft.network.play.server.S23PacketBlockChange",                             "gh");
	public static final Packets S3APacketTabComplete               = new Packets("net.minecraft.network.play.server.S3APacketTabComplete",                             "gi");
	public static final Packets S02PacketChat                      = new Packets("net.minecraft.network.play.server.S02PacketChat",                                    "gj");
	public static final Packets S22PacketMultiBlockChange          = new Packets("net.minecraft.network.play.server.S22PacketMultiBlockChange",                        "gk");
	public static final Packets S32PacketConfirmTransaction        = new Packets("net.minecraft.network.play.server.S32PacketConfirmTransaction",                      "gl");
	public static final Packets S2EPacketCloseWindow               = new Packets("net.minecraft.network.play.server.S2EPacketCloseWindow",                             "gm");
	public static final Packets S2DPacketOpenWindow                = new Packets("net.minecraft.network.play.server.S2DPacketOpenWindow",                              "gn");
	public static final Packets S30PacketWindowItems               = new Packets("net.minecraft.network.play.server.S30PacketWindowItems",                             "go");
	public static final Packets S31PacketWindowProperty            = new Packets("net.minecraft.network.play.server.S31PacketWindowProperty",                          "gp");
	public static final Packets S2FPacketSetSlot                   = new Packets("net.minecraft.network.play.server.S2FPacketSetSlot",                                 "gq");
	public static final Packets S3FPacketCustomPayload             = new Packets("net.minecraft.network.play.server.S3FPacketCustomPayload",                           "gr");
	public static final Packets S40PacketDisconnect                = new Packets("net.minecraft.network.play.server.S40PacketDisconnect",                              "gs");
	public static final Packets S19PacketEntityStatus              = new Packets("net.minecraft.network.play.server.S19PacketEntityStatus",                            "gt");
	public static final Packets S27PacketExplosion                 = new Packets("net.minecraft.network.play.server.S27PacketExplosion",                               "gu");
	public static final Packets S2BPacketChangeGameState           = new Packets("net.minecraft.network.play.server.S2BPacketChangeGameState",                         "gv");
	public static final Packets S00PacketKeepAlive                 = new Packets("net.minecraft.network.play.server.S00PacketKeepAlive",                               "gw");
	public static final Packets S21PacketChunkData                 = new Packets("net.minecraft.network.play.server.S21PacketChunkData",                               "gx");
	public static final Packets S21PacketChunkData$Extracted       = new Packets("net.minecraft.network.play.server.S21PacketChunkData$Extracted",                     "gy");
	public static final Packets S26PacketMapChunkBulk              = new Packets("net.minecraft.network.play.server.S26PacketMapChunkBulk",                            "gz");
	public static final Packets S28PacketEffect                    = new Packets("net.minecraft.network.play.server.S28PacketEffect",                                  "ha");
	public static final Packets S2APacketParticles                 = new Packets("net.minecraft.network.play.server.S2APacketParticles",                               "hb");
	public static final Packets S29PacketSoundEffect               = new Packets("net.minecraft.network.play.server.S29PacketSoundEffect",                             "hc");
	public static final Packets S01PacketJoinGame                  = new Packets("net.minecraft.network.play.server.S01PacketJoinGame",                                "hd");
	public static final Packets S34PacketMaps                      = new Packets("net.minecraft.network.play.server.S34PacketMaps",                                    "he");
	public static final Packets S14PacketEntity                    = new Packets("net.minecraft.network.play.server.S14PacketEntity",                                  "hf");
	public static final Packets S15PacketEntityRelMove             = new Packets("net.minecraft.network.play.server.S14PacketEntity$S15PacketEntityRelMove",           "hg");
	public static final Packets S17PacketEntityLookMove            = new Packets("net.minecraft.network.play.server.S14PacketEntity$S17PacketEntityLookMove",          "hh");
	public static final Packets S16PacketEntityLook                = new Packets("net.minecraft.network.play.server.S14PacketEntity$S16PacketEntityLook",              "hi");
	public static final Packets S36PacketSignEditorOpen            = new Packets("net.minecraft.network.play.server.S36PacketSignEditorOpen",                          "hj");
	public static final Packets S39PacketPlayerAbilities           = new Packets("net.minecraft.network.play.server.S39PacketPlayerAbilities",                         "hk");
	public static final Packets S38PacketPlayerListItem            = new Packets("net.minecraft.network.play.server.S38PacketPlayerListItem",                          "ho");
	public static final Packets S0APacketUseBed                    = new Packets("net.minecraft.network.play.server.S0APacketUseBed",                                  "hp");
	public static final Packets S13PacketDestroyEntities           = new Packets("net.minecraft.network.play.server.S13PacketDestroyEntities",                         "hq");
	public static final Packets S1EPacketRemoveEntityEffect        = new Packets("net.minecraft.network.play.server.S1EPacketRemoveEntityEffect",                      "hr");
	public static final Packets S07PacketRespawn                   = new Packets("net.minecraft.network.play.server.S07PacketRespawn",                                 "hs");
	public static final Packets S19PacketEntityHeadLook            = new Packets("net.minecraft.network.play.server.S19PacketEntityHeadLook",                          "ht");
	public static final Packets S09PacketHeldItemChange            = new Packets("net.minecraft.network.play.server.S09PacketHeldItemChange",                          "hu");
	public static final Packets S3DPacketDisplayScoreboard         = new Packets("net.minecraft.network.play.server.S3DPacketDisplayScoreboard",                       "hv");
	public static final Packets S1CPacketEntityMetadata            = new Packets("net.minecraft.network.play.server.S1CPacketEntityMetadata",                          "hw");
	public static final Packets S1BPacketEntityAttach              = new Packets("net.minecraft.network.play.server.S1BPacketEntityAttach",                            "hx");
	public static final Packets S12PacketEntityVelocity            = new Packets("net.minecraft.network.play.server.S12PacketEntityVelocity",                          "hy");
	public static final Packets S04PacketEntityEquipment           = new Packets("net.minecraft.network.play.server.S04PacketEntityEquipment",                         "hz");
	public static final Packets S1FPacketSetExperience             = new Packets("net.minecraft.network.play.server.S1FPacketSetExperience",                           "ia");
	public static final Packets S06PacketUpdateHealth              = new Packets("net.minecraft.network.play.server.S06PacketUpdateHealth",                            "ib");
	public static final Packets S3BPacketScoreboardObjective       = new Packets("net.minecraft.network.play.server.S3BPacketScoreboardObjective",                     "ic");
	public static final Packets S3EPacketTeams                     = new Packets("net.minecraft.network.play.server.S3EPacketTeams",                                   "id");
	public static final Packets S3CPacketUpdateScore               = new Packets("net.minecraft.network.play.server.S3CPacketUpdateScore",                             "ie");
	public static final Packets S05PacketSpawnPosition             = new Packets("net.minecraft.network.play.server.S05PacketSpawnPosition",                           "ig");
	public static final Packets S03PacketTimeUpdate                = new Packets("net.minecraft.network.play.server.S03PacketTimeUpdate",                              "ih");
	public static final Packets S33PacketUpdateSign                = new Packets("net.minecraft.network.play.server.S33PacketUpdateSign",                              "ii");
	public static final Packets S0DPacketCollectItem               = new Packets("net.minecraft.network.play.server.S0DPacketCollectItem",                             "ij");
	public static final Packets S18PacketEntityTeleport            = new Packets("net.minecraft.network.play.server.S18PacketEntityTeleport",                          "ik");
	public static final Packets S20PacketEntityProperties          = new Packets("net.minecraft.network.play.server.S20PacketEntityProperties",                        "il");
	public static final Packets S20PacketEntityProperties$Snapshot = new Packets("net.minecraft.network.play.server.S20PacketEntityProperties$Snapshot",               "im");
	public static final Packets S1DPacketEntityEffect              = new Packets("net.minecraft.network.play.server.S1DPacketEntityEffect",                            "in");
	public static final Packets C0APacketAnimation                 = new Packets("net.minecraft.network.play.client.C0APacketAnimation",                               "ip");
	public static final Packets C14PacketTabComplete               = new Packets("net.minecraft.network.play.client.C14PacketTabComplete",                             "iq");
	public static final Packets C01PacketChatMessage               = new Packets("net.minecraft.network.play.client.C01PacketChatMessage",                             "ir");
	public static final Packets C16PacketClientStatus              = new Packets("net.minecraft.network.play.client.C16PacketClientStatus",                            "is");
	public static final Packets C16PacketClientStatus$EnumState    = new Packets("net.minecraft.network.play.client.C16PacketClientStatus$EnumState",                  "it");
	public static final Packets C15PacketClientSettings            = new Packets("net.minecraft.network.play.client.C15PacketClientSettings",                          "iu");
	public static final Packets C0FPacketConfirmTransaction        = new Packets("net.minecraft.network.play.client.C0FPacketConfirmTransaction",                      "iv");
	public static final Packets C11PacketEnchantItem               = new Packets("net.minecraft.network.play.client.C11PacketEnchantItem",                             "iw");
	public static final Packets C0EPacketClickWindow               = new Packets("net.minecraft.network.play.client.C0EPacketClickWindow",                             "ix");
	public static final Packets C0DPacketCloseWindow               = new Packets("net.minecraft.network.play.client.C0DPacketCloseWindow",                             "iy");
	public static final Packets C17PacketCustomPayload             = new Packets("net.minecraft.network.play.client.C17PacketCustomPayload",                           "iz");
	public static final Packets C02PacketUseEntity                 = new Packets("net.minecraft.network.play.client.C02PacketUseEntity",                               "ja");
	public static final Packets C02PacketUseEntity$Action          = new Packets("net.minecraft.network.play.client.C02PacketUseEntity$Action",                        "jb");
	public static final Packets C00PacketKeepAlive                 = new Packets("net.minecraft.network.play.client.C00PacketKeepAlive",                               "jc");
	public static final Packets C03PacketPlayer                    = new Packets("net.minecraft.network.play.client.C03PacketPlayer",                                  "jd");
	public static final Packets C04PacketPlayerPosition            = new Packets("net.minecraft.network.play.client.C03PacketPlayer$C04PacketPlayerPosition",          "je");
	public static final Packets C06PacketPlayerPosLook             = new Packets("net.minecraft.network.play.client.C03PacketPlayer$C06PacketPlayerPosLook",           "jf");
	public static final Packets C05PacketPlayerLook                = new Packets("net.minecraft.network.play.client.C03PacketPlayer$C05PacketPlayerLook",              "jg");
	public static final Packets C13PacketPlayerAbilities           = new Packets("net.minecraft.network.play.client.C13PacketPlayerAbilities",                         "jh");
	public static final Packets C07PacketPlayerDigging             = new Packets("net.minecraft.network.play.client.C07PacketPlayerDigging",                           "ji");
	public static final Packets C0BPacketEntityAction              = new Packets("net.minecraft.network.play.client.C0BPacketEntityAction",                            "jj");
	public static final Packets C0CPacketInput                     = new Packets("net.minecraft.network.play.client.C0CPacketInput",                                   "jk");
	public static final Packets C09PacketHeldItemChange            = new Packets("net.minecraft.network.play.client.C09PacketHeldItemChange",                          "jl");
	public static final Packets C10PacketCreativeInventoryAction   = new Packets("net.minecraft.network.play.client.C10PacketCreativeInventoryAction",                 "jm");
	public static final Packets C12PacketUpdateSign                = new Packets("net.minecraft.network.play.client.C12PacketUpdateSign",                              "jn");
	public static final Packets C08PacketPlayerBlockPlacement      = new Packets("net.minecraft.network.play.client.C08PacketPlayerBlockPlacement",                    "jo");
	public static final Packets C00Handshake                       = new Packets("net.minecraft.network.handshake.client.C00Handshake",                                "jp");
	public static final Packets S02PacketLoginSuccess              = new Packets("net.minecraft.network.login.server.S02PacketLoginSuccess",                           "js");
	public static final Packets S01PacketEncryptionRequest         = new Packets("net.minecraft.network.login.server.S01PacketEncryptionRequest",                      "jt");
	public static final Packets S00PacketDisconnect                = new Packets("net.minecraft.network.login.server.S00PacketDisconnect",                             "ju");
	public static final Packets C00PacketLoginStart                = new Packets("net.minecraft.network.login.client.C00PacketLoginStart",                             "jw");
	public static final Packets C01PacketEncryptionResponse        = new Packets("net.minecraft.network.login.client.C01PacketEncryptionResponse",                     "jx");
	public static final Packets S01PacketPong                      = new Packets("net.minecraft.network.status.server.S01PacketPong",                                  "jz");
	public static final Packets S00PacketServerInfo                = new Packets("net.minecraft.network.status.server.S00PacketServerInfo",                            "ka");

	public static final Packets[] packets = new Packets[] {
		S08PacketPlayerPosLook,
		S0EPacketSpawnObject,
		S11PacketSpawnExperienceOrb,
		S2CPacketSpawnGlobalEntity,
		S0FPacketSpawnMob,
		S10PacketSpawnPainting,
		S0CPacketSpawnPlayer,
		S0BPacketAnimation,
		S37PacketStatistics,
		S25PacketBlockBreakAnim,
		S35PacketUpdateTileEntity,
		S24PacketBlockAction,
		S23PacketBlockChange,
		S3APacketTabComplete,
		S02PacketChat,
		S22PacketMultiBlockChange,
		S32PacketConfirmTransaction,
		S2EPacketCloseWindow,
		S2DPacketOpenWindow,
		S30PacketWindowItems,
		S31PacketWindowProperty,
		S2FPacketSetSlot,
		S3FPacketCustomPayload,
		S40PacketDisconnect,
		S19PacketEntityStatus,
		S27PacketExplosion,
		S2BPacketChangeGameState,
		S00PacketKeepAlive,
		S21PacketChunkData,
		S21PacketChunkData$Extracted,
		S26PacketMapChunkBulk,
		S28PacketEffect,
		S2APacketParticles,
		S29PacketSoundEffect,
		S01PacketJoinGame,
		S34PacketMaps,
		S14PacketEntity,
		S15PacketEntityRelMove,
		S17PacketEntityLookMove,
		S16PacketEntityLook,
		S36PacketSignEditorOpen,
		S39PacketPlayerAbilities,
		S38PacketPlayerListItem,
		S0APacketUseBed,
		S13PacketDestroyEntities,
		S1EPacketRemoveEntityEffect,
		S07PacketRespawn,
		S19PacketEntityHeadLook,
		S09PacketHeldItemChange,
		S3DPacketDisplayScoreboard,
		S1CPacketEntityMetadata,
		S1BPacketEntityAttach,
		S12PacketEntityVelocity,
		S04PacketEntityEquipment,
		S1FPacketSetExperience,
		S06PacketUpdateHealth,
		S3BPacketScoreboardObjective,
		S3EPacketTeams,
		S3CPacketUpdateScore,
		S05PacketSpawnPosition,
		S03PacketTimeUpdate,
		S33PacketUpdateSign,
		S0DPacketCollectItem,
		S18PacketEntityTeleport,
		S20PacketEntityProperties,
		S20PacketEntityProperties$Snapshot,
		S1DPacketEntityEffect,
		C0APacketAnimation,
		C14PacketTabComplete,
		C01PacketChatMessage,
		C16PacketClientStatus,
		C16PacketClientStatus$EnumState,
		C15PacketClientSettings,
		C0FPacketConfirmTransaction,
		C11PacketEnchantItem,
		C0EPacketClickWindow,
		C0DPacketCloseWindow,
		C17PacketCustomPayload,
		C02PacketUseEntity,
		C02PacketUseEntity$Action,
		C00PacketKeepAlive,
		C03PacketPlayer,
		C04PacketPlayerPosition,
		C06PacketPlayerPosLook,
		C05PacketPlayerLook,
		C13PacketPlayerAbilities,
		C07PacketPlayerDigging,
		C0BPacketEntityAction,
		C0CPacketInput,
		C09PacketHeldItemChange,
		C10PacketCreativeInventoryAction,
		C12PacketUpdateSign,
		C08PacketPlayerBlockPlacement,
		C00Handshake,
		S02PacketLoginSuccess,
		S01PacketEncryptionRequest,
		S00PacketDisconnect,
		C00PacketLoginStart,
		C01PacketEncryptionResponse,
		S01PacketPong,
		S00PacketServerInfo
	};
	
	private static int nextPacketIndex;
	
	private final String shortName;
	
	private final int index;
	
	private Packets(String seargeName, String obfName)
	{
		super(seargeName, obfName);
		
		this.shortName = seargeName.substring(Math.max(seargeName.lastIndexOf('.'), seargeName.lastIndexOf('$')) + 1);
		this.index = Packets.nextPacketIndex++;
	}
	
	public int getIndex()
	{
		return this.index;
	}
	
	public String getShortName()
	{
		return this.shortName;
	}

	public static int indexOf(String packetClassName)
	{
		for (Packets packet : Packets.packets)
		{
			if (packet.name.equals(packetClassName) || packet.shortName.equals(packetClassName) || packet.obf.equals(packetClassName)) return packet.index;
		}
		
		return -1;
	}
	
	public static int count()
	{
		return Packets.nextPacketIndex;
	}
}
