package me.muxteam.muxteam;

import me.muxteam.basic.Pageifier;
import me.muxteam.muxsystem.MuxInventory;
import me.muxteam.muxsystem.MuxSystem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class MuxFeedback {
    private MuxSystem ms;
    private int feedbacksopen = 0;

    public MuxFeedback(final MuxSystem ms) {
        this.ms = ms;
        final List<Object[]> feedbacks = ms.getDB().getFeedback(false);
        if (feedbacks != null) {
            this.feedbacksopen = feedbacks.size();
        }
    }
    public void close() {
        this.ms = null;
    }

    public int getOpenFeedbacks() {
        return feedbacksopen;
    }

    public boolean handleCommand(final Player p, final String[] args) {
        if (args.length < 2) {
            ms.showItemBar(p, ms.usage("/feedback [nachricht]"));
            return true;
        } else if (ms.checkGeneralCooldown(p.getName(), "FEEDBACK", 300000L, true)) {
            ms.showItemBar(p, "§cDu kannst nur alle 5 Minuten ein Feedback abgeben.");
            return true;
        }
        final String str = ms.fromArgs(args, 0);
        ms.showItemBar(p, "§fFeedback erfolgreich §aabgegeben§7.");
        p.playSound(p.getLocation(), Sound.ORB_PICKUP, 1F, 1F);
        new BukkitRunnable() {
            @Override
            public void run() {
                ms.getDB().addFeedback(p.getName(), str);
            }
        }.runTaskAsynchronously(ms);
        return true;
    }

    public void handleInventory(final Player p, final ItemStack i, final Inventory inv, final int rawslot, final boolean rightclick) {
        final Material m = i.getType();
        final String title = inv.getTitle();
        if (m == Material.RED_ROSE) {
            p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
        } else if (m == Material.ITEM_FRAME) {
            if (title != null && title.contains("archiv")) {
                openFeedback(p, true);
            } else {
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                p.performCommand("a");
            }
        } else if (m == Material.BOOKSHELF) {
           if (title != null && title.contains("Feedbacksarchiv")) {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return;
            }
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            openFeedbackArchive(p, 0);
        } else if (m == Material.FEATHER) {
            if (i.hasItemMeta() == false || i.getItemMeta().hasLore() == false) return;
            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
            final List<String> lore = i.getItemMeta().getLore();
            final String plname = ChatColor.stripColor(lore.get(0).split(" ")[1]);
            final int fid = Integer.parseInt(ChatColor.stripColor(i.getItemMeta().getDisplayName().split(" §0")[1]));
            if (lore.get(lore.size() - 1).contains("archiv")) {
                if (rightclick == false) {
                    p.sendMessage(ms.header((byte) 12, "§c"));
                    p.sendMessage("  §c§lMuxFeedback");
                    p.sendMessage(" ");
                    ms.chatClickHoverShow(p, "  §7Klicke §chier§7, um den Spieler anzuschreiben.", "§c§oKlicke um anzuschreiben", "/smsg " + plname + " ");
                    p.sendMessage(ms.footer((byte) 12, "§c"));
                    p.closeInventory();
                    return;
                }
                ms.getDB().setFeedbackDone(fid, true);
                openFeedback(p, false);
            } else {
                final int page = Integer.parseInt(inv.getItem(4).getItemMeta().getDisplayName().split(":")[1]);
                ms.getDB().setFeedbackDone(fid, false);
                openFeedbackArchive(p, page);
            }
        } else if (m == Material.SKULL_ITEM) {
            final int page = Integer.parseInt(inv.getItem(4).getItemMeta().getDisplayName().split(":")[1]);
            if (rawslot == 7) {
                p.playSound(p.getLocation(), Sound.WOOD_CLICK, 1F, 1F);
                openFeedbackArchive(p, page - 1);
            } else {
                p.playSound(p.getLocation(), Sound.WOOD_CLICK, 1F, 1F);
                openFeedbackArchive(p, page + 1);
            }
        }
    }

    private void openFeedbackArchive(final Player p, final int page) {
        final Inventory inv = ms.getServer().createInventory(null, 45, "§0§lMuxAdmin§0 | Feedbacksarchiv");
        inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        inv.setItem(4, ms.renameItemStack(new ItemStack(Material.BOOKSHELF), "§c§lArchiv §0Page:" + page));
        final List<Object[]> feedback = ms.getDB().getFeedback(true);
        final boolean fulltrusted = ms.isFullTrusted(p.getName());
        final Pageifier<ItemStack> pages = new Pageifier<>(27);
        if (feedback != null) {
            byte count = 1;
            final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
            sdf.setTimeZone(TimeZone.getTimeZone("CET"));
            for (final Object[] obj : feedback) {
                final String msg = (String) obj[4], time = sdf.format(new Date((Long) obj[2]));
                final int id = (Integer) obj[5];
                pages.addItem(ms.addLore(ms.addLore(ms.renameItemStack(new ItemStack(Material.FEATHER), "§2§lFeedback §2#§l" + count + " §0" + id,
                                "§7Spieler: §f" + obj[0], (fulltrusted && obj[1].equals("NULLSTR") == false ? "§7Email: §f" + obj[1] : ""),
                                "§7Zeitpunkt: §f" + time, "§7Thema: §f" + obj[3], "§7Nachricht:"), ms.getLoreFromLongText(msg, "§f")), "",
                        "§fKlicke§7, um den Feedback wieder zu eröffnen."));
                count++;
            }
        }
        ms.addPageButtons(inv, page, pages);
        if (ms.getActiveInv(p.getName()) != MuxInventory.InvType.FEEDBACK) p.closeInventory();
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), MuxInventory.InvType.FEEDBACK);
    }

    public void openFeedback(final Player p, final boolean sound) {
        if (sound) p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
        final Inventory inv2 = ms.getServer().createInventory(null, 45, "§0§lMuxAdmin§0 | Feedback");
        inv2.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        inv2.setItem(4, ms.renameItemStack(new ItemStack(Material.RED_ROSE, 1, (byte) 4), "§c§lFeedback", "§7Die Vorschläge der Spieler helfen", "§7den Server besser anzupassen."));
        inv2.setItem(8, ms.renameItemStack(new ItemStack(Material.BOOKSHELF), "§c§lArchiv", "", "§cKlicke§7, um den Archiv einzusehen."));
        final boolean fulltrusted = ms.isFullTrusted(p.getName());
        final List<Object[]> messages = ms.getDB().getFeedback(false);
        short pos = 18;
        if (messages != null) {
            this.feedbacksopen = messages.size();
            byte count = 1;
            final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
            sdf.setTimeZone(TimeZone.getTimeZone("CET"));
            for (final Object[] obj : messages) {
                final String msg = (String) obj[4], time = sdf.format(new Date((Long) obj[2]));
                final int id = (Integer) obj[5];
                inv2.setItem(pos, ms.addLore(ms.addLore(ms.renameItemStack(new ItemStack(Material.FEATHER), "§2§lFeedback §2#§l" + count + " §0" + id,
                                "§7Spieler: §f" + obj[0], (fulltrusted && obj[1].equals("NULLSTR") == false ? "§7Email: §f" + obj[1] : ""),
                                "§7Zeitpunkt: §f" + time, "§7Thema: §f" + obj[3], "§7Nachricht:"), ms.getLoreFromLongText(msg, "§f")), "",
                        "§2Linksklick§7, um den Spieler anzuschreiben.", "§cRechtsklick§7, um den Feedback zu archivieren§7."));
                pos++;
                count++;
                if (pos == 45) break;
            }
        }
        final MuxInventory.InvType it = ms.getActiveInv(p.getName());
        if (it != MuxInventory.InvType.FEEDBACK && it != MuxInventory.InvType.ADMIN) p.closeInventory();
        p.openInventory(inv2);
        ms.setActiveInv(p.getName(), MuxInventory.InvType.FEEDBACK);
    }
}