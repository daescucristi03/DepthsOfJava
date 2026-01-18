import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.BasicStroke;
import java.util.Random;

public class Enemy extends Entity {
    
    public int hp;
    public int maxHp;
    public boolean ranged;
    public int damage;
    GamePanel gp;
    
    public int attackCooldown = 0;
    
    // Attack Visuals
    public boolean attacking = false;
    public int attackVisualCounter = 0;
    
    // Ranged Logic
    public int shotMode = 0; // 0: Single, 1: V-Shape
    public int shotTimer = 0;
    
    // Pushback Logic
    public boolean beingPushed = false;
    public double pushDirection = 0;
    public int pushDuration = 0;
    public int pushSpeed = 5;
    
    // Boss Logic
    public boolean isBoss = false;
    public int bossAction = 0; // 0: Idle, 1: Jump, 2: Rapid Fire, 3: 360 Shot, 4: Dash
    public int bossActionTimer = 0;
    public int bossPhaseTimer = 0;
    public boolean bossInAir = false;
    public int bossTargetX, bossTargetY;

    public Enemy(GamePanel gp, int x, int y, boolean ranged, int difficultyLevel, boolean isBoss) {
        this.gp = gp;
        this.x = x;
        this.y = y;
        this.ranged = ranged;
        this.speed = 2;
        this.alive = true;
        this.isBoss = isBoss;
        
        // Stats increase with difficulty
        double hpMultiplier = 1.0 + (difficultyLevel * 0.5); // +50% per level
        double dmgMultiplier = 1.0 + (difficultyLevel * 0.1); // +10% per level
        
        if (isBoss) {
            this.maxHp = (int)(200 * hpMultiplier); // Boss has much more HP
            this.damage = (int)(10 * dmgMultiplier);
            this.speed = 3;
            this.ranged = true; // Boss can do both, but flag helps drawing
        } else {
            this.maxHp = (int)(10 * hpMultiplier);
            this.hp = maxHp;
            this.damage = (int)(2 * dmgMultiplier);
        }
        
        this.hp = maxHp;
        
        // Ensure minimums
        if (this.damage < 1) this.damage = 1;
        
        // Randomize initial shot mode for ranged enemies
        if (ranged && !isBoss) {
            Random rand = new Random();
            shotMode = rand.nextInt(2);
        }
    }

    public void update(Player player) {
        if (!alive) return;
        
        if (isBoss) {
            updateBoss(player);
            return;
        }
        
        // Handle Pushback
        if (beingPushed) {
            int nextX = x + (int)(Math.cos(pushDirection) * pushSpeed);
            int nextY = y + (int)(Math.sin(pushDirection) * pushSpeed);
            
            if (!checkTileCollision(nextX, nextY)) {
                x = nextX;
                y = nextY;
            }
            
            pushDuration--;
            if (pushDuration <= 0) {
                beingPushed = false;
            }
            return; // Skip normal movement/attack while being pushed
        }
        
        int distance = (int) Math.sqrt(Math.pow(player.worldX - x, 2) + Math.pow(player.worldY - y, 2));
        
        if (ranged) {
            // Ranged enemies try to keep a distance
            if (distance > 250) {
                moveTowards(player.worldX, player.worldY);
            } else if (distance < 150) {
                moveAway(player.worldX, player.worldY);
            }
            
            // Ranged Attack Logic
            if (attackCooldown == 0 && distance < 400) {
                double angle = Math.atan2(player.worldY - y, player.worldX - x);
                
                if (shotMode == 0) {
                    // Single Shot
                    gp.projectiles.add(new Projectile(gp, x + gp.tileSize/2, y + gp.tileSize/2, angle, damage));
                } else {
                    // V-Shape Shot (3 projectiles)
                    gp.projectiles.add(new Projectile(gp, x + gp.tileSize/2, y + gp.tileSize/2, angle, damage));
                    gp.projectiles.add(new Projectile(gp, x + gp.tileSize/2, y + gp.tileSize/2, angle - 0.3, damage));
                    gp.projectiles.add(new Projectile(gp, x + gp.tileSize/2, y + gp.tileSize/2, angle + 0.3, damage));
                }
                
                attacking = true;
                attackCooldown = 40; // High fire rate (approx 0.66 seconds)
                
                // Switch modes occasionally
                shotTimer++;
                if (shotTimer > 5) {
                    shotMode = (shotMode == 0) ? 1 : 0;
                    shotTimer = 0;
                }
            }
            
        } else {
            // Melee enemies always chase
            moveTowards(player.worldX, player.worldY);
            
            // Melee Attack Logic (Contact)
            if (attackCooldown == 0) {
                Rectangle enemyRect = new Rectangle(x, y, gp.tileSize, gp.tileSize);
                Rectangle playerRect = new Rectangle(player.worldX, player.worldY, gp.tileSize, gp.tileSize);
                
                if (enemyRect.intersects(playerRect)) {
                    attacking = true;
                    player.takeDamage(damage);
                    
                    // Push player back
                    double angle = Math.atan2(player.worldY - y, player.worldX - x);
                    player.startPushback(angle, 10);

                    attackCooldown = 60; // 1 second cooldown
                    System.out.println("Melee Enemy hit! Player HP: " + player.hp);
                }
            }
        }
        
        // Cooldown management
        if (attackCooldown > 0) {
            attackCooldown--;
        }
        
        // Visual management
        if (attacking) {
            attackVisualCounter++;
            if (attackVisualCounter > 15) { // 0.25 seconds visual
                attacking = false;
                attackVisualCounter = 0;
            }
        }
    }
    
    private void updateBoss(Player player) {
        bossPhaseTimer++;
        
        // Boss State Machine
        if (bossAction == 0) { // Idle / Cooldown
            if (bossPhaseTimer > 60) { // 1 second idle
                Random rand = new Random();
                bossAction = rand.nextInt(4) + 1; // Pick action 1-4
                bossPhaseTimer = 0;
                bossActionTimer = 0;
                System.out.println("Boss Action: " + bossAction);
            }
        }
        else if (bossAction == 1) { // Jump Attack
            bossActionTimer++;
            if (bossActionTimer < 60) {
                // Telegraphed "Crouch" or charge
                // Visuals handled in draw
            } else if (bossActionTimer == 60) {
                // Jump "Out of map" (Visual trick)
                bossInAir = true;
                bossTargetX = player.worldX;
                bossTargetY = player.worldY;
            } else if (bossActionTimer < 120) {
                // In air, shadow follows player? Or fixed target?
                // Let's lock target at jump start for dodgeability
            } else if (bossActionTimer == 120) {
                // Land
                bossInAir = false;
                x = bossTargetX;
                y = bossTargetY;
                
                // Check hit
                Rectangle bossRect = new Rectangle(x - gp.tileSize, y - gp.tileSize, gp.tileSize*3, gp.tileSize*3); // Large AOE
                Rectangle playerRect = new Rectangle(player.worldX, player.worldY, gp.tileSize, gp.tileSize);
                
                if (bossRect.intersects(playerRect)) {
                    player.takeDamage(damage * 2);
                    double angle = Math.atan2(player.worldY - y, player.worldX - x);
                    player.startPushback(angle, 30); // Huge pushback
                }
                gp.startShake(20, 20); // Big shake
            } else if (bossActionTimer > 140) {
                bossAction = 0;
                bossPhaseTimer = 0;
            }
        }
        else if (bossAction == 2) { // Rapid Fire
            bossActionTimer++;
            if (bossActionTimer < 60) {
                // Telegraph
            } else if (bossActionTimer < 180) {
                // Fire every 10 frames
                if (bossActionTimer % 10 == 0) {
                    double angle = Math.atan2(player.worldY - y, player.worldX - x);
                    // Add some spread
                    angle += (new Random().nextDouble() - 0.5) * 0.5;
                    gp.projectiles.add(new Projectile(gp, x + gp.tileSize/2, y + gp.tileSize/2, angle, damage));
                }
            } else {
                bossAction = 0;
                bossPhaseTimer = 0;
            }
        }
        else if (bossAction == 3) { // 360 Shot
            bossActionTimer++;
            if (bossActionTimer < 60) {
                // Telegraph
            } else if (bossActionTimer == 60) {
                // Fire 360
                for (int i = 0; i < 12; i++) {
                    double angle = Math.toRadians(i * 30);
                    gp.projectiles.add(new Projectile(gp, x + gp.tileSize/2, y + gp.tileSize/2, angle, damage));
                }
            } else if (bossActionTimer > 80) {
                bossAction = 0;
                bossPhaseTimer = 0;
            }
        }
        else if (bossAction == 4) { // Dash
            bossActionTimer++;
            if (bossActionTimer < 40) {
                // Telegraph: Face player
            } else if (bossActionTimer == 40) {
                // Calculate dash vector
                double angle = Math.atan2(player.worldY - y, player.worldX - x);
                bossTargetX = (int)(Math.cos(angle) * 15); // Speed
                bossTargetY = (int)(Math.sin(angle) * 15);
            } else if (bossActionTimer < 60) {
                // Dashing
                int nextX = x + bossTargetX;
                int nextY = y + bossTargetY;
                if (!checkTileCollision(nextX, nextY)) {
                    x = nextX;
                    y = nextY;
                }
                
                // Hit check
                Rectangle bossRect = new Rectangle(x, y, gp.tileSize, gp.tileSize);
                Rectangle playerRect = new Rectangle(player.worldX, player.worldY, gp.tileSize, gp.tileSize);
                if (bossRect.intersects(playerRect)) {
                    player.takeDamage(damage);
                    double angle = Math.atan2(player.worldY - y, player.worldX - x);
                    player.startPushback(angle, 20);
                }
            } else {
                bossAction = 0;
                bossPhaseTimer = 0;
            }
        }
    }
    
    public void startPushback(double direction, int duration) {
        this.pushDirection = direction;
        this.pushDuration = duration;
        this.beingPushed = true;
    }
    
    private void moveTowards(int targetX, int targetY) {
        int nextX = x;
        int nextY = y;
        
        if (x < targetX) nextX += speed;
        if (x > targetX) nextX -= speed;
        if (y < targetY) nextY += speed;
        if (y > targetY) nextY -= speed;
        
        if (!checkTileCollision(nextX, nextY)) {
            x = nextX;
            y = nextY;
        }
    }
    
    private void moveAway(int targetX, int targetY) {
        int nextX = x;
        int nextY = y;
        
        if (x < targetX) nextX -= speed;
        if (x > targetX) nextX += speed;
        if (y < targetY) nextY -= speed;
        if (y > targetY) nextY += speed;
        
        if (!checkTileCollision(nextX, nextY)) {
            x = nextX;
            y = nextY;
        }
    }
    
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
    
    public void takeDamage(int damage) {
        hp -= damage;
        if (hp <= 0) {
            hp = 0;
            alive = false;
            
            if (isBoss) {
                gp.bossActive = false;
                gp.score += 1000; // Big bonus
                // Reactivate spawners
                for (EnemySpawner s : gp.spawners) {
                    s.active = true;
                }
            }
        }
    }

    public void draw(Graphics2D g2) {
        if (!alive) return;
        
        if (isBoss && bossInAir) {
            // Draw shadow indicating landing spot
            int screenX = bossTargetX - gp.player.worldX + gp.player.screenX;
            int screenY = bossTargetY - gp.player.worldY + gp.player.screenY;
            
            if (screenX + gp.tileSize > 0 && screenX < gp.screenWidth &&
                screenY + gp.tileSize > 0 && screenY < gp.screenHeight) {
                g2.setColor(new Color(0, 0, 0, 100));
                g2.fillOval(screenX, screenY, gp.tileSize, gp.tileSize);
                
                // Draw boss high above (off screen or just scaled up?)
                // Let's just not draw the boss body, implying it's high up
            }
            return;
        }
        
        int screenX = x - gp.player.worldX + gp.player.screenX;
        int screenY = y - gp.player.worldY + gp.player.screenY;

        if (x + gp.tileSize > gp.player.worldX - gp.player.screenX &&
            x - gp.tileSize < gp.player.worldX + gp.player.screenX &&
            y + gp.tileSize > gp.player.worldY - gp.player.screenY &&
            y - gp.tileSize < gp.player.worldY + gp.player.screenY) {
            
            if (isBoss) {
                // Boss Visuals
                g2.setColor(new Color(100, 0, 100)); // Purple Boss
                
                // Telegraphing colors
                if (bossAction == 1 && bossActionTimer < 60) g2.setColor(Color.yellow); // Jump charge
                if (bossAction == 2 && bossActionTimer < 60) g2.setColor(Color.orange); // Rapid charge
                if (bossAction == 3 && bossActionTimer < 60) g2.setColor(Color.cyan); // 360 charge
                if (bossAction == 4 && bossActionTimer < 40) g2.setColor(Color.white); // Dash charge
                
                // Big Hexagon
                int[] xPoints = {screenX + gp.tileSize/2, screenX + gp.tileSize, screenX + gp.tileSize, screenX + gp.tileSize/2, screenX, screenX};
                int[] yPoints = {screenY - gp.tileSize/2, screenY, screenY + gp.tileSize, (int)(screenY + gp.tileSize*1.5), screenY + gp.tileSize, screenY};
                
                g2.fillPolygon(xPoints, yPoints, 6);
                
                g2.setColor(Color.white);
                g2.setStroke(new BasicStroke(3));
                g2.drawPolygon(xPoints, yPoints, 6);
                g2.setStroke(new BasicStroke(1));
                
            } else if (ranged) {
                g2.setColor(new Color(200, 50, 50)); // Reddish
                // Triangle shape for ranged
                int[] xPoints = {screenX + gp.tileSize/2, screenX, screenX + gp.tileSize};
                int[] yPoints = {screenY, screenY + gp.tileSize, screenY + gp.tileSize};
                g2.fillPolygon(xPoints, yPoints, 3);
                
                // Outline
                g2.setColor(new Color(100, 0, 0));
                g2.setStroke(new BasicStroke(2));
                g2.drawPolygon(xPoints, yPoints, 3);
            } else {
                g2.setColor(new Color(200, 150, 50)); // Orange-ish
                // Diamond shape for melee
                int[] xPoints = {screenX + gp.tileSize/2, screenX + gp.tileSize, screenX + gp.tileSize/2, screenX};
                int[] yPoints = {screenY, screenY + gp.tileSize/2, screenY + gp.tileSize, screenY + gp.tileSize/2};
                g2.fillPolygon(xPoints, yPoints, 4);
                
                // Outline
                g2.setColor(new Color(100, 70, 0));
                g2.setStroke(new BasicStroke(2));
                g2.drawPolygon(xPoints, yPoints, 4);
            }
            g2.setStroke(new BasicStroke(1));
            
            // Draw HP bar
            g2.setColor(Color.black);
            g2.fillRect(screenX, screenY - 15, gp.tileSize, 5);
            g2.setColor(Color.green);
            g2.fillRect(screenX, screenY - 15, (int)((double)hp/maxHp * gp.tileSize), 5);
            
            // Draw HP Text
            g2.setColor(Color.white);
            g2.setFont(g2.getFont().deriveFont(10F));
            String hpText = hp + "/" + maxHp;
            // Center text
            int textWidth = (int)g2.getFontMetrics().getStringBounds(hpText, g2).getWidth();
            g2.drawString(hpText, screenX + gp.tileSize/2 - textWidth/2, screenY - 20);
            
            // Draw Melee Attack Visual
            if (attacking && !ranged && !isBoss) {
                g2.setColor(Color.red);
                g2.setStroke(new BasicStroke(2));
                int offset = attackVisualCounter; // Expand slightly
                g2.drawRect(screenX - offset, screenY - offset, gp.tileSize + offset*2, gp.tileSize + offset*2);
                g2.setStroke(new BasicStroke(1));
            }
        }
    }
}
