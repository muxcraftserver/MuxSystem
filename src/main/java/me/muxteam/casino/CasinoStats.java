package me.muxteam.casino;

import jdk.nashorn.internal.objects.NativeMath;
import me.muxteam.muxsystem.MuxInventory;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class CasinoStats {
    public int CRASH_RUNDEN_VERLUST = 0, CRASH_RUNDEN_GEWONNEN = 0;
    public long CRASH_VERLUST = 0, CRASH_GEWINN = 0;
    public double CRASH_HIGHEST = 0D, CRASH_DURCHSCHNITT = 0D;

    public long ROULETTE_VERLUST = 0, ROULETTE_GEWINN = 0;
    public int ROULETTE_ROT = 0, ROULETTE_GREEN = 0, ROULETTE_BLACK = 0;

    public long GRAD_VERLUST = 0, GRAD_GEWINN = 0;
    public int GRAD_RUNDEN_GEWONNEN = 0;

    public int COINFLIP_GEWINNER_P1 = 0, COINFLIP_GEWINNER_P2 = 0;
    public long COINFLIP_GESETZT = 0;

    public int ITEMFLIP_GEWINNER_P1 = 0, ITEMFLIP_GEWINNER_P2 = 0;

    public int SLOT_RUNDEN_GEWONNEN = 0, SLOT_RUNDEN_GESPIELT = 0;
    public long SLOT_VERLUST = 0, SLOT_GEWINN = 0;

    public long RUBBEL_VERLUST1 = 0, RUBBEL_GEWINN1 = 0, RUBBEL_VERLUST2 = 0, RUBBEL_GEWINN2 = 0, RUBBEL_VERLUST3 = 0, RUBBEL_GEWINN3 = 0;

    public int SIXFIELDS_WON_FELD1 = 0, SIXFIELDS_WON_FELD2 = 0, SIXFIELDS_WON_FELD3 = 0,
            SIXFIELDS_WON_FELD4 = 0, SIXFIELDS_WON_FELD5 = 0, SIXFIELDS_WON__FELD6 = 0;
    public long SIXFIELDS_VERLUST = 0, SIXFIELDS_GEWINN = 0;

    public int RPS_GAMES = 0;
    public long RPS_GESETZT = 0;

    public int BLACKJACK_GAMES = 0, BLACKJACK_GAMES_WON = 0;
    public long BLACKJACK_GEWINN = 0, BLACKJACK_VERLUST = 0;

    public long COIN_PUSHER_GEWINN = 0, COIN_PUSHER_VERLUST = 0;

    public int GUESS_THE_NUMBER_GAMES = 0;
    public long GUESS_THE_NUMBER_EINSATZ = 0;

    public int POKER_GAMES = 0;
    public long POKER_EINSATZ = 0;

    public long BARKEEPER = 0;

    public long BANKFEES = 0, CHIPS_BOUGHT = 0, CHIPS_WITHDRAWN = 0;

    public long BANKACCOUNT = 0;

    private final MuxCasino c;

    public CasinoStats(final MuxCasino c) {
        this.c = c;
        loadStats(c.ms.getDB().getCasinoStats());
        new BukkitRunnable() {
            @Override public void run() {
                save();
            }
        }.runTaskTimerAsynchronously(c.ms, 20L * 60L, 20L * 60L);
    }
    public void close() {
        save();
    }
    public void save() {
        c.ms.getDB().saveCasinoStats(getJSONStats());
    }
    public void openCasinoStats(final Player p) {
        if (c.inCasino(p) == false) return;
        final Inventory inv = c.ms.getServer().createInventory(null, 18, "§0§lMuxCasino§0 | Stats");
        inv.addItem(c.ms.renameItemStack(new ItemStack(Material.EMPTY_MAP, 1), "§d§lCrash",
                    "§7An Spieler ausgezahlt:", "§f" + c.ms.getNumberFormat(CRASH_VERLUST) + " Chips", "", "§7Von Spielern investiert:", "§f" + c.ms.getNumberFormat(CRASH_GEWINN) + " Chips", "",
                    "§7Runden über §a1.00x", "§f" + CRASH_RUNDEN_GEWONNEN, "", "§7Runden unter §c1.00x", "§f" + CRASH_RUNDEN_VERLUST, "", "§7Höchster Crash:", "§a" + CRASH_HIGHEST + "x", "",
                    "§7Durch. Crash:", "§f" + NativeMath.round(CRASH_DURCHSCHNITT, 2) + "x"));
        inv.addItem(c.ms.renameItemStack(new ItemStack(Material.EMPTY_MAP, 1), "§d§lRoulette",
                "§7An Spieler ausgezahlt:", "§f" + c.ms.getNumberFormat(ROULETTE_VERLUST) + " Chips", "", "§7Von Spielern investiert:", "§f" + c.ms.getNumberFormat(ROULETTE_GEWINN) + " Chips", "",
                "§7Runden auf §aGRÜN", "§f" + ROULETTE_GREEN, "", "§7Runden auf §cROT", "§f" + ROULETTE_ROT, "", "§7Runden auf §8Schwarz:", "§f" + ROULETTE_BLACK, "",
                "§7Runden insgesamt:", "§f" + (ROULETTE_GREEN + ROULETTE_BLACK + ROULETTE_ROT)));
        inv.addItem(c.ms.renameItemStack(new ItemStack(Material.EMPTY_MAP, 1), "§d§lGlücksrad",
                "§7An Spieler ausgezahlt:", "§f" + c.ms.getNumberFormat(GRAD_VERLUST) + " Chips", "", "§7Von Spielern investiert:", "§f" + c.ms.getNumberFormat(GRAD_GEWINN) + " Chips",
                "", "§7Runden gewonnen:", "§f" + GRAD_RUNDEN_GEWONNEN));
        inv.addItem(c.ms.renameItemStack(new ItemStack(Material.EMPTY_MAP, 1), "§d§lCoinFlip",
                "§7Gewonnene Runden vom Host:", "§f" + COINFLIP_GEWINNER_P1, "", "§7Gewonnene Runden vom Mitspieler:", "§f" + COINFLIP_GEWINNER_P2, "",
                "§7Runden insgesamt:", "§f" + (COINFLIP_GEWINNER_P1 + COINFLIP_GEWINNER_P2), "", "§7Gesetzte Beträge:", "§f" + c.ms.getNumberFormat(COINFLIP_GESETZT) + " Chips"));
        inv.addItem(c.ms.renameItemStack(new ItemStack(Material.EMPTY_MAP, 1), "§d§lItemFlip",
                "§7Gewonnene Runden vom Host:", "§f" + ITEMFLIP_GEWINNER_P1, "", "§7Gewonnene Runden vom Mitspieler:", "§f" + ITEMFLIP_GEWINNER_P2, "",
                "§7Runden insgesamt:", "§f" + (ITEMFLIP_GEWINNER_P1 + ITEMFLIP_GEWINNER_P2)));
        inv.addItem(c.ms.renameItemStack(new ItemStack(Material.EMPTY_MAP, 1), "§d§lSlot Machine",
                "§7An Spieler ausgezahlt:", "§f" + c.ms.getNumberFormat(SLOT_VERLUST) + " Chips", "", "§7Von Spielern investiert:", "§f" + c.ms.getNumberFormat(SLOT_GEWINN) + " Chips", "",
                "§7Runden gewonnen:", "§f" + SLOT_RUNDEN_GEWONNEN, "", "§7Gespielte Runden:", "§f" + SLOT_RUNDEN_GESPIELT));
        inv.addItem(c.ms.renameItemStack(new ItemStack(Material.EMPTY_MAP, 1), "§d§lRubellos",
                "§7An Spieler ausgezahlt:", "§f" + c.ms.getNumberFormat((RUBBEL_VERLUST2 + RUBBEL_VERLUST3 + RUBBEL_VERLUST1)) + " Chips", "",
                "§7Von Spielern investiert:", "§f" + c.ms.getNumberFormat((RUBBEL_GEWINN1 + RUBBEL_GEWINN2 + RUBBEL_GEWINN3)) + " Chips", "",
                "§7Rubbellos 1:", "§7An Spieler:§f " + c.ms.getNumberFormat(RUBBEL_VERLUST1) + " Chips", "§7Von Spielern:§f " + c.ms.getNumberFormat(RUBBEL_GEWINN1) + " Chips", "",
                "§7Rubbellos 2:", "§7An Spieler:§f " + c.ms.getNumberFormat(RUBBEL_VERLUST2) + " Chips", "§7Von Spielern:§f " + c.ms.getNumberFormat(RUBBEL_GEWINN2) + " Chips", "",
                "§7Rubbellos 3:", "§7An Spieler:§f " + c.ms.getNumberFormat(RUBBEL_VERLUST3) + " Chips", "§7Von Spielern:§f " + c.ms.getNumberFormat(RUBBEL_GEWINN3) + " Chips"));
        inv.addItem(c.ms.renameItemStack(new ItemStack(Material.EMPTY_MAP, 1), "§d§l6 Felder",
                "§7An Spieler ausgezahlt:", "§f" + c.ms.getNumberFormat(SIXFIELDS_VERLUST) + " Chips", "", "§7Von Spielern investiert:", "§f" + c.ms.getNumberFormat(SIXFIELDS_GEWINN) + " Chips", "",
                "§7Runden auf Feld 1: §f" + SIXFIELDS_WON_FELD1, "§7Runden auf Feld 2: §f" + SIXFIELDS_WON_FELD2, "§7Runden auf Feld 3: §f" + SIXFIELDS_WON_FELD3,
                "§7Runden auf Feld 4: §f" + SIXFIELDS_WON_FELD4, "§7Runden auf Feld 5: §f" + SIXFIELDS_WON_FELD5, "§7Runden auf Feld 6: §f" + SIXFIELDS_WON__FELD6));
        inv.addItem(c.ms.renameItemStack(new ItemStack(Material.EMPTY_MAP, 1), "§d§lSchere, Stein, Papier",
                "§7Runden insgesamt:", "§f" + RPS_GAMES, "", "§7Gesetzte Beträge:", "§f" + c.ms.getNumberFormat(RPS_GESETZT) + " Chips"));
        inv.addItem(c.ms.renameItemStack(new ItemStack(Material.EMPTY_MAP, 1), "§d§lBlackJack",
                "§7An Spieler ausgezahlt:", "§f" + c.ms.getNumberFormat(BLACKJACK_VERLUST) + " Chips", "", "§7Von Spielern investiert:", "§f" + c.ms.getNumberFormat(BLACKJACK_GEWINN) + " Chips", "",
                "§7Gespielte Runden:", "§f" + BLACKJACK_GAMES, "", "§7Runden gewonnen:", "§f" + BLACKJACK_GAMES_WON));
        inv.addItem(c.ms.renameItemStack(new ItemStack(Material.EMPTY_MAP, 1), "§d§lZahlen Raten",
                "§7Runden insgesamt:", "§f" + GUESS_THE_NUMBER_GAMES, "", "§7Gesetzte Beträge:", "§f" + c.ms.getNumberFormat(GUESS_THE_NUMBER_EINSATZ) + " Chips"));
        inv.addItem(c.ms.renameItemStack(new ItemStack(Material.EMPTY_MAP, 1), "§d§lCoinPusher",
                "§7An Spieler ausgezahlt:", "§f" + c.ms.getNumberFormat(COIN_PUSHER_VERLUST) + " Chips", "", "§7Von Spielern investiert:", "§f"+ c.ms.getNumberFormat(COIN_PUSHER_GEWINN) + " Chips"));
        inv.addItem(c.ms.renameItemStack(new ItemStack(Material.EMPTY_MAP, 1), "§d§lTexas Holdem",
                "§7Runden insgesamt:", "§f" + POKER_GAMES, "", "§7Gesetzte Beträge:", "§f" + c.ms.getNumberFormat(POKER_EINSATZ) + " Chips"));
        inv.addItem(c.ms.renameItemStack(new ItemStack(Material.GLASS_BOTTLE), "§d§lBarkeeper",
                "§7Von Spielern ausgegeben:", "§f" + c.ms.getNumberFormat(BARKEEPER) + " Chips"));
        inv.addItem(c.ms.renameItemStack(new ItemStack(Material.DOUBLE_PLANT), "§d§lBank",
                "§7Bank Kontostand:", (BANKACCOUNT > 0 ? "§a" : "§c") + c.ms.getNumberFormat(BANKACCOUNT) + " Chips", "",
                "§7Chips gekauft:", "§f" + c.ms.getNumberFormat(CHIPS_BOUGHT) + " Chips", "",
                "§7Chips abgehoben:", "§f" + c.ms.getNumberFormat(CHIPS_WITHDRAWN) + " Chips", "",
                "§7Abhebungsgebühren:", "§f" + c.ms.getNumberFormat(BANKFEES) + " Chips"));
        inv.addItem(c.ms.renameItemStack(new ItemStack(Material.INK_SACK, 1, (byte) 5), "§d§lChips",
                "§7Chipsmenge:", "§f" + c.ms.getNumberFormat((long) c.ms.getDB().getServerStats()[2]) + " Chips"));
        if (c.ms.getActiveInv(p.getName()) != MuxInventory.InvType.CASINO) p.closeInventory();
        p.openInventory(inv);
        c.ms.setActiveInv(p.getName(), MuxInventory.InvType.NOINTERACT);
    }
    private void loadStats(final String string) {
        if (string.isEmpty()) return;
        final JSONArray array = new JSONArray(string);
        for (final Object o : array) {
            final JSONObject object = (JSONObject) o;
            if (object.has("value") == false) continue;
            try {
                final Field field = getClass().getDeclaredField(object.optString("name", "NULL"));
                field.setAccessible(true);
                field.set(this, getPrimitiveByType(object.optString("type"), String.valueOf(object.get("value"))));
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }
    private String getJSONStats() {
        final JSONArray array = new JSONArray();
        for (final Field field : getClass().getDeclaredFields()) {
            if ((field.getModifiers() & Modifier.FINAL) == Modifier.FINAL) continue;
            JSONObject object = new JSONObject();
            field.setAccessible(true);
            object.put("name", field.getName());
            object.put("type", field.getType().getName());
            try {
                object.put("value", field.get(CasinoStats.this));
            } catch (final IllegalAccessException e) {
                e.printStackTrace();
            }
            array.put(object);
        }
        return array.toString();
    }
    private Object getPrimitiveByType(final String type, final String object) {
        if (type.equals("double")) {
            return Double.parseDouble(object);
        } else if (type.equalsIgnoreCase("int")) {
            return Integer.parseInt(object);
        } else if (type.equalsIgnoreCase("string")) {
            return object;
        } else if (type.equalsIgnoreCase("long")) {
            return Long.parseLong(object);
        } else if (type.equalsIgnoreCase("byte")) {
            return Byte.parseByte(object);
        } else if (type.equalsIgnoreCase("short")) {
            return Short.parseShort(object);
        }
        return object;
    }
}