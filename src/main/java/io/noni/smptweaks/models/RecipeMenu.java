package io.noni.smptweaks.models;

import io.noni.smptweaks.SMPtweaks;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class RecipeMenu implements InventoryHolder {
    private final Player player;
    private final List<Recipe> filteredRecipes;
    private int page;
    private final String searchQuery;
    private Inventory inventory;

    public RecipeMenu(Player player, List<Recipe> filteredRecipes, int page, String searchQuery) {
        this.player = player;
        this.filteredRecipes = filteredRecipes;
        this.page = page;
        this.searchQuery = searchQuery;
    }

    public void open() {
        int totalPages = getTotalPages();
        if (page >= totalPages) {
            page = totalPages - 1;
        }
        if (page < 0) {
            page = 0;
        }

        String title = "Recipe Disabler";
        if (searchQuery != null && !searchQuery.isEmpty()) {
            title = "Recipes: \"" + searchQuery + "\"";
        }
        
        // Ensure Title is not too long
        if (title.length() > 32) {
            title = title.substring(0, 29) + "...";
        }

        this.inventory = Bukkit.createInventory(this, 54, title);

        int startIndex = page * 45;
        int endIndex = Math.min(startIndex + 45, filteredRecipes.size());

        // Fill top 5 rows
        for (int i = startIndex; i < endIndex; i++) {
            Recipe recipe = filteredRecipes.get(i);
            NamespacedKey key = ((Keyed) recipe).getKey();
            boolean isDisabled = SMPtweaks.getPlugin().getRecipeManager().isRecipeDisabled(key);
            ItemStack item = getRecipeDisplayItem(recipe, isDisabled);
            inventory.setItem(i - startIndex, item);
        }

        // Fill bottom row with gray stained glass panes as borders/background
        ItemStack grayPane = new ItemStack(org.bukkit.Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta paneMeta = grayPane.getItemMeta();
        if (paneMeta != null) {
            paneMeta.setDisplayName(" ");
            grayPane.setItemMeta(paneMeta);
        }

        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, grayPane);
        }

        // Previous Page Button
        if (page > 0) {
            ItemStack prevButton = new ItemStack(org.bukkit.Material.ARROW);
            ItemMeta meta = prevButton.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§a§l← Previous Page");
                meta.setLore(List.of("§7Go to page " + page));
                prevButton.setItemMeta(meta);
            }
            inventory.setItem(45, prevButton);
        }

        // Next Page Button
        if (page < totalPages - 1) {
            ItemStack nextButton = new ItemStack(org.bukkit.Material.ARROW);
            ItemMeta meta = nextButton.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§a§lNext Page →");
                meta.setLore(List.of("§7Go to page " + (page + 2)));
                nextButton.setItemMeta(meta);
            }
            inventory.setItem(53, nextButton);
        }

        // Page indicator
        ItemStack pageIndicator = new ItemStack(org.bukkit.Material.PAPER);
        ItemMeta indicatorMeta = pageIndicator.getItemMeta();
        if (indicatorMeta != null) {
            indicatorMeta.setDisplayName("§e§lPage " + (page + 1) + " of " + totalPages);
            List<String> lore = new ArrayList<>();
            lore.add("§7Total matching: §f" + filteredRecipes.size());
            if (searchQuery != null && !searchQuery.isEmpty()) {
                lore.add("§7Filter: §f\"" + searchQuery + "\"");
            }
            indicatorMeta.setLore(lore);
            pageIndicator.setItemMeta(indicatorMeta);
        }
        inventory.setItem(49, pageIndicator);

        player.openInventory(inventory);
    }

    public int getTotalPages() {
        return Math.max(1, (int) Math.ceil((double) filteredRecipes.size() / 45));
    }

    private ItemStack getRecipeDisplayItem(Recipe recipe, boolean isDisabled) {
        ItemStack item = recipe.getResult();
        if (item == null || item.getType().isAir()) {
            item = new ItemStack(org.bukkit.Material.KNOWLEDGE_BOOK);
        } else {
            item = item.clone();
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            meta = Bukkit.getItemFactory().getItemMeta(item.getType());
        }

        if (meta != null) {
            String displayName = meta.hasDisplayName() ? meta.getDisplayName() : formatMaterialName(item.getType());
            if (isDisabled) {
                meta.setDisplayName("§c[DISABLED] " + displayName);
            } else {
                meta.setDisplayName("§a" + displayName);
            }

            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            // Clean up old recipe status lines if they somehow exist
            lore.removeIf(line -> line.contains("Key:") || line.contains("Type:") || line.contains("Status:") || line.contains("Click to Toggle") || line.contains("------"));
            
            lore.add("§8----------------------");
            NamespacedKey key = ((Keyed) recipe).getKey();
            lore.add("§7Key: §f" + key.toString());
            lore.add("§7Type: §f" + getRecipeTypeName(recipe));
            lore.add("§7Status: " + (isDisabled ? "§c§lDISABLED" : "§a§lENABLED"));
            lore.add("§8----------------------");
            lore.add("§eClick to Toggle Status");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    private String formatMaterialName(org.bukkit.Material material) {
        String name = material.name().replace('_', ' ').toLowerCase();
        String[] words = name.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    private String getRecipeTypeName(Recipe recipe) {
        if (recipe instanceof org.bukkit.inventory.ShapedRecipe) {
            return "Shaped Crafting";
        } else if (recipe instanceof org.bukkit.inventory.ShapelessRecipe) {
            return "Shapeless Crafting";
        } else if (recipe instanceof org.bukkit.inventory.FurnaceRecipe) {
            return "Furnace Smelting";
        } else if (recipe instanceof org.bukkit.inventory.BlastingRecipe) {
            return "Blast Furnace Cooking";
        } else if (recipe instanceof org.bukkit.inventory.SmokingRecipe) {
            return "Smoker Cooking";
        } else if (recipe instanceof org.bukkit.inventory.CampfireRecipe) {
            return "Campfire Cooking";
        } else if (recipe instanceof org.bukkit.inventory.StonecuttingRecipe) {
            return "Stonecutting";
        } else if (recipe instanceof org.bukkit.inventory.SmithingRecipe) {
            return "Smithing";
        } else if (recipe instanceof org.bukkit.inventory.ComplexRecipe) {
            return "Special Crafting";
        } else {
            return recipe.getClass().getSimpleName();
        }
    }

    public void toggleRecipeAtSlot(int slot) {
        int index = (page * 45) + slot;
        if (index >= filteredRecipes.size()) return;
        Recipe recipe = filteredRecipes.get(index);
        if (recipe instanceof Keyed) {
            NamespacedKey key = ((Keyed) recipe).getKey();
            var rm = SMPtweaks.getPlugin().getRecipeManager();
            boolean isDisabled = rm.isRecipeDisabled(key);
            if (isDisabled) {
                rm.enableRecipe(key);
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.2f);
            } else {
                rm.disableRecipe(key);
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.5f);
            }

            // Update the single slot
            ItemStack updatedItem = getRecipeDisplayItem(recipe, !isDisabled);
            inventory.setItem(slot, updatedItem);
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public Player getPlayer() {
        return player;
    }

    public List<Recipe> getFilteredRecipes() {
        return filteredRecipes;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public String getSearchQuery() {
        return searchQuery;
    }
}
