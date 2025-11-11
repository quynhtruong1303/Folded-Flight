package com.oddghosts.foldedflight;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import com.oddghosts.foldedflight.game.GameSurfaceView;
import com.oddghosts.foldedflight.ui.PixelButton;

public class GameplayActivity extends AppCompatActivity {

    // UI Elements
    private TextView timerText;
    private GameSurfaceView gameSurfaceView;

    // Game State
    private boolean isPaused = false;
    private boolean isGameRunning = false;

    // Timer
    private long startTime;
    private long elapsedTime = 0;
    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;

    // Game Settings (from intent)
    private String selectedMap;
    private String selectedPlaneColor;
    private String difficulty;

    // Pause Dialog
    private Dialog pauseDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_gameplay);

        // Get game settings from intent
        selectedMap = getIntent().getStringExtra("MAP");
        if (selectedMap == null) selectedMap = "CITY";

        selectedPlaneColor = getIntent().getStringExtra("PLANE_COLOR");
        if (selectedPlaneColor == null) selectedPlaneColor = "WHITE";

        difficulty = getIntent().getStringExtra("DIFFICULTY");
        if (difficulty == null) difficulty = "NORMAL";

        // Initialize UI
        initializeUI();

        // Setup timer
        setupTimer();

        // Setup pause dialog
        setupPauseDialog();

        // Start the game
        startGame();
    }

    private void initializeUI() {
        timerText = findViewById(R.id.timerText);
        ImageButton pauseButton = findViewById(R.id.pauseButton);
        gameSurfaceView = findViewById(R.id.gameSurfaceView);

        gameSurfaceView.setGameSettings(selectedMap, selectedPlaneColor, difficulty);

        gameSurfaceView.setBackgroundMode(GameSurfaceView.BackgroundMode.SCALE_TO_FIT_HEIGHT);

        // Pause button listener
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pauseGame();
            }
        });
    }

    private void setupTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isPaused && isGameRunning) {
                    long millis = System.currentTimeMillis() - startTime + elapsedTime;
                    int seconds = (int) (millis / 1000);
                    int minutes = seconds / 60;
                    seconds = seconds % 60;

                    timerText.setText(String.format("TIME: %02d:%02d", minutes, seconds));
                    timerHandler.postDelayed(this, 100);
                }
            }
        };
    }

    private void setupPauseDialog() {
        pauseDialog = new Dialog(this);
        pauseDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        pauseDialog.setCancelable(false);

        // Create custom layout for pause dialog
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);
        layout.setBackgroundColor(0xFF2C3E50);

        // Title
        TextView title = new TextView(this);
        title.setText(R.string.paused_text);
        title.setTextSize(48);
        title.setTextColor(Color.WHITE);
        title.setAutoSizeTextTypeUniformWithConfiguration(40, 48, 2, TypedValue.COMPLEX_UNIT_SP);
        title.setTypeface(getResources().getFont(R.font.pixelboy));
        title.setPadding(0, 0, 0, 40);
        layout.addView(title);

        // Resume button
        PixelButton resumeButton = new PixelButton(this);
        resumeButton.setText(R.string.resume_text);
        resumeButton.setCustomFont(R.font.pixelboy);
        resumeButton.setCustomTextSize(56f);
        resumeButton.setButtonBackgroundColor(0xFF4A90E2);
        resumeButton.setLayoutParams(new LinearLayout.LayoutParams(
                300, 100
        ));
        resumeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resumeGame();
            }
        });
        layout.addView(resumeButton);

        // Add spacing
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(1, 30));
        layout.addView(spacer);

        // Exit button
        PixelButton exitButton = new PixelButton(this);
        exitButton.setText(R.string.main_menu_text);
        exitButton.setCustomFont(R.font.pixelboy);
        exitButton.setCustomTextSize(56f);
        exitButton.setButtonBackgroundColor(0xFF9E9E9E);
        exitButton.setLayoutParams(new LinearLayout.LayoutParams(
                300, 100
        ));
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitToMenu();
            }
        });
        layout.addView(exitButton);

        pauseDialog.setContentView(layout);

        // Make dialog background transparent
        if (pauseDialog.getWindow() != null) {
            pauseDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private void startGame() {
        isGameRunning = true;
        startTime = System.currentTimeMillis();
        timerHandler.postDelayed(timerRunnable, 0);
        gameSurfaceView.startGame();
    }

    private void pauseGame() {
        if (!isPaused) {
            isPaused = true;

            // Pause the timer
            elapsedTime += System.currentTimeMillis() - startTime;

            // Pause the game surface
            gameSurfaceView.pauseGame();

            // Show pause dialog
            pauseDialog.show();
        }
    }

    private void resumeGame() {
        isPaused = false;
        startTime = System.currentTimeMillis();

        // Resume timer
        timerHandler.postDelayed(timerRunnable, 0);

        // Resume game surface
        gameSurfaceView.resumeGame();

        // Hide pause dialog
        pauseDialog.dismiss();
    }

    private void exitToMenu() {
        isGameRunning = false;
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isGameRunning && !isPaused) {
            pauseGame();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isGameRunning = false;
        timerHandler.removeCallbacks(timerRunnable);
        if (gameSurfaceView != null) {
            gameSurfaceView.stopGame();
        }
    }

    @Override
    public void onBackPressed() {
        if (!isPaused) {
            pauseGame();
        }
    }
}