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
    private final double GRAVITY = 0.8;
    private final double JUMP_FORCE = -15;
    private final double MOVE_SPEED = 4;
    private final double GROUND_Y = 270;
    private boolean onGround = true;

    // Systems
    private AnimationStateMachine animationSM;
    private InputManager inputManager;
    private String playerId;

    // Combat state
    private int blockstunTimer = 0;
    private int hitstunTimer = 0;
    private boolean invulnerable = false;
    private long invulnerabilityEnd = 0;
    private int comboCount = 0;

    // Special move state
    private boolean canCancelAttack = false;
    private long lastAttackTime = 0;

    public Fighter(String characterName, float startX, float startY, String playerId, boolean facingRight) {
        super(startX, startY);
        this.characterName = characterName.toUpperCase();
        this.playerId = playerId;
        this.facingRight = facingRight;
        this.health = maxHealth;

        // Initialize systems
        AssetManager assetManager = AssetManager.getInstance();
        if (!assetManager.isInitialized()) {
            assetManager.initialize();
        }

        this.animationSM = new AnimationStateMachine(this.characterName);
        this.inputManager = InputManager.getInstance();

        // Position on ground
        this.y = (float) GROUND_Y;

        System.out.println("Fighter created: " + this.characterName + " at " + startX + ", " + startY);
    }

    @Override
    public void tick() {
        updatePhysics();
        updateCombatState();
        processInput();
        updateAnimations();
    }

    private void updatePhysics() {
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
                if (animationSM.getCurrentAnimationType() == AnimationStateMachine.AnimationType.JUMP) {
                    animationSM.transition(AnimationStateMachine.AnimationType.IDLE, true);
                }
            }
        }

        // Apply air resistance for smoother movement
        if (!onGround) {
            velX *= 0.98;
        } else {
            // Ground friction - only apply when no input
            if (!isMovementInputPressed()) {
                velX *= 0.85;
                if (Math.abs(velX) < 0.1) velX = 0;
            }
        }

        // Screen boundaries
        if (x < 0) {
            x = 0;
            velX = Math.max(0, velX); // Don't push further left
        }
        if (x > 740) {
            x = 740;
            velX = Math.min(0, velX); // Don't push further right
        }
    }

    private void updateCombatState() {
        // Update timers
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

        // Update invulnerability
        if (invulnerable && System.currentTimeMillis() > invulnerabilityEnd) {
            invulnerable = false;
        }

        // Update attack canceling window
        if (canCancelAttack && (System.currentTimeMillis() - lastAttackTime) > 300) {
            canCancelAttack = false;
        }
    }

    private void updateAnimations() {
        // Let the animation state machine handle transitions and auto-progressions
        animationSM.getCurrentFrame(); // This updates the state machine internally
    }

    private boolean isMovementInputPressed() {
        return inputManager.isActionPressed(playerId, "left") ||
                inputManager.isActionPressed(playerId, "right");
    }

    private void processInput() {
        // Don't process input during hitstun, blockstun, or death
        if (hitstunTimer > 0 || blockstunTimer > 0 || isDead()) return;

        AnimationStateMachine.AnimationType currentAnim = animationSM.getCurrentAnimationType();

        // Movement
        handleMovementInput(currentAnim);

        // Jump
        if (inputManager.isActionPressed(playerId, "up") && onGround && animationSM.canInterrupt()) {
            jump();
        }

        // Crouch
        handleCrouchInput(currentAnim);

        // Block
        handleBlockInput(currentAnim);

        // Attacks (check in priority order)
        handleAttackInput();

        // Special moves/flips
        handleSpecialMoves();
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

    private void handleCrouchInput(AnimationStateMachine.AnimationType currentAnim) {
        boolean crouchPressed = inputManager.isActionPressed(playerId, "down");

        if (crouchPressed && onGround && animationSM.canInterrupt()) {
            crouch();
        } else if (!crouchPressed && currentAnim == AnimationStateMachine.AnimationType.CROUCH) {
            stand();
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

    private void handleAttackInput() {
        // Check attacks in priority order (fastest to slowest)
        if (inputManager.isActionPressed(playerId, "light_punch")) {
            lightPunch();
        } else if (inputManager.isActionPressed(playerId, "heavy_punch")) {
            heavyPunch();
        } else if (inputManager.isActionPressed(playerId, "light_kick")) {
            lightKick();
        } else if (inputManager.isActionPressed(playerId, "heavy_kick")) {
            heavyKick();
        }
    }

    private void handleSpecialMoves() {
        // Flips require directional input + up
        boolean upPressed = inputManager.isActionPressed(playerId, "up");
        boolean forwardPressed = inputManager.isActionPressed(playerId, facingRight ? "right" : "left");
        boolean backwardPressed = inputManager.isActionPressed(playerId, facingRight ? "left" : "right");

        if (upPressed && forwardPressed && animationSM.canInterrupt()) {
            frontFlip();
        } else if (upPressed && backwardPressed && animationSM.canInterrupt()) {
            backFlip();
        }
    }

    // Movement methods
    private void moveLeft() {
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
        // Don't immediately stop velocity, let physics handle it
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
            animationSM.transition(AnimationStateMachine.AnimationType.JUMP, true);
        }
    }

    private void crouch() {
        velX = 0;
        if (animationSM.getCurrentAnimationType() != AnimationStateMachine.AnimationType.CROUCH) {
            animationSM.transition(AnimationStateMachine.AnimationType.CROUCH, true);
        }
    }

    private void stand() {
        if (animationSM.canInterrupt()) {
            animationSM.transition(AnimationStateMachine.AnimationType.IDLE, true);
        }
    }

    private void block() {
        animationSM.transition(AnimationStateMachine.AnimationType.PARRY_B, false);
    }

    // Attack methods with improved canceling logic
    private void lightPunch() {
        AnimationStateMachine.AnimationType currentAnim = animationSM.getCurrentAnimationType();

        // Check if we can cancel current attack into light punch
        if (canCancelAttack && CombatSystem.canCancelInto(currentAnim, AnimationStateMachine.AnimationType.QUICK_PUNCH)) {
            performAttack(AnimationStateMachine.AnimationType.QUICK_PUNCH);
            return;
        }

        if (animationSM.canInterrupt()) {
            if (onGround) {
                if (currentAnim == AnimationStateMachine.AnimationType.CROUCH) {
                    performAttack(AnimationStateMachine.AnimationType.CROUCH_PUNCH);
                } else {
                    performAttack(AnimationStateMachine.AnimationType.QUICK_PUNCH);
                }
            } else {
                performAttack(AnimationStateMachine.AnimationType.AIR_PUNCH);
            }
        }
    }

    private void heavyPunch() {
        AnimationStateMachine.AnimationType currentAnim = animationSM.getCurrentAnimationType();

        // Check if we can cancel current attack into heavy punch
        if (canCancelAttack && CombatSystem.canCancelInto(currentAnim, AnimationStateMachine.AnimationType.PUNCH)) {
            performAttack(AnimationStateMachine.AnimationType.PUNCH);
            return;
        }

        if (animationSM.canInterrupt()) {
            if (onGround) {
                performAttack(AnimationStateMachine.AnimationType.PUNCH);
            } else {
                performAttack(AnimationStateMachine.AnimationType.PUNCH_DOWN);
            }
        }
    }

    private void lightKick() {
        if (animationSM.canInterrupt()) {
            if (onGround) {
                performAttack(AnimationStateMachine.AnimationType.KICK_LOW);
            } else {
                performAttack(AnimationStateMachine.AnimationType.AIR_KICK);
            }
        }
    }

    private void heavyKick() {
        AnimationStateMachine.AnimationType currentAnim = animationSM.getCurrentAnimationType();

        if (canCancelAttack && CombatSystem.canCancelInto(currentAnim, AnimationStateMachine.AnimationType.UPPER_KICK)) {
            performAttack(AnimationStateMachine.AnimationType.UPPER_KICK);
            return;
        }

        if (animationSM.canInterrupt()) {
            performAttack(AnimationStateMachine.AnimationType.UPPER_KICK);
        }
    }

    private void performAttack(AnimationStateMachine.AnimationType attackType) {
        animationSM.transition(attackType, true);
        lastAttackTime = System.currentTimeMillis();

        CombatSystem.AttackData attackData = CombatSystem.getAttackData(attackType);
        canCancelAttack = attackData != null && attackData.canCancel;

        System.out.println(characterName + " performed " + attackType);
    }

    // Special movement
    private void frontFlip() {
        if (animationSM.canInterrupt() && onGround) {
            animationSM.transition(AnimationStateMachine.AnimationType.FRONT_FLIP, true);
            velX = facingRight ? 6 : -6;
            System.out.println(characterName + " performed front flip!");
        }
    }

    private void backFlip() {
        if (animationSM.canInterrupt() && onGround) {
            animationSM.transition(AnimationStateMachine.AnimationType.BACK_FLIP, true);
            velX = facingRight ? -6 : 6;
            System.out.println(characterName + " performed back flip!");
        }
    }

    @Override
    public void render(GraphicsContext g) {
        Image currentFrame = animationSM.getCurrentFrame();

        if (currentFrame == null) {
            // Fallback rendering with character-specific colors
            Color characterColor = characterName.equals("RYU") ? Color.RED : Color.BLUE;
            g.setFill(characterColor);
            g.fillRect(x, y, 60, 100);
            g.setFill(Color.WHITE);
            g.fillText(characterName, x, y - 10);
            return;
        }

        // Draw shadow
        g.setFill(Color.rgb(0, 0, 0, 0.3));
        g.fillOval(x + 10, GROUND_Y + 50, 40, 8);

        // Flash effects for different states
        if (invulnerable || hitstunTimer > 0 || blockstunTimer > 0) {
            g.setGlobalAlpha(0.7);

            if (invulnerable) {
                g.setFill(Color.CYAN);
            } else if (hitstunTimer > 0) {
                g.setFill(Color.RED);
            } else if (blockstunTimer > 0) {
                g.setFill(Color.YELLOW);
            }

            g.fillRect(x - 2, y - 2, currentFrame.getWidth() + 4, currentFrame.getHeight() + 4);
            g.setGlobalAlpha(1.0);
        }

        // Draw character sprite (flip if facing left)
        if (!facingRight) {
            g.save();
            g.scale(-1, 1);
            g.drawImage(currentFrame, -x - currentFrame.getWidth(), y);
            g.restore();
        } else {
            g.drawImage(currentFrame, x, y);
        }

        // Debug info (can be toggled)
        renderDebugInfo(g);
    }

    private void renderDebugInfo(GraphicsContext g) {
        g.setFill(Color.YELLOW);
        g.fillText(animationSM.getCurrentAnimationType().toString(), x, y - 20);

        // Show frame info
        g.fillText("F:" + animationSM.getCurrentFrameIndex(), x, y - 35);

        // Show combat state
        if (hitstunTimer > 0) g.fillText("HITSTUN:" + hitstunTimer, x, y - 50);
        if (blockstunTimer > 0) g.fillText("BLOCKSTUN:" + blockstunTimer, x, y - 50);
        if (canCancelAttack) g.fillText("CANCEL", x + 70, y - 20);
    }

    // Combat methods
    public void takeDamage(int damage, AnimationStateMachine.AnimationType attackType) {
        if (invulnerable || isDead()) return;

        CombatSystem.AttackData attackData = CombatSystem.getAttackData(attackType);
        AnimationStateMachine.AnimationType currentAnim = animationSM.getCurrentAnimationType();

        if (currentAnim == AnimationStateMachine.AnimationType.PARRY_B) {
            // Blocking reduces damage and causes blockstun
            damage = (int)(damage * 0.25);
            blockstunTimer = attackData != null ? attackData.blockstun : 10;
            System.out.println(characterName + " blocked for " + damage + " chip damage");
        } else {
            // Normal hit
            damage = CombatSystem.calculateScaledDamage(damage, comboCount);
            comboCount++;

            // Choose hit animation based on damage and current state
            AnimationStateMachine.AnimationType hitAnim;
            if (damage >= 100) { // Heavy hit
                if (currentAnim == AnimationStateMachine.AnimationType.CROUCH) {
                    hitAnim = AnimationStateMachine.AnimationType.CROUCH_HIT_BACK;
                } else {
                    hitAnim = AnimationStateMachine.AnimationType.HIT_STAND_BACK;
                }
            } else { // Light hit
                if (currentAnim == AnimationStateMachine.AnimationType.CROUCH) {
                    hitAnim = AnimationStateMachine.AnimationType.CROUCH_HIT;
                } else {
                    hitAnim = AnimationStateMachine.AnimationType.HIT_STAND;
                }
            }

            animationSM.transition(hitAnim, true);
            hitstunTimer = attackData != null ? attackData.hitstun : 15;

            // Knockback
            double knockback = CombatSystem.getKnockbackForce(attackType);
            velX += facingRight ? -knockback : knockback;

            System.out.println(characterName + " took " + damage + " damage (combo: " + comboCount + ")");
        }

        health -= damage;
        if (health <= 0) {
            health = 0;
            die();
        }
    }

    private void die() {
        animationSM.transition(AnimationStateMachine.AnimationType.KNOCKBACK, true);
        System.out.println(characterName + " has been defeated!");
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

        if (currentAnim == AnimationStateMachine.AnimationType.CROUCH) {
            return new Rectangle2D(x + 10, y + 40, 40, 60);
        } else if (currentAnim == AnimationStateMachine.AnimationType.JUMP) {
            return new Rectangle2D(x + 10, y + 10, 40, 80);
        }

        return new Rectangle2D(x + 10, y + 10, 40, 90);
    }

    public boolean canBeHit(Fighter attacker) {
        if (invulnerable || hitstunTimer > 0 || isDead()) return false;
        if (attacker == this) return false; // Can't hit yourself

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

    // Win condition
    public void performWin() {
        animationSM.transition(AnimationStateMachine.AnimationType.WIN, true);
        System.out.println(characterName + " wins!");
    }

    // Reset for new round
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
        animationSM.transition(AnimationStateMachine.AnimationType.IDLE, true);
    }

    // Input handling
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

    // Setters for external control
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
                ", Facing: " + (facingRight ? "Right" : "Left") +
                ", Pos: " + (int)x + "," + (int)y + ")";
    }
}