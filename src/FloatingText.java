import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

public class FloatingText {
    public int x, y;
    public String text;
    public Color color;
    public int lifeTime;
    public int maxLifeTime = 120; // 2 seconds
    public boolean active = true;

    public FloatingText(int x, int y, String text, Color color) {
        this.x = x;
        this.y = y;
        this.text = text;
        this.color = color;
        this.lifeTime = maxLifeTime;
    }

    public void update() {
        lifeTime--;
        y--; // Float up slowly
        if (lifeTime <= 0) {
            active = false;
        }
    }

    public void draw(Graphics2D g2, GamePanel gp) {
        int screenX = x - gp.player.worldX + gp.player.screenX;
        int screenY = y - gp.player.worldY + gp.player.screenY;

        if (screenX + gp.tileSize > -100 && screenX < gp.screenWidth + 100 &&
                screenY + gp.tileSize > -100 && screenY < gp.screenHeight + 100) {

            float alpha = (float)lifeTime / maxLifeTime;
            if (alpha < 0) alpha = 0;
            if (alpha > 1) alpha = 1;

            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 18F));

            // Shadow
            g2.setColor(new Color(0f, 0f, 0f, alpha));
            g2.drawString(text, screenX + 1, screenY + 1);

            // Text
            g2.setColor(new Color(color.getRed()/255f, color.getGreen()/255f, color.getBlue()/255f, alpha));
            g2.drawString(text, screenX, screenY);
        }
    }
}
