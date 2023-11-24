package me.muxteam.marketing;

import me.muxteam.muxsystem.MuxSystem;
import me.muxteam.muxsystem.MuxUser;
import org.apache.commons.lang.mutable.MutableInt;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class MuxTips implements Listener {
    private final MuxSystem ms;
    private final Map<UUID, TipUser> tipUsers = new HashMap<>();

    public MuxTips(final MuxSystem ms) {
        this.ms = ms;
    }

    public void handleJoin(final Player p) {
        final UUID uuid = p.getUniqueId();
        final TipUser user = new TipUser();
        tipUsers.put(uuid, user);
        user.load(uuid);
    }

    public void handleQuit(final UUID uuid) {
        final TipUser user = tipUsers.remove(uuid);
        if (user != null) user.save(uuid);
    }

    public void showTips() {
        for (final Player p : ms.getServer().getOnlinePlayers()) {
            final MuxTips.TipUser user = tipUsers.get(p.getUniqueId());
            final MuxUser u = ms.getMuxUser(p.getName());
            if (u != null && user != null && ms.inEvent(p) == false && user.getLastTipDisplay() <= System.currentTimeMillis() - 1800000L && (System.currentTimeMillis() > u.getLoginTimestamp() + 40000L)) {
                final MuxTips.Tip randomTip = user.getRandomTip(p);
                if (randomTip == null) continue;
                user.setCurrentTip(randomTip);
                user.setLastTipDisplay(System.currentTimeMillis());
                ms.chatClickHoverRun(p, randomTip.getTipSpoiler(), "§5§oTipp anzeigen", "/tips " + user.getRandomID());
                ms.chatClickHoverRun(p, "§f§lKlicke §5§lhier§f§l, um mehr zu erfahren.", "§5§oTipp anzeigen", "/tips " + user.getRandomID());
            }
        }
    }

    public boolean handleCommand(final Player p, final String[] args) {
        if (args.length == 0) {
            final MutableInt count = new MutableInt(1);
            final ItemStack book = ms.book("§aMuxTipps", Arrays.stream(Tip.values()).map(tip -> {
                final String header = count.intValue() == 1 ? "        §7§lMux§2§lCraft\n\n§0  Die Liste der §5Tipps§0.\n            §8-*-\n\n§0#§l" + count.intValue() + " " : "§0#§l" + count.intValue() + " ";
                count.add(1);
                return header + tip.getTipTitle() + "\n\n" + tip.getTipText();
            }).collect(Collectors.toList()));
            ms.openBook(book, p);
            return true;
        }
        final TipUser tipUser = tipUsers.get(p.getUniqueId());
        if (args[0].contains(tipUser.randomID) == false || tipUser.getCurrentTip() == null) {
            p.performCommand("tips");
            return true;
        }
        final Tip tip = tipUser.getCurrentTip();
        tipUser.seentips.put(tip,  -1);
        tipUser.setCurrentTip(null);
        final ItemStack book = ms.cmdBook(tip.getTipTitle(), tip.getTipText(), new Object[] { "/tipps", "\n§0§l§nAlle Tipps lesen", "Klicke hier" });
        ms.openBook(book, p);
       return true;
    }

    public enum Tip {
        HELP_TIP("§f§lMöchtest du alle Befehle sehen?", "§0Mit §5/hilfe §0kannst du alle Befehle auf MuxCraft sehen.", "§0§lBefehle"),
        MINING_TIP("§f§lWusstest du, dass Erze abbauen MuxCoins bringt?", "§0Indem du Erze abbaust in der Wildnis, erhältst du MuxCoins. Gebe §5/coins §0ein für mehr Infos.", "§0§lErze minen"),
        REPAIR_TIP("§f§lMöchtest du deine Items reparieren?", "§0Wenn du mit einem §5Schwert rechtsklickst§0, kannst du alle deine Kampfitems §5reparieren§0.", "§0§lItems reparieren"),
        PLAYER_TIP("§f§lWusstest du, dass du Spieler rechtsklicken kannst?", "§0Du kannst in geschützten Gebieten §5Spieler rechtsklicken§0, um bspw. mit diesen zu handeln.", "§0§lSpieler §0§lrechtsklicken"),
        TRAINING_TIP("§f§lMöchtest du Potion PvP trainieren?", "§0Indem du am Spawn vor dem 1vs1 Pigman §5sneakst §0und ihn §5anklickst§0, betrittst du das Training.", "§0§lTraining"),
        SELL_TIP("§f§lMöchtest du ganz schnell im Shop verkaufen?", "§0Mit dem Befehl §5/sell §0kannst du ganz schnell und bequem von überall Items im Shop zum Verkauf stellen.", "§0§lVerkaufen"),
        SNOWBALL_TIP("§f§lWusstest du schon, dass Schneebälle blind machen?", "§0Wenn du im PvP §5Schneebälle §0auf andere Spielern wirfst, werden diese für §515 Sekunden blind §0und können nichts sehen.","§0§lSchneebälle"),
        TRASH_TIP("§f§lMöchtest du gewisse Items loswerden?", "§0Mit dem Befehl §5/abfall §0kannst du schnell Items loswerden.", "§0§lAbfall"),
        AFFILIATE_TIP("§f§lMöchtest du leicht Echtgeld verdienen?", "§0Indem du neue Spieler einlädst, verdienst du Echtgeld. Bei §5/werben§0 findest du mehr Infos.", "§0§lWerben"),
        REPORT_TIP("§f§lMöchtest du einen Spieler melden?", "§0Mit dem Befehl §5/report §0kannst du Spieler sofort an das Team melden.", "§0§lReport"),
        ULTRABIKE_TIP("§f§lFreunde können miteinsteigen auf dein Bike?", "§0Deine Freunde können mit einem Rechtsklick mit dir §5zusammen auf dein Bike §0einsteigen.", "§0§lULTRA Bike", "muxsystem.ultra");

        private final String tipSpoiler, tipText, tipTitle, tipPerms;
        Tip(final String tipSpoiler, final String tipText, final String tipTitle) {
            this.tipSpoiler = tipSpoiler;
            this.tipText = tipText;
            this.tipTitle = tipTitle;
            this.tipPerms = null;
        }
        Tip(final String tipSpoiler, final String tipText, final String tipTitle, final String tipPerms) {
            this.tipSpoiler = tipSpoiler;
            this.tipText = tipText;
            this.tipTitle = tipTitle;
            this.tipPerms = tipPerms;
        }

        public String getTipTitle() {
            return tipTitle;
        }
        public String getTipText() {
            return tipText;
        }
        public String getTipSpoiler() {
            return tipSpoiler;
        }
        public String getTipPerms() {
            return tipPerms;
        }
    }

    public class TipUser {
        private Map<Tip, Integer> seentips = new EnumMap<>(Tip.class);
        private Tip currentTip = null;
        private final String randomID = ms.getRandomWord();
        private long lastTipDisplay = System.currentTimeMillis() - 1680000L;

        public Tip getRandomTip(final Player p) {
            for (final Tip tip : Tip.values()) {
                final Integer used = seentips.getOrDefault(tip, 0);
                if (used == -1 || used >= 3 || (tip.getTipPerms() != null && p.hasPermission(tip.getTipPerms()) == false)) {
                    continue;
                }
                seentips.put(tip, used + 1);
                return tip;
            }
            return null;
        }
        public String getRandomID() {
            return randomID;
        }
        public long getLastTipDisplay() {
            return lastTipDisplay;
        }
        public void setLastTipDisplay(final long lastTipDisplay) {
            this.lastTipDisplay = lastTipDisplay;
        }
        public void setCurrentTip(final Tip currentTip) {
            this.currentTip = currentTip;
        }
        public Tip getCurrentTip() {
            return currentTip;
        }
        public void load(final UUID uuid) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    seentips  = ms.getDB().loadUserTips(uuid, TipUser.this);
                }
            }.runTaskAsynchronously(ms);
        }

        public void save(final UUID uuid) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    ms.getDB().saveUserTips(uuid, seentips, lastTipDisplay);
                }
            }.runTaskAsynchronously(ms);
        }
    }
}