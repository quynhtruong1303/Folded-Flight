package com.oddghosts.foldedflight.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Custom 8-bit pixel styled card for Folded Flight game
 * Displays any image with retro gaming border aesthetic
 * Reusable for maps, paper plane colors, or any selectable option
 */
public class PixelCard extends View {

    // Card dimensions and styling
    private static final int BORDER_THICKNESS = 8;
    private static final int SHADOW_OFFSET = 12;
    private static final int PADDING = 16;

    // Card colors
    private final int backgroundColor = 0xFFFFFFFF; // White background
    private final int borderColor = 0xFF555555;
    private final int shadowColor = 0xFF828282;
    private final int selectedBorderColor = 0xFF3A7AC2; // Blue for selected

    // State
    private boolean isSelected = false;
    private boolean isPressed = false;
    private Bitmap cardImage;
    private int imageResourceId = -1;

    // Paint objects
    private Paint backgroundPaint;
    private Paint borderPaint;
    private Paint shadowPaint;
    private Paint imagePaint;

    // Rectangles
    private Rect cardRect;
    private Rect shadowRect;
    private Rect imageRect;
    private Rect imageDestRect;

    // Click listener
    private OnClickListener clickListener;

    public PixelCard(Context context) {
        super(context);
        init();
    }

    public PixelCard(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PixelCard(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Initialize paint objects
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setColor(backgroundColor);

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(BORDER_THICKNESS);
        borderPaint.setColor(borderColor);

        shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setStyle(Paint.Style.FILL);
        shadowPaint.setColor(shadowColor);

        imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        imagePaint.setFilterBitmap(false); // Disable filtering for pixel art
    }

    /**
     * Set the card image from a drawable resource
     * Example: card.setImage(R.drawable.city_map);
     */
    public void setImage(int resourceId) {
        this.imageResourceId = resourceId;
        try {
            cardImage = BitmapFactory.decodeResource(getResources(), resourceId);
            invalidate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Set the card image from a Bitmap directly
     */
    public void setImage(Bitmap bitmap) {
        this.cardImage = bitmap;
        invalidate();
    }

    /**
     * Set the background color of the card using a hex color value
     * Example: card.setCardBackgroundColor(0xFFFF5733);
     */
    public void setBackgroundColor(int color) {
        backgroundPaint.setColor(color);
        invalidate();
    }

    /**
     * Set whether this card is selected
     */
    public void setSelected(boolean selected) {
        this.isSelected = selected;
        invalidate();
    }

    public boolean isSelected() {
        return isSelected;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        // Initialize rectangles if needed
        if (cardRect == null) {
            int offset = BORDER_THICKNESS;
            cardRect = new Rect(offset, offset, width - offset, height - offset);

            shadowRect = new Rect(
                    offset + SHADOW_OFFSET,
                    offset + SHADOW_OFFSET,
                    width - offset + SHADOW_OFFSET,
                    height - offset + SHADOW_OFFSET
            );

            // Image takes up entire card area with padding
            imageDestRect = new Rect(
                    cardRect.left + PADDING,
                    cardRect.top + PADDING,
                    cardRect.right - PADDING,
                    cardRect.bottom - PADDING
            );
        }

        // Draw shadow (if not pressed)
        if (!isPressed) {
            canvas.drawRect(shadowRect, shadowPaint);
        }

        // Adjust card position when pressed
        int offsetX = isPressed ? SHADOW_OFFSET / 2 : 0;
        int offsetY = isPressed ? SHADOW_OFFSET / 2 : 0;

        canvas.save();
        canvas.translate(offsetX, offsetY);

        // Draw card background
        canvas.drawRect(cardRect, backgroundPaint);

        // Draw border (gold if selected, normal otherwise)
        borderPaint.setColor(isSelected ? selectedBorderColor : borderColor);
        drawPixelBorder(canvas, cardRect);

        // Draw image if available
        if (cardImage != null) {
            if (imageRect == null) {
                imageRect = new Rect(0, 0, cardImage.getWidth(), cardImage.getHeight());
            }
            canvas.drawBitmap(cardImage, imageRect, imageDestRect, imagePaint);
        }

        // Draw selection indicator if selected
        if (isSelected) {
            drawSelectionIndicator(canvas);
        }

        canvas.restore();
    }

    /**
     * Draws pixel border with corners
     */
    private void drawPixelBorder(Canvas canvas, Rect rect) {
        int cornerSize = BORDER_THICKNESS * 2;

        // Draw border segments (same as PixelButton)
        canvas.drawRect(rect.left + cornerSize, rect.top, rect.right - cornerSize,
                rect.top + BORDER_THICKNESS, borderPaint);
        canvas.drawRect(rect.left + cornerSize, rect.bottom - BORDER_THICKNESS,
                rect.right - cornerSize, rect.bottom, borderPaint);
        canvas.drawRect(rect.left, rect.top + cornerSize, rect.left + BORDER_THICKNESS,
                rect.bottom - cornerSize, borderPaint);
        canvas.drawRect(rect.right - BORDER_THICKNESS, rect.top + cornerSize,
                rect.right, rect.bottom - cornerSize, borderPaint);

        // Corner blocks
        drawCornerBlocks(canvas, rect, cornerSize);
    }

    private void drawCornerBlocks(Canvas canvas, Rect rect, int cornerSize) {
        canvas.drawRect(rect.left + BORDER_THICKNESS, rect.top + BORDER_THICKNESS,
                rect.left + cornerSize, rect.top + cornerSize, borderPaint);
        canvas.drawRect(rect.right - cornerSize, rect.top + BORDER_THICKNESS,
                rect.right - BORDER_THICKNESS, rect.top + cornerSize, borderPaint);
        canvas.drawRect(rect.left + BORDER_THICKNESS, rect.bottom - cornerSize,
                rect.left + cornerSize, rect.bottom - BORDER_THICKNESS, borderPaint);
        canvas.drawRect(rect.right - cornerSize, rect.bottom - cornerSize,
                rect.right - BORDER_THICKNESS, rect.bottom - BORDER_THICKNESS, borderPaint);
    }

    /**
     * Draws a selection indicator in the corner
     */
    private void drawSelectionIndicator(Canvas canvas) {
        Paint checkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        checkPaint.setColor(selectedBorderColor);
        checkPaint.setStyle(Paint.Style.FILL);

        // Draw a pixel checkmark in top right
        int checkX = cardRect.right - 40;
        int checkY = cardRect.top + 20;

        // Simplified pixel checkmark
        canvas.drawRect(checkX, checkY + 12, checkX + 8, checkY + 20, checkPaint);
        canvas.drawRect(checkX + 8, checkY + 8, checkX + 16, checkY + 16, checkPaint);
        canvas.drawRect(checkX + 16, checkY, checkX + 24, checkY + 12, checkPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isPressed = true;
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                isPressed = false;
                invalidate();
                if (clickListener != null) {
                    clickListener.onClick(this);
                }
                performClick();
                return true;
            case MotionEvent.ACTION_CANCEL:
                isPressed = false;
                invalidate();
                return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    public void setOnClickListener(OnClickListener listener) {
        this.clickListener = listener;
    }
}