package me.muxteam.shop;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class PremiumItem {
    private final UUID owner;
    private final ItemStack item;
    private final int price;
    private long id, updated;

    public PremiumItem(final long id, final UUID owner, final ItemStack is, final int price, final long updated) {
        this.id = id;
        this.owner = owner;
        this.item = is;
        this.price = price;
        this.updated = updated;
    }

    public void setKeyId(final long id) {
        this.id = id;
    }

    public long getKeyId() {
        return id;
    }

    public UUID getOwner() {
        return owner;
    }

    public ItemStack getItem() {
        return item;
    }

    public int getPrice() {
        return price;
    }

    public long getUpdated() {
        return updated;
    }

    public boolean isValid() {
        return updated + 86500000 * 2 > System.currentTimeMillis();
    }

    public void update() {
        this.updated = System.currentTimeMillis();
    }
}