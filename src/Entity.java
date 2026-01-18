import java.awt.image.BufferedImage;

/**
 * The Entity class serves as the base class for all game objects that have a position and movement.
 * This includes the Player, Enemies, Projectiles, LootBoxes, and Spawners.
 */
public class Entity {
    /** The x-coordinate of the entity in the world. */
    public int x;
    /** The y-coordinate of the entity in the world. */
    public int y;
    /** The movement speed of the entity. */
    public int speed;
    
    // Sprite images (currently unused in this stylized version, but kept for future sprite support)
    public BufferedImage up1, up2, down1, down2, left1, left2, right1, right2;
    
    /** The current direction the entity is facing or moving. */
    public String direction;
    
    // Animation counters (unused in current shape-based rendering)
    public int spriteCounter = 0;
    public int spriteNum = 1;
    
    /** Flag indicating if the entity is alive/active. If false, it may be removed from the game. */
    public boolean alive = true;
}
