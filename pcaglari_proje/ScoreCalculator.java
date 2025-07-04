/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.pcaglari_proje;

/**
 *
 * @author Ebrar Yıldız
 */

import java.util.*;

public class ScoreCalculator {

    public static Map<String, Integer> calculatePossibleScores(int[] dice) {
        Map<String, Integer> scores = new HashMap<>();
        for (int i = 0; i < 13; i++) {
            scores.put(getCategoryName(i), calculateScoreForCategory(dice, i));
        }
        return scores;
    }

    public static int calculateScoreForCategory(int[] dice, int category) {
        int[] counts = new int[7];
        for (int die : dice) counts[die]++;

        switch (category) {
            case 0: return counts[1] * 1;
            case 1: return counts[2] * 2;
            case 2: return counts[3] * 3;
            case 3: return counts[4] * 4;
            case 4: return counts[5] * 5;
            case 5: return counts[6] * 6;
            case 6:
                for (int i = 1; i <= 6; i++) if (counts[i] >= 3) return sum(dice);
                return 0;
            case 7:
                for (int i = 1; i <= 6; i++) if (counts[i] >= 4) return sum(dice);
                return 0;
            case 8:
                if (Arrays.stream(counts).anyMatch(c -> c == 3) &&
                    Arrays.stream(counts).anyMatch(c -> c == 2)) return 25;
                return 0;
            case 9:
                if (hasStraight(counts, 4)) return 30;
                return 0;
            case 10:
                if (hasStraight(counts, 5)) return 40;
                return 0;
            case 11:
                if (Arrays.stream(counts).anyMatch(c -> c == 5)) return 50;
                return 0;
            case 12:
                return sum(dice);
            default:
                return 0;
        }
    }

    private static boolean hasStraight(int[] counts, int length) {
        int consecutive = 0;
        for (int i = 1; i <= 6; i++) {
            if (counts[i] > 0) {
                consecutive++;
                if (consecutive >= length) return true;
            } else {
                consecutive = 0;
            }
        }
        return false;
    }

    private static int sum(int[] dice) {
        return Arrays.stream(dice).sum();
    }

    private static String getCategoryName(int index) {
        String[] categories = {
            "Aslar", "İkiler", "Üçler", "Dörtlüler", "Beşler", "Altılılar",
            "Üçlü", "Dörtlü", "Full House", "Küçük Düz", "Büyük Düz", "Yahtzee", "Şans"
        };
        return categories[index];
    }
}
