package com.oddghosts.foldedflight.game;

/**
 * Physics Engine for Paper Plane
 * Adapted from Elijah Camp
 * Created: 10/30/2025
 * Modified: Toned down aerodynamics + ground effect
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
    private float maxSpeed = Float.MAX_VALUE;

    // Physics constants
    private static final float GRAVITY = 500.0f;
    private float dragCoefficient = 0.005f;

    // Angle of Attack constants (toned down for better control)
    private static final float CLIMB_DRAG_MULTIPLIER = 1.8f;
    private static final float DIVE_DRAG_MULTIPLIER = 0.6f;
    private static final float DIVE_SPEED_BOOST = 100.0f;
    private static final float STALL_ANGLE = 0.7f;
    private static final float STALL_PENALTY = 150.0f;

    // World bounds
    private float worldWidth = 800;
    private float worldHeight = 600;

    // Bounce and friction coefficients
    private static final float BOUNCE_DAMPING = 0.6f;
    private static final float GROUND_FRICTION = 0.95f;
    private static final float MIN_FORWARD_VELOCITY = 100.0f;

    // Ground effect
    private static final float GROUND_EFFECT_HEIGHT = 60.0f;
    private static final float GROUND_EFFECT_STRENGTH = 0.08f;

    // Current state
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

        // Apply aerodynamics
        applyAngleOfAttack();
        applyDrag();
        applyGroundEffect();

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
     * Apply angle of attack physics (toned down)
     */
    private void applyAngleOfAttack() {
        float speed = getSpeed();
        if (speed < 0.01f) {
            currentAngleOfAttack = 0;
            isStalling = false;
            return;
        }

        currentAngleOfAttack = velocityY / speed;

        // Stall check (more forgiving)
        if (currentAngleOfAttack < -STALL_ANGLE && speed < 350) {
            isStalling = true;
            accelY += STALL_PENALTY;
            accelX -= 30.0f;
        } else {
            isStalling = false;
        }

        // Diving speed boost (gentler)
        if (currentAngleOfAttack > 0.3f) {
            accelX += DIVE_SPEED_BOOST * currentAngleOfAttack;
        }
    }

    /**
     * Apply drag force (air resistance)
     */
    private void applyDrag() {
        float speed = getSpeed();
        if (speed < 0.01f) return;

        float dragMultiplier = 1.0f;

        if (currentAngleOfAttack < -0.1f) {
            float climbFactor = Math.min(1.0f, -currentAngleOfAttack / STALL_ANGLE);
            dragMultiplier = 1.0f + (CLIMB_DRAG_MULTIPLIER - 1.0f) * climbFactor;
        } else if (currentAngleOfAttack > 0.1f) {
            float diveFactor = Math.min(1.0f, currentAngleOfAttack / 0.7f);
            dragMultiplier = 1.0f - (1.0f - DIVE_DRAG_MULTIPLIER) * diveFactor;
        }

        float effectiveDrag = dragCoefficient * dragMultiplier;
        accelX -= effectiveDrag * velocityX;
        accelY -= effectiveDrag * velocityY;
    }

    /**
     * Apply ground effect - subtle lift when flying low
     */
    private void applyGroundEffect() {
        float heightAboveGround = worldHeight - y - radius;

        if (heightAboveGround < GROUND_EFFECT_HEIGHT && heightAboveGround > 0) {
            float effect = 1.0f - (heightAboveGround / GROUND_EFFECT_HEIGHT);
            effect = effect * effect;

            if (velocityY > 0) {
                accelY -= velocityY * effect * GROUND_EFFECT_STRENGTH;
            }
        }
    }

    /**
     * Check collision with world bounds
     */
    private void checkBounds() {
        // Ground
        if (y + radius >= worldHeight) {
            y = worldHeight - radius;
            velocityY *= -BOUNCE_DAMPING;
            velocityX *= GROUND_FRICTION;

            if (velocityX < MIN_FORWARD_VELOCITY) {
                velocityX = MIN_FORWARD_VELOCITY;
            }
            isStalling = false;
        }

        // Ceiling
        if (y - radius <= 0) {
            y = radius;
            velocityY *= -BOUNCE_DAMPING;
        }

        // Left wall
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