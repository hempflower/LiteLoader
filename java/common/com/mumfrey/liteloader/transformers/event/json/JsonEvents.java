package com.mumfrey.liteloader.transformers.event.json;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.mumfrey.liteloader.transformers.event.MethodInfo;

/**
 * Serialisable class which represents a set of event injection definitions
 * 
 * @author Adam Mummery-Smith
 */
public class JsonEvents implements Serializable
{
	private static final long serialVersionUID = 1L;

	private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	
	private static final Pattern tokenPattern = Pattern.compile("^\\$\\{([a-zA-Z0-9_\\-\\.\\$]+)\\}$");
	
	@SerializedName("obfuscation")
	private JsonObfuscationTable obfuscation;
	
	@SerializedName("descriptors")
	private List<JsonDescriptor> descriptors;
	
	@SerializedName("events")
	private List<JsonEvent> events;
	
	private transient Map<String, MethodInfo> methods = new HashMap<String, MethodInfo>();
	
	public void parse()
	{
		try
		{
			this.obfuscation.parse();
			
			if (this.descriptors != null)
			{
				for (JsonDescriptor descriptor : this.descriptors)
				{
					this.methods.put(descriptor.getKey(), descriptor.parse(this.obfuscation));
				}
			}
			
			for (JsonEvent event : this.events)
			{
				event.parse(this);
			}
		}
		catch (InvalidEventJsonException ex)
		{
			throw ex;
		}
		catch (Exception ex)
		{
			throw new InvalidEventJsonException("An error occurred whilst parsing the event definition: " + ex.getClass().getSimpleName() + ": " + ex.getMessage(), ex);
		}
	}
	
	public void register(ModEventInjectionTransformer transformer)
	{
		for (JsonEvent event : this.events)
		{
			event.register(transformer);
		}
	}

	public MethodInfo getMethod(String token)
	{
		String key = JsonEvents.parseToken(token);
		if (key == null)
		{
			throw new InvalidEventJsonException("\"" + token + "\" is not a valid token");
		}

		MethodInfo method = this.methods.get(key);
		if (method == null)
		{
			throw new InvalidEventJsonException("Could not locate method with token " + token);
		}
		return method;
	}

	public String toJson()
	{
		return JsonEvents.gson.toJson(this);
	}

	public static JsonEvents parse(String json)
	{
		JsonEvents newJsonEvents = JsonEvents.gson.fromJson(json, JsonEvents.class);
		newJsonEvents.parse();
		return newJsonEvents;
	}
	
	protected static String parseToken(String token)
	{
		token = token.replace(" ", "").trim();
		
		Matcher tokenPatternMatcher = JsonEvents.tokenPattern.matcher(token);
		if (tokenPatternMatcher.matches())
		{
			return tokenPatternMatcher.group(1);
		}
		
		return null;
	}
}
