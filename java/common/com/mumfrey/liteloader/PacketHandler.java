package com.mumfrey.liteloader;

import java.util.List;

import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;

/**
 * Interface for mods which want to handle raw packets
 * 
 * @author Adam Mummery-Smith
 */
public interface PacketHandler extends LiteMod
{
	/**
	 * Get list of packets to handle
	 */
	public List<Class<? extends Packet>> getHandledPackets(); 

	/**
	 * @param netHandler
	 * @param packet
	 * @return
	 */
	public abstract boolean handlePacket(INetHandler netHandler, Packet packet);
}
