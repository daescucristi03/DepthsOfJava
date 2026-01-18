import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.util.Random;

/**
 * The LootBox class represents a treasure chest in the game.
 * It can be opened by the player to receive random items (Weapon, Armor, Potion, Range Potion).
 */
public class LootBox extends Entity {
    
    /** Flag indicating if the box has been opened. */
    public boolean opened = false;
    /** The name of the item contained in the box. */
    public String lootItem;
    
    /**
     * Constructor for LootBox.
     * 
     * @param x The x-coordinate of the box in the world.
     * @param y The y-coordinate of the box in the world.
     */
    public LootBox(int x, int y) {
        this.x = x;
        this.y = y;
        determineLoot();
    }

    /**
     * Randomly determines the loot item contained in this box.
     */
    private void determineLoot() {
        Random rand = new Random();
        int r = rand.nextInt(4);
        if (r == 0) lootItem = "Weapon";
        else if (r == 1) lootItem = "Armor";
        else if (r == 2) lootItem = "Potion";
        else lootItem = "Range Potion";
    }

    /**
     * Legacy draw method. Not used in current implementation.
     * @param g2 Graphics context.
     * @param tileSize Tile size.
     */
    public void draw(Graphics2D g2, int tileSize) {
        // Legacy method, kept to avoid breaking if called with old signature, though GamePanel uses the new one now.
    }
    
    /**
     * Draws the loot box on the screen.
     * Only draws if the box is within the player's view (camera) and hasn't been opened.
     * 
     * @param g2 The Graphics2D context.
     * @param gp The GamePanel instance (for camera position).
     */
    public void draw(Graphics2D g2, GamePanel gp) {
        int screenX = x - gp.player.worldX + gp.player.screenX;
        int screenY = y - gp.player.worldY + gp.player.screenY;

        if (x + gp.tileSize > gp.player.worldX - gp.player.screenX &&
            x - gp.tileSize < gp.player.worldX + gp.player.screenX &&
            y + gp.tileSize > gp.player.worldY - gp.player.screenY &&
            y - gp.tileSize < gp.player.worldY + gp.player.screenY) {
            
            if (!opened) {
                // Stylized Chest
                
                // Base (Dark Brown)
                g2.setColor(new Color(100, 50, 0));
                g2.fillRect(screenX + 2, screenY + 4, gp.tileSize - 4, gp.tileSize - 8);
                
                // Lid (Lighter Brown)
                g2.setColor(new Color(150, 75, 0));
                g2.fillRect(screenX + 2, screenY + 4, gp.tileSize - 4, (gp.tileSize - 8) / 2);
                
                // Gold Trim/Lock
                g2.setColor(new Color(255, 215, 0));
                g2.setStroke(new BasicStroke(2));
                g2.drawRect(screenX + 2, screenY + 4, gp.tileSize - 4, gp.tileSize - 8);
                g2.fillRect(screenX + gp.tileSize/2 - 2, screenY + gp.tileSize/2 - 2, 4, 4); // Lock
                g2.setStroke(new BasicStroke(1));
            }
        }
    }
}
