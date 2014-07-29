package com.mumfrey.liteloader.util.log;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.MissingFormatArgumentException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.helpers.Booleans;

/**
 * Gateway class for the log4j logger
 *
 * @author Adam Mummery-Smith
 */
public class LiteLoaderLogger extends AbstractAppender
{
	public static final boolean DEBUG = Booleans.parseBoolean(System.getProperty("liteloader.debug"), false);
	
	private static final int LOG_TAIL_SIZE = 500;

	private static Logger logger = (Logger)LogManager.getLogger("LiteLoader");
	
	private static LinkedList<String> logTail = new LinkedList<String>();
	
	private static long logIndex = 0;
	
	private static Throwable lastThrowable;
	
	static
	{
		LiteLoaderLogger.logger.addAppender(new LiteLoaderLogger());
	}
	
	protected LiteLoaderLogger()
	{
		super("Internal Log Appender", null, null);
		this.start();
	}
	
	@Override
	public void append(LogEvent event)
	{
		synchronized (LiteLoaderLogger.logTail)
		{
			LiteLoaderLogger.logIndex++;
			this.append(event.getMessage().getFormattedMessage());
			Throwable thrown = event.getThrown();
			if (thrown != null)
			{
				this.append(String.format("\2474%s: \2476%s", thrown.getClass().getSimpleName(), thrown.getMessage()));
			}
		}
	}

	/**
	 * @param message
	 */
	private void append(String message)
	{
		while (message.indexOf('\n') > -1)
		{
			int LF = message.indexOf('\n');
			this.appendLine(message.substring(0, LF));
			message = message.substring(LF + 1);
		}
		
		this.appendLine(message);
	}

	/**
	 * @param line
	 */
	private void appendLine(String line)
	{
		LiteLoaderLogger.logTail.add(line);
		
		if (LiteLoaderLogger.logTail.size() > LiteLoaderLogger.LOG_TAIL_SIZE)
			LiteLoaderLogger.logTail.remove();
	}
	
	public static long getLogIndex()
	{
		return LiteLoaderLogger.logIndex;
	}
	
	public static List<String> getLogTail()
	{
		List<String> log = new ArrayList<String>();
		
		synchronized (LiteLoaderLogger.logTail)
		{
			log.addAll(LiteLoaderLogger.logTail);
		}
		
		return log;
	}
	
	public static Logger getLogger()
	{
		return LiteLoaderLogger.logger;
	}
	
	public static void clearLastThrowable()
	{
		LiteLoaderLogger.lastThrowable = null;
	}
	
	public static Throwable getLastThrowable()
	{
		Throwable lastThrowableWrapped = null;
		
		// Wrap the throwable to avoid loader constraint violations during PREINIT and INIT
		if (LiteLoaderLogger.lastThrowable != null)
		{
			StringWriter sw = new StringWriter();
			LiteLoaderLogger.lastThrowable.printStackTrace(new PrintWriter(sw));
			lastThrowableWrapped = new Throwable(sw.toString());
			try { sw.close(); } catch (IOException ex) {}
		}
		
		return lastThrowableWrapped;
	}
	
	private static void log(Level level, String format, Object... data)
	{
		try
		{
			LiteLoaderLogger.logger.log(level, String.format(format, data));
		}
		catch (MissingFormatArgumentException ex)
		{
			LiteLoaderLogger.logger.log(level, format.replace('%', '@'));
		}
	}
	
	private static void log(Level level, Throwable th, String format, Object... data)
	{
		LiteLoaderLogger.lastThrowable = th;
		
		try
		{
			LiteLoaderLogger.logger.log(level, String.format(format, data), th);
		}
		catch (LinkageError ex) // This happens because of ClassLoader scope derpiness during the PREINIT and INIT phases
		{
			th.printStackTrace();
		}
		catch (Throwable th2)
		{
			th2.initCause(th);
			th2.printStackTrace();
		}
	}
	
	public static void severe(String format, Object... data)
	{
		LiteLoaderLogger.log(Level.ERROR, format, data);
	}
	
	public static void severe(Throwable th, String format, Object... data)
	{
		LiteLoaderLogger.lastThrowable = th;
		
		try
		{
			LiteLoaderLogger.log(Level.ERROR, th, format, data);
		}
		catch (LinkageError ex) // This happens because of ClassLoader scope derpiness during the PREINIT and INIT phases
		{
			th.printStackTrace();
		}
		catch (Throwable th2)
		{
			th2.initCause(th);
			th2.printStackTrace();
		}
	}
	
	public static void warning(String format, Object... data)
	{
		LiteLoaderLogger.log(Level.WARN, format, data);
	}
	
	public static void warning(Throwable th, String format, Object... data)
	{
		LiteLoaderLogger.lastThrowable = th;
		
		try
		{
			LiteLoaderLogger.log(Level.WARN, th, format, data);
		}
		catch (LinkageError ex) // This happens because of ClassLoader scope derpiness during the PREINIT and INIT phases
		{
			th.printStackTrace();
		}
		catch (Throwable th2)
		{
			th2.initCause(th);
			th2.printStackTrace();
		}
	}
	
	public static void info(String format, Object... data)
	{
		LiteLoaderLogger.log(Level.INFO, format, data);
	}
	
	public static void debug(String format, Object... data)
	{
		if (LiteLoaderLogger.DEBUG)
		{
			System.err.print("[DEBUG] ");
			System.err.println(String.format(format, data));
		}
	}

	public static void debug(Throwable th, String format, Object... data)
	{
		if (LiteLoaderLogger.DEBUG)
		{
			th.printStackTrace();
			LiteLoaderLogger.debug(format, data);
		}
	}
	
	public static void debug(Throwable th)
	{
		if (LiteLoaderLogger.DEBUG)
		{
			th.printStackTrace();
		}
	}
}