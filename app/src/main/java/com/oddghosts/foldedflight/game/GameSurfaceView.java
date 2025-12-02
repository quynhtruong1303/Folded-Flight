package com.oddghosts.foldedflight.game;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.core.content.res.ResourcesCompat;

import com.oddghosts.foldedflight.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    public interface GameOverListener {
        void onGameOver(int distance, int coins);
    }

    // Thread and running state
    private Thread gameThread;
    private boolean isRunning = false;
    private boolean isPaused = false;
    private boolean isFlying = false;

    // Surface holder
    private SurfaceHolder surfaceHolder;
    private Context context;

    // Game settings
    private String mapType = "CITY";
    private String planeColor = "WHITE";
    private String difficulty = "NORMAL";

    // Physics variables
    private PlanePhysics plane;
    private float liftForce;
    private float forwardThrust;
    private float dragCoefficient;
    private float maxSpeed;

    // Camera
    private float cameraX = 0;

    // Touch controls
    private boolean upPressed = false;
    private boolean downPressed = false;

    // Graphics
    private Bitmap originalBackgroundBitmap;
    private Bitmap scaledBackgroundBitmap;
    private Bitmap planeBitmap;
    private Paint paint;

    // Screen dimensions
    private int screenWidth;
    private int screenHeight;

    // Background dimensions
    private int backgroundWidth;
    private int backgroundHeight;

    // Timing
    private long lastFrameTime;
    private static final int TARGET_FPS = 60;
    private static final long FRAME_TIME = 1000 / TARGET_FPS;

    // Game timer (seconds survived)
    private float survivalTimeSeconds = 0f;

    // Obstacles
    private final List<Obstacle> obstacles = new ArrayList<>();
    private long lastObstacleSpawnTime = 0L;
    private long obstacleSpawnDelayMs = 2500L; // Time between obstacle spawns
    private float obstacleSpeed = -300f; // Negative = moves left

    // Obstacle sprites by map
    private ObstacleSet cityObstacles;
    private ObstacleSet forestObstacles;

    // Coins
    private final List<Coin> coins = new ArrayList<>();
    private long lastCoinSpawnTime = 0L;
    private long coinSpawnDelayMs = 1500L;
    private Bitmap coinSprite;
    private int coinCount = 0;

    // Random generator
    private Random random = new Random();

    // Game over flag
    private boolean isGameOver = false;

    // Callback for game over event
    private GameOverListener gameOverListener;

    // Obstacle definition class
    private static class ObstacleDefinition {
        String name;
        boolean isGrounded; // If true, sits on ground
        float heightRatio; // Height as fraction of screen
        int resourceId;

        ObstacleDefinition(String name, boolean isGrounded, float heightRatio, int resourceId) {
            this.name = name;
            this.isGrounded = isGrounded;
            this.heightRatio = heightRatio;
            this.resourceId = resourceId;
        }
    }

    // Obstacle set for each map
    private static class ObstacleSet {
        List<ObstacleDefinition> obstacles = new ArrayList<>();

        void add(String name, boolean isGrounded, float heightRatio, int resourceId) {
            obstacles.add(new ObstacleDefinition(name, isGrounded, heightRatio, resourceId));
        }

        ObstacleDefinition getRandom(Random random) {
            if (obstacles.isEmpty()) return null;
            return obstacles.get(random.nextInt(obstacles.size()));
        }
    }

    public GameSurfaceView(Context context) {
        super(context);
        init(context);
    }

    public GameSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public GameSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);

        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(false); // Keep pixel art sharp

        this.context = context;
        setFocusable(true);
    }

    public void setGameSettings(String map, String planeColor, String difficulty) {
        this.mapType = map;
        this.planeColor = planeColor;
        this.difficulty = difficulty;

        // Set difficulty parameters
        if (difficulty.equals("HARD")) {
            liftForce = 2000.0f;
            forwardThrust = 200.0f;
            dragCoefficient = 0.008f;
            maxSpeed = 2000.0f;
            obstacleSpawnDelayMs = 1500L; // Faster spawn = ~1.5 obstacles visible at once
        } else {
            liftForce = 1500.0f;
            forwardThrust = 100.0f;
            dragCoefficient = 0.005f;
            maxSpeed = 1200.0f;
            obstacleSpawnDelayMs = 2500L; // Normal: one obstacle at a time
        }

        // Apply max speed to plane if it exists
        if (plane != null) {
            plane.setMaxSpeed(maxSpeed);
            plane.setDragCoefficient(dragCoefficient);
        }
    }

    /**
     * Public methods to control plane movement from buttons
     */
    public void setUpPressed(boolean pressed) {
        if (isFlying && !isGameOver) {
            upPressed = pressed;
            if (pressed) {
                downPressed = false;
            }
        }
    }

    public void setDownPressed(boolean pressed) {
        if (isFlying && !isGameOver) {
            downPressed = pressed;
            if (pressed) {
                upPressed = false;
            }
        }
    }

    private void loadResources() {
        // Load background based on map type
        if (mapType.equals("CITY")) {
            originalBackgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.city_map);
        } else {
            originalBackgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.forest_map);
        }

        // Scale background to fit screen height
        scaleBackgroundToFitHeight();

        // Load plane sprite based on color
        int planeResId = R.drawable.white_plane;
        switch (planeColor) {
            case "RED":
                planeResId = R.drawable.red_plane;
                break;
            case "BLUE":
                planeResId = R.drawable.blue_plane;
                break;
            case "YELLOW":
                planeResId = R.drawable.yellow_plane;
                break;
        }
        planeBitmap = BitmapFactory.decodeResource(getResources(), planeResId);

        // Scale plane to appropriate size
        if (planeBitmap != null) {
            planeBitmap = Bitmap.createScaledBitmap(planeBitmap, 192, 192, false);
        }

        // Load obstacle sprites
        loadObstacleSprites();

        // Load coin sprite
        try {
            coinSprite = BitmapFactory.decodeResource(getResources(), R.drawable.coin);
        } catch (Exception e) {
            // Coin sprite not found, coins will be disabled
            coinSprite = null;
        }
    }

    /**
     * Load obstacle sprites based on map type with proper sizing requirements
     *
     * City Map (5 obstacles):
     * - Building: ground, 50% screen height (very tall obstacle)
     * - Jet, Alien: top half, max 1/3 screen
     * - Bird: top half, 1/5 screen
     * - Lamppost: ground, 30% screen
     *
     * Forest Map:
     * - Tree: like building (ground, 85% screen)
     * - Ghost_1, Ghost_2: like jet/alien (top half, max 1/3 screen)
     * - Zombie: like lamppost (ground, 30% screen)
     */
    private void loadObstacleSprites() {
        cityObstacles = new ObstacleSet();
        forestObstacles = new ObstacleSet();

        // City Map Obstacles
        // Note: Update these resource IDs to match your actual drawable names
        try {
            // Building - ground, 85% of screen
            cityObstacles.add("building", true, 0.50f, R.drawable.building);

            // Lamppost - ground, 1/4 screen
            cityObstacles.add("lamppost", true, 0.30f, R.drawable.lamp_post);

            // Flying obstacles - top half
            // Jet and Alien: max 1/3 screen, Bird: 1/5 screen
            try { cityObstacles.add("jet", false, 0.3f, R.drawable.jet); } catch (Exception e) {}
            try { cityObstacles.add("alien", false, 0.3f, R.drawable.alien); } catch (Exception e) {}
            try { cityObstacles.add("bird", false, 0.2f, R.drawable.bird); } catch (Exception e) {}
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Forest Map Obstacles
        try {
            // Tree - ground, 85% of screen (like building)
            try { forestObstacles.add("tree", true, 0.50f, R.drawable.tree); } catch (Exception e) {}

            // Zombie - ground, 1/4 screen (like lamppost)
            forestObstacles.add("zombie", true, 0.30f, R.drawable.zombie);

            // Flying obstacles - top half, max 1/3 screen
            try { forestObstacles.add("ghost_1", false, 0.3f, R.drawable.ghost_1); } catch (Exception e) {}
            try { forestObstacles.add("ghost_2", false, 0.3f, R.drawable.ghost_2); } catch (Exception e) {}
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Scale background to match screen height, maintain aspect ratio
     */
    private void scaleBackgroundToFitHeight() {
        if (originalBackgroundBitmap == null || screenHeight == 0 || screenWidth == 0) {
            return;
        }

        float scale = (float) screenHeight / originalBackgroundBitmap.getHeight();
        backgroundWidth = (int) (originalBackgroundBitmap.getWidth() * scale);
        backgroundHeight = screenHeight;

        scaledBackgroundBitmap = Bitmap.createScaledBitmap(
                originalBackgroundBitmap,
                backgroundWidth,
                backgroundHeight,
                false
        );
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        screenWidth = getWidth();
        screenHeight = getHeight();

        loadResources();

        // Initialize plane physics
        plane = new PlanePhysics(100, screenHeight / 2.0f, 32, 1.0f);
        // Set extremely large world width for infinite scrolling
        plane.setWorldBounds(Float.MAX_VALUE, screenHeight);
        plane.setDragCoefficient(dragCoefficient);
        plane.setMaxSpeed(maxSpeed);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        screenWidth = width;
        screenHeight = height;

        scaleBackgroundToFitHeight();

        if (plane != null) {
            plane.setWorldBounds(Float.MAX_VALUE, height);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopGame();
    }

    public void startGame() {
        isRunning = true;
        isPaused = false;
        lastObstacleSpawnTime = System.currentTimeMillis();
        lastCoinSpawnTime = System.currentTimeMillis();
        gameThread = new Thread(this);
        gameThread.start();
        lastFrameTime = System.nanoTime();
    }

    public void pauseGame() {
        isPaused = true;
    }

    public void resumeGame() {
        isPaused = false;
        lastFrameTime = System.nanoTime();
    }

    public void stopGame() {
        isRunning = false;
        try {
            if (gameThread != null) {
                gameThread.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reset the game to initial state
     */
    public void resetGame() {
        isFlying = false;
        isGameOver = false;
        cameraX = 0;
        upPressed = false;
        downPressed = false;
        coinCount = 0;
        survivalTimeSeconds = 0f;

        obstacles.clear();
        lastObstacleSpawnTime = System.currentTimeMillis();

        coins.clear();
        lastCoinSpawnTime = System.currentTimeMillis();

        if (plane != null) {
            plane.reset(100, screenHeight / 2.0f);
        }

        lastFrameTime = System.nanoTime();
    }

    @Override
    public void run() {
        while (isRunning) {
            if (!isPaused) {
                long currentTime = System.nanoTime();
                float deltaTime = (currentTime - lastFrameTime) / 1000000000.0f;
                lastFrameTime = currentTime;

                // Limit delta time
                if (deltaTime > 0.05f) deltaTime = 0.05f;

                update(deltaTime);
                draw();

                // Frame rate limiting
                long frameTime = System.nanoTime() - currentTime;
                long sleepTime = FRAME_TIME - (frameTime / 1000000);
                if (sleepTime > 0) {
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void update(float deltaTime) {
        if (!isFlying || plane == null || isGameOver) return;

        // Update survival time
        survivalTimeSeconds += deltaTime;

        // Apply forward thrust
        plane.applyForce(forwardThrust, 0);

        // Apply lift/dive forces
        if (upPressed) {
            plane.applyForce(0, -liftForce);
        }
        if (downPressed) {
            plane.applyForce(0, liftForce * 0.5f);
        }

        // Update physics
        plane.update(deltaTime);

        // Update camera to follow plane
        cameraX = plane.getX() - screenWidth * 0.25f;
        if (cameraX < 0) cameraX = 0;

        // Update obstacles
        updateObstacles(deltaTime);

        // Update coins
        if (coinSprite != null) {
            updateCoins(deltaTime);
        }

        // Check collisions
        checkCollisions();
        if (coinSprite != null) {
            checkCoinCollisions();
        }
    }

    private void draw() {
        if (surfaceHolder.getSurface().isValid()) {
            Canvas canvas = surfaceHolder.lockCanvas();
            if (canvas != null) {
                // Clear canvas with sky color
                canvas.drawColor(Color.rgb(135, 206, 250));

                // Draw scrolling background
                drawScrollingBackground(canvas);

                // Draw obstacles
                drawObstacles(canvas);

                // Draw coins
                if (coinSprite != null) {
                    drawCoins(canvas);
                }

                // Draw instructions if not flying
                if (!isFlying) {
                    drawStartInstructions(canvas);
                }

                // Draw plane if flying
                if (isFlying && plane != null && planeBitmap != null) {
                    drawPlane(canvas);
                }

                // Draw game over screen
                if (isGameOver) {
                    drawGameOver(canvas);
                }

                // Draw debug info
                if (isFlying && plane != null && !isGameOver) {
                    drawDebugInfo(canvas);
                }

                // Draw coin counter
                if (isFlying && coinSprite != null && !isGameOver) {
                    drawCoinCounter(canvas);
                }

                surfaceHolder.unlockCanvasAndPost(canvas);
            }
        }
    }

    /**
     * Draw scrolling background with infinite tiling
     */
    private void drawScrollingBackground(Canvas canvas) {
        if (scaledBackgroundBitmap == null) return;

        // Parallax scrolling (slower than camera movement)
        float drawX = -cameraX * 0.5f;

        // Use modulo to create seamless infinite tiling
        drawX = drawX % backgroundWidth;
        if (drawX > 0) drawX -= backgroundWidth;

        // Draw enough copies to fill the screen
        float currentX = drawX;
        while (currentX < screenWidth) {
            canvas.drawBitmap(scaledBackgroundBitmap, currentX, 0, paint);
            currentX += backgroundWidth;
        }
    }

    private void drawStartInstructions(Canvas canvas) {
        Typeface customTypeface = ResourcesCompat.getFont(context, R.font.pixelboy);

        // Stroke paint (black outline)
        Paint strokePaint = new Paint();
        strokePaint.setColor(Color.BLACK);
        strokePaint.setTextSize(80);
        strokePaint.setTextAlign(Paint.Align.CENTER);
        strokePaint.setTypeface(customTypeface);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(8);

        // Fill paint (white text)
        Paint fillPaint = new Paint();
        fillPaint.setColor(Color.WHITE);
        fillPaint.setTextSize(80);
        fillPaint.setTextAlign(Paint.Align.CENTER);
        fillPaint.setTypeface(customTypeface);

        // Main title
        canvas.drawText("TAP TO LAUNCH!", screenWidth / 2, screenHeight / 2, strokePaint);
        canvas.drawText("TAP TO LAUNCH!", screenWidth / 2, screenHeight / 2, fillPaint);

        // Subtext
        strokePaint.setTextSize(60);
        fillPaint.setTextSize(60);

        canvas.drawText("Use buttons to climb/dive", screenWidth / 2, screenHeight / 2 + 80, strokePaint);
        canvas.drawText("Use buttons to climb/dive", screenWidth / 2, screenHeight / 2 + 80, fillPaint);
    }

    private void drawPlane(Canvas canvas) {
        float drawX = plane.getX() - cameraX - planeBitmap.getWidth() / 2;
        float drawY = plane.getY() - planeBitmap.getHeight() / 2;

        canvas.save();

        // Calculate rotation based on velocity
        float angle = 0;
        if (Math.abs(plane.getVelocityY()) > 10) {
            angle = (float) Math.toDegrees(Math.atan2(plane.getVelocityY(), plane.getVelocityX()));
            angle = Math.max(-30, Math.min(30, angle));
        }

        // Rotate canvas around plane center
        canvas.rotate(angle, drawX + planeBitmap.getWidth() / 2, drawY + planeBitmap.getHeight() / 2);
        canvas.drawBitmap(planeBitmap, drawX, drawY, paint);

        canvas.restore();
    }

    private void drawDebugInfo(Canvas canvas) {
        Paint debugPaint = new Paint();
        debugPaint.setColor(Color.WHITE);
        debugPaint.setTextSize(30);
        debugPaint.setShadowLayer(2, 1, 1, Color.BLACK);

        canvas.drawText(String.format("Time: %.1fs", survivalTimeSeconds), 20, screenHeight - 100, debugPaint);
        canvas.drawText(String.format("Speed: %.0f", plane.getSpeed()), 20, screenHeight - 60, debugPaint);
        canvas.drawText(String.format("Distance: %.0fm", plane.getX() / 10), 20, screenHeight - 20, debugPaint);
    }

    private void drawCoinCounter(Canvas canvas) {
        Paint coinPaint = new Paint();
        coinPaint.setColor(Color.WHITE);
        coinPaint.setTextSize(40);
        coinPaint.setShadowLayer(2, 1, 1, Color.BLACK);
        coinPaint.setTextAlign(Paint.Align.CENTER);

        String text = "Coins: " + coinCount;
        canvas.drawText(text, screenWidth / 2f, 60, coinPaint);
    }

    private void drawGameOver(Canvas canvas) {
        Typeface customTypeface = ResourcesCompat.getFont(context, R.font.pixelboy);

        // Semi-transparent overlay
        Paint overlayPaint = new Paint();
        overlayPaint.setColor(Color.argb(180, 0, 0, 0));
        canvas.drawRect(0, 0, screenWidth, screenHeight, overlayPaint);

        // Stroke paint (black outline)
        Paint strokePaint = new Paint();
        strokePaint.setColor(Color.BLACK);
        strokePaint.setTextSize(100);
        strokePaint.setTextAlign(Paint.Align.CENTER);
        strokePaint.setTypeface(customTypeface);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(10);

        // Fill paint (red text)
        Paint fillPaint = new Paint();
        fillPaint.setColor(Color.RED);
        fillPaint.setTextSize(100);
        fillPaint.setTextAlign(Paint.Align.CENTER);
        fillPaint.setTypeface(customTypeface);

        // Game Over text
        canvas.drawText("GAME OVER!", screenWidth / 2, screenHeight / 2 - 50, strokePaint);
        canvas.drawText("GAME OVER!", screenWidth / 2, screenHeight / 2 - 50, fillPaint);

        // Stats
        fillPaint.setColor(Color.WHITE);
        fillPaint.setTextSize(50);
        strokePaint.setTextSize(50);
        strokePaint.setStrokeWidth(6);

        String stats = String.format("Distance: %.0fm | Time: %.1fs", plane.getX() / 10, survivalTimeSeconds);
        canvas.drawText(stats, screenWidth / 2, screenHeight / 2 + 50, strokePaint);
        canvas.drawText(stats, screenWidth / 2, screenHeight / 2 + 50, fillPaint);

        if (coinSprite != null) {
            String coinText = "Coins: " + coinCount;
            canvas.drawText(coinText, screenWidth / 2, screenHeight / 2 + 120, strokePaint);
            canvas.drawText(coinText, screenWidth / 2, screenHeight / 2 + 120, fillPaint);
        }
    }

    /**
     * Spawn a new obstacle based on map type
     * One obstacle spawns at a time with proper spacing
     */
    private void spawnObstacle() {
        // Safety check: Don't spawn if screen not initialized
        if (screenWidth == 0 || screenHeight == 0) {
            android.util.Log.w("GameSurface", "Cannot spawn obstacle: screen dimensions not set");
            return;
        }

        // Get obstacle set based on map
        ObstacleSet obstacleSet = mapType.equals("CITY") ? cityObstacles : forestObstacles;
        if (obstacleSet == null || obstacleSet.obstacles.isEmpty()) return;

        // Pick a random obstacle definition
        ObstacleDefinition def = obstacleSet.getRandom(random);
        if (def == null) return;

        // Load the sprite
        Bitmap baseSprite;
        try {
            baseSprite = BitmapFactory.decodeResource(getResources(), def.resourceId);
            if (baseSprite == null) return;
        } catch (Exception e) {
            return;
        }

        // Calculate dimensions
        float height = screenHeight * def.heightRatio;
        float aspect = (float) baseSprite.getWidth() / (float) baseSprite.getHeight();
        float width = height * aspect;

        // Calculate position
        float x = cameraX + screenWidth + width; // Spawn off right edge
        float y;

        if (def.isGrounded) {
            // Ground obstacles: Position so bottom edge aligns with screen bottom
            // y is the TOP of the bitmap, so: top = screenHeight - height
            // This makes bottom = screenHeight (at screen edge) ✓
            y = screenHeight - height;

            // Debug logging
            android.util.Log.d("Obstacle", String.format(
                    "Grounded %s: screenH=%d, height=%.0f, y=%.0f, bottom=%.0f",
                    def.name, screenHeight, height, y, y + height
            ));
        } else {
            // Flying obstacles in top half of screen
            float minY = 50f; // Keep away from very top
            float maxY = (screenHeight / 2f) - height - 50f; // Top half only
            y = minY + random.nextFloat() * (maxY - minY);
        }

        obstacles.add(new Obstacle(x, y, width, height, baseSprite));
    }

    private void updateObstacles(float deltaTime) {
        long now = System.currentTimeMillis();

        // Spawn new obstacle based on delay
        if (now - lastObstacleSpawnTime > obstacleSpawnDelayMs) {
            spawnObstacle();
            lastObstacleSpawnTime = now;
        }

        // Move obstacles + remove off-screen ones
        for (int i = obstacles.size() - 1; i >= 0; i--) {
            Obstacle o = obstacles.get(i);
            o.update(deltaTime, obstacleSpeed);

            if (o.isOffScreen(cameraX)) {
                obstacles.remove(i);
            }
        }
    }

    private void drawObstacles(Canvas canvas) {
        for (Obstacle o : obstacles) {
            o.draw(canvas, cameraX);
        }
    }

    // ---------------- COINS ----------------

    private void spawnCoin() {
        if (screenWidth == 0 || screenHeight == 0 || coinSprite == null) return;

        // Coin size relative to screen
        float coinSize = screenHeight * 0.06f; // 6% of screen height
        float width = coinSize;
        float height = coinSize;

        // World X: just off the right edge of the camera view
        float x = cameraX + screenWidth + width;

        // Safety margin around obstacles (coins won't spawn within this distance)
        float safetyMargin = coinSize * 1.5f; // 1.5x coin size for comfortable spacing

        // Try to find a safe Y position that doesn't overlap with obstacles
        float y = -1;
        int maxAttempts = 10; // Try up to 10 times to find safe position
        int successAttempt = 0;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            // Random vertical position, avoid extreme top/bottom
            float minY = 150f;
            float maxY = screenHeight - height - 150f;
            float candidateY = minY + random.nextFloat() * (maxY - minY);

            // Create expanded coin bounds with safety margin
            Rect coinBoundsWithMargin = new Rect(
                    (int) (x - safetyMargin),
                    (int) (candidateY - safetyMargin),
                    (int) (x + width + safetyMargin),
                    (int) (candidateY + height + safetyMargin)
            );

            // Check if this position (with margin) overlaps with any obstacle
            boolean safePosition = true;
            for (Obstacle o : obstacles) {
                // Check if coin (with margin) would overlap with obstacle hitbox
                if (Rect.intersects(coinBoundsWithMargin, o.getBounds())) {
                    safePosition = false;
                    break;
                }
            }

            // If position is safe, use it
            if (safePosition) {
                y = candidateY;
                successAttempt = attempt + 1;
                break;
            }
        }

        // Only spawn coin if we found a safe position
        if (y >= 0) {
            coins.add(new Coin(x, y, width, height, coinSprite));
            android.util.Log.d("Coin", String.format("Spawned coin at y=%.0f (found safe spot in %d attempts)", y, successAttempt));
        } else {
            // If no safe position found after max attempts, skip spawning this coin
            android.util.Log.d("Coin", "Skipped coin spawn - no safe position found (obstacles blocking all positions)");
        }
    }

    private void updateCoins(float deltaTime) {
        long now = System.currentTimeMillis();

        // Spawn new coin based on delay
        if (now - lastCoinSpawnTime > coinSpawnDelayMs) {
            spawnCoin();
            lastCoinSpawnTime = now;
        }

        // Move coins + remove off-screen ones
        for (int i = coins.size() - 1; i >= 0; i--) {
            Coin c = coins.get(i);
            c.update(deltaTime, obstacleSpeed); // Same speed as obstacles

            if (c.isOffScreen(cameraX)) {
                coins.remove(i);
            }
        }
    }

    private void drawCoins(Canvas canvas) {
        for (Coin c : coins) {
            c.draw(canvas, cameraX);
        }
    }

    private void checkCoinCollections() {
        if (!isFlying || plane == null) return;
        if (coins.isEmpty()) return;

        Rect planeRect = getPlaneBounds();

        for (int i = coins.size() - 1; i >= 0; i--) {
            Coin c = coins.get(i);
            if (Rect.intersects(planeRect, c.getBounds())) {
                coinCount++; // Collected a coin
                coins.remove(i); // Remove coin from world
            }
        }
    }

    // Plane bounds in WORLD coordinates
    private Rect getPlaneBounds() {
        if (plane == null || planeBitmap == null) {
            return new Rect(0, 0, 0, 0);
        }

        float centerX = plane.getX();
        float centerY = plane.getY();

        // Use smaller hitbox for more forgiving gameplay
        int hitboxWidth = planeBitmap.getWidth() / 2;
        int hitboxHeight = planeBitmap.getHeight() / 2;

        int left = (int) (centerX - hitboxWidth / 2f);
        int top = (int) (centerY - hitboxHeight / 2f);
        int right = left + hitboxWidth;
        int bottom = top + hitboxHeight;

        return new Rect(left, top, right, bottom);
    }

    private void checkCollisions() {
        if (!isFlying || plane == null || isGameOver) return;

        Rect planeRect = getPlaneBounds();

        // Check collision with obstacles
        for (Obstacle o : obstacles) {
            if (Rect.intersects(planeRect, o.getBounds())) {
                // Collision detected!
                gameOver();
                return;
            }
        }
    }

    private void checkCoinCollisions() {
        checkCoinCollections();
    }

    private void gameOver() {
        isGameOver = true;
        upPressed = false;
        downPressed = false;

        // Calculate final distance (plane.getX() / 10 = distance in meters)
        int finalDistance = plane != null ? (int)(plane.getX() / 10) : 0;

        // Notify listener about game over with score and coins
        if (gameOverListener != null) {
            gameOverListener.onGameOver(finalDistance, coinCount);
        }

        // The game will continue running but won't accept input
        // Player can use pause menu to restart or exit
    }

    /**
     * Set the game over listener
     */
    public void setGameOverListener(GameOverListener listener) {
        this.gameOverListener = listener;
    }

    /**
     * Get the current distance traveled in meters
     */
    public int getDistance() {
        return plane != null ? (int)(plane.getX() / 10) : 0;
    }

    /**
     * Get the current coin count
     */
    public int getCoinCount() {
        return coinCount;
    }

    /**
     * Check if the game is over
     */
    public boolean isGameOver() {
        return isGameOver;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!isFlying && !isGameOver) {
                    // Launch the plane
                    isFlying = true;
                    plane.launch(300, -80);
                    lastFrameTime = System.nanoTime();
                    lastObstacleSpawnTime = System.currentTimeMillis();
                    lastCoinSpawnTime = System.currentTimeMillis();
                    return true;
                }
                return false;

            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                return false;
        }

        return super.onTouchEvent(event);
    }
}