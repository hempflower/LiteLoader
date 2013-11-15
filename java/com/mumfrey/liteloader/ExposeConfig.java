package com.mumfrey.liteloader;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.mumfrey.liteloader.modconfig.ConfigStrategy;

/**
 * Annotation which can be a applied to mod classes to indicate they should be serialised with Gson
 *
 * @author Adam Mummery-Smith
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExposeConfig
{
	/**
	 * Configuration strategy to use
	 */
	ConfigStrategy strategy() default ConfigStrategy.Unversioned;
	
	/**
	 * Config file name, if not specified the mod class name is used
	 */
	String filename() default "";
}
