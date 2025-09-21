package Client;

import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import java.util.Random;

public class Ryu extends Entity {

    // Ryu: vars...
    private int health;
    private int maxHealth = 100;
    private double velX, velY;
    private boolean facingRight = true;

    // STATES
    private final int IDLING = 0;
    private final int PARRYING_R = 1;
    private final int PARRYING_L = 2;
    private final int CROUCHING = 3;
    private final int JUMPING = 9;
    private final int FRONT_FLIPPING = 10;
    private final int BACK_FLIPPING = 11;

    // ground attacks
    private final int ATTACKING_G = 4;
    private final int ATTACKING_H = 5;
    private final int ATTACKING_B = 6;
    private final int ATTACKING_N = 7;

    // crouch attacks
    private final int ATTACKING_C_G = 8;

    // air attacks
    private final int ATTACKING_A_G = 12;
    private final int ATTACKING_A_H = 13;
    private final int ATTACKING_A_B = 14;

    // hurting anims
    private final int HURTING = 15;
    private final int DEAD = 16;
    private final int BLOCKING = 17;

    // platformer
    private final double GRAV = 0.5;
    private final double JUMP_SPEED = -12;
    private final double TERMINAL_VELOCITY = 6;
    private final double GROUND_Y = 450;

    private boolean[] anims = new boolean[20];
    private long lastAnimTime;
    private int currentState = IDLING;

    // cooldowns
    private boolean hurting;
    private boolean isAttacking = false;
    private boolean canMove = true;
    private boolean onGround = true;
    private long lastHitTime;

    // random generator
    private Random rand;

    public Ryu(double startX, double groundY) {
        super((float) startX, (float) groundY);
        this.y = (float) groundY;
        rand = new Random();
        health = maxHealth;
        anims[IDLING] = true; // Start in idle state
    }

    @Override
    public void tick() {
        // Update physics
        x += velX;
        y += velY;

        // Apply gravity if in the air
        if (y < GROUND_Y) {
            velY += GRAV;
            if (velY > TERMINAL_VELOCITY) {
                velY = TERMINAL_VELOCITY;
            }
            onGround = false;
        } else {
            y = (float) GROUND_Y;
            velY = 0;
            onGround = true;
            // If was jumping, return to idle
            if (currentState == JUMPING) {
                handleAnims(IDLING);
            }
        }

        // Apply friction
        velX *= 0.85;

        // Check screen boundaries
        checkWalls();
    }

    @Override
    public void render(GraphicsContext g) {
        Image currentFrame = getCurrentAnimFrame();
        if (currentFrame == null) return;

        // Draw shadow
        g.setFill(javafx.scene.paint.Color.rgb(0, 0, 0, 0.5));
        g.fillOval(x + currentFrame.getWidth() / 2 - 32, GROUND_Y + 110, 64, 16);

        // Apply "hurt" translation if needed
        if (hurting) {
            g.save();
            g.translate(rand.nextInt(3) - 1.5, rand.nextInt(3) - 1.5);
        }

        // Flip image if facing left
        if (!facingRight) {
            g.save();
            g.scale(-1, 1);
            g.drawImage(currentFrame, -x - currentFrame.getWidth(), y);
            g.restore();
        } else {
            g.drawImage(currentFrame, x, y);
        }

        if (hurting) {
            g.restore();
        }
    }

    // Movement methods for the fight scene controller
    public void moveForward() {
        if (canMove && onGround && currentState != CROUCHING) {
            velX = facingRight ? 2 : -2;
            if (currentState == IDLING) {
                handleAnims(facingRight ? PARRYING_R : PARRYING_L);
            }
        }
    }

    public void moveBackward() {
        if (canMove && onGround && currentState != CROUCHING) {
            velX = facingRight ? -2 : 2;
            if (currentState == IDLING) {
                handleAnims(facingRight ? PARRYING_L : PARRYING_R);
            }
        }
    }

    public void jump() {
        if (canMove && onGround) {
            velY = JUMP_SPEED;
            onGround = false;
            handleAnims(JUMPING);
        }
    }

    public void crouch() {
        if (canMove && onGround) {
            handleAnims(CROUCHING);
            velX = 0;
        }
    }

    public void stopCrouching() {
        if (currentState == CROUCHING) {
            handleAnims(IDLING);
        }
    }

    public void stopMoving() {
        if (currentState == PARRYING_R || currentState == PARRYING_L) {
            handleAnims(IDLING);
        }
    }

    // Attack methods
    public void punch() {
        if (canAttack()) {
            if (onGround) {
                if (currentState == CROUCHING) {
                    handleAnims(ATTACKING_C_G);
                } else {
                    handleAnims(ATTACKING_G);
                }
            } else {
                handleAnims(ATTACKING_A_G);
            }
            isAttacking = true;
            canMove = false;
        }
    }

    public void quickPunch() {
        if (canAttack() && onGround) {
            handleAnims(ATTACKING_H);
            isAttacking = true;
            canMove = false;
        }
    }

    public void kick() {
        if (canAttack()) {
            if (onGround) {
                handleAnims(ATTACKING_N);
            } else {
                handleAnims(ATTACKING_A_B);
            }
            isAttacking = true;
            canMove = false;
        }
    }

    public void uppercut() {
        if (canAttack() && onGround) {
            handleAnims(ATTACKING_B);
            isAttacking = true;
            canMove = false;
        }
    }

    public void block() {
        if (canMove && onGround) {
            handleAnims(BLOCKING);
        }
    }

    public void stopBlocking() {
        if (currentState == BLOCKING) {
            handleAnims(IDLING);
        }
    }

    private boolean canAttack() {
        return canMove && currentState != HURTING && currentState != DEAD;
    }

    // Combat methods
    public void takeDamage(int damage) {
        if (currentState == BLOCKING) {
            damage /= 2; // Reduce damage when blocking
        }

        health -= damage;
        if (health <= 0) {
            health = 0;
            handleAnims(DEAD);
            canMove = false;
        } else {
            hurting = true;
            lastHitTime = System.currentTimeMillis();
            handleAnims(HURTING);
            canMove = false;
            // Add slight knockback
            velX = facingRight ? -2 : 2;
        }
    }

    public boolean isAttackHitting(Entity opponent) {
        if (!isAttacking || !isInAttackFrame()) return false;

        Rectangle2D attackBounds = getAttackBounds();
        Rectangle2D opponentBounds = getOpponentHitBounds(opponent);

        return attackBounds.intersects(opponentBounds);
    }

    private boolean isInAttackFrame() {
        long now = System.currentTimeMillis();
        long diff = now - lastAnimTime;

        switch (currentState) {
            case ATTACKING_G:
                return diff >= 200 && diff <= 400; // Active frames
            case ATTACKING_H:
                return diff >= 100 && diff <= 200;
            case ATTACKING_B:
                return diff >= 225 && diff <= 375;
            case ATTACKING_N:
                return diff >= 200 && diff <= 300;
            case ATTACKING_C_G:
                return diff >= 50 && diff <= 100;
            case ATTACKING_A_G:
            case ATTACKING_A_H:
            case ATTACKING_A_B:
                return diff >= 200 && diff <= 400;
            default:
                return false;
        }
    }

    private Rectangle2D getOpponentHitBounds(Entity opponent) {
        // Assuming opponent has similar dimensions
        return new Rectangle2D(opponent.x, opponent.y, 60, 110);
    }

    private Image getCurrentAnimFrame() {
        long now = System.currentTimeMillis();
        long diff = now - lastAnimTime;

        // Check if animation finished and reset state
        boolean animFinished = false;

        if (anims[IDLING]) return Assets.idle[(int) ((diff / 150) % Assets.idle.length)];
        if (anims[PARRYING_R]) return Assets.parry_f[(int) ((diff / 120) % Assets.parry_f.length)];
        if (anims[PARRYING_L]) return Assets.parry_b[(int) ((diff / 120) % Assets.parry_b.length)];
        if (anims[CROUCHING]) return Assets.crouch[0];
        if (anims[BLOCKING]) return Assets.parry_b[(int) ((diff / 120) % Assets.parry_b.length)];
        if (anims[JUMPING]) return Assets.jump[(int) ((diff / 80) % Assets.jump.length)];

        // Attack animations with auto-return to idle
        if (anims[ATTACKING_G]) {
            int frameIndex = (int) (diff / 80);
            if (frameIndex < Assets.punch.length) {
                return Assets.punch[frameIndex];
            } else {
                isAttacking = false;
                canMove = true;
                handleAnims(IDLING);
                return Assets.idle[0];
            }
        }

        if (anims[ATTACKING_H]) {
            int frameIndex = (int) (diff / 60);
            if (frameIndex < Assets.quick_punch.length) {
                return Assets.quick_punch[frameIndex];
            } else {
                isAttacking = false;
                canMove = true;
                handleAnims(IDLING);
                return Assets.idle[0];
            }
        }

        if (anims[ATTACKING_B]) {
            int frameIndex = (int) (diff / 80);
            if (frameIndex < Assets.upper_kick.length) {
                return Assets.upper_kick[frameIndex];
            } else {
                isAttacking = false;
                canMove = true;
                handleAnims(IDLING);
                return Assets.idle[0];
            }
        }

        if (anims[ATTACKING_N]) {
            int frameIndex = (int) (diff / 100);
            if (frameIndex < Assets.kick_low.length) {
                return Assets.kick_low[frameIndex];
            } else {
                isAttacking = false;
                canMove = true;
                handleAnims(IDLING);
                return Assets.idle[0];
            }
        }

        if (anims[ATTACKING_C_G]) {
            int frameIndex = (int) (diff / 80);
            if (frameIndex < Assets.crouch_punch.length) {
                return Assets.crouch_punch[frameIndex];
            } else {
                isAttacking = false;
                canMove = true;
                handleAnims(CROUCHING);
                return Assets.crouch[0];
            }
        }

        // Air attacks
        if (anims[ATTACKING_A_G]) {
            int frameIndex = (int) (diff / 80);
            if (frameIndex < Assets.air_punch.length) {
                return Assets.air_punch[frameIndex];
            } else {
                isAttacking = false;
                velX = 0;
                handleAnims(JUMPING);
                return Assets.jump[0];
            }
        }

        if (anims[ATTACKING_A_H]) {
            int frameIndex = (int) (diff / 80);
            if (frameIndex < Assets.punch_down.length) {
                return Assets.punch_down[frameIndex];
            } else {
                isAttacking = false;
                velX = 0;
                handleAnims(JUMPING);
                return Assets.jump[0];
            }
        }

        if (anims[ATTACKING_A_B]) {
            int frameIndex = (int) (diff / 80);
            if (frameIndex < Assets.air_kick.length) {
                return Assets.air_kick[frameIndex];
            } else {
                isAttacking = false;
                velX = 0;
                handleAnims(JUMPING);
                return Assets.jump[0];
            }
        }

        if (anims[HURTING]) {
            int frameIndex = (int) (diff / 100);
            if (frameIndex < Assets.hit_stand_back.length) {
                return Assets.hit_stand_back[frameIndex];
            } else {
                hurting = false;
                canMove = true;
                handleAnims(IDLING);
                return Assets.idle[0];
            }
        }

        if (anims[DEAD]) {
            return Assets.dead[0];
        }

        return Assets.idle[0];
    }

    public void handleAnims(int newAnim) {
        // Don't interrupt certain animations
        if (hurting && newAnim != HURTING && newAnim != DEAD) return;
        if (currentState == DEAD && newAnim != DEAD) return;

        currentState = newAnim;
        for (int i = 0; i < anims.length; i++) {
            anims[i] = (i == newAnim);
        }
        lastAnimTime = System.currentTimeMillis();
    }

    private void checkWalls() {
        if (x < 0) {
            x = 0;
        } else if (x + 60 > 800) { // Screen width
            x = 740;
        }
    }

    public Rectangle2D getHitBounds() {
        if (anims[CROUCHING] || anims[ATTACKING_C_G]) {
            return new Rectangle2D(x, y + 30, 60, 80);
        }
        return new Rectangle2D(x, y, 60, 110);
    }

    public Rectangle2D getAttackBounds() {
        if (anims[ATTACKING_G]) return new Rectangle2D(x + (facingRight ? 40 : -60), y + 10, 60, 30);
        if (anims[ATTACKING_H]) return new Rectangle2D(x + (facingRight ? 40 : -60), y + 10, 60, 30);
        if (anims[ATTACKING_B]) return new Rectangle2D(x + (facingRight ? 60 : -60), y, 60, 50);
        if (anims[ATTACKING_N]) return new Rectangle2D(x + (facingRight ? 60 : -60), y + 50, 60, 50);
        if (anims[ATTACKING_C_G]) return new Rectangle2D(x + (facingRight ? 30 : -60), y + 40, 60, 30);
        if (anims[ATTACKING_A_G]) return new Rectangle2D(x + (facingRight ? 30 : -60), y + 20, 60, 50);
        if (anims[ATTACKING_A_H]) return new Rectangle2D(x + (facingRight ? 30 : -60), y + 20, 60, 50);
        if (anims[ATTACKING_A_B]) return new Rectangle2D(x + (facingRight ? 40 : -60), y + 40, 60, 30);
        return new Rectangle2D(0, 0, 0, 0);
    }

    // Face opponent
    public void faceOpponent(Entity opponent) {
        facingRight = (opponent.x > this.x);
    }

    // Getters
    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }
    public double getRyuX() { return x; }
    public boolean isFacingRight() { return facingRight; }
    public boolean isDead() { return currentState == DEAD; }
    public boolean isAttacking() { return isAttacking; }
    public int getCurrentState() { return currentState; }
    public String getName() { return "RYU"; }
}