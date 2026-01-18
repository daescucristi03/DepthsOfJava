import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Random;

/**
 * The TileManager class handles the generation and rendering of the game world (dungeon).
 * It uses a procedural generation algorithm to create a unique map layout for each run.
 */
public class TileManager {

    GamePanel gp;
    /** 2D array representing the map layout. 0 = Floor, 1 = Wall. */
    public int[][] mapTileNum;
    
    // Palette
    Color floorColor = new Color(20, 20, 30);
    Color wallColor = new Color(40, 40, 60);
    Color wallBorder = new Color(60, 60, 90);
    
    /**
     * Constructor for TileManager.
     * Initializes the map array and generates the dungeon.
     * 
     * @param gp The GamePanel instance.
     */
    public TileManager(GamePanel gp) {
        this.gp = gp;
        mapTileNum = new int[gp.maxWorldCol][gp.maxWorldRow];
        generateDungeon();
    }
    
    /**
     * Generates a procedural dungeon using a Random Walker algorithm.
     * It starts with a map full of walls and carves out floor tiles.
     */
    public void generateDungeon() {
        // Initialize all as walls (1)
        for (int col = 0; col < gp.maxWorldCol; col++) {
            for (int row = 0; row < gp.maxWorldRow; row++) {
                mapTileNum[col][row] = 1;
            }
        }
        
        // Improved Random Walker to create larger rooms
        int x = gp.maxWorldCol / 2;
        int y = gp.maxWorldRow / 2;
        int steps = 1500; // Increased steps
        Random rand = new Random();
        
        mapTileNum[x][y] = 0; // Start point
        
        for (int i = 0; i < steps; i++) {
            int direction = rand.nextInt(4);
            if (direction == 0) y--; // Up
            if (direction == 1) y++; // Down
            if (direction == 2) x--; // Left
            if (direction == 3) x++; // Right
            
            // Keep within bounds (leave 1 tile border)
            if (x < 2) x = 2;
            if (x > gp.maxWorldCol - 3) x = gp.maxWorldCol - 3;
            if (y < 2) y = 2;
            if (y > gp.maxWorldRow - 3) y = gp.maxWorldRow - 3;
            
            // Carve a 3x3 area instead of 1x1 to make rooms bigger
            int brushSize = 1; // Easy to adjust tunnel width now
            for (int rX = -brushSize; rX <= brushSize; rX++) {
                for (int rY = -brushSize; rY <= brushSize; rY++) {
                    mapTileNum[x + rX][y + rY] = 0;
                }
            }
        }
    }
    
    /**
     * Draws the visible portion of the map on the screen.
     * It iterates through the map array and draws tiles relative to the player's camera position.
     * 
     * @param g2 The Graphics2D context.
     */
    public void draw(Graphics2D g2) {
        int worldCol = 0;
        int worldRow = 0;

        while (worldCol < gp.maxWorldCol && worldRow < gp.maxWorldRow) {

            int tileNum = mapTileNum[worldCol][worldRow];
            
            int worldX = worldCol * gp.tileSize;
            int worldY = worldRow * gp.tileSize;
            int screenX = worldX - gp.player.worldX + gp.player.screenX;
            int screenY = worldY - gp.player.worldY + gp.player.screenY;

            // Only draw tiles visible on screen (with some buffer)
            if (worldX + gp.tileSize > gp.player.worldX - gp.player.screenX &&
                worldX - gp.tileSize < gp.player.worldX + gp.player.screenX &&
                worldY + gp.tileSize > gp.player.worldY - gp.player.screenY &&
                worldY - gp.tileSize < gp.player.worldY + gp.player.screenY) {
                
                if (tileNum == 0) {
                    g2.setColor(floorColor);
                    g2.fillRect(screenX, screenY, gp.tileSize, gp.tileSize);
                    // Subtle grid
                    g2.setColor(new Color(30, 30, 45));
                    g2.drawRect(screenX, screenY, gp.tileSize, gp.tileSize);
                } else {
                    g2.setColor(wallColor);
                    g2.fillRect(screenX, screenY, gp.tileSize, gp.tileSize);
                    // 3D effect for walls
                    g2.setColor(wallBorder);
                    g2.fillRect(screenX, screenY, gp.tileSize, gp.tileSize/4); // Top highlight
                    g2.setColor(new Color(20, 20, 30));
                    g2.fillRect(screenX, screenY + gp.tileSize - gp.tileSize/4, gp.tileSize, gp.tileSize/4); // Bottom shadow
                }
            }

            worldCol++;
            if (worldCol == gp.maxWorldCol) {
                worldCol = 0;
                worldRow++;
            }
        }
    }
}
