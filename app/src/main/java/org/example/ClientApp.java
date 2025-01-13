package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;

public class ClientApp extends JFrame {
    private JTextField serverIpField;
    private JTextField serverPortField;
    private JTextField subscriberNameField;
    private JTextArea outputArea;

    public ClientApp() {
        setTitle("Distributed System Client App");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Input Panel
        JPanel inputPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Client Configuration"));

        inputPanel.add(new JLabel("Server IP:"));
        serverIpField = new JTextField("localhost");
        inputPanel.add(serverIpField);

        inputPanel.add(new JLabel("Server Port:"));
        serverPortField = new JTextField("5001");
        inputPanel.add(serverPortField);

        inputPanel.add(new JLabel("Subscriber Name:"));
        subscriberNameField = new JTextField();
        inputPanel.add(subscriberNameField);

        JButton subscribeButton = new JButton("Subscribe");
        subscribeButton.addActionListener(new SubscribeAction());
        inputPanel.add(subscribeButton);

        JButton capacityButton = new JButton("Get Capacity");
        capacityButton.addActionListener(new CapacityAction());
        inputPanel.add(capacityButton);

        add(inputPanel, BorderLayout.NORTH);

        // Output Panel
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Output"));
        add(scrollPane, BorderLayout.CENTER);
    }

    private class SubscribeAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String serverIp = serverIpField.getText();
            int serverPort = Integer.parseInt(serverPortField.getText());
            String subscriberName = subscriberNameField.getText();

            try (Socket socket = new Socket(serverIp, serverPort);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                // Send SUBS message
                out.writeObject("SUBS:" + subscriberName);
                String response = (String) in.readObject();
                outputArea.append("Response: " + response + "\n");
            } catch (IOException | ClassNotFoundException ex) {
                outputArea.append("Error: " + ex.getMessage() + "\n");
            }
        }
    }

    private class CapacityAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String serverIp = serverIpField.getText();
            int serverPort = Integer.parseInt(serverPortField.getText());

            try (Socket socket = new Socket(serverIp, serverPort);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                // Send GET_CAPACITY message
                out.writeObject("GET_CAPACITY");
                String response = (String) in.readObject();
                outputArea.append("Server Capacity: " + response + "\n");
            } catch (IOException | ClassNotFoundException ex) {
                outputArea.append("Error: " + ex.getMessage() + "\n");
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClientApp app = new ClientApp();
            app.setVisible(true);
        });
    }
}
