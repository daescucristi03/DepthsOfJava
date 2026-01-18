import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.Random;

public class DamageNumber {
    public double x, y;
    public int value;
    public int lifeTime;
    public int maxLifeTime = 60; // 1 second at 60 FPS
    public boolean active = true;
    
    private double motionX, motionY;
    
    public DamageNumber(int startX, int startY, int value) {
        this.value = value;
        this.lifeTime = maxLifeTime;
        
        Random rand = new Random();
        // Random offset for start location (jitter) - Increased spread
        this.x = startX + rand.nextInt(80) - 40; 
        this.y = startY + rand.nextInt(80) - 40;
        
        // Random direction "pop" - Increased speed
        double speed = 6 + rand.nextDouble() * 6; // Speed between 6 and 12
        double angle = rand.nextDouble() * Math.PI * 2; // Random angle 0 to 360
        
        this.motionX = Math.cos(angle) * speed;
        this.motionY = Math.sin(angle) * speed;
    }
    
    public void update() {
        lifeTime--;
        
        x += motionX;
        y += motionY;
        
        // Friction to slow down the pop - Reduced friction so they travel further
        motionX *= 0.98;
        motionY *= 0.98;
        
        if (lifeTime <= 0) {
            active = false;
        }
    }
    
    public void draw(Graphics2D g2, GamePanel gp) {
        int screenX = (int)x - gp.player.worldX + gp.player.screenX;
        int screenY = (int)y - gp.player.worldY + gp.player.screenY;
        
        if (screenX + gp.tileSize > -50 && screenX < gp.screenWidth + 50 &&
            screenY + gp.tileSize > -50 && screenY < gp.screenHeight + 50) {
            
            // Calculate fade out alpha
            float alpha = (float)lifeTime / maxLifeTime;
            if (alpha < 0) alpha = 0;
            if (alpha > 1) alpha = 1;
            
            // Larger, Bold Font
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 32F));
            
            String text = "DMG " + value;
            
            // Shadow (Black with alpha)
            g2.setColor(new Color(0f, 0f, 0f, alpha));
            g2.drawString(text, screenX + 2, screenY + 2);
            
            // Text (Red with alpha)
            g2.setColor(new Color(1f, 0f, 0f, alpha));
            g2.drawString(text, screenX, screenY);
        }
    }
}
