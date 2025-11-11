package com.oddghosts.foldedflight.game;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.core.content.res.ResourcesCompat;

import com.oddghosts.foldedflight.R;

public class GameSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

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

    // Camera
    private float cameraX = 0;

    // Touch controls
    private boolean upPressed = false;
    private boolean downPressed = false;
    private float touchStartY = 0;

    // Graphics
    private Bitmap originalBackgroundBitmap;  // Original unscaled bitmap
    private Bitmap scaledBackgroundBitmap;    // Scaled for current screen
    private Bitmap planeBitmap;
    private Paint paint;

    // Background scaling options
    public enum BackgroundMode {
        SCALE_TO_FIT_HEIGHT,    // Scale to match height, scroll horizontally
    }

    private BackgroundMode backgroundMode = BackgroundMode.SCALE_TO_FIT_HEIGHT;

    // Screen dimensions
    private int screenWidth;
    private int screenHeight;

    // Background dimensions
    private int backgroundWidth;
    private int backgroundHeight;
    private float backgroundScale = 1.0f;

    // For tiling mode
    private int tileWidth;
    private int tileHeight;

    // Timing
    private long lastFrameTime;
    private static final int TARGET_FPS = 60;
    private static final long FRAME_TIME = 1000 / TARGET_FPS;

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
        paint.setFilterBitmap(true); // Enable filtering for smooth scaling

        this.context = context;

        setFocusable(true);
    }

    public void setGameSettings(String map, String planeColor, String difficulty) {
        this.mapType = map;
        this.planeColor = planeColor;
        this.difficulty = difficulty;

        // Set difficulty parameters
        if (difficulty.equals("HARD")) {
            liftForce = 2500.0f;
            forwardThrust = 200.0f;
            dragCoefficient = 0.008f;
        } else {
            liftForce = 1500.0f;
            forwardThrust = 100.0f;
            dragCoefficient = 0.005f;
        }
    }

    /**
     * Set the background scaling mode
     */
    public void setBackgroundMode(BackgroundMode mode) {
        this.backgroundMode = mode;
        if (screenWidth > 0 && screenHeight > 0) {
            prepareBackground();
        }
    }

    private void loadResources() {
        // Load original background based on map type
        if (mapType.equals("CITY")) {
            originalBackgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.city_map);
        } else {
            originalBackgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.forest_map);
        }

        // Prepare background based on selected mode
        prepareBackground();

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

        // Scale plane to appropriate size (about 64x64)
        if (planeBitmap != null) {
            planeBitmap = Bitmap.createScaledBitmap(planeBitmap, 192, 192, true);
        }
    }

    /**
     * Prepare background
     */
    private void prepareBackground() {
        if (originalBackgroundBitmap == null || screenHeight == 0 || screenWidth == 0) {
            return;
        }
        prepareScaleToFitHeight();
    }

    /**
     * Scale background to match screen height, maintain aspect ratio
     * Width may extend beyond screen for scrolling
     */
    private void prepareScaleToFitHeight() {
        float scale = (float) screenHeight / originalBackgroundBitmap.getHeight();
        backgroundScale = scale;

        backgroundWidth = (int) (originalBackgroundBitmap.getWidth() * scale);
        backgroundHeight = screenHeight;

        // Create scaled bitmap
        scaledBackgroundBitmap = Bitmap.createScaledBitmap(
                originalBackgroundBitmap,
                backgroundWidth,
                backgroundHeight,
                true
        );
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        screenWidth = getWidth();
        screenHeight = getHeight();

        // Load resources after we know screen dimensions
        loadResources();

        // Initialize plane physics (start from left side, middle height)
        plane = new PlanePhysics(100, screenHeight / 2.0f, 32, 1.0f);
        plane.setWorldBounds(100000, screenHeight);
        plane.setDragCoefficient(dragCoefficient);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        screenWidth = width;
        screenHeight = height;

        // Recalculate background scaling when screen size changes
        prepareBackground();

        if (plane != null) {
            plane.setWorldBounds(100000, height);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopGame();
    }

    public void startGame() {
        isRunning = true;
        isPaused = false;
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
        // Reset flying state
        isFlying = false;

        // Reset camera
        cameraX = 0;

        // Reset touch controls
        upPressed = false;
        downPressed = false;
        touchStartY = 0;

        // Reset plane physics to starting position
        if (plane != null) {
            plane.reset(100, screenHeight / 2.0f);
        }

        // Reset frame time
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
        if (!isFlying || plane == null) return;

        // Always apply forward thrust
        plane.applyForce(forwardThrust, 0);

        // Apply lift/dive forces based on touch
        if (upPressed) {
            plane.applyForce(0, -liftForce);
        }
        if (downPressed) {
            plane.applyForce(0, liftForce * 0.5f);
        }

        // Update physics
        plane.update(deltaTime);

        // Ensure minimum forward velocity
        if (plane.getVelocityX() < 150) {
            plane.launch(150, plane.getVelocityY());
        }

        // Update camera to follow plane
        cameraX = plane.getX() - screenWidth * 0.25f;
        if (cameraX < 0) cameraX = 0;
    }

    private void draw() {
        if (surfaceHolder.getSurface().isValid()) {
            Canvas canvas = surfaceHolder.lockCanvas();
            if (canvas != null) {
                // Clear canvas with sky color
                canvas.drawColor(Color.rgb(135, 206, 250));

                // Draw background based on mode
                drawBackground(canvas);

                // Draw instructions if not flying
                if (!isFlying) {
                    drawStartInstructions(canvas);
                }

                // Draw plane if flying
                if (isFlying && plane != null && planeBitmap != null) {
                    drawPlane(canvas);
                }

                // Draw debug info (optional)
                if (isFlying && plane != null) {
                    drawDebugInfo(canvas);
                }

                surfaceHolder.unlockCanvasAndPost(canvas);
            }
        }
    }

    /**
     * Draw background based on the selected mode
     */
    private void drawBackground(Canvas canvas) {
        if (scaledBackgroundBitmap == null) return;
        drawScrollingBackground(canvas);
    }

    /**
     * Draw scrolling background (SCALE_TO_FIT_HEIGHT)
     */
    private void drawScrollingBackground(Canvas canvas) {
        // Calculate visible portion of background
        float drawX = -cameraX * 0.5f; // Slower scrolling for parallax effect

        // If background is smaller than needed, tile it
        if (backgroundWidth < screenWidth * 2) {
            // Draw multiple copies
            while (drawX < screenWidth) {
                canvas.drawBitmap(scaledBackgroundBitmap, drawX, 0, paint);
                drawX += backgroundWidth;
            }
        } else {
            // Draw single background
            canvas.drawBitmap(scaledBackgroundBitmap, drawX, 0, paint);
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

        // ===== MAIN TITLE =====
        canvas.drawText("TAP TO LAUNCH!", screenWidth / 2, screenHeight / 2, strokePaint);
        canvas.drawText("TAP TO LAUNCH!", screenWidth / 2, screenHeight / 2, fillPaint);

        // ===== SUBTEXT =====
        strokePaint.setTextSize(60);
        fillPaint.setTextSize(60);

        canvas.drawText("Hold UP side to climb", screenWidth / 2, screenHeight / 2 + 80, strokePaint);
        canvas.drawText("Hold UP side to climb", screenWidth / 2, screenHeight / 2 + 80, fillPaint);

        canvas.drawText("Hold DOWN side to dive", screenWidth / 2, screenHeight / 2 + 130, strokePaint);
        canvas.drawText("Hold DOWN side to dive", screenWidth / 2, screenHeight / 2 + 130, fillPaint);
    }

    private void drawPlane(Canvas canvas) {
        float drawX = plane.getX() - cameraX - planeBitmap.getWidth() / 2;
        float drawY = plane.getY() - planeBitmap.getHeight() / 2;

        canvas.save();

        // Calculate rotation based on velocity
        float angle = 0;
        if (Math.abs(plane.getVelocityY()) > 10) {
            angle = (float) Math.toDegrees(Math.atan2(plane.getVelocityY(), plane.getVelocityX()));
            angle = Math.max(-30, Math.min(30, angle)); // Limit rotation
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

        canvas.drawText(String.format("Speed: %.0f", plane.getSpeed()), 20, screenHeight - 60, debugPaint);
        canvas.drawText(String.format("Distance: %.0fm", plane.getX() / 10), 20, screenHeight - 20, debugPaint);
        canvas.drawText("Mode: " + backgroundMode.name(), 20, 60, debugPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchStartY = y;

                if (!isFlying) {
                    // Start the game
                    isFlying = true;
                    plane.launch(300, -80);
                    lastFrameTime = System.nanoTime();
                } else {
                    // Control the plane
                    if (y < screenHeight / 2) {
                        upPressed = true;
                        downPressed = false;
                    } else {
                        downPressed = true;
                        upPressed = false;
                    }
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                if (isFlying) {
                    if (y < screenHeight / 2) {
                        upPressed = true;
                        downPressed = false;
                    } else {
                        downPressed = true;
                        upPressed = false;
                    }
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                upPressed = false;
                downPressed = false;
                return true;
        }

        return super.onTouchEvent(event);
    }
}