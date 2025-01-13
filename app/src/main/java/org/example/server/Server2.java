package org.example.server;

import java.io.*;
import java.net.*;
import org.example.protos.SubscriberOuterClass;

public class Server2 {
    public static void main(String[] args) {
        int port = 5002; // Sunucunun dinleyeceği port
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Sunucu " + port + " portunda dinleniyor...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Yeni bir istemci bağlandı: " + clientSocket.getInetAddress());

                try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                     ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream())) {

                    // İstemciden veri al
                    byte[] data = (byte[]) in.readObject();
                    SubscriberOuterClass.Subscriber subscriber = SubscriberOuterClass.Subscriber.parseFrom(data);

                    // Gelen abone bilgilerini yazdır
                    System.out.println("Abone Alındı: " + subscriber.getNameSurname());

                    // Yanıt oluştur ve istemciye gönder
                    SubscriberOuterClass.Subscriber response = SubscriberOuterClass.Subscriber.newBuilder()
                            .setID(subscriber.getID())
                            .setNameSurname(subscriber.getNameSurname())
                            .setStartDate(subscriber.getStartDate())
                            .setLastAccessed(System.currentTimeMillis())
                            .addAllInterests(subscriber.getInterestsList())
                            .setIsOnline(true)
                            .build();

                    out.writeObject(response.toByteArray());
                } catch (IOException | ClassNotFoundException e) {
                    System.err.println("İstemci işleme hatası: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Sunucu başlatılamadı: " + e.getMessage());
        }
    }
}
