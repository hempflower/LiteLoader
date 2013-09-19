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
	public static final String VERSION = "1.6.3";

	private File gameDirectory;
	
	private File assetsDirectory;
	
	private String profile;

	private List<String> unClassifiedArgs = new ArrayList<String>();
	
	private Map<String, String> classifiedArgs = new HashMap<String, String>();
	
	private ArgumentAcceptingOptionSpec<String> modsOption;
	private OptionSet parsedOptions;
	
	private List<String> passThroughArgs;
	
	@Override
	public void acceptOptions(List<String> args, File gameDirectory, File assetsDirectory, String profile)
	{
		this.gameDirectory = gameDirectory;
		this.assetsDirectory = assetsDirectory;
		this.profile = profile;
		
		LiteLoaderTransformer.gameDirectory = gameDirectory;
		LiteLoaderTransformer.assetsDirectory = assetsDirectory;
		LiteLoaderTransformer.profile = profile;
		
		OptionParser optionParser = new OptionParser();
		this.modsOption = optionParser.accepts("mods", "Comma-separated list of mods to load").withRequiredArg().ofType(String.class).withValuesSeparatedBy(',');
		optionParser.allowsUnrecognizedOptions();
		NonOptionArgumentSpec<String> nonOptions = optionParser.nonOptions();
		
		this.parsedOptions = optionParser.parse(args.toArray(new String[args.size()]));
		this.passThroughArgs = this.parsedOptions.valuesOf(nonOptions);
		
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
		@SuppressWarnings("unchecked")
		final List<String> argumentList = (List<String>)Launch.blackboard.get("ArgumentList");
		
		if (argumentList != null)
		{
			if (!argumentList.contains("--version"))
			{
				argumentList.add("--version");
				argumentList.add(this.VERSION);
			}
			
			if (!argumentList.contains("--gameDir") && gameDirectory != null)
			{
				argumentList.add("--gameDir");
				argumentList.add(gameDirectory.getAbsolutePath());
			}
			
			if (!argumentList.contains("--assetsDir") && assetsDirectory != null)
			{
				argumentList.add("--assetsDir");
				argumentList.add(assetsDirectory.getAbsolutePath());
			}
		}
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
					this.unClassifiedArgs.add(arg);
			}
		}
	}

	private String addClassifiedArg(String classifiedArg, String arg)
	{
		this.classifiedArgs.put(classifiedArg, arg);
		return null;
	}

	@Override
	public void injectIntoClassLoader(LaunchClassLoader classLoader)
	{
		LiteLoaderTransformer.launchClassLoader = classLoader;
		classLoader.registerTransformer("com.mumfrey.liteloader.launch.LiteLoaderTransformer");
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
		
		for (String unClassifiedArg : this.unClassifiedArgs)
			args.add(unClassifiedArg);
		
		for (Entry<String, String> classifiedArg : this.classifiedArgs.entrySet())
		{
			args.add(classifiedArg.getKey().trim());
			args.add(classifiedArg.getValue().trim());
		}
		
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
