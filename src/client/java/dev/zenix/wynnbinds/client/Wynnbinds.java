package dev.zenix.wynnbinds.client;

import java.util.HashMap;
import java.util.HashSet;

public enum Wynnbinds {
        // Wynntils
        WYNNTILS_FIRST_SPELL(
                        "Cast 1st Spell",
                        "key.keyboard.z",
                        "Wynntils 1st Spell"),
        WYNNTILS_SECOND_SPELL(
                        "Cast 2nd Spell",
                        "key.keyboard.x",
                        "Wynntils 2nd Spell"),
        WYNNTILS_THIRD_SPELL(
                        "Cast 3rd Spell",
                        "key.keyboard.c",
                        "Wynntils 3rd Spell"),
        WYNNTILS_FOURTH_SPELL(
                        "Cast 4th Spell",
                        "key.keyboard.v",
                        "Wynntils 4th Spell"),

        // Wynncraft Spell Caster
        SPELL_CASTER_FIRST_SPELL(
                        "key.wynncraft-spell-caster.spell.first",
                        "key.keyboard.unknown",
                        "Spell Caster 1st Spell"),
        SPELL_CASTER_SECOND_SPELL(
                        "key.wynncraft-spell-caster.spell.second",
                        "key.keyboard.unknown",
                        "Spell Caster 2nd Spell"),
        SPELL_CASTER_THIRD_SPELL(
                        "key.wynncraft-spell-caster.spell.third",
                        "key.keyboard.unknown",
                        "Spell Caster 3rd Spell"),
        SPELL_CASTER_FOURTH_SPELL(
                        "key.wynncraft-spell-caster.spell.fourth",
                        "key.keyboard.unknown",
                        "Spell Caster 4th Spell"),
        SPELL_CASTER_MELEE(
                        "key.wynncraft-spell-caster.spell.melee",
                        "key.keyboard.unknown",
                        "Spell Caster Melee"),
        SPELL_CASTER_CONFIG(
                        "key.wynncraft-spell-caster.config",
                        "key.keyboard.unknown",
                        "Spell Caster Config"),

        // BetterWynnMacros
        BETTER_WYNNMACROS_FIRST_SPELL(
                        "key.ktnwynnmacros.spell.1",
                        "key.keyboard.unknown",
                        "BetterWynnMacros 1st Spell"),
        BETTER_WYNNMACROS_SECOND_SPELL(
                        "key.ktnwynnmacros.spell.2",
                        "key.keyboard.unknown",
                        "BetterWynnMacros 2nd Spell"),
        BETTER_WYNNMACROS_THIRD_SPELL(
                        "key.ktnwynnmacros.spell.3",
                        "key.keyboard.unknown",
                        "BetterWynnMacros 3rd Spell"),
        BETTER_WYNNMACROS_FOURTH_SPELL(
                        "key.ktnwynnmacros.spell.4",
                        "key.keyboard.unknown",
                        "BetterWynnMacros 4th Spell");

        private final String translationKey;
        private final String defaultBoundKey;
        private final String displayName;

        private Wynnbinds(String translationKey, String defaultBoundKey, String displayName) {
                this.translationKey = translationKey;
                this.defaultBoundKey = defaultBoundKey;
                this.displayName = displayName;
        }

        public String getTranslationKey() {
                return this.translationKey;
        }

        public String getDefaultBoundKey() {
                return this.defaultBoundKey;
        }

        public String getDisplayName() {
                return this.displayName;
        }

        public static HashSet<String> getAllTranslationKeys() {
                HashSet<String> keys = new HashSet<>();
                for (Wynnbinds bind : Wynnbinds.values()) {
                        keys.add(bind.getTranslationKey());
                }
                return keys;
        }

        public static HashMap<String, String> getDefaultKeyBinds() {
                HashMap<String, String> defaultBinds = new HashMap<>();
                for (Wynnbinds bind : Wynnbinds.values()) {
                        defaultBinds.put(bind.getTranslationKey(), bind.getDefaultBoundKey());
                }
                return defaultBinds;
        }
}
