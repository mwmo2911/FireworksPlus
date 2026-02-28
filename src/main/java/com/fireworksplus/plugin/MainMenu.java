package com.fireworksplus.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

public class MainMenu implements Listener {

    private final JavaPlugin plugin;
    private final ShowMenu showMenu;
    private final BuilderMenu builderMenu;
    private final ScheduleMenu scheduleMenu;
    private LanguageMenu languageMenu;
    private final ShowStorage storage;
    private final ScheduleManager scheduleManager;
    private final I18n i18n;

    public MainMenu(
            JavaPlugin plugin,
            ShowMenu showMenu,
            BuilderMenu builderMenu,
            ScheduleMenu scheduleMenu,
            LanguageMenu languageMenu,
            ShowStorage storage,
            ScheduleManager scheduleManager
    ) {
        this.plugin = plugin;
        this.showMenu = showMenu;
        this.builderMenu = builderMenu;
        this.scheduleMenu = scheduleMenu;
        this.languageMenu = languageMenu;
        this.storage = storage;
        this.scheduleManager = scheduleManager;
        this.i18n = ((FireworksPlus) plugin).getI18n();

    }

    public void open(Player p) {
        int size = clampSize(plugin.getConfig().getInt("main_gui.size", 27));
        Inventory inv = Bukkit.createInventory(p, size, title());

        Material filler = Material.GRAY_STAINED_GLASS_PANE;
        ItemStack fill = item(filler, " ", List.of());
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, fill);

        int showsSlot = plugin.getConfig().getInt("main_gui.shows_slot", 12);
        int builderSlot = plugin.getConfig().getInt("main_gui.builder_slot", 14);

        inv.setItem(showsSlot, item(Material.FIREWORK_STAR,
                ChatColor.AQUA + i18n.tr("gui.main.shows", "Shows"),
                List.of(ChatColor.GRAY + i18n.tr("gui.main.shows_lore", "Browse and start firework shows"))));

        inv.setItem(builderSlot, item(Material.ANVIL,
                ChatColor.AQUA + i18n.tr("gui.main.builder", "Builder"),
                List.of(ChatColor.GRAY + i18n.tr("gui.main.builder_lore", "Create custom shows"))));

        int languageSlot = resolveLanguageSlot(showsSlot, builderSlot);
        inv.setItem(languageSlot, languageIconItem());

        if (hasPermission(p, "fireworksplus.admin.reload")) {
            int reloadSlot = plugin.getConfig().getInt("main_gui.reload_slot", 22);
            inv.setItem(reloadSlot, item(Material.REDSTONE,
                    ChatColor.RED + i18n.tr("gui.main.reload", "Reload"),
                    List.of(ChatColor.GRAY + i18n.tr("gui.main.reload_lore", "Reload config and data files"))));
        }

        if (hasPermission(p, "fireworksplus.admin.schedule")) {
            int schedulesSlot = plugin.getConfig().getInt("main_gui.schedules_slot", 24);
            inv.setItem(schedulesSlot, item(Material.PAPER,
                    ChatColor.AQUA + i18n.tr("gui.main.schedules", "Schedules"),
                    List.of(ChatColor.GRAY + i18n.tr("gui.main.schedules_lore", "View scheduled shows"))));
        }

        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!e.getView().getTitle().equals(title())) return;

        e.setCancelled(true);

        int raw = e.getRawSlot();
        if (raw < 0 || raw >= e.getInventory().getSize()) return;

        int showsSlot = plugin.getConfig().getInt("main_gui.shows_slot", 12);
        int builderSlot = plugin.getConfig().getInt("main_gui.builder_slot", 14);
        int reloadSlot = plugin.getConfig().getInt("main_gui.reload_slot", 22);
        int schedulesSlot = plugin.getConfig().getInt("main_gui.schedules_slot", 24);
        int languageSlot = resolveLanguageSlot(showsSlot, builderSlot);

        if (raw == showsSlot) {
            showMenu.open(p);
            return;
        }

        if (raw == languageSlot) {
            if (languageMenu != null) {
                languageMenu.open(p);
            }
            return;
        }

        if (raw == builderSlot) {
            if (!hasPermission(p, "fireworksplus.builder")) {
                p.sendMessage(ChatColor.RED + i18n.tr("msg.no_permission", "No permission."));
                return;
            }
            builderMenu.open(p);
            return;
        }

        if (raw == reloadSlot && hasPermission(p, "fireworksplus.admin.reload")) {
            if (plugin instanceof FireworksPlus fp) {
                fp.reloadPluginData();
            } else {
                plugin.reloadConfig();
                storage.reload();
                scheduleManager.reload();
            }
            p.sendMessage(ChatColor.GREEN + i18n.tr("msg.reload_ok", "Reloaded config and data files."));
            open(p);
            return;
        }

        if (raw == schedulesSlot && hasPermission(p, "fireworksplus.admin.schedule")) {
            if (scheduleMenu != null) {
                scheduleMenu.open(p);
            }
        }
    }

    private int resolveLanguageSlot(int showsSlot, int builderSlot) {
        int midpoint = (showsSlot + builderSlot) / 2;
        int configured = plugin.getConfig().getInt("main_gui.language_slot", midpoint);
        if (configured < 0 || configured >= 54) {
            return midpoint;
        }
        return configured;
    }

    private ItemStack languageIconItem() {
        String name = ChatColor.AQUA + i18n.tr("gui.main.language", "Language");
        List<String> lore = List.of(ChatColor.GRAY + i18n.tr("gui.main.language_lore", "Choose plugin language"));

        String textureUrl = plugin.getConfig().getString(
                "main_gui.language_icon_texture",
                "https://textures.minecraft.net/texture/cf40942f364f6cbceffcf1151796410286a48b1aeba77243e218026c09cd1"
        );

        if (textureUrl == null || textureUrl.isBlank()) {
            return item(Material.NAME_TAG, name, lore);
        }

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta rawMeta = head.getItemMeta();
        if (!(rawMeta instanceof SkullMeta skullMeta)) {
            return item(Material.NAME_TAG, name, lore);
        }

        if (!applyTextureUrl(skullMeta, textureUrl)) {
            return item(Material.NAME_TAG, name, lore);
        }

        skullMeta.setDisplayName(name);
        skullMeta.setLore(lore);
        head.setItemMeta(skullMeta);
        return head;
    }

    private boolean applyTextureUrl(SkullMeta meta, String textureUrl) {
        try {
            String base64Seed = textureUrl.trim();
            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.nameUUIDFromBytes(base64Seed.getBytes(StandardCharsets.UTF_8)), "main_lang");
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(new URL(textureUrl.trim()));
            profile.setTextures(textures);
            meta.setOwnerProfile(profile);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean hasPermission(Player p, String node) {
        return p.hasPermission("fireworksplus.*") || p.hasPermission(node);
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

    private String title() {
        FileConfiguration c = plugin.getConfig();
        String configuredTitle = c.getString("main_gui.title", "");
        if (configuredTitle == null || configuredTitle.isBlank()) {
            configuredTitle = i18n.tr("gui.main.title", "&cFireworksPlus");
        }
        return color(configuredTitle);
    }

    private String color(String s) {
        if (s == null) return "";
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private int clampSize(int size) {
        int s = Math.max(9, Math.min(54, size));
        return ((s + 8) / 9) * 9;
    }
}