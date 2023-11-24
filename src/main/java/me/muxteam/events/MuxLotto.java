package me.muxteam.events;

import me.muxteam.basic.ConfirmInventory;
import me.muxteam.basic.MuxActions;
import me.muxteam.muxsystem.MuxSystem;
import me.muxteam.muxsystem.MuxUser;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

@NonJoinableEvent
public class MuxLotto extends Event {
    private int lottocoins = 200;
    private long currentEndTime;

    public MuxLotto(final MuxEvents e) {
        super(e);
        name = "MuxLotto";
        item = new ItemStack(Material.GOLD_INGOT);
        chatOnly = true;
    }

    @Override
    public void stop() {
        final List<Player> lottos = new ArrayList<>(players);
        if (lottos.isEmpty()) return;
        ms.broadcastMessage("§d§lMuxEvent>§7 Lotto wird aufgelöst...", null, MuxSystem.Priority.HIGH);
        new BukkitRunnable() {
            @Override
            public void run() {
                final int gewinn = lottos.size() * lottocoins;
                final Player winner = lottos.get(r.nextInt(lottos.size()));
                ms.broadcastMessage("§d§lMuxEvent>§7 Der Spieler §d" + winner.getName() + " §7hat beim Lotto §agewonnen§7!", null, MuxSystem.Priority.HIGH);
                final MuxUser u = ms.getMuxUser(winner.getName());
                u.addCoins(gewinn);
                ms.getHistory().addHistory(winner.getUniqueId(), null, "COINS", String.valueOf(gewinn), "Event");
                ms.saveMuxUser(u);
                ms.sendScoreboard(winner);
                winner.sendMessage("§d§lMuxEvent>§7 Dir wurden §e" + ms.getNumberFormat(gewinn) + " MuxCoins §7gutgeschrieben.");
            }
        }.runTaskLater(ms, 60L);
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "§7Sobald du ein Lotto-Ticket kaufst, hast",
                "§7du die Möglichkeit, den großen Jackpot",
                "§7nach der Auflösung zu gewinnen.",
                "",
                "§7Ticketpreis: §e" + ms.getNumberFormat(lottocoins) + " MuxCoins",
                "§7Jackpot: §e" + ms.getNumberFormat(players.size() * (long) lottocoins) + " MuxCoins"
        };
    }

    @Override
    public String[] getAdminInformation() {
        return new String[]{
                "§7Teilnehmer: §d" + players.size()
        };
    }

    @Override
    public void start() {
        for (final Player pl : ms.getServer().getOnlinePlayers()) {
            ms.chatClickHoverRun(pl, "§d§lMuxEvent>§7 Lotto hat jetzt begonnen.", "§d§oKlicke um teilzunehmen", "/event join");
            ms.chatClickHoverRun(pl, "§d§lMuxEvent>§7 Klicke §dhier §7um für §e" + ms.getNumberFormat(lottocoins) + " MuxCoins §7teilzunehmen.", "§d§oKlicke um teilzunehmen", "/event join");
            pl.playSound(pl.getLocation(), Sound.NOTE_PLING, 1F, 0.1F);
        }
        currentEndTime = System.currentTimeMillis() + 10000;
    }

    @Override
    public void update() {
        if (currentEndTime <= System.currentTimeMillis()) {
            stopEvent(true);
        }
    }

    @Override
    public long getUpdateTime() {
        return 1L;
    }

    @Override
    public boolean join(final Player p) {
        final MuxUser u = ms.getMuxUser(p.getName());
        if (u.getCoins() < lottocoins) {
            ms.showItemBar(p, "§cDu hast nicht genügend MuxCoins, um ein Lotto-Ticket zu kaufen.");
            return false;
        }
        final boolean expensive = ((double) lottocoins / (double) u.getCoins()) > 0.10D;
        final MuxActions.PlayerAction confirm = player -> {
            currentEndTime = System.currentTimeMillis() + 10_000L;
            p.playSound(p.getLocation(), Sound.ORB_PICKUP, 1F, 1F);
            u.addCoins(-lottocoins);
            ms.getHistory().addHistory(p.getUniqueId(), null, "COINS", String.valueOf(-lottocoins), "Event");
            ms.sendScoreboard(p);
            if (expensive) {
                ms.showItemBar(p, "§fDu nimmst nun am §dEvent §fteil.");
                players.add(p);
            }
            p.closeInventory();
        }, cancel = HumanEntity::closeInventory;
        if (expensive) {
            new ConfirmInventory(ms, confirm, cancel).show(p, "§0§lLotto Ticket kaufen", "§aBestätigen (" + ms.getNumberFormat(lottocoins) + " MuxCoins)", "§cAbbrechen");
            return false;
        }
        confirm.call(p);
        return true;
    }

    public int getCoins() {
        return lottocoins;
    }

    public void setCoins(final Player p, final String c) {
        int coins;
        try {
            coins = Integer.parseInt(ms.removeNonDigits(c));
            if (coins < 5) coins = 5;
            else if (coins > getMaxTicketPrice()) throw new NumberFormatException();
        } catch (final NumberFormatException e) {
            ms.showItemBar(p, "§cBitte gebe eine Zahl von 5 bis " + ms.getNumberFormat(getMaxTicketPrice()) + " ein.");
            return;
        }
        lottocoins = coins;
    }
    private long getMaxTicketPrice() {
        final long supply = ms.getActiveMuxCoinsSupply();
        final double percentage = 0.0001D;
        long result = (long) Math.ceil((supply * percentage));
        int n = (int) Math.ceil(Math.log10(result + 1D));
        n = Math.max(0, n - 1);
        result = (long) Math.ceil(result / Math.pow(10, n)) * (long) Math.pow(10, n);
        return Math.max(result, 1000L);
    }
}