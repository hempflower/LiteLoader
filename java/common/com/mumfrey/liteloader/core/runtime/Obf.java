package com.mumfrey.liteloader.core.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Centralised obfuscation table for LiteLoader
 *
 * @author Adam Mummery-Smith
 * TODO Obfuscation 1.7.10
 */
public class Obf
{
	// Non-obfuscated references, here for convenience
	// -----------------------------------------------------------------------------------------
	public static final Obf          CallbackProxyClient = new Obf("com.mumfrey.liteloader.client.CallbackProxyClient"                 );
	public static final Obf          CallbackProxyServer = new Obf("com.mumfrey.liteloader.server.CallbackProxyServer"                 );
	public static final Obf                   EventProxy = new Obf("com.mumfrey.liteloader.core.event.EventProxy"                      );
	public static final Obf                  HandlerList = new Obf("com.mumfrey.liteloader.core.event.HandlerList"                     );
	public static final Obf             BakedHandlerList = new Obf("com.mumfrey.liteloader.core.event.HandlerList$BakedHandlerList"    );
	public static final Obf                   LoadingBar = new Obf("com.mumfrey.liteloader.client.gui.startup.LoadingBar"              );
	public static final Obf                  GameProfile = new Obf("com.mojang.authlib.GameProfile"                                    );
	public static final Obf                MinecraftMain = new Obf("net.minecraft.client.main.Main"                                    );
	public static final Obf              MinecraftServer = new Obf("net.minecraft.server.MinecraftServer"                              );
	public static final Obf                         GL11 = new Obf("org.lwjgl.opengl.GL11"                                             );
	public static final Obf                  constructor = new Obf("<init>"                                                            );

	// Classes
	// -----------------------------------------------------------------------------------------
	public static final Obf                    Minecraft = new Obf("net.minecraft.client.Minecraft",                             "bao" );
	public static final Obf               EntityRenderer = new Obf("net.minecraft.client.renderer.EntityRenderer",               "blt" );
	public static final Obf                    GuiIngame = new Obf("net.minecraft.client.gui.GuiIngame",                         "bbv" );
	public static final Obf                     Profiler = new Obf("net.minecraft.profiler.Profiler",                            "qi"  );
	public static final Obf                CrashReport$6 = new Obf("net.minecraft.crash.CrashReport$6",                          "h"   );
	public static final Obf            S01PacketJoinGame = new Obf("net.minecraft.network.play.server.S01PacketJoinGame",        "hd"  );
	public static final Obf        S02PacketLoginSuccess = new Obf("net.minecraft.network.login.server.S02PacketLoginSuccess",   "js"  );
	public static final Obf                S02PacketChat = new Obf("net.minecraft.network.play.server.S02PacketChat",            "gj"  );
	public static final Obf       S3FPacketCustomPayload = new Obf("net.minecraft.network.play.server.S3FPacketCustomPayload",   "gr"  );
	public static final Obf                  INetHandler = new Obf("net.minecraft.network.INetHandler",                          "fb"  );
	public static final Obf         C01PacketChatMessage = new Obf("net.minecraft.network.play.client.C01PacketChatMessage",     "ir"  );
	public static final Obf       C17PacketCustomPayload = new Obf("net.minecraft.network.play.client.C17PacketCustomPayload",   "iz"  );
	public static final Obf             IntegratedServer = new Obf("net.minecraft.server.integrated.IntegratedServer",           "bsx" );
	public static final Obf                WorldSettings = new Obf("net.minecraft.world.WorldSettings",                          "ahj" );
	public static final Obf   ServerConfigurationManager = new Obf("net.minecraft.server.management.ServerConfigurationManager", "oi"  );
	public static final Obf               EntityPlayerMP = new Obf("net.minecraft.entity.player.EntityPlayerMP",                 "mw"  );
	public static final Obf               NetworkManager = new Obf("net.minecraft.network.NetworkManager",                       "ej"  );
	public static final Obf              DedicatedServer = new Obf("net.minecraft.server.dedicated.DedicatedServer",             "lt"  );
	public static final Obf         EntityClientPlayerMP = new Obf("net.minecraft.client.entity.EntityClientPlayerMP",           "bjk" );
	public static final Obf                       Blocks = new Obf("net.minecraft.init.Blocks",                                  "ajn" );
	public static final Obf                        Items = new Obf("net.minecraft.init.Items",                                   "ade" );
	public static final Obf                  FrameBuffer = new Obf("net.minecraft.client.shader.Framebuffer",                    "bmg" );
	public static final Obf                   GuiNewChat = new Obf("net.minecraft.client.gui.GuiNewChat",                        "bcc" );

	// Fields
	// -----------------------------------------------------------------------------------------
	public static final Obf            minecraftProfiler = new Obf("field_71424_I",                                              "z"   ); // Minecraft/mcProfiler
	public static final Obf              entityRenderMap = new Obf("field_78729_o",                                              "q"   ); // RenderManager/entityRenderMap
	public static final Obf              reloadListeners = new Obf("field_110546_b",                                             "d"   ); // SimpleReloadableResourceManager/reloadListeners
	public static final Obf                   netManager = new Obf("field_147393_d",                                             "d"   ); // NetHandlerLoginClient/field_147393_d
	public static final Obf              registryObjects = new Obf("field_82596_a",                                              "c"   ); // RegistrySimple/registryObjects
	public static final Obf         underlyingIntegerMap = new Obf("field_148759_a",                                             "a"   ); // RegistryNamespaced/underlyingIntegerMap
	public static final Obf                  identityMap = new Obf("field_148749_a",                                             "a"   ); // ObjectIntIdentityMap/field_148749_a
	public static final Obf                   objectList = new Obf("field_148748_b",                                             "b"   ); // ObjectIntIdentityMap/field_148748_b
	public static final Obf          mapSpecialRenderers = new Obf("field_147559_m",                                             "m"   ); // TileEntityRendererDispatcher/mapSpecialRenderers
	public static final Obf     tileEntityNameToClassMap = new Obf("field_145855_i",                                             "i"   ); // TileEntity/nameToClassMap
	public static final Obf     tileEntityClassToNameMap = new Obf("field_145853_j",                                             "i"   ); // TileEntity/classToNameMap

	// Methods
	// -----------------------------------------------------------------------------------------
	public static final Obf                processPacket = new Obf("func_148833_a",                                              "a"   );
	public static final Obf                  runGameLoop = new Obf("func_71411_J",                                               "ak"  );
	public static final Obf                      runTick = new Obf("func_71407_l",                                               "p"   ); 
	public static final Obf        updateCameraAndRender = new Obf("func_78480_b",                                               "b"   ); 
	public static final Obf                  renderWorld = new Obf("func_78471_a",                                               "a"   ); 
	public static final Obf            renderGameOverlay = new Obf("func_73830_a",                                               "a"   ); 
	public static final Obf                 startSection = new Obf("func_76320_a",                                               "a"   ); 
	public static final Obf                   endSection = new Obf("func_76319_b",                                               "b"   ); 
	public static final Obf              endStartSection = new Obf("func_76318_c",                                               "c"   );  
	public static final Obf                  spawnPlayer = new Obf("func_148545_a",                                              "a"   );
	public static final Obf                respawnPlayer = new Obf("func_72368_a",                                               "a"   );
	public static final Obf initializeConnectionToPlayer = new Obf("func_72355_a",                                               "a"   );
	public static final Obf               playerLoggedIn = new Obf("func_72377_c",                                               "c"   );
	public static final Obf              playerLoggedOut = new Obf("func_72367_e",                                               "e"   );
	public static final Obf                    startGame = new Obf("func_71384_a",                                               "ag"  );
	public static final Obf                  startServer = new Obf("func_71197_b",                                               "e"   );
	public static final Obf            startServerThread = new Obf("func_71256_s",                                               "w"   );
	public static final Obf              sendChatMessage = new Obf("func_71165_d",                                               "a"   );
	public static final Obf        updateFramebufferSize = new Obf("func_147119_ah",                                             "an"  );
	public static final Obf            framebufferRender = new Obf("func_147615_c",                                              "c"   );
	public static final Obf       bindFramebufferTexture = new Obf("func_147612_c",                                              "c"   );
	public static final Obf                     drawChat = new Obf("func_146230_a",                                              "a"   );

	public static final int MCP = 0;
	public static final int SRG = 1;
	public static final int OBF = 2;
	
	private static Properties mcpNames;

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
	 */
	protected Obf(String mcpName)
	{
		this(mcpName, mcpName, mcpName);
	}
	
	/**
	 * @param mcpName
	 * @param seargeName
	 * @param obfName
	 */
	protected Obf(String seargeName, String obfName)
	{
		this(seargeName, obfName, null);
	}

	/**
	 * @param seargeName
	 * @param obfName
	 * @param mcpName
	 */
	protected Obf(String seargeName, String obfName, String mcpName)
	{
		this.name = mcpName != null ? mcpName : this.getDeobfuscatedName(seargeName);
		this.ref = this.name.replace('.', '/');
		this.srg = seargeName;
		this.obf = obfName;
		
		this.names = new String[] { this.name, this.srg, this.obf };
	}
	
	/**
	 * @param type
	 * @return
	 */
	public String getDescriptor(int type)
	{
		return String.format("L%s;", this.names[type].replace('.', '/'));
	}

	/**
	 * @param seargeName
	 * @return
	 */
	protected String getDeobfuscatedName(String seargeName)
	{
		return Obf.getDeobfName(seargeName);
	}

	/**
	 * @param seargeName
	 * @return
	 */
	static String getDeobfName(String seargeName)
	{
		if (Obf.mcpNames == null)
		{
			Obf.mcpNames = new Properties();
			InputStream is = Obf.class.getResourceAsStream("/obfuscation.properties");
			if (is != null)
			{
				try
				{
					Obf.mcpNames.load(is);
				}
				catch (IOException ex) {}
				
				try
				{
					is.close();
				}
				catch (IOException ex) {}
			}
		}
		
		return Obf.mcpNames.getProperty(seargeName, seargeName);
	}
}
