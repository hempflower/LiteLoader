package com.mumfrey.liteloader.launch;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.NonOptionArgumentSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

/**
 *
 * @author Adam Mummery-Smith
 */
public class LiteLoaderTweaker implements ITweaker
{
	public static final String VERSION = "1.6.4";

	private File gameDirectory;
	
	private File assetsDirectory;
	
	private String profile;

	private List<String> singularLaunchArgs = new ArrayList<String>();
	
	private Map<String, String> launchArgs;
	
	private ArgumentAcceptingOptionSpec<String> modsOption;
	private OptionSet parsedOptions;
	
	private List<String> passThroughArgs;
	
	@SuppressWarnings("unchecked")
	@Override
	public void acceptOptions(List<String> args, File gameDirectory, File assetsDirectory, String profile)
	{
		this.gameDirectory = gameDirectory;
		this.assetsDirectory = assetsDirectory;
		this.profile = profile;
		
		LiteLoaderTransformer.gameDirectory = gameDirectory;
		LiteLoaderTransformer.assetsDirectory = assetsDirectory;
		LiteLoaderTransformer.profile = profile != null ? profile : VERSION;
		
		OptionParser optionParser = new OptionParser();
		this.modsOption = optionParser.accepts("mods", "Comma-separated list of mods to load").withRequiredArg().ofType(String.class).withValuesSeparatedBy(',');
		optionParser.allowsUnrecognizedOptions();
		NonOptionArgumentSpec<String> nonOptions = optionParser.nonOptions();
		
		this.parsedOptions = optionParser.parse(args.toArray(new String[args.size()]));
		this.passThroughArgs = this.parsedOptions.valuesOf(nonOptions);
		
		this.launchArgs = (Map<String, String>)Launch.blackboard.get("launchArgs");
		if (this.launchArgs == null)
		{
			this.launchArgs = new HashMap<String, String>();			
			Launch.blackboard.put("launchArgs", this.launchArgs);
		}
		
		// Parse out the arguments ourself because joptsimple doesn't really provide a good way to
		// add arguments to the unparsed argument list after parsing
		this.parseArgs(this.passThroughArgs);
		
		// Put required arguments to the blackboard if they don't already exist there
		this.provideRequiredArgs(gameDirectory, assetsDirectory);
		
		if (this.parsedOptions.has(this.modsOption))
		{
			LiteLoaderTransformer.modsToLoad = this.modsOption.values(this.parsedOptions);
		}
	}

	/**
	 * @param gameDirectory
	 * @param assetsDirectory
	 */
	public void provideRequiredArgs(File gameDirectory, File assetsDirectory)
	{
		if (!this.launchArgs.containsKey("--version"))
			this.addClassifiedArg("--version", this.VERSION);
		
		if (!this.launchArgs.containsKey("--gameDir") && gameDirectory != null)
			this.addClassifiedArg("--gameDir", gameDirectory.getAbsolutePath());
		
		if (!this.launchArgs.containsKey("--assetsDir") && assetsDirectory != null)
			this.addClassifiedArg("--assetsDir", assetsDirectory.getAbsolutePath());
	}

	private void parseArgs(List<String> args)
	{
		String classifier = null;
		
		for (String arg : args)
		{
			if (arg.startsWith("-"))
			{
				if (classifier != null)
					classifier = this.addClassifiedArg(classifier, "");
				else if (arg.contains("="))
					classifier = this.addClassifiedArg(arg.substring(0, arg.indexOf('=')), arg.substring(arg.indexOf('=') + 1));
				else
					classifier = arg;
			}
			else
			{
				if (classifier != null)
					classifier = this.addClassifiedArg(classifier, arg);
				else
					this.singularLaunchArgs.add(arg);
			}
		}
	}

	private String addClassifiedArg(String classifiedArg, String arg)
	{
		this.launchArgs.put(classifiedArg, arg);
		return null;
	}

	@Override
	public void injectIntoClassLoader(LaunchClassLoader classLoader)
	{
		LiteLoaderTransformer.launchClassLoader = classLoader;
		classLoader.registerTransformer(LiteLoaderTransformer.class.getName());
	}

	@Override
	public String getLaunchTarget()
	{
		return "net.minecraft.client.main.Main";
	}

	@Override
	public String[] getLaunchArguments()
	{
		List<String> args = new ArrayList<String>();
		
		for (String unClassifiedArg : this.singularLaunchArgs)
			args.add(unClassifiedArg);
		
		for (Entry<String, String> launchArg : this.launchArgs.entrySet())
		{
			args.add(launchArg.getKey().trim());
			args.add(launchArg.getValue().trim());
		}
		
		this.singularLaunchArgs.clear();
		this.launchArgs.clear();
		
		return args.toArray(new String[args.size()]);
	}

	public File getGameDirectory()
	{
		return this.gameDirectory;
	}

	public File getAssetsDirectory()
	{
		return this.assetsDirectory;
	}
	
	public String getProfile()
	{
		return this.profile;
	}
}
