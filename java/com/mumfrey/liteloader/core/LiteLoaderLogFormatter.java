package com.mumfrey.liteloader.core;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

final class LiteLoaderLogFormatter extends Formatter
{
	private SimpleDateFormat simpleDateFormatLogFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	@Override
	public String format(LogRecord logRecord)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(this.simpleDateFormatLogFormatter.format(Long.valueOf(logRecord.getMillis())));
		Level var3 = logRecord.getLevel();
		
		if (var3 == Level.SEVERE)
			sb.append(" [").append(var3.getLocalizedName()).append("] ");
		else
			sb.append(" [").append(var3.toString().toUpperCase()).append("] ");
		
		sb.append(logRecord.getMessage());
		sb.append('\n');
		Throwable th = logRecord.getThrown();
		
		if (th != null)
		{
			StringWriter var5 = new StringWriter();
			th.printStackTrace(new PrintWriter(var5));
			sb.append(var5.toString());
		}
		
		return sb.toString();
	}
}
