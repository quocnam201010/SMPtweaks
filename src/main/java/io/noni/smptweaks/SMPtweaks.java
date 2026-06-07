package io.noni.smptweaks;

import io.noni.smptweaks.commands.*;
import io.noni.smptweaks.database.DatabaseManager;
import io.noni.smptweaks.events.*;
import io.noni.smptweaks.managers.RecipeManager;
import io.noni.smptweaks.models.ConfigCache;
import io.noni.smptweaks.tasks.*;
import io.noni.smptweaks.utils.LoggingUtils;
import io.noni.smptweaks.utils.TranslationUtils;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Recipe;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Stream;

public final class SMPtweaks extends JavaPlugin {
    private static SMPtweaks plugin;
    private static DatabaseManager databaseManager;
    private static FileConfiguration config;
    private static ConfigCache configCache;
    private static RecipeManager recipeManager;
    private static Map<String, String> translations;
    private static Map<UUID, UUID> playerTrackers = new HashMap<>();
    private static List<UUID> coordinateDisplays = new ArrayList<>();
    private boolean isPaperServer = false;

    /**
     * Plugin startup logic
     */
    @Override
    @SuppressWarnings("ConstantConditions")
    public void onEnable() {
        // Variable for checking startup duration
        long startingTime = System.currentTimeMillis();

        // Static reference to plugin
        plugin = this;

        // Check if optional Paper classes are available
        try {
            Class.forName("com.destroystokyo.paper.event.entity.PreCreatureSpawnEvent");
            Class.forName("com.destroystokyo.paper.event.entity.PhantomPreSpawnEvent");
            isPaperServer = true;
            LoggingUtils.info("Paper-API detected! SMPtweaks will use Paper events");
        } catch (ClassNotFoundException e) {
            LoggingUtils.info("This server doesn't seem to run Paper or a Paper-fork, falling back to using Spigot events");
        }

        // Copy default config files
        getConfig().options().copyDefaults();
        saveDefaultConfig();

        // Static reference to config
        config = getConfig();
        configCache = new ConfigCache();
        recipeManager = new RecipeManager(this);
        Bukkit.getScheduler().runTaskLater(this, () -> recipeManager.initializeRecipes(), 1L);

        // Static reference to Hikari
        databaseManager = new DatabaseManager();

        // Load translations
        var languageCode = config.getString("language");
        translations = TranslationUtils.loadTranslations(languageCode);

        // Register everything (events, recipes, tasks)
        registerAll();

        //
        // Register Commands
        //
        var smpCommand = new SMPtweaksCommand();
        getCommand("smptweaks").setExecutor(smpCommand);
        getCommand("smptweaks").setTabCompleter(smpCommand);
        getCommand("whereis").setExecutor(new WhereisCommand());
        getCommand("collect").setExecutor(new CollectCommand());
        getCommand("track").setExecutor(new TrackCommand());
        getCommand("coords").setExecutor(new CoordsCommand());
        getCommand("level").setExecutor(new LevelCommand());
        getCommand("level").setTabCompleter(new LevelTab());

        var recipesCommand = new RecipesCommand();
        getCommand("recipes").setExecutor(recipesCommand);
        getCommand("recipes").setTabCompleter(recipesCommand);

        // Include bStats
        new Metrics(this, 11736);

        //
        // Done :)
        //
        LoggingUtils.info("Up and running! Startup took " + (System.currentTimeMillis() - startingTime) + "ms");
    }

    /**
     * Register events, recipes, placeholders and tasks based on config
     */
    private void registerAll() {
        //
        // Register Event Listeners
        //
        Stream.of(
            config.getBoolean("disable_night_skip")
                    ? new TimeSkip() : null,

            config.getBoolean("disable_night_skip")
                    ? new PlayerBedEnter() : null,

            config.getBoolean("disable_night_skip")
                    ? new PlayerBedLeave() : null,

            config.getBoolean("remove_xp_on_death.enabled") ||
            config.getBoolean("remove_inventory_on_death.enabled") ||
            config.getBoolean("remove_equipment_on_death.enabled") ||
            config.getBoolean("decrease_item_durability_on_death.enabled") ||
            config.getBoolean("decrease_max_health_on_hunger_death.enabled")
                    ? new PlayerDeath() : null,

            config.getInt("respawn_health") != 20 ||
            config.getInt("respawn_food_level") != 20 ||
            config.getBoolean("decrease_max_health_on_hunger_death.enabled")
                    ? new PlayerRespawn() : null,

            config.getDouble("xp_multiplier") != 1
                    ? new PlayerExpChange() : null,

            config.getDouble("mending_repair_amount_multiplier") != 1
                    ? new PlayerItemMend() : null,

            config.getBoolean("server_levels.enabled")
                    ? new PlayerExpPickup() : null,

            config.getBoolean("buff_vegetarian_food")
                    ? new PlayerItemConsume() : null,

            config.getBoolean("disable_too_expensive_repairs")
                    ? new AnvilInventoryClickEvent() : null,

            config.getBoolean("server_levels.enabled") ||
            config.getBoolean("rewards.enabled") ||
            config.getBoolean("decrease_max_health_on_hunger_death.enabled")
                    ? new PlayerJoin() : null,

            config.getBoolean("spawn_rates.enabled")
                    ? (isPaperServer ? new PaperPreCreatureSpawn() : new CreatureSpawn()) : null,

            config.getBoolean("shulkers_spawn_naturally")
                    ? (isPaperServer ? new PaperShulkerSpawn() : new ShulkerSpawn()) : null,

            config.getBoolean("spawn_rates.enabled") &&
            configCache.getEntitySpawnRates().containsKey(EntityType.PHANTOM)
                    ? (isPaperServer ? new PaperPhantomPreSpawn() : null) : null,

            config.getBoolean("custom_drops.enabled")
                    ? new EntityDeath() : null,

            config.getBoolean("better_tipped_arrows")
                    ? new EntityDamageByEntity() : null,

            config.getBoolean("better_tipped_arrows")
                    ? new ProjectileLaunch() : null,

            config.getBoolean("server_levels.enabled") ||
            config.getBoolean("rewards.enabled") ||
            config.getBoolean("decrease_max_health_on_hunger_death.enabled")
                    ? new PlayerLeave() : null,

            config.getBoolean("disable_animal_breeding.enabled")
                    ? new EntityBreed() : null,

            config.getBoolean("disable_natural_growth.enabled") ||
            config.getBoolean("disable_bonemeal_fertilization.enabled")
                    ? new BlockGrowthLimit() : null,

            config.getBoolean("enable_commands.track")
                    ? new TrackedPlayerLeave() : null,

            config.getBoolean("fun_fishing.enabled")
                    ? new PlayerFish() : null,

            config.getBoolean("disable_trading.enabled")
                    ? new MerchantTrading() : null,

            new RecipeMenuListener()
        ).forEach(this::registerEvent);

        //
        // Register Recipes
        //
        if(config.getBoolean("custom_recipes.enabled")) {
            configCache.getShapedRecipes().forEach(this::registerRecipe);
            configCache.getShapelessRecipes().forEach(this::registerRecipe);
        }

        //
        // Register PlaceholderExpansions
        //
        if(config.getBoolean("server_levels.enabled") && config.getBoolean("papi_placeholders.enabled")) {
            registerPlaceholders();
        }

        //
        // Schedule tasks
        //
        if(config.getInt("day_duration_modifier") != 0 || config.getInt("night_duration_modifier") != 0) {
            Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new TimeModifierTask(
                    config.getInt("day_duration_modifier"),
                    config.getInt("night_duration_modifier")
            ), 0L, 2L);
        }
        if(config.getBoolean("clear_weather_at_dawn")) {
            Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new WeatherClearerTask(), 0L, 200L);
        }
        if(config.getBoolean("enable_commands.track")) {
            Bukkit.getScheduler().scheduleAsyncRepeatingTask(this, new TrackerUpdateTask(), 0L, 10L);
        }
        if(config.getBoolean("enable_commands.coords")) {
            Bukkit.getScheduler().scheduleAsyncRepeatingTask(this, new CoordsDisplayTask(), 0L, 10L);
        }
    }

    public void reloadPlugin() {
        LoggingUtils.info("Reloading plugin configuration...");

        // Restore all disabled recipes before reloading
        if (recipeManager != null) {
            recipeManager.restoreAllRecipes();
        }

        // 1. Turn daylight cycle back on if it was set
        if (config != null && config.getInt("day_duration_modifier") != 0) {
            try {
                Bukkit.getWorlds().get(0).setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
            } catch (Exception e) {
                // Ignore if world is not loaded yet
            }
        }

        // 2. Unregister event listeners
        org.bukkit.event.HandlerList.unregisterAll(this);

        // 3. Cancel all tasks
        Bukkit.getScheduler().cancelTasks(this);

        // 4. Remove custom recipes
        if (configCache != null) {
            for (org.bukkit.inventory.ShapedRecipe recipe : configCache.getShapedRecipes()) {
                Bukkit.removeRecipe(recipe.getKey());
            }
            for (org.bukkit.inventory.ShapelessRecipe recipe : configCache.getShapelessRecipes()) {
                Bukkit.removeRecipe(recipe.getKey());
            }
        }

        // 5. Close database manager
        if (databaseManager != null && databaseManager.getHikariDataSource() != null) {
            databaseManager.getHikariDataSource().close();
        }

        // 6. Reload config from file system
        reloadConfig();
        config = getConfig();

        // 7. Re-initialize database, config cache, translations
        databaseManager = new DatabaseManager();
        configCache = new ConfigCache();
        recipeManager = new RecipeManager(this);
        Bukkit.getScheduler().runTaskLater(this, () -> recipeManager.initializeRecipes(), 1L);

        var languageCode = config.getString("language");
        translations = TranslationUtils.loadTranslations(languageCode);

        // 8. Re-register events, recipes, tasks
        registerAll();

        LoggingUtils.info("Plugin reloaded successfully.");
    }

    /**
     * Register events
     * @param listener The Listener to register
     */
    private void registerEvent(@Nullable Listener listener) {
        if(listener != null) {
            getServer().getPluginManager().registerEvents(listener, this);
        }
    }

    /**
     * Register recipes
     * @param recipe The Recipe to add to Bukkit
     */
    private void registerRecipe(@Nullable Recipe recipe) {
        if(recipe != null) {
            Bukkit.addRecipe(recipe);
        }
    }

    /**
     * Register placeholders
     */
    private void registerPlaceholders() {
        if(getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            LoggingUtils.warn("Unable to find PlaceholderAPI. Is it installed?");
            return;
        }

        if(!(new io.noni.smptweaks.placeholders.LevelExpansion().register())) {
            LoggingUtils.warn("Unable to register PlaceholderAPI expansion");
        }
    }

    /**
     * Plugin shutdown logic
     */
    @Override
    public void onDisable() {
        // Make sure Daylight cycle is turned back on
        if(config.getInt("day_duration_modifier") != 0) {
            Bukkit.getWorlds().get(0).setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
        }

        // Store all player data
        if(
            config.getBoolean("server_levels.enabled") ||
            config.getBoolean("rewards.enabled")
        ) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                new PlayerMetaStorerTask(player).run();
            }
        }
    }

    /**
     * Get reference to this plugin
     * @return Plugin
     */
    public static SMPtweaks getPlugin() {
        return plugin;
    }

    /**
     * Get reference to DB
     * @return DatabaseManager
     */
    public static DatabaseManager getDB() {
        return databaseManager;
    }

    /**
     * Get reference to config
     * @return FileConfiguration
     */
    public static FileConfiguration getCfg() {
        return config;
    }

    /**
     * Get reference to cached config
     * @return ConfigCache
     */
    public static ConfigCache getConfigCache() {
        return configCache;
    }

    /**
     * Get reference to recipe manager
     * @return RecipeManager
     */
    public static RecipeManager getRecipeManager() {
        return recipeManager;
    }

    /**
     * Get translations map
     * @return Translations
     */
    public static Map<String, String> getTranslations() {
        return translations;
    }

    /**
     * Get playerTrackers map
     * @return playerTrackers
     */
    public static Map<UUID, UUID> getPlayerTrackers() {
        return playerTrackers;
    }

    /**
     * Get coordinateDisplays list
     * @return coordinateDisplays
     */
    public static List<UUID> getCoordinateDisplays() { return coordinateDisplays; }
}
