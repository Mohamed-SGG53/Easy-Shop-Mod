package com.example.shopmod.data;

import java.util.HashMap;
import java.util.Map;

public class I18n {

    private static final Map<String, String> EN = new HashMap<>();

    static {
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

        EN.put("msg.shop_created", "\u00a7aShop created: \u00a7e%s's Shop \u00a7a\u00a7r");
        EN.put("msg.shop_exists", "\u00a7cYou already have a shop! Use \u00a7e/close_shop \u00a7cto remove it first.");
        EN.put("msg.shop_closed", "\u00a7aShop closed successfully! \u00a77(Your items are saved)");
        EN.put("msg.shop_not_found", "\u00a7cYour shop NPC was not found. Use \u00a7a/create_shop \u00a7cto create it again. \u00a77Your offers are still saved.");
        EN.put("msg.shop_npc_not_found", "\u00a7cYou don't have a shop!");
        EN.put("msg.shop_npc_killed", "\u00a7c\u2620 \u00a7e%s\u00a7c's Shop was destroyed by \u00a7e%s\u00a7c! The offers are still saved.");
        EN.put("msg.shop_not_exists", "\u00a7cShop \u00a7e%s \u00a7cnot found!");
        EN.put("msg.no_shops", "\u00a77No shops yet.");
        EN.put("msg.shop_list_header", "\u00a76\u2500\u2500\u2500\u2500\u2500\u2500\u2500 Shop List \u2500\u2500\u2500\u2500\u2500\u2500\u2500");
        EN.put("msg.shop_list_item", "\u00a7a\u25b6 \u00a7f%s's Shop \u00a7c(%d offers)");

        EN.put("msg.offer_added", "\u00a7aOffer added successfully!");
        EN.put("msg.offer_removed", "\u00a7aOffer removed!");
        EN.put("msg.offer_returned_storage", "\u00a7eInventory full, item moved to Storage!");
        EN.put("msg.offer_returned_inventory", "\u00a7aItem returned to inventory!");

        EN.put("msg.select_item", "\u00a7cSelect item to sell first!");
        EN.put("msg.select_price", "\u00a7cSet the price first!");
        EN.put("msg.not_enough_items", "\u00a7cYou need \u00a7e%d %s\u00a7c! You have \u00a7e%d");
        EN.put("msg.not_enough_money", "\u00a7cYou need \u00a7e%d \u00a7c%s \u00a7c - You have \u00a7e%d");

        EN.put("msg.trade_success", "\u00a7aTrade complete! You got \u00a7f%s");
        EN.put("msg.trade_item_taken", "\u00a7aTook \u00a7f%s \u00a7ax%d");
        EN.put("msg.inventory_full", "\u00a7cInventory is full!");

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