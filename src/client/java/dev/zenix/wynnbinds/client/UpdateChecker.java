package dev.zenix.wynnbinds.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import dev.zenix.wynnbinds.Wynnbinds;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;

public class UpdateChecker {

    private static final String API_URL = "https://api.github.com/repos/OhhhZenix/Wynnbinds/releases/latest";
    private final ScheduledExecutorService scheduler;
    private final HttpClient httpClient;
    private final Gson gson;

    public UpdateChecker() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.gson = new Gson();
    }

    /* ============================= */
 /* Lifecycle */
 /* ============================= */
    public void start() {
        scheduler.scheduleAtFixedRate(this::checkForUpdates, 0, 1, TimeUnit.HOURS);
    }

    public void stop() {
        scheduler.shutdown();
    }

    /* ============================= */
 /* Update Logic */
 /* ============================= */
    private void checkForUpdates() {
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(API_URL)).header("Accept", "application/json")
                    .header("User-Agent", "Wynnbinds-UpdateChecker").timeout(Duration.ofSeconds(10)).GET().build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).orTimeout(15, TimeUnit.SECONDS)
                    .thenAccept(this::handleResponse).exceptionally(ex -> {
                Wynnbinds.LOGGER.debug("Update check failed: {}", ex.getMessage());
                return null;
            });
        } catch (Exception e) {
            Wynnbinds.LOGGER.debug("Failed to start update check", e);
        }
    }

    private void handleResponse(HttpResponse<String> response) {
        if (response.statusCode() != 200) {
            Wynnbinds.LOGGER.debug("GitHub API returned status {}", response.statusCode());
            return;
        }

        try {
            Map<?, ?> json = gson.fromJson(response.body(), Map.class);
            String latestVersion = (String) json.get("tag_name");

            if (latestVersion == null) {
                return;
            }

            String currentVersion = FabricLoader.getInstance().getModContainer(Wynnbinds.MOD_ID)
                    .map(mc -> mc.getMetadata().getVersion().getFriendlyString()).orElse("0.0.0");

            if (!isNewer(latestVersion, currentVersion)) {
                return;
            }

            notifyPlayer(latestVersion, currentVersion);
        } catch (JsonSyntaxException e) {
            Wynnbinds.LOGGER.debug("Failed parsing update response", e);
        }
    }

    private void notifyPlayer(String latest, String current) {
        String homepageUrl = FabricLoader.getInstance().getModContainer(Wynnbinds.MOD_ID)
                .flatMap(mc -> mc.getMetadata().getContact().get("homepage"))
                .orElse("https://github.com/OhhhZenix/Wynnbinds");

        Utils.sendNotification(Component.nullToEmpty("New update available: " + latest),
                WynnbindsClient.getInstance().getConfig().isUpdateNotificationsEnabled());

        Wynnbinds.LOGGER.info("{} v{} is available (current: v{}). Download: {}", Wynnbinds.MOD_NAME, latest, current,
                homepageUrl);
    }

    private boolean isNewer(String latest, String current) {
        return compareSemver(latest, current) > 0;
    }

    private int compareSemver(String v1, String v2) {
        boolean v1Pre = v1.contains("-");
        boolean v2Pre = v2.contains("-");

        v1 = normalizeVersion(v1);
        v2 = normalizeVersion(v2);

        String[] a = v1.split("\\.");
        String[] b = v2.split("\\.");

        int len = Math.max(a.length, b.length);

        for (int i = 0; i < len; i++) {
            int n1 = i < a.length ? parseSafe(a[i]) : 0;
            int n2 = i < b.length ? parseSafe(b[i]) : 0;

            if (n1 != n2) {
                return Integer.compare(n1, n2);
            }
        }

        // Stable > prerelease
        if (v1Pre != v2Pre) {
            return v1Pre ? -1 : 1;
        }

        return 0;
    }

    private String normalizeVersion(String version) {
        if (version.startsWith("v") || version.startsWith("V")) {
            version = version.substring(1);
        }

        int dashIndex = version.indexOf("-");
        if (dashIndex != -1) {
            version = version.substring(0, dashIndex);
        }

        return version;
    }

    private int parseSafe(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
