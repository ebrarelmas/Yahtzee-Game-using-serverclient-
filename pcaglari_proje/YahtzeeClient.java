/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.pcaglari_proje;

/**
 *
 * @author Ebrar Yıldız
 */

import java.io.*;
import java.net.*;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class YahtzeeClient {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private YahtzeeGameUI gameUI;
    
    //Sunucuya bağlanır.
    public YahtzeeClient(String serverAddress) throws IOException {
        socket = new Socket(serverAddress, 12345);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
    }
//GUI bileşenini ayarlar.
    public void setGameUI(YahtzeeGameUI gameUI) {
        this.gameUI = gameUI;
    }
//Sunucuya bir hamle gönderir.
    public void sendMove(String move) {
        out.println(move);
    }
//Gelen mesaj
    public String readMessage() throws IOException {
        return in.readLine();
    }
//Bağlantıyı kapatır.
    public void close() throws IOException {
        socket.close();
    }
//Sunucudan gelen mesajları dinlemeye başlar.
    public void startListening() {
        new Thread(() -> {
            try {
                String message;
                while ((message = readMessage()) != null) {
                    final String msg = message;

                    SwingUtilities.invokeLater(() -> {
                        if (msg.startsWith("MOVE:TURN")) {
                            
                            if (gameUI != null) {
                                gameUI.setMyTurn(true);
                            }
                        } else if (msg.startsWith("OPPONENT_NAME:")) {
                            
                            String opponentName = msg.substring("OPPONENT_NAME:".length());
                            if (gameUI != null) {
                                gameUI.setOpponentName(opponentName);
                            }
                        } else if (msg.startsWith("MOVE:DISCONNECT")) {
                            
                            JOptionPane.showMessageDialog(gameUI, "Rakip bağlantısı kesildi. Oyun sonlandı.");
                            if (gameUI != null) {
                                gameUI.resetGame(false);
                                gameUI.handleDisconnect();
                            }
                        }   else if (msg.equals("MOVE:RESTARTED")) {
                            
                            // Karşı taraf da sıfırladı
                            if (gameUI != null) {
                                gameUI.resetGame(false);
                            }
                        } else {
                            
                            // Diğer oyun mesajları ve hamleleri GUI'de işle
                            if (gameUI != null) {
                                gameUI.processIncomingMove(msg);
                            }
                        }
                    });
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(gameUI, "Sunucu bağlantısı kesildi.");
                    if (gameUI != null) {
                        gameUI.handleDisconnect();
                    }
                });
            }
        }).start();
    }
    //Oyuncu bağlantısını manuel olarak sonlandırır.
    public void disconnect() {
    try {
        socket.close(); // socket, sunucuyla bağlantıdır
    } catch (IOException e) {
        e.printStackTrace();
    }
}

//Programın giriş noktası. Oyuncudan ad alınır ve sunucuya bağlanılır.
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String playerName = JOptionPane.showInputDialog("Oyuncu adınızı girin:");
            if (playerName != null && !playerName.trim().isEmpty()) {
                try {
                    String serverAddress = "13.53.42.45";
                    YahtzeeGameUI gameUI = new YahtzeeGameUI(playerName);
                    YahtzeeClient client = new YahtzeeClient(serverAddress);
                    client.setGameUI(gameUI);
                    gameUI.setClient(client);

                    // Oyuncu adını sunucuya gönder
                    client.sendMove("NAME:" + playerName);

                    // Sunucudan gelen mesajları dinlemeye başla
                    client.startListening();

                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(null, "Sunucuya bağlanılamadı: " + ex.getMessage());
                }
            }
        });
    }
}