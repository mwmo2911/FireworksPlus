package com.fireworksplus.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class BuilderTypesMenu implements Listener {

    private static final String TITLE_BASE = ChatColor.DARK_AQUA + "%s";

    private final JavaPlugin plugin;
    private final BuilderManager builderManager;
    private final BuilderMenu builderMenu;
    private final I18n i18n;
    private final NamespacedKey keyTypeId;

    public BuilderTypesMenu(JavaPlugin plugin, BuilderManager builderManager, BuilderMenu builderMenu) {
        this.plugin = plugin;
        this.builderManager = builderManager;
        this.builderMenu = builderMenu;
        this.i18n = ((FireworksPlus) plugin).getI18n();
        this.keyTypeId = new NamespacedKey(plugin, "builder_type_id");
    }

    public void open(Player p) {
        BuilderSession s = builderManager.getOrCreate(p);

        String title = String.format(TITLE_BASE, i18n.tr("gui.types.title", "Type Editor"));
        Inventory inv = Bukkit.createInventory(p, 27, title);

        typeButton(inv, 11, FireworkEffect.Type.BALL, s);
        typeButton(inv, 12, FireworkEffect.Type.BALL_LARGE, s);
        typeButton(inv, 13, FireworkEffect.Type.STAR, s);
        typeButton(inv, 14, FireworkEffect.Type.BURST, s);
        typeButton(inv, 15, FireworkEffect.Type.CREEPER, s);

        inv.setItem(22, button(Material.BOOK, ChatColor.AQUA + i18n.tr("gui.types.current", "Current Types"),
                List.of(
                        ChatColor.GRAY + i18n.tr("gui.types.selected", "Selected:") + " " + ChatColor.WHITE + s.fireworkTypes.size(),
                        ChatColor.DARK_GRAY + i18n.tr("gui.types.click_hint", "Click adds, Shift-click removes")
                ), "__info__"));

        inv.setItem(26, button(Material.ARROW, ChatColor.AQUA + i18n.tr("gui.common.back", "Back"),
                List.of(ChatColor.GRAY + i18n.tr("gui.types.back_lore", "Return to builder")), "__back__"));

        p.openInventory(inv);
    }

    private void typeButton(Inventory inv, int slot, FireworkEffect.Type type, BuilderSession s) {
        boolean has = s.fireworkTypes.stream().anyMatch(x -> x.equalsIgnoreCase(type.name()));
        String status = has ? (ChatColor.GREEN + i18n.tr("gui.types.in", "IN TYPES")) : (ChatColor.RED + i18n.tr("gui.types.not_in", "NOT IN TYPES"));

        inv.setItem(slot, button(materialFor(type),
                ChatColor.AQUA + display(type),
                List.of(
                        ChatColor.GRAY + i18n.tr("gui.types.status", "Status:") + " " + status,
                        ChatColor.DARK_GRAY + i18n.tr("gui.types.click_add", "Click: add"),
                        ChatColor.DARK_GRAY + i18n.tr("gui.types.shift_remove", "Shift-click: remove")
                ), type.name()));
    }

    private ItemStack button(Material m, String name, List<String> lore, String typeId) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(keyTypeId, PersistentDataType.STRING, typeId);
            it.setItemMeta(meta);
        }
        return it;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        String title = String.format(TITLE_BASE, i18n.tr("gui.types.title", "Type Editor"));
        if (!e.getView().getTitle().equals(title)) return;

        e.setCancelled(true);

        int slot = e.getRawSlot();
        if (slot < 0 || slot >= 27) return;

        if (slot == 26) {
            builderMenu.open(p);
            return;
        }

        ItemStack it = e.getCurrentItem();
        if (it == null || it.getType() == Material.AIR) return;
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String typeId = pdc.get(keyTypeId, PersistentDataType.STRING);
        if (typeId == null || typeId.isBlank() || "__back__".equals(typeId)) return;

        FireworkEffect.Type type;
        try {
            type = FireworkEffect.Type.valueOf(typeId.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return;
        }

        BuilderSession s = builderManager.getOrCreate(p);
        boolean shift = (e.getClick() == ClickType.SHIFT_LEFT || e.getClick() == ClickType.SHIFT_RIGHT);

        if (shift) {
            boolean removed = s.fireworkTypes.removeIf(x -> x.equalsIgnoreCase(type.name()));
            if (removed) {
                p.sendMessage(ChatColor.YELLOW + i18n.tr("msg.removed_type", "Removed type:") + " " + ChatColor.WHITE + display(type));
            } else {
                p.sendMessage(ChatColor.GRAY + i18n.tr("msg.type_not_selected", "That type is not selected."));
            }
        } else {
            if (s.fireworkTypes.stream().noneMatch(x -> x.equalsIgnoreCase(type.name()))) {
                s.fireworkTypes.add(type.name());
                p.sendMessage(ChatColor.GREEN + i18n.tr("msg.added_type", "Added type:") + " " + ChatColor.WHITE + display(type));
            } else {
                p.sendMessage(ChatColor.GRAY + i18n.tr("msg.type_already_selected", "Already selected:") + " " + ChatColor.WHITE + display(type));
            }
        }

        open(p);
    }

    private String display(FireworkEffect.Type type) {
        String key = "gui.types.name." + type.name().toLowerCase(java.util.Locale.ROOT);
        String fallback = type.name().replace("_", " ");
        return i18n.tr(key, fallback);
    }
    private Material materialFor(FireworkEffect.Type type) {
        return switch (type) {
            case BALL -> Material.SLIME_BALL;
            case BALL_LARGE -> Material.MAGMA_CREAM;
            case STAR -> Material.NETHER_STAR;
            case BURST -> Material.FIRE_CHARGE;
            case CREEPER -> Material.CREEPER_HEAD;
        };
    }
}