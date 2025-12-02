package com.oddghosts.foldedflight.game;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

/**
 * Obstacles handling with collision detection
 * Adapted from Maxwell Heller
 * Created: 11/26/2025
 */

public class Obstacle {

    private float x;
    private float y;
    private float width;
    private float height;
    private Bitmap bitmap;

    public Obstacle(float x, float y, float width, float height, Bitmap baseSprite) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        // Scale sprite to obstacle size.
        // filter = false to keep pixel art sharp (no blur).
        this.bitmap = Bitmap.createScaledBitmap(
                baseSprite,
                (int) width,
                (int) height,
                false
        );
    }

    public void update(float deltaTime, float speedX) {
        x += speedX * deltaTime;
    }

    public void draw(Canvas canvas, float cameraX) {
        float drawLeft = x - cameraX;
        float drawTop = y;
        canvas.drawBitmap(bitmap, drawLeft, drawTop, null); // null paint: no extra filtering
    }

    public boolean isOffScreen(float cameraX) {
        return x + width < cameraX;
    }

    // Collision bounds in WORLD coordinates
    public Rect getBounds() {
        // Hitbox is half the sprite size
        float hitboxWidth = width * 0.75f;
        float hitboxHeight = height * 0.75f;

        // Center the hitbox inside the sprite
        float left = x + (width - hitboxWidth) / 2f;
        float top = y + (height - hitboxHeight) / 2f;

        return new Rect(
                (int) left,
                (int) top,
                (int) (left + hitboxWidth),
                (int) (top + hitboxHeight)
        );
    }

}


