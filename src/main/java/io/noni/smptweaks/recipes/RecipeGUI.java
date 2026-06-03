package io.noni.smptweaks.recipes;

import io.noni.smptweaks.SMPtweaks;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RecipeGUI implements InventoryHolder {
    private final Player player;
    private final Inventory inventory;
    private final RecipeManager recipeManager;

    private String currentView = null; // null = Main Menu, "all", "enabled", "disabled", group name, or "search:query"
    private int page = 0;
    private List<Recipe> cachedRecipes = new ArrayList<>();

    public RecipeGUI(Player player, RecipeManager recipeManager) {
        this.player = player;
        this.recipeManager = recipeManager;
        this.inventory = Bukkit.createInventory(this, 54, "Recipe Manager");
        openMainMenu();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        player.openInventory(inventory);
    }

    /**
     * Build and display the main menu
     */
    public void openMainMenu() {
        this.currentView = null;
        this.page = 0;
        this.cachedRecipes.clear();
        inventory.clear();

        // 1. Fill background with gray glass panes
        ItemStack border = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8 || (i >= 18 && i < 27)) {
                inventory.setItem(i, border);
            }
        }

        // 2. Add filters
        List<Recipe> allRecipes = recipeManager.getAllOriginalRecipes();
        int totalRecipes = allRecipes.size();
        int disabledCount = 0;
        for (Recipe r : allRecipes) {
            if (r instanceof Keyed keyed && recipeManager.isRecipeDisabled(keyed.getKey())) {
                disabledCount++;
            }
        }
        int enabledCount = totalRecipes - disabledCount;

        // Filter: All (Slot 11)
        ItemStack allFilter = createGuiItem(Material.BOOK, "§a§lAll Recipes",
                "§7Total: §f" + totalRecipes,
                " ",
                "§eLeft-click to view all recipes"
        );
        inventory.setItem(11, allFilter);

        // Filter: Enabled (Slot 13)
        ItemStack enabledFilter = createGuiItem(Material.EMERALD, "§a§lEnabled Recipes",
                "§7Active: §f" + enabledCount,
                " ",
                "§eLeft-click to view active recipes"
        );
        inventory.setItem(13, enabledFilter);

        // Filter: Disabled (Slot 15)
        ItemStack disabledFilter = createGuiItem(Material.BARRIER, "§c§lDisabled Recipes",
                "§7Disabled: §c" + disabledCount,
                " ",
                "§eLeft-click to view disabled recipes"
        );
        inventory.setItem(15, disabledFilter);

        // 3. Add Search Button (Slot 40)
        ItemStack searchBtn = createGuiItem(Material.COMPASS, "§6§lSearch Recipes",
                "§7Search recipes globally by name or key.",
                " ",
                "§eClick to type query in chat"
        );
        inventory.setItem(40, searchBtn);

        // 4. Add manual groups (starting at slot 28)
        int slot = 28; // Start middle row
        Map<String, List<String>> groups = recipeManager.getManualGroups();
        for (Map.Entry<String, List<String>> entry : groups.entrySet()) {
            if (slot >= 44) break; // Don't overflow the middle row
            
            String groupName = entry.getKey();
            List<String> groupRecipeKeys = entry.getValue();
            
            // Get stats for group
            int groupTotal = groupRecipeKeys.size();
            int groupDisabled = 0;
            for (String keyStr : groupRecipeKeys) {
                if (recipeManager.isRecipeDisabled(keyStr)) {
                    groupDisabled++;
                }
            }

            // Figure out group icon: use first recipe's result material if possible, else Chest
            Material iconMaterial = Material.CHEST;
            if (!groupRecipeKeys.isEmpty()) {
                String firstKey = groupRecipeKeys.get(0);
                NamespacedKey nsk = NamespacedKey.fromString(firstKey);
                if (nsk != null) {
                    // Look up from cachedOriginalRecipes instead of Bukkit.getRecipe
                    for (Recipe r : allRecipes) {
                        if (r instanceof Keyed keyed && keyed.getKey().equals(nsk)) {
                            iconMaterial = r.getResult().getType();
                            break;
                        }
                    }
                }
            }

            String formattedName = formatName(groupName);
            ItemStack groupItem = createGuiItem(iconMaterial, "§d§lGroup: " + formattedName,
                    "§7Recipes in group: §f" + groupTotal,
                    "§7Disabled: §c" + groupDisabled,
                    " ",
                    "§eLeft-click to view recipes",
                    "§bRight-click to ENABLE all",
                    "§cShift-click to DISABLE all"
            );
            inventory.setItem(slot, groupItem);
            slot++;
        }

        // Help Information (Slot 49)
        ItemStack helpInfo = createGuiItem(Material.PAPER, "§e§lHelp & Information",
                "§7Click on any recipe to toggle it.",
                "§7Disabled recipes cannot be discovered,",
                "§7crafted, or made by autocrafters.",
                " ",
                "§cDynamic blocking works instantly."
        );
        inventory.setItem(49, helpInfo);
    }

    /**
     * Setup GUI view for search filter results
     */
    public void openSearchView(String query) {
        openListView("search:" + query.toLowerCase().trim());
    }

    /**
     * Build and display the paginated recipe list for a filter or group
     */
    public void openListView(String viewName) {
        this.currentView = viewName;
        inventory.clear();

        // 1. Fetch matching recipes
        List<Recipe> allRecipes = recipeManager.getAllOriginalRecipes();
        this.cachedRecipes = new ArrayList<>();

        if (viewName.equals("all")) {
            cachedRecipes.addAll(allRecipes);
        } else if (viewName.equals("enabled")) {
            for (Recipe r : allRecipes) {
                if (r instanceof Keyed keyed && !recipeManager.isRecipeDisabled(keyed.getKey())) {
                    cachedRecipes.add(r);
                }
            }
        } else if (viewName.equals("disabled")) {
            for (Recipe r : allRecipes) {
                if (r instanceof Keyed keyed && recipeManager.isRecipeDisabled(keyed.getKey())) {
                    cachedRecipes.add(r);
                }
            }
        } else if (viewName.startsWith("search:")) {
            String query = viewName.substring(7);
            for (Recipe r : allRecipes) {
                if (r instanceof Keyed keyed) {
                    String key = keyed.getKey().toString().toLowerCase();
                    String outputMat = r.getResult().getType().name().toLowerCase();
                    if (key.contains(query) || outputMat.contains(query)) {
                        cachedRecipes.add(r);
                    }
                }
            }
        } else {
            // It's a manual group
            List<String> groupKeys = recipeManager.getManualGroups().get(viewName);
            if (groupKeys != null) {
                for (Recipe r : allRecipes) {
                    if (r instanceof Keyed keyed && groupKeys.contains(keyed.getKey().toString().toLowerCase())) {
                        cachedRecipes.add(r);
                    }
                }
            }
        }

        // 2. Setup navigation row (Row 5: slots 45-53)
        ItemStack border = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, border);
        }

        // Slot 45: Back
        inventory.setItem(45, createGuiItem(Material.ARROW, "§c« Back to Menu"));

        // Slot 48: Prev Page
        if (page > 0) {
            inventory.setItem(48, createGuiItem(Material.SPECTRAL_ARROW, "§e« Previous Page"));
        }

        // Slot 49: Page Info
        int totalRecipes = cachedRecipes.size();
        int maxPages = (int) Math.ceil((double) totalRecipes / 45.0);
        if (maxPages == 0) maxPages = 1;
        
        if (viewName.startsWith("search:")) {
            String query = viewName.substring(7);
            inventory.setItem(49, createGuiItem(Material.PAPER, "§bSearch: §f" + query,
                    "§7Results found: §f" + totalRecipes,
                    "§7Page: §f" + (page + 1) + " of " + maxPages
            ));
        } else {
            inventory.setItem(49, createGuiItem(Material.PAPER, "§bPage " + (page + 1) + " of " + maxPages,
                    "§7Total items in view: §f" + totalRecipes
            ));
        }

        // Slot 50: Next Page
        if (page + 1 < maxPages) {
            inventory.setItem(50, createGuiItem(Material.SPECTRAL_ARROW, "§eNext Page »"));
        }

        // Slot 51: Search (Compass icon)
        inventory.setItem(51, createGuiItem(Material.COMPASS, "§6§lNew Search",
                "§7Search recipes globally by name or key.",
                " ",
                "§eClick to type query in chat"
        ));

        // Slot 53: Batch Toggle
        inventory.setItem(53, createGuiItem(Material.LEVER, "§bBatch Options",
                "§eLeft-click to ENABLE all in this view",
                "§cShift-click to DISABLE all in this view"
        ));

        // 3. Render recipes for current page
        int startIndex = page * 45;
        int endIndex = Math.min(startIndex + 45, totalRecipes);

        for (int i = startIndex; i < endIndex; i++) {
            int guiSlot = i - startIndex;
            Recipe recipe = cachedRecipes.get(i);
            
            // Clone/create the item stack representation
            ItemStack item = recipe.getResult().clone();
            ItemMeta meta = item.getItemMeta();
            
            if (meta != null) {
                List<String> lore = meta.getLore();
                if (lore == null) lore = new ArrayList<>();
                
                NamespacedKey key = ((Keyed) recipe).getKey();
                boolean disabled = recipeManager.isRecipeDisabled(key);
                
                lore.add(" ");
                lore.add("§7Key: §f" + key.toString());
                if (disabled) {
                    lore.add("§7Status: §c§lDISABLED");
                    meta.setDisplayName("§m" + (meta.hasDisplayName() ? meta.getDisplayName() : formatMaterialName(item.getType())));
                } else {
                    lore.add("§7Status: §a§lENABLED");
                }
                lore.add(" ");
                lore.add("§eClick to toggle status");
                
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inventory.setItem(guiSlot, item);
        }
    }

    /**
     * Handles clicks inside the inventory
     */
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        // Sound representation
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);

        if (currentView == null) {
            // Main menu click logic
            if (slot == 11) {
                openListView("all");
            } else if (slot == 13) {
                openListView("enabled");
            } else if (slot == 15) {
                openListView("disabled");
            } else if (slot == 40) {
                // Search Triggered from Main Menu
                player.closeInventory();
                recipeManager.startSearch(player, null);
                player.sendMessage("§6==========================================");
                player.sendMessage("§aEnter search term in chat (e.g. §firon§a).");
                player.sendMessage("§aType §ccancel§a to exit search mode.");
                player.sendMessage("§6==========================================");
            } else if (slot >= 28 && slot < 45) {
                ItemStack item = inventory.getItem(slot);
                if (item != null && item.getType() != Material.GRAY_STAINED_GLASS_PANE) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null && meta.getDisplayName().startsWith("§d§lGroup: ")) {
                        String cleanName = ChatColor.stripColor(meta.getDisplayName())
                                .replace("Group: ", "")
                                .toLowerCase()
                                .replace(" ", "_");
                        
                        if (recipeManager.getManualGroups().containsKey(cleanName)) {
                            if (event.isShiftClick()) {
                                recipeManager.toggleRecipes(recipeManager.getManualGroups().get(cleanName), true);
                                player.sendMessage("§cDisabled all recipes in group " + formatName(cleanName));
                                player.playSound(player.getLocation(), Sound.ENTITY_BAT_DEATH, 0.8f, 0.8f);
                                openMainMenu();
                            } else if (event.isRightClick()) {
                                recipeManager.toggleRecipes(recipeManager.getManualGroups().get(cleanName), false);
                                player.sendMessage("§aEnabled all recipes in group " + formatName(cleanName));
                                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
                                openMainMenu();
                            } else {
                                openListView(cleanName);
                            }
                        }
                    }
                }
            }
        } else {
            // List view click logic
            if (slot == 45) {
                openMainMenu();
            } else if (slot == 48 && inventory.getItem(48) != null) {
                page--;
                openListView(currentView);
            } else if (slot == 50 && inventory.getItem(50) != null) {
                page++;
                openListView(currentView);
            } else if (slot == 51) {
                // Search Triggered from List View
                player.closeInventory();
                recipeManager.startSearch(player, currentView);
                player.sendMessage("§6==========================================");
                player.sendMessage("§aEnter search term in chat (e.g. §firon§a).");
                player.sendMessage("§aType §ccancel§a to exit search mode.");
                player.sendMessage("§6==========================================");
            } else if (slot == 53) {
                boolean disable = event.isShiftClick();
                List<String> keysToToggle = new ArrayList<>();
                for (Recipe r : cachedRecipes) {
                    if (r instanceof Keyed keyed) {
                        keysToToggle.add(keyed.getKey().toString().toLowerCase());
                    }
                }
                recipeManager.toggleRecipes(keysToToggle, disable);
                player.sendMessage(disable ? "§cDisabled " + keysToToggle.size() + " recipes." : "§aEnabled " + keysToToggle.size() + " recipes.");
                player.playSound(player.getLocation(), disable ? Sound.ENTITY_BAT_DEATH : Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.0f);
                openListView(currentView);
            } else if (slot < 45) {
                int targetIndex = page * 45 + slot;
                if (targetIndex < cachedRecipes.size()) {
                    Recipe recipe = cachedRecipes.get(targetIndex);
                    if (recipe instanceof Keyed keyed) {
                        NamespacedKey key = keyed.getKey();
                        boolean wasDisabled = recipeManager.isRecipeDisabled(key);
                        recipeManager.setRecipeDisabled(key, !wasDisabled);
                        
                        player.playSound(player.getLocation(), wasDisabled ? Sound.ENTITY_EXPERIENCE_ORB_PICKUP : Sound.BLOCK_ANVIL_LAND, 0.6f, 1.5f);
                        openListView(currentView); // Refresh list immediately
                    }
                }
            }
        }
    }

    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> list = new ArrayList<>();
            for (String l : lore) {
                list.add(l);
            }
            meta.setLore(list);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String formatName(String raw) {
        String[] parts = raw.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(part.substring(0, 1).toUpperCase())
                  .append(part.substring(1).toLowerCase())
                  .append(" ");
            }
        }
        return sb.toString().trim();
    }

    private String formatMaterialName(Material material) {
        return formatName(material.name());
    }
}
