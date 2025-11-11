package com.oddghosts.foldedflight.game;

/**
 * Physics engine for the paper plane
 * Adapted from Elijah Camp
 * Created: 10/30/2025
 * Modified: Added speed limit and infinite scrolling support
 */
public class PlanePhysics {

    // Position
    private float x;
    private float y;

    // Velocity
    private float velocityX;
    private float velocityY;

    // Acceleration
    private float accelX;
    private float accelY;

    // Properties
    private float mass;
    private float radius;

    // Speed limit
    private float maxSpeed = Float.MAX_VALUE; // Default unlimited

    // Physics constants
    private static final float GRAVITY = 500.0f; // Pixels per second squared
    private float dragCoefficient = 0.005f;

    // World bounds
    private float worldWidth = 800;
    private float worldHeight = 600;

    // Bounce and friction coefficients
    private static final float BOUNCE_DAMPING = 0.6f;
    private static final float GROUND_FRICTION = 0.95f; // Reduced friction to keep moving
    private static final float MIN_FORWARD_VELOCITY = 100.0f; // Minimum forward speed

    public PlanePhysics(float x, float y, float radius, float mass) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.mass = mass;
        this.velocityX = 0;
        this.velocityY = 0;
        this.accelX = 0;
        this.accelY = 0;
    }

    /**
     * Set maximum speed limit
     */
    public void setMaxSpeed(float maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    /**
     * Update physics simulation
     * @param deltaTime Time since last update in seconds
     */
    public void update(float deltaTime) {
        // Apply gravity
        accelY += GRAVITY;

        // Apply drag
        applyDrag();

        // Update velocity
        velocityX += accelX * deltaTime;
        velocityY += accelY * deltaTime;

        // Apply speed limit
        float speed = (float) Math.sqrt(velocityX * velocityX + velocityY * velocityY);
        if (speed > maxSpeed) {
            float scale = maxSpeed / speed;
            velocityX *= scale;
            velocityY *= scale;
        }

        // Update position
        x += velocityX * deltaTime;
        y += velocityY * deltaTime;

        // Check bounds
        checkBounds();

        // Reset acceleration for next frame
        accelX = 0;
        accelY = 0;
    }

    /**
     * Apply drag force (air resistance)
     */
    private void applyDrag() {
        float speed = (float) Math.sqrt(velocityX * velocityX + velocityY * velocityY);

        if (speed > 0.01f) {
            // Simple linear drag model
            accelX -= dragCoefficient * velocityX;
            accelY -= dragCoefficient * velocityY;
        }
    }

    /**
     * Check collision with world bounds
     * Modified for infinite horizontal scrolling
     */
    private void checkBounds() {
        // Bottom collision (ground)
        if (y + radius >= worldHeight) {
            y = worldHeight - radius;
            velocityY *= -BOUNCE_DAMPING;

            // Apply minimal friction but maintain forward momentum
            velocityX *= GROUND_FRICTION;

            // Ensure minimum forward velocity for infinite scrolling
            if (velocityX < MIN_FORWARD_VELOCITY) {
                velocityX = MIN_FORWARD_VELOCITY;
            }
        }

        // Top collision
        if (y - radius <= 0) {
            y = radius;
            velocityY *= -BOUNCE_DAMPING;
        }

        // NO RIGHT WALL CHECK - allows infinite scrolling
        // The game world extends infinitely to the right

        // Left wall (prevent going backwards)
        if (x - radius <= 0) {
            x = radius;
            velocityX = Math.max(velocityX, MIN_FORWARD_VELOCITY);
        }
    }

    /**
     * Launch with initial velocity
     */
    public void launch(float vx, float vy) {
        velocityX = vx;
        velocityY = vy;
    }

    /**
     * Apply force to the plane
     */
    public void applyForce(float fx, float fy) {
        accelX += fx / mass;
        accelY += fy / mass;
    }

    /**
     * Set world bounds for collision detection
     */
    public void setWorldBounds(float width, float height) {
        this.worldWidth = width;
        this.worldHeight = height;
    }

    /**
     * Set drag coefficient for air resistance
     */
    public void setDragCoefficient(float dragCoefficient) {
        this.dragCoefficient = dragCoefficient;
    }

    /**
     * Get current speed
     */
    public float getSpeed() {
        return (float) Math.sqrt(velocityX * velocityX + velocityY * velocityY);
    }

    /**
     * Check if the plane is still moving
     */
    public boolean isMoving() {
        return Math.abs(velocityX) > 0.1f || Math.abs(velocityY) > 0.1f;
    }

    /**
     * Reset the plane to initial position
     */
    public void reset(float x, float y) {
        this.x = x;
        this.y = y;
        this.velocityX = 0;
        this.velocityY = 0;
        this.accelX = 0;
        this.accelY = 0;
    }

    // Getters
    public float getX() { return x; }
    public float getY() { return y; }
    public float getVelocityX() { return velocityX; }
    public float getVelocityY() { return velocityY; }
    public float getRadius() { return radius; }

    // Setters
    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }
}