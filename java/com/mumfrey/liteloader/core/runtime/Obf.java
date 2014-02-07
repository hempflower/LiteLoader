package com.mumfrey.liteloader.core.runtime;

/**
 * Centralised obfuscation table for LiteLoader
 *
 * @author Adam Mummery-Smith
 * TODO Obfuscation 1.7.2
 */
public enum Obf
{
	// Non-obfuscated references, here for convenience
	// -----------------------------------------------------------------------------------------
	       InjectedCallbackProxy("com.mumfrey.liteloader.core.transformers.InjectedCallbackProxy"    ),
	                 GameProfile("com.mojang.authlib.GameProfile"                                    ),
	               MinecraftMain("net.minecraft.client.main.Main"                                    ),
	                 constructor("<init>"                                                            ),

	// Classes
	// -----------------------------------------------------------------------------------------
	         RenderLightningBolt("net.minecraft.client.renderer.entity.RenderLightningBolt",   "bny" ),
	                   Minecraft("net.minecraft.client.Minecraft",                             "azd" ),
	              EntityRenderer("net.minecraft.client.renderer.EntityRenderer",               "bll" ),
	                   GuiIngame("net.minecraft.client.gui.GuiIngame",                         "bah" ),
	                    Profiler("net.minecraft.profiler.Profiler",                            "ov"  ),
	               CrashReport$6("net.minecraft.crash.CrashReport$6",                          "i"   ),
	           S01PacketJoinGame("net.minecraft.network.play.server.S01PacketJoinGame",        "gu"  ),
	       S02PacketLoginSuccess("net.minecraft.network.login.server.S02PacketLoginSuccess",   "jg"  ),
	               S02PacketChat("net.minecraft.network.play.server.S02PacketChat",            "ga"  ),
	      S3FPacketCustomPayload("net.minecraft.network.play.server.S3FPacketCustomPayload",   "gi"  ),
	                 INetHandler("net.minecraft.network.INetHandler",                          "es"  ),
	        C01PacketChatMessage("net.minecraft.network.play.client.C01PacketChatMessage",     "ie"  ),
	      C17PacketCustomPayload("net.minecraft.network.play.client.C17PacketCustomPayload",   "in"  ),
	            IntegratedServer("net.minecraft.server.integrated.IntegratedServer",           "bsk" ),
	               WorldSettings("net.minecraft.world.WorldSettings",                          "afv" ),
	  ServerConfigurationManager("net.minecraft.server.management.ServerConfigurationManager", "ld"  ),
	              EntityPlayerMP("net.minecraft.entity.player.EntityPlayerMP",                 "mm"  ),
	              NetworkManager("net.minecraft.network.NetworkManager",                       "ef"  ),

	// Fields
	// -----------------------------------------------------------------------------------------
	           minecraftProfiler("mcProfiler",                              "field_71424_I",   "A"   ), // Minecraft/mcProfiler
	             entityRenderMap("entityRenderMap",                         "field_78729_o",   "q"   ), // RenderManager/entityRenderMap
	             reloadListeners("reloadListeners",                         "field_110546_b",  "d"   ), // SimpleReloadableResourceManager/reloadListeners
	                  netManager("field_147393_d",                                             "d"   ), // NetHandlerLoginClient/field_147393_d

	// Methods
	// -----------------------------------------------------------------------------------------
	               processPacket("processPacket",                           "func_148833_a",   "a"   ),
	                 runGameLoop("runGameLoop",                             "func_71411_J",    "ad"  ),
	                     runTick("runTick",                                 "func_71407_l",    "o"   ), 
	       updateCameraAndRender("updateCameraAndRender",                   "func_78480_b",    "b"   ), 
	                 renderWorld("renderWorld",                             "func_78471_a",    "a"   ), 
	           renderGameOverlay("renderGameOverlay",                       "func_73830_a",    "a"   ), 
	                startSection("startSection",                            "func_76320_a",    "a"   ), 
	                  endSection("endSection",                              "func_76319_b",    "b"   ), 
	             endStartSection("endStartSection",                         "func_76318_c",    "c"   ),  
	                 spawnPlayer("func_148545_a",                           "func_148545_a",   "a"   ),
	               respawnPlayer("respawnPlayer",                           "func_72368_a",    "a"   ),
	initializeConnectionToPlayer("initializeConnectionToPlayer",            "func_72355_a",    "a"   ),
	              playerLoggedIn("playerLoggedIn",                          "func_72377_c",    "c"   ),
	             playerLoggedOut("playerLoggedOut",                         "func_72367_e",    "e"   );

	public static final int MCP = 0;
	public static final int SRG = 1;
	public static final int OBF = 2;

	/**
	 * Array of names, indexed by MCP, SRG, OBF constants
	 */
	public final String[] names;
	   
	/**
	 * Class, field or method name in unobfuscated (MCP) format
	 */
	public final String name;

	/**
	 * Class name in bytecode notation with slashes instead of dots
	 */
	public final String ref;
	
	/**
	 * Class, field or method name in searge format
	 */
	public final String srg;
	
	/**
	 * Class, field or method name in obfuscated (original) format
	 */
	public final String obf;
	
	/**
	 * @param mcpName
	 * @param seargeName
	 * @param obfName
	 */
	private Obf(String mcpName, String seargeName, String obfName)
	{
		this.name = mcpName;
		this.ref = mcpName.replace('.', '/');
		this.srg = seargeName;
		this.obf = obfName;
		
		this.names = new String[] { this.name, this.srg, this.obf };
	}
	
	/**
	 * @param mcpName
	 * @param obfName
	 */
	private Obf(String mcpName, String obfName)
	{
		this(mcpName, mcpName, obfName);
	}
	
	/**
	 * @param mcpName
	 */
	private Obf(String mcpName)
	{
		this(mcpName, mcpName, mcpName);
	}
	
	/**
	 * @param type
	 * @return
	 */
	public String getDescriptor(int type)
	{
		return String.format("L%s;", this.names[type].replace('.', '/'));
	}
}
