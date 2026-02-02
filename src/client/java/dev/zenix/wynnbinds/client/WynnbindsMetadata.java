package dev.zenix.wynnbinds.client;

public class WynnbindsMetadata {

    private String displayName;
    private String defaultKey;

    public WynnbindsMetadata(String displayName, String defaultKey) {
        this.displayName = displayName;
        this.defaultKey = defaultKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDefaultKey() {
        return defaultKey;
    }
}
