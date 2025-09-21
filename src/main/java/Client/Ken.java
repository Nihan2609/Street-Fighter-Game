package Client;

import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import java.util.Random;

public class Ken extends Entity {

    // Ken: vars...
    private int health;
    private int maxHealth = 100;
    private double velX, velY;
    private boolean facingRight = false; // Ken starts facing left

    // STATES (same as Ryu)
    private final int IDLING = 0;
    private final int PARRYING_R = 1;
    private final int PARRYING_L = 2;
    private final int CROUCHING = 3;
    private final int ATTACKING_G = 4;
    private final int ATTACKING_H = 5;
    private final int ATTACKING_B = 6;
    private final int ATTACKING_N = 7;
    private final int ATTACKING_C_G = 8;
    private final int JUMPING = 9;
    private final int ATTACKING_A_G = 12;
    private final int ATTACKING_A_H = 13;
    private final int ATTACKING_A_B = 14;
    private final int HURTING = 15;
    private final int DEAD = 16;
    private final int BLOCKING = 17;

    // Physics constants
    private final double GRAV = 0.5;
    private final double JUMP_SPEED = -12;
    private final double TERMINAL_VELOCITY = 6;
    private final double GROUND_Y = 450;

    private boolean[] anims = new boolean[20];
    private long lastAnimTime;
    private int currentState = IDLING;

    // State flags
    private boolean hurting;
    private boolean isAttacking = false;
    private boolean canMove = true;
    private boolean onGround = true;
    private long lastHitTime;
    private Random rand;

    public Ken(double startX, double groundY) {
        super((float) startX, (float) groundY);
        this.y = (float) groundY;
        rand = new Random();
        health = maxHealth;
        anims[IDLING] = true;
        System.out.println("Ken created at: " + startX + ", " + groundY);
    }

    @Override
    public void tick() {
        // Same physics logic as Ryu
        x += velX;
        y += velY;

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
            if (currentState == JUMPING) {
                handleAnims(IDLING);
            }
        }

        velX *= 0.85;
        checkWalls();
    }

    @Override
    public void render(GraphicsContext g) {
        Image currentFrame = getCurrentAnimFrame();
        if (currentFrame == null) {
            // Debug: draw a rectangle if no image
            g.setFill(javafx.scene.paint.Color.BLUE);
            g.fillRect(x, y, 60, 110);
            return;
        }

        // Draw shadow
        g.setFill(javafx.scene.paint.Color.rgb(0, 0, 0, 0.5));
        g.fillOval(x + currentFrame.getWidth() / 2 - 32, GROUND_Y + 110, 64, 16);

        if (hurting) {
            g.save();
            g.translate(rand.nextInt(3) - 1.5, rand.nextInt(3) - 1.5);
        }

        // Flip image based on facing direction
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

    // Movement methods (same as Ryu)
    public void moveForward() {
        System.out.println("Ken moveForward called");
        if (canMove && onGround && currentState != CROUCHING) {
            velX = facingRight ? 2 : -2;
            if (currentState == IDLING) {
                handleAnims(facingRight ? PARRYING_R : PARRYING_L);
            }
        }
    }

    public void moveBackward() {
        System.out.println("Ken moveBackward called");
        if (canMove && onGround && currentState != CROUCHING) {
            velX = facingRight ? -2 : 2;
            if (currentState == IDLING) {
                handleAnims(facingRight ? PARRYING_L : PARRYING_R);
            }
        }
    }

    public void jump() {
        System.out.println("Ken jump called");
        if (canMove && onGround) {
            velY = JUMP_SPEED;
            onGround = false;
            handleAnims(JUMPING);
        }
    }

    public void crouch() {
        System.out.println("Ken crouch called");
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

    // Attack methods (same as Ryu)
    public void punch() {
        System.out.println("Ken punch called");
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
        System.out.println("Ken quickPunch called");
        if (canAttack() && onGround) {
            handleAnims(ATTACKING_H);
            isAttacking = true;
            canMove = false;
        }
    }

    public void kick() {
        System.out.println("Ken kick called");
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
        System.out.println("Ken uppercut called");
        if (canAttack() && onGround) {
            handleAnims(ATTACKING_B);
            isAttacking = true;
            canMove = false;
        }
    }

    public void block() {
        System.out.println("Ken block called");
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
            damage /= 2;
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
                return diff >= 200 && diff <= 400;
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
        return new Rectangle2D(opponent.x, opponent.y, 60, 110);
    }

    private Image getCurrentAnimFrame() {
        long now = System.currentTimeMillis();
        long diff = now - lastAnimTime;

        // Use Ken's sprite sheets (Assets.idle1, Assets.punch1, etc.)
        // Check if Ken sprites exist, fallback to Ryu sprites if not
        try {
            if (anims[IDLING]) {
                if (Assets.idle1 != null && Assets.idle1.length > 0) {
                    return Assets.idle1[(int) ((diff / 150) % Assets.idle1.length)];
                } else if (Assets.idle != null && Assets.idle.length > 0) {
                    return Assets.idle[(int) ((diff / 150) % Assets.idle.length)];
                }
            }

            if (anims[JUMPING]) {
                if (Assets.jump1 != null && Assets.jump1.length > 0) {
                    return Assets.jump1[(int) ((diff / 80) % Assets.jump1.length)];
                } else if (Assets.jump != null && Assets.jump.length > 0) {
                    return Assets.jump[(int) ((diff / 80) % Assets.jump.length)];
                }
            }

            // Attack animations with auto-return to idle
            if (anims[ATTACKING_G]) {
                int frameIndex = (int) (diff / 80);
                Image[] frames = (Assets.punch1 != null && Assets.punch1.length > 0) ? Assets.punch1 : Assets.punch;
                if (frames != null && frameIndex < frames.length) {
                    return frames[frameIndex];
                } else {
                    isAttacking = false;
                    canMove = true;
                    handleAnims(IDLING);
                    return getCurrentAnimFrame(); // Recursive call to get idle frame
                }
            }

            if (anims[ATTACKING_H]) {
                int frameIndex = (int) (diff / 60);
                Image[] frames = (Assets.quick_punch1 != null && Assets.quick_punch1.length > 0) ? Assets.quick_punch1 : Assets.quick_punch;
                if (frames != null && frameIndex < frames.length) {
                    return frames[frameIndex];
                } else {
                    isAttacking = false;
                    canMove = true;
                    handleAnims(IDLING);
                    return getCurrentAnimFrame();
                }
            }

            if (anims[ATTACKING_B]) {
                int frameIndex = (int) (diff / 80);
                Image[] frames = (Assets.upper_kick1 != null && Assets.upper_kick1.length > 0) ? Assets.upper_kick1 : Assets.upper_kick;
                if (frames != null && frameIndex < frames.length) {
                    return frames[frameIndex];
                } else {
                    isAttacking = false;
                    canMove = true;
                    handleAnims(IDLING);
                    return getCurrentAnimFrame();
                }
            }

            if (anims[ATTACKING_N]) {
                int frameIndex = (int) (diff / 100);
                Image[] frames = (Assets.kick_low1 != null && Assets.kick_low1.length > 0) ? Assets.kick_low1 : Assets.kick_low;
                if (frames != null && frameIndex < frames.length) {
                    return frames[frameIndex];
                } else {
                    isAttacking = false;
                    canMove = true;
                    handleAnims(IDLING);
                    return getCurrentAnimFrame();
                }
            }

            if (anims[ATTACKING_A_G]) {
                int frameIndex = (int) (diff / 80);
                Image[] frames = (Assets.air_punch1 != null && Assets.air_punch1.length > 0) ? Assets.air_punch1 : Assets.air_punch;
                if (frames != null && frameIndex < frames.length) {
                    return frames[frameIndex];
                } else {
                    isAttacking = false;
                    velX = 0;
                    handleAnims(JUMPING);
                    return getCurrentAnimFrame();
                }
            }

            if (anims[ATTACKING_A_B]) {
                int frameIndex = (int) (diff / 80);
                Image[] frames = (Assets.air_kick1 != null && Assets.air_kick1.length > 0) ? Assets.air_kick1 : Assets.air_kick;
                if (frames != null && frameIndex < frames.length) {
                    return frames[frameIndex];
                } else {
                    isAttacking = false;
                    velX = 0;
                    handleAnims(JUMPING);
                    return getCurrentAnimFrame();
                }
            }

            if (anims[DEAD]) {
                if (Assets.dead1 != null && Assets.dead1.length > 0) {
                    return Assets.dead1[0];
                } else if (Assets.dead != null && Assets.dead.length > 0) {
                    return Assets.dead[0];
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting Ken animation frame: " + e.getMessage());
        }

        // Fallback to idle
        if (Assets.idle1 != null && Assets.idle1.length > 0) {
            return Assets.idle1[0];
        } else if (Assets.idle != null && Assets.idle.length > 0) {
            return Assets.idle[0];
        }

        return null; // This will trigger the blue rectangle fallback in render()
    }

    public void handleAnims(int newAnim) {
        if (hurting && newAnim != HURTING && newAnim != DEAD) return;
        if (currentState == DEAD && newAnim != DEAD) return;

        System.out.println("Ken animation changing from " + currentState + " to " + newAnim);
        currentState = newAnim;
        for (int i = 0; i < anims.length; i++) {
            anims[i] = (i == newAnim);
        }
        lastAnimTime = System.currentTimeMillis();
    }

    private void checkWalls() {
        if (x < 0) {
            x = 0;
        } else if (x + 60 > 800) {
            x = 740;
        }
    }

    public Rectangle2D getAttackBounds() {
        if (anims[ATTACKING_G]) return new Rectangle2D(x + (facingRight ? 40 : -60), y + 10, 60, 30);
        if (anims[ATTACKING_H]) return new Rectangle2D(x + (facingRight ? 40 : -60), y + 10, 60, 30);
        if (anims[ATTACKING_B]) return new Rectangle2D(x + (facingRight ? 60 : -60), y, 60, 50);
        if (anims[ATTACKING_N]) return new Rectangle2D(x + (facingRight ? 60 : -60), y + 50, 60, 50);
        if (anims[ATTACKING_C_G]) return new Rectangle2D(x + (facingRight ? 30 : -60), y + 40, 60, 30);
        if (anims[ATTACKING_A_G]) return new Rectangle2D(x + (facingRight ? 30 : -60), y + 20, 60, 50);
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
    public boolean isFacingRight() { return facingRight; }
    public boolean isDead() { return currentState == DEAD; }
    public boolean isAttacking() { return isAttacking; }
    public int getCurrentState() { return currentState; }
    public String getName() { return "KEN"; }
}