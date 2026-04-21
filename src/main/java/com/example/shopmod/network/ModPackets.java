package com.example.shopmod.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class ModPackets {

    // ========================================================================
    // NBT helpers - serialize CompoundTag to/from FriendlyByteBuf
    // (replaces ByteBufCodecs.compoundTagCodec which had type mismatches)
    // ========================================================================

    private static void writeNbt(FriendlyByteBuf buf, CompoundTag tag) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            NbtIo.write(tag, new DataOutputStream(baos));
            buf.writeByteArray(baos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to write NBT", e);
        }
    }

    private static CompoundTag readNbt(FriendlyByteBuf buf) {
        try {
            byte[] data = buf.readByteArray();
            Tag tag = NbtIo.read(new DataInputStream(new ByteArrayInputStream(data)));
            if (tag == null) return new CompoundTag();
            if (tag instanceof CompoundTag compound) return compound;
            // If NbtIo.read returns CompoundTag directly, cast it
            return (tag instanceof CompoundTag) ? (CompoundTag) tag : new CompoundTag();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read NBT", e);
        }
    }

    // S2C IDs
    public static final CustomPacketPayload.Type<OpenOwnerPayload>  OPEN_OWNER_ID   = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("shopmod", "open_shop_owner"));
    public static final CustomPacketPayload.Type<OpenBuyerPayload>  OPEN_BUYER_ID   = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("shopmod", "open_shop_buyer"));
    public static final CustomPacketPayload.Type<OpenPickerPayload> OPEN_PICKER_ID  = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("shopmod", "open_item_picker"));
    public static final CustomPacketPayload.Type<OpenAmountPayload> OPEN_AMOUNT_ID  = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("shopmod", "open_amount_input"));
    public static final CustomPacketPayload.Type<SyncShopPayload>   SYNC_SHOP_ID    = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("shopmod", "sync_shop_data"));
    public static final CustomPacketPayload.Type<OpenStoragePayload> OPEN_STORAGE_ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("shopmod", "open_storage"));

    // S2C - Shop List
    public static final CustomPacketPayload.Type<OpenShopsListPayload> OPEN_SHOPS_LIST_ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("shopmod", "open_shops_list"));

    // S2C - Shop navigation data
    public static final CustomPacketPayload.Type<ShopNavPayload> SHOP_NAV_ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("shopmod", "shop_nav"));

    // C2S IDs
    public static final CustomPacketPayload.Type<AddTradePayload>       ADD_TRADE_ID        = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("shopmod", "add_trade"));
    public static final CustomPacketPayload.Type<AddEnchantedBookTradePayload> ADD_BOOK_TRADE_ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("shopmod", "add_book_trade"));
    public static final CustomPacketPayload.Type<RemoveTradePayload>    REMOVE_TRADE_ID     = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("shopmod", "remove_trade"));
    public static final CustomPacketPayload.Type<DoTradePayload>        DO_TRADE_ID         = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("shopmod", "do_trade"));
    public static final CustomPacketPayload.Type<ReqPickerPayload>      REQ_PICKER_ID       = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("shopmod", "request_item_picker"));
    public static final CustomPacketPayload.Type<ReqAmountPayload>      REQ_AMOUNT_ID       = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("shopmod", "request_amount"));
    public static final CustomPacketPayload.Type<ReqOwnerScreenPayload> REQ_OWNER_SCREEN_ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("shopmod", "request_owner_screen"));
    public static final CustomPacketPayload.Type<ReqStoragePayload>     REQ_STORAGE_ID      = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("shopmod", "request_storage"));
    public static final CustomPacketPayload.Type<TakeStoragePayload>    TAKE_STORAGE_ID     = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("shopmod", "take_storage"));
    public static final CustomPacketPayload.Type<OpenShopFromListPayload> OPEN_SHOP_FROM_LIST_ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("shopmod", "open_shop_from_list"));
    public static final CustomPacketPayload.Type<CreateShopFromListPayload> CREATE_SHOP_FROM_LIST_ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("shopmod", "create_shop_from_list"));
    public static final CustomPacketPayload.Type<ToggleShopMovePayload> TOGGLE_SHOP_MOVE_ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("shopmod", "toggle_shop_move"));

    // Shop entry info for the list
    public record ShopEntryInfo(String ownerName, long uuidMost, long uuidLeast, int offerCount) {}

    // ============================================================
    // S2C Payloads
    // ============================================================

    public record OpenShopsListPayload(
        List<ShopEntryInfo> shops,
        boolean playerHasShop,
        String playerName,
        long playerUuidMost,
        long playerUuidLeast
    ) implements CustomPacketPayload {
        public static final StreamCodec<FriendlyByteBuf, OpenShopsListPayload> CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeVarInt(value.shops().size());
                for (ShopEntryInfo entry : value.shops()) {
                    buf.writeUtf(entry.ownerName());
                    buf.writeLong(entry.uuidMost());
                    buf.writeLong(entry.uuidLeast());
                    buf.writeVarInt(entry.offerCount());
                }
                buf.writeBoolean(value.playerHasShop());
                buf.writeUtf(value.playerName());
                buf.writeLong(value.playerUuidMost());
                buf.writeLong(value.playerUuidLeast());
            },
            buf -> {
                int size = buf.readVarInt();
                List<ShopEntryInfo> shops = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    shops.add(new ShopEntryInfo(
                        buf.readUtf(),
                        buf.readLong(),
                        buf.readLong(),
                        buf.readVarInt()
                    ));
                }
                return new OpenShopsListPayload(
                    shops,
                    buf.readBoolean(),
                    buf.readUtf(),
                    buf.readLong(),
                    buf.readLong()
                );
            }
        );
        @Override public CustomPacketPayload.Type<OpenShopsListPayload> type() { return OPEN_SHOPS_LIST_ID; }
    }

    public record OpenOwnerPayload(String shopName, CompoundTag data) implements CustomPacketPayload {
        public static final StreamCodec<FriendlyByteBuf, OpenOwnerPayload> CODEC = StreamCodec.of(
            (buf, v) -> {
                buf.writeUtf(v.shopName());
                writeNbt(buf, v.data());
            },
            buf -> new OpenOwnerPayload(buf.readUtf(), readNbt(buf))
        );
        @Override public CustomPacketPayload.Type<OpenOwnerPayload> type() { return OPEN_OWNER_ID; }
    }

    public record OpenBuyerPayload(String shopName, CompoundTag data) implements CustomPacketPayload {
        public static final StreamCodec<FriendlyByteBuf, OpenBuyerPayload> CODEC = StreamCodec.of(
            (buf, v) -> {
                buf.writeUtf(v.shopName());
                writeNbt(buf, v.data());
            },
            buf -> new OpenBuyerPayload(buf.readUtf(), readNbt(buf))
        );
        @Override public CustomPacketPayload.Type<OpenBuyerPayload> type() { return OPEN_BUYER_ID; }
    }

    public record OpenPickerPayload(String shopName) implements CustomPacketPayload {
        public static final StreamCodec<FriendlyByteBuf, OpenPickerPayload> CODEC = StreamCodec.of(
            (buf, v) -> buf.writeUtf(v.shopName()),
            buf -> new OpenPickerPayload(buf.readUtf())
        );
        @Override public CustomPacketPayload.Type<OpenPickerPayload> type() { return OPEN_PICKER_ID; }
    }

    public record OpenAmountPayload(String shopName, String itemId) implements CustomPacketPayload {
        public static final StreamCodec<FriendlyByteBuf, OpenAmountPayload> CODEC = StreamCodec.of(
            (buf, v) -> {
                buf.writeUtf(v.shopName());
                buf.writeUtf(v.itemId());
            },
            buf -> new OpenAmountPayload(buf.readUtf(), buf.readUtf())
        );
        @Override public CustomPacketPayload.Type<OpenAmountPayload> type() { return OPEN_AMOUNT_ID; }
    }

    public record SyncShopPayload(CompoundTag data) implements CustomPacketPayload {
        public static final StreamCodec<FriendlyByteBuf, SyncShopPayload> CODEC = StreamCodec.of(
            (buf, v) -> writeNbt(buf, v.data()),
            buf -> new SyncShopPayload(readNbt(buf))
        );
        @Override public CustomPacketPayload.Type<SyncShopPayload> type() { return SYNC_SHOP_ID; }
    }

    public record OpenStoragePayload(String shopName, CompoundTag data) implements CustomPacketPayload {
        public static final StreamCodec<FriendlyByteBuf, OpenStoragePayload> CODEC = StreamCodec.of(
            (buf, v) -> {
                buf.writeUtf(v.shopName());
                writeNbt(buf, v.data());
            },
            buf -> new OpenStoragePayload(buf.readUtf(), readNbt(buf))
        );
        @Override public CustomPacketPayload.Type<OpenStoragePayload> type() { return OPEN_STORAGE_ID; }
    }

    // ============================================================
    // C2S Payloads
    // ============================================================

    public record OpenShopFromListPayload(String ownerName) implements CustomPacketPayload {
        public static final StreamCodec<FriendlyByteBuf, OpenShopFromListPayload> CODEC = StreamCodec.of(
            (buf, v) -> buf.writeUtf(v.ownerName()),
            buf -> new OpenShopFromListPayload(buf.readUtf())
        );
        @Override public CustomPacketPayload.Type<OpenShopFromListPayload> type() { return OPEN_SHOP_FROM_LIST_ID; }
    }

    public record CreateShopFromListPayload() implements CustomPacketPayload {
        public static final StreamCodec<FriendlyByteBuf, CreateShopFromListPayload> CODEC = StreamCodec.unit(new CreateShopFromListPayload());
        @Override public CustomPacketPayload.Type<CreateShopFromListPayload> type() { return CREATE_SHOP_FROM_LIST_ID; }
    }

    public record AddTradePayload(String shopName, String sellId, int sellCount,
                                  String buyId, int buyCount) implements CustomPacketPayload {
        public static final StreamCodec<FriendlyByteBuf, AddTradePayload> CODEC = StreamCodec.of(
            (buf, v) -> {
                buf.writeUtf(v.shopName());
                buf.writeUtf(v.sellId());
                buf.writeVarInt(v.sellCount());
                buf.writeUtf(v.buyId());
                buf.writeVarInt(v.buyCount());
            },
            buf -> new AddTradePayload(
                buf.readUtf(),   // shopName
                buf.readUtf(),   // sellId
                buf.readVarInt(), // sellCount
                buf.readUtf(),   // buyId
                buf.readVarInt()  // buyCount
            )
        );
        @Override public CustomPacketPayload.Type<AddTradePayload> type() { return ADD_TRADE_ID; }
    }

    public record RemoveTradePayload(String shopName, int index) implements CustomPacketPayload {
        public static final StreamCodec<FriendlyByteBuf, RemoveTradePayload> CODEC = StreamCodec.of(
            (buf, v) -> {
                buf.writeUtf(v.shopName());
                buf.writeVarInt(v.index());
            },
            buf -> new RemoveTradePayload(buf.readUtf(), buf.readVarInt())
        );
        @Override public CustomPacketPayload.Type<RemoveTradePayload> type() { return REMOVE_TRADE_ID; }
    }

    public record DoTradePayload(String shopName, int index) implements CustomPacketPayload {
        public static final StreamCodec<FriendlyByteBuf, DoTradePayload> CODEC = StreamCodec.of(
            (buf, v) -> {
                buf.writeUtf(v.shopName());
                buf.writeVarInt(v.index());
            },
            buf -> new DoTradePayload(buf.readUtf(), buf.readVarInt())
        );
        @Override public CustomPacketPayload.Type<DoTradePayload> type() { return DO_TRADE_ID; }
    }

    public record ReqPickerPayload(String shopName) implements CustomPacketPayload {
        public static final StreamCodec<FriendlyByteBuf, ReqPickerPayload> CODEC = StreamCodec.of(
            (buf, v) -> buf.writeUtf(v.shopName()),
            buf -> new ReqPickerPayload(buf.readUtf())
        );
        @Override public CustomPacketPayload.Type<ReqPickerPayload> type() { return REQ_PICKER_ID; }
    }

    public record ReqAmountPayload(String shopName, String itemId) implements CustomPacketPayload {
        public static final StreamCodec<FriendlyByteBuf, ReqAmountPayload> CODEC = StreamCodec.of(
            (buf, v) -> {
                buf.writeUtf(v.shopName());
                buf.writeUtf(v.itemId());
            },
            buf -> new ReqAmountPayload(buf.readUtf(), buf.readUtf())
        );
        @Override public CustomPacketPayload.Type<ReqAmountPayload> type() { return REQ_AMOUNT_ID; }
    }

    public record ReqOwnerScreenPayload(String shopName) implements CustomPacketPayload {
        public static final StreamCodec<FriendlyByteBuf, ReqOwnerScreenPayload> CODEC = StreamCodec.of(
            (buf, v) -> buf.writeUtf(v.shopName()),
            buf -> new ReqOwnerScreenPayload(buf.readUtf())
        );
        @Override public CustomPacketPayload.Type<ReqOwnerScreenPayload> type() { return REQ_OWNER_SCREEN_ID; }
    }

    public record ReqStoragePayload(String shopName) implements CustomPacketPayload {
        public static final StreamCodec<FriendlyByteBuf, ReqStoragePayload> CODEC = StreamCodec.of(
            (buf, v) -> buf.writeUtf(v.shopName()),
            buf -> new ReqStoragePayload(buf.readUtf())
        );
        @Override public CustomPacketPayload.Type<ReqStoragePayload> type() { return REQ_STORAGE_ID; }
    }

    public record TakeStoragePayload(String shopName, int index) implements CustomPacketPayload {
        public static final StreamCodec<FriendlyByteBuf, TakeStoragePayload> CODEC = StreamCodec.of(
            (buf, v) -> {
                buf.writeUtf(v.shopName());
                buf.writeVarInt(v.index());
            },
            buf -> new TakeStoragePayload(buf.readUtf(), buf.readVarInt())
        );
        @Override public CustomPacketPayload.Type<TakeStoragePayload> type() { return TAKE_STORAGE_ID; }
    }

    public record AddEnchantedBookTradePayload(
        String    shopName,
        CompoundTag sellData,
        CompoundTag buyData
    ) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<AddEnchantedBookTradePayload> ID = ADD_BOOK_TRADE_ID;

        public static final StreamCodec<FriendlyByteBuf, AddEnchantedBookTradePayload> CODEC =
            StreamCodec.of(
                (buf, v) -> {
                    buf.writeUtf(v.shopName());
                    writeNbt(buf, v.sellData());
                    writeNbt(buf, v.buyData());
                },
                buf -> new AddEnchantedBookTradePayload(
                    buf.readUtf(),
                    readNbt(buf),
                    readNbt(buf)
                )
            );

        @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return ID; }
    }

    public record ToggleShopMovePayload(String shopName, boolean enabled) implements CustomPacketPayload {
        public static final StreamCodec<FriendlyByteBuf, ToggleShopMovePayload> CODEC = StreamCodec.of(
            (buf, v) -> {
                buf.writeUtf(v.shopName());
                buf.writeBoolean(v.enabled());
            },
            buf -> new ToggleShopMovePayload(buf.readUtf(), buf.readBoolean())
        );
        @Override public CustomPacketPayload.Type<ToggleShopMovePayload> type() { return TOGGLE_SHOP_MOVE_ID; }
    }

    public record ShopNavPayload(List<String> ownerNames) implements CustomPacketPayload {
        public static final StreamCodec<FriendlyByteBuf, ShopNavPayload> CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeVarInt(value.ownerNames().size());
                for (String name : value.ownerNames()) {
                    buf.writeUtf(name);
                }
            },
            buf -> {
                int size = buf.readVarInt();
                List<String> names = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    names.add(buf.readUtf());
                }
                return new ShopNavPayload(names);
            }
        );
        @Override public CustomPacketPayload.Type<ShopNavPayload> type() { return SHOP_NAV_ID; }
    }

    private ModPackets() {}
}