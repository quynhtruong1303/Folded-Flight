package com.oddghosts.foldedflight;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.oddghosts.foldedflight.ui.PixelCard;
import com.oddghosts.foldedflight.ui.PixelButton;

public class GameSettingsActivity extends AppCompatActivity {

    // Cards
    private PixelCard cityCard;
    private PixelCard forestCard;
    private PixelCard planeWhite;
    private PixelCard planeRed;
    private PixelCard planeBlue;
    private PixelCard planeYellow;
    private PixelCard difficultyNormal, difficultyHard;

    // Buttons
    private PixelButton startGameButton;
    private PixelButton backButton;

    // Selected settings
    private String selectedMap = "CITY";
    private String selectedPlaneColor = "WHITE";
    private String selectedDifficulty = "NORMAL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_settings);

        // Initialize all views
        cityCard = findViewById(R.id.cityCard);
        forestCard = findViewById(R.id.forestCard);
        planeWhite = findViewById(R.id.planeWhite);
        planeRed = findViewById(R.id.planeRed);
        planeBlue = findViewById(R.id.planeBlue);
        planeYellow = findViewById(R.id.planeYellow);
        difficultyNormal = findViewById(R.id.difficultyNormal);
        difficultyHard = findViewById(R.id.difficultyHard);
        startGameButton = findViewById(R.id.startGameButton);
        backButton = findViewById(R.id.backButton);

        // Setup map cards
        setupMapCards();

        // Setup plane cards
        setupPlaneCards();

        // Setup difficulty
        setupDifficultyCards();

        // Setup buttons
        setupButtons();
    }

    private void setupMapCards() {
        // Load your map images from drawable
        cityCard.setImage(R.drawable.city_map);
        forestCard.setImage(R.drawable.forest_map);

        // Set city as default selection
        cityCard.setSelected(true);

        // Click listeners
        cityCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cityCard.setSelected(true);
                forestCard.setSelected(false);
                selectedMap = "CITY";
                Toast.makeText(GameSettingsActivity.this, "City Map Selected", Toast.LENGTH_SHORT).show();
            }
        });

        forestCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                forestCard.setSelected(true);
                cityCard.setSelected(false);
                selectedMap = "FOREST";
                Toast.makeText(GameSettingsActivity.this, "Forest Map Selected", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupPlaneCards() {
        // Load plane color images from drawable
        planeWhite.setImage(R.drawable.white_plane);
        planeRed.setImage(R.drawable.red_plane);
        planeBlue.setImage(R.drawable.blue_plane);
        planeYellow.setImage(R.drawable.yellow_plane);

        // Set white as default selection
        planeWhite.setSelected(true);

        // Click listeners
        planeWhite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectPlane(planeWhite, "WHITE");
            }
        });

        planeRed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectPlane(planeRed, "RED");
            }
        });

        planeBlue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectPlane(planeBlue, "BLUE");
            }
        });

        planeYellow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectPlane(planeYellow, "YELLOW");
            }
        });
    }

    private void selectPlane(PixelCard selectedCard, String color) {
        // Deselect all planes
        planeWhite.setSelected(false);
        planeRed.setSelected(false);
        planeBlue.setSelected(false);
        planeYellow.setSelected(false);

        // Select the clicked one
        selectedCard.setSelected(true);
        selectedPlaneColor = color;

        Toast.makeText(this, color + " Plane Selected", Toast.LENGTH_SHORT).show();
    }

    private void setupDifficultyCards() {
        // load images
        difficultyNormal.setImage(R.drawable.normal_text);
        difficultyHard.setImage(R.drawable.hard_text);

        // Set background color to green
        difficultyNormal.setBackgroundColor(0xFF8FD9FB);
        difficultyHard.setBackgroundColor(0xFF8FD9FB);

        // Normal difficulty by default
        difficultyNormal.setSelected(true);

        // Click listeners
        difficultyNormal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectDifficulty(difficultyNormal, "NORMAL");
            }
        });

        difficultyHard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectDifficulty(difficultyHard, "HARD");
            }
        });
    }

    private void selectDifficulty(PixelCard selectedCard, String difficulty) {
        // Deselect all difficulties
        difficultyNormal.setSelected(false);
        difficultyHard.setSelected(false);

        // Select the clicked one
        selectedCard.setSelected(true);
        selectedDifficulty = difficulty;

        Toast.makeText(this, difficulty + " Difficulty Selected", Toast.LENGTH_SHORT).show();
    }

    private void setupButtons() {
        // Set custom font on buttons
        startGameButton.setCustomFont(R.font.pixelboy);
        backButton.setCustomFont(R.font.pixelboy);

        // Set text size
        startGameButton.setCustomTextSize(64f);
        backButton.setCustomTextSize(56f);

        // Customize button colors
        startGameButton.setButtonBackgroundColor(0xFF4A90E2); // Green
        backButton.setButtonBackgroundColor(0xFF9E9E9E);      // Gray

        // Start game button click
        startGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGame();
            }
        });

        // Back button click
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Go back to main menu
            }
        });
    }

    /**
     * Start the game with selected settings
     */
    private void startGame() {
        // TODO: Create GameActivity and start it with the selected settings
        Toast.makeText(this,
                "Starting Game!\nMap: " + selectedMap + "\nPlane: " + selectedPlaneColor,
                Toast.LENGTH_LONG).show();

        // Example of how to pass data to game activity:
        // Intent intent = new Intent(this, GameActivity.class);
        // intent.putExtra("MAP", selectedMap);
        // intent.putExtra("PLANE_COLOR", selectedPlaneColor);
        // startActivity(intent);

        // For now, just go back to main menu after showing toast
        // Remove this later when you have GameActivity
        finish();
    }
}