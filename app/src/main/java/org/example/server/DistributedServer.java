package org.example.server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import org.example.protos.SubscriberOuterClass;

public class DistributedServer {
    private final int serverId; // Sunucunun ID'si
    private final int clientPort; // İstemcilerden gelen bağlantılar için port
    private final List<Integer> peerPorts; // Diğer sunucuların peer portları
    private final ConcurrentLinkedQueue<SubscriberOuterClass.Subscriber> subscribers = new ConcurrentLinkedQueue<>();
    private final ExecutorService threadPool = Executors.newFixedThreadPool(10);

    // Fault Tolerance Seviyesi (1 veya 2)
    private final int faultToleranceLevel;

    public DistributedServer(int serverId, int clientPort, List<Integer> peerPorts, int faultToleranceLevel) {
        this.serverId = serverId;
        this.clientPort = clientPort;
        this.peerPorts = peerPorts;
        this.faultToleranceLevel = faultToleranceLevel;
    }

    public void start() {
        new Thread(this::startClientListener).start();
    }

    private void startClientListener() {
        try (ServerSocket serverSocket = new ServerSocket(clientPort)) {
            System.out.println("Server " + serverId + " listening on port " + clientPort);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                threadPool.execute(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("Error starting client listener: " + e.getMessage());
        }
    }

    private void handleClient(Socket clientSocket) {
        try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream())) {

            // İstemciden gelen veriyi al
            byte[] data = (byte[]) in.readObject();
            SubscriberOuterClass.Subscriber subscriber = SubscriberOuterClass.Subscriber.parseFrom(data);

            // Kendi sunucusuna aboneyi ekle
            subscribers.add(subscriber);
            System.out.println("Server " + serverId + " received subscriber: " + subscriber.getNameSurname());

            // Diğer sunuculara yedekle
            backupToPeers(subscriber);

            // Yanıt gönder
            out.writeObject(subscriber.toByteArray());

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error handling client: " + e.getMessage());
        }
    }

    private void backupToPeers(SubscriberOuterClass.Subscriber subscriber) {
        if (faultToleranceLevel == 1) {
            // Fault Tolerance 1: Tek bir sunucuya yedekle
            int selectedPeerPort = selectPeerPort();
            sendToPeer(selectedPeerPort, subscriber);
        } else if (faultToleranceLevel == 2) {
            // Fault Tolerance 2: Tüm sunuculara yedekle
            for (int peerPort : peerPorts) {
                sendToPeer(peerPort, subscriber);
            }
        }
    }

    private int selectPeerPort() {
        // Basit bir seçim algoritması: İlk uygun portu seç
        for (int peerPort : peerPorts) {
            if (isPeerAvailable(peerPort)) {
                return peerPort;
            }
        }
        return -1; // Hiçbir peer yoksa -1 döner
    }

    private boolean isPeerAvailable(int peerPort) {
        try (Socket socket = new Socket("localhost", peerPort)) {
            return true; // Peer çalışıyorsa true döner
        } catch (IOException e) {
            return false; // Peer kapalıysa false döner
        }
    }

    private void sendToPeer(int peerPort, SubscriberOuterClass.Subscriber subscriber) {
        try (Socket socket = new Socket("localhost", peerPort);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

            out.writeObject(subscriber.toByteArray());
            String logMessage = "Yedeklendi: " + subscriber.getNameSurname() + " to peer on port " + peerPort;
            System.out.println(logMessage);

            // Log dosyasına yaz
            logBackup(logMessage);

        } catch (IOException e) {
            String errorMessage = "Error sending to peer on port " + peerPort + ": " + e.getMessage();
            System.err.println(errorMessage);

            // Hata durumunu log dosyasına yaz
            logBackup(errorMessage);
        }
    }

    private void logBackup(String message) {
        String fileName = "server" + serverId + "_backup.log";
        try (FileWriter fw = new FileWriter(fileName, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(message); // Log mesajını dosyaya yaz
        } catch (IOException e) {
            System.err.println("Log dosyasına yazılamadı: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        // Server1: 6001 (client port), peer ports: 5002, 5003, fault tolerance level 1
        DistributedServer server1 = new DistributedServer(1, 5001, List.of(5002, 5003), 1);

        // Server2: 6002 (client port), peer ports: 5001, 5003, fault tolerance level 2
        DistributedServer server2 = new DistributedServer(2, 5002, List.of(5001, 5003), 2);

        // Server3: 6003 (client port), peer ports: 5001, 5002, fault tolerance level 2
        DistributedServer server3 = new DistributedServer(3, 5003, List.of(5001, 5002), 2);

        server1.start();
        server2.start();
        server3.start();
    }
}
