package com.mumfrey.liteloader.transformers.event.json;

import java.io.Serializable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

/**
 * Serialisable class which represents a set of event injection definitions. Instances of this class are
 * created by deserialising with JSON. The JSON string should be passed to the static {@link #parse} method
 * which returns an instance of the class.
 * 
 * After parsing, the events defined here can be injected into an event transformer instance by calling the
 * {@link #register} method
 * 
 * @author Adam Mummery-Smith
 */
public class JsonEvents implements Serializable
{
	private static final long serialVersionUID = 1L;

	private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	
	/**
	 * Tokens are an instruction to the parser to look up a value rather than using a literal
	 */
	private static final Pattern tokenPattern = Pattern.compile("^\\$\\{([a-zA-Z0-9_\\-\\.\\$]+)\\}$");
	
	/**
	 * Serialised obfusctation entries
	 */
	@SerializedName("obfuscation")
	private JsonObfuscationTable obfuscation;
	
	/**
	 * Serialised method descriptors
	 */
	@SerializedName("descriptors")
	private List<JsonDescriptor> descriptors;
	
	/**
	 * Serialised events
	 */
	@SerializedName("events")
	private List<JsonEvent> events;
	
	/**
	 * Parsed method descriptors 
	 */
	private transient JsonMethods methods;
	
	/**
	 * Attempts to parse the information in this object
	 */
	private void parse()
	{
		if (this.events == null || this.events.isEmpty())
		{
			throw new InvalidEventJsonException("No events were defined in the supplied JSON");
		}
		
		try
		{
			// Parse the obfuscation table
			this.obfuscation.parse();
			
			// Parse the descriptor list
			this.methods = new JsonMethods(this.obfuscation, this.descriptors);
			
			// Parse the events
			for (JsonEvent event : this.events)
			{
				event.parse(this.methods);
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
	
	/**
	 * Parse a token name, returns the token name as a string if the token is valid, or null if the token is not valid
	 * 
	 * @param token
	 * @return
	 */
	static String parseToken(String token)
	{
		token = token.replace(" ", "").trim();
		
		Matcher tokenPatternMatcher = JsonEvents.tokenPattern.matcher(token);
		if (tokenPatternMatcher.matches())
		{
			return tokenPatternMatcher.group(1);
		}
		
		return null;
	}

	/**
	 * Called to register all events defined in this object into the specified transformer
	 * 
	 * @param transformer
	 */
	public void register(ModEventInjectionTransformer transformer)
	{
		for (JsonEvent event : this.events)
		{
			event.register(transformer);
		}
	}

//	public String toJson()
//	{
//		return JsonEvents.gson.toJson(this);
//	}

	/**
	 * Parse a new JsonEvents object from the supplied JSON string
	 * 
	 * @param json
	 * @return new JsonEvents instance
	 * @throws InvalidEventJsonException if the JSON ins invalid
	 */
	public static JsonEvents parse(String json) throws InvalidEventJsonException
	{
		try
		{
			JsonEvents newJsonEvents = JsonEvents.gson.fromJson(json, JsonEvents.class);
			newJsonEvents.parse();
			return newJsonEvents;
		}
		catch (InvalidEventJsonException ex)
		{
			throw ex;
		}
		catch (Throwable th)
		{
			throw new InvalidEventJsonException("An error occurred whilst parsing the event definition: " + th.getClass().getSimpleName() + ": " + th.getMessage(), th);
		}
	}
}
