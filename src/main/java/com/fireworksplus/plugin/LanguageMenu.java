package com.fireworksplus.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LanguageMenu implements Listener {

    private static final int SLOT_EN = 10;
    private static final int SLOT_ES = 12;
    private static final int SLOT_PL = 14;
    private static final int SLOT_DE = 16;
    private static final int SLOT_BACK = 26;

    private static final String BUILTIN_TEXTURE_EN = "";
    private static final String BUILTIN_TEXTURE_ES = "";
    private static final String BUILTIN_TEXTURE_PL = "";
    private static final String BUILTIN_TEXTURE_DE = "";

    private final FireworksPlus plugin;
    private final I18n i18n;
    private final NamespacedKey keyLangCode;
    private MainMenu mainMenu;

    public LanguageMenu(FireworksPlus plugin) {
        this.plugin = plugin;
        this.i18n = plugin.getI18n();
        this.keyLangCode = new NamespacedKey(plugin, "lang_code");
    }

    public void setMainMenu(MainMenu mainMenu) {
        this.mainMenu = mainMenu;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(player, 27, title());

        ItemStack fill = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, fill);
        }

        inv.setItem(SLOT_EN, languageItem("en", "gui.language.english", "English", List.of(
                ChatColor.DARK_GRAY + i18n.tr("gui.language.click_set", "Click to set this language")
        )));

        inv.setItem(SLOT_ES, languageItem("es", "gui.language.spanish", "Español", List.of(
                ChatColor.DARK_GRAY + i18n.tr("gui.language.click_set", "Click to set this language")
        )));

        inv.setItem(SLOT_PL, languageItem("pl", "gui.language.polish", "Polski", List.of(
                ChatColor.DARK_GRAY + i18n.tr("gui.language.click_set", "Click to set this language")
        )));

        inv.setItem(SLOT_DE, languageItem("de", "gui.language.german", "Deutsch", List.of(
                ChatColor.DARK_GRAY + i18n.tr("gui.language.click_set", "Click to set this language")
        )));

        inv.setItem(SLOT_BACK, item(Material.ARROW,
                ChatColor.AQUA + i18n.tr("gui.common.back", "Back"),
                List.of(ChatColor.GRAY + i18n.tr("gui.language.back_lore", "Return to main menu"))));

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!e.getView().getTitle().equals(title())) return;

        e.setCancelled(true);

        int rawSlot = e.getRawSlot();
        if (rawSlot < 0 || rawSlot >= e.getInventory().getSize()) return;

        if (rawSlot == SLOT_BACK) {
            if (mainMenu != null) {
                mainMenu.open(player);
            }
            return;
        }

        ItemStack current = e.getCurrentItem();
        if (current == null || current.getType() == Material.AIR) return;

        ItemMeta meta = current.getItemMeta();
        if (meta == null) return;

        String code = meta.getPersistentDataContainer().get(keyLangCode, PersistentDataType.STRING);
        if (code == null || code.isBlank()) return;

        String normalizedCode = code.trim().toLowerCase(Locale.ROOT);
        String configured = plugin.getConfig().getString("language", "en");
        if (configured != null && normalizedCode.equals(configured.trim().toLowerCase(Locale.ROOT))) {
            player.sendMessage(ChatColor.YELLOW + i18n.tr("msg.language_set", "Language set to:") + " " + ChatColor.WHITE + displayName(normalizedCode));
            return;
        }

        plugin.getConfig().set("language", normalizedCode);
        plugin.saveConfig();
        plugin.reloadPluginData();

        player.sendMessage(ChatColor.GREEN + i18n.tr("msg.language_set", "Language set to:") + " " + ChatColor.WHITE + displayName(normalizedCode));

        if (mainMenu != null) {
            Bukkit.getScheduler().runTask(plugin, () -> mainMenu.open(player));
        }
    }

    private String title() {
        return ChatColor.AQUA + i18n.tr("gui.language.title", "Language");
    }

    private ItemStack languageItem(String code, String nameKey, String fallbackName, List<String> lore) {
        ItemStack head = configuredHead(code, nameKey, fallbackName, lore);
        if (head != null) return head;
        return fallbackMap(code, nameKey, fallbackName, lore);
    }

    private ItemStack configuredHead(String code, String nameKey, String fallbackName, List<String> lore) {
        String textureInput = plugin.getConfig().getString("language_heads." + code + ".texture", "");
        String configuredTexture = normalizeTextureInput(textureInput);
        String builtInTexture = normalizeTextureInput(builtInTexture(code));

        String texture = !configuredTexture.isBlank() ? configuredTexture : builtInTexture;
        if (texture.isBlank()) {
            return null;
        }

        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta rawMeta = item.getItemMeta();
        if (!(rawMeta instanceof SkullMeta skullMeta)) {
            return null;
        }

        applyCommonMeta(skullMeta, code, nameKey, fallbackName, lore);
        if (!applyBase64Texture(skullMeta, texture)) {
            return null;
        }

        item.setItemMeta(skullMeta);
        return item;
    }

    private ItemStack fallbackMap(String code, String nameKey, String fallbackName, List<String> lore) {

        String flag = switch (code.toLowerCase(Locale.ROOT)) {
            case "en" -> "[EN]";
            case "es" -> "[ES]";
            case "pl" -> "[PL]";
            case "de" -> "[DE]";
            default -> "[LANG]";
        };

        return item(Material.PAPER, ChatColor.AQUA + flag + " " + i18n.tr(nameKey, fallbackName), lore);
    }

    private String builtInTexture(String code) {
        return switch (code.toLowerCase(Locale.ROOT)) {
            case "en" -> BUILTIN_TEXTURE_EN;
            case "es" -> BUILTIN_TEXTURE_ES;
            case "pl" -> BUILTIN_TEXTURE_PL;
            case "de" -> BUILTIN_TEXTURE_DE;
            default -> "";
        };
    }

    private String normalizeTextureInput(String textureInput) {
        if (textureInput == null) return "";

        String raw = textureInput.trim();
        if (raw.isBlank()) return "";

        if (raw.startsWith("http://") || raw.startsWith("https://")) {
            return toBase64Texture(raw);
        }

        if (raw.matches("^[a-fA-F0-9]{32,}$")) {
            return toBase64Texture("http://textures.minecraft.net/texture/" + raw);
        }

        if (raw.matches("^[A-Za-z0-9+/=]+$")) {
            try {
                Base64.getDecoder().decode(raw);
                return raw;
            } catch (IllegalArgumentException ignored) {
                return "";
            }
        }

        return "";
    }

    private String toBase64Texture(String url) {
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"}}}";
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }


    private void applyCommonMeta(ItemMeta meta, String code, String nameKey, String fallbackName, List<String> lore) {
        meta.setDisplayName(ChatColor.AQUA + i18n.tr(nameKey, fallbackName));
        meta.setLore(new ArrayList<>(lore));

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(keyLangCode, PersistentDataType.STRING, code);
    }

    private boolean applyBase64Texture(SkullMeta meta, String base64) {
        try {
            String textureUrl = extractTextureUrl(base64);
            if (textureUrl != null && !textureUrl.isBlank() && applyViaBukkitProfile(meta, textureUrl, base64)) {
                return true;
            }
            Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
            Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");

            Object profile = gameProfileClass
                    .getConstructor(UUID.class, String.class)
                    .newInstance(UUID.nameUUIDFromBytes(base64.getBytes(StandardCharsets.UTF_8)), "lang_flag");

            Object propertyMap = gameProfileClass.getMethod("getProperties").invoke(profile);
            Object property = propertyClass
                    .getConstructor(String.class, String.class)
                    .newInstance("textures", base64.trim());
            propertyMap.getClass().getMethod("put", Object.class, Object.class).invoke(propertyMap, "textures", property);

            try {
                java.lang.reflect.Field profileField = meta.getClass().getDeclaredField("profile");
                profileField.setAccessible(true);
                profileField.set(meta, profile);
                return true;
            } catch (NoSuchFieldException ignored) {
                java.lang.reflect.Method setProfile = meta.getClass().getDeclaredMethod("setProfile", gameProfileClass);
                setProfile.setAccessible(true);
                setProfile.invoke(meta, profile);
                return true;
            }
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean applyViaBukkitProfile(SkullMeta meta, String textureUrl, String base64Seed) {
        try {
            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.nameUUIDFromBytes(base64Seed.getBytes(StandardCharsets.UTF_8)), "lang_flag");
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(new URL(textureUrl));
            profile.setTextures(textures);
            meta.setOwnerProfile(profile);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private String extractTextureUrl(String base64) {
        try {
            String json = new String(Base64.getDecoder().decode(base64.trim()), StandardCharsets.UTF_8);
            Matcher m = Pattern.compile("\\\"url\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(json);
            if (m.find()) {
                return m.group(1);
            }
        } catch (IllegalArgumentException ignored) {
        }
        return null;
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String displayName(String code) {
        if (code == null) return "";
        return switch (code.toLowerCase(Locale.ROOT)) {
            case "en" -> i18n.tr("gui.language.english", "English");
            case "es" -> i18n.tr("gui.language.spanish", "Español");
            case "pl" -> i18n.tr("gui.language.polish", "Polski");
            case "de" -> i18n.tr("gui.language.german", "Deutsch");
            default -> code;
        };
    }
}