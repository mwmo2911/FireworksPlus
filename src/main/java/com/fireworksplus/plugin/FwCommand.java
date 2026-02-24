package com.fireworksplus.plugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FwCommand implements CommandExecutor {

    private final FireworksPlus plugin;
    private final MainMenu mainMenu;
    private final ShowMenu menu;
    private final ShowService shows;
    private final ShowStorage storage;
    private final DraftManager drafts;
    private final ScheduleManager schedule;
    private final I18n i18n;

    public FwCommand(
            FireworksPlus plugin,
            MainMenu mainMenu,
            ShowMenu menu,
            ShowService shows,
            ShowStorage storage,
            DraftManager drafts,
            ScheduleManager schedule
    ) {
        this.plugin = plugin;
        this.mainMenu = mainMenu;
        this.menu = menu;
        this.shows = shows;
        this.storage = storage;
        this.drafts = drafts;
        this.schedule = schedule;
        this.i18n = plugin.getI18n();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(i18n.tr("msg.command_ingame_only", "This command can only be used in-game."));
            return true;
        }

        if (!hasPermission(player, "fireworksplus.use")) {
            player.sendMessage(ChatColor.RED + i18n.tr("msg.no_permission", "No permission."));
            return true;
        }

        if (args.length == 0) {
            if (mainMenu != null) {
                mainMenu.open(player);
            } else {
                menu.open(player);
            }
            return true;
        }

        String sub = args[0];

        if ("help".equalsIgnoreCase(sub)) {
            sendHelp(player);
            return true;
        }

        if ("version".equalsIgnoreCase(sub) || "ver".equalsIgnoreCase(sub)) {
            sendVersionBox(player);
            return true;
        }

        if ("reload".equalsIgnoreCase(sub)) {
            if (!hasPermission(player, "fireworksplus.admin.reload")) {
                player.sendMessage(ChatColor.RED + i18n.tr("msg.no_permission", "No permission."));
                return true;
            }

            plugin.reloadPluginData();
            player.sendMessage(ChatColor.GREEN + i18n.tr("msg.reload_ok", "Reloaded config and data files."));
            return true;
        }

        if ("stop".equalsIgnoreCase(sub)) {
            boolean stopped = shows.stopShow(player);
            player.sendMessage(stopped
                    ? ChatColor.GREEN + "Show stopped."
                    : ChatColor.RED + "No running show.");
            return true;
        }

        if ("list".equalsIgnoreCase(sub)) {
            sendShowList(player);
            return true;
        }

        if ("edit".equalsIgnoreCase(sub)) {
            if (!hasPermission(player, "fireworksplus.builder")) {
                player.sendMessage(ChatColor.RED + i18n.tr("msg.no_permission", "No permission."));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + i18n.tr("msg.usage", "Usage:") + " " + ChatColor.WHITE + usageEdit());
                return true;
            }

            String showId = resolveCustomShowId(args[1]);
            if (showId == null) {
                player.sendMessage(ChatColor.RED + "Custom show not found: " + ChatColor.WHITE + args[1]);
                return true;
            }

            boolean opened = menu.editCustom(player, showId);
            if (!opened) {
                player.sendMessage(ChatColor.RED + "Custom show not found: " + ChatColor.WHITE + args[1]);
            }
            return true;
        }

        if ("info".equalsIgnoreCase(sub)) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + i18n.tr("msg.usage", "Usage:") + " " + ChatColor.WHITE + usageInfo());
                return true;
            }

            String showId = resolveShowId(args[1]);
            if (showId == null) {
                player.sendMessage(ChatColor.RED + "Show not found: " + ChatColor.WHITE + args[1]);
                sendShowList(player);
                return true;
            }

            sendShowInfo(player, showId);
            return true;
        }

        if ("play".equalsIgnoreCase(sub)) {
            if (args.length < 2) {
                sendShowList(player);
                player.sendMessage(ChatColor.GRAY + i18n.tr("msg.usage", "Usage:") + " " + ChatColor.WHITE + usagePlay());
                return true;
            }

            String showId = resolveShowId(args[1]);
            if (showId == null) {
                player.sendMessage(ChatColor.RED + "Show not found: " + ChatColor.WHITE + args[1]);
                sendShowList(player);
                return true;
            }

            if (plugin.getConfig().isConfigurationSection("shows." + showId)) {
                String reason = shows.playBuiltIn(player, showId);
                if (reason != null) {
                    player.sendMessage(ChatColor.RED + reason);
                    return true;
                }
                sendBuiltInPreview(player, showId);
                return true;
            }

            DraftShow custom = storage.loadCustomShow(showId);
            String reason = shows.playCustom(player, custom);
            if (reason != null) {
                player.sendMessage(ChatColor.RED + reason);
                return true;
            }

            player.sendMessage(ChatColor.AQUA + "Starting: " + ChatColor.WHITE + "Custom show");
            player.sendMessage(ChatColor.DARK_GRAY + "ID: " + ChatColor.GRAY + showId);
            return true;
        }

        if ("schedule".equalsIgnoreCase(sub)) {
            if (!hasPermission(player, "fireworksplus.admin.schedule")) {
                player.sendMessage(ChatColor.RED + i18n.tr("msg.no_permission", "No permission."));
                return true;
            }
            if (args.length != 4) {
                player.sendMessage(ChatColor.RED + i18n.tr("msg.usage", "Usage:") + " " + ChatColor.WHITE + usageSchedule());
                return true;
            }

            String showId = resolveShowId(args[1]);
            if (showId == null) {
                player.sendMessage(ChatColor.RED + "Show not found: " + ChatColor.WHITE + args[1]);
                sendShowList(player);
                return true;
            }

            String err = schedule.addSchedule(showId, player.getLocation(), args[2], args[3]);
            if (err != null) {
                player.sendMessage(ChatColor.RED + err);
                return true;
            }

            player.sendMessage(ChatColor.GREEN + "Scheduled show: " + ChatColor.WHITE + showId);
            return true;
        }

        if ("unschedule".equalsIgnoreCase(sub)) {
            if (!hasPermission(player, "fireworksplus.admin.schedule")) {
                player.sendMessage(ChatColor.RED + i18n.tr("msg.no_permission", "No permission."));
                return true;
            }
            if (args.length != 2) {
                player.sendMessage(ChatColor.RED + i18n.tr("msg.usage", "Usage:") + " " + ChatColor.WHITE + usageUnschedule());
                return true;
            }

            boolean ok = schedule.removeSchedule(args[1]);
            if (ok) {
                player.sendMessage(ChatColor.GREEN + "Schedule removed: " + ChatColor.WHITE + args[1]);
            } else {
                player.sendMessage(ChatColor.RED + "Schedule not found: " + ChatColor.WHITE + args[1]);
            }
            return true;
        }

        if ("schedules".equalsIgnoreCase(sub)) {
            if (!hasPermission(player, "fireworksplus.admin.schedule")) {
                player.sendMessage(ChatColor.RED + i18n.tr("msg.no_permission", "No permission."));
                return true;
            }

            List<String> all = schedule.listSchedulesPretty();
            if (all.isEmpty()) {
                player.sendMessage(ChatColor.RED + "No schedules found.");
                return true;
            }

            player.sendMessage(ChatColor.AQUA + "Schedules:");
            for (String line : all) player.sendMessage(line);
            return true;
        }

        if ("delete".equalsIgnoreCase(sub)) {
            if (!hasPermission(player, "fireworksplus.admin.delete")) {
                player.sendMessage(ChatColor.RED + i18n.tr("msg.no_permission", "No permission."));
                return true;
            }

            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + i18n.tr("msg.usage", "Usage:") + " " + ChatColor.WHITE + usageDelete());
                return true;
            }

            String showId = resolveShowId(args[1]);
            if (showId == null) {
                player.sendMessage(ChatColor.RED + "Custom show not found: " + ChatColor.WHITE + args[1]);
                return true;
            }

            if (plugin.getConfig().isConfigurationSection("shows." + showId)) {
                player.sendMessage(ChatColor.RED + "Cannot delete built-in show: " + ChatColor.WHITE + showId);
                return true;
            }

            boolean ok = storage.deleteCustomShow(showId);
            player.sendMessage(ok
                    ? ChatColor.GREEN + "Deleted custom show: " + ChatColor.WHITE + showId
                    : ChatColor.RED + "Custom show not found: " + ChatColor.WHITE + showId);
            return true;
        }

        player.sendMessage(ChatColor.RED + "Unknown subcommand.");
        sendHelp(player);
        return true;
    }

    private String argShow() {
        return "<" + i18n.tr("arg.show", "show") + ">";
    }

    private String argId() {
        return "<" + i18n.tr("arg.id", "id") + ">";
    }

    private String usageEdit() {
        return "/fw edit " + argShow();
    }

    private String usageInfo() {
        return "/fw info " + argShow();
    }

    private String usagePlay() {
        return "/fw play " + argShow();
    }

    private String usageSchedule() {
        return "/fw schedule " + argShow() + " <" + i18n.tr("arg.date", "date") + "> <" + i18n.tr("arg.time", "time") + ">";
    }

    private String usageUnschedule() {
        return "/fw unschedule " + argId();
    }

    private String usageDelete() {
        return "/fw delete " + argShow();
    }

    private void sendVersionBox(Player p) {
        String line1 =
                ChatColor.GOLD + "FireworksPlus " +
                        ChatColor.YELLOW + "v" + plugin.getDescription().getVersion();

        String line2 =
                ChatColor.GOLD + "Created by: " +
                        ChatColor.YELLOW + "mwmo2911";

        p.sendMessage(line1);
        p.sendMessage(line2);
    }

    private void sendHelp(Player p) {
        p.sendMessage(ChatColor.GRAY + "Commands:");
        p.sendMessage(ChatColor.YELLOW + "/fw" + ChatColor.WHITE + " - Open the GUI");
        p.sendMessage(ChatColor.YELLOW + "/fw help" + ChatColor.WHITE + " - Show this help list");
        p.sendMessage(ChatColor.YELLOW + usagePlay() + ChatColor.WHITE + " - " + i18n.tr("help.start_show", "Start a show"));
        p.sendMessage(ChatColor.YELLOW + usageInfo() + ChatColor.WHITE + " - " + i18n.tr("help.show_details", "Show details for a show"));
        if (hasPermission(p, "fireworksplus.builder")) {
            p.sendMessage(ChatColor.YELLOW + usageEdit() + ChatColor.WHITE + " - " + i18n.tr("help.edit_custom", "Edit a custom show"));
        }
        p.sendMessage(ChatColor.YELLOW + "/fw list" + ChatColor.WHITE + " - List all shows");
        p.sendMessage(ChatColor.YELLOW + "/fw stop" + ChatColor.WHITE + " - Stop the running show");
        p.sendMessage(ChatColor.YELLOW + "/fw version" + ChatColor.WHITE + " - Show plugin version");
        if (hasPermission(p, "fireworksplus.admin.schedule")) {
            p.sendMessage(ChatColor.YELLOW + usageSchedule() + ChatColor.WHITE + " - " + i18n.tr("help.schedule_show", "Schedule a show"));
            p.sendMessage(ChatColor.YELLOW + "/fw schedules" + ChatColor.WHITE + " - " + i18n.tr("help.list_schedules", "List schedules"));
            p.sendMessage(ChatColor.YELLOW + usageUnschedule() + ChatColor.WHITE + " - " + i18n.tr("help.remove_schedule", "Remove a schedule"));
        }
        if (hasPermission(p, "fireworksplus.admin.delete")) {
            p.sendMessage(ChatColor.YELLOW + usageDelete() + ChatColor.WHITE + " - " + i18n.tr("help.delete_custom", "Delete a custom show"));
        }
        if (hasPermission(p, "fireworksplus.admin.reload")) {
            p.sendMessage(ChatColor.YELLOW + "/fw reload" + ChatColor.WHITE + " - Reload config and data files");
        }
    }

    private void sendShowList(Player player) {
        List<String> builtIn = new ArrayList<>();
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("shows");
        if (sec != null) builtIn.addAll(sec.getKeys(false));
        builtIn.sort(String.CASE_INSENSITIVE_ORDER);

        List<String> custom = new ArrayList<>(storage.listCustomShows());
        custom.sort(String.CASE_INSENSITIVE_ORDER);

        if (builtIn.isEmpty() && custom.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No shows found.");
            return;
        }

        if (!builtIn.isEmpty()) {
            player.sendMessage(ChatColor.AQUA + "Built-in shows:");
            player.sendMessage(ChatColor.GRAY + String.join(", ", builtIn));
        }

        if (!custom.isEmpty()) {
            player.sendMessage(ChatColor.AQUA + "Custom shows:");
            player.sendMessage(ChatColor.GRAY + String.join(", ", custom));
        }
    }

    private void sendShowInfo(Player player, String showId) {
        if (plugin.getConfig().isConfigurationSection("shows." + showId)) {
            sendBuiltInPreview(player, showId);
            return;
        }

        DraftShow custom = storage.loadCustomShow(showId);
        if (custom == null) {
            player.sendMessage(ChatColor.RED + "Show not found: " + ChatColor.WHITE + showId);
            return;
        }

        player.sendMessage(ChatColor.AQUA + i18n.tr("msg.custom_show", "Custom show") + ": " + ChatColor.WHITE + showId);
        player.sendMessage(ChatColor.GRAY + i18n.tr("msg.duration", "Duration:") + " " + ChatColor.WHITE + custom.durationSeconds + "s");
        player.sendMessage(ChatColor.GRAY + i18n.tr("msg.interval", "Interval:") + " " + ChatColor.WHITE + custom.intervalTicks + " " + i18n.tr("msg.ticks", "ticks"));
        player.sendMessage(ChatColor.GRAY + i18n.tr("msg.radius", "Radius:") + " " + ChatColor.WHITE + custom.radius);
        player.sendMessage(ChatColor.GRAY + i18n.tr("msg.power", "Power:") + " " + ChatColor.WHITE + custom.powerMin + "-" + custom.powerMax);
        player.sendMessage(ChatColor.GRAY + i18n.tr("msg.type", "Type:") + " " + ChatColor.WHITE + formatTypes(custom.fireworkTypes));
        player.sendMessage(ChatColor.GRAY + i18n.tr("msg.particles", "Particles:") + " " + ChatColor.WHITE + formatParticles(custom));
        player.sendMessage(ChatColor.GRAY + i18n.tr("msg.points", "Points:") + " " + ChatColor.WHITE + custom.points.size());
        if (!custom.palette.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + i18n.tr("msg.palette", "Palette:") + " " + ChatColor.WHITE + String.join(", ", custom.palette));
        }
    }

    private void sendBuiltInPreview(Player player, String showId) {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("shows." + showId);
        if (sec == null) {
            player.sendMessage(ChatColor.RED + "Show not found: " + ChatColor.WHITE + showId);
            return;
        }

        String display = sec.getString("display", showId);
        int fireworks = sec.getInt("fireworks", 30);
        int interval = sec.getInt("interval_ticks", 6);
        double radius = sec.getDouble("radius", 1.6);
        int pMin = sec.getInt("power_min", 1);
        int pMax = sec.getInt("power_max", 2);
        List<String> palette = sec.getStringList("palette");
        List<String> particles = sec.getStringList("particles");

        player.sendMessage(ChatColor.AQUA + i18n.tr("msg.builtin_show", "Built-in show:") + " " + ChatColor.WHITE + ChatColor.translateAlternateColorCodes('&', display));
        player.sendMessage(ChatColor.GRAY + i18n.tr("msg.id", "ID:") + " " + ChatColor.WHITE + showId);
        player.sendMessage(ChatColor.GRAY + i18n.tr("msg.fireworks", "Fireworks:") + " " + ChatColor.WHITE + fireworks);
        player.sendMessage(ChatColor.GRAY + i18n.tr("msg.interval", "Interval:") + " " + ChatColor.WHITE + interval + " " + i18n.tr("msg.ticks", "ticks"));
        player.sendMessage(ChatColor.GRAY + i18n.tr("msg.radius", "Radius:") + " " + ChatColor.WHITE + radius);
        player.sendMessage(ChatColor.GRAY + i18n.tr("msg.power", "Power:") + " " + ChatColor.WHITE + pMin + "-" + pMax);
        if (!palette.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + i18n.tr("msg.palette", "Palette:") + " " + ChatColor.WHITE + String.join(", ", palette));
        }
        if (particles != null && !particles.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + i18n.tr("msg.particles", "Particles:") + " " + ChatColor.WHITE + particles.stream().map(this::displayParticleName).collect(java.util.stream.Collectors.joining(", ")));
        }
    }

    private String formatTypes(List<String> types) {
        if (types == null || types.isEmpty()) return i18n.tr("msg.random", "Random");
        return String.join(", ", types.stream().map(this::displayTypeName).collect(java.util.stream.Collectors.toList()));
    }

    private String formatParticles(DraftShow show) {
        if (show.trailParticles != null && !show.trailParticles.isEmpty()) {
            return String.join(", ", show.trailParticles.stream().map(this::displayParticleName).collect(java.util.stream.Collectors.toList()));
        }
        return show.particleTrail ? i18n.tr("msg.on", "On") : i18n.tr("msg.off", "Off");
    }

    private String displayTypeName(String typeId) {
        if (typeId == null || typeId.isBlank()) return "";
        String key = "gui.types.name." + typeId.toLowerCase(java.util.Locale.ROOT);
        return i18n.tr(key, typeId.replace("_", " "));
    }

    private String displayParticleName(String particleId) {
        if (particleId == null || particleId.isBlank()) return "";
        String key = "gui.particles.name." + particleId.toLowerCase(java.util.Locale.ROOT);
        return i18n.tr(key, particleId.replace("_", " "));
    }

    private String resolveShowId(String input) {
        if (input == null || input.isBlank()) return null;
        String normalized = normalize(input);

        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("shows");
        if (sec != null) {
            List<String> keys = new ArrayList<>(sec.getKeys(false));
            keys.sort(Comparator.comparing(String::toLowerCase));
            for (String key : keys) {
                if (normalize(key).equals(normalized)) {
                    return key;
                }
            }
        }

        for (String key : storage.listCustomShows()) {
            if (normalize(key).equals(normalized)) {
                return key;
            }
        }

        return null;
    }

    private String resolveCustomShowId(String input) {
        if (input == null || input.isBlank()) return null;
        String normalized = normalize(input);
        for (String key : storage.listCustomShows()) {
            if (normalize(key).equals(normalized)) {
                return key;
            }
        }
        return null;
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.toLowerCase()
                .replace(" ", "")
                .replace("_", "")
                .replace("-", "");
    }

    private boolean hasPermission(Player p, String node) {
        return p.hasPermission("fireworksplus.*") || p.hasPermission(node);
    }
}