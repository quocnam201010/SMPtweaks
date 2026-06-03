package io.noni.smptweaks.recipes;

import io.noni.smptweaks.SMPtweaks;
import io.noni.smptweaks.utils.LoggingUtils;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Recipe;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class RecipeManager {
    private final SMPtweaks plugin;
    private final File configFile;
    private FileConfiguration config;

    private final Set<String> disabledRecipes = new LinkedHashSet<>();
    private final Map<String, List<String>> manualGroups = new LinkedHashMap<>();
    
    // Cache all registered recipes before unregistering them
    private final Map<String, Recipe> cachedOriginalRecipes = new LinkedHashMap<>();
    
    // Track players currently typing a search query in chat
    private final Map<UUID, String> activeSearches = new HashMap<>();

    public RecipeManager() {
        this.plugin = SMPtweaks.getPlugin();
        this.configFile = new File(plugin.getDataFolder(), "recipes.yml");
        
        // Save default from resources if it doesn't exist
        if (!configFile.exists()) {
            try {
                plugin.saveResource("recipes.yml", false);
            } catch (Exception e) {
                try {
                    configFile.createNewFile();
                } catch (IOException ioException) {
                    LoggingUtils.error("Could not create recipes.yml file");
                    ioException.printStackTrace();
                }
            }
        }
        
        // 1. Cache all recipes CURRENTLY in Bukkit (before we load config and remove them)
        cacheOriginalRecipes();

        // 2. Load configurations (which fills disabledRecipes list)
        loadConfig();

        // 3. Remove from registry
        applyRegistryRemovals();
    }

    /**
     * Cache vanilla and other registered recipes
     */
    private void cacheOriginalRecipes() {
        cachedOriginalRecipes.clear();
        Iterator<Recipe> it = Bukkit.recipeIterator();
        int count = 0;
        while (it.hasNext()) {
            Recipe r = it.next();
            if (r instanceof Keyed keyed) {
                String keyStr = keyed.getKey().toString().toLowerCase();
                cachedOriginalRecipes.put(keyStr, r);
                count++;
            }
        }
        LoggingUtils.info("Cached " + count + " recipes from Bukkit registry.");
    }

    /**
     * Loads the recipes.yml config and populates internal structures
     */
    public void loadConfig() {
        this.config = YamlConfiguration.loadConfiguration(configFile);
        
        this.disabledRecipes.clear();
        this.manualGroups.clear();

        // Load disabled recipes
        List<String> disabledList = config.getStringList("disabled");
        for (String keyStr : disabledList) {
            if (keyStr != null && !keyStr.isEmpty()) {
                disabledRecipes.add(keyStr.trim().toLowerCase());
            }
        }

        // Load manual groups
        ConfigurationSection groupsSection = config.getConfigurationSection("groups");
        if (groupsSection != null) {
            for (String groupKey : groupsSection.getKeys(false)) {
                List<String> recipeKeys = groupsSection.getStringList(groupKey);
                List<String> cleanedRecipeKeys = new ArrayList<>();
                for (String keyStr : recipeKeys) {
                    if (keyStr != null && !keyStr.isEmpty()) {
                        cleanedRecipeKeys.add(keyStr.trim().toLowerCase());
                    }
                }
                manualGroups.put(groupKey, cleanedRecipeKeys);
            }
        }
        
        LoggingUtils.info("Loaded " + disabledRecipes.size() + " disabled recipes and " + manualGroups.size() + " recipe groups.");
    }

    /**
     * Saves the current list of disabled recipes back to recipes.yml
     */
    public void saveConfig() {
        config.set("disabled", new ArrayList<>(disabledRecipes));
        try {
            config.save(configFile);
        } catch (IOException e) {
            LoggingUtils.error("Failed to save recipes.yml!");
            e.printStackTrace();
        }
    }

    /**
     * Cleanly unregisters disabled recipes from the server's recipe registry on startup/reload
     */
    public void applyRegistryRemovals() {
        int removedCount = 0;
        for (String keyStr : disabledRecipes) {
            NamespacedKey key = NamespacedKey.fromString(keyStr);
            if (key != null) {
                if (Bukkit.removeRecipe(key)) {
                    removedCount++;
                }
            }
        }
        if (removedCount > 0) {
            LoggingUtils.info("Unregistered " + removedCount + " recipes from the server registry.");
        }
    }

    /**
     * Check if a recipe is disabled
     */
    public boolean isRecipeDisabled(NamespacedKey key) {
        if (key == null) return false;
        return disabledRecipes.contains(key.toString().toLowerCase());
    }

    /**
     * Check if a recipe is disabled by string
     */
    public boolean isRecipeDisabled(String keyStr) {
        if (keyStr == null) return false;
        return disabledRecipes.contains(keyStr.toLowerCase());
    }

    /**
     * Toggles a recipe state
     */
    public void setRecipeDisabled(NamespacedKey key, boolean disable) {
        if (key == null) return;
        String keyStr = key.toString().toLowerCase();
        
        if (disable) {
            disabledRecipes.add(keyStr);
            Bukkit.removeRecipe(key);
            undiscoverRecipeForEveryone(key);
        } else {
            disabledRecipes.remove(keyStr);
        }
        saveConfig();
    }

    /**
     * Undiscover a recipe for all online players
     */
    public void undiscoverRecipeForEveryone(NamespacedKey key) {
        if (key == null) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.undiscoverRecipe(key);
        }
    }

    /**
     * Remove all disabled recipes from a player's recipe book (e.g. on join)
     */
    public void undiscoverDisabledRecipes(Player player) {
        if (player == null) return;
        for (String keyStr : disabledRecipes) {
            NamespacedKey key = NamespacedKey.fromString(keyStr);
            if (key != null) {
                player.undiscoverRecipe(key);
            }
        }
    }

    /**
     * Gets a map of all manual groups
     */
    public Map<String, List<String>> getManualGroups() {
        return manualGroups;
    }

    /**
     * Gets the list of disabled recipes (as string list)
     */
    public Set<String> getDisabledRecipes() {
        return disabledRecipes;
    }

    /**
     * Batch toggle a list of recipe keys
     */
    public void toggleRecipes(Collection<String> keys, boolean disable) {
        for (String keyStr : keys) {
            NamespacedKey key = NamespacedKey.fromString(keyStr);
            if (key != null) {
                if (disable) {
                    disabledRecipes.add(keyStr);
                    Bukkit.removeRecipe(key);
                    undiscoverRecipeForEveryone(key);
                } else {
                    disabledRecipes.remove(keyStr);
                }
            }
        }
        saveConfig();
    }

    /**
     * Returns a sorted list of all original recipes, merging any new ones added after startup
     */
    public synchronized List<Recipe> getAllOriginalRecipes() {
        Iterator<Recipe> it = Bukkit.recipeIterator();
        while (it.hasNext()) {
            Recipe r = it.next();
            if (r instanceof Keyed keyed) {
                String keyStr = keyed.getKey().toString().toLowerCase();
                if (!cachedOriginalRecipes.containsKey(keyStr)) {
                    cachedOriginalRecipes.put(keyStr, r);
                }
            }
        }

        List<Recipe> sortedList = new ArrayList<>(cachedOriginalRecipes.values());
        sortedList.sort((r1, r2) -> {
            int comp = r1.getResult().getType().name().compareTo(r2.getResult().getType().name());
            if (comp != 0) return comp;
            if (r1 instanceof Keyed k1 && r2 instanceof Keyed k2) {
                return k1.getKey().toString().compareTo(k2.getKey().toString());
            }
            return 0;
        });
        return sortedList;
    }

    /**
     * Start chat search input capture for a player
     */
    public void startSearch(Player player, String currentView) {
        activeSearches.put(player.getUniqueId(), currentView);
    }

    /**
     * Check if player has an active search prompt
     */
    public boolean isSearching(Player player) {
        return activeSearches.containsKey(player.getUniqueId());
    }

    /**
     * End search capture and open filtered GUI view
     */
    public void endSearch(Player player, String query) {
        String previousView = activeSearches.remove(player.getUniqueId());

        if (query.equalsIgnoreCase("cancel")) {
            player.sendMessage("§cSearch cancelled.");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 1.0f);
            
            // Re-open GUI to previous view or main menu
            RecipeGUI gui = new RecipeGUI(player, this);
            if (previousView != null) {
                gui.openListView(previousView);
            }
            gui.open();
            return;
        }

        player.sendMessage("§aSearching recipes for: §f" + query);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.5f);
        
        RecipeGUI gui = new RecipeGUI(player, this);
        gui.openSearchView(query);
        gui.open();
    }
}
