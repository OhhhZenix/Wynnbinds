package dev.zenix.wynnbinds.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.gson.Gson;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;

public class WynnbindsUpdateChecker implements Runnable {

    private final AtomicBoolean running;
    private final long checkInterval;
    private final HttpClient httpClient;
    private final Gson gson = new Gson();

    private String lastNotifiedVersion = null;

    public WynnbindsUpdateChecker(AtomicBoolean running) {
        this.running = running;
        this.checkInterval = 3_600_000; // 1 hour
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public void run() {
        while (running.get()) {
            try {
                final String API_URL =
                        "https://api.github.com/repos/OhhhZenix/Wynnbinds/releases/latest";

                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(API_URL))
                        .header("Accept", "application/json").build();

                HttpResponse<String> response =
                        httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    WynnbindsClient.LOGGER.warn("Failed to fetch update info. Status code: {}",
                            response.statusCode());
                    continue;
                }

                Map<?, ?> json = gson.fromJson(response.body(), Map.class);
                String latestVersion = (String) json.get("tag_name");

                if (latestVersion == null) {
                    continue;
                }

                String currentVersion = FabricLoader.getInstance()
                        .getModContainer(WynnbindsClient.MOD_ID).map(modContainer -> modContainer
                                .getMetadata().getVersion().getFriendlyString())
                        .orElse("0.0.0");

                String homepageUrl =
                        FabricLoader.getInstance().getModContainer(WynnbindsClient.MOD_ID)
                                .flatMap(modContainer -> modContainer.getMetadata().getContact()
                                        .get("homepage"))
                                .orElse("https://github.com/OhhhZenix/Wynnbinds");

                // Normalize versions before compare
                if (compareSemver(latestVersion, currentVersion) > 0
                        && !latestVersion.equals(lastNotifiedVersion)) {

                    lastNotifiedVersion = latestVersion;

                    WynnbindsUtils.sendNotification(
                            Text.of("New update available: " + latestVersion), WynnbindsClient
                                    .getInstance().getConfig().isUpdateNotificationsEnabled());

                    WynnbindsClient.LOGGER.info(
                            "{} v{} is now available. You're running v{}. Visit {} to download.",
                            WynnbindsClient.MOD_NAME, latestVersion, currentVersion, homepageUrl);
                }

            } catch (Exception e) {
                WynnbindsClient.LOGGER.warn("Failed to check for updates", e);
            }

            try {
                Thread.sleep(checkInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Compares semantic versions safely.
     * 
     * Supports: v1.2.3, 1.2.3-beta, 1.2
     */
    private int compareSemver(String v1, String v2) {
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
        return 0;
    }

    private String normalizeVersion(String version) {
        // remove leading 'v'
        if (version.startsWith("v") || version.startsWith("V")) {
            version = version.substring(1);
        }

        // remove pre-release suffix
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
