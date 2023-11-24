package me.muxteam.basic;

import me.muxteam.basic.MuxActions.PlayerAction;
import me.muxteam.muxsystem.MuxInventory.InvType;
import me.muxteam.muxsystem.MuxSystem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class ConfirmInventory {
	private MuxSystem ms;
	private ConfirmActions actions;
	private byte abortcolor = (byte) 14;

	public ConfirmInventory(final MuxSystem ms, final PlayerAction confirm, final PlayerAction cancel) {
		this.ms = ms;
		this.actions = new ConfirmActions(confirm, cancel);
	}

	public Inventory show(final Player p, final String name, final String confirmlabel, final String abortlabel) {
		final Inventory i = ms.getServer().createInventory(null, 45, name);
		final ItemStack ok = ms.renameItemStack(new ItemStack(new ItemStack(Material.STAINED_CLAY, 1, (byte) 5)), confirmlabel),
				abort = ms.renameItemStack(new ItemStack(new ItemStack(Material.STAINED_CLAY, 1, abortcolor)), abortlabel);
		createButton(i, 10, ok);
		createButton(i, 14, abort);
		p.openInventory(i);
		ms.getInv().setConfirm(p.getName(), actions);
		ms.setActiveInv(p.getName(), InvType.CONFIRM);
		this.ms = null;
		this.actions = null;
		return i;
	}
	public Inventory show(final Player p, final String name, final String confirmlabel, final String abortlabel, final ItemStack item, final String itemname, final String ... itemlore) {
		final Inventory inv = ms.getServer().createInventory(null, 54, name);
		final ItemStack ok = ms.renameItemStack(new ItemStack(new ItemStack(Material.STAINED_CLAY, 1, (byte) 5)), confirmlabel),
				abort = ms.renameItemStack(new ItemStack(new ItemStack(Material.STAINED_CLAY, 1, abortcolor)), abortlabel);
		inv.setItem(13, ms.renameItemStack(item, itemname, itemlore));
		createButton(inv, 27, ok);
		createButton(inv, 33, abort);
		p.openInventory(inv);
		ms.getInv().setConfirm(p.getName(), actions);
		ms.setActiveInv(p.getName(), InvType.CONFIRM);
		this.ms = null;
		this.actions = null;
		return inv;
	}
	public ConfirmInventory setAbortColor(final byte color) {
		abortcolor = color;
		return this;
	}
	private void createButton(final Inventory i, int pos, final ItemStack item) {
		for (short n = 1; n <= 9; n++) {
			i.setItem(pos, item);
			if (n % 3 == 0) pos += 7;
			else pos++;
		}
	}
	public final class ConfirmActions {
		public final PlayerAction cancelaction, confirmaction;
		public ConfirmActions(final PlayerAction confirm, final PlayerAction cancel) {
			this.confirmaction = confirm;
			this.cancelaction = cancel;
		}
	}
}
