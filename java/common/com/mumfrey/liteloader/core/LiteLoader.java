package com.mumfrey.liteloader.core;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.activity.InvalidActivityException;

import net.minecraft.client.resources.IResourcePack;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraft.network.INetHandler;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.profiler.Profiler;
import net.minecraft.world.World;

import org.apache.logging.log4j.Logger;

import com.mumfrey.liteloader.LiteMod;
import com.mumfrey.liteloader.api.CoreProvider;
import com.mumfrey.liteloader.api.CustomisationProvider;
import com.mumfrey.liteloader.api.LiteAPI;
import com.mumfrey.liteloader.api.ModLoadObserver;
import com.mumfrey.liteloader.api.PostRenderObserver;
import com.mumfrey.liteloader.api.ShutdownObserver;
import com.mumfrey.liteloader.api.TickObserver;
import com.mumfrey.liteloader.api.WorldObserver;
import com.mumfrey.liteloader.api.manager.APIAdapter;
import com.mumfrey.liteloader.api.manager.APIProvider;
import com.mumfrey.liteloader.common.GameEngine;
import com.mumfrey.liteloader.common.LoadingProgress;
import com.mumfrey.liteloader.core.api.LiteLoaderCoreAPI;
import com.mumfrey.liteloader.core.event.EventProxy;
import com.mumfrey.liteloader.crashreport.CallableLaunchWrapper;
import com.mumfrey.liteloader.crashreport.CallableLiteLoaderBrand;
import com.mumfrey.liteloader.crashreport.CallableLiteLoaderMods;
import com.mumfrey.liteloader.interfaces.Loadable;
import com.mumfrey.liteloader.interfaces.LoadableMod;
import com.mumfrey.liteloader.interfaces.LoaderEnumerator;
import com.mumfrey.liteloader.interfaces.PanelManager;
import com.mumfrey.liteloader.interfaces.ObjectFactory;
import com.mumfrey.liteloader.launch.LoaderEnvironment;
import com.mumfrey.liteloader.launch.LoaderProperties;
import com.mumfrey.liteloader.modconfig.ConfigManager;
import com.mumfrey.liteloader.modconfig.Exposable;
import com.mumfrey.liteloader.permissions.PermissionsManagerClient;
import com.mumfrey.liteloader.permissions.PermissionsManagerServer;
import com.mumfrey.liteloader.util.Input;
import com.mumfrey.liteloader.util.log.LiteLoaderLogger;

/**
 * LiteLoader is a simple loader which loads and provides useful callbacks to
 * lightweight mods
 * 
 * @author Adam Mummery-Smith
 */
public final class LiteLoader
{
	/**
	 * LiteLoader is a singleton, this is the singleton instance
	 */
	private static LiteLoader instance;
	
	/**
	 * Logger for LiteLoader events
	 */
	private static final Logger logger = LiteLoaderLogger.getLogger();
	
	/**
	 * Tweak system class loader 
	 */
	private static LaunchClassLoader classLoader;
	
	/**
	 * Reference to the game engine instance
	 */
	private GameEngine<?, ?> engine;
	
	/**
	 * Minecraft Profiler
	 */
	private Profiler profiler;
	
	/**
	 * Loader environment instance 
	 */
	private final LoaderEnvironment environment;
	
	/**
	 * Loader Properties adapter 
	 */
	private final LoaderProperties properties;
	
	/**
	 * Mod enumerator instance
	 */
	private final LoaderEnumerator enumerator;

	/**
	 * Mods
	 */
	protected final LiteLoaderMods mods;
	
	/**
	 * API Provider instance 
	 */
	private final APIProvider apiProvider;
	
	/**
	 * API Adapter instance
	 */
	private final APIAdapter apiAdapter;
	
	/**
	 * Our core API instance
	 */
	private final LiteLoaderCoreAPI api;
	
	/**
	 * Factory which can be used to instance main loader helper objects
	 */
	private final ObjectFactory<?, ?> objectFactory;
	
	/**
	 * Core providers
	 */
	private final List<CoreProvider> coreProviders = new LinkedList<CoreProvider>();
	
	/**
	 * 
	 */
	private final List<TickObserver> tickObservers = new LinkedList<TickObserver>();
	
	/**
	 * 
	 */
	private final List<WorldObserver> worldObservers = new LinkedList<WorldObserver>();
	
	/**
	 * 
	 */
	private final List<ShutdownObserver> shutdownObservers = new LinkedList<ShutdownObserver>();
	
	/**
	 * 
	 */
	private final List<PostRenderObserver> postRenderObservers = new LinkedList<PostRenderObserver>();
	
	/**
	 * Mod panel manager, deliberately raw
	 */
	@SuppressWarnings("rawtypes")
	private PanelManager modPanelManager;
	
	/**
	 * Interface Manager
	 */
	private LiteLoaderInterfaceManager interfaceManager;
	
	/**
	 * Event manager
	 */
	private Events<?, ?> events;

	/**
	 * Plugin channel manager 
	 */
	private final ClientPluginChannels clientPluginChannels;
	
	/**
	 * Server channel manager 
	 */
	private final ServerPluginChannels serverPluginChannels;
	
	/**
	 * Permission Manager
	 */
	private final PermissionsManagerClient permissionsManagerClient;
	
	private final PermissionsManagerServer permissionsManagerServer;
	
	/**
	 * Mod configuration manager
	 */
	private final ConfigManager configManager;
	
	/**
	 * Flag which keeps track of whether late initialisation has completed
	 */
	private boolean modInitComplete;
	
	/**
	 * 
	 */
	private Input input;
	
	/**
	 * LiteLoader constructor
	 * @param profile 
	 * @param modNameFilter 
	 */
	private LiteLoader(LoaderEnvironment environment, LoaderProperties properties)
	{
		this.environment = environment;
		this.properties = properties;
		this.enumerator = environment.getEnumerator();
		
		this.configManager = new ConfigManager();
		this.input = new Input(new File(environment.getCommonConfigFolder(), "liteloader.keys.properties"));

		this.mods = new LiteLoaderMods(this, environment, properties, this.configManager);
		
		this.apiProvider = environment.getAPIProvider();
		this.apiAdapter = environment.getAPIAdapter();
		
		this.api = this.apiProvider.getAPI(LiteLoaderCoreAPI.class);
		if (this.api == null)
		{
			throw new IllegalStateException("The core API was not registered. Startup halted");
		}
		
		this.objectFactory = this.api.getObjectFactory();

		this.clientPluginChannels = this.objectFactory.getClientPluginChannels();
		this.serverPluginChannels = this.objectFactory.getServerPluginChannels();
		
		this.permissionsManagerClient = this.objectFactory.getClientPermissionManager();
		this.permissionsManagerServer = this.objectFactory.getServerPermissionManager();
	}
	
	/**
	 * Set up reflection methods required by the loader
	 */
	private void onInit()
	{
		try
		{
			this.coreProviders.addAll(this.apiAdapter.getCoreProviders());
			this.tickObservers.addAll(this.apiAdapter.getAllObservers(TickObserver.class));
			this.worldObservers.addAll(this.apiAdapter.getAllObservers(WorldObserver.class));
			this.shutdownObservers.addAll(this.apiAdapter.getAllObservers(ShutdownObserver.class));
			this.postRenderObservers.addAll(this.apiAdapter.getAllObservers(PostRenderObserver.class));
			
			for (CoreProvider coreProvider : this.coreProviders)
			{
				coreProvider.onInit();
			}
			
			this.enumerator.onInit();
			this.mods.init(this.apiAdapter.getAllObservers(ModLoadObserver.class));
		}
		catch (Throwable th)
		{
			LiteLoaderLogger.severe(th, "Error initialising LiteLoader", th);
		}
	}
	
	/**
	 * 
	 */
	private void onPostInit()
	{
		LoadingProgress.setMessage("LiteLoader POSTINIT...");
		
		this.initLifetimeObjects();
		
		this.postInitCoreProviders();
		
		// Spawn mod instances and initialise them
		this.loadAndInitMods();

		for (CoreProvider coreProvider : this.coreProviders)
		{
			coreProvider.onPostInitComplete(this.mods);
		}
		
		// Save stuff
		this.properties.writeProperties();
	}

	/**
	 * @param resourcePack
	 * @return
	 * 
	 * @deprecated Use LiteLoader.getGameEngine().registerResourcePack() instead
	 */
	@Deprecated
	public boolean registerModResourcePack(IResourcePack resourcePack)
	{
		return this.engine.registerResourcePack(resourcePack);
	}

	/**
	 * @param resourcePack
	 * @return
	 * 
	 * @deprecated Use LiteLoader.getGameEngine().unRegisterResourcePack() instead
	 */
	@Deprecated
	public boolean unRegisterModResourcePack(IResourcePack resourcePack)
	{
		return this.engine.unRegisterResourcePack(resourcePack);
	}
	
	/**
	 * Get the singleton instance of LiteLoader, initialises the loader if
	 * necessary
	 * 
	 * @param locationProvider
	 * @return LiteLoader instance
	 */
	public static final LiteLoader getInstance()
	{
		return LiteLoader.instance;
	}
	
	/**
	 * Get the LiteLoader logger object
	 * 
	 * @return
	 * @deprecated use LiteLoaderLogger instead
	 */
	@Deprecated
	public static final Logger getLogger()
	{
		return LiteLoader.logger;
	}
	
	/**
	 * Get the tweak system classloader
	 * 
	 * @return
	 */
	public static LaunchClassLoader getClassLoader()
	{
		return LiteLoader.classLoader;
	}
	
	/**
	 * Get the output stream which we are using for console output
	 * 
	 * @return System.err
	 * @deprecated use log4j instead
	 */
	@Deprecated
	public static final PrintStream getConsoleStream()
	{
		return System.err;
	}
	
	/**
	 * Get LiteLoader version
	 * 
	 * @return
	 */
	public static final String getVersion()
	{
		return LiteLoaderVersion.CURRENT.getLoaderVersion();
	}
	
	/**
	 * Get LiteLoader version
	 * 
	 * @return
	 */
	public static final String getVersionDisplayString()
	{
		return String.format("LiteLoader %s", LiteLoaderVersion.CURRENT.getLoaderVersion());
	}
	
	/**
	 * Get the loader revision
	 * 
	 * @return
	 */
	public static final int getRevision()
	{
		return LiteLoaderVersion.CURRENT.getLoaderRevision();
	}
	
	/**
	 * @return
	 */
	public static final LiteAPI[] getAPIs()
	{
		LiteAPI[] apis = LiteLoader.instance.apiProvider.getAPIs();
		LiteAPI[] apisCopy = new LiteAPI[apis.length];
		System.arraycopy(apis, 0, apisCopy, 0, apis.length);
		return apisCopy;
	}
	
	/**
	 * @param identifier
	 * @return
	 */
	public static final LiteAPI getAPI(String identifier)
	{
		return LiteLoader.instance.apiProvider.getAPI(identifier);
	}
	
	@SuppressWarnings("unchecked")
	public static final <C extends CustomisationProvider> C getCustomisationProvider(LiteAPI api, Class<C> providerType)
	{
		List<CustomisationProvider> customisationProviders = api.getCustomisationProviders();
		if (customisationProviders != null)
		{
			for (CustomisationProvider provider : customisationProviders)
				if (providerType.isAssignableFrom(provider.getClass())) return (C)provider;
		}
		
		return null;
	}
	
	/**
	 * @param identifier
	 * @return
	 */
	public static boolean isAPIAvailable(String identifier)
	{
		return LiteLoader.getAPI(identifier) != null;
	}
	
	/**
	 * @return
	 * 
	 * @deprecated use getClientPermissionsManager instead
	 */
	@Deprecated
	public static PermissionsManagerClient getPermissionsManager()
	{
		return LiteLoader.instance.permissionsManagerClient;
	}
	
	public static PermissionsManagerClient getClientPermissionsManager()
	{
		return LiteLoader.instance.permissionsManagerClient;
	}

	public static PermissionsManagerServer getServerPermissionsManager()
	{
		return LiteLoader.instance.permissionsManagerServer;
	}
	
	public static GameEngine<?, ?> getGameEngine()
	{
		return LiteLoader.instance.engine;
	}
	
	/**
	 * @return
	 */
	public static LiteLoaderInterfaceManager getInterfaceManager()
	{
		return LiteLoader.instance.interfaceManager;
	}
	
	/**
	 * Get the event manager
	 * 
	 * @deprecated DO NOT USE, register listeners with the interface manager instead!
	 * 
	 * @return
	 */
	@Deprecated
	public static Events<?, ?> getEvents()
	{
		return LiteLoader.instance.events;
	}
	
	/**
	 * Get the plugin channel manager
	 * 
	 * @return
	 * @deprecated use LiteLoader.getClientPluginChannels()
	 */
	@Deprecated
	public static ClientPluginChannels getPluginChannels()
	{
		return LiteLoader.instance.clientPluginChannels;
	}

	/**
	 * Get the client-side plugin channel manager
	 * 
	 * @return
	 */
	public static ClientPluginChannels getClientPluginChannels()
	{
		return LiteLoader.instance.clientPluginChannels;
	}
	
	/**
	 * Get the server-side plugin channel manager
	 * 
	 * @return
	 */
	public static ServerPluginChannels getServerPluginChannels()
	{
		return LiteLoader.instance.serverPluginChannels;
	}
	
	/**
	 * Get the input manager
	 */
	public static Input getInput()
	{
		return LiteLoader.instance.input;
	}
	
	/**
	 * Get the mod panel manager
	 * 
	 * @return
	 */
	@SuppressWarnings({ "cast", "unchecked" })
	public static <T> PanelManager<T> getModPanelManager()
	{
		return (PanelManager<T>)LiteLoader.instance.modPanelManager;
	}
	
	/**
	 * Get the "mods" folder
	 */
	public static File getModsFolder()
	{
		return LiteLoader.instance.environment.getModsFolder();
	}
	
	/**
	 * Get the common (version-independent) config folder
	 */
	public static File getCommonConfigFolder()
	{
		return LiteLoader.instance.environment.getCommonConfigFolder();
	}
	
	/**
	 * Get the config folder for this version
	 */
	public static File getConfigFolder()
	{
		return LiteLoader.instance.environment.getVersionedConfigFolder();
	}
	
	/**
	 * @return
	 */
	public static File getGameDirectory()
	{
		return LiteLoader.instance.environment.getGameDirectory();
	}
	
	/**
	 * @return
	 */
	public static File getAssetsDirectory()
	{
		return LiteLoader.instance.environment.getAssetsDirectory();
	}
	
	/**
	 * @return
	 */
	public static String getProfile()
	{
		return LiteLoader.instance.environment.getProfile();
	}
	
	/**
	 * Used to get the name of the modpack being used
	 * 
	 * @return name of the modpack in use or null if no pack
	 */
	public static String getBranding()
	{
		return LiteLoader.instance.properties.getBranding();
	}
	
	/**
	 * @return
	 */
	public static boolean isDevelopmentEnvironment()
	{
		return "true".equals(System.getProperty("mcpenv"));
	}
	
	/**
	 * Used for crash reporting, returns a text list of all loaded mods
	 * 
	 * @return List of loaded mods as a string
	 */
	public String getLoadedModsList()
	{
		return this.mods.getLoadedModsList();
	}
	
	/**
	 * Get a list containing all loaded mods
	 */
	public List<LiteMod> getLoadedMods()
	{
		List<LiteMod> loadedMods = new ArrayList<LiteMod>();
		
		for (ModInfo<LoadableMod<?>> loadedMod : this.mods.getLoadedMods())
		{
			loadedMods.add(loadedMod.getMod());
		}
		
		return loadedMods;		
	}
	
	/**
	 * Get a list containing all mod files which were NOT loaded
	 */
	public List<Loadable<?>> getDisabledMods()
	{
		List<Loadable<?>> disabledMods = new ArrayList<Loadable<?>>();
		
		for (ModInfo<?> disabledMod : this.mods.getDisabledMods())
		{
			disabledMods.add(disabledMod.getContainer());
		}
		
		return disabledMods;
	}
	
	/**
	 * Get the list of injected tweak containers
	 */
	@SuppressWarnings("unchecked")
	public Collection<Loadable<File>> getInjectedTweaks()
	{
		Collection<Loadable<File>> tweaks = new ArrayList<Loadable<File>>();
		
		for (ModInfo<Loadable<?>> tweak : this.mods.getInjectedTweaks())
		{
			tweaks.add((Loadable<File>)tweak.getContainer());
		}
		
		return tweaks;
	}

	/**
	 * Get a reference to a loaded mod, if the mod exists
	 * 
	 * @param modName Mod's name, identifier or class name
	 * @return
	 * @throws InvalidActivityException
	 */
	public <T extends LiteMod> T getMod(String modName) throws InvalidActivityException, IllegalArgumentException
	{
		if (!this.modInitComplete)
		{
			throw new InvalidActivityException("Attempted to get a reference to a mod before loader startup is complete");
		}
		
		return this.mods.getMod(modName);
	}
	
	/**
	 * Get a reference to a loaded mod, if the mod exists
	 * 
	 * @param modName Mod's name or class name
	 * @return
	 * @throws InvalidActivityException
	 */
	public <T extends LiteMod> T getMod(Class<T> modClass)
	{
		if (!this.modInitComplete)
		{
			throw new RuntimeException("Attempted to get a reference to a mod before loader startup is complete");
		}
		
		return this.mods.getMod(modClass);
	}
	
	/**
	 * Get whether the specified mod is installed
	 *
	 * @param modName
	 * @return
	 */
	public boolean isModInstalled(String modName)
	{
		if (!this.modInitComplete || modName == null) return false;
		
		return this.mods.isModInstalled(modName);
	}

	/**
	 * Get a metadata value for the specified mod
	 * 
	 * @param modNameOrId
	 * @param metaDataKey
	 * @param defaultValue
	 * @return
	 * @throws InvalidActivityException Thrown by getMod if init is not complete 
	 * @throws IllegalArgumentException Thrown by getMod if argument is null
	 */
	public String getModMetaData(String modNameOrId, String metaDataKey, String defaultValue) throws InvalidActivityException, IllegalArgumentException
	{
		return this.mods.getModMetaData(modNameOrId, metaDataKey, defaultValue);
	}
	
	/**
	 * Get a metadata value for the specified mod
	 * 
	 * @param mod
	 * @param metaDataKey
	 * @param defaultValue
	 * @return
	 */
	public String getModMetaData(LiteMod mod, String metaDataKey, String defaultValue)
	{
		return this.mods.getModMetaData(mod, metaDataKey, defaultValue);
	}

	/**
	 * Get a metadata value for the specified mod
	 * 
	 * @param modClass
	 * @param metaDataKey
	 * @param defaultValue
	 * @return
	 */
	public String getModMetaData(Class<? extends LiteMod> modClass, String metaDataKey, String defaultValue)
	{
		return this.mods.getModMetaData(modClass, metaDataKey, defaultValue);
	}

	/**
	 * Get the mod identifier, this is used for versioning, exclusivity, and enablement checks
	 * 
	 * @param modClass
	 * @return
	 */
	public String getModIdentifier(Class<? extends LiteMod> modClass)
	{
		return this.mods.getModIdentifier(modClass);
	}
	
	/**
	 * Get the mod identifier, this is used for versioning, exclusivity, and enablement checks
	 * 
	 * @param modClass
	 * @return
	 */
	public String getModIdentifier(LiteMod mod)
	{
		return this.mods.getModIdentifier(mod);
	}
	
	/**
	 * Get the container (mod file, classpath jar or folder) for the specified mod
	 * 
	 * @param modClass
	 * @return
	 */
	public LoadableMod<?> getModContainer(Class<? extends LiteMod> modClass)
	{
		return this.mods.getModContainer(modClass);
	}
	
	/**
	 * Get the container (mod file, classpath jar or folder) for the specified mod
	 * 
	 * @param modClass
	 * @return
	 */
	public LoadableMod<?> getModContainer(LiteMod mod)
	{
		return this.mods.getModContainer(mod);
	}
	
	/**
	 * Get the mod which matches the specified identifier
	 * 
	 * @param identifier
	 * @return
	 */
	public Class<? extends LiteMod> getModFromIdentifier(String identifier)
	{
		return this.mods.getModFromIdentifier(identifier);
	}
	
	/**
	 * @param identifier Identifier of the mod to enable
	 */
	public void enableMod(String identifier)
	{
		this.mods.setModEnabled(identifier, true);
	}

	/**
	 * @param identifier Identifier of the mod to disable
	 */
	public void disableMod(String identifier)
	{
		this.mods.setModEnabled(identifier, false);
	}
	
	/**
	 * @param identifier Identifier of the mod to enable/disable
	 * @param enabled
	 */
	public void setModEnabled(String identifier, boolean enabled)
	{
		this.mods.setModEnabled(identifier, enabled);
	}

	/**
	 * @param modName
	 * @return
	 */
	public boolean isModEnabled(String modName)
	{
		return this.mods.isModEnabled(modName);
	}
	
	/**
	 * @param modName
	 * @return
	 */
	public boolean isModActive(String modName)
	{
		return this.mods.isModActive(modName);
	}
	
	/**
	 * @param exposable
	 */
	public void writeConfig(Exposable exposable)
	{
		this.configManager.invalidateConfig(exposable);
	}
	
	/**
	 * Register an arbitrary Exposable
	 * 
	 * @param exposable Exposable object to register
	 * @param fileName Override config file name to use (leave null to use value from ExposableConfig specified value)
	 */
	public void registerExposable(Exposable exposable, String fileName)
	{
		this.configManager.registerExposable(exposable, fileName, true);
		this.configManager.initConfig(exposable);
	}

	/**
	 * Initialise lifetime objects like the game engine, event broker and interface manager
	 */
	private void initLifetimeObjects()
	{
		// Cache game engine reference
		this.engine = this.objectFactory.getGameEngine();
		
		// Cache profiler instance
		this.profiler = this.objectFactory.getGameEngine().getProfiler();
		
		// Create the event broker
		this.events = this.objectFactory.getEventBroker();
		if (this.events != null)
		{
			this.events.setMods(this.mods);
		}

		// Get the mod panel manager
		this.modPanelManager = this.objectFactory.getModPanelManager();
		if (this.modPanelManager != null)
		{
			this.modPanelManager.init(this.mods, this.configManager);
		}
		
		// Create the interface manager
		this.interfaceManager = new LiteLoaderInterfaceManager(this.apiAdapter);
	}

	/**
	 * 
	 */
	private void postInitCoreProviders()
	{
		for (CoreProvider coreProvider : this.coreProviders)
		{
			coreProvider.onPostInit(this.engine);
		}

		this.interfaceManager.registerInterfaces();
	}

	private void loadAndInitMods()
	{
		int totalMods = this.enumerator.modsToLoadCount();
		LiteLoaderLogger.info("Discovered %d total mod(s)", totalMods);
		
		if (totalMods > 0)
		{
			this.mods.loadMods();
			this.mods.initMods();
		}
		else
		{
			LiteLoaderLogger.info("Mod class discovery failed or no mod classes were found. Not loading any mods.");
		}
		
		// Initialises the required hooks for loaded mods
		this.interfaceManager.onPostInit();
		
		this.modInitComplete = true;
		this.mods.onPostInit();
	}

	void onPostInitMod(LiteMod mod)
	{
		// add mod to permissions manager if permissible
		this.permissionsManagerClient.registerMod(mod);
	}

	/**
	 * Called after mod late init
	 */
	void onStartupComplete()
	{
		// Set the loader branding in ClientBrandRetriever using reflection
		LiteLoaderBootstrap.setBranding("LiteLoader");
		
		for (CoreProvider coreProvider : this.coreProviders)
		{
			coreProvider.onStartupComplete();
		}
	}

	/**
	 * Called on login
	 * 
	 * @param netHandler
	 * @param loginPacket
	 */
	void onJoinGame(INetHandler netHandler, S01PacketJoinGame loginPacket)
	{
		this.permissionsManagerClient.onJoinGame(netHandler, loginPacket);

		for (CoreProvider coreProvider : this.coreProviders)
		{
			coreProvider.onJoinGame(netHandler, loginPacket);
		}
	}
	
	/**
	 * Called when the world reference is changed
	 * 
	 * @param world
	 */
	void onWorldChanged(World world)
	{
		if (world != null)
		{
			// For bungeecord
			this.permissionsManagerClient.scheduleRefresh();
		}
		
		for (WorldObserver worldObserver : this.worldObservers)
		{
			worldObserver.onWorldChanged(world);
		}
	}
	
	/**
	 * @param mouseX
	 * @param mouseY
	 * @param partialTicks
	 */
	void onPostRender(int mouseX, int mouseY, float partialTicks)
	{
		this.profiler.startSection("core");
		
		for (PostRenderObserver postRenderObserver : this.postRenderObservers)
		{
			postRenderObserver.onPostRender(mouseX, mouseY, partialTicks);
		}
		
		this.profiler.endSection();
	}

	/**
	 * @param clock
	 * @param partialTicks
	 * @param inGame
	 */
	void onTick(boolean clock, float partialTicks, boolean inGame)
	{
		if (clock)
		{
			// Tick the permissions manager
			this.profiler.startSection("permissionsmanager");
			this.permissionsManagerClient.onTick(this.engine, partialTicks, inGame);
			
			// Tick the config manager
			this.profiler.endStartSection("configmanager");
			this.configManager.onTick();
			
			this.profiler.endSection();
			
			if (!this.engine.isRunning())
			{
				this.onShutDown();
				return;
			}
		}

		this.profiler.startSection("observers");
		
		for (TickObserver tickObserver : this.tickObservers)
		{
			tickObserver.onTick(clock, partialTicks, inGame);
		}
		
		this.profiler.endSection();
	}

	private void onShutDown()
	{
		LiteLoaderLogger.info("LiteLoader is shutting down, shutting down core providers and syncing configuration");
		
		for (ShutdownObserver lifeCycleObserver : this.shutdownObservers)
		{
			lifeCycleObserver.onShutDown();
		}

		this.configManager.syncConfig();
	}
	
	/**
	 * Get whether the "mod info" screen tab is shown in the main menu
	 * 
	 * @deprecated use getModPanelManager().getDisplayModInfoScreenTab(); instead
	 */
	@Deprecated
	public boolean getDisplayModInfoScreenTab()
	{
		return (this.modPanelManager != null) ? this.modPanelManager.isTabVisible() : false;
	}

	/**
	 * Display the "mod info" overlay over the specified GUI
	 * 
	 * @param parentScreen
	 * 
	 * @deprecated use getModPanelManager().displayModInfoScreen(parentScreen); instead
	 */
	@SuppressWarnings("unchecked")
	@Deprecated
	public void displayModInfoScreen(Object parentScreen)
	{
		// Use implicit cast, because we want this to fail if the user tries to give us an invalid class
		if (this.modPanelManager != null) this.modPanelManager.displayLiteLoaderPanel(parentScreen);
	}

	/**
	 * @param objCrashReport This is an object so that we don't need to transform the obfuscated name in the transformer
	 */
	public static void populateCrashReport(Object objCrashReport)
	{
		if (objCrashReport instanceof CrashReport)
		{
			EventProxy.populateCrashReport((CrashReport)objCrashReport);
			LiteLoader.populateCrashReport((CrashReport)objCrashReport);
		}
	}

	private static void populateCrashReport(CrashReport crashReport)
	{
		CrashReportCategory category = crashReport.getCategory(); // crashReport.makeCategoryDepth("Mod System Details", 1);
		category.addCrashSectionCallable("Mod Pack",        new CallableLiteLoaderBrand(crashReport));
		category.addCrashSectionCallable("LiteLoader Mods", new CallableLiteLoaderMods(crashReport));
		category.addCrashSectionCallable("LaunchWrapper",   new CallableLaunchWrapper(crashReport));
	}

	static final void createInstance(LoaderEnvironment environment, LoaderProperties properties, LaunchClassLoader classLoader)
	{
		if (LiteLoader.instance == null)
		{
			LiteLoader.classLoader = classLoader;
			LiteLoader.instance = new LiteLoader(environment, properties);
		}
	}
	
	static final void invokeInit()
	{
		LiteLoaderLogger.info("LiteLoader begin INIT...");
		
		LiteLoader.instance.onInit();
	}

	static final void invokePostInit()
	{
		LiteLoaderLogger.info("LiteLoader begin POSTINIT...");

		LiteLoader.instance.onPostInit();
	}
}