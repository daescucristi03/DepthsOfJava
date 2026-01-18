import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.BasicStroke;
import java.awt.AlphaComposite;
import java.util.ArrayList;
import java.util.Random;

/**
 * The Player class represents the user-controlled character in the game.
 * It handles movement, combat, stats, abilities (dash), and rendering.
 */
public class Player extends Entity {

    GamePanel gp;
    KeyHandler keyH;

    // Screen position (fixed at center)
    public final int screenX;
    public final int screenY;

    // World position
    public int worldX, worldY;

    // Stats
    public int hp;
    public int maxHp;
    public int armor;
    public int damage;

    // Attack
    public int baseAttackRange;
    public int attackRange;
    public int rangePotionTimer = 0;
    public boolean attacking = false;
    public int attackCounter = 0;
    public int attackDuration = 10; // Faster attack (was 20)

    // Pushback Logic
    public boolean beingPushed = false;
    public double pushDirection = 0;
    public int pushDuration = 0;
    public int pushSpeed = 5;

    // Dash Logic
    public boolean dashing = false;
    public int dashCounter = 0;
    public int dashCooldown = 0;
    public int dashSpeed = 12;
    public int dashDuration = 10;
    public int dashCooldownDuration = 60; // 1 second

    // Invulnerability Logic
    public boolean invincible = false;
    public int invincibleTimer = 0;

    // Message for pickups (Legacy, now using FloatingText)
    public String message = "";
    public int messageCounter = 0;

    /**
     * Constructor for Player.
     *
     * @param gp The GamePanel instance.
     * @param keyH The KeyHandler instance for input.
     */
    public Player(GamePanel gp, KeyHandler keyH) {
        this.gp = gp;
        this.keyH = keyH;

        // Center player on screen
        screenX = gp.screenWidth / 2 - (gp.tileSize / 2);
        screenY = gp.screenHeight / 2 - (gp.tileSize / 2);

        setDefaultValues();
    }

    /**
     * Sets the default values for the player's stats and state.
     */
    public void setDefaultValues() {
        // worldX and worldY will be set by GamePanel setup
        speed = 4;
        direction = "down";
        maxHp = 100;
        hp = maxHp;
        armor = 0;
        damage = 5;
        baseAttackRange = gp.tileSize * 2;
        attackRange = baseAttackRange; // Default range
        alive = true;
        beingPushed = false;
        invincible = false;
        invincibleTimer = 0;
        dashing = false;
        dashCooldown = 0;
    }

    /**
     * Updates the player's logic (movement, combat, timers).
     */
    public void update() {
        if (!alive) return;

        // Invulnerability Timer
        if (invincible) {
            invincibleTimer--;
            if (invincibleTimer <= 0) {
                invincible = false;
            }
        }

        // Handle Pushback
        if (beingPushed) {
            int nextX = worldX + (int)(Math.cos(pushDirection) * pushSpeed);
            int nextY = worldY + (int)(Math.sin(pushDirection) * pushSpeed);

            if (!checkTileCollision(nextX, nextY)) {
                worldX = nextX;
                worldY = nextY;
            }

            // Update x,y for compatibility
            x = worldX;
            y = worldY;

            pushDuration--;
            if (pushDuration <= 0) {
                beingPushed = false;
            }
            return; // Skip normal movement/attack while being pushed
        }

        // Dash Logic
        if (dashing) {
            int currentSpeed = dashSpeed;
            int nextX = worldX;
            int nextY = worldY;

            if (direction.equals("up")) nextY -= currentSpeed;
            else if (direction.equals("down")) nextY += currentSpeed;
            else if (direction.equals("left")) nextX -= currentSpeed;
            else if (direction.equals("right")) nextX += currentSpeed;

            if (!checkTileCollision(nextX, nextY)) {
                worldX = nextX;
                worldY = nextY;
            }

            dashCounter++;
            if (dashCounter > dashDuration) {
                dashing = false;
                dashCounter = 0;
            }
            return; // Skip normal movement while dashing
        }

        if (dashCooldown > 0) {
            dashCooldown--;
        }

        if (keyH.shiftPressed && dashCooldown == 0 && !dashing) {
            dashing = true;
            dashCooldown = dashCooldownDuration;
            setInvincible(dashDuration);
        }

        if (keyH.spacePressed && !attacking) {
            attacking = true;
            attackCounter = 0;
            performAttack();
        }

        if (attacking) {
            attackCounter++;
            if (attackCounter > attackDuration) {
                attacking = false;
                attackCounter = 0;
            }
        }

        // Range Potion Timer
        if (rangePotionTimer > 0) {
            rangePotionTimer--;
            if (rangePotionTimer == 0) {
                attackRange = baseAttackRange;
                gp.floatingTexts.add(new FloatingText(worldX, worldY, "Range Normal", Color.WHITE));
            }
        }

        int tempWorldX = worldX;
        int tempWorldY = worldY;
        boolean moving = false;

        if (keyH.upPressed) {
            direction = "up";
            tempWorldY -= speed;
            moving = true;
        }
        if (keyH.downPressed) {
            direction = "down";
            tempWorldY += speed;
            moving = true;
        }
        if (keyH.leftPressed) {
            direction = "left";
            tempWorldX -= speed;
            moving = true;
        }
        if (keyH.rightPressed) {
            direction = "right";
            tempWorldX += speed;
            moving = true;
        }

        if (moving) {
            // Check tile collision
            if (!checkTileCollision(tempWorldX, tempWorldY)) {
                worldX = tempWorldX;
                worldY = tempWorldY;
            }
        }

        // Update x,y for compatibility with Entity logic (though we use worldX/Y now)
        x = worldX;
        y = worldY;

        // Check collision with loot boxes
        checkLootBoxCollision();
    }

    /**
     * Initiates a pushback effect on the player.
     *
     * @param direction The angle of the pushback in radians.
     * @param duration The duration of the pushback in frames.
     */
    public void startPushback(double direction, int duration) {
        this.pushDirection = direction;
        this.pushDuration = duration;
        this.beingPushed = true;
    }

    /**
     * Makes the player invincible for a specified duration.
     *
     * @param duration The duration of invincibility in frames.
     */
    public void setInvincible(int duration) {
        this.invincible = true;
        this.invincibleTimer = duration;
    }

    /**
     * Checks for collision with map tiles.
     *
     * @param nextWorldX The potential next x-coordinate.
     * @param nextWorldY The potential next y-coordinate.
     * @return True if a collision occurs, false otherwise.
     */
    private boolean checkTileCollision(int nextWorldX, int nextWorldY) {
        // Simple 4-corner collision check
        int leftCol = nextWorldX / gp.tileSize;
        int rightCol = (nextWorldX + gp.tileSize - 1) / gp.tileSize;
        int topRow = nextWorldY / gp.tileSize;
        int bottomRow = (nextWorldY + gp.tileSize - 1) / gp.tileSize;

        if (leftCol < 0 || rightCol >= gp.maxWorldCol || topRow < 0 || bottomRow >= gp.maxWorldRow) {
            return true; // Out of bounds
        }

        int tileNum1 = gp.tileM.mapTileNum[leftCol][topRow];
        int tileNum2 = gp.tileM.mapTileNum[rightCol][topRow];
        int tileNum3 = gp.tileM.mapTileNum[leftCol][bottomRow];
        int tileNum4 = gp.tileM.mapTileNum[rightCol][bottomRow];

        if (tileNum1 == 1 || tileNum2 == 1 || tileNum3 == 1 || tileNum4 == 1) {
            return true; // Collision with wall
        }

        return false;
    }

    /**
     * Performs an attack, checking for collisions with enemies within range.
     */
    private void performAttack() {
        Rectangle attackArea = new Rectangle(worldX - attackRange/2, worldY - attackRange/2, attackRange + gp.tileSize, attackRange + gp.tileSize);

        // Use a copy of the list to avoid ConcurrentModificationException
        // because addScore() might trigger spawnBoss() which clears the enemies list
        for (Enemy enemy : new ArrayList<>(gp.enemies)) {
            if (enemy.alive) {
                Rectangle enemyRect = new Rectangle(enemy.x, enemy.y, gp.tileSize, gp.tileSize);
                if (attackArea.intersects(enemyRect)) {
                    enemy.takeDamage(damage);
                    pushBack(enemy);

                    // Add Score only if not boss
                    if (!enemy.isBoss) {
                        gp.addScore(10);
                        if (!enemy.alive) {
                            gp.addScore(50); // Bonus for kill
                        }
                    }

                    System.out.println("Hit enemy! HP: " + enemy.hp);
                }
            }
        }
    }

    /**
     * Pushes an enemy back away from the player.
     *
     * @param enemy The enemy to push back.
     */
    private void pushBack(Enemy enemy) {
        double angle = Math.atan2(enemy.y - worldY, enemy.x - worldX);
        enemy.startPushback(angle, 10); // Push for 10 frames
    }

    /**
     * Checks for collision with loot boxes and applies their effects.
     */
    private void checkLootBoxCollision() {
        Rectangle playerRect = new Rectangle(worldX, worldY, gp.tileSize, gp.tileSize);
        for (LootBox box : gp.lootBoxes) {
            if (!box.opened) {
                Rectangle boxRect = new Rectangle(box.x, box.y, gp.tileSize, gp.tileSize);
                if (playerRect.intersects(boxRect)) {
                    box.opened = true;
                    applyLootEffect(box.lootItem);
                }
            }
        }
    }

    /**
     * Applies the effect of a looted item.
     *
     * @param item The name of the item.
     */
    private void applyLootEffect(String item) {
        if (item.equals("Weapon")) {
            damage += 2;
            gp.floatingTexts.add(new FloatingText(worldX, worldY, "Damage Up!", Color.ORANGE));
        } else if (item.equals("Armor")) {
            armor += 1;
            gp.floatingTexts.add(new FloatingText(worldX, worldY, "Armor Up!", Color.GRAY));
        } else if (item.equals("Potion")) {
            hp += 20;
            if (hp > maxHp) hp = maxHp;
            gp.floatingTexts.add(new FloatingText(worldX, worldY, "HP Restored!", Color.GREEN));
        } else if (item.equals("Range Potion")) {
            Random rand = new Random();
            // 50% to 250% increase
            double increase = 0.5 + (rand.nextDouble() * 2.0);
            int addedRange = (int)(baseAttackRange * increase);
            attackRange = baseAttackRange + addedRange;

            // 5 to 7 seconds (300 to 420 frames at 60 FPS)
            rangePotionTimer = 300 + rand.nextInt(121);

            gp.floatingTexts.add(new FloatingText(worldX, worldY, "Range Up! (" + (int)(increase * 100) + "%)", Color.CYAN));
        }
        System.out.println("Picked up: " + item);
    }

    /**
     * Handles taking damage from an enemy or hazard.
     *
     * @param incomingDamage The raw damage amount.
     */
    public void takeDamage(int incomingDamage) {
        if (invincible) return; // No damage if invincible

        // Cap damage reduction at 95%
        int reduction = Math.min(armor, (int)(incomingDamage * 0.95));
        int actualDamage = incomingDamage - reduction;

        // Ensure at least 1 damage if incoming > 0
        if (actualDamage < 1 && incomingDamage > 0) actualDamage = 1;

        hp -= actualDamage;
        gp.startShake(10, 20); // Keep shake on taking damage

        if (hp <= 0) {
            hp = 0;
            alive = false;
            gp.gameState = gp.gameOverState;
            gp.commandNum = 0; // Reset menu selection for Game Over screen
        }
    }

    /**
     * Draws the player and related UI elements (messages, timers).
     *
     * @param g2 The Graphics2D context.
     */
    public void draw(Graphics2D g2) {
        if (!alive) return;

        // Visual effect for invincibility (blinking)
        if (invincible) {
            if (invincibleTimer % 20 > 10) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
            }
        }

        // Dash Trail (Simple)
        if (dashing) {
            g2.setColor(new Color(255, 255, 255, 100));
            g2.fillOval(screenX - (int)(Math.cos(Math.toRadians(getAngleFromDirection())) * 20),
                    screenY - (int)(Math.sin(Math.toRadians(getAngleFromDirection())) * 20),
                    gp.tileSize, gp.tileSize);
        }

        // Draw Player (Stylized)
        g2.setColor(Color.white);
        g2.fillOval(screenX, screenY, gp.tileSize, gp.tileSize); // Round player

        // Outline
        g2.setColor(Color.lightGray);
        g2.setStroke(new BasicStroke(2));
        g2.drawOval(screenX, screenY, gp.tileSize, gp.tileSize);
        g2.setStroke(new BasicStroke(1));

        // Reset composite
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

        if (attacking) {
            g2.setColor(new Color(200, 200, 255, 100)); // Bluish attack

            // Calculate current size based on animation progress
            double progress = (double)attackCounter / attackDuration;
            int currentRange = (int)(attackRange * progress);
            int currentSize = currentRange + gp.tileSize;

            // Center the growing circle
            int drawX = screenX + gp.tileSize/2 - currentSize/2;
            int drawY = screenY + gp.tileSize/2 - currentSize/2;

            g2.fillOval(drawX, drawY, currentSize, currentSize);

            // Draw border
            g2.setColor(new Color(200, 200, 255));
            g2.drawOval(drawX, drawY, currentSize, currentSize);
        }

        // Draw Range Timer if active
        if (rangePotionTimer > 0) {
            g2.setColor(Color.cyan);
            g2.setFont(g2.getFont().deriveFont(14F));
            String timerText = "Range Boost: " + (rangePotionTimer/60 + 1) + "s";
            g2.drawString(timerText, screenX - 20, screenY - 40);
        }

        // Draw Invincibility Timer
        if (invincible) {
            g2.setColor(Color.green);
            g2.setFont(g2.getFont().deriveFont(14F));
            String timerText = "Shield: " + (invincibleTimer/60 + 1) + "s";
            g2.drawString(timerText, screenX - 20, screenY - 60);
        }

        // Draw Dash Cooldown
        if (dashCooldown > 0) {
            g2.setColor(Color.gray);
            g2.fillRect(screenX, screenY + gp.tileSize + 5, gp.tileSize, 5);
            g2.setColor(Color.white);
            g2.fillRect(screenX, screenY + gp.tileSize + 5, (int)((double)(dashCooldownDuration - dashCooldown)/dashCooldownDuration * gp.tileSize), 5);
        }
    }

    /**
     * Helper method to get angle from direction string.
     * @return Angle in degrees.
     */
    private double getAngleFromDirection() {
        if (direction.equals("right")) return 0;
        if (direction.equals("down")) return 90;
        if (direction.equals("left")) return 180;
        if (direction.equals("up")) return 270;
        return 0;
    }
}
