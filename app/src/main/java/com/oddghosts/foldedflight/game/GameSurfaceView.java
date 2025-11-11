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
        paint.setFilterBitmap(true);

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
        } else {
            liftForce = 1500.0f;
            forwardThrust = 100.0f;
            dragCoefficient = 0.005f;
            maxSpeed = 1200.0f;
        }

        // Apply max speed to plane if it exists
        if (plane != null) {
            plane.setMaxSpeed(maxSpeed);
            plane.setDragCoefficient(dragCoefficient);
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
            planeBitmap = Bitmap.createScaledBitmap(planeBitmap, 192, 192, true);
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
                true
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
        cameraX = 0;
        upPressed = false;
        downPressed = false;

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
        if (!isFlying || plane == null) return;

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
    }

    private void draw() {
        if (surfaceHolder.getSurface().isValid()) {
            Canvas canvas = surfaceHolder.lockCanvas();
            if (canvas != null) {
                // Clear canvas with sky color
                canvas.drawColor(Color.rgb(135, 206, 250));

                // Draw scrolling background
                drawScrollingBackground(canvas);

                // Draw instructions if not flying
                if (!isFlying) {
                    drawStartInstructions(canvas);
                }

                // Draw plane if flying
                if (isFlying && plane != null && planeBitmap != null) {
                    drawPlane(canvas);
                }

                // Draw debug info
                if (isFlying && plane != null) {
                    drawDebugInfo(canvas);
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
        // This ensures the background repeats forever
        drawX = drawX % backgroundWidth;
        if (drawX > 0) drawX -= backgroundWidth; // Keep drawX negative or zero

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

        canvas.drawText(String.format("Speed: %.0f", plane.getSpeed()), 20, screenHeight - 60, debugPaint);
        canvas.drawText(String.format("Distance: %.0fm", plane.getX() / 10), 20, screenHeight - 20, debugPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!isFlying) {
                    // Launch the plane
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