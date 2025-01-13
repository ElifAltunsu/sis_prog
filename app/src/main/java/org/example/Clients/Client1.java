package org.example.Clients;

import java.io.*;
import java.net.*;
import org.example.protos.SubscriberOuterClass;

public class Client1 {
    public static void main(String[] args) {
        String serverAddress = "localhost";
        int[] ports = {5001, 5002, 5003}; // Sunucu portları
        int assignedPort = ports[0]; // Client1'in atanmış portu (6001)

        tryToConnect(serverAddress, ports, assignedPort, "Client1");
    }

    private static void tryToConnect(String serverAddress, int[] ports, int assignedPort, String clientName) {
        for (int port : ports) {
            if (port != assignedPort && assignedPort != port) {
                System.out.println(clientName + " atanmış port: " + assignedPort + ", Ancak bağlanmayı deniyor: " + port);
            }

            try (Socket socket = new Socket(serverAddress, port);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                System.out.println(clientName + " " + port + " portuna bağlandı.");

                // Subscriber nesnesini oluştur
                SubscriberOuterClass.Subscriber subscriber = SubscriberOuterClass.Subscriber.newBuilder()
                        .setID(1)
                        .setNameSurname("Client1 User")
                        .setStartDate(System.currentTimeMillis())
                        .setLastAccessed(System.currentTimeMillis())
                        .addInterests("Technology")
                        .setDemand(SubscriberOuterClass.Subscriber.Demand.SUBS)
                        .build();

                // Sunucuya gönder
                out.writeObject(subscriber.toByteArray());
                System.out.println(clientName + " abone bilgisi gönderildi.");

                // Sunucudan yanıt al
                byte[] responseBytes = (byte[]) in.readObject();
                SubscriberOuterClass.Subscriber response = SubscriberOuterClass.Subscriber.parseFrom(responseBytes);
                System.out.println(clientName + " sunucudan yanıt aldı: " + response.getNameSurname());

                break; // Başarılı bir bağlantı kurulduysa döngüden çık
            } catch (IOException | ClassNotFoundException e) {
                System.err.println(clientName + " port " + port + " için bağlantı hatası: " + e.getMessage());
                // Başarısız bağlantı durumunda diğer porta geç
            }
        }
    }
}
