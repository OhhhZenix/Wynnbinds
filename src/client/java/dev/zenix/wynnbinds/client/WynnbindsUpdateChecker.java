package dev.zenix.wynnbinds.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;

import com.google.gson.Gson;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class WynnbindsUpdateChecker implements Runnable {

    // in milliseconds
    private long lastCheckTime;

    // in milliseconds
    private long checkInterval;

    public WynnbindsUpdateChecker() {
        lastCheckTime = System.currentTimeMillis();
        checkInterval = 3_600_000; // 1 hour in milliseconds
    }

    @Override
    public void run() {
        if (!shouldCheckForUpdates())
            return;

        // update timer
        lastCheckTime = System.currentTimeMillis();

        // try to fetch latest version from GitHub API
        try {
            final String API_URL =
                    "https://api.github.com/repos/OhhhZenix/Wynnbinds/releases/latest";
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest httpRequest = HttpRequest.newBuilder().uri(URI.create(API_URL))
                    .header("Accept", "application/json").build();
            HttpResponse<String> httpResponse =
                    httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (httpResponse.statusCode() != 200) {
                System.out.println("Failed to fetch. Status code: " + httpResponse.statusCode());
                return;
            }

            String body = httpResponse.body();
            Gson gson = new Gson();
            var json = gson.fromJson(body, HashMap.class);
            String latestVersion = (String) json.get("tag_name");
            String currentVersion = FabricLoader.getInstance()
                    .getModContainer(WynnbindsClient.MOD_ID).map(modContainer -> modContainer
                            .getMetadata().getVersion().getFriendlyString())
                    .orElse("0.0.0");
            String homepageUrl = FabricLoader.getInstance().getModContainer(WynnbindsClient.MOD_ID)
                    .flatMap(
                            modContainer -> modContainer.getMetadata().getContact().get("homepage"))
                    .orElse("https://github.com/OhhhZenix/Wynnbinds");

            if (WynnbindsUtils.compareSemver(latestVersion, currentVersion) > 0)
                MinecraftClient.getInstance().player.sendMessage(Text.of(String.format(
                        "Wynnbinds v%s is now available. You're running v%s. Visit %s to download.",
                        latestVersion, currentVersion, homepageUrl)), false);
        } catch (Exception e) {
            WynnbindsClient.LOGGER.warn("Failed to check for updates", e);
        }
    }

    private boolean shouldCheckForUpdates() {
        return System.currentTimeMillis() - lastCheckTime > checkInterval;
    }
}
