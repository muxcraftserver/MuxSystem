package me.muxteam.events;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@NonJoinableEvent
public class MuxEXP extends Event {
    public byte expboost = 0;
    private boolean byplayer = false;

    public MuxEXP(final MuxEvents e) {
        super(e);
        item = new ItemStack(Material.EXP_BOTTLE);
        name = ms.getMultiplier(expboost) + " EXP";
    }

    public void setEXPBoost(final byte expboost, final boolean byplayer) {
        if (expboost < 0 || expboost > 4) return;
        this.expboost = expboost;
        name = ms.getMultiplier(ms.getEvents().getExpBoost()) + " EXP";
        this.byplayer = byplayer;
    }

    @Override
    public void stop() {
        expboost = 0;
    }

    @Override
    public String[] getDescription() {
        if (byplayer) {
            return new String[]{
                    "§7Bei diesem Event wird auf dem Server",
                    "§7die Anzahl an EXP, die du gewöhnlich",
                    "§7erhältst, temporär vervielfacht.",
                    "",
                    "§dDurch einen Booster derzeit aktiv."
            };
        }
        return new String[]{
                "§7Bei diesem Event wird auf dem Server",
                "§7die Anzahl an EXP, die du gewöhnlich",
                "§7erhältst, temporär vervielfacht.",
        };
    }

    @Override
    public void start() {
        if (expboost == 0) setEXPBoost((byte) 1, false);
        byplayer = false;
        for (final Player pl : ms.getServer().getOnlinePlayers()) {
            pl.sendMessage("§d§lMuxEvent>§7 " + getName() + " hat jetzt begonnen.");
            pl.playSound(pl.getLocation(), Sound.NOTE_PLING, 1F, 0.1F);
        }
        spawnEventNPC("§d§lEXP Event Infos");
    }

    @Override
    public boolean join(final Player p) {
        p.sendMessage("§d§lMuxEvent>§7 Derzeit läuft das §d" + getName() + "§7 Event.");
        p.sendMessage("§d§lMuxEvent>§7 Du bekommst §dmehr EXP §7als sonst.");
        return false;
    }
}