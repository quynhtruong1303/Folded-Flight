package com.oddghosts.foldedflight.game;

/**
 * Physics engine for the paper plane
 * Adapted from Elijah Camp
 * Created: 10/30/2025
 * Modified: Added speed limit, angle of attack aerodynamics
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

    // Angle of Attack constants
    private static final float CLIMB_DRAG_MULTIPLIER = 2.5f;  // Climbing = more drag
    private static final float DIVE_DRAG_MULTIPLIER = 0.4f;   // Diving = less drag
    private static final float DIVE_SPEED_BOOST = 150.0f;     // Bonus acceleration when diving
    private static final float STALL_ANGLE = 0.6f;            // ~34 degrees - steep climb triggers stall
    private static final float STALL_PENALTY = 300.0f;        // Extra downward force when stalling

    // World bounds
    private float worldWidth = 800;
    private float worldHeight = 600;

    // Bounce and friction coefficients
    private static final float BOUNCE_DAMPING = 0.6f;
    private static final float GROUND_FRICTION = 0.95f;
    private static final float MIN_FORWARD_VELOCITY = 100.0f;

    // Current angle of attack (for external use, e.g., UI display)
    private float currentAngleOfAttack = 0f;
    private boolean isStalling = false;

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

        // Apply angle-of-attack aerodynamics
        applyAngleOfAttack(deltaTime);

        // Apply drag (now modified by angle of attack)
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
     * Apply angle of attack physics
     * Climbing = more drag, lose speed
     * Diving = less drag, gain speed
     * Steep climb = stall (nose drops)
     */
    private void applyAngleOfAttack(float deltaTime) {
        float speed = (float) Math.sqrt(velocityX * velocityX + velocityY * velocityY);
        if (speed < 0.01f) {
            currentAngleOfAttack = 0;
            isStalling = false;
            return;
        }

        // Calculate angle of attack: ratio of vertical to horizontal velocity
        // Negative = climbing, Positive = diving
        currentAngleOfAttack = velocityY / speed;

        // Check for stall condition (climbing too steeply with low speed)
        if (currentAngleOfAttack < -STALL_ANGLE && speed < 400) {
            isStalling = true;
            // Stall: plane nose drops, loses lift
            accelY += STALL_PENALTY;
            // Also lose forward momentum during stall
            accelX -= 50.0f;
        } else {
            isStalling = false;
        }

        // Diving boost: gain speed when pointing downward
        if (currentAngleOfAttack > 0.3f) {
            // Convert potential energy to speed (like real physics!)
            accelX += DIVE_SPEED_BOOST * currentAngleOfAttack;
        }
    }

    /**
     * Apply drag force (air resistance)
     * Modified by angle of attack
     */
    private void applyDrag() {
        float speed = (float) Math.sqrt(velocityX * velocityX + velocityY * velocityY);

        if (speed > 0.01f) {
            // Calculate drag multiplier based on angle of attack
            float dragMultiplier = 1.0f;

            if (currentAngleOfAttack < -0.1f) {
                // Climbing: interpolate toward max climb drag
                float climbFactor = Math.min(1.0f, -currentAngleOfAttack / STALL_ANGLE);
                dragMultiplier = 1.0f + (CLIMB_DRAG_MULTIPLIER - 1.0f) * climbFactor;
            } else if (currentAngleOfAttack > 0.1f) {
                // Diving: interpolate toward min dive drag
                float diveFactor = Math.min(1.0f, currentAngleOfAttack / 0.7f);
                dragMultiplier = 1.0f - (1.0f - DIVE_DRAG_MULTIPLIER) * diveFactor;
            }

            // Apply modified drag
            float effectiveDrag = dragCoefficient * dragMultiplier;
            accelX -= effectiveDrag * velocityX;
            accelY -= effectiveDrag * velocityY;
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

            // Reset stall on ground contact
            isStalling = false;
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
     * Get current angle of attack
     * Negative = climbing, Positive = diving
     * Range roughly -1 to 1
     */
    public float getAngleOfAttack() {
        return currentAngleOfAttack;
    }

    /**
     * Check if plane is currently stalling
     */
    public boolean isStalling() {
        return isStalling;
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
        this.currentAngleOfAttack = 0;
        this.isStalling = false;
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
