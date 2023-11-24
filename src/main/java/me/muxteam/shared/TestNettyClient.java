package me.muxteam.shared;

import de.vantrex.simplenetty.annotations.exceptions.AnnotationNotFoundException;
import de.vantrex.simplenetty.client.SimpleClient;
import de.vantrex.simplenetty.client.settings.SimpleClientSettings;
import de.vantrex.simplenetty.packet.exceptions.PacketAlreadyRegisteredException;
import de.vantrex.simplenetty.packet.exceptions.PacketIdAlreadyRegisteredException;
import de.vantrex.simplenetty.protocol.impl.NumericProtocol;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * DIESE KLASSE ERST DELETEN WENN WIR EINE ANDERE METHODE ZUM TESTEN HABEN (2 Server z.B.)
 */
public class TestNettyClient {

    public static void main(String[] args) {
        System.out.println("STARTING");
        SimpleClient client = new SimpleClient(new SimpleClientSettings()
                .address("195.82.158.107") // <- address saved in SharedData DB table
                .port(38104)
                .threads(2)
                .protocol(new NumericProtocol()));
        try {
            client.getSettings().protocol().registerPacket(MuxSharedPackets.PacketResetVotes.class);
        } catch (AnnotationNotFoundException | PacketAlreadyRegisteredException | PacketIdAlreadyRegisteredException e) {
            throw new RuntimeException(e);
        }
        client.getSettings().protocol().registerListener(new TestNettyListener());
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("connected? " + client.getChannel());
        if (client.getChannel() == null) { // Bei mir am PC wird der Thread im Constructor von new SimpleClient() nicht gestartet?
            try {
                Method method = client.getClass().getDeclaredMethod("init");
                method.setAccessible(true);
                method.invoke(client);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    //    client.connect();
        client.send(new MuxSharedPackets.PacketResetVotes());
        try {
            Thread.sleep(10_000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("closing");
        client.close();
        System.out.println("closed");

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
