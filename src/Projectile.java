import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * The Projectile class represents a projectile fired by an enemy (or potentially the player).
 * It handles movement, collision detection with walls and the player, and rendering.
 */
public class Projectile extends Entity {

    GamePanel gp;
    double dx, dy;
    boolean active;
    int damage;

    /**
     * Constructor for Projectile.
     * 
     * @param gp The GamePanel instance.
     * @param startX The starting x-coordinate.
     * @param startY The starting y-coordinate.
     * @param angle The angle of trajectory in radians.
     * @param damage The damage this projectile deals.
     */
    public Projectile(GamePanel gp, int startX, int startY, double angle, int damage) {
        this.gp = gp;
        this.x = startX;
        this.y = startY;
        this.damage = damage;
        this.speed = 6; // Projectile speed
        this.active = true;

        // Calculate velocity based on angle
        this.dx = Math.cos(angle) * speed;
        this.dy = Math.sin(angle) * speed;
    }

    /**
     * Updates the projectile's position and checks for collisions.
     */
    public void update() {
        x += dx;
        y += dy;

        // Deactivate if out of bounds (World bounds now)
        if (x < 0 || x > gp.worldWidth || y < 0 || y > gp.worldHeight) {
            active = false;
        }
        
        // Check collision with walls
        int col = (int)x / gp.tileSize;
        int row = (int)y / gp.tileSize;
        if (col >= 0 && col < gp.maxWorldCol && row >= 0 && row < gp.maxWorldRow) {
            if (gp.tileM.mapTileNum[col][row] == 1) {
                active = false;
            }
        }
        
        // Check collision with player
        if (active) {
            Rectangle projRect = new Rectangle(x, y, 10, 10); // Projectile size 10x10
            Rectangle playerRect = new Rectangle(gp.player.worldX, gp.player.worldY, gp.tileSize, gp.tileSize);
            
            if (projRect.intersects(playerRect)) {
                gp.player.takeDamage(damage);
                active = false;
                System.out.println("Player hit by projectile!");
            }
        }
    }

    /**
     * Draws the projectile on the screen.
     * Only draws if the projectile is within the player's view (camera).
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
            
            g2.setColor(Color.yellow);
            g2.fillOval(screenX, screenY, 10, 10);
        }
    }
}
