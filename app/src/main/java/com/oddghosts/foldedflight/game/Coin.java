package com.oddghosts.foldedflight.game;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

/**
 * Collectible Coin System
 * Adapted from Maxwell Heller
 * Created: 11/26/2025
 */
public class Coin {

    private float x;
    private float y;
    private float width;
    private float height;
    private Bitmap bitmap;

    public Coin(float x, float y, float width, float height, Bitmap baseSprite) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        // Scale coin sprite to the requested size (keeps pixel art crisp)
        this.bitmap = Bitmap.createScaledBitmap(
                baseSprite,
                (int) width,
                (int) height,
                false
        );
    }

    public void update(float deltaTime, float speedX) {
        // same idea as obstacles: move with world
        x += speedX * deltaTime;
    }

    public void draw(Canvas canvas, float cameraX) {
        float drawLeft = x - cameraX;
        float drawTop = y;
        canvas.drawBitmap(bitmap, drawLeft, drawTop, null);
    }

    public boolean isOffScreen(float cameraX) {
        return x + width < cameraX;
    }

    public Rect getBounds() {
        return new Rect(
                (int) x,
                (int) y,
                (int) (x + width),
                (int) (y + height)
        );
    }
}
