package me.muxteam.basic;

import me.muxteam.basic.MuxActions.AnvilAction;
import me.muxteam.muxsystem.MuxInventory.InvType;
import me.muxteam.muxsystem.MuxSystem;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class MuxAnvil {
	private MuxSystem ms;
	private AnvilAction action;

	public MuxAnvil(final MuxSystem ms, final AnvilAction action) {
		this.ms = ms;
		this.action = action;
	}

	public void show(final Player p, final String text, final ItemStack item) {
		final EntityPlayer ep = ((CraftPlayer) p).getHandle();
		final int c = ep.nextContainerCounter();
		final ContainerAnvil container = new ContainerAnvil(ep.inventory, ep.world, new BlockPosition(0,0,0), ep);

		container.checkReachable = false;

		container.getBukkitView().getTopInventory().setItem(0, ms.renameItemStack(item, text));

		ep.playerConnection.sendPacket(new PacketPlayOutOpenWindow(c, "minecraft:anvil", new ChatMessage("Repairing"), 0));
		ep.activeContainer = container;
		ep.activeContainer.windowId = c;
		ep.activeContainer.addSlotListener(ep);

		ms.setActiveInv(p.getName(), InvType.MUXANVIL);
		ms.getInv().setAnvil(p.getName(), this.action);
		this.ms = null;
		this.action = null;
	}
	public void show(final Player p, final String text) {
		show(p, text, new ItemStack(Material.PAPER));
	}
}