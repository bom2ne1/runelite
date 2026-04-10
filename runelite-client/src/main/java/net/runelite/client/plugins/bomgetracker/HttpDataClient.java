package net.runelite.client.plugins.bomgetracker;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.bomgetracker.model.AlchData;
import net.runelite.client.plugins.bomgetracker.model.FlipData;
import net.runelite.client.plugins.bomgetracker.model.FlipStats;
import net.runelite.client.plugins.bomgetracker.model.TradeData;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
public class HttpDataClient {
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final String serverUrl;

    public HttpDataClient(OkHttpClient httpClient, String serverUrl) {
        this.httpClient = httpClient;
        this.gson = new Gson();
        this.serverUrl = serverUrl;
    }

    /**
     * Fetch recent trades from /api/trades?limit=100
     * Returns empty list if server offline or error
     */
    public List<TradeData> fetchTrades(int limit) {
        String url = serverUrl + "/api/trades?limit=" + limit;
        Request request = new Request.Builder().url(url).get().build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String json = response.body().string();
                TradeData[] trades = gson.fromJson(json, TradeData[].class);
                return Arrays.asList(trades);
            }
        } catch (Exception e) {
            log.debug("Failed to fetch trades: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * Fetch flip history from /api/flips/history?limit=50&days=7&status=completed
     */
    public List<FlipData> fetchFlips(int limit, int days, String status) {
        String url = String.format("%s/api/flips/history?limit=%d&days=%d&status=%s",
            serverUrl, limit, days, status);
        Request request = new Request.Builder().url(url).get().build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String json = response.body().string();
                FlipData[] flips = gson.fromJson(json, FlipData[].class);
                return Arrays.asList(flips);
            }
        } catch (Exception e) {
            log.debug("Failed to fetch flips: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * Fetch aggregate stats from /api/flips/stats?days=7
     */
    @Nullable
    public FlipStats fetchStats(int days) {
        String url = String.format("%s/api/flips/stats?days=%d", serverUrl, days);
        Request request = new Request.Builder().url(url).get().build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String json = response.body().string();
                return gson.fromJson(json, FlipStats.class);
            }
        } catch (Exception e) {
            log.debug("Failed to fetch stats: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Fetch alch profit data from /api/alch-profits
     */
    public List<AlchData> fetchAlchProfits() {
        String url = serverUrl + "/api/alch-profits";
        log.info("[HttpDataClient] Fetching alch profits from: {}", url);
        Request request = new Request.Builder().url(url).get().build();

        try (Response response = httpClient.newCall(request).execute()) {
            log.info("[HttpDataClient] Response code: {}", response.code());
            if (response.isSuccessful() && response.body() != null) {
                String json = response.body().string();
                log.info("[HttpDataClient] Received {} bytes of JSON", json.length());
                AlchData[] alchData = gson.fromJson(json, AlchData[].class);
                log.info("[HttpDataClient] Parsed {} alch items", alchData != null ? alchData.length : 0);
                return Arrays.asList(alchData);
            } else {
                log.warn("[HttpDataClient] Non-successful response or null body");
            }
        } catch (Exception e) {
            log.error("[HttpDataClient] Failed to fetch alch profits", e);
        }
        return Collections.emptyList();
    }

    /**
     * Check server connectivity
     */
    public boolean isServerOnline() {
        String url = serverUrl + "/api/trades/status";
        Request request = new Request.Builder()
            .url(url)
            .get()
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            return response.isSuccessful();
        } catch (Exception e) {
            return false;
        }
    }
}
