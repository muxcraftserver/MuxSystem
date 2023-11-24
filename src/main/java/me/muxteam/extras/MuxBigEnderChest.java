package me.muxteam.extras;

import me.muxteam.muxsystem.MuxSystem;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.UUID;

public final class MuxBigEnderChest {
	private MuxSystem ms;
	private MuxExtras extras;

	public MuxBigEnderChest(final MuxSystem ms) {
		this.ms = ms;
		this.extras = ms.getExtras();
	}

	public void close() {
		this.extras = null;
		this.ms = null;
	}
	public boolean openExtraEnderChest(final Player p, final boolean sound) {
		if (extras.hasExtraEnderChest(p.getUniqueId())) {
			if (ms.getInv().isBeingEdited(p, false)) {
				ms.showItemBar(p, "§cBitte warte etwas.");
				return true;
			}
			p.closeInventory();
			String enderchest = ms.getDB().loadEnderChest(p.getUniqueId());
			if (enderchest == null) {
				createAccount(p.getUniqueId());
				enderchest = toBase64(p.getEnderChest());
			} else if (enderchest.equals("error")) {
				return true;
			}
			if (sound) p.playSound(p.getLocation(), Sound.CHEST_OPEN, 1F, 1F);
			p.openInventory(getInventory(p, enderchest));
			return true;
		}
		return false;
	}
	public void handleClose(final Player p, final Inventory inv) {
		if (inv.getHolder() != null && inv.getHolder().equals(p)) {
			p.playSound(p.getLocation(), Sound.CHEST_CLOSE, 1F, 1F);
			ms.getDB().saveEnderChest(p.getUniqueId(), p.getName(), toBase64(inv));
		}
	}
	public Inventory getInventory(final OfflinePlayer p, final String enderchest) {
		final Inventory inv = ms.getServer().createInventory(p.isOnline() ? p.getPlayer() : null , 54, ms.getLang("extraenderchest")), mysqlInv = fromBase64(enderchest);
		if (mysqlInv == null) {
			return null;
		} else for (int i = 0; i < mysqlInv.getSize(); i++) {
			inv.setItem(i, mysqlInv.getItem(i));
		}
		return inv;
	}
	public void setEnderchest(final Player p, final UUID uuid, final String name, final Inventory inv) {
		p.playSound(p.getLocation(), Sound.CHEST_CLOSE, 1F, 1F);
		ms.getDB().saveEnderChest(uuid, name, toBase64(inv));
	}
	public void createAccount(final UUID uuid) {
		final Player p = ms.getServer().getPlayer(uuid);
		ms.getDB().saveEnderChest(uuid, p.getName(), toBase64(p.getEnderChest()));
	}
	private String toBase64(final Inventory inventory) {
		try {
			final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			final BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
			dataOutput.writeInt(inventory.getSize());
			for (int i = 0; i < inventory.getSize(); i++) {
				dataOutput.writeObject(inventory.getItem(i));
			}
			dataOutput.close();
			final byte[] b = outputStream.toByteArray();
			outputStream.close();
			return Base64Coder.encodeLines(b);
		} catch (final Exception e) {
			System.err.println("MuxSystem> Error while saving a MuxBigEnderChest.");
			e.printStackTrace();
		}
		return null;
	}
	private Inventory fromBase64(final String data) {
		try {
			final ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
			final BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
			final Inventory inventory = ms.getServer().createInventory(null, dataInput.readInt());
			for (int i = 0; i < inventory.getSize(); i++) {
				inventory.setItem(i, (ItemStack) dataInput.readObject());
			}
			dataInput.close();
			inputStream.close();
			return inventory;
		} catch (final Exception e) {
			System.err.println("MuxSystem> Error while loading a MuxBigEnderChest.");
			e.printStackTrace();
		}
		return null;
	}
}