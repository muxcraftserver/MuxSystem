package me.muxteam.shared;

import de.vantrex.simplenetty.listener.SimplePacketListener;
import de.vantrex.simplenetty.listener.handler.SimplePacketHandler;
import de.vantrex.simplenetty.session.Session;

/**
 * DIESE KLASSE ERST DELETEN WENN WIR EINE ANDERE METHODE ZUM TESTEN HABEN (2 Server z.B.)
 */
public class TestNettyListener implements SimplePacketListener {

    @SimplePacketHandler
    public void handleResetVote(MuxSharedPackets.PacketResetVotes packet, Session session) {
        System.out.println("HUH");
    }

}
