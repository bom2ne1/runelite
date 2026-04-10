package net.runelite.client.plugins.bomgetracker;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Writes trade events to the exchange-logger directory as JSON lines.
 * Same format as the exchange-logger plugin so the Node.js file watcher
 * can still read them — provides a zero-downtime migration path.
 */
@Slf4j
public class FallbackFileLogger
{
	private static final Gson GSON = new Gson();
	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	private final Path logDir;

	public FallbackFileLogger()
	{
		this.logDir = Paths.get(System.getProperty("user.home"), ".runelite", "exchange-logger");
	}

	public void log(TradeEvent event)
	{
		try
		{
			Files.createDirectories(logDir);

			String filename = LocalDate.now().format(DATE_FMT) + ".log";
			Path logFile = logDir.resolve(filename);

			String json = GSON.toJson(event);

			try (BufferedWriter writer = Files.newBufferedWriter(logFile,
				StandardOpenOption.CREATE, StandardOpenOption.APPEND))
			{
				writer.write(json);
				writer.newLine();
			}
		}
		catch (IOException e)
		{
			log.error("Failed to write fallback log", e);
		}
	}
}
