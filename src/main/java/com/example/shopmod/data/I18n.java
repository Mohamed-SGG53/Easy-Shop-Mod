package com.example.shopmod.data;

import java.util.HashMap;
import java.util.Map;

public class I18n {
    
    private static final Map<String, String> EN = new HashMap<>();
    
    static {
        // English
        EN.put("shop.title", "%s - Shop Manager");
        EN.put("shop.storage", "Storage");
        EN.put("shop.set_price", "Set Price");
        EN.put("shop.add_offer", "Add Offer");
        EN.put("shop.close", "Close");
        EN.put("shop.sell_item", "Sell Item:");
        EN.put("shop.price", "Price:");
        EN.put("shop.offers", "Offers");
        EN.put("shop.no_offers", "No offers yet");
        EN.put("shop.page", "Page %d/%d");
        EN.put("shop.items", "%d items");
        EN.put("shop.add_buyable", "Add a Buyable Item:");
        EN.put("shop.set_price_label", "Set Price:");
        
        EN.put("msg.shop_created", "§aShop created: §e%s's Shop §a✔");
        EN.put("msg.shop_exists", "§cYou already have a shop! Use §e/close_shop §cto remove it first.");
        EN.put("msg.shop_closed", "§aShop closed successfully! §7(Your items are saved)");
        EN.put("msg.shop_not_found", "§cYou don't have a shop!");
        EN.put("msg.shop_npc_not_found", "§eShop NPC was not found, data cleared.");
        EN.put("msg.shop_not_exists", "§cShop §e%s §cnot found!");
        EN.put("msg.no_shops", "§7No shops yet.");
        EN.put("msg.shop_list_header", "§6══════ Shop List ══════");
        EN.put("msg.shop_list_item", "§a• §f%s's Shop §7(%d offers)");
        
        EN.put("msg.offer_added", "§aOffer added successfully!");
        EN.put("msg.offer_removed", "§aOffer removed!");
        EN.put("msg.offer_returned_storage", "§eInventory full, item moved to Storage!");
        EN.put("msg.offer_returned_inventory", "§aItem returned to inventory!");
        
        EN.put("msg.select_item", "§cSelect item to sell first!");
        EN.put("msg.select_price", "§cSet the price first!");
        EN.put("msg.not_enough_items", "§cYou need §e%d %s§c! You have §e%d");
        EN.put("msg.not_enough_money", "§cYou need §e%d §c%s §7- You have §e%d");
        
        EN.put("msg.trade_success", "§aTrade complete! You got §f%s");
        EN.put("msg.trade_item_taken", "§aTook §f%s §ax%d");
        EN.put("msg.inventory_full", "§cInventory is full!");
        
        EN.put("storage.title", "Storage - %s");
        EN.put("storage.page", "Page %d/%d  |  %d items");
        EN.put("storage.empty", "Storage is empty");
        
        EN.put("picker.title", "Select Price Item");
        EN.put("picker.search", "Search...");
        EN.put("picker.page_info", "Page %d/%d  |  %d items");
        EN.put("picker.back", "Back");
        
        EN.put("amount.title", "Set Quantity");
        EN.put("amount.enter", "Enter quantity as price:");
        EN.put("amount.confirm", "Confirm");
        
        EN.put("buyer.title", "%s's Shop");
        EN.put("buyer.empty", "Shop is empty");
        EN.put("buyer.you_get", "You will get:");
        EN.put("buyer.you_pay", "You will pay:");
        EN.put("buyer.quantity", "Quantity: %d");
        EN.put("buyer.buy", "Buy");
        EN.put("buyer.for", "for");
    }
    
    public static String get(String key, Object... args) {
        String template = EN.getOrDefault(key, key);
        if (args.length > 0) {
            return String.format(template, args);
        }
        return template;
    }
}
