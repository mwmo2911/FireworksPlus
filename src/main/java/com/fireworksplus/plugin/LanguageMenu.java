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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class LanguageMenu implements Listener {

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

    public void open(Player p) {
        Inventory inv = Bukkit.createInventory(p, 27, ChatColor.AQUA + i18n.tr("gui.language.title", "Language"));

        ItemStack fill = item(Material.GRAY_STAINED_GLASS_PANE, " ", new ArrayList<String>());
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, fill);
        }

        inv.setItem(10, languageItem("en", "gui.language.english", "English", Arrays.asList(
                ChatColor.GRAY + i18n.tr("gui.language.english_lore", "United Kingdom flag"),
                ChatColor.DARK_GRAY + i18n.tr("gui.language.click_set", "Click to set this language")
        )));

        inv.setItem(12, languageItem("es", "gui.language.spanish", "Español", Arrays.asList(
                ChatColor.GRAY + i18n.tr("gui.language.spanish_lore", "Spain flag"),
                ChatColor.DARK_GRAY + i18n.tr("gui.language.click_set", "Click to set this language")
        )));

        inv.setItem(14, languageItem("pl", "gui.language.polish", "Polski", Arrays.asList(
                ChatColor.GRAY + i18n.tr("gui.language.polish_lore", "Poland flag"),
                ChatColor.DARK_GRAY + i18n.tr("gui.language.click_set", "Click to set this language")
        )));

        inv.setItem(16, languageItem("de", "gui.language.german", "Deutsch", Arrays.asList(
                ChatColor.GRAY + i18n.tr("gui.language.german_lore", "Germany flag"),
                ChatColor.DARK_GRAY + i18n.tr("gui.language.click_set", "Click to set this language")
        )));

        inv.setItem(26, item(Material.ARROW,
                ChatColor.AQUA + i18n.tr("gui.common.back", "Back"),
                Arrays.asList(ChatColor.GRAY + i18n.tr("gui.language.back_lore", "Return to main menu"))));

        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();

        String title = ChatColor.AQUA + i18n.tr("gui.language.title", "Language");
        if (!e.getView().getTitle().equals(title)) return;

        e.setCancelled(true);

        int raw = e.getRawSlot();
        if (raw < 0 || raw >= e.getInventory().getSize()) return;

        if (raw == 26) {
            if (mainMenu != null) {
                mainMenu.open(p);
            }
            return;
        }

        ItemStack current = e.getCurrentItem();
        if (current == null || current.getType() == Material.AIR) return;
        ItemMeta meta = current.getItemMeta();
        if (meta == null) return;

        String code = meta.getPersistentDataContainer().get(keyLangCode, PersistentDataType.STRING);
        if (code == null || code.isBlank()) return;

        plugin.getConfig().set("language", code);
        plugin.saveConfig();
        plugin.reloadPluginData();

        p.sendMessage(ChatColor.GREEN + i18n.tr("msg.language_set", "Language set to:") + " " + ChatColor.WHITE + displayName(code));

        if (mainMenu != null) {
            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    mainMenu.open(p);
                }
            });
        }
    }

    private ItemStack languageItem(String code, String nameKey, String fallbackName, List<String> lore) {
        ItemStack head = configuredHead(code, nameKey, fallbackName, lore);
        if (head != null) {
            return head;
        }
        return fallbackMap(code, nameKey, fallbackName, lore);
    }

    private ItemStack configuredHead(String code, String nameKey, String fallbackName, List<String> lore) {
        String texture = plugin.getConfig().getString("language_heads." + code + ".texture", "");
        String owner = plugin.getConfig().getString("language_heads." + code + ".owner", "");
        if ((texture == null || texture.isBlank()) && (owner == null || owner.isBlank())) {
            return null;
        }

        ItemStack it = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta rawMeta = it.getItemMeta();
        if (!(rawMeta instanceof SkullMeta)) {
            return null;
        }

        SkullMeta meta = (SkullMeta) rawMeta;

        applyCommonMeta(meta, code, nameKey, fallbackName, lore);

        boolean textured = false;
        if (texture != null && !texture.isBlank()) {
            textured = applyBase64Texture(meta, texture);
        }

        if (!textured && owner != null && !owner.isBlank()) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(owner));
        }

        it.setItemMeta(meta);
        return it;
    }

    private ItemStack flagBanner(String code, String nameKey, String fallbackName, List<String> lore) {
        Material base = switch (code.toLowerCase(Locale.ROOT)) {
            case "en" -> Material.BLUE_BANNER;
            case "es" -> Material.YELLOW_BANNER;
            case "pl" -> Material.WHITE_BANNER;
            case "de" -> Material.YELLOW_BANNER;
            default -> Material.WHITE_BANNER;
        };

        private ItemStack fallbackMap(String code, String nameKey, String fallbackName, List<String> lore) {
            String flag;
            switch (code.toLowerCase(Locale.ROOT)) {
                case "en":
                    flag = "[EN]";
                    break;
                case "es":
                    flag = "[ES]";
                    break;
                case "pl":
                    flag = "[PL]";
                    break;
                case "de":
                    flag = "[DE]";
                    break;
                default:
                    flag = "[LANG]";
                    break;
            }

            ItemStack it = new ItemStack(Material.MAP);
            ItemMeta meta = it.getItemMeta();
            if (meta == null) {
                return item(Material.PAPER, ChatColor.AQUA + i18n.tr(nameKey, fallbackName), lore);
            }

            applyCommonMeta(meta, code, nameKey, fallbackName, lore);
            meta.setDisplayName(ChatColor.AQUA + flag + " " + i18n.tr(nameKey, fallbackName));
            it.setItemMeta(meta);
            return it;
        }

    private void applyCommonMeta(ItemMeta meta, String code, String nameKey, String fallbackName, List<String> lore) {
        meta.setDisplayName(ChatColor.AQUA + i18n.tr(nameKey, fallbackName));
        meta.setLore(new ArrayList<>(lore));
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(keyLangCode, PersistentDataType.STRING, code);
    }

    private boolean applyBase64Texture(SkullMeta meta, String base64) {
        try {
            Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
            Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");

            Object profile = gameProfileClass
                    .getConstructor(java.util.UUID.class, String.class)
                    .newInstance(java.util.UUID.nameUUIDFromBytes(base64.getBytes(java.nio.charset.StandardCharsets.UTF_8)), "lang_flag");

            Object propertyMap = gameProfileClass.getMethod("getProperties").invoke(profile);
            Object property = propertyClass
                    .getConstructor(String.class, String.class)
                    .newInstance("textures", base64.trim());
            propertyMap.getClass().getMethod("put", Object.class, Object.class).invoke(propertyMap, "textures", property);

            java.lang.reflect.Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private ItemStack item(Material mat, String name, List<String> lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
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