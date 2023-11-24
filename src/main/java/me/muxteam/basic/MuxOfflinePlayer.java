package me.muxteam.basic;

import net.minecraft.server.v1_8_R3.InventoryEnderChest;
import net.minecraft.server.v1_8_R3.NBTCompressedStreamTools;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.NBTTagList;
import net.minecraft.server.v1_8_R3.PlayerInventory;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftInventory;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftInventoryPlayer;
import org.bukkit.inventory.Inventory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

public class MuxOfflinePlayer {
	private final UUID uuid;
	private final String name;
	private NBTTagCompound nbt = null;
	private File file;

	public MuxOfflinePlayer(final OfflinePlayer op) {
		this.uuid = op.getUniqueId();
		this.name = op.getName();

		try {
			load();
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	private void load() throws IOException {
		final File folder = new File(Bukkit.getWorlds().get(0).getWorldFolder(), "playerdata");
		this.file = new File(folder, uuid.toString() + ".dat");

		if (file.exists() == false) throw new FileNotFoundException();

		if (file.canWrite() == false) throw new IOException();
		try (final FileInputStream in = new FileInputStream(file)) {
			nbt = NBTCompressedStreamTools.a(in);
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	public void save() {
		try (final FileOutputStream os = new FileOutputStream(file)) {
			NBTCompressedStreamTools.a(nbt, os);
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	public UUID getUUID() {
		return uuid;
	}
	public String getName() {
		return name;
	}

	private Inventory getInventory(final String path) {
		if (nbt == null) return null;
		final PlayerInventory pi = new PlayerInventory(null);
		pi.b(nbt.getList(path, NBTType.NBT_COMPOUND.getId()));
		return new CraftInventoryPlayer(pi);
	}

	public Inventory getInventory() {
		return getInventory("Inventory");
	}

	public void setInventory(final Inventory inventory) {
		setInventory("Inventory", inventory);
	}

	private void setInventory(final String path, final Inventory inventory) {
		if (path.equals("EnderItems")) {
			nbt.set(path, ((InventoryEnderChest)((CraftInventory)inventory).getInventory()).h());
		} else {
			nbt.set(path, ((CraftInventoryPlayer)inventory).getInventory().a(new NBTTagList()));
		}
		save();
	}

	public Inventory getEnderChest() {
		if (nbt == null) return null;
		final InventoryEnderChest pe = new InventoryEnderChest();
		pe.a(nbt.getList("EnderItems", NBTType.NBT_COMPOUND.getId()));
		return new CraftInventory(pe);
	}

	public void setEnderChest(final Inventory inventory) {
		setInventory("EnderItems", inventory);
	}

	public Location getLocation() {
		if (nbt == null) return null;
		final NBTTagList position = nbt.getList("Pos", NBTType.NBT_DOUBLE.getId()), rotation = nbt.getList("Rotation", NBTType.NBT_FLOAT.getId());
		final UUID world = new UUID(nbt.getLong("WorldUUIDMost"), nbt.getLong("WorldUUIDLeast"));
		final double x = position.d(0), y = position.d(1), z = position.d(2);
		final float yaw = rotation.e(0), pitch = rotation.e(1);
		final World w = Bukkit.getWorld(world);
		if (w == null) return null;
		return new Location(w, x, y, z, yaw, pitch);
	}
	public enum NBTType {
		NBT_END(0),
		NBT_BYTE(1),
		NBT_SHORT(2),
		NBT_INT(3),
		NBT_LONG(4),
		NBT_FLOAT(5),
		NBT_DOUBLE(6),
		NBT_ARRAY_BYTE(7),
		NBT_STRING(8),
		NBT_LIST(9),
		NBT_COMPOUND(10),
		NBT_ARRAY_INT(11);

		private final int id;

		public int getId() {
			return id;
		}

		NBTType(final int type) {
			id = type;
		}
	}
}