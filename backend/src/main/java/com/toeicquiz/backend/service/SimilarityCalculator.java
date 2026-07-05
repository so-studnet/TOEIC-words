package com.toeicquiz.backend.service;

import org.springframework.stereotype.Component;

/**
 * Part1 詳細設計書 §1: 重み付きダメラウ・レーベンシュタイン距離による類似度計算。
 */
@Component
public class SimilarityCalculator {

    private static final double INSERT_DELETE_COST = 1.0;
    private static final double SUBSTITUTION_COST = 1.0;
    private static final double RL_SUBSTITUTION_COST = 0.3;
    private static final double TRANSPOSITION_COST = 0.5;

    public int calculate(String input, String correct) {
        String a = input == null ? "" : input.toLowerCase();
        String b = correct.toLowerCase();
        double distance = weightedDamerauLevenshtein(a, b);
        double ratio = 1.0 - distance / b.length();
        double percent = Math.max(0, ratio) * 100;
        return (int) Math.round(percent);
    }

    private double weightedDamerauLevenshtein(String a, String b) {
        int n = a.length();
        int m = b.length();
        double[][] d = new double[n + 1][m + 1];

        for (int i = 0; i <= n; i++) d[i][0] = i * INSERT_DELETE_COST;
        for (int j = 0; j <= m; j++) d[0][j] = j * INSERT_DELETE_COST;

        for (int i = 1; i <= n; i++) {
            char ca = a.charAt(i - 1);
            for (int j = 1; j <= m; j++) {
                char cb = b.charAt(j - 1);
                double subCost = (ca == cb) ? 0 : substitutionCost(ca, cb);

                double value = Math.min(
                        d[i - 1][j] + INSERT_DELETE_COST,
                        Math.min(
                                d[i][j - 1] + INSERT_DELETE_COST,
                                d[i - 1][j - 1] + subCost
                        )
                );

                if (i > 1 && j > 1 && ca != cb
                        && ca == b.charAt(j - 2) && a.charAt(i - 2) == cb) {
                    value = Math.min(value, d[i - 2][j - 2] + TRANSPOSITION_COST);
                }

                d[i][j] = value;
            }
        }
        return d[n][m];
    }

    private double substitutionCost(char a, char b) {
        if ((a == 'r' && b == 'l') || (a == 'l' && b == 'r')) {
            return RL_SUBSTITUTION_COST;
        }
        return SUBSTITUTION_COST;
    }
}
