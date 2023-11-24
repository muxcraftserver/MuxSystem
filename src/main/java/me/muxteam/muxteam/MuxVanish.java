package me.muxteam.muxteam;

import me.muxteam.muxsystem.MuxSystem;
import me.muxteam.shared.MuxSharedPackets;
import org.bukkit.Server;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MuxVanish {
    private MuxSystem ms;
    private final Map<String, Long> vplayers = new HashMap<>();

    public MuxVanish(final MuxSystem ms) {
        this.ms = ms;
    }
    public void close() {
        final Server sr = ms.getServer();
        for (final String s : vplayers.keySet()) {
            final Player p = sr.getPlayer(s);
            if (p == null) continue;
            p.removePotionEffect(PotionEffectType.INVISIBILITY);
            ms.sendNormalTitle(p, "§aDu bist wieder sichtbar.", 20, 100, 20);
            sr.getOnlinePlayers().forEach(pl -> pl.showPlayer(p));
        }
        this.ms = null;
    }
    public boolean isVanish(final String pname) {
        return vplayers.containsKey(pname);
    }
    public void handleQuit(final Player p) {
        final String pname = p.getName();
        if (vplayers.containsKey(pname)) {
            ms.getForkJoinPool().execute(() -> ms.getShared().getNetty().sendPacket(new MuxSharedPackets.PacketPlayerVanish(pname, true)));
            ms.getShared().onSharedVanish(pname, true);
            p.removePotionEffect(PotionEffectType.INVISIBILITY);
        }
        final Server sr = ms.getServer();
        if (p.hasPermission("muxsystem.vanish")) {
            final Set<String> closeinv = new HashSet<>();
            p.getInventory().getViewers().forEach(victim -> closeinv.add(victim.getName()));
            for (final String v : closeinv) {
                final Player pl = sr.getPlayer(v);
                pl.closeInventory();
            }
        }
    }
    public void handleJoin(final Player p) {
        final Server sr = ms.getServer();
        if (p.hasPermission("muxsystem.vanish") && vplayers.containsKey(p.getName())) {
            ms.getForkJoinPool().execute(() -> ms.getShared().getNetty().sendPacket(new MuxSharedPackets.PacketPlayerVanish(p.getName(), false)));
            ms.getShared().onSharedVanish(p.getName(), false);
            sr.getOnlinePlayers().forEach(victim -> {
                if (victim.isOp() == false && (p.isOp() || victim.hasPermission("muxsystem.vanish") == false)) {
                    victim.hidePlayer(p);
                }
            });
            p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 9999999, 0));
        }
        if (p.isOp() == false) {
            for (final String s : vplayers.keySet()) {
                final Player pl = sr.getPlayer(s);
                if (pl == null || (pl.isOp() == false && p.hasPermission("muxsystem.vanish"))) continue;
                p.hidePlayer(pl);
            }
        }
    }
    public boolean handleCommand(final Player p, final String[] args) {
        if (p.hasPermission("muxsystem.vanish") == false) {
            ms.sendNoCMDMessage(p);
            return true;
        }
        final Long time = vplayers.remove(p.getName());
        if (time != null) {
            final Set<String> exclude = ms.getServer().getOnlinePlayers().stream().filter(victim -> victim.canSee(p)).map(HumanEntity::getName).collect(Collectors.toSet());
            ms.getServer().getOnlinePlayers().forEach(victim -> victim.showPlayer(p));
            p.removePotionEffect(PotionEffectType.INVISIBILITY);
            ms.getFriends().sendFriendAlert(p, exclude);
            ms.showItemBar(p, "§fDu hast dein Vanish-Modus §cdeaktiviert§f.");
            ms.getShared().onSharedVanish(p.getName(), true);
            ms.getShared().getNetty().sendPacket(new MuxSharedPackets.PacketPlayerVanish(p.getName(), true));
            return true;
        }
        final Set<String> closeinv = new HashSet<>();
        p.getInventory().getViewers().forEach(victim -> closeinv.add(victim.getName()));
        ms.getServer().getOnlinePlayers().forEach(victim -> {
            if (victim.isOp() == false && (p.isOp() || victim.hasPermission("muxsystem.vanish") == false)) {
                victim.hidePlayer(p);
                if (closeinv.contains(victim.getName())) victim.closeInventory();
            }
        });
        p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 9999999, 0));
        vplayers.put(p.getName(), System.currentTimeMillis());
        ms.getShared().onSharedVanish(p.getName(), false);
        ms.getShared().getNetty().sendPacket(new MuxSharedPackets.PacketPlayerVanish(p.getName(), false));

        ms.showItemBar(p, "§fDu hast dein Vanish-Modus §aaktiviert§f.");
        if (args.length == 0 || args[0].equals("-p") == false) ms.getPets().deactivatePet(p);
        if (p.isInsideVehicle() == false || p.getVehicle() instanceof ArmorStand == false) ms.getMounts().deactivateMount(p);
        return true;
    }
}