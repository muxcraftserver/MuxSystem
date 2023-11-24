package me.muxteam.events;

import me.muxteam.muxsystem.MuxSystem;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@NonJoinableEvent
public class MuxGiveAll extends Event {
    private List<ItemStack> items;
    private final Inventory inv;

    public MuxGiveAll(final MuxEvents e) {
        super(e);
        name = "MuxGiveAll";
        item = new ItemStack(Material.PRISMARINE_CRYSTALS);
        final FileConfiguration hashYML = ms.getHashYAML();
        if (hashYML.contains("eventgiveall")) {
            items = fromBase64(hashYML.getString("eventgiveall"));
        } else {
            items = new ArrayList<>();
            items.add(new ItemStack(Material.DIRT));
        }
        inv = ms.getServer().createInventory(null, 54, "§0§lMuxEvent§0 | GiveAll");
        inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        inv.setItem(4, ms.renameItemStack(getItem(), "§d§l" + getName(), getDescription()));
        inv.setItem(6, ms.renameItemStack(new ItemStack(Material.CHEST), "§6§lKisten verteilen", "", "§7Klicke, um eine Kiste §6auszuwählen§7."));
        inv.setItem(8, ms.renameItemStack(new ItemStack(Material.RABBIT_FOOT), "§6§lNächstes Item", "", "§7Klicke, um das nächste Item zu §6verteilen§7."));
        for (int i = 18; i < items.size() + 18; i++) {
            inv.setItem(i, items.get(i - 18));
        }
    }

    public Inventory getInventory() {
        return inv;
    }

    public List<ItemStack> getItems() {
        return items;
    }

    public void saveItems(final List<ItemStack> items) {
        this.items = items;
        ms.getHashYAML().set("eventgiveall", toBase64(items));
    }

    public boolean giveItem() {
        if (items.isEmpty()) return false;
        final Iterator<ItemStack> iterator = items.iterator();
        final ItemStack it = iterator.next();
        iterator.remove();
        if (it == null || it.getType() == Material.AIR || it.getType() == Material.MOB_SPAWNER) return true;
        final String itemname = it.getType().toString().toLowerCase().replace("_", "").replace("item", ""),
                msg = "§a" + it.getAmount() + " " + itemname + " wurde dir ins Inventar hinzugefügt.";
        ms.broadcastMessage("§d§lMuxGiveAll>§7 Alle Spieler haben §e" + it.getAmount() + " " + itemname + " §7erhalten.", null, MuxSystem.Priority.HIGH);
        for (final Player pl : ms.getServer().getOnlinePlayers()) {
            if (ms.hasVoted(pl) == false) {
                ms.chatClickHoverRun(pl,  "§d§lMuxGiveAll>§7 Klicke §dhier§7, um zu voten und teilzunehmen.", "§d§oKlicke zum voten", "/vote");
                continue;
            }
            pl.getInventory().addItem(it.clone());
            ms.showItemBar(pl, msg);
        }
        return true;
    }

    private String toBase64(final List<ItemStack> items) {
        try {
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            final BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeInt(items.size());
            for (final ItemStack item : items) {
                dataOutput.writeObject(item);
            }
            dataOutput.close();
            final byte[] b = outputStream.toByteArray();
            outputStream.close();
            return Base64Coder.encodeLines(b);
        } catch (final Exception e) {
            System.err.println("MuxSystem> Error while saving MuxGiveAll Items.");
            e.printStackTrace();
        }
        return null;
    }

    private List<ItemStack> fromBase64(final String data) {
        try {
            final ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            final BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            final List<ItemStack> itemstacks = new ArrayList<>();
            final int size = dataInput.readInt();
            for (int i = 0; i < size; i++) {
                itemstacks.add((ItemStack) dataInput.readObject());
            }
            dataInput.close();
            inputStream.close();
            return itemstacks;
        } catch (final Exception e) {
            System.err.println("MuxSystem> Error while loading MuxGiveAll Items.");
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "§7Verschiedene Items oder Kisten",
                "§7werden an alle Spieler verteilt,",
                "§7die bereits gevotet haben.",
        };
    }

    @Override
    public void start() {
        for (final Player pl : ms.getServer().getOnlinePlayers()) {
            pl.sendMessage("§d§lMuxEvent>§7 GiveAll hat jetzt begonnen.");
            if (ms.hasVoted(pl) == false) ms.chatClickHoverRun(pl, "§d§lMuxEvent>§7 Klicke §dhier§7, um zu voten und teilzunehmen.", "§d§oKlicke zum voten", "/vote");
            pl.playSound(pl.getLocation(), Sound.NOTE_PLING, 1F, 0.1F);
        }
        spawnEventNPC("§d§lEvent Infos");
    }

    @Override
    public boolean join(final Player p) {
        p.sendMessage("§d§lMuxEvent>§7 Derzeit läuft das §d" + getName() + "§7 Event.");
        p.sendMessage("§d§lMuxEvent>§7 Verschiedene Items werden vergeben.");
        return false;
    }
}