package me.muxteam.events;

import me.muxteam.muxsystem.MuxSystem;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;

@NonJoinableEvent
public class MuxRandom extends Event {
    private short randomcoins;
    public boolean paused = false;

    public MuxRandom(final MuxEvents e) {
        super(e);
        name = "MuxZufall";
        item = new ItemStack(Material.COMPASS);
    }

    private void random() {
        final Collection<? extends Player> collection = ms.getServer().getOnlinePlayers();
        if (collection.isEmpty()) {
            return;
        }
        final Player p = collection.toArray(new Player[0])[r.nextInt(collection.size())];
        if (p.isOnline() == false) return;
        final String name = p.getName();
        if (p.hasPermission("muxsystem.exclude")) {
            ms.broadcastMessage("§d§lMuxZufall>§7 " + name + " hätte gewonnen, ist aber ausgeschlossen.", null, MuxSystem.Priority.NORMAL);
            return;
        } else if (ms.hasVoted(p) == false) {
            ms.broadcastMessage("§d§lMuxZufall>§7 " + name + " hätte gewonnen, hat aber nicht gevotet.", null, MuxSystem.Priority.NORMAL);
            for (final Player pl : ms.getServer().getOnlinePlayers()) {
                if (ms.hasVoted(pl) == false) ms.chatClickHoverRun(pl, "§d§lMuxZufall>§7 Klicke §dhier§7, um zu voten und teilzunehmen.", "§d§oKlicke zum voten", "/vote");
            }
            return;
        } else if (randomcoins > 0) {
            addCoins(p, randomcoins);
            p.sendMessage("§d§lMuxEvent>§7 Dir wurden §e" + ms.getNumberFormat(randomcoins) + " MuxCoins §7gutgeschrieben.");
        }
        ms.broadcastMessage("§d§lMuxZufall>§7 " + name + " hat die Verlosung §agewonnen§7!", null, MuxSystem.Priority.NORMAL);
    }

    public void setPaused(final boolean paused) {
        this.paused = paused;
    }

    public void setCoins(final Player p, final String c) {
        short coins;
        try {
            coins = Short.parseShort(ms.removeNonDigits(c));
            if (coins < 0) coins = 0;
            else if (coins > 30000) throw new NumberFormatException();
        } catch (final NumberFormatException e) {
            ms.showItemBar(p, "§cBitte gebe eine Zahl von 0 bis 30.000 ein.");
            return;
        }
        randomcoins = coins;
        if (randomcoins == 0) {
            ms.showItemBar(p, "§cDer Gewinnpreis wurde entfernt.");
        } else {
            ms.broadcastMessage("§d§lMuxZufall>§7 Der Gewinn wurde auf §e" + randomcoins + " MuxCoins §7gesetzt.", null, MuxSystem.Priority.HIGH);
        }
    }

    public short getCoins() {
        return randomcoins;
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "§7Der Gewinner wird hier absolut zufällig",
                "§7ausgewählt. Vote, um bei der Auslosung",
                "§7sofort teilnehmen zu können.",
        };
    }

    @Override
    public String[] getAdminInformation() {
        short notvoted = 0;
        for (final Player pl : ms.getServer().getOnlinePlayers()) {
            if (ms.hasVoted(pl) == false) {
                notvoted++;
            }
        }
        return new String[]{
                "§7MuxCoins gesetzt: §e" + randomcoins,
                "§7Nicht gevoted: §d" + notvoted
        };
    }

    @Override
    public long getUpdateTime() {
        return 140L;
    }

    @Override
    public void update() {
        if (paused == false) {
            random();
        }
    }

    @Override
    public void start() {
        for (final Player pl : ms.getServer().getOnlinePlayers()) {
            ms.chatClickHoverRun(pl, "§d§lMuxEvent>§7 Das Zufall Event hat begonnen.", "§d§oKlicke um teilzunehmen", "/event join");
            ms.chatClickHoverRun(pl, "§d§lMuxEvent>§7 Es werden §dzufällige Spieler §7ausgewählt.", "§d§oKlicke um teilzunehmen", "/event join");
            pl.playSound(pl.getLocation(), Sound.NOTE_PLING, 1F, 0.1F);
        }
        spawnEventNPC("§d§lEvent Infos");
    }

    @Override
    public boolean join(final Player p) {
        if (ms.hasVoted(p)) {
            ms.showItemBar(p, "§aDu hast bereits gevotet und nimmst somit am Event teil.");
        } else {
            ms.chatClickHoverLink(p, "§d§lMuxEvent>§7 Vote, um am Event teilzunehmen.", "§d§oKlicke um zu voten", "https://muxcraft.eu/vote/?ign=" + p.getName());
            ms.chatClickHoverLink(p, "§d§lMuxEvent>§7 Klicke §dhier§7, um zu voten.", "§d§oKlicke um zu voten", "https://muxcraft.eu/vote/?ign=" + p.getName());
        }
        return false;
    }
}