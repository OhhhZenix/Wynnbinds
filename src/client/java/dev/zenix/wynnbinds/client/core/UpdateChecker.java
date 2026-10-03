package dev.zenix.wynnbinds.client.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import dev.zenix.wynnbinds.Wynnbinds;
import dev.zenix.wynnbinds.client.WynnbindsClient;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;

public final class UpdateChecker extends Thread {

  private static final String MODRINTH_PROJECT = Wynnbinds.MOD_ID;
  private static final String GITHUB_URL = "https://github.com/OhhhZenix/" + Wynnbinds.MOD_NAME;
  private final HttpClient httpClient;

  public UpdateChecker() {
    this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  }

  private void handleResponse(HttpResponse<String> response) {
    if (response.statusCode() != 200) {
      Wynnbinds.LOGGER.debug("Modrinth API returned status {}", response.statusCode());
      return;
    }

    try {
      JsonArray versions = JsonParser.parseString(response.body()).getAsJsonArray();
      if (versions.isEmpty()) return;
      String latestVersion = versions.get(0).getAsJsonObject().get("version_number").getAsString();

      String currentVersion =
          FabricLoader.getInstance()
              .getModContainer(Wynnbinds.MOD_ID)
              .map(mc -> mc.getMetadata().getVersion().getFriendlyString())
              .orElse("0.0.0");

      if (!isNewer(latestVersion, currentVersion)) return;

      notifyPlayer(latestVersion, currentVersion);
    } catch (Exception e) {
      Wynnbinds.LOGGER.debug("Failed parsing update response", e);
    }
  }

  private void notifyPlayer(String latest, String current) {
    String homepageUrl =
        FabricLoader.getInstance()
            .getModContainer(Wynnbinds.MOD_ID)
            .flatMap(mc -> mc.getMetadata().getContact().get("homepage"))
            .orElse(GITHUB_URL);

    Utils.sendNotification(
        Component.nullToEmpty("New update available: " + latest),
        WynnbindsClient.getInstance().getConfig().isUpdateNotificationsEnabled());

    Wynnbinds.LOGGER.info(
        "{} v{} is available (current: v{}). Download: {}",
        Wynnbinds.MOD_NAME,
        latest,
        current,
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

  private void checkForUpdates() {
    try {
      String gameVersions =
          URLEncoder.encode(
              "[\"" + SharedConstants.getCurrentVersion().name() + "\"]", StandardCharsets.UTF_8);
      String loaders = URLEncoder.encode("[\"fabric\"]", StandardCharsets.UTF_8);
      String apiUrl =
          String.format(
              "https://api.modrinth.com/v2/project/%s/version?game_versions=%s&loaders=%s",
              MODRINTH_PROJECT, gameVersions, loaders);

      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(apiUrl))
              .header("Accept", "application/json")
              .header("User-Agent", String.format("%s (%s)", Wynnbinds.MOD_NAME, GITHUB_URL))
              .timeout(Duration.ofSeconds(10))
              .GET()
              .build();

      httpClient
          .sendAsync(request, HttpResponse.BodyHandlers.ofString())
          .orTimeout(15, TimeUnit.SECONDS)
          .thenAccept(this::handleResponse)
          .exceptionally(
              ex -> {
                Wynnbinds.LOGGER.debug("Update check failed: {}", ex.getMessage());
                return null;
              });
    } catch (Exception e) {
      Wynnbinds.LOGGER.debug("Failed to start update check", e);
    }
  }

  @Override
  public void run() {
    while (WynnbindsClient.isRunning()) {
      try {
        checkForUpdates();
        Thread.sleep(TimeUnit.HOURS.toMillis(1));
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
