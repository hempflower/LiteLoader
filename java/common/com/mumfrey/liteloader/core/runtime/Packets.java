package com.mumfrey.liteloader.core.runtime;

import java.util.HashMap;
import java.util.Map;

/**
 * Packet obfuscation table
 *
 * @author Adam Mummery-Smith
 * TODO Obfuscation 1.8
 */
public class Packets extends Obf
{
	private static Map<String, Packets> packetMap = new HashMap<String, Packets>();
	
	public static Packets S08PacketPlayerPosLook           = new Packets("net.minecraft.network.play.server.S08PacketPlayerPosLook",                    "ii");
	public static Packets S0EPacketSpawnObject             = new Packets("net.minecraft.network.play.server.S0EPacketSpawnObject",                      "il");
	public static Packets S11PacketSpawnExperienceOrb      = new Packets("net.minecraft.network.play.server.S11PacketSpawnExperienceOrb",               "im");
	public static Packets S2CPacketSpawnGlobalEntity       = new Packets("net.minecraft.network.play.server.S2CPacketSpawnGlobalEntity",                "in");
	public static Packets S0FPacketSpawnMob                = new Packets("net.minecraft.network.play.server.S0FPacketSpawnMob",                         "io");
	public static Packets S10PacketSpawnPainting           = new Packets("net.minecraft.network.play.server.S10PacketSpawnPainting",                    "ip");
	public static Packets S0CPacketSpawnPlayer             = new Packets("net.minecraft.network.play.server.S0CPacketSpawnPlayer",                      "iq");
	public static Packets S0BPacketAnimation               = new Packets("net.minecraft.network.play.server.S0BPacketAnimation",                        "ir");
	public static Packets S37PacketStatistics              = new Packets("net.minecraft.network.play.server.S37PacketStatistics",                       "is");
	public static Packets S25PacketBlockBreakAnim          = new Packets("net.minecraft.network.play.server.S25PacketBlockBreakAnim",                   "it");
	public static Packets S35PacketUpdateTileEntity        = new Packets("net.minecraft.network.play.server.S35PacketUpdateTileEntity",                 "iu");
	public static Packets S24PacketBlockAction             = new Packets("net.minecraft.network.play.server.S24PacketBlockAction",                      "iv");
	public static Packets S23PacketBlockChange             = new Packets("net.minecraft.network.play.server.S23PacketBlockChange",                      "iw");
	public static Packets S41PacketServerDifficulty        = new Packets("net.minecraft.network.play.server.S41PacketServerDifficulty",                 "ix");
	public static Packets S3APacketTabComplete             = new Packets("net.minecraft.network.play.server.S3APacketTabComplete",                      "iy");
	public static Packets S02PacketChat                    = new Packets("net.minecraft.network.play.server.S02PacketChat",                             "iz");
	public static Packets S22PacketMultiBlockChange        = new Packets("net.minecraft.network.play.server.S22PacketMultiBlockChange",                 "ja");
	public static Packets S32PacketConfirmTransaction      = new Packets("net.minecraft.network.play.server.S32PacketConfirmTransaction",               "jc");
	public static Packets S2EPacketCloseWindow             = new Packets("net.minecraft.network.play.server.S2EPacketCloseWindow",                      "jd");
	public static Packets S2DPacketOpenWindow              = new Packets("net.minecraft.network.play.server.S2DPacketOpenWindow",                       "je");
	public static Packets S30PacketWindowItems             = new Packets("net.minecraft.network.play.server.S30PacketWindowItems",                      "jf");
	public static Packets S31PacketWindowProperty          = new Packets("net.minecraft.network.play.server.S31PacketWindowProperty",                   "jg");
	public static Packets S2FPacketSetSlot                 = new Packets("net.minecraft.network.play.server.S2FPacketSetSlot",                          "jh");
	public static Packets S3FPacketCustomPayload           = new Packets("net.minecraft.network.play.server.S3FPacketCustomPayload",                    "ji");
	public static Packets S40PacketDisconnect              = new Packets("net.minecraft.network.play.server.S40PacketDisconnect",                       "jj");
	public static Packets S19PacketEntityStatus            = new Packets("net.minecraft.network.play.server.S19PacketEntityStatus",                     "jk");
	public static Packets S49PacketUpdateEntityNBT         = new Packets("net.minecraft.network.play.server.S49PacketUpdateEntityNBT",                  "jl");
	public static Packets S27PacketExplosion               = new Packets("net.minecraft.network.play.server.S27PacketExplosion",                        "jm");
	public static Packets S46PacketSetCompressionLevel     = new Packets("net.minecraft.network.play.server.S46PacketSetCompressionLevel",              "jn");
	public static Packets S2BPacketChangeGameState         = new Packets("net.minecraft.network.play.server.S2BPacketChangeGameState",                  "jo");
	public static Packets S00PacketKeepAlive               = new Packets("net.minecraft.network.play.server.S00PacketKeepAlive",                        "jp");
	public static Packets S21PacketChunkData               = new Packets("net.minecraft.network.play.server.S21PacketChunkData",                        "jq");
	public static Packets S26PacketMapChunkBulk            = new Packets("net.minecraft.network.play.server.S26PacketMapChunkBulk",                     "js");
	public static Packets S28PacketEffect                  = new Packets("net.minecraft.network.play.server.S28PacketEffect",                           "jt");
	public static Packets S2APacketParticles               = new Packets("net.minecraft.network.play.server.S2APacketParticles",                        "ju");
	public static Packets S29PacketSoundEffect             = new Packets("net.minecraft.network.play.server.S29PacketSoundEffect",                      "jv");
	public static Packets S01PacketJoinGame                = new Packets("net.minecraft.network.play.server.S01PacketJoinGame",                         "jw");
	public static Packets S34PacketMaps                    = new Packets("net.minecraft.network.play.server.S34PacketMaps",                             "jx");
	public static Packets S14PacketEntity                  = new Packets("net.minecraft.network.play.server.S14PacketEntity",                           "jy");
	public static Packets S15PacketEntityRelMove           = new Packets("net.minecraft.network.play.server.S14PacketEntity$S15PacketEntityRelMove",    "jz");
	public static Packets S17PacketEntityLookMove          = new Packets("net.minecraft.network.play.server.S14PacketEntity$S17PacketEntityLookMove",   "ka");
	public static Packets S16PacketEntityLook              = new Packets("net.minecraft.network.play.server.S14PacketEntity$S16PacketEntityLook",       "kb");
	public static Packets S36PacketSignEditorOpen          = new Packets("net.minecraft.network.play.server.S36PacketSignEditorOpen",                   "kc");
	public static Packets S39PacketPlayerAbilities         = new Packets("net.minecraft.network.play.server.S39PacketPlayerAbilities",                  "kd");
	public static Packets S42PacketCombatEvent             = new Packets("net.minecraft.network.play.server.S42PacketCombatEvent",                      "ke");
	public static Packets S38PacketPlayerListItem          = new Packets("net.minecraft.network.play.server.S38PacketPlayerListItem",                   "kh");
	public static Packets S0APacketUseBed                  = new Packets("net.minecraft.network.play.server.S0APacketUseBed",                           "kl");
	public static Packets S13PacketDestroyEntities         = new Packets("net.minecraft.network.play.server.S13PacketDestroyEntities",                  "km");
	public static Packets S1EPacketRemoveEntityEffect      = new Packets("net.minecraft.network.play.server.S1EPacketRemoveEntityEffect",               "kn");
	public static Packets S48PacketResourcePackSend        = new Packets("net.minecraft.network.play.server.S48PacketResourcePackSend",                 "ko");
	public static Packets S07PacketRespawn                 = new Packets("net.minecraft.network.play.server.S07PacketRespawn",                          "kp");
	public static Packets S19PacketEntityHeadLook          = new Packets("net.minecraft.network.play.server.S19PacketEntityHeadLook",                   "kq");
	public static Packets S44PacketWorldBorder             = new Packets("net.minecraft.network.play.server.S44PacketWorldBorder",                      "kr");
	public static Packets S43PacketCamera                  = new Packets("net.minecraft.network.play.server.S43PacketCamera",                           "ku");
	public static Packets S09PacketHeldItemChange          = new Packets("net.minecraft.network.play.server.S09PacketHeldItemChange",                   "kv");
	public static Packets S3DPacketDisplayScoreboard       = new Packets("net.minecraft.network.play.server.S3DPacketDisplayScoreboard",                "kw");
	public static Packets S1CPacketEntityMetadata          = new Packets("net.minecraft.network.play.server.S1CPacketEntityMetadata",                   "kx");
	public static Packets S1BPacketEntityAttach            = new Packets("net.minecraft.network.play.server.S1BPacketEntityAttach",                     "ky");
	public static Packets S12PacketEntityVelocity          = new Packets("net.minecraft.network.play.server.S12PacketEntityVelocity",                   "kz");
	public static Packets S04PacketEntityEquipment         = new Packets("net.minecraft.network.play.server.S04PacketEntityEquipment",                  "la");
	public static Packets S1FPacketSetExperience           = new Packets("net.minecraft.network.play.server.S1FPacketSetExperience",                    "lb");
	public static Packets S06PacketUpdateHealth            = new Packets("net.minecraft.network.play.server.S06PacketUpdateHealth",                     "lc");
	public static Packets S3BPacketScoreboardObjective     = new Packets("net.minecraft.network.play.server.S3BPacketScoreboardObjective",              "ld");
	public static Packets S3EPacketTeams                   = new Packets("net.minecraft.network.play.server.S3EPacketTeams",                            "le");
	public static Packets S3CPacketUpdateScore             = new Packets("net.minecraft.network.play.server.S3CPacketUpdateScore",                      "lf");
	public static Packets S05PacketSpawnPosition           = new Packets("net.minecraft.network.play.server.S05PacketSpawnPosition",                    "lh");
	public static Packets S03PacketTimeUpdate              = new Packets("net.minecraft.network.play.server.S03PacketTimeUpdate",                       "li");
	public static Packets S45PacketTitle                   = new Packets("net.minecraft.network.play.server.S45PacketTitle",                            "lj");
	public static Packets S33PacketUpdateSign              = new Packets("net.minecraft.network.play.server.S33PacketUpdateSign",                       "ll");
	public static Packets S47PacketPlayerListHeaderFooter  = new Packets("net.minecraft.network.play.server.S47PacketPlayerListHeaderFooter",           "lm");
	public static Packets S0DPacketCollectItem             = new Packets("net.minecraft.network.play.server.S0DPacketCollectItem",                      "ln");
	public static Packets S18PacketEntityTeleport          = new Packets("net.minecraft.network.play.server.S18PacketEntityTeleport",                   "lo");
	public static Packets S20PacketEntityProperties        = new Packets("net.minecraft.network.play.server.S20PacketEntityProperties",                 "lp");
	public static Packets S1DPacketEntityEffect            = new Packets("net.minecraft.network.play.server.S1DPacketEntityEffect",                     "lr");
	public static Packets C14PacketTabComplete             = new Packets("net.minecraft.network.play.client.C14PacketTabComplete",                      "lt");
	public static Packets C01PacketChatMessage             = new Packets("net.minecraft.network.play.client.C01PacketChatMessage",                      "lu");
	public static Packets C16PacketClientStatus            = new Packets("net.minecraft.network.play.client.C16PacketClientStatus",                     "lv");
	public static Packets C15PacketClientSettings          = new Packets("net.minecraft.network.play.client.C15PacketClientSettings",                   "lx");
	public static Packets C0FPacketConfirmTransaction      = new Packets("net.minecraft.network.play.client.C0FPacketConfirmTransaction",               "ly");
	public static Packets C11PacketEnchantItem             = new Packets("net.minecraft.network.play.client.C11PacketEnchantItem",                      "lz");
	public static Packets C0EPacketClickWindow             = new Packets("net.minecraft.network.play.client.C0EPacketClickWindow",                      "ma");
	public static Packets C0DPacketCloseWindow             = new Packets("net.minecraft.network.play.client.C0DPacketCloseWindow",                      "mb");
	public static Packets C17PacketCustomPayload           = new Packets("net.minecraft.network.play.client.C17PacketCustomPayload",                    "mc");
	public static Packets C02PacketUseEntity               = new Packets("net.minecraft.network.play.client.C02PacketUseEntity",                        "md");
	public static Packets C00PacketKeepAlive               = new Packets("net.minecraft.network.play.client.C00PacketKeepAlive",                        "mf");
	public static Packets C03PacketPlayer                  = new Packets("net.minecraft.network.play.client.C03PacketPlayer",                           "mg");
	public static Packets C04PacketPlayerPosition          = new Packets("net.minecraft.network.play.client.C03PacketPlayer$C04PacketPlayerPosition",   "mh");
	public static Packets C06PacketPlayerPosLook           = new Packets("net.minecraft.network.play.client.C03PacketPlayer$C06PacketPlayerPosLook",    "mi");
	public static Packets C05PacketPlayerLook              = new Packets("net.minecraft.network.play.client.C03PacketPlayer$C05PacketPlayerLook",       "mj");
	public static Packets C13PacketPlayerAbilities         = new Packets("net.minecraft.network.play.client.C13PacketPlayerAbilities",                  "mk");
	public static Packets C07PacketPlayerDigging           = new Packets("net.minecraft.network.play.client.C07PacketPlayerDigging",                    "ml");
	public static Packets C0BPacketEntityAction            = new Packets("net.minecraft.network.play.client.C0BPacketEntityAction",                     "mn");
	public static Packets C0CPacketInput                   = new Packets("net.minecraft.network.play.client.C0CPacketInput",                            "mp");
	public static Packets C19PacketResourcePackStatus      = new Packets("net.minecraft.network.play.client.C19PacketResourcePackStatus",               "mq");
	public static Packets C09PacketHeldItemChange          = new Packets("net.minecraft.network.play.client.C09PacketHeldItemChange",                   "ms");
	public static Packets C10PacketCreativeInventoryAction = new Packets("net.minecraft.network.play.client.C10PacketCreativeInventoryAction",          "mt");
	public static Packets C12PacketUpdateSign              = new Packets("net.minecraft.network.play.client.C12PacketUpdateSign",                       "mu");
	public static Packets C0APacketAnimation               = new Packets("net.minecraft.network.play.client.C0APacketAnimation",                        "mv");
	public static Packets C18PacketSpectate                = new Packets("net.minecraft.network.play.client.C18PacketSpectate",                         "mw");
	public static Packets C08PacketPlayerBlockPlacement    = new Packets("net.minecraft.network.play.client.C08PacketPlayerBlockPlacement",             "mx");
	public static Packets C00Handshake                     = new Packets("net.minecraft.network.handshake.client.C00Handshake",                         "mz");
	public static Packets S02PacketLoginSuccess            = new Packets("net.minecraft.network.login.server.S02PacketLoginSuccess",                    "nd");
	public static Packets S01PacketEncryptionRequest       = new Packets("net.minecraft.network.login.server.S01PacketEncryptionRequest",               "ne");
	public static Packets S03PacketEnableCompression       = new Packets("net.minecraft.network.login.server.S03PacketEnableCompression",               "nf");
	public static Packets S00PacketDisconnect              = new Packets("net.minecraft.network.login.server.S00PacketDisconnect",                      "ng");
	public static Packets C00PacketLoginStart              = new Packets("net.minecraft.network.login.client.C00PacketLoginStart",                      "ni");
	public static Packets C01PacketEncryptionResponse      = new Packets("net.minecraft.network.login.client.C01PacketEncryptionResponse",              "nj");
	public static Packets S01PacketPong                    = new Packets("net.minecraft.network.status.server.S01PacketPong",                           "nn");
	public static Packets S00PacketServerInfo              = new Packets("net.minecraft.network.status.server.S00PacketServerInfo",                     "no");
	public static Packets C01PacketPing                    = new Packets("net.minecraft.network.status.client.C01PacketPing",                           "nw");
	public static Packets C00PacketServerQuery             = new Packets("net.minecraft.network.status.client.C00PacketServerQuery",                    "nx");

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
		S41PacketServerDifficulty,
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
		S49PacketUpdateEntityNBT,
		S27PacketExplosion,
		S46PacketSetCompressionLevel,
		S2BPacketChangeGameState,
		S00PacketKeepAlive,
		S21PacketChunkData,
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
		S42PacketCombatEvent,
		S38PacketPlayerListItem,
		S0APacketUseBed,
		S13PacketDestroyEntities,
		S1EPacketRemoveEntityEffect,
		S48PacketResourcePackSend,
		S07PacketRespawn,
		S19PacketEntityHeadLook,
		S44PacketWorldBorder,
		S43PacketCamera,
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
		S45PacketTitle,
		S33PacketUpdateSign,
		S47PacketPlayerListHeaderFooter,
		S0DPacketCollectItem,
		S18PacketEntityTeleport,
		S20PacketEntityProperties,
		S1DPacketEntityEffect,
		C14PacketTabComplete,
		C01PacketChatMessage,
		C16PacketClientStatus,
		C15PacketClientSettings,
		C0FPacketConfirmTransaction,
		C11PacketEnchantItem,
		C0EPacketClickWindow,
		C0DPacketCloseWindow,
		C17PacketCustomPayload,
		C02PacketUseEntity,
		C00PacketKeepAlive,
		C03PacketPlayer,
		C04PacketPlayerPosition,
		C06PacketPlayerPosLook,
		C05PacketPlayerLook,
		C13PacketPlayerAbilities,
		C07PacketPlayerDigging,
		C0BPacketEntityAction,
		C0CPacketInput,
		C19PacketResourcePackStatus,
		C09PacketHeldItemChange,
		C10PacketCreativeInventoryAction,
		C12PacketUpdateSign,
		C0APacketAnimation,
		C18PacketSpectate,
		C08PacketPlayerBlockPlacement,
		C00Handshake,
		S02PacketLoginSuccess,
		S01PacketEncryptionRequest,
		S03PacketEnableCompression,
		S00PacketDisconnect,
		C00PacketLoginStart,
		C01PacketEncryptionResponse,
		S01PacketPong,
		S00PacketServerInfo,
		C01PacketPing,
		C00PacketServerQuery,
	};
	
	private static int nextPacketIndex;
	
	private final String shortName;
	
	private final int index;
	
	private Packets(String seargeName, String obfName)
	{
		super(seargeName, obfName);
		
		this.shortName = seargeName.substring(Math.max(seargeName.lastIndexOf('.'), seargeName.lastIndexOf('$')) + 1);
		this.index = Packets.nextPacketIndex++;
		Packets.packetMap.put(this.shortName, this);
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

	/**
	 * @param name
	 * @return
	 */
	public static Packets getByName(String name)
	{
		return Packets.packetMap.get(name);
	}
}
