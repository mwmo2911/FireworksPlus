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

public class I18n {

    private final JavaPlugin plugin;
    private YamlConfiguration active;
    private YamlConfiguration fallback;

    public I18n(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        ensureBundledFile("en");
        ensureBundledFile("es");
        ensureBundledFile("pl");
        ensureBundledFile("de");

        fallback = loadLang("en");

        String configured = plugin.getConfig().getString("language", "en");
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

    private void ensureBundledFile(String code) {
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists() && !langFolder.mkdirs()) {
            return;
        }

        File target = new File(langFolder, code + ".yml");
        if (target.exists()) return;

        try {
            plugin.saveResource("lang/" + code + ".yml", false);
        } catch (IllegalArgumentException ignored) {
            // Not present in jar; skip.
        }
    }

    private YamlConfiguration loadLang(String code) {
        File file = new File(plugin.getDataFolder(), "lang/" + code + ".yml");
        if (file.exists()) {
            return YamlConfiguration.loadConfiguration(file);
        }

        try (InputStream in = plugin.getResource("lang/" + code + ".yml")) {
            if (in == null) return null;
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to load language '" + code + "': " + ex.getMessage());
            return null;
        }
    }

    private String normalizeLanguage(String raw) {
        if (raw == null || raw.isBlank()) return "en";
        String code = raw.trim().toLowerCase(Locale.ROOT);
        return switch (code) {
            case "en", "es", "pl", "de" -> code;
            default -> "en";
        };
    }

    private String color(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}