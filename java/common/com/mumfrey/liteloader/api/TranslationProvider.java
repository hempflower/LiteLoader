package com.mumfrey.liteloader.api;

/**
 * Interface for providers which can handle translation requests
 *
 * @author Adam Mummery-Smith
 */
public interface TranslationProvider extends CustomisationProvider
{
	/**
	 * Translate the supplied key or return NULL if the provider has no translation for the specified key
	 */
	public abstract String translate(String key, Object... args);
}
