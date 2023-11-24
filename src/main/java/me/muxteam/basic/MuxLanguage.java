package me.muxteam.basic;

import me.muxteam.muxsystem.MuxSystem;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.PropertyResourceBundle;

public final class MuxLanguage {
	private String locale;
	private PropertyResourceBundle bundle;

	public MuxLanguage(final MuxSystem ms) {
		loadConfig(ms, ms.getConfig());
	}

	public void loadConfig(final MuxSystem ms, final FileConfiguration config) {
		locale = config.getString("lang");
		final String filename = "lang_" + locale + ".properties";
		try {
			bundle = new PropertyResourceBundle(new InputStreamReader(ms.getResource(filename), StandardCharsets.UTF_8));
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
	public String getLang(final String holder) {
		String l;
		try {
			l = bundle.getString(holder);
		} catch (final Exception ex) {
			System.err.println("MuxSystem> Language missing: " + holder);
			return holder;
		}
		return l.startsWith("\"") ? l.substring(1) : l;
	}
	public String getLocale() {
		return locale.toUpperCase();
	}
}