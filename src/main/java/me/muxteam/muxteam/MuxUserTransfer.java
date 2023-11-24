package me.muxteam.muxteam;

import me.muxteam.basic.ConfirmInventory;
import me.muxteam.casino.MuxCasinoUser;
import me.muxteam.extras.MuxExtraUser;
import me.muxteam.muxsystem.MuxSystem;
import me.muxteam.muxsystem.MuxUser;
import me.muxteam.muxsystem.MySQL;
import me.muxteam.ranks.MuxRanks;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MuxUserTransfer {
    private MuxSystem ms;

    public MuxUserTransfer(final MuxSystem ms) {
        this.ms = ms;
    }

    public void close() {
        this.ms = null;
    }

    public boolean handleCommand(final CommandSender sender, final String[] args) {
        if (ms.isFullTrusted(sender.getName()) == false) {
            if (sender instanceof Player) ms.sendNoCMDMessage((Player) sender);
            return true;
        } else if (args.length != 2) {
            if (sender instanceof Player)
                ms.showItemBar((Player) sender, ms.usage("/usertransfer [spieler] [zu spieler]"));
            else sender.sendMessage(ms.usage("/usertransfer [spieler] [zu spieler]"));
            return true;
        }
        final OfflinePlayer from = ms.getPlayer(args[0], true), to = ms.getPlayer(args[1], true);
        if (from == null) {
            if (sender instanceof Player)
                ms.showItemBar((Player) sender, "§cDer erste Spieler war noch nie auf dem Server.");
            else sender.sendMessage("§cDer erste Spieler war noch nie auf dem Server.");
            return true;
        } else if (to == null) {
            if (sender instanceof Player)
                ms.showItemBar((Player) sender, "§cDer zweite Spieler war noch nie auf dem Server.");
            else sender.sendMessage("§cDer zweite Spieler war noch nie auf dem Server.");
            return true;
        } else if (from == to) {
            if (sender instanceof Player) ms.showItemBar((Player) sender, "§cEs ist der selbe Spieler.");
            else sender.sendMessage("§cEs ist der selbe Spieler.");
            return true;
        }
        handleUserTransfer(sender, from, to, false);
        return true;
    }

    private void handleUserTransfer(final CommandSender executor, final OfflinePlayer from, final OfflinePlayer to, final boolean confirmed) {
        if (confirmed == false && executor instanceof Player) {
            new ConfirmInventory(ms, p -> {
                p.closeInventory();
                p.playSound(p.getLocation(), Sound.VILLAGER_YES, 1F, 1F);
                handleUserTransfer(p, from, to, true);
            }, p -> {
                p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                ms.showItemBar(p, "§cÜbertragung abgebrochen.");
                p.closeInventory();
            }).show((Player) executor, "§0§lÜbertragung bestätigen", "§aBestätigen", "§cAbbrechen", ms.getHead(from.getName()), "§f§lÜbertragung", "§7Von: §c" + from.getName(), "§7Auf: §a" + to.getName());
            return;
        }
        final MySQL db = ms.getDB();
        final MuxUser userFrom = from.isOnline() ? ms.getMuxUser(from.getName()) : db.loadPlayer(from.getUniqueId()),
                userTo = to.isOnline() ? ms.getMuxUser(to.getName()) : db.loadPlayer(to.getUniqueId());
        if (userFrom == null) {
            executor.sendMessage("§cUserdaten für den ersten Spieler sind nicht vorhanden.");
            return;
        } else if (userTo == null) {
            executor.sendMessage("§cUserdaten für den zweiten Spieler sind nicht vorhanden.");
            return;
        } else if (to.isOnline() == false) {
            executor.sendMessage("§eDer zweite Spieler ist nicht online.");
            return;
        }
        final MuxCasinoUser casinoUserFrom = ms.getCasino().getCasinoUser(from.getUniqueId()),
                casinoUserTo = ms.getCasino().getCasinoUser(to.getUniqueId());
        final MuxExtraUser extraUserFrom = from.isOnline() ? ms.getExtras().getExtraUser(from.getUniqueId()) : db.loadExtras(from.getUniqueId()),
                extraUserTo = to.isOnline() ? ms.getExtras().getExtraUser(to.getUniqueId()) : db.loadExtras(to.getUniqueId());
        if (from.isOnline()) {
            from.getPlayer().closeInventory();
        }
        userTo.addCoins(userFrom.getCoins());
        userTo.addCents(userFrom.getCents());
        userTo.addGems(userFrom.getGems());
        userFrom.setCents(0);
        userFrom.setCoins(0L);
        userFrom.setGems(0);
        db.savePlayer(userFrom);
        db.savePlayer(userTo);
        casinoUserTo.setChips(casinoUserTo.getChips() + casinoUserFrom.getChips());
        casinoUserFrom.setChips(0);
        db.saveCasino(casinoUserFrom);
        db.saveCasino(casinoUserTo);
        final MuxRanks perms = ms.getPerms();
        final MuxRanks.PermissionsGroup groupFrom = perms.getGroupOf(from.getUniqueId());
        final MuxRanks.PermissionsGroup nonePrimaryGroupFrom = perms.getGroup(perms.getUserData(from.getUniqueId()).getGroup());
        if (groupFrom != null) {
            if (nonePrimaryGroupFrom != null && groupFrom.equals(nonePrimaryGroupFrom) == false) {
                perms.changePlayerGroup(to.getUniqueId(), userTo.getName(), nonePrimaryGroupFrom.getName(), executor);
                perms.changePlayerGroup(from.getUniqueId(), userFrom.getName(), "Default", executor);
            }
            perms.changePlayerGroup(to.getUniqueId(), userTo.getName(), groupFrom.getName(), executor);
            perms.changePlayerGroup(from.getUniqueId(), userFrom.getName(), groupFrom.isTeamGroup() ? "teamnull" : "Default", executor);
        }
        if (extraUserTo != null && extraUserFrom != null) {
            extraUserTo.getExtraCMDs().addAll(extraUserFrom.getExtraCMDs());
            extraUserTo.getEmojis().addAll(extraUserFrom.getEmojis());
            extraUserTo.getPerks().getOwned().addAll(extraUserFrom.getPerks().getOwned());
            extraUserTo.getPets().getOwned().addAll(extraUserFrom.getPets().getOwned());
            extraUserTo.getMounts().getOwned().addAll(extraUserFrom.getMounts().getOwned());
            extraUserTo.setEnderChest(extraUserFrom.getEnderChest());
            extraUserFrom.getExtraCMDs().clear();
            extraUserFrom.getEmojis().clear();
            extraUserFrom.getPerks().getActive().clear();
            extraUserFrom.getPerks().getOwned().clear();
            extraUserFrom.getMounts().setActive((short) -1);
            extraUserFrom.getMounts().getOwned().clear();
            extraUserFrom.getPets().setActive(null);
            extraUserFrom.getPets().getOwned().clear();
            extraUserFrom.setEnderChest((byte) 0);
            db.saveExtras(extraUserFrom);
            db.saveExtras(extraUserTo);
        }
        if (executor instanceof Player) {
            final Player p = (Player) executor;
            ms.showItemBar(p, "§aÜbertragung erfolgreich.");
        }
        ms.getChat().sendOfflineMessageOrChat(to, userTo, "MuxTransfer", "Du hast durch einen Spieler Transfer den Rang, alle Coins, Gems, Chips und Extras des Spielers §6" + userFrom.getName() + " §ferhalten.", "§f", null);
        ms.getBans().applyPermaBan(executor, from.getUniqueId(), "Usertransfer > " + userTo.getName());
        ms.getHistory().addPaymentHistory(to.getUniqueId(), from.getUniqueId(), "PAYMENT", "USER TRANSFER", "0", System.currentTimeMillis());
        ms.getHistory().addPaymentHistory(from.getUniqueId(), to.getUniqueId(), "PAYMENT", "USER TRANSFER", "0", System.currentTimeMillis());
        executor.sendMessage("§fÜbertragung von §c" + userFrom.getName() + " §fzu §a" + userTo.getName() + " §ferfolgreich abgeschlossen.");
    }
}