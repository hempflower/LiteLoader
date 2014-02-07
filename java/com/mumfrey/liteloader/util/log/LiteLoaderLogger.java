package com.mumfrey.liteloader.util.log;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;

/**
 * Gateway class for the log4j logger
 *
 * @author Adam Mummery-Smith
 */
public class LiteLoaderLogger extends AbstractAppender
{
	private static final int LOG_TAIL_SIZE = 500;

	private static Logger logger = (Logger)LogManager.getLogger("LiteLoader");
	
	private static LinkedList<String> logTail = new LinkedList<String>();
	
	private static long logIndex = 0;
	
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
			String message = event.getMessage().getFormattedMessage();
			while (message.indexOf('\n') > -1)
			{
				int LF = message.indexOf('\n');
				LiteLoaderLogger.logTail.add(message.substring(0, LF));
				if (LiteLoaderLogger.logTail.size() > 500) LiteLoaderLogger.logTail.remove();
				message = message.substring(LF + 1);
			}
			LiteLoaderLogger.logTail.add(message);
			if (LiteLoaderLogger.logTail.size() > LOG_TAIL_SIZE) LiteLoaderLogger.logTail.remove();
		}
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
	
	private static void log(Level level, String format, Object... data)
	{
		LiteLoaderLogger.logger.log(level, String.format(format, data));
	}
	
	private static void log(Level level, Throwable th, String format, Object... data)
	{
		LiteLoaderLogger.logger.log(level, String.format(format, data), th);
	}
	
	public static void severe(String format, Object... data)
	{
		LiteLoaderLogger.log(Level.ERROR, format, data);
	}
	
	public static void severe(Throwable th, String format, Object... data)
	{
		LiteLoaderLogger.log(Level.ERROR, th, format, data);
	}
	
	public static void warning(String format, Object... data)
	{
		LiteLoaderLogger.log(Level.WARN, format, data);
	}
	
	public static void warning(Throwable th, String format, Object... data)
	{
		LiteLoaderLogger.log(Level.WARN, th, format, data);
	}
	
	public static void info(String format, Object... data)
	{
		LiteLoaderLogger.log(Level.INFO, format, data);
	}
}