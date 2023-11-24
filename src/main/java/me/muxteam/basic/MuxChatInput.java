package me.muxteam.basic;

import me.muxteam.basic.MuxActions.AnvilAction;
import me.muxteam.muxsystem.MuxSystem;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class MuxChatInput {
    private final MuxSystem ms;
    private final AnvilAction action;
    private long time;
    private String cancelreason;
    private int tries, currentTries;
    private boolean infinite, cancelled, handleCancelLocal;

    public MuxChatInput(final MuxSystem ms, final AnvilAction action) {
        this.ms = ms;
        this.action = action;
    }

    public void show(final Player p, final String text, final boolean infinite, final String cancelReason, final int tries, boolean handleCancelLocal) {
        this.handleCancelLocal = handleCancelLocal;
        this.show(p, text, infinite, cancelReason, tries);
    }

    public void show(final Player p, final String text, final boolean infinite, final String cancelReason, final int tries) {
        p.closeInventory();
        ms.getChat().disableChat(p.getName());
        p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
        p.sendMessage(new String[]{"", "", text});
        ms.getChat().setChatInput(p.getName(), this);
        this.infinite = infinite;
        this.time = System.currentTimeMillis();
        this.tries = tries;
        this.cancelreason = cancelReason;
    }

    public void show(final Player p, final String text, final boolean infinite) {
        this.show(p, text, infinite, null, -1);
    }

    public void show(final Player p, final String text) {
        this.show(p, text, false);
    }

    public long getTime() {
        return time;
    }

    public boolean isInfinite() {
        return infinite;
    }

    public int getTries() {
        return tries;
    }

    public int getCurrentTries() {
        return currentTries;
    }

    public String getCancelReason() {
        return cancelreason;
    }

    public AnvilAction getAction() {
        return action;
    }

    public void tryInput() {
        currentTries++;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void cancel() {
        this.cancelled = true;
    }

    public boolean isHandleCancelLocal() {
        return handleCancelLocal;
    }
}