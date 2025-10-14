package Client;

import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;

public class Fighter extends Entity {
    // Core stats
    private int health;
    private int maxHealth = 1000;
    private String characterName;
    private boolean facingRight;

    // Physics
    private double velX, velY;
    private final double GRAVITY = 0.6; // REDUCED from 0.8 for smoother air movement
    private final double JUMP_FORCE = -14; // SLIGHTLY REDUCED from -15
    private final double MOVE_SPEED = 4;
    private final double AIR_MOVE_SPEED = 2.5; // NEW: Slower air movement
    private final double GROUND_Y = 270;
    private boolean onGround = true;

    // Systems
    AnimationStateMachine animationSM;
    private InputManager inputManager;
    private String playerId;

    // Combat state
    private int blockstunTimer = 0;
    private int hitstunTimer = 0;
    private boolean invulnerable = false;
    private long invulnerabilityEnd = 0;
    private int comboCount = 0;

    // Special move
    private boolean canCancelAttack = false;
    private long lastAttackTime = 0;

    // Jump state
    private boolean jumpInitiated = false;
    private boolean canPerformAirAction = true;

    // NEW: Attack state tracking
    private boolean isPerformingGroundAttack = false;
    private long attackStartTime = 0;

    public Fighter(String characterName, float startX, float startY, String playerId, boolean facingRight) {
        super(startX, startY);
        this.characterName = characterName.toUpperCase();
        this.playerId = playerId;
        this.facingRight = facingRight;
        this.health = maxHealth;

        AssetManager assetManager = AssetManager.getInstance();
        if (!assetManager.isInitialized()) {
            assetManager.initialize();
        }

        this.animationSM = new AnimationStateMachine(this.characterName);
        this.inputManager = InputManager.getInstance();

        // Position on ground
        this.y = (float) GROUND_Y;
    }

    @Override
    public void tick() {
        updatePhysics();
        updateCombatState();
        processInput();
        updateAnimations();
    }

    private void updatePhysics() {
        if (isDead() || animationSM.getCurrentAnimationType() == AnimationStateMachine.AnimationType.WIN) {
            return;
        }

        // Apply movement
        x += (float) velX;
        y += (float) velY;

        // Gravity
        if (y < GROUND_Y) {
            velY += GRAVITY;
            onGround = false;
        } else {
            y = (float) GROUND_Y;
            velY = 0;

            if (!onGround) {
                // Just landed
                onGround = true;
                jumpInitiated = false;
                canPerformAirAction = true;
                isPerformingGroundAttack = false; // Reset attack state on landing

                // Return to idle
                AnimationStateMachine.AnimationType currentAnim = animationSM.getCurrentAnimationType();
                if (currentAnim == AnimationStateMachine.AnimationType.JUMP ||
                        currentAnim == AnimationStateMachine.AnimationType.AIR_PUNCH ||
                        currentAnim == AnimationStateMachine.AnimationType.AIR_KICK ||
                        currentAnim == AnimationStateMachine.AnimationType.PUNCH_DOWN ||
                        currentAnim == AnimationStateMachine.AnimationType.FRONT_FLIP ||
                        currentAnim == AnimationStateMachine.AnimationType.BACK_FLIP) {
                    animationSM.transition(AnimationStateMachine.AnimationType.IDLE, true);
                }
            }
        }

        // Air friction - INCREASED for smoother feel
        if (!onGround) {
            velX *= 0.95; // CHANGED from 0.98 - slower deceleration
        } else {
            // Ground friction
            if (!isMovementInputPressed() || isPerformingGroundAttack) { // NEW: Stop on attack
                velX *= 0.85;
                if (Math.abs(velX) < 0.1) velX = 0;
            }
        }

        // Boundary checks
        if (x < 0) {
            x = 0;
            velX = Math.max(0, velX);
        }
        if (x > 740) {
            x = 740;
            velX = Math.min(0, velX);
        }
    }

    private void updateCombatState() {
        // NEW: Check if attack animation finished
        if (isPerformingGroundAttack && onGround) {
            AnimationStateMachine.AnimationType currentAnim = animationSM.getCurrentAnimationType();

            // If attack animation finished, allow movement again
            if (animationSM.isAnimationFinished() ||
                    currentAnim == AnimationStateMachine.AnimationType.IDLE ||
                    currentAnim == AnimationStateMachine.AnimationType.PARRY_FORWARD ||
                    currentAnim == AnimationStateMachine.AnimationType.PARRY_BACKWARD) {
                isPerformingGroundAttack = false;
            }
        }

        if (blockstunTimer > 0) {
            blockstunTimer--;
            if (blockstunTimer == 0) {
                animationSM.transition(AnimationStateMachine.AnimationType.IDLE, true);
            }
        }

        if (hitstunTimer > 0) {
            hitstunTimer--;
            if (hitstunTimer == 0) {
                animationSM.transition(AnimationStateMachine.AnimationType.IDLE, true);
                comboCount = 0;
            }
        }

        if (invulnerable && System.currentTimeMillis() > invulnerabilityEnd) {
            invulnerable = false;
        }

        if (canCancelAttack && (System.currentTimeMillis() - lastAttackTime) > 300) {
            canCancelAttack = false;
        }
    }

    private void updateAnimations() {
        animationSM.getCurrentFrame();
    }

    private boolean isMovementInputPressed() {
        return inputManager.isActionPressed(playerId, "left") ||
                inputManager.isActionPressed(playerId, "right");
    }

    private void processInput() {
        if (hitstunTimer > 0 || blockstunTimer > 0) return;
        if (isDead()) return;

        AnimationStateMachine.AnimationType currentAnim = animationSM.getCurrentAnimationType();

        if (!onGround) {
            handleAirborneInput();
        } else {
            handleGroundInput(currentAnim);
        }
    }

    private void handleAirborneInput() {
        boolean upPressed = inputManager.isActionPressed(playerId, "up");
        boolean forwardPressed = inputManager.isActionPressed(playerId, facingRight ? "right" : "left");
        boolean backwardPressed = inputManager.isActionPressed(playerId, facingRight ? "left" : "right");

        AnimationStateMachine.AnimationType currentAnim = animationSM.getCurrentAnimationType();

        // Flips during jump
        if (canPerformAirAction && jumpInitiated && currentAnim == AnimationStateMachine.AnimationType.JUMP) {
            if (forwardPressed) {
                performFrontFlip();
                return;
            } else if (backwardPressed) {
                performBackFlip();
                return;
            }
        }

        // Air attacks
        if (canPerformAirAction && (currentAnim == AnimationStateMachine.AnimationType.JUMP ||
                currentAnim == AnimationStateMachine.AnimationType.FRONT_FLIP ||
                currentAnim == AnimationStateMachine.AnimationType.BACK_FLIP)) {

            if (inputManager.isActionPressed(playerId, "light_punch") ||
                    inputManager.isActionPressed(playerId, "heavy_punch")) {

                if (inputManager.isActionPressed(playerId, "down")) {
                    performAirAttack(AnimationStateMachine.AnimationType.PUNCH_DOWN);
                } else {
                    performAirAttack(AnimationStateMachine.AnimationType.AIR_PUNCH);
                }
            } else if (inputManager.isActionPressed(playerId, "light_kick") ||
                    inputManager.isActionPressed(playerId, "heavy_kick")) {
                performAirAttack(AnimationStateMachine.AnimationType.AIR_KICK);
            }
        }

        // Air movement - REDUCED speed
        if (inputManager.isActionPressed(playerId, "left")) {
            velX -= 0.2; // CHANGED from 0.3
            velX = Math.max(-AIR_MOVE_SPEED, velX);
        } else if (inputManager.isActionPressed(playerId, "right")) {
            velX += 0.2; // CHANGED from 0.3
            velX = Math.min(AIR_MOVE_SPEED, velX);
        }
    }

    private void handleGroundInput(AnimationStateMachine.AnimationType currentAnim) {
        // Jump input
        if (inputManager.isActionPressed(playerId, "up") && animationSM.canInterrupt()) {
            jump();
            return;
        }

        // Ground attacks - check first before movement
        handleGroundAttackInput();

        // Movement - ONLY if not attacking
        if (!isPerformingGroundAttack) {
            handleMovementInput(currentAnim);
        }

        // Block
        handleBlockInput(currentAnim);
    }

    private void handleMovementInput(AnimationStateMachine.AnimationType currentAnim) {
        boolean leftPressed = inputManager.isActionPressed(playerId, "left");
        boolean rightPressed = inputManager.isActionPressed(playerId, "right");

        if (leftPressed && !rightPressed) {
            moveLeft();
        } else if (rightPressed && !leftPressed) {
            moveRight();
        } else {
            stopMoving();
        }
    }

    private void handleBlockInput(AnimationStateMachine.AnimationType currentAnim) {
        boolean blockPressed = inputManager.isActionPressed(playerId, "block");

        if (blockPressed && animationSM.canInterrupt()) {
            block();
        } else if (!blockPressed && currentAnim == AnimationStateMachine.AnimationType.PARRY_B) {
            stand();
        }
    }

    private void handleGroundAttackInput() {
        AnimationStateMachine.AnimationType currentAnim = animationSM.getCurrentAnimationType();

        // Check attacks in priority order - ONLY if can interrupt
        if (animationSM.canInterrupt()) {
            if (inputManager.isActionPressed(playerId, "light_punch")) {
                performAttack(AnimationStateMachine.AnimationType.QUICK_PUNCH);
            } else if (inputManager.isActionPressed(playerId, "heavy_punch")) {
                // Check for uppercut (down + heavy punch)
                if (inputManager.isActionPressed(playerId, "down")) {
                    performAttack(AnimationStateMachine.AnimationType.UPPERCUT);
                } else {
                    performAttack(AnimationStateMachine.AnimationType.PUNCH);
                }
            } else if (inputManager.isActionPressed(playerId, "light_kick")) {
                performAttack(AnimationStateMachine.AnimationType.KICK_LOW);
            } else if (inputManager.isActionPressed(playerId, "heavy_kick")) {
                performAttack(AnimationStateMachine.AnimationType.UPPER_KICK);
            }
        }
    }

    // Movement methods
    private void moveLeft() {
        // NEW: Don't move if attacking
        if (isPerformingGroundAttack) {
            velX = 0;
            return;
        }

        if (animationSM.canInterrupt() && onGround) {
            velX = -MOVE_SPEED;

            AnimationStateMachine.AnimationType desired = facingRight
                    ? AnimationStateMachine.AnimationType.PARRY_BACKWARD
                    : AnimationStateMachine.AnimationType.PARRY_FORWARD;

            if (animationSM.getCurrentAnimationType() != desired) {
                animationSM.transition(desired, false);
            }
        }
    }

    private void moveRight() {
        // NEW: Don't move if attacking
        if (isPerformingGroundAttack) {
            velX = 0;
            return;
        }

        if (animationSM.canInterrupt() && onGround) {
            velX = MOVE_SPEED;

            AnimationStateMachine.AnimationType desired = facingRight
                    ? AnimationStateMachine.AnimationType.PARRY_FORWARD
                    : AnimationStateMachine.AnimationType.PARRY_BACKWARD;

            if (animationSM.getCurrentAnimationType() != desired) {
                animationSM.transition(desired, false);
            }
        }
    }

    private void stopMoving() {
        AnimationStateMachine.AnimationType currentAnim = animationSM.getCurrentAnimationType();
        if (currentAnim == AnimationStateMachine.AnimationType.PARRY_FORWARD ||
                currentAnim == AnimationStateMachine.AnimationType.PARRY_BACKWARD) {
            animationSM.transition(AnimationStateMachine.AnimationType.IDLE, false);
        }
    }

    private void jump() {
        if (onGround && animationSM.canInterrupt()) {
            velY = JUMP_FORCE;
            onGround = false;
            jumpInitiated = true;
            canPerformAirAction = true;

            // Reduced initial horizontal velocity
            if (inputManager.isActionPressed(playerId, "left")) {
                velX = -MOVE_SPEED * 0.6; // CHANGED from 0.8
            } else if (inputManager.isActionPressed(playerId, "right")) {
                velX = MOVE_SPEED * 0.6; // CHANGED from 0.8
            }

            animationSM.transition(AnimationStateMachine.AnimationType.JUMP, true);
            AudioManager.playJumpSound();
        }
    }

    private void performFrontFlip() {
        if (canPerformAirAction) {
            animationSM.transition(AnimationStateMachine.AnimationType.FRONT_FLIP, true);
            velX = facingRight ? 5 : -5; // CHANGED from 6
            velY = Math.min(velY, -4); // CHANGED from -5 - less upward boost
            canPerformAirAction = false;
        }
    }

    private void performBackFlip() {
        if (canPerformAirAction) {
            animationSM.transition(AnimationStateMachine.AnimationType.BACK_FLIP, true);
            velX = facingRight ? -5 : 5; // CHANGED from -6
            velY = Math.min(velY, -4); // CHANGED from -5 - less upward boost
            canPerformAirAction = false;
        }
    }

    private void performAirAttack(AnimationStateMachine.AnimationType attackType) {
        if (canPerformAirAction) {
            animationSM.transition(attackType, true);
            canPerformAirAction = false;
            lastAttackTime = System.currentTimeMillis();

            // Reduce momentum during air attack for better control
            velX *= 0.5; // NEW: Slow down horizontal movement during attack

            // Slight momentum adjustment based on attack
            if (attackType == AnimationStateMachine.AnimationType.PUNCH_DOWN) {
                velY = Math.max(velY, 2); // CHANGED from 3 - slower descent
            } else {
                // Slight float during other air attacks
                velY *= 0.7; // NEW: Reduce downward velocity
            }

            // Play sound
            if (attackType == AnimationStateMachine.AnimationType.AIR_KICK) {
                AudioManager.playKickSound();
            } else {
                AudioManager.playPunchSound();
            }
        }
    }

    private void stand() {
        if (animationSM.canInterrupt()) {
            animationSM.transition(AnimationStateMachine.AnimationType.IDLE, true);
        }
    }

    private void block() {
        animationSM.transition(AnimationStateMachine.AnimationType.PARRY_B, false);
        velX = 0; // NEW: Stop movement when blocking
    }

    private void performAttack(AnimationStateMachine.AnimationType attackType) {
        // NEW: Set attack state and stop movement
        isPerformingGroundAttack = true;
        velX = 0; // STOP all horizontal movement
        attackStartTime = System.currentTimeMillis();

        animationSM.transition(attackType, true);
        lastAttackTime = System.currentTimeMillis();

        CombatSystem.AttackData attackData = CombatSystem.getAttackData(attackType);
        canCancelAttack = attackData != null && attackData.canCancel;

        // Play appropriate sound
        switch (attackType) {
            case PUNCH:
            case QUICK_PUNCH:
                AudioManager.playPunchSound();
                break;
            case KICK_LOW:
            case UPPER_KICK:
                AudioManager.playKickSound();
                break;
            case UPPERCUT:
                AudioManager.playUppercutSound();
                break;
            default:
                AudioManager.playPunchSound();
                break;
        }
    }

    @Override
    public void render(GraphicsContext g) {
        Image currentFrame = animationSM.getCurrentFrame();

        if (currentFrame == null) {
            return;
        }

        // Draw shadow
        g.setFill(Color.rgb(0, 0, 0, 0.3));
        g.fillOval(x + 10, GROUND_Y + 90, 40, 8);

        // Draw character
        if (!facingRight) {
            g.save();
            g.scale(-1, 1);
            g.drawImage(currentFrame, -x - currentFrame.getWidth(), y);
            g.restore();
        } else {
            g.drawImage(currentFrame, x, y);
        }

        // NEW: Debug info (optional - remove in production)
        // drawDebugInfo(g);
    }

    // NEW: Debug method to visualize state
    private void drawDebugInfo(GraphicsContext g) {
        g.setFill(Color.YELLOW);
        g.setFont(javafx.scene.text.Font.font(10));

        String state = isPerformingGroundAttack ? "ATTACKING" : "FREE";
        g.fillText(state, x, y - 10);

        g.fillText("VelX: " + String.format("%.1f", velX), x, y - 20);
    }

    // Combat methods
    public void takeDamage(int damage, AnimationStateMachine.AnimationType attackType) {
        if (invulnerable || isDead()) return;

        CombatSystem.AttackData attackData = CombatSystem.getAttackData(attackType);
        AnimationStateMachine.AnimationType currentAnim = animationSM.getCurrentAnimationType();

        if (currentAnim == AnimationStateMachine.AnimationType.PARRY_B) {
            damage = (int)(damage * 0.25);
            blockstunTimer = attackData != null ? attackData.blockstun : 10;
        } else {
            damage = CombatSystem.calculateScaledDamage(damage, comboCount);
            comboCount++;

            AnimationStateMachine.AnimationType hitAnim;
            if (damage >= 100) {
                hitAnim = AnimationStateMachine.AnimationType.HIT_STAND_BACK;
            } else {
                hitAnim = AnimationStateMachine.AnimationType.HIT_STAND;
            }

            animationSM.transition(hitAnim, true);
            hitstunTimer = attackData != null ? attackData.hitstun : 15;

            // Knockback
            double knockback = CombatSystem.getKnockbackForce(attackType);
            velX += facingRight ? -knockback : knockback;

            if (!onGround) {
                canPerformAirAction = false;
            }

            // NEW: Cancel attack state when hit
            isPerformingGroundAttack = false;
        }

        health -= damage;
        if (health <= 0) {
            health = 0;
            die();
        }

        AudioManager.playHitSound();
    }

    private void die() {
        animationSM.transition(AnimationStateMachine.AnimationType.DEAD, true);
        isPerformingGroundAttack = false;
        velX = 0;
        AudioManager.playDeathSound();
    }

    public boolean isAttacking() {
        AnimationStateMachine.AnimationType currentType = animationSM.getCurrentAnimationType();
        return CombatSystem.getAttackData(currentType) != null;
    }

    public Rectangle2D getAttackHitbox() {
        if (!isAttacking() || !animationSM.isInActiveFrames()) {
            return new Rectangle2D(0, 0, 0, 0);
        }

        AnimationStateMachine.AnimationType currentType = animationSM.getCurrentAnimationType();
        return CombatSystem.getAttackHitbox(this, currentType, facingRight);
    }

    public Rectangle2D getHurtbox() {
        AnimationStateMachine.AnimationType currentAnim = animationSM.getCurrentAnimationType();

        if (currentAnim == AnimationStateMachine.AnimationType.JUMP) {
            return new Rectangle2D(x + 10, y + 10, 40, 80);
        }

        return new Rectangle2D(x + 10, y + 10, 40, 90);
    }

    public boolean canBeHit(Fighter attacker) {
        if (invulnerable || hitstunTimer > 0 || isDead()) return false;
        if (attacker == this) return false;

        Rectangle2D attackHitbox = attacker.getAttackHitbox();
        Rectangle2D myHurtbox = getHurtbox();

        return attackHitbox.intersects(myHurtbox) &&
                attacker.animationSM.isInActiveFrames();
    }

    public void faceOpponent(Fighter opponent) {
        if (animationSM.canInterrupt() && opponent != null) {
            boolean shouldFaceRight = (opponent.x > this.x);
            if (facingRight != shouldFaceRight) {
                facingRight = shouldFaceRight;
            }
        }
    }

    public void performWin() {
        animationSM.transition(AnimationStateMachine.AnimationType.WIN, true);
        isPerformingGroundAttack = false;
        velX = 0;

        if (playerId.equals("P1")) {
            AudioManager.playP1WinSound();
        } else {
            AudioManager.playP2WinSound();
        }
    }

    public void resetForNewRound() {
        health = maxHealth;
        comboCount = 0;
        blockstunTimer = 0;
        hitstunTimer = 0;
        invulnerable = false;
        canCancelAttack = false;
        velX = 0;
        velY = 0;
        y = (float) GROUND_Y;
        onGround = true;
        jumpInitiated = false;
        canPerformAirAction = true;
        isPerformingGroundAttack = false; // NEW
        animationSM.transition(AnimationStateMachine.AnimationType.IDLE, true);
    }

    public void handleKeyPressed(KeyCode key) {
        inputManager.handleKeyPressed(key);
    }

    public void handleKeyReleased(KeyCode key) {
        inputManager.handleKeyReleased(key);
    }

    // Getters
    @Override
    public String getName() { return characterName; }
    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }
    public boolean isFacingRight() { return facingRight; }
    public boolean isDead() { return health <= 0; }
    public AnimationStateMachine.AnimationType getCurrentAnimation() {
        return animationSM.getCurrentAnimationType();
    }
    public boolean isOnGround() { return onGround; }
    public int getComboCount() { return comboCount; }
    public boolean isInvulnerable() { return invulnerable; }
    public double getVelX() { return velX; }
    public double getVelY() { return velY; }
    public String getPlayerId() { return playerId; }
    public boolean isPerformingAttack() { return isPerformingGroundAttack; } // NEW

    public void setInvulnerable(boolean invulnerable, long duration) {
        this.invulnerable = invulnerable;
        if (invulnerable && duration > 0) {
            this.invulnerabilityEnd = System.currentTimeMillis() + duration;
        }
    }

    @Override
    public String toString() {
        return characterName + " (HP: " + health + "/" + maxHealth +
                ", Anim: " + animationSM.getCurrentAnimationType() +
                ", Attacking: " + isPerformingGroundAttack + // NEW
                ", Facing: " + (facingRight ? "Right" : "Left") +
                ", Pos: " + (int)x + "," + (int)y + ")";
    }
}