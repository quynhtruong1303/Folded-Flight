package com.oddghosts.foldedflight;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.oddghosts.foldedflight.ui.PixelButton;

public class MainActivity extends AppCompatActivity {

    // Buttons
    private PixelButton playButton;
    private PixelButton highScoresButton;
    private PixelButton exitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize buttons
        playButton = findViewById(R.id.playButton);
        highScoresButton = findViewById(R.id.highScoresButton);
        exitButton = findViewById(R.id.exitButton);

        // Setup buttons
        setupButtons();
    }

    private void setupButtons() {
        // Set custom font on all buttons
        playButton.setCustomFont(R.font.pixelboy);
        highScoresButton.setCustomFont(R.font.pixelboy);
        exitButton.setCustomFont(R.font.pixelboy);

        // Set text size
        playButton.setCustomTextSize(60f);
        highScoresButton.setCustomTextSize(60f);
        exitButton.setCustomTextSize(60f);

        // Customize button colors to blue
        playButton.setButtonBackgroundColor(0xFF4A90E2);
        highScoresButton.setButtonBackgroundColor(0xFF4A90E2);
        exitButton.setButtonBackgroundColor(0xFF4A90E2);

        // Start button click - Go to game settings
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, GameSettingsActivity.class);
                startActivity(intent);
            }
        });

        // High Scores button click
        highScoresButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "High Scores", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, HighScoreActivity.class);
                startActivity(intent);
            }
        });

        // Exit button click
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Close the app
            }
        });
    }
}