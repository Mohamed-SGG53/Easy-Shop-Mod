package com.example.shopmod.data;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class I18n {

    private static final Map<String, String> EN = new HashMap<>();
    private static final Map<String, String> AR = new HashMap<>();
    private static final Map<String, String> ZH = new HashMap<>();
    private static final Map<String, String> JA = new HashMap<>();
    private static final Map<String, String> KO = new HashMap<>();
    private static final Map<String, String> ES = new HashMap<>();
    private static final Map<String, String> PT = new HashMap<>();
    private static final Map<String, String> RU = new HashMap<>();
    private static final Map<String, String> DE = new HashMap<>();
    private static final Map<String, String> FR = new HashMap<>();

    private static final Map<String, Map<String, String>> ALL_LANGS = new HashMap<>();
    private static final Map<String, String> LANG_ALIASES = new HashMap<>();

    private static String currentLang = "en_us";
    private static boolean langDetected = false;

    static {
        initEN();
        initAR();
        initZH();
        initJA();
        initKO();
        initES();
        initPT();
        initRU();
        initDE();
        initFR();

        ALL_LANGS.put("en_us", EN);
        ALL_LANGS.put("ar_sa", AR);
        ALL_LANGS.put("zh_cn", ZH);
        ALL_LANGS.put("ja_jp", JA);
        ALL_LANGS.put("ko_kr", KO);
        ALL_LANGS.put("es_es", ES);
        ALL_LANGS.put("pt_br", PT);
        ALL_LANGS.put("ru_ru", RU);
        ALL_LANGS.put("de_de", DE);
        ALL_LANGS.put("fr_fr", FR);

        LANG_ALIASES.put("en_gb", "en_us");
        LANG_ALIASES.put("en_au", "en_us");
        LANG_ALIASES.put("en_ca", "en_us");
        LANG_ALIASES.put("en_nz", "en_us");
        LANG_ALIASES.put("de_at", "de_de");
        LANG_ALIASES.put("de_ch", "de_de");
        LANG_ALIASES.put("fr_ca", "fr_fr");
        LANG_ALIASES.put("pt_pt", "pt_br");
        LANG_ALIASES.put("es_ar", "es_es");
        LANG_ALIASES.put("es_cl", "es_es");
        LANG_ALIASES.put("es_mx", "es_es");
        LANG_ALIASES.put("es_uy", "es_es");
        LANG_ALIASES.put("es_ve", "es_es");
    }

    // ==================== English (US) ====================
    private static void initEN() {
        EN.put("shop.title", "%s - Shop Manager");
        EN.put("shop.storage", "Storage");
        EN.put("shop.set_price", "Set Price");
        EN.put("shop.add_offer", "Add Offer");
        EN.put("shop.close", "Close");
        EN.put("shop.open", "Open");
        EN.put("shop.sell_item", "Sell Item:");
        EN.put("shop.price", "Price:");
        EN.put("shop.offers", "Offers");
        EN.put("shop.no_offers", "No offers yet");
        EN.put("shop.page", "Page %d/%d");
        EN.put("shop.items", "%d items");
        EN.put("shop.add_buyable", "Add a Buyable Item:");
        EN.put("shop.set_price_label", "Set Price:");
        EN.put("shop.item_header", "Item");
        EN.put("shop.price_header", "Price");
        EN.put("shop.cancel_header", "Cancel");
        EN.put("shop.cancel_btn", "Cancel X");
        EN.put("shop.move_label", "Shop Move: ");
        EN.put("shop.move_on", "On");
        EN.put("shop.move_off", "Off");
        EN.put("shop.item_price", "Item Price:");
        EN.put("shop.page_offers", "Page %d/%d | Offers: %d");
        EN.put("shop.next", ">");
        EN.put("shop.prev", "<");

        EN.put("shoplist.title", "Shops List");
        EN.put("shoplist.create", "Create Your Shop");
        EN.put("shoplist.open", "Open");
        EN.put("shoplist.close", "Close");
        EN.put("shoplist.empty", "No shops yet");
        EN.put("shoplist.count", "%d Shops");
        EN.put("shoplist.shop_of", "%s's Shop");
        EN.put("shoplist.offer_single", "(%d Offer)");
        EN.put("shoplist.offer_plural", "(%d Offers)");

        EN.put("msg.shop_created", "\u00a7aShop created: \u00a7e%s's Shop \u00a7a\u00a7r");
        EN.put("msg.shop_exists", "\u00a7cYou already have a shop! Use \u00a7e/shop close \u00a7cto remove it first.");
        EN.put("msg.shop_closed", "\u00a7aShop closed successfully! \u00a77(Your items are saved)");
        EN.put("msg.shop_not_found", "\u00a7cYour shop NPC was not found. Use \u00a7a/shop create \u00a7cto create it again. \u00a77Your offers are still saved.");
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
        EN.put("msg.shop_deleted_admin", "\u00a7aShop deleted for: \u00a7e%s");

        EN.put("msg.trade_success", "\u00a7aTrade complete! You got \u00a7f%s");
        EN.put("msg.trade_item_taken", "\u00a7aTook \u00a7f%s \u00a7ax%d");
        EN.put("msg.inventory_full", "\u00a7cInventory is full!");
        EN.put("msg.skin_not_found", "\u00a7e\u26a0 \u00a7cYou have not placed your skin PNG in {path}\u00a7c. Your face will appear as default in the Shops List until you add it.");
        EN.put("skin.click_to_open", "\u00a7eClick to open folder");

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
        EN.put("buyer.buy", "Buy");
        EN.put("buyer.details", "Details");
        EN.put("buyer.no_trades", "No trades available");
        EN.put("buyer.offers_count", "Offers: %d");
        EN.put("buyer.you_get", "You will get:");
        EN.put("buyer.you_pay", "You will pay:");
        EN.put("buyer.quantity", "Quantity: %d");
        EN.put("buyer.for", "for");

        EN.put("select.title", "Select Item");
        EN.put("select.inventory", "Inventory");
        EN.put("select.hotbar", "Hotbar");
        EN.put("select.sell", "Sell");
        EN.put("select.hint", "Left: Pick/Place all | Right: Split/Place one");

        EN.put("msg.target_has_shop", "\u00a7cThis player already has a shop! Cannot own more than one shop.");
        EN.put("msg.shop_deleted_notify", "\u00a7cYour shop has been deleted. Type \u00a7e/shop recover \u00a7cto retrieve your items.");
        EN.put("msg.no_recovery_items", "\u00a7cNo items to recover.");
        EN.put("msg.all_recovered", "\u00a7aAll your items have been recovered!");
        EN.put("msg.recovery_inventory_full", "\u00a7cInventory is full! Make space and try again.");
        EN.put("msg.admin_only", "\u00a7cThis command is for administrators only!");
        EN.put("msg.transfer_success", "\u00a7aShop ownership transferred successfully!");
        EN.put("msg.repair_success", "\u00a7aShop ownership repaired successfully!");
        EN.put("msg.shop_not_found_admin", "\u00a7cShop not found for player: \u00a7e%s");
        EN.put("msg.no_shops_without_uuid", "\u00a77No shops found without UUID (all are already linked).");
        EN.put("msg.player_not_found", "\u00a7cPlayer not found: \u00a7e%s");
        EN.put("msg.no_shop_to_transfer", "\u00a7cPlayer \u00a7e%s \u00a7cdoes not have a shop!");
        EN.put("recovery.title", "Item Recovery");
    }

    // ==================== Arabic (ar_sa) ====================
    private static void initAR() {
        AR.put("shop.title", "%s - مدير المتجر");
        AR.put("shop.storage", "مخزن");
        AR.put("shop.set_price", "تحديد السعر");
        AR.put("shop.add_offer", "إضافة عرض");
        AR.put("shop.close", "إغلاق");
        AR.put("shop.open", "فتح");
        AR.put("shop.sell_item", ":العنصر للبيع");
        AR.put("shop.price", ":السعر");
        AR.put("shop.offers", "عروض");
        AR.put("shop.no_offers", "لا توجد عروض بعد");
        AR.put("shop.page", "صفحة %d/%d");
        AR.put("shop.items", "%d عنصر");
        AR.put("shop.add_buyable", "أضف عنصر للبيع:");
        AR.put("shop.set_price_label", "تحديد السعر:");
        AR.put("shop.item_header", "العنصر");
        AR.put("shop.price_header", "السعر");
        AR.put("shop.cancel_header", "إلغاء");
        AR.put("shop.cancel_btn", "إلغاء X");
        AR.put("shop.move_label", "تحريك المتجر: ");
        AR.put("shop.move_on", "تشغيل");
        AR.put("shop.move_off", "إيقاف");
        AR.put("shop.item_price", "سعر العنصر:");
        AR.put("shop.page_offers", "صفحة %d/%d | العروض: %d");
        AR.put("shop.next", ">");
        AR.put("shop.prev", "<");

        AR.put("shoplist.title", "قائمة المتاجر");
        AR.put("shoplist.create", "أنشئ متجرك");
        AR.put("shoplist.open", "فتح");
        AR.put("shoplist.close", "إغلاق");
        AR.put("shoplist.empty", "لا توجد متاجر بعد");
        AR.put("shoplist.count", "%d متاجر");
        AR.put("shoplist.shop_of", "متجر %s");
        AR.put("shoplist.offer_single", "(%d عرض)");
        AR.put("shoplist.offer_plural", "(%d عروض)");

        AR.put("msg.shop_created", "\u00a7aتم إنشاء المتجر: \u00a7eمتجر %s \u00a7a\u00a7r");
        AR.put("msg.shop_exists", "\u00a7cلديك متجر بالفعل! استخدم \u00a7e/shop close \u00a7cلإزالته أولاً.");
        AR.put("msg.shop_closed", "\u00a7aتم إغلاق المتجر بنجاح! \u00a77(تم حفظ عناصرك)");
        AR.put("msg.shop_not_found", "\u00a7cلم يتم العثور على NPC المتجر. استخدم \u00a7a/shop create \u00a7cلإنشائه مرة أخرى. \u00a77عروضك محفوظة.");
        AR.put("msg.shop_npc_not_found", "\u00a7cليس لديك متجر!");
        AR.put("msg.shop_npc_killed", "\u00a7c\u2620 \u00a7eمتجر %s\u00a7c تم تدميره بواسطة \u00a7e%s\u00a7c! العروض محفوظة.");
        AR.put("msg.shop_not_exists", "\u00a7cالمتجر \u00a7e%s \u00a7cغير موجود!");
        AR.put("msg.no_shops", "\u00a77لا توجد متاجر بعد.");
        AR.put("msg.shop_list_header", "\u00a76\u2500\u2500\u2500\u2500\u2500\u2500\u2500 قائمة المتاجر \u2500\u2500\u2500\u2500\u2500\u2500\u2500");
        AR.put("msg.shop_list_item", "\u00a7a\u25b6 \u00a7fمتجر %s \u00a7c(%d عروض)");
        AR.put("msg.shop_deleted_admin", "\u00a7aتم حذف المتجر للاعب: \u00a7e%s");

        AR.put("msg.offer_added", "\u00a7aتم إضافة العرض بنجاح!");
        AR.put("msg.offer_removed", "\u00a7aتم إزالة العرض!");
        AR.put("msg.offer_returned_storage", "\u00a7eالمخزون ممتلئ، تم نقل العنصر إلى المخزن!");
        AR.put("msg.offer_returned_inventory", "\u00a7aتم إرجاع العنصر إلى المخزون!");

        AR.put("msg.select_item", "\u00a7cاختر العنصر الذي تريد بيعه أولاً!");
        AR.put("msg.select_price", "\u00a7cحدد السعر أولاً!");
        AR.put("msg.not_enough_items", "\u00a7cتحتاج \u00a7e%d %s\u00a7c! لديك \u00a7e%d");
        AR.put("msg.not_enough_money", "\u00a7cتحتاج \u00a7e%d\u00a7c من \u00a7c%s \u00a7c- لديك \u00a7e%d");

        AR.put("msg.trade_success", "\u00a7aتمت الصفقة بنجاح! حصلت على \u00a7f%s");
        AR.put("msg.trade_item_taken", "\u00a7aتم أخذ \u00a7f%s \u00a7ax%d");
        AR.put("msg.inventory_full", "\u00a7cالمخزون ممتلئ!");
        AR.put("msg.skin_not_found", "\u00a7e\u26a0 \u00a7cلم تقم بوضع صورة السكن الخاصة بك PNG في {path}\u00a7c. سيظهر وجهك بشكل افتراضي في قائمة المتاجر حتى تقوم بإضافتها.");
        AR.put("skin.click_to_open", "\u00a7eاضغط لفتح المجلد");

        AR.put("storage.title", "مخزن - %s");
        AR.put("storage.page", "صفحة %d/%d | %d عنصر");
        AR.put("storage.empty", "المخزن فارغ");

        AR.put("picker.title", "اختر عنصر السعر");
        AR.put("picker.search", "بحث...");
        AR.put("picker.page_info", "صفحة %d/%d | %d عنصر");
        AR.put("picker.back", "رجوع");

        AR.put("amount.title", "تحديد الكمية");
        AR.put("amount.enter", "أدخل الكمية ك سعر:");
        AR.put("amount.confirm", "تأكيد");

        AR.put("buyer.title", "متجر %s");
        AR.put("buyer.empty", "المتجر فارغ");
        AR.put("buyer.buy", "شراء");
        AR.put("buyer.details", "التفاصيل");
        AR.put("buyer.no_trades", "لا توجد عروض متاحة");
        AR.put("buyer.offers_count", "العروض: %d");
        AR.put("buyer.you_get", "ستحصل على:");
        AR.put("buyer.you_pay", "ستدفع:");
        AR.put("buyer.quantity", "الكمية: %d");
        AR.put("buyer.for", "بـ");

        AR.put("select.title", "اختيار عنصر");
        AR.put("select.inventory", "المخزون");
        AR.put("select.hotbar", "شريط الأدوات");
        AR.put("select.sell", "بيع");
        AR.put("select.hint", "يسار: اختيار/وضع الكل | يمين: تقسيم/وضع واحد");

        AR.put("msg.target_has_shop", "\u00a7cهذا اللاعب يمتلك متجر بالفعل! لا يمكن امتلاك أكثر من متجر.");
        AR.put("msg.shop_deleted_notify", "\u00a7cتم حذف متجرك. اكتب \u00a7e/shop recover \u00a7cلاسترداد عناصرك.");
        AR.put("msg.no_recovery_items", "\u00a7cلا توجد عناصر لاستردادها.");
        AR.put("msg.all_recovered", "\u00a7aتم استرداد كل عناصرك!");
        AR.put("msg.recovery_inventory_full", "\u00a7cالمخزون ممتلئ! اعمل مكان وجرب تاني.");
        AR.put("msg.admin_only", "\u00a7cهذا الأمر للمشرفين فقط!");
        AR.put("msg.transfer_success", "\u00a7aتم تحويل ملكية المتجر بنجاح!");
        AR.put("msg.repair_success", "\u00a7aتم إصلاح ملكية المتجر بنجاح!");
        AR.put("msg.shop_not_found_admin", "\u00a7cلم يتم العثور على متجر للاعب: \u00a7e%s");
        AR.put("msg.no_shops_without_uuid", "\u00a77لا توجد متاجر بدون UUID (جميعها مربوطة بالفعل).");
        AR.put("msg.player_not_found", "\u00a7cلم يتم العثور على اللاعب: \u00a7e%s");
        AR.put("msg.no_shop_to_transfer", "\u00a7cاللاعب \u00a7e%s \u00a7cلا يمتلك متجر!");
        AR.put("recovery.title", "استرداد العناصر");
    }

    // ==================== Chinese Simplified (zh_cn) ====================
    private static void initZH() {
        ZH.put("shop.title", "%s - 商店管理");
        ZH.put("shop.storage", "仓库");
        ZH.put("shop.set_price", "设置价格");
        ZH.put("shop.add_offer", "添加商品");
        ZH.put("shop.close", "关闭");
        ZH.put("shop.open", "打开");
        ZH.put("shop.sell_item", "出售物品:");
        ZH.put("shop.price", "价格:");
        ZH.put("shop.offers", "商品");
        ZH.put("shop.no_offers", "暂无商品");
        ZH.put("shop.page", "第 %d/%d 页");
        ZH.put("shop.items", "%d 个物品");
        ZH.put("shop.add_buyable", "添加可购买的物品:");
        ZH.put("shop.set_price_label", "设置价格:");
        ZH.put("shop.item_header", "物品");
        ZH.put("shop.price_header", "价格");
        ZH.put("shop.cancel_header", "取消");
        ZH.put("shop.cancel_btn", "取消 X");
        ZH.put("shop.move_label", "商店移动: ");
        ZH.put("shop.move_on", "开");
        ZH.put("shop.move_off", "关");
        ZH.put("shop.item_price", "物品价格:");
        ZH.put("shop.page_offers", "第 %d/%d 页 | 商品: %d");
        ZH.put("shop.next", ">");
        ZH.put("shop.prev", "<");

        ZH.put("shoplist.title", "商店列表");
        ZH.put("shoplist.create", "创建你的商店");
        ZH.put("shoplist.open", "打开");
        ZH.put("shoplist.close", "关闭");
        ZH.put("shoplist.empty", "暂无商店");
        ZH.put("shoplist.count", "%d 个商店");
        ZH.put("shoplist.shop_of", "%s的商店");
        ZH.put("shoplist.offer_single", "(%d 个商品)");
        ZH.put("shoplist.offer_plural", "(%d 个商品)");

        ZH.put("msg.shop_created", "\u00a7a商店已创建: \u00a7e%s的商店 \u00a7a\u00a7r");
        ZH.put("msg.shop_exists", "\u00a7c你已经有一个商店了! 使用 \u00a7e/shop close \u00a7c先移除它.");
        ZH.put("msg.shop_closed", "\u00a7a商店已成功关闭! \u00a77(你的物品已保存)");
        ZH.put("msg.shop_not_found", "\u00a7c未找到商店NPC. 使用 \u00a7a/shop create \u00a7c重新创建. \u00a77商品仍然保存着.");
        ZH.put("msg.shop_npc_not_found", "\u00a7c你没有商店!");
        ZH.put("msg.shop_npc_killed", "\u00a7c\u2620 \u00a7e%s\u00a7c的商店被\u00a7e%s\u00a7c摧毁了! 商品仍然保存着.");
        ZH.put("msg.shop_not_exists", "\u00a7c商店 \u00a7e%s \u00a7c未找到!");
        ZH.put("msg.no_shops", "\u00a77暂无商店.");
        ZH.put("msg.shop_list_header", "\u00a76\u2500\u2500\u2500\u2500\u2500\u2500\u2500 商店列表 \u2500\u2500\u2500\u2500\u2500\u2500\u2500");
        ZH.put("msg.shop_list_item", "\u00a7a\u25b6 \u00a7f%s的商店 \u00a7c(%d 个商品)");
        ZH.put("msg.shop_deleted_admin", "\u00a7a已删除玩家的商店: \u00a7e%s");


        ZH.put("msg.offer_added", "\u00a7a商品添加成功!");
        ZH.put("msg.offer_removed", "\u00a7a商品已移除!");
        ZH.put("msg.offer_returned_storage", "\u00a7e背包已满,物品已移至仓库!");
        ZH.put("msg.offer_returned_inventory", "\u00a7a物品已返回背包!");

        ZH.put("msg.select_item", "\u00a7c请先选择要出售的物品!");
        ZH.put("msg.select_price", "\u00a7c请先设置价格!");
        ZH.put("msg.not_enough_items", "\u00a7c你需要 \u00a7e%d %s\u00a7c! 你只有 \u00a7e%d");
        ZH.put("msg.not_enough_money", "\u00a7c你需要 \u00a7e%d\u00a7c 个 \u00a7c%s \u00a7c- 你只有 \u00a7e%d");

        ZH.put("msg.trade_success", "\u00a7a交易完成! 你获得了 \u00a7f%s");
        ZH.put("msg.trade_item_taken", "\u00a7a取走了 \u00a7f%s \u00a7ax%d");
        ZH.put("msg.inventory_full", "\u00a7c背包已满!");
        ZH.put("msg.skin_not_found", "\u00a7e\u26a0 \u00a7c你还没有将皮肤 PNG 图片放入 {path}\u00a7c。在你的商店列表中，你的头像将显示为默认头像，直到你添加为止。");
        ZH.put("skin.click_to_open", "\u00a7e点击打开文件夹");

        ZH.put("storage.title", "仓库 - %s");
        ZH.put("storage.page", "第 %d/%d 页 | %d 个物品");
        ZH.put("storage.empty", "仓库为空");

        ZH.put("picker.title", "选择价格物品");
        ZH.put("picker.search", "搜索...");
        ZH.put("picker.page_info", "第 %d/%d 页 | %d 个物品");
        ZH.put("picker.back", "返回");

        ZH.put("amount.title", "设置数量");
        ZH.put("amount.enter", "输入数量作为价格:");
        ZH.put("amount.confirm", "确认");

        ZH.put("buyer.title", "%s的商店");
        ZH.put("buyer.empty", "商店为空");
        ZH.put("buyer.buy", "购买");
        ZH.put("buyer.details", "详情");
        ZH.put("buyer.no_trades", "暂无可购买的物品");
        ZH.put("buyer.offers_count", "商品: %d");
        ZH.put("buyer.you_get", "你将获得:");
        ZH.put("buyer.you_pay", "你将支付:");
        ZH.put("buyer.quantity", "数量: %d");
        ZH.put("buyer.for", "用");

        ZH.put("select.title", "选择物品");
        ZH.put("select.inventory", "背包");
        ZH.put("select.hotbar", "快捷栏");
        ZH.put("select.sell", "出售");
        ZH.put("select.hint", "左键: 拿取/放置全部 | 右键: 分割/放置一个");

        ZH.put("msg.target_has_shop", "\u00a7c该玩家已经有一个商店了！无法拥有多个商店。");
        ZH.put("msg.shop_deleted_notify", "\u00a7c你的商店已被删除。输入 \u00a7e/shop recover \u00a7c来取回你的物品。");
        ZH.put("msg.no_recovery_items", "\u00a7c没有可恢复的物品。");
        ZH.put("msg.all_recovered", "\u00a7a你所有的物品都已恢复！");
        ZH.put("msg.recovery_inventory_full", "\u00a7c背包已满！请腾出空间后重试。");
        ZH.put("msg.admin_only", "\u00a7c此命令仅限管理员使用！");
        ZH.put("msg.transfer_success", "\u00a7a商店所有权已成功转移！");
        ZH.put("msg.repair_success", "\u00a7a商店所有权已成功修复！");
        ZH.put("msg.shop_not_found_admin", "\u00a7c未找到玩家的商店: \u00a7e%s");
        ZH.put("msg.no_shops_without_uuid", "\u00a77没有找到缺少UUID的商店（所有商店已关联）。");
        ZH.put("msg.player_not_found", "\u00a7c未找到玩家: \u00a7e%s");
        ZH.put("msg.no_shop_to_transfer", "\u00a7c玩家 \u00a7e%s \u00a7c没有商店！");
        ZH.put("recovery.title", "物品恢复");
    }

    // ==================== Japanese (ja_jp) ====================
    private static void initJA() {
        JA.put("shop.title", "%s - ショップ管理");
        JA.put("shop.storage", "ストレージ");
        JA.put("shop.set_price", "価格設定");
        JA.put("shop.add_offer", "商品追加");
        JA.put("shop.close", "閉じる");
        JA.put("shop.open", "開く");
        JA.put("shop.sell_item", "販売アイテム:");
        JA.put("shop.price", "価格:");
        JA.put("shop.offers", "商品");
        JA.put("shop.no_offers", "まだ商品がありません");
        JA.put("shop.page", "ページ %d/%d");
        JA.put("shop.items", "%d 個のアイテム");
        JA.put("shop.add_buyable", "購入可能アイテムを追加:");
        JA.put("shop.set_price_label", "価格を設定:");
        JA.put("shop.item_header", "アイテム");
        JA.put("shop.price_header", "価格");
        JA.put("shop.cancel_header", "キャンセル");
        JA.put("shop.cancel_btn", "キャンセル X");
        JA.put("shop.move_label", "ショップ移動: ");
        JA.put("shop.move_on", "オン");
        JA.put("shop.move_off", "オフ");
        JA.put("shop.item_price", "アイテム価格:");
        JA.put("shop.page_offers", "ページ %d/%d | 商品: %d");
        JA.put("shop.next", ">");
        JA.put("shop.prev", "<");

        JA.put("shoplist.title", "ショップリスト");
        JA.put("shoplist.create", "ショップを作成");
        JA.put("shoplist.open", "開く");
        JA.put("shoplist.close", "閉じる");
        JA.put("shoplist.empty", "ショップがありません");
        JA.put("shoplist.count", "%d ショップ");
        JA.put("shoplist.shop_of", "%sのショップ");
        JA.put("shoplist.offer_single", "(%d 商品)");
        JA.put("shoplist.offer_plural", "(%d 商品)");

        JA.put("msg.shop_created", "\u00a7aショップを作成しました: \u00a7e%sのショップ \u00a7a\u00a7r");
        JA.put("msg.shop_exists", "\u00a7cすでにショップがあります! \u00a7e/shop close \u00a7cを先に使用してください.");
        JA.put("msg.shop_closed", "\u00a7aショップを正常に閉じました! \u00a77(アイテムは保存されています)");
        JA.put("msg.shop_not_found", "\u00a7cショップNPCが見つかりません. \u00a7a/shop create \u00a7cで再作成してください. \u00a77商品は保存されています.");
        JA.put("msg.shop_npc_not_found", "\u00a7cショップがありません!");
        JA.put("msg.shop_npc_killed", "\u00a7c\u2620 \u00a7e%s\u00a7cのショップが\u00a7e%s\u00a7cによって破壊されました! 商品は保存されています.");
        JA.put("msg.shop_not_exists", "\u00a7cショップ \u00a7e%s \u00a7cが見つかりません!");
        JA.put("msg.no_shops", "\u00a77まだショップがありません.");
        JA.put("msg.shop_list_header", "\u00a76\u2500\u2500\u2500\u2500\u2500\u2500\u2500 ショップリスト \u2500\u2500\u2500\u2500\u2500\u2500\u2500");
        JA.put("msg.shop_list_item", "\u00a7a\u25b6 \u00a7f%sのショップ \u00a7c(%d 商品)");
        JA.put("msg.shop_deleted_admin", "\u00a7aショップを削除しました: \u00a7e%s");


        JA.put("msg.offer_added", "\u00a7a商品を追加しました!");
        JA.put("msg.offer_removed", "\u00a7a商品を削除しました!");
        JA.put("msg.offer_returned_storage", "\u00a7eインベントリが満杯です、アイテムをストレージに移動しました!");
        JA.put("msg.offer_returned_inventory", "\u00a7aアイテムをインベントリに戻しました!");

        JA.put("msg.select_item", "\u00a7c販売アイテムを先に選択してください!");
        JA.put("msg.select_price", "\u00a7c価格を先に設定してください!");
        JA.put("msg.not_enough_items", "\u00a7c\u00a7e%d %s\u00a7cが必要です! 所持数: \u00a7e%d");
        JA.put("msg.not_enough_money", "\u00a7c\u00a7e%d\u00a7c個の \u00a7c%s \u00a7cが必要です - 所持数: \u00a7e%d");

        JA.put("msg.trade_success", "\u00a7a取引完了! \u00a7f%sを入手しました");
        JA.put("msg.trade_item_taken", "\u00a7a\u00a7f%s \u00a7ax%dを取り出しました");
        JA.put("msg.inventory_full", "\u00a7cインベントリが満杯です!");
        JA.put("msg.skin_not_found", "\u00a7e\u26a0 \u00a7c\u30b9\u30ad\u30f3PNG\u753b\u50cf\u3092 {path}\u00a7c \u306b\u914d\u7f6e\u3057\u3066\u3044\u307e\u305b\u3093\u3002\u8ffd\u52a0\u3059\u308b\u307e\u3067\u3001\u30b7\u30e7\u30c3\u30d7\u30ea\u30b9\u30c8\u3067\u306f\u30c7\u30d5\u30a9\u30eb\u30c8\u306e\u984d\u304c\u8868\u793a\u3055\u308c\u307e\u3059\u3002");
        JA.put("skin.click_to_open", "\u00a7eフォルダを開くにはクリック");

        JA.put("storage.title", "ストレージ - %s");
        JA.put("storage.page", "ページ %d/%d | %d 個のアイテム");
        JA.put("storage.empty", "ストレージは空です");

        JA.put("picker.title", "価格アイテムを選択");
        JA.put("picker.search", "検索...");
        JA.put("picker.page_info", "ページ %d/%d | %d 個のアイテム");
        JA.put("picker.back", "戻る");

        JA.put("amount.title", "数量を設定");
        JA.put("amount.enter", "価格として数量を入力:");
        JA.put("amount.confirm", "確認");

        JA.put("buyer.title", "%sのショップ");
        JA.put("buyer.empty", "ショップは空です");
        JA.put("buyer.buy", "購入");
        JA.put("buyer.details", "詳細");
        JA.put("buyer.no_trades", "取引可能なアイテムがありません");
        JA.put("buyer.offers_count", "商品: %d");
        JA.put("buyer.you_get", "入手するもの:");
        JA.put("buyer.you_pay", "支払うもの:");
        JA.put("buyer.quantity", "数量: %d");
        JA.put("buyer.for", "で");

        JA.put("select.title", "アイテムを選択");
        JA.put("select.inventory", "インベントリ");
        JA.put("select.hotbar", "ホットバー");
        JA.put("select.sell", "販売");
        JA.put("select.hint", "左クリック: 全て取得/配置 | 右クリック: 分割/1つ配置");

        JA.put("msg.target_has_shop", "\u00a7cこのプレイヤーはすでにショップを持っています！複数のショップは所有できません。");
        JA.put("msg.shop_deleted_notify", "\u00a7cあなたのショップは削除されました。アイテムを取り戻すには \u00a7e/shop recover \u00a7cを入力してください。");
        JA.put("msg.no_recovery_items", "\u00a7c回復可能なアイテムがありません。");
        JA.put("msg.all_recovered", "\u00a7aすべてのアイテムが回復されました！");
        JA.put("msg.recovery_inventory_full", "\u00a7cインベントリが満杯です！スペースを空けて再試行してください。");
        JA.put("msg.admin_only", "\u00a7cこのコマンドは管理者のみ使用可能です！");
        JA.put("msg.transfer_success", "\u00a7aショップの所有権が正常に移転されました！");
        JA.put("msg.repair_success", "\u00a7aショップの所有権が正常に修復されました！");
        JA.put("msg.shop_not_found_admin", "\u00a7cプレイヤーのショップが見つかりません: \u00a7e%s");
        JA.put("msg.no_shops_without_uuid", "\u00a77UUIDがないショップは見つかりません（すべて既にリンク済みです）。");
        JA.put("msg.player_not_found", "\u00a7cプレイヤーが見つかりません: \u00a7e%s");
        JA.put("msg.no_shop_to_transfer", "\u00a7cプレイヤー \u00a7e%s \u00a7cはショップを持っていません！");
        JA.put("recovery.title", "アイテム回復");
    }

    // ==================== Korean (ko_kr) ====================
    private static void initKO() {
        KO.put("shop.title", "%s - 상점 관리");
        KO.put("shop.storage", "보관함");
        KO.put("shop.set_price", "가격 설정");
        KO.put("shop.add_offer", "상품 추가");
        KO.put("shop.close", "닫기");
        KO.put("shop.open", "열기");
        KO.put("shop.sell_item", "판매 아이템:");
        KO.put("shop.price", "가격:");
        KO.put("shop.offers", "상품");
        KO.put("shop.no_offers", "상품이 없습니다");
        KO.put("shop.page", "%d/%d 페이지");
        KO.put("shop.items", "%d개 아이템");
        KO.put("shop.add_buyable", "판매 아이템 추가:");
        KO.put("shop.set_price_label", "가격 설정:");
        KO.put("shop.item_header", "아이템");
        KO.put("shop.price_header", "가격");
        KO.put("shop.cancel_header", "취소");
        KO.put("shop.cancel_btn", "취소 X");
        KO.put("shop.move_label", "상점 이동: ");
        KO.put("shop.move_on", "켜기");
        KO.put("shop.move_off", "끄기");
        KO.put("shop.item_price", "아이템 가격:");
        KO.put("shop.page_offers", "%d/%d 페이지 | 상품: %d");
        KO.put("shop.next", ">");
        KO.put("shop.prev", "<");

        KO.put("shoplist.title", "상점 목록");
        KO.put("shoplist.create", "상점 만들기");
        KO.put("shoplist.open", "열기");
        KO.put("shoplist.close", "닫기");
        KO.put("shoplist.empty", "상점이 없습니다");
        KO.put("shoplist.count", "%d개 상점");
        KO.put("shoplist.shop_of", "%s의 상점");
        KO.put("shoplist.offer_single", "(%d개 상품)");
        KO.put("shoplist.offer_plural", "(%d개 상품)");

        KO.put("msg.shop_created", "\u00a7a상점이 생성되었습니다: \u00a7e%s의 상점 \u00a7a\u00a7r");
        KO.put("msg.shop_exists", "\u00a7c이미 상점이 있습니다! \u00a7e/shop close \u00a7c을 사용하여 먼저 제거하세요.");
        KO.put("msg.shop_closed", "\u00a7a상점이 성공적으로 닫혔습니다! \u00a77(아이템이 저장되었습니다)");
        KO.put("msg.shop_not_found", "\u00a7c상점 NPC를 찾을 수 없습니다. \u00a7a/shop create \u00a7c을 사용하여 다시 만드세요. \u00a77상품은 저장되어 있습니다.");
        KO.put("msg.shop_npc_not_found", "\u00a7c상점이 없습니다!");
        KO.put("msg.shop_npc_killed", "\u00a7c\u2620 \u00a7e%s\u00a7c의 상점이 \u00a7e%s\u00a7c에 의해 파괴되었습니다! 상품은 저장되어 있습니다.");
        KO.put("msg.shop_not_exists", "\u00a7c상점 \u00a7e%s \u00a7c을 찾을 수 없습니다!");
        KO.put("msg.no_shops", "\u00a77아직 상점이 없습니다.");
        KO.put("msg.shop_list_header", "\u00a76\u2500\u2500\u2500\u2500\u2500\u2500\u2500 상점 목록 \u2500\u2500\u2500\u2500\u2500\u2500\u2500");
        KO.put("msg.shop_list_item", "\u00a7a\u25b6 \u00a7f%s의 상점 \u00a7c(%d개 상품)");
        KO.put("msg.shop_deleted_admin", "\u00a7a상점이 삭제되었습니다: \u00a7e%s");


        KO.put("msg.offer_added", "\u00a7a상품이 추가되었습니다!");
        KO.put("msg.offer_removed", "\u00a7a상품이 삭제되었습니다!");
        KO.put("msg.offer_returned_storage", "\u00a7e인벤토리가 가득 찼습니다, 아이템이 보관함으로 이동되었습니다!");
        KO.put("msg.offer_returned_inventory", "\u00a7a아이템이 인벤토리로 반환되었습니다!");

        KO.put("msg.select_item", "\u00a7c먼저 판매할 아이템을 선택하세요!");
        KO.put("msg.select_price", "\u00a7c먼저 가격을 설정하세요!");
        KO.put("msg.not_enough_items", "\u00a7c\u00a7e%d %s\u00a7c이(가) 필요합니다! 보유량: \u00a7e%d");
        KO.put("msg.not_enough_money", "\u00a7c\u00a7e%d\u00a7c개의 \u00a7c%s \u00a7c이(가) 필요합니다 - 보유량: \u00a7e%d");

        KO.put("msg.trade_success", "\u00a7a거래 완료! \u00a7f%s을(를) 획득했습니다");
        KO.put("msg.trade_item_taken", "\u00a7a\u00a7f%s \u00a7ax%d을(를) 꺼냈습니다");
        KO.put("msg.inventory_full", "\u00a7c인벤토리가 가득 찼습니다!");
        KO.put("msg.skin_not_found", "\u00a7e\u26a0 \u00a7c\uac00\uc774\uc2a4\ud0a8 PNG \uc774\ubbf8\uc9c0\ub97c {path}\u00a7c \uc5d0 \ub123\uc9c0 \uc54a\uc558\uc2b5\ub2c8\ub2e4. \ucd94\uac00\ud560 \ub54c\uae4c\uc9c0 \uc0c1\uc810 \ubaa9\ub85d\uc5d0\uc11c \uae30\ubcf8 \ud504\ub85c\ud544 \uc774\ubbf8\uc9c0\uac00 \ud45c\uc2dc\ub429\ub2c8\ub2e4.");
        KO.put("skin.click_to_open", "\u00a7e폴더를 열려면 클릭");

        KO.put("storage.title", "보관함 - %s");
        KO.put("storage.page", "%d/%d 페이지 | %d개 아이템");
        KO.put("storage.empty", "보관함이 비어 있습니다");

        KO.put("picker.title", "가격 아이템 선택");
        KO.put("picker.search", "검색...");
        KO.put("picker.page_info", "%d/%d 페이지 | %d개 아이템");
        KO.put("picker.back", "뒤로");

        KO.put("amount.title", "수량 설정");
        KO.put("amount.enter", "가격으로 수량 입력:");
        KO.put("amount.confirm", "확인");

        KO.put("buyer.title", "%s의 상점");
        KO.put("buyer.empty", "상점이 비어 있습니다");
        KO.put("buyer.buy", "구매");
        KO.put("buyer.details", "상세");
        KO.put("buyer.no_trades", "거래 가능한 아이템이 없습니다");
        KO.put("buyer.offers_count", "상품: %d");
        KO.put("buyer.you_get", "받는 것:");
        KO.put("buyer.you_pay", "지불하는 것:");
        KO.put("buyer.quantity", "수량: %d");
        KO.put("buyer.for", "로");

        KO.put("select.title", "아이템 선택");
        KO.put("select.inventory", "인벤토리");
        KO.put("select.hotbar", "핫바");
        KO.put("select.sell", "판매");
        KO.put("select.hint", "좌클릭: 전체 선택/배치 | 우클릭: 분할/1개 배치");

        KO.put("msg.target_has_shop", "\u00a7c이 플레이어는 이미 상점을 가지고 있습니다! 둘 이상의 상점을 소유할 수 없습니다.");
        KO.put("msg.shop_deleted_notify", "\u00a7c당신의 상점이 삭제되었습니다. 아이템을 회복하려면 \u00a7e/shop recover \u00a7c를 입력하세요.");
        KO.put("msg.no_recovery_items", "\u00a7c복구할 아이템이 없습니다.");
        KO.put("msg.all_recovered", "\u00a7a모든 아이템이 복구되었습니다!");
        KO.put("msg.recovery_inventory_full", "\u00a7c인벤토리가 가득 찼습니다! 공간을 만들고 다시 시도하세요.");
        KO.put("msg.admin_only", "\u00a7c이 명령어는 관리자만 사용할 수 있습니다!");
        KO.put("msg.transfer_success", "\u00a7a상점 소유권이 성공적으로 이전되었습니다!");
        KO.put("msg.repair_success", "\u00a7a상점 소유권이 성공적으로 복구되었습니다!");
        KO.put("msg.shop_not_found_admin", "\u00a7c플레이어의 상점을 찾을 수 없습니다: \u00a7e%s");
        KO.put("msg.no_shops_without_uuid", "\u00a77UUID가 없는 상점이 없습니다 (모두 이미 연결되어 있습니다).");
        KO.put("msg.player_not_found", "\u00a7c플레이어를 찾을 수 없습니다: \u00a7e%s");
        KO.put("msg.no_shop_to_transfer", "\u00a7c플레이어 \u00a7e%s \u00a7c는 상점이 없습니다!");
        KO.put("recovery.title", "아이템 복구");
    }

    // ==================== Spanish (es_es) ====================
    private static void initES() {
        ES.put("shop.title", "%s - Gestor de Tienda");
        ES.put("shop.storage", "Almacen");
        ES.put("shop.set_price", "Fijar Precio");
        ES.put("shop.add_offer", "Agregar Oferta");
        ES.put("shop.close", "Cerrar");
        ES.put("shop.open", "Abrir");
        ES.put("shop.sell_item", "Articulo en Venta:");
        ES.put("shop.price", "Precio:");
        ES.put("shop.offers", "Ofertas");
        ES.put("shop.no_offers", "Sin ofertas aun");
        ES.put("shop.page", "Pagina %d/%d");
        ES.put("shop.items", "%d articulos");
        ES.put("shop.add_buyable", "Agregar Articulo a la Venta:");
        ES.put("shop.set_price_label", "Fijar Precio:");
        ES.put("shop.item_header", "Articulo");
        ES.put("shop.price_header", "Precio");
        ES.put("shop.cancel_header", "Cancelar");
        ES.put("shop.cancel_btn", "Cancelar X");
        ES.put("shop.move_label", "Mover Tienda: ");
        ES.put("shop.move_on", "Activado");
        ES.put("shop.move_off", "Desactivado");
        ES.put("shop.item_price", "Precio del Articulo:");
        ES.put("shop.page_offers", "Pagina %d/%d | Ofertas: %d");
        ES.put("shop.next", ">");
        ES.put("shop.prev", "<");

        ES.put("shoplist.title", "Lista de Tiendas");
        ES.put("shoplist.create", "Crear Tu Tienda");
        ES.put("shoplist.open", "Abrir");
        ES.put("shoplist.close", "Cerrar");
        ES.put("shoplist.empty", "Sin tiendas aun");
        ES.put("shoplist.count", "%d Tiendas");
        ES.put("shoplist.shop_of", "Tienda de %s");
        ES.put("shoplist.offer_single", "(%d oferta)");
        ES.put("shoplist.offer_plural", "(%d ofertas)");

        ES.put("msg.shop_created", "\u00a7aTienda creada: \u00a7eTienda de %s \u00a7a\u00a7r");
        ES.put("msg.shop_exists", "\u00a7cYa tienes una tienda! Usa \u00a7e/shop close \u00a7cpara eliminarla primero.");
        ES.put("msg.shop_closed", "\u00a7aTienda cerrada con exito! \u00a77(Tus articulos estan guardados)");
        ES.put("msg.shop_not_found", "\u00a7cNo se encontro el NPC de tu tienda. Usa \u00a7a/shop create \u00a7cpara crearla de nuevo. \u00a77Tus ofertas siguen guardadas.");
        ES.put("msg.shop_npc_not_found", "\u00a7cNo tienes una tienda!");
        ES.put("msg.shop_npc_killed", "\u00a7c\u2620 \u00a7eLa tienda de %s\u00a7c fue destruida por \u00a7e%s\u00a7c! Las ofertas siguen guardadas.");
        ES.put("msg.shop_not_exists", "\u00a7cTienda \u00a7e%s \u00a7cno encontrada!");
        ES.put("msg.no_shops", "\u00a77Aun no hay tiendas.");
        ES.put("msg.shop_list_header", "\u00a76\u2500\u2500\u2500\u2500\u2500\u2500\u2500 Lista de Tiendas \u2500\u2500\u2500\u2500\u2500\u2500\u2500");
        ES.put("msg.shop_list_item", "\u00a7a\u25b6 \u00a7fTienda de %s \u00a7c(%d ofertas)");
        ES.put("msg.shop_deleted_admin", "\u00a7aTienda eliminada para: \u00a7e%s");


        ES.put("msg.offer_added", "\u00a7aOferta agregada con exito!");
        ES.put("msg.offer_removed", "\u00a7aOferta eliminada!");
        ES.put("msg.offer_returned_storage", "\u00a7eInventario lleno, articulo movido al Almacen!");
        ES.put("msg.offer_returned_inventory", "\u00a7aArticulo devuelto al inventario!");

        ES.put("msg.select_item", "\u00a7cSelecciona el articulo a vender primero!");
        ES.put("msg.select_price", "\u00a7cFija el precio primero!");
        ES.put("msg.not_enough_items", "\u00a7cNecesitas \u00a7e%d %s\u00a7c! Tienes \u00a7e%d");
        ES.put("msg.not_enough_money", "\u00a7cNecesitas \u00a7e%d\u00a7c de \u00a7c%s \u00a7c- Tienes \u00a7e%d");

        ES.put("msg.trade_success", "\u00a7aIntercambio completado! Obtuviste \u00a7f%s");
        ES.put("msg.trade_item_taken", "\u00a7aTomaste \u00a7f%s \u00a7ax%d");
        ES.put("msg.inventory_full", "\u00a7cEl inventario esta lleno!");
        ES.put("msg.skin_not_found", "\u00a7e\u26a0 \u00a7cNo has colocado tu skin PNG en {path}\u00a7c. Tu cara aparecera como predeterminada en la Lista de Tiendas hasta que la agregues.");
        ES.put("skin.click_to_open", "\u00a7eClic para abrir carpeta");

        ES.put("storage.title", "Almacen - %s");
        ES.put("storage.page", "Pagina %d/%d | %d articulos");
        ES.put("storage.empty", "El almacen esta vacio");

        ES.put("picker.title", "Seleccionar Articulo de Precio");
        ES.put("picker.search", "Buscar...");
        ES.put("picker.page_info", "Pagina %d/%d | %d articulos");
        ES.put("picker.back", "Volver");

        ES.put("amount.title", "Establecer Cantidad");
        ES.put("amount.enter", "Introduce la cantidad como precio:");
        ES.put("amount.confirm", "Confirmar");

        ES.put("buyer.title", "Tienda de %s");
        ES.put("buyer.empty", "La tienda esta vacia");
        ES.put("buyer.buy", "Comprar");
        ES.put("buyer.details", "Detalles");
        ES.put("buyer.no_trades", "No hay intercambios disponibles");
        ES.put("buyer.offers_count", "Ofertas: %d");
        ES.put("buyer.you_get", "Obtendras:");
        ES.put("buyer.you_pay", "Pagaras:");
        ES.put("buyer.quantity", "Cantidad: %d");
        ES.put("buyer.for", "por");

        ES.put("select.title", "Seleccionar Articulo");
        ES.put("select.inventory", "Inventario");
        ES.put("select.hotbar", "Barra Rapida");
        ES.put("select.sell", "Vender");
        ES.put("select.hint", "Izquierdo: Tomar/Colocar todo | Derecho: Dividir/Colocar uno");

        ES.put("msg.target_has_shop", "\u00a7cEste jugador ya tiene una tienda! No puede tener mas de una.");
        ES.put("msg.shop_deleted_notify", "\u00a7cTu tienda ha sido eliminada. Escribe \u00a7e/shop recover \u00a7cpara recuperar tus articulos.");
        ES.put("msg.no_recovery_items", "\u00a7cNo hay articulos para recuperar.");
        ES.put("msg.all_recovered", "\u00a7aTodos tus articulos han sido recuperados!");
        ES.put("msg.recovery_inventory_full", "\u00a7cEl inventario esta lleno! Haz espacio e intenta de nuevo.");
        ES.put("msg.admin_only", "\u00a7cEste comando es solo para administradores!");
        ES.put("msg.transfer_success", "\u00a7aPropiedad de la tienda transferida con exito!");
        ES.put("msg.repair_success", "\u00a7aPropiedad de la tienda reparada con exito!");
        ES.put("msg.shop_not_found_admin", "\u00a7cTienda no encontrada para el jugador: \u00a7e%s");
        ES.put("msg.no_shops_without_uuid", "\u00a77No se encontraron tiendas sin UUID (todas ya estan vinculadas).");
        ES.put("msg.player_not_found", "\u00a7cJugador no encontrado: \u00a7e%s");
        ES.put("msg.no_shop_to_transfer", "\u00a7cEl jugador \u00a7e%s \u00a7cno tiene tienda!");
        ES.put("recovery.title", "Recuperacion de Articulos");
    }

    // ==================== Portuguese Brazil (pt_br) ====================
    private static void initPT() {
        PT.put("shop.title", "%s - Gerenciador da Loja");
        PT.put("shop.storage", "Armazenamento");
        PT.put("shop.set_price", "Definir Preco");
        PT.put("shop.add_offer", "Adicionar Oferta");
        PT.put("shop.close", "Fechar");
        PT.put("shop.open", "Abrir");
        PT.put("shop.sell_item", "Item a Venda:");
        PT.put("shop.price", "Preco:");
        PT.put("shop.offers", "Ofertas");
        PT.put("shop.no_offers", "Nenhuma oferta ainda");
        PT.put("shop.page", "Pagina %d/%d");
        PT.put("shop.items", "%d itens");
        PT.put("shop.add_buyable", "Adicionar Item a Venda:");
        PT.put("shop.set_price_label", "Definir Preco:");
        PT.put("shop.item_header", "Item");
        PT.put("shop.price_header", "Preco");
        PT.put("shop.cancel_header", "Cancelar");
        PT.put("shop.cancel_btn", "Cancelar X");
        PT.put("shop.move_label", "Mover Loja: ");
        PT.put("shop.move_on", "Ativado");
        PT.put("shop.move_off", "Desativado");
        PT.put("shop.item_price", "Preco do Item:");
        PT.put("shop.page_offers", "Pagina %d/%d | Ofertas: %d");
        PT.put("shop.next", ">");
        PT.put("shop.prev", "<");

        PT.put("shoplist.title", "Lista de Lojas");
        PT.put("shoplist.create", "Criar Sua Loja");
        PT.put("shoplist.open", "Abrir");
        PT.put("shoplist.close", "Fechar");
        PT.put("shoplist.empty", "Nenhuma loja ainda");
        PT.put("shoplist.count", "%d Lojas");
        PT.put("shoplist.shop_of", "Loja de %s");
        PT.put("shoplist.offer_single", "(%d oferta)");
        PT.put("shoplist.offer_plural", "(%d ofertas)");

        PT.put("msg.shop_created", "\u00a7aLoja criada: \u00a7eLoja de %s \u00a7a\u00a7r");
        PT.put("msg.shop_exists", "\u00a7cVoce ja tem uma loja! Use \u00a7e/shop close \u00a7cpara remove-la primeiro.");
        PT.put("msg.shop_closed", "\u00a7aLoja fechada com sucesso! \u00a77(Seus itens foram salvos)");
        PT.put("msg.shop_not_found", "\u00a7cO NPC da sua loja nao foi encontrado. Use \u00a7a/shop create \u00a7cpara cria-la novamente. \u00a77Suas ofertas ainda estao salvas.");
        PT.put("msg.shop_npc_not_found", "\u00a7cVoce nao tem uma loja!");
        PT.put("msg.shop_npc_killed", "\u00a7c\u2620 \u00a7eA loja de %s\u00a7c foi destruida por \u00a7e%s\u00a7c! As ofertas ainda estao salvas.");
        PT.put("msg.shop_not_exists", "\u00a7cLoja \u00a7e%s \u00a7cnao encontrada!");
        PT.put("msg.no_shops", "\u00a77Nenhuma loja ainda.");
        PT.put("msg.shop_list_header", "\u00a76\u2500\u2500\u2500\u2500\u2500\u2500\u2500 Lista de Lojas \u2500\u2500\u2500\u2500\u2500\u2500\u2500");
        PT.put("msg.shop_list_item", "\u00a7a\u25b6 \u00a7fLoja de %s \u00a7c(%d ofertas)");
        PT.put("msg.shop_deleted_admin", "\u00a7aLoja deletada para: \u00a7e%s");


        PT.put("msg.offer_added", "\u00a7aOferta adicionada com sucesso!");
        PT.put("msg.offer_removed", "\u00a7aOferta removida!");
        PT.put("msg.offer_returned_storage", "\u00a7eInventario cheio, item movido ao Armazenamento!");
        PT.put("msg.offer_returned_inventory", "\u00a7aItem devolvido ao inventario!");

        PT.put("msg.select_item", "\u00a7cSelecione o item para vender primeiro!");
        PT.put("msg.select_price", "\u00a7cDefina o preco primeiro!");
        PT.put("msg.not_enough_items", "\u00a7cVoce precisa de \u00a7e%d %s\u00a7c! Voce tem \u00a7e%d");
        PT.put("msg.not_enough_money", "\u00a7cVoce precisa de \u00a7e%d\u00a7c de \u00a7c%s \u00a7c- Voce tem \u00a7e%d");

        PT.put("msg.trade_success", "\u00a7aTroca concluida! Voce obteve \u00a7f%s");
        PT.put("msg.trade_item_taken", "\u00a7aPegou \u00a7f%s \u00a7ax%d");
        PT.put("msg.inventory_full", "\u00a7cO inventario esta cheio!");
        PT.put("msg.skin_not_found", "\u00a7e\u26a0 \u00a7cVoce nao colocou sua skin PNG em {path}\u00a7c. Seu rosto aparecera como padrao na Lista de Lojas ate voce adiciona-la.");
        PT.put("skin.click_to_open", "\u00a7eClique para abrir pasta");

        PT.put("storage.title", "Armazenamento - %s");
        PT.put("storage.page", "Pagina %d/%d | %d itens");
        PT.put("storage.empty", "O armazenamento esta vazio");

        PT.put("picker.title", "Selecionar Item de Preco");
        PT.put("picker.search", "Buscar...");
        PT.put("picker.page_info", "Pagina %d/%d | %d itens");
        PT.put("picker.back", "Voltar");

        PT.put("amount.title", "Definir Quantidade");
        PT.put("amount.enter", "Digite a quantidade como preco:");
        PT.put("amount.confirm", "Confirmar");

        PT.put("buyer.title", "Loja de %s");
        PT.put("buyer.empty", "A loja esta vazia");
        PT.put("buyer.buy", "Comprar");
        PT.put("buyer.details", "Detalhes");
        PT.put("buyer.no_trades", "Nenhum intercambio disponivel");
        PT.put("buyer.offers_count", "Ofertas: %d");
        PT.put("buyer.you_get", "Voce recebera:");
        PT.put("buyer.you_pay", "Voce pagara:");
        PT.put("buyer.quantity", "Quantidade: %d");
        PT.put("buyer.for", "por");

        PT.put("select.title", "Selecionar Item");
        PT.put("select.inventory", "Inventario");
        PT.put("select.hotbar", "Barra Rapida");
        PT.put("select.sell", "Vender");
        PT.put("select.hint", "Esquerdo: Pegar/Colocar tudo | Direito: Dividir/Colocar um");

        PT.put("msg.target_has_shop", "\u00a7cEste jogador ja tem uma loja! Nao pode ter mais de uma.");
        PT.put("msg.shop_deleted_notify", "\u00a7cSua loja foi excluida. Digite \u00a7e/shop recover \u00a7cpara recuperar seus itens.");
        PT.put("msg.no_recovery_items", "\u00a7cNenhum item para recuperar.");
        PT.put("msg.all_recovered", "\u00a7aTodos os seus itens foram recuperados!");
        PT.put("msg.recovery_inventory_full", "\u00a7cO inventario esta cheio! Faca espaco e tente novamente.");
        PT.put("msg.admin_only", "\u00a7cEste comando e apenas para administradores!");
        PT.put("msg.transfer_success", "\u00a7aPropriedade da loja transferida com sucesso!");
        PT.put("msg.repair_success", "\u00a7aPropriedade da loja reparada com sucesso!");
        PT.put("msg.shop_not_found_admin", "\u00a7cLoja nao encontrada para o jogador: \u00a7e%s");
        PT.put("msg.no_shops_without_uuid", "\u00a77Nenhuma loja encontrada sem UUID (todas ja estao vinculadas).");
        PT.put("msg.player_not_found", "\u00a7cJogador nao encontrado: \u00a7e%s");
        PT.put("msg.no_shop_to_transfer", "\u00a7cO jogador \u00a7e%s \u00a7cnao tem loja!");
        PT.put("recovery.title", "Recuperacao de Itens");
    }

    // ==================== Russian (ru_ru) ====================
    private static void initRU() {
        RU.put("shop.title", "%s - Upravlenie magazinom");
        RU.put("shop.storage", "Hranilishche");
        RU.put("shop.set_price", "Ustanovit tsenu");
        RU.put("shop.add_offer", "Dobavit tovar");
        RU.put("shop.close", "Zakryt");
        RU.put("shop.open", "Otkryt");
        RU.put("shop.sell_item", "Tovar dlya prodazhi:");
        RU.put("shop.price", "Tsena:");
        RU.put("shop.offers", "Tovary");
        RU.put("shop.no_offers", "Poka net tovarov");
        RU.put("shop.page", "Stranitsa %d/%d");
        RU.put("shop.items", "%d predmetov");
        RU.put("shop.add_buyable", "Dobavit tovar dlya prodazhi:");
        RU.put("shop.set_price_label", "Ustanovit tsenu:");
        RU.put("shop.item_header", "Predmet");
        RU.put("shop.price_header", "Tsena");
        RU.put("shop.cancel_header", "Otmena");
        RU.put("shop.cancel_btn", "Otmena X");
        RU.put("shop.move_label", "Peremeshenie magazina: ");
        RU.put("shop.move_on", "VKl");
        RU.put("shop.move_off", "VYKl");
        RU.put("shop.item_price", "Tsena predmeta:");
        RU.put("shop.page_offers", "Stranitsa %d/%d | Tovary: %d");
        RU.put("shop.next", ">");
        RU.put("shop.prev", "<");

        RU.put("shoplist.title", "Spisok magazinov");
        RU.put("shoplist.create", "Sozdat svoi magazin");
        RU.put("shoplist.open", "Otkryt");
        RU.put("shoplist.close", "Zakryt");
        RU.put("shoplist.empty", "Poka net magazinov");
        RU.put("shoplist.count", "%d magazinov");
        RU.put("shoplist.shop_of", "Magazin %s");
        RU.put("shoplist.offer_single", "(%d tovar)");
        RU.put("shoplist.offer_plural", "(%d tovara)");

        RU.put("msg.shop_created", "\u00a7aMagazin sozdan: \u00a7eMagazin %s \u00a7a\u00a7r");
        RU.put("msg.shop_exists", "\u00a7cU vas uzhe est magazin! Ispolzuite \u00a7e/shop close \u00a7cchtoby snachala udalit ego.");
        RU.put("msg.shop_closed", "\u00a7aMagazin uspeshno zakryt! \u00a77(Vashi predmety sokhraneny)");
        RU.put("msg.shop_not_found", "\u00a7cNPC vashego magazina ne naiden. Ispolzuite \u00a7a/shop create \u00a7cchtoby sozdat ego snova. \u00a77Vashi tovary po-prezhnemu sokhraneny.");
        RU.put("msg.shop_npc_not_found", "\u00a7cU vas net magazina!");
        RU.put("msg.shop_npc_killed", "\u00a7c\u2620 \u00a7eMagazin %s\u00a7c byl unichtozhen igrokom \u00a7e%s\u00a7c! Tovary po-prezhnemu sokhraneny.");
        RU.put("msg.shop_not_exists", "\u00a7cMagazin \u00a7e%s \u00a7cne naiden!");
        RU.put("msg.no_shops", "\u00a77Poka net magazinov.");
        RU.put("msg.shop_list_header", "\u00a76\u2500\u2500\u2500\u2500\u2500\u2500\u2500 Spisok magazinov \u2500\u2500\u2500\u2500\u2500\u2500\u2500");
        RU.put("msg.shop_list_item", "\u00a7a\u25b6 \u00a7fMagazin %s \u00a7c(%d tovarov)");
        RU.put("msg.shop_deleted_admin", "\u00a7aMagazin udalen dlya: \u00a7e%s");


        RU.put("msg.offer_added", "\u00a7aTovar dobavlen!");
        RU.put("msg.offer_removed", "\u00a7aTovar udalen!");
        RU.put("msg.offer_returned_storage", "\u00a7eInventar polon, predmet peremeshchen v khranilishche!");
        RU.put("msg.offer_returned_inventory", "\u00a7aPredmet vernut v inventar!");

        RU.put("msg.select_item", "\u00a7cSnachala vyberite predmet dlya prodazhi!");
        RU.put("msg.select_price", "\u00a7cSnachala ustanovite tsenu!");
        RU.put("msg.not_enough_items", "\u00a7cVam nuzhno \u00a7e%d %s\u00a7c! U vas \u00a7e%d");
        RU.put("msg.not_enough_money", "\u00a7cVam nuzhno \u00a7e%d\u00a7c iz \u00a7c%s \u00a7c- U vas \u00a7e%d");

        RU.put("msg.trade_success", "\u00a7aObmen zavershen! Vy poluchili \u00a7f%s");
        RU.put("msg.trade_item_taken", "\u00a7aVzyato \u00a7f%s \u00a7ax%d");
        RU.put("msg.inventory_full", "\u00a7cInventar polon!");
        RU.put("msg.skin_not_found", "\u00a7e\u26a0 \u00a7cVy ne pomestili svoy skin PNG v {path}\u00a7c. Vashe litso budet otobrazhat'sya po umolchaniyu v spiske magazinov, poka vy ego ne dobavite.");
        RU.put("skin.click_to_open", "\u00a7eNazhmite dlya otkrytiya papki");

        RU.put("storage.title", "Khranilishche - %s");
        RU.put("storage.page", "Stranitsa %d/%d | %d predmetov");
        RU.put("storage.empty", "Khranilishche pusto");

        RU.put("picker.title", "Vyberite predmet tseny");
        RU.put("picker.search", "Poisk...");
        RU.put("picker.page_info", "Stranitsa %d/%d | %d predmetov");
        RU.put("picker.back", "Nazad");

        RU.put("amount.title", "Ustanovit kolichestvo");
        RU.put("amount.enter", "Vvedite kolichestvo kak tsenu:");
        RU.put("amount.confirm", "Podtverdit");

        RU.put("buyer.title", "Magazin %s");
        RU.put("buyer.empty", "Magazin pust");
        RU.put("buyer.buy", "Kupit");
        RU.put("buyer.details", "Podrobnosti");
        RU.put("buyer.no_trades", "Net dostupnykh obmenov");
        RU.put("buyer.offers_count", "Tovarov: %d");
        RU.put("buyer.you_get", "Vy poluchite:");
        RU.put("buyer.you_pay", "Vy zaplatite:");
        RU.put("buyer.quantity", "Kolichestvo: %d");
        RU.put("buyer.for", "za");

        RU.put("select.title", "Vybor predmeta");
        RU.put("select.inventory", "Inventar");
        RU.put("select.hotbar", "Bystraya panel");
        RU.put("select.sell", "Prodat");
        RU.put("select.hint", "Levyy: Vzyat/Polozhit vse | Pravyy: Razdelit/Polozhit odin");

        RU.put("msg.target_has_shop", "\u00a7cU etogo igroka uzhe est' magazin! Nel'ya vladel' bolee chem odnim.");
        RU.put("msg.shop_deleted_notify", "\u00a7cVash magazin byl udalen. Napisite \u00a7e/shop recover \u00a7cdlya vosstanovleniya predmetov.");
        RU.put("msg.no_recovery_items", "\u00a7cNet predmetov dlya vosstanovleniya.");
        RU.put("msg.all_recovered", "\u00a7aVse vashi predmety vosstanovleny!");
        RU.put("msg.recovery_inventory_full", "\u00a7cInventar' polon! Oslobodite mesto i poprobujte snova.");
        RU.put("msg.admin_only", "\u00a7cEta komanda tol'ko dlya administratorov!");
        RU.put("msg.transfer_success", "\u00a7aSobstvennost' magazina uspeshno peredana!");
        RU.put("msg.repair_success", "\u00a7aSobstvennost' magazina uspeshno vosstanovlena!");
        RU.put("msg.shop_not_found_admin", "\u00a7cMagazin ne najden dlya igroka: \u00a7e%s");
        RU.put("msg.no_shops_without_uuid", "\u00a77Magazinov bez UUID ne najdeno (vse uzhe svyazany).");
        RU.put("msg.player_not_found", "\u00a7cIgrok ne najden: \u00a7e%s");
        RU.put("msg.no_shop_to_transfer", "\u00a7cU igroka \u00a7e%s \u00a7cnet magazina!");
        RU.put("recovery.title", "Vosstanovlenie predmetov");
    }

    // ==================== German (de_de) ====================
    private static void initDE() {
        DE.put("shop.title", "%s - Shop-Verwaltung");
        DE.put("shop.storage", "Lager");
        DE.put("shop.set_price", "Preis festlegen");
        DE.put("shop.add_offer", "Angebot hinzufuegen");
        DE.put("shop.close", "Schliessen");
        DE.put("shop.open", "Oeffnen");
        DE.put("shop.sell_item", "Verkaufsgegenstand:");
        DE.put("shop.price", "Preis:");
        DE.put("shop.offers", "Angebote");
        DE.put("shop.no_offers", "Noch keine Angebote");
        DE.put("shop.page", "Seite %d/%d");
        DE.put("shop.items", "%d Gegenstaende");
        DE.put("shop.add_buyable", "Kaufbaren Gegenstand hinzufuegen:");
        DE.put("shop.set_price_label", "Preis festlegen:");
        DE.put("shop.item_header", "Gegenstand");
        DE.put("shop.price_header", "Preis");
        DE.put("shop.cancel_header", "Abbrechen");
        DE.put("shop.cancel_btn", "Abbrechen X");
        DE.put("shop.move_label", "Shop bewegen: ");
        DE.put("shop.move_on", "An");
        DE.put("shop.move_off", "Aus");
        DE.put("shop.item_price", "Gegenstandspreis:");
        DE.put("shop.page_offers", "Seite %d/%d | Angebote: %d");
        DE.put("shop.next", ">");
        DE.put("shop.prev", "<");

        DE.put("shoplist.title", "Shop-Liste");
        DE.put("shoplist.create", "Erstelle deinen Shop");
        DE.put("shoplist.open", "Oeffnen");
        DE.put("shoplist.close", "Schliessen");
        DE.put("shoplist.empty", "Noch keine Shops");
        DE.put("shoplist.count", "%d Shops");
        DE.put("shoplist.shop_of", "%s's Shop");
        DE.put("shoplist.offer_single", "(%d Angebot)");
        DE.put("shoplist.offer_plural", "(%d Angebote)");

        DE.put("msg.shop_created", "\u00a7aShop erstellt: \u00a7e%s's Shop \u00a7a\u00a7r");
        DE.put("msg.shop_exists", "\u00a7cDu hast bereits einen Shop! Verwende \u00a7e/shop close \u00a7cum ihn zuerst zu entfernen.");
        DE.put("msg.shop_closed", "\u00a7aShop erfolgreich geschlossen! \u00a77(Deine Gegenstaende wurden gespeichert)");
        DE.put("msg.shop_not_found", "\u00a7cDein Shop-NPC wurde nicht gefunden. Verwende \u00a7a/shop create \u00a7cum ihn neu zu erstellen. \u00a77Deine Angebote sind weiterhin gespeichert.");
        DE.put("msg.shop_npc_not_found", "\u00a7cDu hast keinen Shop!");
        DE.put("msg.shop_npc_killed", "\u00a7c\u2620 \u00a7e%s's Shop\u00a7c wurde von \u00a7e%s\u00a7c zerstoert! Die Angebote sind weiterhin gespeichert.");
        DE.put("msg.shop_not_exists", "\u00a7cShop \u00a7e%s \u00a7cnicht gefunden!");
        DE.put("msg.no_shops", "\u00a77Noch keine Shops vorhanden.");
        DE.put("msg.shop_list_header", "\u00a76\u2500\u2500\u2500\u2500\u2500\u2500\u2500 Shop-Liste \u2500\u2500\u2500\u2500\u2500\u2500\u2500");
        DE.put("msg.shop_list_item", "\u00a7a\u25b6 \u00a7f%s's Shop \u00a7c(%d Angebote)");
        DE.put("msg.shop_deleted_admin", "\u00a7aShop geloescht fuer: \u00a7e%s");


        DE.put("msg.offer_added", "\u00a7aAngebot erfolgreich hinzugefuegt!");
        DE.put("msg.offer_removed", "\u00a7aAngebot entfernt!");
        DE.put("msg.offer_returned_storage", "\u00a7eInventar voll, Gegenstand ins Lager verschoben!");
        DE.put("msg.offer_returned_inventory", "\u00a7aGegenstand ins Inventar zurueckgegeben!");

        DE.put("msg.select_item", "\u00a7cWaehle zuerst einen Gegenstand zum Verkaufen!");
        DE.put("msg.select_price", "\u00a7cLege zuerst den Preis fest!");
        DE.put("msg.not_enough_items", "\u00a7cDu benoetigst \u00a7e%d %s\u00a7c! Du hast \u00a7e%d");
        DE.put("msg.not_enough_money", "\u00a7cDu benoetigst \u00a7e%d\u00a7c \u00a7c%s \u00a7c- Du hast \u00a7e%d");

        DE.put("msg.trade_success", "\u00a7aHandel abgeschlossen! Du hast \u00a7f%s erhalten");
        DE.put("msg.trade_item_taken", "\u00a7a\u00a7f%s \u00a7ax%d genommen");
        DE.put("msg.inventory_full", "\u00a7cInventar ist voll!");
        DE.put("msg.skin_not_found", "\u00a7e\u26a0 \u00a7cDu hast dein Skin PNG nicht in {path}\u00a7c abgelegt. Dein Gesicht wird in der Shop-Liste als Standard angezeigt, bis du es hinzufuegst.");
        DE.put("skin.click_to_open", "\u00a7eKlicken zum Oeffnen des Ordners");

        DE.put("storage.title", "Lager - %s");
        DE.put("storage.page", "Seite %d/%d | %d Gegenstaende");
        DE.put("storage.empty", "Lager ist leer");

        DE.put("picker.title", "Preisgegenstand auswaehlen");
        DE.put("picker.search", "Suchen...");
        DE.put("picker.page_info", "Seite %d/%d | %d Gegenstaende");
        DE.put("picker.back", "Zurueck");

        DE.put("amount.title", "Menge festlegen");
        DE.put("amount.enter", "Menge als Preis eingeben:");
        DE.put("amount.confirm", "Bestaetigen");

        DE.put("buyer.title", "%s's Shop");
        DE.put("buyer.empty", "Shop ist leer");
        DE.put("buyer.buy", "Kaufen");
        DE.put("buyer.details", "Details");
        DE.put("buyer.no_trades", "Keine Angebote verfuegbar");
        DE.put("buyer.offers_count", "Angebote: %d");
        DE.put("buyer.you_get", "Du erhaeltst:");
        DE.put("buyer.you_pay", "Du bezahlst:");
        DE.put("buyer.quantity", "Menge: %d");
        DE.put("buyer.for", "fuer");

        DE.put("select.title", "Gegenstand auswaehlen");
        DE.put("select.inventory", "Inventar");
        DE.put("select.hotbar", "Schnellleiste");
        DE.put("select.sell", "Verkaufen");
        DE.put("select.hint", "Links: Alles nehmen/platzieren | Rechts: Teilen/Einen platzieren");

        DE.put("msg.target_has_shop", "\u00a7cDieser Spieler hat bereits ein Geschaft! Man kann nur eines besitzen.");
        DE.put("msg.shop_deleted_notify", "\u00a7cDein Geschaft wurde geloescht. Tippe \u00a7e/shop recover \u00a7cum deine Gegenstaende zurueckzubekommen.");
        DE.put("msg.no_recovery_items", "\u00a7cKeine Gegenstaende zur Wiederherstellung.");
        DE.put("msg.all_recovered", "\u00a7aAlle deine Gegenstaende wurden wiederhergestellt!");
        DE.put("msg.recovery_inventory_full", "\u00a7cInventar ist voll! Mach Platz und versuche es erneut.");
        DE.put("msg.admin_only", "\u00a7cDieser Befehl ist nur fuer Administratoren!");
        DE.put("msg.transfer_success", "\u00a7aGeschaftsbesitz erfolgreich uebertragen!");
        DE.put("msg.repair_success", "\u00a7aGeschaftsbesitz erfolgreich repariert!");
        DE.put("msg.shop_not_found_admin", "\u00a7cGeschaft nicht gefunden fuer Spieler: \u00a7e%s");
        DE.put("msg.no_shops_without_uuid", "\u00a77Keine Geschaefte ohne UUID gefunden (alle sind bereits verknuepft).");
        DE.put("msg.player_not_found", "\u00a7cSpieler nicht gefunden: \u00a7e%s");
        DE.put("msg.no_shop_to_transfer", "\u00a7cSpieler \u00a7e%s \u00a7chat kein Geschaefte!");
        DE.put("recovery.title", "Gegenstaende wiederherstellen");
    }

    // ==================== French (fr_fr) ====================
    private static void initFR() {
        FR.put("shop.title", "%s - Gestionnaire de Boutique");
        FR.put("shop.storage", "Stockage");
        FR.put("shop.set_price", "Definir le Prix");
        FR.put("shop.add_offer", "Ajouter une Offre");
        FR.put("shop.close", "Fermer");
        FR.put("shop.open", "Ouvrir");
        FR.put("shop.sell_item", "Article a Vendre:");
        FR.put("shop.price", "Prix:");
        FR.put("shop.offers", "Offres");
        FR.put("shop.no_offers", "Pas encore d'offres");
        FR.put("shop.page", "Page %d/%d");
        FR.put("shop.items", "%d articles");
        FR.put("shop.add_buyable", "Ajouter un Article a Vendre:");
        FR.put("shop.set_price_label", "Definir le Prix:");
        FR.put("shop.item_header", "Article");
        FR.put("shop.price_header", "Prix");
        FR.put("shop.cancel_header", "Annuler");
        FR.put("shop.cancel_btn", "Annuler X");
        FR.put("shop.move_label", "Deplacer Boutique: ");
        FR.put("shop.move_on", "Actif");
        FR.put("shop.move_off", "Inactif");
        FR.put("shop.item_price", "Prix de l'Article:");
        FR.put("shop.page_offers", "Page %d/%d | Offres: %d");
        FR.put("shop.next", ">");
        FR.put("shop.prev", "<");

        FR.put("shoplist.title", "Liste des Boutiques");
        FR.put("shoplist.create", "Creer Votre Boutique");
        FR.put("shoplist.open", "Ouvrir");
        FR.put("shoplist.close", "Fermer");
        FR.put("shoplist.empty", "Pas encore de boutiques");
        FR.put("shoplist.count", "%d Boutiques");
        FR.put("shoplist.shop_of", "Boutique de %s");
        FR.put("shoplist.offer_single", "(%d offre)");
        FR.put("shoplist.offer_plural", "(%d offres)");

        FR.put("msg.shop_created", "\u00a7aBoutique creee: \u00a7eBoutique de %s \u00a7a\u00a7r");
        FR.put("msg.shop_exists", "\u00a7cVous avez deja une boutique! Utilisez \u00a7e/shop close \u00a7cpour la supprimer d'abord.");
        FR.put("msg.shop_closed", "\u00a7aBoutique fermee avec succes! \u00a77(Vos articles sont sauvegardes)");
        FR.put("msg.shop_not_found", "\u00a7cLe NPC de votre boutique n'a pas ete trouve. Utilisez \u00a7a/shop create \u00a7cpour la recreer. \u00a77Vos offres sont toujours sauvegardees.");
        FR.put("msg.shop_npc_not_found", "\u00a7cVous n'avez pas de boutique!");
        FR.put("msg.shop_npc_killed", "\u00a7c\u2620 \u00a7eLa boutique de %s\u00a7c a ete detruite par \u00a7e%s\u00a7c! Les offres sont toujours sauvegardees.");
        FR.put("msg.shop_not_exists", "\u00a7cBoutique \u00a7e%s \u00a7cintrouvable!");
        FR.put("msg.no_shops", "\u00a77Pas encore de boutiques.");
        FR.put("msg.shop_list_header", "\u00a76\u2500\u2500\u2500\u2500\u2500\u2500\u2500 Liste des Boutiques \u2500\u2500\u2500\u2500\u2500\u2500\u2500");
        FR.put("msg.shop_list_item", "\u00a7a\u25b6 \u00a7fBoutique de %s \u00a7c(%d offres)");

        FR.put("msg.offer_added", "\u00a7aOffre ajoutee avec succes!");
        FR.put("msg.offer_removed", "\u00a7aOffre supprimee!");
        FR.put("msg.offer_returned_storage", "\u00a7eInventaire plein, article deplace dans le Stockage!");
        FR.put("msg.offer_returned_inventory", "\u00a7aArticle retourne dans l'inventaire!");
        FR.put("msg.shop_deleted_admin", "\u00a7aBoutique supprimee pour: \u00a7e%s");


        FR.put("msg.select_item", "\u00a7cSelectionnez d'abord l'article a vendre!");
        FR.put("msg.select_price", "\u00a7cDefinissez d'abord le prix!");
        FR.put("msg.not_enough_items", "\u00a7cVous avez besoin de \u00a7e%d %s\u00a7c! Vous avez \u00a7e%d");
        FR.put("msg.not_enough_money", "\u00a7cVous avez besoin de \u00a7e%d\u00a7c de \u00a7c%s \u00a7c- Vous avez \u00a7e%d");

        FR.put("msg.trade_success", "\u00a7aEchange termine! Vous avez obtenu \u00a7f%s");
        FR.put("msg.trade_item_taken", "\u00a7aPris \u00a7f%s \u00a7ax%d");
        FR.put("msg.inventory_full", "\u00a7cL'inventaire est plein!");
        FR.put("msg.skin_not_found", "\u00a7e\u26a0 \u00a7cVous n'avez pas place votre skin PNG dans {path}\u00a7c. Votre visage apparaitra par defaut dans la Liste des Boutiques jusqu'a ce que vous l'ajoutiez.");
        FR.put("skin.click_to_open", "\u00a7eCliquez pour ouvrir le dossier");

        FR.put("storage.title", "Stockage - %s");
        FR.put("storage.page", "Page %d/%d | %d articles");
        FR.put("storage.empty", "Le stockage est vide");

        FR.put("picker.title", "Selectionner l'Article de Prix");
        FR.put("picker.search", "Rechercher...");
        FR.put("picker.page_info", "Page %d/%d | %d articles");
        FR.put("picker.back", "Retour");

        FR.put("amount.title", "Definir la Quantite");
        FR.put("amount.enter", "Entrez la quantite comme prix:");
        FR.put("amount.confirm", "Confirmer");

        FR.put("buyer.title", "Boutique de %s");
        FR.put("buyer.empty", "La boutique est vide");
        FR.put("buyer.buy", "Acheter");
        FR.put("buyer.details", "Details");
        FR.put("buyer.no_trades", "Aucun echange disponible");
        FR.put("buyer.offers_count", "Offres: %d");
        FR.put("buyer.you_get", "Vous obtiendrez:");
        FR.put("buyer.you_pay", "Vous paierez:");
        FR.put("buyer.quantity", "Quantite: %d");
        FR.put("buyer.for", "pour");

        FR.put("select.title", "Selectionner un Article");
        FR.put("select.inventory", "Inventaire");
        FR.put("select.hotbar", "Barre d'actions");
        FR.put("select.sell", "Vendre");
        FR.put("select.hint", "Gauche: Prendre/Placer tout | Droit: Diviser/Placer un");

        FR.put("msg.target_has_shop", "\u00a7cCe joueur a deja une boutique! On ne peut en posseder qu'une.");
        FR.put("msg.shop_deleted_notify", "\u00a7cVotre boutique a ete supprimee. Tapez \u00a7e/shop recover \u00a7cpour recuperer vos objets.");
        FR.put("msg.no_recovery_items", "\u00a7cAucun objet a recuperer.");
        FR.put("msg.all_recovered", "\u00a7aTous vos objets ont ete recuperes!");
        FR.put("msg.recovery_inventory_full", "\u00a7cL'inventaire est plein! Faites de la place et reessayez.");
        FR.put("msg.admin_only", "\u00a7cCette commande est reservee aux administrateurs!");
        FR.put("msg.transfer_success", "\u00a7aPropriete de la boutique transferee avec succes!");
        FR.put("msg.repair_success", "\u00a7aPropriete de la boutique reparee avec succes!");
        FR.put("msg.shop_not_found_admin", "\u00a7cBoutique non trouvee pour le joueur: \u00a7e%s");
        FR.put("msg.no_shops_without_uuid", "\u00a77Aucune boutique sans UUID trouvee (toutes sont deja liees).");
        FR.put("msg.player_not_found", "\u00a7cJoueur non trouve: \u00a7e%s");
        FR.put("msg.no_shop_to_transfer", "\u00a7cLe joueur \u00a7e%s \u00a7cn'a pas de boutique!");
        FR.put("recovery.title", "Recuperation d'objets");
    }

    // ==================== Language Detection ====================

    /**
     * Detects the current Minecraft language and switches to the closest supported language.
     * Falls back to English if no match is found.
     */
    public static void detectLanguage(String mcLangCode) {
        if (mcLangCode == null) mcLangCode = "en_us";
        mcLangCode = mcLangCode.toLowerCase(Locale.ROOT);

        // Direct match
        if (ALL_LANGS.containsKey(mcLangCode)) {
            currentLang = mcLangCode;
            langDetected = true;
            System.out.println("[ShopMod] I18n: Language set to " + currentLang);
            return;
        }

        // Alias match (e.g. en_gb -> en_us)
        if (LANG_ALIASES.containsKey(mcLangCode)) {
            currentLang = LANG_ALIASES.get(mcLangCode);
            langDetected = true;
            System.out.println("[ShopMod] I18n: Language " + mcLangCode + " aliased to " + currentLang);
            return;
        }

        // Base language match (e.g. "ar" matches "ar_sa")
        String baseLang = mcLangCode.split("_")[0];
        for (String key : ALL_LANGS.keySet()) {
            if (key.startsWith(baseLang + "_")) {
                currentLang = key;
                langDetected = true;
                System.out.println("[ShopMod] I18n: Language " + mcLangCode + " matched to " + currentLang);
                return;
            }
        }

        // Fallback to English
        currentLang = "en_us";
        langDetected = true;
        System.out.println("[ShopMod] I18n: No match for " + mcLangCode + ", falling back to en_us");
    }

    /**
     * Force set a specific language code.
     */
    public static void setLanguage(String langCode) {
        if (langCode == null) return;
        langCode = langCode.toLowerCase(Locale.ROOT);
        if (ALL_LANGS.containsKey(langCode)) {
            currentLang = langCode;
        } else if (LANG_ALIASES.containsKey(langCode)) {
            currentLang = LANG_ALIASES.get(langCode);
        }
    }

    /**
     * Returns the current language code.
     */
    public static String getCurrentLanguage() {
        return currentLang;
    }

    /**
     * Main translation method. Looks up key in current language, falls back to English.
     */
    public static String get(String key, Object... args) {
        Map<String, String> lang = ALL_LANGS.get(currentLang);
        if (lang == null) lang = EN;

        String template = lang.getOrDefault(key, EN.getOrDefault(key, key));
        if (args.length > 0) {
            return String.format(template, args);
        }
        return template;
    }
}