package com.example.shopmod.network;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public final class ModPackets {

    // S2C IDs
    public static final CustomPayload.Id<OpenOwnerPayload>  OPEN_OWNER_ID   = new CustomPayload.Id<>(Identifier.of("shopmod","open_shop_owner"));
    public static final CustomPayload.Id<OpenBuyerPayload>  OPEN_BUYER_ID   = new CustomPayload.Id<>(Identifier.of("shopmod","open_shop_buyer"));
    public static final CustomPayload.Id<OpenPickerPayload> OPEN_PICKER_ID  = new CustomPayload.Id<>(Identifier.of("shopmod","open_item_picker"));
    public static final CustomPayload.Id<OpenAmountPayload> OPEN_AMOUNT_ID  = new CustomPayload.Id<>(Identifier.of("shopmod","open_amount_input"));
    public static final CustomPayload.Id<SyncShopPayload>   SYNC_SHOP_ID    = new CustomPayload.Id<>(Identifier.of("shopmod","sync_shop_data"));
    public static final CustomPayload.Id<OpenStoragePayload> OPEN_STORAGE_ID = new CustomPayload.Id<>(Identifier.of("shopmod","open_storage"));

    // S2C - Shop List
    public static final CustomPayload.Id<OpenShopsListPayload> OPEN_SHOPS_LIST_ID = new CustomPayload.Id<>(Identifier.of("shopmod","open_shops_list"));

    // S2C - Shop navigation data (sent alongside any shop opening)
    public static final CustomPayload.Id<ShopNavPayload> SHOP_NAV_ID = new CustomPayload.Id<>(Identifier.of("shopmod","shop_nav"));

    // C2S IDs
    public static final CustomPayload.Id<AddTradePayload>       ADD_TRADE_ID        = new CustomPayload.Id<>(Identifier.of("shopmod","add_trade"));
    public static final CustomPayload.Id<AddEnchantedBookTradePayload> ADD_BOOK_TRADE_ID = new CustomPayload.Id<>(Identifier.of("shopmod","add_book_trade"));
    public static final CustomPayload.Id<RemoveTradePayload>    REMOVE_TRADE_ID     = new CustomPayload.Id<>(Identifier.of("shopmod","remove_trade"));
    public static final CustomPayload.Id<DoTradePayload>        DO_TRADE_ID         = new CustomPayload.Id<>(Identifier.of("shopmod","do_trade"));
    public static final CustomPayload.Id<ReqPickerPayload>      REQ_PICKER_ID       = new CustomPayload.Id<>(Identifier.of("shopmod","request_item_picker"));
    public static final CustomPayload.Id<ReqAmountPayload>      REQ_AMOUNT_ID       = new CustomPayload.Id<>(Identifier.of("shopmod","request_amount"));
    public static final CustomPayload.Id<ReqOwnerScreenPayload> REQ_OWNER_SCREEN_ID = new CustomPayload.Id<>(Identifier.of("shopmod","request_owner_screen"));
    public static final CustomPayload.Id<ReqStoragePayload>     REQ_STORAGE_ID      = new CustomPayload.Id<>(Identifier.of("shopmod","request_storage"));
    public static final CustomPayload.Id<TakeStoragePayload>    TAKE_STORAGE_ID     = new CustomPayload.Id<>(Identifier.of("shopmod","take_storage"));
    public static final CustomPayload.Id<OpenShopFromListPayload> OPEN_SHOP_FROM_LIST_ID = new CustomPayload.Id<>(Identifier.of("shopmod","open_shop_from_list"));
    public static final CustomPayload.Id<CreateShopFromListPayload> CREATE_SHOP_FROM_LIST_ID = new CustomPayload.Id<>(Identifier.of("shopmod","create_shop_from_list"));

    // Shop entry info for the list
    public record ShopEntryInfo(String ownerName, long uuidMost, long uuidLeast, int offerCount) {}

    // S2C Payloads

    public record OpenShopsListPayload(
        List<ShopEntryInfo> shops,
        boolean playerHasShop,
        String playerName,
        long playerUuidMost,
        long playerUuidLeast
    ) implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, OpenShopsListPayload> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeVarInt(value.shops().size());
                for (ShopEntryInfo entry : value.shops()) {
                    buf.writeString(entry.ownerName());
                    buf.writeLong(entry.uuidMost());
                    buf.writeLong(entry.uuidLeast());
                    buf.writeVarInt(entry.offerCount());
                }
                buf.writeBoolean(value.playerHasShop());
                buf.writeString(value.playerName());
                buf.writeLong(value.playerUuidMost());
                buf.writeLong(value.playerUuidLeast());
            },
            buf -> {
                int size = buf.readVarInt();
                List<ShopEntryInfo> shops = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    shops.add(new ShopEntryInfo(
                        buf.readString(),
                        buf.readLong(),
                        buf.readLong(),
                        buf.readVarInt()
                    ));
                }
                return new OpenShopsListPayload(
                    shops,
                    buf.readBoolean(),
                    buf.readString(),
                    buf.readLong(),
                    buf.readLong()
                );
            }
        );
        @Override public Id<OpenShopsListPayload> getId() { return OPEN_SHOPS_LIST_ID; }
    }

    public record OpenOwnerPayload(String shopName, NbtCompound data) implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, OpenOwnerPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, OpenOwnerPayload::shopName,
            PacketCodecs.NBT_COMPOUND, OpenOwnerPayload::data,
            OpenOwnerPayload::new);
        @Override public Id<OpenOwnerPayload> getId() { return OPEN_OWNER_ID; }
    }

    public record OpenBuyerPayload(String shopName, NbtCompound data) implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, OpenBuyerPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, OpenBuyerPayload::shopName,
            PacketCodecs.NBT_COMPOUND, OpenBuyerPayload::data,
            OpenBuyerPayload::new);
        @Override public Id<OpenBuyerPayload> getId() { return OPEN_BUYER_ID; }
    }

    public record OpenPickerPayload(String shopName) implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, OpenPickerPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, OpenPickerPayload::shopName, OpenPickerPayload::new);
        @Override public Id<OpenPickerPayload> getId() { return OPEN_PICKER_ID; }
    }

    public record OpenAmountPayload(String shopName, String itemId) implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, OpenAmountPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, OpenAmountPayload::shopName,
            PacketCodecs.STRING, OpenAmountPayload::itemId,
            OpenAmountPayload::new);
        @Override public Id<OpenAmountPayload> getId() { return OPEN_AMOUNT_ID; }
    }

    public record SyncShopPayload(NbtCompound data) implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, SyncShopPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.NBT_COMPOUND, SyncShopPayload::data, SyncShopPayload::new);
        @Override public Id<SyncShopPayload> getId() { return SYNC_SHOP_ID; }
    }

    public record OpenStoragePayload(String shopName, NbtCompound data) implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, OpenStoragePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, OpenStoragePayload::shopName,
            PacketCodecs.NBT_COMPOUND, OpenStoragePayload::data,
            OpenStoragePayload::new);
        @Override public Id<OpenStoragePayload> getId() { return OPEN_STORAGE_ID; }
    }

    // C2S Payloads

    public record OpenShopFromListPayload(String ownerName) implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, OpenShopFromListPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, OpenShopFromListPayload::ownerName,
            OpenShopFromListPayload::new);
        @Override public Id<OpenShopFromListPayload> getId() { return OPEN_SHOP_FROM_LIST_ID; }
    }

    public record CreateShopFromListPayload() implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, CreateShopFromListPayload> CODEC = PacketCodec.unit(new CreateShopFromListPayload());
        @Override public Id<CreateShopFromListPayload> getId() { return CREATE_SHOP_FROM_LIST_ID; }
    }

    public record AddTradePayload(String shopName, String sellId, int sellCount,
                                  String buyId, int buyCount) implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, AddTradePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING,  AddTradePayload::shopName,
            PacketCodecs.STRING,  AddTradePayload::sellId,
            PacketCodecs.VAR_INT, AddTradePayload::sellCount,
            PacketCodecs.STRING,  AddTradePayload::buyId,
            PacketCodecs.VAR_INT, AddTradePayload::buyCount,
            AddTradePayload::new);
        @Override public Id<AddTradePayload> getId() { return ADD_TRADE_ID; }
    }

    public record RemoveTradePayload(String shopName, int index) implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, RemoveTradePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, RemoveTradePayload::shopName,
            PacketCodecs.VAR_INT, RemoveTradePayload::index,
            RemoveTradePayload::new);
        @Override public Id<RemoveTradePayload> getId() { return REMOVE_TRADE_ID; }
    }

    public record DoTradePayload(String shopName, int index) implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, DoTradePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, DoTradePayload::shopName,
            PacketCodecs.VAR_INT, DoTradePayload::index,
            DoTradePayload::new);
        @Override public Id<DoTradePayload> getId() { return DO_TRADE_ID; }
    }

    public record ReqPickerPayload(String shopName) implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, ReqPickerPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, ReqPickerPayload::shopName, ReqPickerPayload::new);
        @Override public Id<ReqPickerPayload> getId() { return REQ_PICKER_ID; }
    }

    public record ReqAmountPayload(String shopName, String itemId) implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, ReqAmountPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, ReqAmountPayload::shopName,
            PacketCodecs.STRING, ReqAmountPayload::itemId,
            ReqAmountPayload::new);
        @Override public Id<ReqAmountPayload> getId() { return REQ_AMOUNT_ID; }
    }

    public record ReqOwnerScreenPayload(String shopName) implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, ReqOwnerScreenPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, ReqOwnerScreenPayload::shopName, ReqOwnerScreenPayload::new);
        @Override public Id<ReqOwnerScreenPayload> getId() { return REQ_OWNER_SCREEN_ID; }
    }

    public record ReqStoragePayload(String shopName) implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, ReqStoragePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, ReqStoragePayload::shopName, ReqStoragePayload::new);
        @Override public Id<ReqStoragePayload> getId() { return REQ_STORAGE_ID; }
    }

    public record TakeStoragePayload(String shopName, int index) implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, TakeStoragePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, TakeStoragePayload::shopName,
            PacketCodecs.VAR_INT, TakeStoragePayload::index,
            TakeStoragePayload::new);
        @Override public Id<TakeStoragePayload> getId() { return TAKE_STORAGE_ID; }
    }

    public record AddEnchantedBookTradePayload(
        String    shopName,
        NbtCompound sellData,   // NBT كامل للـ sell item
        NbtCompound buyData     // NBT كامل للـ buy item (الكتاب)
    ) implements CustomPayload {
        public static final Id<AddEnchantedBookTradePayload> ID = ADD_BOOK_TRADE_ID;

        public static final PacketCodec<PacketByteBuf, AddEnchantedBookTradePayload> CODEC =
            PacketCodec.tuple(
                PacketCodecs.STRING,    AddEnchantedBookTradePayload::shopName,
                PacketCodecs.NBT_COMPOUND, AddEnchantedBookTradePayload::sellData,
                PacketCodecs.NBT_COMPOUND, AddEnchantedBookTradePayload::buyData,
                AddEnchantedBookTradePayload::new
            );

        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record ShopNavPayload(List<String> ownerNames) implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, ShopNavPayload> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeVarInt(value.ownerNames().size());
                for (String name : value.ownerNames()) {
                    buf.writeString(name);
                }
            },
            buf -> {
                int size = buf.readVarInt();
                List<String> names = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    names.add(buf.readString());
                }
                return new ShopNavPayload(names);
            }
        );
        @Override public Id<ShopNavPayload> getId() { return SHOP_NAV_ID; }
    }

    private ModPackets() {}
}
