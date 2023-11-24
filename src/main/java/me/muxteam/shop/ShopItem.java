package me.muxteam.shop;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

public class ShopItem {
    private final String fullid;
    private final UUID uuid;
    private final short durability;
    private final int itemid;
    private int amount;
    private double price;
    private long id;

    public ShopItem(final long id, final UUID uuid, final String fullid, double price, int amount) {
        this.id = id;
        this.uuid = uuid;
        this.fullid = fullid;
        final String[] split = fullid.split(":");
        this.itemid = Integer.parseInt(split[0]);
        this.durability = Short.parseShort(split[1]);
        this.price = price;
        this.amount = amount;
    }

    public long getKeyId() {
        return id;
    }

    public void setKeyId(final long id) {
        this.id = id;
    }

    public UUID getUUID() {
        return uuid;
    }

    public String getFullId() {
        return fullid;
    }

    public int getId() {
        return itemid;
    }

    public short getDurability() {
        return durability;
    }

    public ItemStack getItemStack() {
        if (itemid == 52) {
            final ItemStack is = new ItemStack(itemid, 1, durability);
            final ItemMeta im = is.getItemMeta();
            im.setDisplayName("§a§lMuxSpawner");
            is.setItemMeta(im);
            return is;
        }
        return new ItemStack(itemid, 1, durability);
    }

    public double getPrice() {
        return price;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(final int amount) {
        this.amount = amount;
    }

    public void setPrice(final double price) {
        this.price = price;
    }

    public void removeOne() {
        this.amount--;
    }
}