package net.runelite.client.plugins.bomgetracker;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.*;

/**
 * Sends trade events to the BomGE server via HTTP POST.
 * Uses a single-threaded executor to preserve trade order.
 * Failed sends are queued for retry; if the queue overflows,
 * events are written to the fallback file logger.
 */
@Slf4j
public class TradeEventSender
{
	private static final int MAX_RETRY_QUEUE = 200;
	private static final int RETRY_INTERVAL_SECONDS = 30;
	private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
	private static final Gson GSON = new Gson();

	private final OkHttpClient httpClient;
	private final ScheduledExecutorService executor;
	private final ConcurrentLinkedQueue<TradeEvent> retryQueue;
	private final FallbackFileLogger fallbackLogger;

	private volatile String serverUrl;
	private volatile String apiKey;
	private volatile boolean alsoLogToFile;
	private final ScheduledFuture<?> retryTask;

	public TradeEventSender(FallbackFileLogger fallbackLogger)
	{
		this.httpClient = new OkHttpClient.Builder()
			.connectTimeout(5, TimeUnit.SECONDS)
			.writeTimeout(5, TimeUnit.SECONDS)
			.readTimeout(10, TimeUnit.SECONDS)
			.build();

		this.executor = Executors.newSingleThreadScheduledExecutor(r ->
		{
			Thread t = new Thread(r, "BomGE-Sender");
			t.setDaemon(true);
			return t;
		});

		this.retryQueue = new ConcurrentLinkedQueue<>();
		this.fallbackLogger = fallbackLogger;

		// Schedule retry task
		this.retryTask = executor.scheduleWithFixedDelay(
			this::processRetryQueue,
			RETRY_INTERVAL_SECONDS,
			RETRY_INTERVAL_SECONDS,
			TimeUnit.SECONDS
		);
	}

	public void configure(String serverUrl, String apiKey, boolean alsoLogToFile)
	{
		this.serverUrl = serverUrl;
		this.apiKey = apiKey;
		this.alsoLogToFile = alsoLogToFile;
	}

	/**
	 * Submit a trade event for async sending. Non-blocking.
	 */
	public void send(TradeEvent event)
	{
		// Always log to file if configured (backup)
		if (alsoLogToFile)
		{
			fallbackLogger.log(event);
		}

		executor.submit(() -> doSend(event));
	}

	private void doSend(TradeEvent event)
	{
		if (serverUrl == null || serverUrl.isEmpty())
		{
			log.debug("No server URL configured, skipping HTTP send");
			return;
		}

		String json = GSON.toJson(event);
		String url = serverUrl.endsWith("/") ? serverUrl + "api/trades" : serverUrl + "/api/trades";

		Request.Builder reqBuilder = new Request.Builder()
			.url(url)
			.post(RequestBody.create(JSON, json))
			.header("Content-Type", "application/json");

		if (apiKey != null && !apiKey.isEmpty())
		{
			reqBuilder.header("X-API-Key", apiKey);
		}

		try (Response response = httpClient.newCall(reqBuilder.build()).execute())
		{
			if (response.isSuccessful())
			{
				log.debug("Trade sent successfully: {} {} x{}", event.getState(), event.getItemId(), event.getQty());
			}
			else
			{
				log.warn("Server returned {}: {}", response.code(), response.body() != null ? response.body().string() : "");
				enqueueForRetry(event);
			}
		}
		catch (IOException e)
		{
			log.warn("Failed to send trade (will retry): {}", e.getMessage());
			enqueueForRetry(event);
		}
	}

	private void enqueueForRetry(TradeEvent event)
	{
		if (retryQueue.size() >= MAX_RETRY_QUEUE)
		{
			// Queue full — flush oldest to file
			TradeEvent dropped = retryQueue.poll();
			if (dropped != null && !alsoLogToFile)
			{
				fallbackLogger.log(dropped);
			}
		}
		retryQueue.add(event);
	}

	private void processRetryQueue()
	{
		if (retryQueue.isEmpty())
		{
			return;
		}

		log.debug("Retrying {} queued trades", retryQueue.size());

		// Try to send each queued event; stop on first failure
		while (!retryQueue.isEmpty())
		{
			TradeEvent event = retryQueue.peek();
			if (event == null)
			{
				break;
			}

			String json = GSON.toJson(event);
			String url = serverUrl != null
				? (serverUrl.endsWith("/") ? serverUrl + "api/trades" : serverUrl + "/api/trades")
				: null;

			if (url == null)
			{
				break;
			}

			Request.Builder reqBuilder = new Request.Builder()
				.url(url)
				.post(RequestBody.create(JSON, json))
				.header("Content-Type", "application/json");

			if (apiKey != null && !apiKey.isEmpty())
			{
				reqBuilder.header("X-API-Key", apiKey);
			}

			try (Response response = httpClient.newCall(reqBuilder.build()).execute())
			{
				if (response.isSuccessful())
				{
					retryQueue.poll(); // Remove successfully sent item
					log.debug("Retry successful for item {}", event.getItemId());
				}
				else
				{
					// Server is responding but rejecting — stop retrying this batch
					log.warn("Retry got {}, will try again later", response.code());
					break;
				}
			}
			catch (IOException e)
			{
				// Server unreachable — stop retrying
				log.debug("Retry failed (server unreachable), will try again in {}s", RETRY_INTERVAL_SECONDS);
				break;
			}
		}
	}

	/**
	 * Get the HTTP client for use by other components
	 */
	public OkHttpClient getHttpClient()
	{
		return httpClient;
	}

	/**
	 * Flush remaining retry queue to file and shut down the executor.
	 */
	public void shutdown()
	{
		if (retryTask != null)
		{
			retryTask.cancel(false);
		}

		// Flush remaining queue to file
		TradeEvent event;
		while ((event = retryQueue.poll()) != null)
		{
			if (!alsoLogToFile)
			{
				fallbackLogger.log(event);
			}
		}

		executor.shutdown();
		try
		{
			if (!executor.awaitTermination(5, TimeUnit.SECONDS))
			{
				executor.shutdownNow();
			}
		}
		catch (InterruptedException e)
		{
			executor.shutdownNow();
			Thread.currentThread().interrupt();
		}

		httpClient.dispatcher().executorService().shutdown();
		httpClient.connectionPool().evictAll();
	}
}
