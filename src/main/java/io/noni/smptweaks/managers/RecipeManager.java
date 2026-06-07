package io.noni.smptweaks.managers;

import io.noni.smptweaks.SMPtweaks;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Recipe;

import java.io.File;
import java.util.*;

public class RecipeManager {
    private final SMPtweaks plugin;
    private final File file;
    private FileConfiguration config;
    private final Map<NamespacedKey, Recipe> originalRecipes = new LinkedHashMap<>();
    private final Set<NamespacedKey> disabledRecipes = new HashSet<>();

    public RecipeManager(SMPtweaks plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "disabled_recipes.yml");
        loadConfig();
    }

    /**
     * Load the list of disabled recipes from config
     */
    public void loadConfig() {
        if (!file.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                file.createNewFile();
            } catch (Exception e) {
                plugin.getLogger().warning("Could not create disabled_recipes.yml: " + e.getMessage());
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
        disabledRecipes.clear();
        List<String> list = config.getStringList("disabled");
        for (String keyStr : list) {
            NamespacedKey key = NamespacedKey.fromString(keyStr);
            if (key != null) {
                disabledRecipes.add(key);
            }
        }
    }

    /**
     * Save the list of disabled recipes to config
     */
    public void saveConfig() {
        List<String> list = new ArrayList<>();
        for (NamespacedKey key : disabledRecipes) {
            list.add(key.toString());
        }
        config.set("disabled", list);
        try {
            config.save(file);
        } catch (Exception e) {
            plugin.getLogger().warning("Could not save disabled_recipes.yml: " + e.getMessage());
        }
    }

    /**
     * Cache all original server recipes and apply initial disabled states.
     */
    public void initializeRecipes() {
        originalRecipes.clear();
        updateRecipeCache();
    }

    /**
     * Scan the server's recipe registry for new recipes and add them to our cache.
     * Also immediately removes any new recipes that are supposed to be disabled.
     */
    public void updateRecipeCache() {
        Iterator<Recipe> iterator = Bukkit.recipeIterator();
        boolean removedAny = false;
        while (iterator.hasNext()) {
            Recipe recipe = iterator.next();
            if (recipe instanceof Keyed) {
                NamespacedKey key = ((Keyed) recipe).getKey();
                if (!originalRecipes.containsKey(key)) {
                    originalRecipes.put(key, recipe);
                    if (disabledRecipes.contains(key)) {
                        Bukkit.removeRecipe(key);
                        removedAny = true;
                    }
                }
            }
        }
        if (removedAny) {
            Bukkit.updateRecipes();
        }
    }

    /**
     * Restores all cached recipes back to the server
     */
    public void restoreAllRecipes() {
        for (Map.Entry<NamespacedKey, Recipe> entry : originalRecipes.entrySet()) {
            if (Bukkit.getRecipe(entry.getKey()) == null) {
                try {
                    Bukkit.addRecipe(entry.getValue());
                } catch (Exception e) {
                    // Ignore or log if it fails
                }
            }
        }
        Bukkit.updateRecipes();
    }

    public boolean isRecipeDisabled(NamespacedKey key) {
        return disabledRecipes.contains(key);
    }

    /**
     * Disables a recipe
     */
    public void disableRecipe(NamespacedKey key) {
        if (disabledRecipes.add(key)) {
            saveConfig();
            Bukkit.removeRecipe(key);
            Bukkit.updateRecipes();
        }
    }

    /**
     * Re-enables a recipe
     */
    public void enableRecipe(NamespacedKey key) {
        if (disabledRecipes.remove(key)) {
            saveConfig();
            Recipe original = originalRecipes.get(key);
            if (original != null) {
                try {
                    Bukkit.addRecipe(original);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to re-add recipe " + key + ": " + e.getMessage());
                }
            }
            Bukkit.updateRecipes();
        }
    }

    public Map<NamespacedKey, Recipe> getOriginalRecipes() {
        return originalRecipes;
    }

    public Set<NamespacedKey> getDisabledRecipes() {
        return disabledRecipes;
    }
}
