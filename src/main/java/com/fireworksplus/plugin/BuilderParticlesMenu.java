package com.fireworksplus.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
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
import java.util.Locale;

public class BuilderParticlesMenu implements Listener {

    private static final String TITLE_BASE = ChatColor.DARK_AQUA + "%s";

    private final JavaPlugin plugin;
    private final BuilderManager builderManager;
    private final BuilderMenu builderMenu;
    private final I18n i18n;
    private final NamespacedKey keyParticleId;

    public BuilderParticlesMenu(JavaPlugin plugin, BuilderManager builderManager, BuilderMenu builderMenu) {
        this.plugin = plugin;
        this.builderManager = builderManager;
        this.builderMenu = builderMenu;
        this.i18n = ((FireworksPlus) plugin).getI18n();
        this.keyParticleId = new NamespacedKey(plugin, "builder_particle_id");
    }

    public void open(Player p) {
        BuilderSession s = builderManager.getOrCreate(p);

        String title = String.format(TITLE_BASE, i18n.tr("gui.particles.title", "Particle Editor"));
        Inventory inv = Bukkit.createInventory(p, 27, title);
        List<String> options = particleOptions();

        int[] slots = new int[] {1, 2, 3, 4, 5, 6, 7, 12, 13, 14};
        int count = Math.min(options.size(), slots.length);
        for (int i = 0; i < count; i++) {
            particleButton(inv, slots[i], options.get(i), s);
        }

        inv.setItem(26, button(Material.ARROW, ChatColor.AQUA + i18n.tr("gui.common.back", "Back"),
                List.of(ChatColor.GRAY + i18n.tr("gui.particles.back_lore", "Return to builder")), "__back__"));

        p.openInventory(inv);
    }

    private void particleButton(Inventory inv, int slot, String particleName, BuilderSession s) {
        boolean has = s.trailParticles.stream().anyMatch(x -> x.equalsIgnoreCase(particleName));
        String status = has ? (ChatColor.GREEN + i18n.tr("gui.particles.in_list", "IN LIST")) : (ChatColor.RED + i18n.tr("gui.particles.not_in_list", "NOT IN LIST"));

        inv.setItem(slot, button(materialForParticle(particleName),
                ChatColor.AQUA + display(particleName),
                List.of(
                        ChatColor.GRAY + i18n.tr("gui.particles.status", "Status:") + " " + status,
                        ChatColor.DARK_GRAY + i18n.tr("gui.particles.click_add", "Click: add"),
                        ChatColor.DARK_GRAY + i18n.tr("gui.particles.shift_remove", "Shift-click: remove")
                ), particleName));
    }

    private ItemStack button(Material m, String name, List<String> lore, String particleId) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(keyParticleId, PersistentDataType.STRING, particleId);
            it.setItemMeta(meta);
        }
        return it;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        String title = String.format(TITLE_BASE, i18n.tr("gui.particles.title", "Particle Editor"));
        if (!e.getView().getTitle().equals(title)) return;

        e.setCancelled(true);

        int slot = e.getRawSlot();
        if (slot < 0 || slot >= 27) return;

        if (slot == 26) {
            builderMenu.open(p);
            return;
        }

        BuilderSession s = builderManager.getOrCreate(p);

        ItemStack it = e.getCurrentItem();
        if (it == null || it.getType() == Material.AIR) return;
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String particleName = pdc.get(keyParticleId, PersistentDataType.STRING);
        if (particleName == null || particleName.isBlank() || "__back__".equals(particleName)) return;

        boolean shift = (e.getClick() == ClickType.SHIFT_LEFT || e.getClick() == ClickType.SHIFT_RIGHT);
        if (shift) {
            boolean removed = s.trailParticles.removeIf(x -> x.equalsIgnoreCase(particleName));
            if (removed) {
                p.sendMessage(ChatColor.YELLOW + i18n.tr("msg.removed_particle", "Removed particle:") + " " + ChatColor.WHITE + display(particleName));
            } else {
                p.sendMessage(ChatColor.GRAY + i18n.tr("msg.particle_not_selected", "That particle is not selected."));
            }
        } else {
            if (s.trailParticles.stream().noneMatch(x -> x.equalsIgnoreCase(particleName))) {
                s.trailParticles.add(particleName);
                s.particleTrail = true;
                p.sendMessage(ChatColor.GREEN + i18n.tr("msg.added_particle", "Added particle:") + " " + ChatColor.WHITE + display(particleName));
            } else {
                p.sendMessage(ChatColor.GRAY + i18n.tr("msg.particle_already_selected", "Already selected:") + " " + ChatColor.WHITE + display(particleName));
            }
        }

        open(p);
    }

    private String display(String particleName) {
        if (particleName == null || particleName.isBlank()) return "";
        String key = "gui.particles.name." + particleName.toLowerCase(Locale.ROOT);
        String fallback = particleName.replace("_", " ");
        return i18n.tr(key, fallback);
    }

    private Material materialForParticle(String particleName) {
        if (particleName == null) return Material.GLOWSTONE_DUST;
        String key = particleName.trim().toUpperCase(Locale.ROOT);
        return switch (key) {
            case "DRIP_WATER", "WATER_SPLASH", "WATER_WAKE", "WATER_BUBBLE" -> Material.WATER_BUCKET;
            case "DRIP_LAVA", "LAVA" -> Material.LAVA_BUCKET;
            case "HEART" -> Material.POPPY;
            case "END_ROD" -> Material.END_ROD;
            case "VILLAGER_HAPPY" -> Material.EMERALD;
            case "CRIT" -> Material.IRON_NUGGET;
            case "CLOUD" -> Material.WHITE_DYE;
            case "ENCHANT" -> Material.ENCHANTING_TABLE;
            case "FIREWORKS_SPARK" -> Material.FIREWORK_STAR;
            default -> Material.GLOWSTONE_DUST;
        };
    }

    private List<String> particleOptions() {
        List<String> configured = plugin.getConfig().getStringList("particles.options");
        List<String> options = new ArrayList<>();
        if (configured != null) {
            for (String entry : configured) {
                if (entry == null || entry.isBlank()) continue;
                String normalized = entry.trim().toUpperCase(Locale.ROOT);
                try {
                    Particle.valueOf(normalized);
                    options.add(normalized);
                } catch (IllegalArgumentException ignored) {
                    continue;
                }
            }
        }

        if (!options.isEmpty()) {
            return options;
        }

        List<String> fallback = List.of("FIREWORKS_SPARK", "END_ROD", "VILLAGER_HAPPY", "CRIT", "CLOUD", "ENCHANT");
        List<String> filtered = new ArrayList<>();
        for (String entry : fallback) {
            try {
                Particle.valueOf(entry);
                filtered.add(entry);
            } catch (IllegalArgumentException ignored) {
                continue;
            }
        }
        return filtered;
    }
}
