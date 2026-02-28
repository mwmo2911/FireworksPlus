package com.fireworksplus.plugin;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class FireworksPlus extends JavaPlugin {

    private I18n i18n;

    private ShowService showService;
    private ShowStorage showStorage;
    private DraftManager draftManager;
    private ScheduleManager scheduleManager;
    private UpdateChecker updateChecker;

    private ShowMenu showMenu;
    private BuilderMenu builderMenu;
    private BuilderColorsMenu builderColorsMenu;
    private BuilderTypesMenu builderTypesMenu;
    private BuilderParticlesMenu builderParticlesMenu;

    private MainMenu mainMenu;
    private ScheduleMenu scheduleMenu;
    private LanguageMenu languageMenu;

    private BuilderManager builderManager;
    private BuilderChatListener builderChatListener;
    private UpdateListener updateListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        applyConfigDefaults();
        this.i18n = new I18n(this);
        this.i18n.reload();

        this.showService = new ShowService(this);
        this.showStorage = new ShowStorage(this);
        this.draftManager = new DraftManager(this);
        this.scheduleManager = new ScheduleManager(this, showService, showStorage);

        this.builderManager = new BuilderManager(this);

        this.builderMenu = new BuilderMenu(this, builderManager, showStorage);
        this.builderChatListener = new BuilderChatListener(this, builderManager, builderMenu);
        this.builderColorsMenu = new BuilderColorsMenu(this, builderManager, builderMenu);
        this.builderTypesMenu = new BuilderTypesMenu(this, builderManager, builderMenu);
        this.builderParticlesMenu = new BuilderParticlesMenu(this, builderManager, builderMenu);

        builderMenu.setChatListener(builderChatListener);
        builderMenu.setColorsMenu(builderColorsMenu);
        builderMenu.setTypesMenu(builderTypesMenu);
        builderMenu.setParticlesMenu(builderParticlesMenu);

        this.showMenu = new ShowMenu(this, showService, showStorage, builderMenu);
        this.scheduleMenu = new ScheduleMenu(this, scheduleManager);
        this.languageMenu = new LanguageMenu(this);
        this.mainMenu = new MainMenu(this, showMenu, builderMenu, scheduleMenu, languageMenu, showStorage, scheduleManager);
        this.updateChecker = new UpdateChecker(this);
        this.updateListener = new UpdateListener(updateChecker);

        showMenu.setMainMenu(mainMenu);
        builderMenu.setMainMenu(mainMenu);
        scheduleMenu.setMainMenu(mainMenu);
        languageMenu.setMainMenu(mainMenu);

        Bukkit.getPluginManager().registerEvents(mainMenu, this);
        Bukkit.getPluginManager().registerEvents(showMenu, this);
        Bukkit.getPluginManager().registerEvents(builderMenu, this);
        Bukkit.getPluginManager().registerEvents(builderTypesMenu, this);
        Bukkit.getPluginManager().registerEvents(builderParticlesMenu, this);
        Bukkit.getPluginManager().registerEvents(scheduleMenu, this);
        Bukkit.getPluginManager().registerEvents(languageMenu, this);
        Bukkit.getPluginManager().registerEvents(builderColorsMenu, this);
        Bukkit.getPluginManager().registerEvents(builderChatListener, this);
        Bukkit.getPluginManager().registerEvents(updateListener, this);

        PluginCommand cmd = getCommand("fw");
        if (cmd != null) {
            cmd.setExecutor(new FwCommand(this, mainMenu, showMenu, showService, showStorage, draftManager, scheduleManager));
            cmd.setTabCompleter(new FwTabCompleter(this, showStorage, scheduleManager));
        }

        scheduleManager.startPolling();
        updateChecker.notifyOnlineAdminsIfOutdated();
        getLogger().info("FireworksPlus enabled.");
    }

    private void applyConfigDefaults() {
        boolean changed = false;

        if (!getConfig().isSet("main_gui.language_slot")) {
            getConfig().set("main_gui.language_slot", 13);
            changed = true;
        }

        if (!getConfig().isSet("main_gui.language_icon_texture")) {
            getConfig().set("main_gui.language_icon_texture", "https://textures.minecraft.net/texture/cf40942f364f6cbceffcf1151796410286a48b1aeba77243e218026c09cd1");
            changed = true;
        }

        changed |= ensureLanguageHeadDefaults("en");
        changed |= ensureLanguageHeadDefaults("es");
        changed |= ensureLanguageHeadDefaults("pl");
        changed |= ensureLanguageHeadDefaults("de");

        if (changed) {
            saveConfig();
        }
    }

    private boolean ensureLanguageHeadDefaults(String code) {
        boolean changed = false;
        String base = "language_heads." + code;

        if (!getConfig().isSet(base + ".texture")) {
            getConfig().set(base + ".texture", "");
            changed = true;
        }

        return changed;
    }

    public MainMenu getMainMenu() {
        return mainMenu;
    }

    public I18n getI18n() {
        return i18n;
    }

    public void reloadPluginData() {
        reloadConfig();
        showStorage.reload();
        scheduleManager.reload();
        if (i18n != null) {
            i18n.reload();
        }
    }
}