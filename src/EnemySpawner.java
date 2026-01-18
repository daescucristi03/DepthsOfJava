import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Random;

/**
 * The EnemySpawner class represents an entity that periodically spawns enemies.
 * It handles the timing and logic for creating new Enemy instances in the game world.
 */
public class EnemySpawner extends Entity {
    
    GamePanel gp;
    int spawnTimer = 0;
    int spawnInterval = 300; // 5 seconds at 60 FPS
    public boolean active = true;

    /**
     * Constructor for EnemySpawner.
     * 
     * @param gp The GamePanel instance.
     * @param x The x-coordinate of the spawner in the world.
     * @param y The y-coordinate of the spawner in the world.
     */
    public EnemySpawner(GamePanel gp, int x, int y) {
        this.gp = gp;
        this.x = x;
        this.y = y;
    }

    /**
     * Updates the spawner logic.
     * Increments the timer and spawns an enemy if the interval is reached and the spawner is active.
     */
    public void update() {
        if (!active) return;
        
        spawnTimer++;
        if (spawnTimer >= spawnInterval) {
            spawnEnemy();
            spawnTimer = 0;
        }
    }
    
    /**
     * Spawns a new enemy at the spawner's location.
     * Checks if the total number of enemies is below the limit before spawning.
     * Randomly decides if the enemy is ranged or melee.
     */
    private void spawnEnemy() {
        if (gp.enemies.size() < 20) { // Limit total enemies
            Random rand = new Random();
            boolean ranged = rand.nextBoolean();
            // Pass false for isBoss
            gp.enemies.add(new Enemy(gp, x, y, ranged, gp.difficultyLevel, false));
            System.out.println("Spawned enemy at " + x + ", " + y);
        }
    }

    /**
     * Draws the spawner on the screen.
     * Only draws if the spawner is within the player's view (camera).
     * 
     * @param g2 The Graphics2D context.
     */
    public void draw(Graphics2D g2) {
        int screenX = x - gp.player.worldX + gp.player.screenX;
        int screenY = y - gp.player.worldY + gp.player.screenY;

        if (x + gp.tileSize > gp.player.worldX - gp.player.screenX &&
            x - gp.tileSize < gp.player.worldX + gp.player.screenX &&
            y + gp.tileSize > gp.player.worldY - gp.player.screenY &&
            y - gp.tileSize < gp.player.worldY + gp.player.screenY) {
            
            if (active) {
                g2.setColor(new Color(100, 0, 100)); // Purple spawner
            } else {
                g2.setColor(new Color(50, 50, 50)); // Inactive Grey
            }
            g2.fillRect(screenX, screenY, gp.tileSize, gp.tileSize);
        }
    }
}
