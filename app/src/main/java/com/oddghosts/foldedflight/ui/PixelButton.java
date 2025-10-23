package com.oddghosts.foldedflight.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.res.ResourcesCompat;

/**
 * Custom 8-bit pixel styled button for Folded Flight game
 * Designed for 64x64 sprite aesthetic with retro gaming feel
 */
public class PixelButton extends AppCompatButton {

    // Pixel border thickness
    private static final int BORDER_THICKNESS = 4;
    private static final int SHADOW_OFFSET = 6;

    // Color scheme
    private int backgroundColor = 0xFF4A90E2; // Default blue
    private int borderColor = 0xFF2E5C8A;
    private int shadowColor = 0xFF1A3A5A;
    private int pressedColor = 0xFF3A7AC2;
    private int textColor = 0xFFFFFFFF;

    // State tracking
    private boolean isPressed = false;

    // Paint objects
    private Paint backgroundPaint;
    private Paint borderPaint;
    private Paint shadowPaint;
    private Paint textPaint;

    // Rect for drawing
    private Rect buttonRect;
    private Rect shadowRect;

    public PixelButton(Context context) {
        super(context);
        init();
    }

    public PixelButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PixelButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Initialize paint objects
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setStyle(Paint.Style.FILL);

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(BORDER_THICKNESS);

        shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(48f); // 8-bit style text size

        // Set default font to monospace
        setTypeface(Typeface.MONOSPACE);

        // Disable default background
        setBackground(null);

        updateColors();
    }

    /**
     * Set a custom font for the button
     * Call this method after creating the button to use a custom font
     * Example: button.setCustomFont(R.font.pixelboy);
     */
    public void setCustomFont(int fontResourceId) {
        try {
            Typeface customFont = ResourcesCompat.getFont(getContext(), fontResourceId);
            if (customFont != null) {
                setTypeface(customFont);
                textPaint.setTypeface(customFont); // Apply to textPaint used in onDraw
                invalidate(); // Force redraw with new font
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Set custom text size for the button
     * Example: button.setCustomTextSize(60f);
     */
    public void setCustomTextSize(float size) {
        textPaint.setTextSize(size);
        setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, size);
        invalidate();
    }

    private void updateColors() {
        backgroundPaint.setColor(isPressed ? pressedColor : backgroundColor);
        borderPaint.setColor(borderColor);
        shadowPaint.setColor(shadowColor);
        textPaint.setColor(textColor);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();

        // Create rectangles for drawing
        if (buttonRect == null) {
            buttonRect = new Rect(
                    BORDER_THICKNESS,
                    BORDER_THICKNESS,
                    width - BORDER_THICKNESS,
                    height - BORDER_THICKNESS
            );

            shadowRect = new Rect(
                    BORDER_THICKNESS + SHADOW_OFFSET,
                    BORDER_THICKNESS + SHADOW_OFFSET,
                    width - BORDER_THICKNESS + SHADOW_OFFSET,
                    height - BORDER_THICKNESS + SHADOW_OFFSET
            );
        }

        // Draw shadow (if not pressed)
        if (!isPressed) {
            canvas.drawRect(shadowRect, shadowPaint);
        }

        // Adjust button position when pressed
        int offsetX = isPressed ? SHADOW_OFFSET / 2 : 0;
        int offsetY = isPressed ? SHADOW_OFFSET / 2 : 0;

        canvas.save();
        canvas.translate(offsetX, offsetY);

        // Draw button background
        canvas.drawRect(buttonRect, backgroundPaint);

        // Draw pixel border with corners
        drawPixelBorder(canvas, buttonRect);

        // Draw text
        String text = getText().toString();
        float textX = buttonRect.centerX();
        float textY = buttonRect.centerY() - ((textPaint.descent() + textPaint.ascent()) / 2);
        canvas.drawText(text, textX, textY, textPaint);

        canvas.restore();
    }

    /**
     * Draws a pixelated border with chunky corners for authentic 8-bit look
     */
    private void drawPixelBorder(Canvas canvas, Rect rect) {
        int cornerSize = BORDER_THICKNESS * 2;

        // Top border
        canvas.drawRect(
                rect.left + cornerSize,
                rect.top,
                rect.right - cornerSize,
                rect.top + BORDER_THICKNESS,
                borderPaint
        );

        // Bottom border
        canvas.drawRect(
                rect.left + cornerSize,
                rect.bottom - BORDER_THICKNESS,
                rect.right - cornerSize,
                rect.bottom,
                borderPaint
        );

        // Left border
        canvas.drawRect(
                rect.left,
                rect.top + cornerSize,
                rect.left + BORDER_THICKNESS,
                rect.bottom - cornerSize,
                borderPaint
        );

        // Right border
        canvas.drawRect(
                rect.right - BORDER_THICKNESS,
                rect.top + cornerSize,
                rect.right,
                rect.bottom - cornerSize,
                borderPaint
        );

        // Corner pixels (4x4 blocks)
        drawCornerPixels(canvas, rect, cornerSize);
    }

    /**
     * Draws pixel-style corners
     */
    private void drawCornerPixels(Canvas canvas, Rect rect, int cornerSize) {
        // Top-left corner
        canvas.drawRect(
                rect.left + BORDER_THICKNESS,
                rect.top + BORDER_THICKNESS,
                rect.left + cornerSize,
                rect.top + cornerSize,
                borderPaint
        );

        // Top-right corner
        canvas.drawRect(
                rect.right - cornerSize,
                rect.top + BORDER_THICKNESS,
                rect.right - BORDER_THICKNESS,
                rect.top + cornerSize,
                borderPaint
        );

        // Bottom-left corner
        canvas.drawRect(
                rect.left + BORDER_THICKNESS,
                rect.bottom - cornerSize,
                rect.left + cornerSize,
                rect.bottom - BORDER_THICKNESS,
                borderPaint
        );

        // Bottom-right corner
        canvas.drawRect(
                rect.right - cornerSize,
                rect.bottom - cornerSize,
                rect.right - BORDER_THICKNESS,
                rect.bottom - BORDER_THICKNESS,
                borderPaint
        );
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isPressed = true;
                updateColors();
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isPressed = false;
                updateColors();
                invalidate();
                break;
        }
        return super.onTouchEvent(event);
    }

    // Color setters for customization
    public void setButtonBackgroundColor(int color) {
        this.backgroundColor = color;
        updateColors();
        invalidate();
    }

    public void setBorderColor(int color) {
        this.borderColor = color;
        updateColors();
        invalidate();
    }

    public void setShadowColor(int color) {
        this.shadowColor = color;
        updateColors();
        invalidate();
    }

    public void setPressedColor(int color) {
        this.pressedColor = color;
        updateColors();
        invalidate();
    }

    public void setTextColor(int color) {
        this.textColor = color;
        updateColors();
        invalidate();
    }
}