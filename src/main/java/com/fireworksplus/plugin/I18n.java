package com.fireworksplus.plugin;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

public class I18n {

    private final JavaPlugin plugin;
    private YamlConfiguration active;
    private YamlConfiguration fallback;

    public I18n(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        syncBundledFile("en");
        syncBundledFile("es");
        syncBundledFile("pl");
        syncBundledFile("de");

        fallback = loadLang("en");

        String configured = plugin.getConfig().getString("language", "");
        if (configured == null || configured.isBlank()) {
            configured = plugin.getConfig().getString("lang", "en");
        }
        String code = normalizeLanguage(configured);

        active = loadLang(code);
        if (active == null) {
            plugin.getLogger().warning("Language file not found for '" + code + "'. Falling back to 'en'.");
            active = fallback;
        }
    }

    public String tr(String key, String defaultValue) {
        String value = getRaw(key, defaultValue);
        return color(value);
    }

    public String trf(String key, String defaultValue, Object... args) {
        String template = getRaw(key, defaultValue);
        return color(String.format(template, args));
    }

    private String getRaw(String key, String defaultValue) {
        if (active != null && active.contains(key)) {
            return active.getString(key, defaultValue);
        }
        if (fallback != null && fallback.contains(key)) {
            return fallback.getString(key, defaultValue);
        }
        return defaultValue;
    }

    private void syncBundledFile(String code) {
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists() && !langFolder.mkdirs()) {
            return;
        }

        try {
            plugin.saveResource("lang/" + code + ".yml", false);
        } catch (IllegalArgumentException ignored) {
        }
    }

    private YamlConfiguration loadLang(String code) {
        YamlConfiguration bundled = loadBundledLang(code);
        YamlConfiguration englishBundled = loadBundledLang("en");
        YamlConfiguration merged = bundled != null ? bundled : new YamlConfiguration();

        File file = new File(plugin.getDataFolder(), "lang/" + code + ".yml");
        if (file.exists()) {
            YamlConfiguration disk = YamlConfiguration.loadConfiguration(file);
            for (Map.Entry<String, Object> entry : disk.getValues(true).entrySet()) {
                if (entry.getValue() instanceof org.bukkit.configuration.ConfigurationSection) continue;

                String key = entry.getKey();
                Object diskValue = entry.getValue();

                if (shouldKeepBundledValue(code, key, diskValue, bundled, englishBundled)) {
                    continue;
                }
                merged.set(key, diskValue);
            }
        }

        if (!merged.getKeys(true).isEmpty()) {
            return merged;
        }
        return null;
    }

    private boolean shouldKeepBundledValue(
            String code,
            String key,
            Object diskValue,
            YamlConfiguration selectedBundled,
            YamlConfiguration englishBundled
    ) {
        if ("en".equals(code)) return false;
        if (selectedBundled == null) return false;

        if (selectedBundled.contains(key)) return true;

        if (!(diskValue instanceof String)) return false;
        if (englishBundled == null || !englishBundled.contains(key)) return false;

        Object englishValue = englishBundled.get(key);
        if (!(englishValue instanceof String)) return false;

        String diskText = ((String) diskValue).trim();
        String enText = ((String) englishValue).trim();
        return diskText.equals(enText);
    }

    private YamlConfiguration loadBundledLang(String code) {
        try (InputStream in = plugin.getResource("lang/" + code + ".yml")) {
            if (in == null) return null;
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to load bundled language '" + code + "': " + ex.getMessage());
            return null;
        }
    }

    private String normalizeLanguage(String raw) {
        if (raw == null || raw.isBlank()) return "en";

        String code = raw.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        if (code.contains("-")) {
            code = code.substring(0, code.indexOf('-'));
        }

        return switch (code) {
            case "en", "english" -> "en";
            case "es", "spa", "spanish", "espanol", "español" -> "es";
            case "pl", "pol", "polish", "polski" -> "pl";
            case "de", "ger", "german", "deutsch" -> "de";
            default -> "en";
        };
    }

    private String color(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}