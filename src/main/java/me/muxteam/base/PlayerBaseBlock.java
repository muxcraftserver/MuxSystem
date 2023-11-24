package me.muxteam.base;

import java.util.List;

public class PlayerBaseBlock {
    private final String id;
    private long amount;
    private final double pricePerBlock, priceForAmount;

    public PlayerBaseBlock(final String id, final long amount, final double pricePerBlock, final List<String> halfBlocks) {
        this.id = id;
        boolean b = false;
        if (amount >= 2) {
            for (final String hb : halfBlocks) {
                if (id.startsWith(hb)) {
                    b = true;
                    this.amount = amount / 2;
                    break;
                }
            }
        }
        if (b == false) this.amount = amount;
        this.pricePerBlock = pricePerBlock;
        this.priceForAmount = pricePerBlock * this.amount;
    }

    public double getPricePerBlock() {
        return pricePerBlock;
    }

    public double getPriceForAmount() {
        return priceForAmount;
    }

    public long getAmount() {
        return amount;
    }

    public String getId() {
        return id;
    }
}