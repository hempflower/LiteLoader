package com.mumfrey.liteloader;

import net.minecraft.network.login.INetHandlerLoginClient;
import net.minecraft.network.login.server.S02PacketLoginSuccess;

/**
 *
 * @author Adam Mummery-Smith
 */
public interface PostLoginListener extends LiteMod
{
	public abstract void onPostLogin(INetHandlerLoginClient netHandler, S02PacketLoginSuccess packet);
}
