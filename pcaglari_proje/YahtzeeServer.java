package com.mycompany.pcaglari_proje;

import java.io.*;
import java.net.*;

/**
 *
 * @author Ebrar Yıldız
 */
public class YahtzeeServer {
    private static final int PORT = 12345; // Sunucunun dinleyeceği port
    private static final ClientHandler[] clients = new ClientHandler[2]; // Maksimum 2 oyuncu için istemci tutucu
    private static int currentPlayerIndex = 0; // Şu anda hangi oyuncunun sırası olduğunu tutar

    public static void main(String[] args) {
        System.out.println("Yahtzee Sunucusu başlatılıyor...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                // Yeni bağlantı kabul edilir
                Socket clientSocket = serverSocket.accept();

                // Eğer iki oyuncu zaten bağlıysa, yeni geleni reddet
                boolean full = true;
                for (ClientHandler client : clients) {
                    if (client == null) {
                        full = false;
                        break;
                    }
                }

                if (full) {
                    PrintWriter tempOut = new PrintWriter(clientSocket.getOutputStream(), true);
                    tempOut.println("ERROR:Sunucu dolu. Maksimum 2 oyuncu.");
                    clientSocket.close();
                    continue;
                }

                // Boş bir slot (0 veya 1) bulunur
                int playerId = -1;
                for (int i = 0; i < 2; i++) {
                    if (clients[i] == null) {
                        playerId = i;
                        break;
                    }
                }

                // Yeni istemci işleyicisi başlatılır
                ClientHandler clientHandler = new ClientHandler(clientSocket, playerId);
                clients[playerId] = clientHandler;
                new Thread(clientHandler).start();

                // Her iki oyuncu bağlandıysa oyunu başlat
                if (clients[0] != null && clients[1] != null) {
                    clients[currentPlayerIndex].sendMessage("MOVE:TURN"); // İlk oyuncuya sıra
                    clients[0].sendMessage("OPPONENT_NAME:" + clients[1].playerName); // Karşılıklı oyuncu isimlerini gönder
                    clients[1].sendMessage("OPPONENT_NAME:" + clients[0].playerName);

                    // Oyun başlatma mesajı gönder
                    clients[0].sendMessage("MOVE:RESTARTED");
                    clients[1].sendMessage("MOVE:RESTARTED");

                    // İlk oyuncuya yeniden sıra ver
                    clients[currentPlayerIndex].sendMessage("MOVE:TURN");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Oyuncularla iletişimi yöneten sınıf
    static class ClientHandler implements Runnable {
        private final Socket socket; // Oyuncuya ait socket
        private BufferedReader in;   // Gelen veriyi okumak için
        private PrintWriter out;     // Oyuncuya mesaj göndermek için
        private String playerName = "Oyuncu"; // Oyuncunun adı
        private final int playerId; // Oyuncunun 0 veya 1 ID'si

        public ClientHandler(Socket socket, int playerId) {
            this.socket = socket;
            this.playerId = playerId;
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Bu oyuncuya mesaj gönderir
        public void sendMessage(String message) {
            out.println(message);
        }

        @Override
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("Oyuncu " + playerId + " mesajı: " + message);

                    // Oyuncu adını aldıysa diğer oyuncuya da bildir
                    if (message.startsWith("NAME:")) {
                        playerName = message.substring(5);
                        System.out.println("Oyuncu " + playerId + " adı güncellendi: " + playerName);
                        int otherId = (playerId == 0) ? 1 : 0;
                        if (clients[otherId] != null) {
                            clients[otherId].sendMessage("OPPONENT_NAME:" + playerName);
                            this.sendMessage("OPPONENT_NAME:" + clients[otherId].playerName);
                        }
                        continue;
                    }

                    // Sıra geçişi mesajı alındığında diğer oyuncuya sırayı ver
                    if (message.equals("MOVE:TURN")) {
                        do {
                            currentPlayerIndex = (currentPlayerIndex + 1) % 2;
                        } while (clients[currentPlayerIndex] == null);
                        clients[currentPlayerIndex].sendMessage("MOVE:TURN");
                        continue;
                    }

                    // Oyun yeniden başlatıldı mesajı geldiğinde her iki oyuncuya bildir
                    if (message.equals("MOVE:RESTARTED")) {
                        System.out.println("Oyuncu " + playerId + " yeniden başlatma talebi gönderdi.");
                        clients[0].sendMessage("MOVE:RESTARTED");
                        clients[1].sendMessage("MOVE:RESTARTED");
                        clients[currentPlayerIndex].sendMessage("MOVE:TURN");
                    }

                    // Oyuncu çıkmak istiyorsa bağlantıyı kapat
                    if (message.equals("MOVE:DISCONNECT")) {
                        out.println("MOVE:DISCONNECT");
                        break;
                    }

                    // Gelen mesajı diğer oyuncuya ilet
                    for (int i = 0; i < 2; i++) {
                        if (clients[i] != null && clients[i] != this) {
                            clients[i].sendMessage(message);
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("Oyuncu " + playerId + " bağlantı hatası: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Oyuncu bağlantısı koptuğunda diziden sil
                clients[playerId] = null;

                // Diğer oyuncuya bağlantı koptuğunu bildir
                for (ClientHandler client : clients) {
                    if (client != null) {
                        client.sendMessage("DISCONNECT:Oyuncu bağlantısı koptu.");
                    }
                }
            }
        }
    }
}

