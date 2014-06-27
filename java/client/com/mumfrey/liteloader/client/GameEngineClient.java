package com.mumfrey.liteloader.client;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.integrated.IntegratedServer;

import com.mumfrey.liteloader.client.overlays.IMinecraft;
import com.mumfrey.liteloader.common.GameEngine;
import com.mumfrey.liteloader.common.LoadingProgress;

/**
 *
 * @author Adam Mummery-Smith
 */
public class GameEngineClient implements GameEngine<Minecraft, IntegratedServer>
{
	private final Minecraft engine = Minecraft.getMinecraft();
	
	/**
	 * Registered resource packs 
	 */
	private final Map<String, IResourcePack> registeredResourcePacks = new HashMap<String, IResourcePack>();

	/**
	 * True while initialising mods if we need to do a resource manager reload once the process is completed
	 */
	private boolean pendingResourceReload;

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.common.GameEngine#getProfiler()
	 */
	@Override
	public Profiler getProfiler()
	{
		return this.engine.mcProfiler;
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.common.GameEngine#refreshResources(boolean)
	 */
	@Override
	public void refreshResources(boolean force)
	{
		if (this.pendingResourceReload || force)
		{
			LoadingProgress.setMessage("Reloading Resources...");
			this.pendingResourceReload = false;
			this.engine.refreshResources();
		}
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.common.GameEngine#isClient()
	 */
	@Override
	public boolean isClient()
	{
		return true;
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.common.GameEngine#isServer()
	 */
	@Override
	public boolean isServer()
	{
		return this.isSinglePlayer();
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.common.GameEngine#isInGame()
	 */
	@Override
	public boolean isInGame()
	{
		return this.engine.thePlayer != null && this.engine.theWorld != null && this.engine.theWorld.isRemote;
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.common.GameEngine#isRunning()
	 */
	@Override
	public boolean isRunning()
	{
		return ((IMinecraft)this.engine).isRunning();
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.common.GameEngine#isSingleplayer()
	 */
	@Override
	public boolean isSinglePlayer()
	{
		return this.engine.isSingleplayer();
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.common.GameEngine#getClient()
	 */
	@Override
	public Minecraft getClient()
	{
		return this.engine;
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.common.GameEngine#getServer()
	 */
	@Override
	public IntegratedServer getServer()
	{
		return this.engine.getIntegratedServer();
	}
	
	public GameSettings getGameSettings()
	{
		return this.engine.gameSettings;
	}

	public ScaledResolution getScaledResolution()
	{
		return new ScaledResolution(this.engine, this.engine.displayWidth, this.engine.displayHeight);
	}

	public GuiNewChat getChatGUI()
	{
		return this.engine.ingameGUI.getChatGUI();
	}

	public GuiScreen getCurrentScreen()
	{
		return this.engine.currentScreen;
	}

	public boolean hideGUI()
	{
		return this.engine.gameSettings.hideGUI;
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.common.GameEngine#getResourceManager()
	 */
	@Override
	public IResourceManager getResourceManager()
	{
		return this.engine.getResourceManager();
	}

	/**
	 * @return
	 */
	public SoundHandler getSoundHandler()
	{
		return this.engine.getSoundHandler();
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.common.GameEngine#registerResourcePack(net.minecraft.client.resources.IResourcePack)
	 */
	@Override
	public boolean registerResourcePack(IResourcePack resourcePack)
	{
		if (!this.registeredResourcePacks.containsKey(resourcePack.getPackName()))
		{
			this.pendingResourceReload = true;

			List<IResourcePack> defaultResourcePacks = ((IMinecraft)this.engine).getDefaultResourcePacks();
			if (!defaultResourcePacks.contains(resourcePack))
			{
				defaultResourcePacks.add(resourcePack);
				this.registeredResourcePacks.put(resourcePack.getPackName(), resourcePack);
				return true;
			}
		}
		
		return false;
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.common.GameEngine#unRegisterResourcePack(net.minecraft.client.resources.IResourcePack)
	 */
	@Override
	public boolean unRegisterResourcePack(IResourcePack resourcePack)
	{
		if (this.registeredResourcePacks.containsValue(resourcePack))
		{
			this.pendingResourceReload = true;

			List<IResourcePack> defaultResourcePacks = ((IMinecraft)this.engine).getDefaultResourcePacks();
			this.registeredResourcePacks.remove(resourcePack.getPackName());
			defaultResourcePacks.remove(resourcePack);
			return true;
		}
		
		return false;
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.common.GameEngine#getKeyBindings()
	 */
	@Override
	public List<KeyBinding> getKeyBindings()
	{
		LinkedList<KeyBinding> keyBindings = new LinkedList<KeyBinding>();
		keyBindings.addAll(Arrays.asList(this.engine.gameSettings.keyBindings));
		return keyBindings;
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.common.GameEngine#setKeyBindings(java.util.List)
	 */
	@Override
	public void setKeyBindings(List<KeyBinding> keyBindings)
	{
		this.engine.gameSettings.keyBindings = keyBindings.toArray(new KeyBinding[0]);
	}
}
