package me.muxteam.basic;

import me.muxteam.basic.MuxHolograms.IndividualHologram;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class MuxActions {

	public interface PlayerAction {

		void call(final Player p);

	}

	public interface AnvilAction {

		void call(final String input, final Player p);

	}

	public interface RouletteAction {

		void call(final ItemStack i);

	}

	public interface HologramAction {

		void call(final IndividualHologram holo, final Player p);
	}

	public interface Action {

		void call();

	}

}