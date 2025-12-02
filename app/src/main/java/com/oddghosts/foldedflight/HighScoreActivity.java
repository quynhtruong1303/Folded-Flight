package com.oddghosts.foldedflight;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.oddghosts.foldedflight.ui.PixelButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HighScoreActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "FoldedFlightPrefs";
    private static final String HIGH_SCORES_KEY = "high_scores";
    private static final String MOST_COINS_KEY = "most_coins";
    private static final int MAX_HIGH_SCORES = 5;

    private TextView highScoreTitle;
    private TextView highScore1, highScore2, highScore3, highScore4, highScore5;
    private TextView mostCoinsText;
    private PixelButton backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_high_score);

        // Initialize views
        highScoreTitle = findViewById(R.id.highScoreTitle);
        highScore1 = findViewById(R.id.highScore1);
        highScore2 = findViewById(R.id.highScore2);
        highScore3 = findViewById(R.id.highScore3);
        highScore4 = findViewById(R.id.highScore4);
        highScore5 = findViewById(R.id.highScore5);
        mostCoinsText = findViewById(R.id.mostCoinsText);
        backButton = findViewById(R.id.backButton);

        // Setup back button
        setupBackButton();

        // Load and display high scores
        loadHighScores();

        // Load and display most coins
        loadMostCoins();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupBackButton() {
        backButton.setCustomFont(R.font.pixelboy);
        backButton.setCustomTextSize(56f);
        backButton.setButtonBackgroundColor(0xFF9E9E9E);

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void loadHighScores() {
        List<Integer> scores = getHighScoresFromPrefs();

        TextView[] scoreViews = {highScore1, highScore2, highScore3, highScore4, highScore5};

        // Display top score in title
        if (!scores.isEmpty()) {
            highScoreTitle.setText("Top Score: " + formatDistance(scores.get(0)));
        } else {
            highScoreTitle.setText("High Scores");
        }

        // Display all scores in the list
        for (int i = 0; i < MAX_HIGH_SCORES; i++) {
            if (i < scores.size()) {
                scoreViews[i].setText((i + 1) + ".) " + formatDistance(scores.get(i)));
            } else {
                scoreViews[i].setText((i + 1) + ".) ---");
            }
        }
    }

    /**
     * Format distance in meters
     */
    private String formatDistance(int distance) {
        return distance + "m";
    }

    /**
     * Get high scores from SharedPreferences
     */
    private List<Integer> getHighScoresFromPrefs() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Set<String> scoresSet = prefs.getStringSet(HIGH_SCORES_KEY, new HashSet<>());

        List<Integer> scores = new ArrayList<>();
        for (String scoreStr : scoresSet) {
            try {
                scores.add(Integer.parseInt(scoreStr));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        // Sort in descending order
        Collections.sort(scores, Collections.reverseOrder());

        return scores;
    }

    /**
     * Save a new score (static method to be called from GameplayActivity)
     */
    public static void saveScore(android.content.Context context, int score) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Set<String> scoresSet = new HashSet<>(prefs.getStringSet(HIGH_SCORES_KEY, new HashSet<>()));

        // Add the new score
        scoresSet.add(String.valueOf(score));

        // Convert to list and sort
        List<Integer> scores = new ArrayList<>();
        for (String scoreStr : scoresSet) {
            try {
                scores.add(Integer.parseInt(scoreStr));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        Collections.sort(scores, Collections.reverseOrder());

        // Keep only top 5
        while (scores.size() > MAX_HIGH_SCORES) {
            scores.remove(scores.size() - 1);
        }

        // Save back to preferences
        Set<String> newScoresSet = new HashSet<>();
        for (Integer s : scores) {
            newScoresSet.add(String.valueOf(s));
        }

        prefs.edit().putStringSet(HIGH_SCORES_KEY, newScoresSet).apply();
    }

    /**
     * Check if a score qualifies as a high score
     */
    public static boolean isHighScore(android.content.Context context, int score) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Set<String> scoresSet = prefs.getStringSet(HIGH_SCORES_KEY, new HashSet<>());

        if (scoresSet.size() < MAX_HIGH_SCORES) {
            return true; // Less than 5 scores, so any score qualifies
        }

        List<Integer> scores = new ArrayList<>();
        for (String scoreStr : scoresSet) {
            try {
                scores.add(Integer.parseInt(scoreStr));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        Collections.sort(scores);

        // Check if new score is higher than the lowest high score
        return scores.isEmpty() || score > scores.get(0);
    }

    /**
     * Load and display the most coins collected
     */
    private void loadMostCoins() {
        int mostCoins = getMostCoinsFromPrefs();
        mostCoinsText.setText("Most Coins: " + mostCoins);
    }

    /**
     * Get most coins from SharedPreferences
     */
    private int getMostCoinsFromPrefs() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getInt(MOST_COINS_KEY, 0);
    }

    /**
     * Save coins if it's a new record (static method to be called from GameplayActivity)
     */
    public static void saveMostCoins(android.content.Context context, int coins) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int currentMost = prefs.getInt(MOST_COINS_KEY, 0);

        if (coins > currentMost) {
            prefs.edit().putInt(MOST_COINS_KEY, coins).apply();
        }
    }

    /**
     * Get the current most coins record (static method)
     */
    public static int getMostCoins(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getInt(MOST_COINS_KEY, 0);
    }
}