package com.mumfrey.liteloader.util.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class LiteLoaderLogFormatter extends Formatter
{
	private static SimpleDateFormat simpleDateFormatLogFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private boolean prepend = false;
	
	public LiteLoaderLogFormatter(boolean prependDateAndTime)
	{
		this.prepend = prependDateAndTime || System.getProperty("liteloaderFormatLog") != null;
	}

	@Override
	public String format(LogRecord logRecord)
	{
		StringBuilder sb = new StringBuilder();
		if (this.prepend)
		{
			sb.append(this.simpleDateFormatLogFormatter.format(Long.valueOf(logRecord.getMillis())));
			Level level = logRecord.getLevel();
			if (level == Level.SEVERE)
				sb.append(" [").append(level.getLocalizedName()).append("] ");
			else
				sb.append(" [").append(level.toString().toUpperCase()).append("] ");
		}
		else
		{
			sb.append("LiteLoader> ");
		}
		
		sb.append(logRecord.getMessage());
		sb.append('\n');
		Throwable th = logRecord.getThrown();
		if (th != null)
		{
			StringWriter stringWriter = new StringWriter();
			th.printStackTrace(new PrintWriter(stringWriter));
			sb.append(stringWriter.toString());
		}
		
		return sb.toString();
	}
}