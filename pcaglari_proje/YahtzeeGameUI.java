/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.pcaglari_proje;

/**
 *
 * @author Ebrar Yıldız
 */
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import static javax.swing.WindowConstants.EXIT_ON_CLOSE;
import javax.swing.table.DefaultTableCellRenderer;

public class YahtzeeGameUI extends JFrame {

    private JButton[] diceButtons = new JButton[5];
    private boolean[] heldDice = new boolean[5];
    private int[] diceValues = new int[5];
    private JButton rollButton;
    private int rollCount = 0;
    private YahtzeeClient client;
    private JTable scoreTable;
    private DefaultTableModel tableModel;
    private boolean[] myCategoryLocked = new boolean[13];
    private boolean[] opponentCategoryLocked = new boolean[13];
    private String playerName;
    private String opponentName = "Rakip";
    private boolean myTurn = false;
    private JLabel rollInfoLabel;
    private int myTotalScore = 0;
    private int opponentTotalScore = 0;
    private JLabel totalScoreLabel;
    private JButton restartButton;

    private final String[] categories = {
        "Aslar", "İkiler", "Üçler", "Dörtlüler", "Beşler", "Altılılar",
        "Üçlü", "Dörtlü", "Full House", "Küçük Düz", "Büyük Düz", "Yahtzee", "Şans"
    };

    public YahtzeeGameUI(String playerName) {
        this.playerName = playerName;
        this.opponentName = "Bekleniyor...";

        setTitle("Yahtzee - " + playerName + " vs " + opponentName);
        setSize(600, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        Color backgroundColor = new Color(220, 235, 245);  // Açık mavi arka plan
        getContentPane().setBackground(backgroundColor);

        // === Üst Panel ===
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(backgroundColor);
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Zar At Butonu (geliştirilmiş)
        rollButton = new JButton(" ZAR AT");
        rollButton.setFont(new Font("Arial", Font.BOLD, 18));
        rollButton.setBackground(new Color(70, 130, 180));
        rollButton.setForeground(Color.WHITE);
        rollButton.setFocusPainted(false);
        rollButton.setPreferredSize(new Dimension(160, 50));
        rollButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        rollButton.setBorder(BorderFactory.createLineBorder(new Color(30, 60, 90), 2));
        rollButton.setEnabled(false);
        rollButton.addActionListener(e -> {
            try {
                rollDice();
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Sunucuya mesaj gönderilemedi!");
            }
        });

        topPanel.add(rollButton, BorderLayout.WEST);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (client != null) {
                    client.sendMove("MOVE:DISCONNECT");
                    try {
                        client.close(); // Socket'i düzgün kapat
                    } catch (IOException ex) {
                        Logger.getLogger(YahtzeeGameUI.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });

        // Zar hakkı labeli
        rollInfoLabel = new JLabel("Zar hakkı: 0/3");
        rollInfoLabel.setFont(new Font("Arial", Font.BOLD, 16));
        rollInfoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        rollInfoLabel.setForeground(new Color(40, 40, 40));
        topPanel.add(rollInfoLabel, BorderLayout.CENTER);

        // Yeniden Başlat Butonu
        restartButton = new JButton("Yeniden Başlat");
        restartButton.setFont(new Font("Arial", Font.BOLD, 14));
        restartButton.setBackground(new Color(220, 20, 60));
        restartButton.setForeground(Color.WHITE);
        restartButton.setFocusPainted(false);
        restartButton.setVisible(false);
        restartButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        restartButton.addActionListener(e -> {
            client.sendMove("MOVE:RESTARTED");
            restartButton.setEnabled(false);
        });
        topPanel.add(restartButton, BorderLayout.EAST);

        // Skor etiketi
        totalScoreLabel = new JLabel("Ben: 0  Rakip: 0");
        totalScoreLabel.setFont(new Font("Arial", Font.BOLD, 16));
        totalScoreLabel.setHorizontalAlignment(SwingConstants.CENTER);
        totalScoreLabel.setForeground(new Color(20, 20, 20));
        topPanel.add(totalScoreLabel, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);

        // === Zar Paneli ===
        JPanel dicePanel = new JPanel(new GridLayout(1, 5, 10, 10));
        dicePanel.setBackground(backgroundColor);
        dicePanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        for (int i = 0; i < 5; i++) {
            final int index = i;
            diceButtons[i] = new JButton("?");
            diceButtons[i].setFont(new Font("Arial", Font.BOLD, 28));
            diceButtons[i].setFocusPainted(false);
            diceButtons[i].setBackground(Color.WHITE);
            diceButtons[i].setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
            diceButtons[i].setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            diceButtons[i].addActionListener(e -> toggleHold(index));
            dicePanel.add(diceButtons[i]);
        }
        add(dicePanel, BorderLayout.CENTER);

        // === Skor Tablosu ===
        String[] columnNames = {"Kategori", "Ben", "Rakip"};
        Object[][] data = new Object[13][3];
        for (int i = 0; i < 13; i++) {
            data[i][0] = categories[i];
            data[i][1] = "-";
            data[i][2] = "-";
        }
        tableModel = new DefaultTableModel(data, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return myTurn && rollCount > 0 && column == 1 && !myCategoryLocked[row];
            }
        };

        scoreTable = new JTable(tableModel);
        scoreTable.setFont(new Font("SansSerif", Font.PLAIN, 14));
        scoreTable.setRowHeight(30);
        scoreTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 14));
        scoreTable.getTableHeader().setBackground(new Color(200, 200, 200));

        scoreTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (column == 1) {
                    c.setBackground(myCategoryLocked[row] ? new Color(144, 238, 144) : Color.WHITE);
                } else if (column == 2) {
                    c.setBackground(opponentCategoryLocked[row] ? new Color(255, 182, 193) : Color.WHITE);
                } else {
                    c.setBackground(backgroundColor);
                }
                setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        });

        scoreTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!myTurn || rollCount == 0) {
                    return;
                }
                int row = scoreTable.getSelectedRow();
                if (!myCategoryLocked[row]) {
                    scoreCategory(row);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(scoreTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Skor Tablosu"));
        add(scrollPane, BorderLayout.SOUTH);

        setVisible(true);
    }

    // Sunucudan gelen hareketleri işler
    public void processIncomingMove(String move) {
        System.out.println("Gelen mesaj: " + move);
        // Rakibin yaptığı hamledeki skor bilgilerini tabloya işler
        if (move.startsWith("MOVE:SCORE:")) {
            String[] parts = move.split(":");
            String category = parts[2];
            int score = Integer.parseInt(parts[3]);
            int row = Arrays.asList(categories).indexOf(category);
            tableModel.setValueAt(score, row, 2);
            opponentCategoryLocked[row] = true;
            scoreTable.repaint();
            updateOpponentTotalScore();

            checkGameEnd();// Oyun bitti mi kontrol et
            // Rakip oyuncunun adını günceller
        } else if (move.startsWith("OPPONENT_NAME:")) {
            setOpponentName(move.substring(14));
        }

    }

    // Oyunu yeniden başlatır ve sunucuya bilgi gönderir
    public void resetGame() {
        resetGame(true); // Varsayılan olarak sunucuya mesaj gönder
    }

    // Oyunu sıfırlar; zarları, skorları, butonları ve durumu temizler
    public void resetGame(boolean sendRestartMessage) {
        for (int i = 0; i < categories.length; i++) {
            myCategoryLocked[i] = false;
            opponentCategoryLocked[i] = false;
            tableModel.setValueAt("-", i, 1);
            tableModel.setValueAt("-", i, 2);
        }
        myTotalScore = 0;
        opponentTotalScore = 0;
        updateTotalScoreLabel();

        Arrays.fill(diceValues, 0);
        Arrays.fill(heldDice, false);
        for (JButton btn : diceButtons) {
            btn.setText("?");
            btn.setBackground(null);
        }

        rollCount = 0;
        updateRollInfo();

        restartButton.setVisible(false);
        setMyTurn(false); // Oyuncunun sırasını pasif yapar

        if (sendRestartMessage) {
            client.sendMove("MOVE:RESTARTED");
        }
    }

    // Belirli bir kategoriye skor ekler ve turu sonlandırır
    private void scoreCategory(int row) {
        int score = calculateScore(row);
        tableModel.setValueAt(score, row, 1);
        myCategoryLocked[row] = true;
        updateMyTotalScore();

        // Geçici skorları temizle
        for (int i = 0; i < categories.length; i++) {
            if (!myCategoryLocked[i]) {
                tableModel.setValueAt("-", i, 1);
            }
        }
        scoreTable.repaint();
        client.sendMove("MOVE:SCORE:" + categories[row] + ":" + score);
        client.sendMove("MOVE:TURN");
        endTurn();
        checkGameEnd(); // Oyun bitti mi kontrol et
    }

    // Verilen kategoriye göre zar skorunu hesaplar
    private int calculateScore(int category) {
        return ScoreCalculator.calculateScoreForCategory(diceValues, category);
    }

    // Henüz kilitlenmemiş kategorilere geçici skor tahminlerini hesaplayıp gösterir
    private void updatePossibleScores() {
        Map<String, Integer> scores = ScoreCalculator.calculatePossibleScores(diceValues);
        for (int i = 0; i < categories.length; i++) {
            if (!myCategoryLocked[i]) {
                tableModel.setValueAt(scores.get(categories[i]), i, 1);
            }
        }
    }

// Oyuncunun zar atma işlemini gerçekleştirir
    private void rollDice() throws IOException {
        if (!myTurn) {
            JOptionPane.showMessageDialog(this, "Sıra sende değil!");
            return;
        }

        if (rollCount >= 3) {
            JOptionPane.showMessageDialog(this, "3 kere zar attınız, kategori seçin.");
            return;
        }

        Random rand = new Random();
        for (int i = 0; i < 5; i++) {
            if (!heldDice[i]) {
                diceValues[i] = rand.nextInt(6) + 1;
                diceButtons[i].setText(String.valueOf(diceValues[i]));
            }
        }

        rollCount++;
        updateRollInfo();
        client.sendMove("MOVE:ROLL:" + Arrays.toString(diceValues));
        updatePossibleScores();

        if (rollCount == 3) {
            JOptionPane.showMessageDialog(this, "Zar atış hakkınız doldu, kategori seçin.");
        }
    }

// Oyuncunun tüm kategorileri seçip seçmediğini kontrol eder
    private boolean isPlayerFinished() {
        for (boolean locked : myCategoryLocked) {
            if (!locked) {
                return false;
            }
        }
        return true;
    }

// Rakibin tüm kategorileri seçip seçmediğini kontrol eder
    private boolean isOpponentFinished() {
        for (boolean locked : opponentCategoryLocked) {
            if (!locked) {
                return false;
            }
        }
        return true;
    }

// Her iki oyuncu da bitirdiyse sonucu kontrol eder ve kazananı ilan eder
    private void checkGameEnd() {
        if (isPlayerFinished() && isOpponentFinished()) {
            String message;
            if (myTotalScore > opponentTotalScore) {
                message = "Oyun bitti! Kazanan: " + playerName + " (" + myTotalScore + " - " + opponentTotalScore + ")";
            } else if (myTotalScore < opponentTotalScore) {
                message = "Oyun bitti! Kazanan: " + opponentName + " (" + opponentTotalScore + " - " + myTotalScore + ")";
            } else {
                message = "Oyun bitti! Berabere! (" + myTotalScore + " - " + opponentTotalScore + ")";
            }
            JOptionPane.showMessageDialog(this, message);
            restartButton.setVisible(true);
            rollButton.setEnabled(false);
        }
    }

// Oyuncunun toplam skorunu günceller
    private void updateMyTotalScore() {
        myTotalScore = 0;
        for (int i = 0; i < categories.length; i++) {
            if (myCategoryLocked[i]) {
                Object value = tableModel.getValueAt(i, 1);
                if (value instanceof Integer) {
                    myTotalScore += (int) value;
                } else if (value instanceof String && !((String) value).equals("-")) {
                    myTotalScore += Integer.parseInt((String) value);
                }
            }
        }
        updateTotalScoreLabel();
    }

// Oyuncunun toplam skorunu günceller
    private void updateOpponentTotalScore() {
        opponentTotalScore = 0;
        for (int i = 0; i < categories.length; i++) {
            if (opponentCategoryLocked[i]) {
                Object value = tableModel.getValueAt(i, 2);
                if (value instanceof Integer) {
                    opponentTotalScore += (int) value;
                } else if (value instanceof String && !((String) value).equals("-")) {
                    opponentTotalScore += Integer.parseInt((String) value);
                }
            }
        }
        updateTotalScoreLabel();
    }

// Toplam skorları ekranda gösterir
    private void updateTotalScoreLabel() {
        totalScoreLabel.setText(this.playerName + " (ben): " + myTotalScore +"     "+ this.opponentName + " (Rakip):  " + opponentTotalScore);
    }

// Belirtilen zarın tutulup tutulmayacağını değiştirir
    private void toggleHold(int index) {
        if (!myTurn) {
            return;
        }
        heldDice[index] = !heldDice[index];
        diceButtons[index].setBackground(heldDice[index] ? Color.YELLOW : null);
    }

// Tur sonunda yapılması gereken sıfırlamaları yapar
    private void endTurn() {
        Arrays.fill(heldDice, false);
        for (JButton btn : diceButtons) {
            btn.setText("?");
            btn.setBackground(null);
        }
        rollCount = 0;
        updateRollInfo();
        setMyTurn(false);
    }

// Oyuncunun sırasının gelip gelmediğini ayarlar
    public void setMyTurn(boolean isMyTurn) {
        this.myTurn = isMyTurn;
        rollButton.setEnabled(isMyTurn);
        String message = isMyTurn ? "Sıra sende!" : "Rakip oynuyor...";
        rollInfoLabel.setText(message);
    }

// Sunucu bağlantısı kesildiğinde yapılacakları düzenler
    public void handleDisconnect() {
        setMyTurn(false);
        rollButton.setEnabled(false);
    }

// Client nesnesini GUI ile bağlar
    public void setClient(YahtzeeClient client) {
        this.client = client;
    }

// Zar hakkında kalan bilgileri günceller
    private void updateRollInfo() {
        rollInfoLabel.setText("Zar hakkı: " + rollCount + "/3");
    }

// Rakip oyuncunun adını ayarlar ve pencere başlığını günceller
    public void setOpponentName(String name) {
        this.opponentName = name;
        setTitle("Yahtzee - " + playerName + " vs " + opponentName);
    }

// Oyunun bitiminde yeniden başlatma butonunu etkinleştirir
    public void enableRestartButton() {
        restartButton.setVisible(true);
        restartButton.setEnabled(true);
    }
}
