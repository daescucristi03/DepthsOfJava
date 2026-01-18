import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.BasicStroke;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class GamePanel extends JPanel implements Runnable {

    final int originalTileSize = 16;
    final int scale = 3;
    public final int tileSize = originalTileSize * scale;
    public final int maxScreenCol = 16;
    public final int maxScreenRow = 12;
    public final int screenWidth = tileSize * maxScreenCol;
    public final int screenHeight = tileSize * maxScreenRow;

    // WORLD SETTINGS
    public final int maxWorldCol = 100;
    public final int maxWorldRow = 100;
    public final int worldWidth = tileSize * maxWorldCol;
    public final int worldHeight = tileSize * maxWorldRow;

    int FPS = 60;

    KeyHandler keyH = new KeyHandler();
    Thread gameThread;
    public TileManager tileM = new TileManager(this);
    public Player player = new Player(this, keyH);
    public Leaderboard leaderboard = new Leaderboard();

    public ArrayList<LootBox> lootBoxes = new ArrayList<>();
    public ArrayList<Enemy> enemies = new ArrayList<>();
    public ArrayList<Projectile> projectiles = new ArrayList<>();
    public ArrayList<EnemySpawner> spawners = new ArrayList<>();
    public ArrayList<DamageNumber> damageNumbers = new ArrayList<>();
    public ArrayList<FloatingText> floatingTexts = new ArrayList<>();
    public int difficultyLevel = 0;
    public int score = 0;
    public int totalScore = 0;
    public int stage = 1;
    public String playerName = "";

    // Boss Logic
    public boolean bossActive = false;
    public int nextBossScore = 5000;
    public boolean bossSpawnPending = false;
    public int bossSpawnTimer = 0;

    // Stage Transition
    public boolean stageTransitionPending = false;
    public int stageTransitionTimer = 0;

    // Stage Message
    public int stageMessageTimer = 0;

    // GAME STATE
    public int gameState;
    public int previousState; // To return from settings
    public final int titleState = 0;
    public final int playState = 1;
    public final int pauseState = 2;
    public final int settingsState = 3;
    public final int gameOverState = 4;
    public final int leaderboardState = 5;
    public final int nameInputState = 6;
    public final int controlsState = 7;

    public int commandNum = 0;

    // Settings
    public boolean musicOn = true;
    public boolean soundOn = true;
    public boolean fullScreen = false;

    // Screen Shake
    public int shakeDuration = 0;
    public int shakeMagnitude = 0;

    public GamePanel() {
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.black);
        this.setDoubleBuffered(true);
        this.addKeyListener(keyH);
        this.setFocusable(true);
    }

    public void setupGame() {
        gameState = titleState;
        score = 0;
        totalScore = 0;
        stage = 1;
        difficultyLevel = 0;
        bossActive = false;
        bossSpawnPending = false;
        stageTransitionPending = false;
        nextBossScore = 5000;

        // Find a valid spawn point for player
        placePlayerOnFloor();

        // Add Spawners
        for (int i = 0; i < 5; i++) {
            placeSpawnerOnFloor();
        }

        // Add LootBoxes
        for (int i = 0; i < 10; i++) {
            placeLootBoxOnFloor();
        }
    }

    public void startShake(int magnitude, int duration) {
        this.shakeMagnitude = magnitude;
        this.shakeDuration = duration;
    }

    private void placePlayerOnFloor() {
        Random rand = new Random();
        while(true) {
            int col = rand.nextInt(maxWorldCol);
            int row = rand.nextInt(maxWorldRow);
            if (tileM.mapTileNum[col][row] == 0) {
                player.worldX = col * tileSize;
                player.worldY = row * tileSize;
                break;
            }
        }
    }

    private void placeSpawnerOnFloor() {
        Random rand = new Random();
        while(true) {
            int col = rand.nextInt(maxWorldCol);
            int row = rand.nextInt(maxWorldRow);
            if (tileM.mapTileNum[col][row] == 0) {
                // Ensure not too close to player spawn (optional but good practice)
                spawners.add(new EnemySpawner(this, col * tileSize, row * tileSize));
                break;
            }
        }
    }

    private void placeLootBoxOnFloor() {
        Random rand = new Random();
        while(true) {
            int col = rand.nextInt(maxWorldCol);
            int row = rand.nextInt(maxWorldRow);
            if (tileM.mapTileNum[col][row] == 0) {
                lootBoxes.add(new LootBox(col * tileSize, row * tileSize));
                break;
            }
        }
    }

    public void resetGame() {
        tileM.generateDungeon(); // New dungeon
        player.setDefaultValues();
        lootBoxes.clear();
        enemies.clear();
        projectiles.clear();
        spawners.clear();
        damageNumbers.clear();
        floatingTexts.clear();
        score = 0;
        totalScore = 0;
        setupGame();
        // gameState is set to titleState by setupGame(), we will override it where needed
    }

    public void addScore(int value) {
        score += value;
        difficultyLevel = (totalScore + score) / 1000;

        // Check for Boss Spawn
        if (score >= nextBossScore && !bossActive && !bossSpawnPending && !stageTransitionPending) {
            prepareBossSpawn();
            nextBossScore += 1000;
        }
    }

    private void prepareBossSpawn() {
        bossSpawnPending = true;
        bossSpawnTimer = 300; // 5 seconds at 60 FPS

        // Kill all existing enemies
        enemies.clear();

        // Deactivate spawners
        for (EnemySpawner s : spawners) {
            s.active = false;
        }
    }

    private void spawnBoss() {
        bossActive = true;
        bossSpawnPending = false;

        // Spawn Boss near player but not on top
        // Simple logic: find a spot 5-10 tiles away
        Random rand = new Random();
        int bossX, bossY;
        while(true) {
            int col = (player.worldX / tileSize) + rand.nextInt(10) - 5;
            int row = (player.worldY / tileSize) + rand.nextInt(10) - 5;

            if (col > 0 && col < maxWorldCol && row > 0 && row < maxWorldRow && tileM.mapTileNum[col][row] == 0) {
                bossX = col * tileSize;
                bossY = row * tileSize;
                break;
            }
        }

        enemies.add(new Enemy(this, bossX, bossY, true, difficultyLevel, true));
        System.out.println("BOSS SPAWNED!");
    }

    public void bossDefeated() {
        bossActive = false;

        // Award 50% of current score as bonus
        int bonus = (int)(score * 0.50);
        score += bonus;

        totalScore += score;
        score = 0;
        difficultyLevel = (totalScore + score) / 1000;

        // Start delay before next stage
        stageTransitionPending = true;
        stageTransitionTimer = 150; // 2.5 seconds at 60 FPS
    }

    public void startNextStage() {
        stage++;
        stageTransitionPending = false;

        // Regenerate Level
        tileM.generateDungeon();

        // Clear entities
        lootBoxes.clear();
        enemies.clear();
        projectiles.clear();
        spawners.clear();
        damageNumbers.clear();
        floatingTexts.clear();

        // Place entities
        placePlayerOnFloor();
        for (int i = 0; i < 5; i++) placeSpawnerOnFloor();
        for (int i = 0; i < 10; i++) placeLootBoxOnFloor();

        // Reset Boss Logic
        bossActive = false;
        bossSpawnPending = false;
        nextBossScore = 5000;

        // Invulnerability
        player.setInvincible(1800); // 30 seconds

        // Stage Message
        stageMessageTimer = 180; // 3 seconds
    }

    public void setFullScreen() {
        // Get the window
        JFrame window = (JFrame)SwingUtilities.getWindowAncestor(this);
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

        if (fullScreen) {
            window.dispose();
            window.setUndecorated(true);
            window.setVisible(true);
            gd.setFullScreenWindow(window);
        } else {
            window.dispose();
            window.setUndecorated(false);
            window.setVisible(true);
            gd.setFullScreenWindow(null);
            window.pack();
            window.setLocationRelativeTo(null);
        }
        this.requestFocus(); // Refocus panel to keep key listener working
    }

    public void startGameThread() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        double drawInterval = 1000000000 / FPS;
        double delta = 0;
        long lastTime = System.nanoTime();
        long currentTime;

        while (gameThread != null) {
            currentTime = System.nanoTime();
            delta += (currentTime - lastTime) / drawInterval;
            lastTime = currentTime;

            if (delta >= 1) {
                update();
                repaint();
                delta--;
            }
        }
    }

    public void update() {
        if (shakeDuration > 0) {
            shakeDuration--;
        }

        if (stageMessageTimer > 0) {
            stageMessageTimer--;
        }

        if (gameState == titleState) {
            if (keyH.upPressed) {
                commandNum--;
                if (commandNum < 0) commandNum = 3;
                keyH.upPressed = false;
            }
            if (keyH.downPressed) {
                commandNum++;
                if (commandNum > 3) commandNum = 0;
                keyH.downPressed = false;
            }
            if (keyH.enterPressed) {
                if (commandNum == 0) {
                    resetGame(); // Ensure fresh game
                    gameState = nameInputState;
                    playerName = "";
                }
                if (commandNum == 1) {
                    gameState = leaderboardState;
                }
                if (commandNum == 2) {
                    previousState = titleState;
                    gameState = settingsState;
                    commandNum = 0; // Reset for settings menu
                }
                if (commandNum == 3) {
                    System.exit(0);
                }
                keyH.enterPressed = false;
            }
        }
        else if (gameState == nameInputState) {
            if (keyH.charTyped) {
                if (Character.isLetterOrDigit(keyH.lastChar) || keyH.lastChar == ' ') {
                    if (playerName.length() < 10) {
                        playerName += keyH.lastChar;
                    }
                }
                keyH.charTyped = false;
            }
            if (keyH.backspacePressed) {
                if (playerName.length() > 0) {
                    playerName = playerName.substring(0, playerName.length() - 1);
                }
                keyH.backspacePressed = false;
            }
            if (keyH.enterPressed) {
                if (playerName.length() > 0) {
                    gameState = playState;
                }
                keyH.enterPressed = false;
            }
        }
        else if (gameState == playState) {
            player.update();

            // Boss Spawn Timer
            if (bossSpawnPending) {
                bossSpawnTimer--;
                if (bossSpawnTimer <= 0) {
                    spawnBoss();
                }
            }

            // Stage Transition Timer
            if (stageTransitionPending) {
                stageTransitionTimer--;
                if (stageTransitionTimer <= 0) {
                    startNextStage();
                }
            }

            // Update Spawners
            for (EnemySpawner spawner : spawners) {
                spawner.update();
            }

            // Update Enemies
            Iterator<Enemy> enemyIt = enemies.iterator();
            while(enemyIt.hasNext()) {
                Enemy enemy = enemyIt.next();
                if (enemy.alive) {
                    enemy.update(player);
                } else {
                    enemyIt.remove();
                }
            }

            // Update Projectiles
            Iterator<Projectile> it = projectiles.iterator();
            while(it.hasNext()) {
                Projectile p = it.next();
                if(p.active) {
                    p.update();
                } else {
                    it.remove();
                }
            }

            // Update LootBoxes (Respawn logic)
            int boxesToSpawn = 0;
            Iterator<LootBox> boxIt = lootBoxes.iterator();
            while(boxIt.hasNext()) {
                LootBox box = boxIt.next();
                if (box.opened) {
                    if (isOffScreen(box)) {
                        boxIt.remove();
                        boxesToSpawn++;
                    }
                }
            }
            for(int i=0; i<boxesToSpawn; i++) {
                placeLootBoxOnFloor();
            }

            // Update Damage Numbers
            Iterator<DamageNumber> dnIt = damageNumbers.iterator();
            while(dnIt.hasNext()) {
                DamageNumber dn = dnIt.next();
                if (dn.active) {
                    dn.update();
                } else {
                    dnIt.remove();
                }
            }

            // Update Floating Texts
            Iterator<FloatingText> ftIt = floatingTexts.iterator();
            while(ftIt.hasNext()) {
                FloatingText ft = ftIt.next();
                if (ft.active) {
                    ft.update();
                } else {
                    ftIt.remove();
                }
            }

            if (keyH.escPressed) {
                gameState = pauseState;
                commandNum = 0; // Reset for pause menu
                keyH.escPressed = false;
            }
        }
        else if (gameState == pauseState) {
            if (keyH.upPressed) {
                commandNum--;
                if (commandNum < 0) commandNum = 2;
                keyH.upPressed = false;
            }
            if (keyH.downPressed) {
                commandNum++;
                if (commandNum > 2) commandNum = 0;
                keyH.downPressed = false;
            }
            if (keyH.enterPressed) {
                if (commandNum == 0) { // Resume
                    gameState = playState;
                }
                if (commandNum == 1) { // Settings
                    previousState = pauseState;
                    gameState = settingsState;
                    commandNum = 0;
                }
                if (commandNum == 2) { // Main Menu
                    gameState = titleState;
                    commandNum = 0;
                }
                keyH.enterPressed = false;
            }
        }
        else if (gameState == settingsState) {
            if (keyH.upPressed) {
                commandNum--;
                if (commandNum < 0) commandNum = 5; // Increased for Controls
                keyH.upPressed = false;
            }
            if (keyH.downPressed) {
                commandNum++;
                if (commandNum > 5) commandNum = 0;
                keyH.downPressed = false;
            }
            if (keyH.enterPressed) {
                if (commandNum == 0) {
                    musicOn = !musicOn;
                }
                if (commandNum == 1) {
                    soundOn = !soundOn;
                }
                if (commandNum == 2) {
                    fullScreen = !fullScreen;
                }
                if (commandNum == 3) { // Controls
                    gameState = controlsState;
                }
                if (commandNum == 4) { // Apply
                    setFullScreen();
                }
                if (commandNum == 5) { // Back
                    gameState = previousState;
                    commandNum = 0;
                }
                keyH.enterPressed = false;
            }
            if (keyH.escPressed) {
                gameState = previousState; // Default back to previous state
                keyH.escPressed = false;
            }
        }
        else if (gameState == controlsState) {
            if (keyH.enterPressed || keyH.escPressed) {
                gameState = settingsState;
                keyH.enterPressed = false;
                keyH.escPressed = false;
            }
        }
        else if (gameState == leaderboardState) {
            if (keyH.escPressed || keyH.enterPressed) {
                gameState = titleState;
                keyH.escPressed = false;
                keyH.enterPressed = false;
            }
        }
        else if (gameState == gameOverState) {
            if (keyH.upPressed) {
                commandNum--;
                if (commandNum < 0) commandNum = 2;
                keyH.upPressed = false;
            }
            if (keyH.downPressed) {
                commandNum++;
                if (commandNum > 2) commandNum = 0;
                keyH.downPressed = false;
            }
            if (keyH.enterPressed) {
                leaderboard.addScore(playerName, totalScore + score);

                if (commandNum == 0) { // Retry
                    resetGame();
                    // Keep same name, go straight to play
                    gameState = playState;
                }
                if (commandNum == 1) { // Change Adventurer
                    resetGame();
                    gameState = nameInputState;
                    playerName = "";
                }
                if (commandNum == 2) { // Main Menu
                    gameState = titleState;
                    commandNum = 0;
                }
                keyH.enterPressed = false;
            }
        }
    }

    private boolean isOffScreen(Entity entity) {
        int screenX = entity.x - player.worldX + player.screenX;
        int screenY = entity.y - player.worldY + player.screenY;
        int buffer = tileSize * 2;
        return screenX < -buffer || screenX > screenWidth + buffer ||
                screenY < -buffer || screenY > screenHeight + buffer;
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Handle scaling for different screen sizes
        double widthScale = (double)getWidth() / screenWidth;
        double heightScale = (double)getHeight() / screenHeight;

        // Maintain aspect ratio or stretch? Let's stretch for now as requested "size to any screen size"
        // But usually aspect ratio is better. Let's scale everything.
        g2.scale(widthScale, heightScale);

        // Apply Screen Shake
        int tx = 0;
        int ty = 0;
        if (shakeDuration > 0) {
            tx = (int)(Math.random() * shakeMagnitude - shakeMagnitude/2);
            ty = (int)(Math.random() * shakeMagnitude - shakeMagnitude/2);
            g2.translate(tx, ty);
        }

        if (gameState == titleState) {
            drawTitleScreen(g2);
        } else if (gameState == nameInputState) {
            drawNameInputScreen(g2);
        } else if (gameState == playState) {
            drawGame(g2);
        } else if (gameState == pauseState) {
            drawGame(g2);
            drawPauseScreen(g2);
        } else if (gameState == settingsState) {
            drawSettingsScreen(g2);
        } else if (gameState == controlsState) {
            drawControlsScreen(g2);
        } else if (gameState == leaderboardState) {
            drawLeaderboardScreen(g2);
        } else if (gameState == gameOverState) {
            drawGame(g2);
            drawGameOverScreen(g2);
        }

        // Reset Transform
        if (shakeDuration > 0) {
            g2.translate(-tx, -ty);
        }

        g2.dispose();
    }

    public void drawGame(Graphics2D g2) {

        tileM.draw(g2);

        // Draw Spawners
        for (EnemySpawner spawner : spawners) {
            spawner.draw(g2);
        }

        // Draw LootBoxes
        for (LootBox box : lootBoxes) {
            box.draw(g2, this);
        }

        // Draw Enemies
        for (Enemy enemy : enemies) {
            enemy.draw(g2);
        }

        // Draw Projectiles
        for (Projectile p : projectiles) {
            p.draw(g2);
        }

        player.draw(g2);

        // Draw Damage Numbers
        for (DamageNumber dn : damageNumbers) {
            dn.draw(g2, this);
        }

        // Draw Floating Texts
        for (FloatingText ft : floatingTexts) {
            ft.draw(g2, this);
        }

        // --- UI OVERLAY ---

        // Top Bar
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(0, 0, screenWidth, 40);

        // Bottom Bar
        g2.fillRect(0, screenHeight - 40, screenWidth, 40);

        // Boss Health Bar (Top Center)
        if (bossActive && !enemies.isEmpty()) {
            Enemy boss = enemies.get(0); // Boss is the only enemy
            if (boss.isBoss) {
                int barWidth = screenWidth / 2;
                int barX = screenWidth / 4;
                int barY = 10;

                g2.setColor(Color.black);
                g2.fillRect(barX, barY, barWidth, 20);
                g2.setColor(Color.red);
                g2.fillRect(barX, barY, (int)((double)boss.hp/boss.maxHp * barWidth), 20);
                g2.setColor(Color.white);
                g2.drawRect(barX, barY, barWidth, 20);
                g2.setFont(g2.getFont().deriveFont(Font.BOLD, 12F));
                g2.drawString("BOSS", barX + 5, barY + 15);
            }
        }

        // Stage (Bottom Center)
        g2.setColor(Color.white);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 20F));
        String stageText = "Stage: " + stage;
        int stageWidth = (int)g2.getFontMetrics().getStringBounds(stageText, g2).getWidth();
        g2.drawString(stageText, screenWidth/2 - stageWidth/2, screenHeight - 12);

        // Draw Stage Message (Center Screen)
        if (stageMessageTimer > 0) {
            g2.setColor(new Color(255, 255, 255, Math.min(255, stageMessageTimer * 5))); // Fade out
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 60F));
            String text = "STAGE " + stage;
            int x = getXforCenteredText(text, g2);
            int y = screenHeight / 2;
            g2.drawString(text, x, y);
        }

        // Draw Stage Complete Message
        if (stageTransitionPending) {
            g2.setColor(Color.yellow);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 50F));
            String text = "STAGE COMPLETE!";
            int x = getXforCenteredText(text, g2);
            int y = screenHeight / 2;
            g2.drawString(text, x, y);
        }

        // Player Stats (Bottom Left)
        drawPlayerStats(g2);

        // Score (Bottom Right)
        g2.setColor(Color.white);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 16F));
        String scoreText = "Score: " + score;
        int scoreWidth = (int)g2.getFontMetrics().getStringBounds(scoreText, g2).getWidth();
        g2.drawString(scoreText, screenWidth - scoreWidth - 10, screenHeight - 12);
    }

    private void drawPlayerStats(Graphics2D g2) {
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 14F));
        int uiX = 10;
        int uiY = screenHeight - 12;

        g2.setColor(Color.white);
        String stats = "HP: " + player.hp + "/" + player.maxHp + " | DMG: " + player.damage + " | ARM: " + player.armor;
        g2.drawString(stats, uiX, uiY);
    }

    public void drawTitleScreen(Graphics2D g2) {
        // Background
        g2.setColor(new Color(20, 20, 30)); // Dark blue-grey
        g2.fillRect(0, 0, screenWidth, screenHeight);

        // Grid pattern
        g2.setColor(new Color(30, 30, 45));
        for (int i = 0; i < screenWidth; i += tileSize) {
            g2.drawLine(i, 0, i, screenHeight);
        }
        for (int i = 0; i < screenHeight; i += tileSize) {
            g2.drawLine(0, i, screenWidth, i);
        }

        // Title Name
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 60F));
        String text = "Depths of Java";
        int x = getXforCenteredText(text, g2);
        int y = tileSize * 3;

        // Shadow
        g2.setColor(Color.black);
        g2.drawString(text, x+5, y+5);

        // Main Color
        g2.setColor(Color.white);
        g2.drawString(text, x, y);

        // Menu
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 28F));

        // Menu Box
        int menuX = screenWidth / 2 - tileSize * 4;
        int menuY = tileSize * 5;
        int menuWidth = tileSize * 8;
        int menuHeight = tileSize * 6;

        // Semi-transparent box behind menu
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRoundRect(menuX, menuY, menuWidth, menuHeight, 20, 20);
        g2.setColor(Color.white);
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(menuX, menuY, menuWidth, menuHeight, 20, 20);

        // Options
        text = "NEW GAME";
        x = getXforCenteredText(text, g2);
        y += tileSize * 3.5;
        g2.drawString(text, x, y);
        if (commandNum == 0) {
            g2.drawString(">", x - tileSize, y);
        }

        text = "LEADERBOARD";
        x = getXforCenteredText(text, g2);
        y += tileSize;
        g2.drawString(text, x, y);
        if (commandNum == 1) {
            g2.drawString(">", x - tileSize, y);
        }

        text = "SETTINGS";
        x = getXforCenteredText(text, g2);
        y += tileSize;
        g2.drawString(text, x, y);
        if (commandNum == 2) {
            g2.drawString(">", x - tileSize, y);
        }

        text = "QUIT";
        x = getXforCenteredText(text, g2);
        y += tileSize;
        g2.drawString(text, x, y);
        if (commandNum == 3) {
            g2.drawString(">", x - tileSize, y);
        }
    }

    public void drawNameInputScreen(Graphics2D g2) {
        g2.setColor(Color.black);
        g2.fillRect(0, 0, screenWidth, screenHeight);

        g2.setColor(Color.white);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 40F));
        String text = "ENTER YOUR NAME";
        int x = getXforCenteredText(text, g2);
        int y = screenHeight / 3;
        g2.drawString(text, x, y);

        // Input Box
        g2.setColor(Color.gray);
        g2.drawRect(screenWidth/2 - 150, screenHeight/2 - 25, 300, 50);

        g2.setColor(Color.white);
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 30F));
        x = getXforCenteredText(playerName, g2);
        g2.drawString(playerName, x, screenHeight/2 + 10);

        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 20F));
        text = "Press ENTER to start";
        x = getXforCenteredText(text, g2);
        y = screenHeight - tileSize * 3;
        g2.drawString(text, x, y);
    }

    public void drawLeaderboardScreen(Graphics2D g2) {
        g2.setColor(Color.black);
        g2.fillRect(0, 0, screenWidth, screenHeight);

        g2.setColor(Color.white);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 40F));
        String text = "LEADERBOARD";
        int x = getXforCenteredText(text, g2);
        int y = tileSize * 2;
        g2.drawString(text, x, y);

        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 20F));
        y += tileSize * 2;

        ArrayList<Leaderboard.ScoreEntry> scores = leaderboard.getScores();
        for (int i = 0; i < scores.size(); i++) {
            Leaderboard.ScoreEntry entry = scores.get(i);
            text = (i + 1) + ". " + entry.name + " - " + entry.score;
            x = getXforCenteredText(text, g2);
            g2.drawString(text, x, y);
            y += 30;
        }

        if (scores.isEmpty()) {
            text = "No scores yet!";
            x = getXforCenteredText(text, g2);
            g2.drawString(text, x, y);
        }

        text = "Press ENTER to return";
        x = getXforCenteredText(text, g2);
        y = screenHeight - tileSize * 2;
        g2.drawString(text, x, y);
    }

    public void drawPauseScreen(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(0, 0, screenWidth, screenHeight);

        g2.setColor(Color.white);
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 40F));
        String text = "PAUSED";
        int x = getXforCenteredText(text, g2);
        int y = screenHeight / 4;
        g2.drawString(text, x, y);

        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 24F));

        text = "RESUME";
        x = getXforCenteredText(text, g2);
        y += tileSize * 3;
        g2.drawString(text, x, y);
        if (commandNum == 0) {
            g2.drawString(">", x - tileSize, y);
        }

        text = "SETTINGS";
        x = getXforCenteredText(text, g2);
        y += tileSize;
        g2.drawString(text, x, y);
        if (commandNum == 1) {
            g2.drawString(">", x - tileSize, y);
        }

        text = "MAIN MENU";
        x = getXforCenteredText(text, g2);
        y += tileSize;
        g2.drawString(text, x, y);
        if (commandNum == 2) {
            g2.drawString(">", x - tileSize, y);
        }
    }

    public void drawSettingsScreen(Graphics2D g2) {
        g2.setColor(Color.black);
        g2.fillRect(0, 0, screenWidth, screenHeight);

        g2.setColor(Color.white);
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 30F));
        String text = "SETTINGS";
        int x = getXforCenteredText(text, g2);
        int y = tileSize * 2;
        g2.drawString(text, x, y);

        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 20F));

        text = "Music: " + (musicOn ? "ON" : "OFF");
        x = getXforCenteredText(text, g2);
        y += tileSize * 2;
        g2.drawString(text, x, y);
        if (commandNum == 0) {
            g2.drawString(">", x - tileSize, y);
        }

        text = "Sound: " + (soundOn ? "ON" : "OFF");
        x = getXforCenteredText(text, g2);
        y += tileSize;
        g2.drawString(text, x, y);
        if (commandNum == 1) {
            g2.drawString(">", x - tileSize, y);
        }

        text = "Fullscreen: " + (fullScreen ? "ON" : "OFF");
        x = getXforCenteredText(text, g2);
        y += tileSize;
        g2.drawString(text, x, y);
        if (commandNum == 2) {
            g2.drawString(">", x - tileSize, y);
        }

        text = "CONTROLS";
        x = getXforCenteredText(text, g2);
        y += tileSize;
        g2.drawString(text, x, y);
        if (commandNum == 3) {
            g2.drawString(">", x - tileSize, y);
        }

        text = "APPLY";
        x = getXforCenteredText(text, g2);
        y += tileSize * 2;
        g2.drawString(text, x, y);
        if (commandNum == 4) {
            g2.drawString(">", x - tileSize, y);
        }

        text = "BACK";
        x = getXforCenteredText(text, g2);
        y += tileSize;
        g2.drawString(text, x, y);
        if (commandNum == 5) {
            g2.drawString(">", x - tileSize, y);
        }
    }

    public void drawControlsScreen(Graphics2D g2) {
        g2.setColor(Color.black);
        g2.fillRect(0, 0, screenWidth, screenHeight);

        g2.setColor(Color.white);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 40F));
        String text = "CONTROLS";
        int x = getXforCenteredText(text, g2);
        int y = tileSize * 2;
        g2.drawString(text, x, y);

        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 20F));
        y += tileSize * 2;

        String[] controls = {
                "W / UP : Move Up",
                "S / DOWN : Move Down",
                "A / LEFT : Move Left",
                "D / RIGHT : Move Right",
                "SPACE : Attack",
                "SHIFT : Dash",
                "ENTER : Select / Pause",
                "ESC : Pause / Back"
        };

        for (String line : controls) {
            x = getXforCenteredText(line, g2);
            g2.drawString(line, x, y);
            y += 30;
        }

        text = "Press ENTER or ESC to return";
        x = getXforCenteredText(text, g2);
        y = screenHeight - tileSize * 2;
        g2.drawString(text, x, y);
    }

    public void drawGameOverScreen(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(0, 0, screenWidth, screenHeight);

        g2.setColor(Color.red);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 50F));
        String text = "GAME OVER";
        int x = getXforCenteredText(text, g2);
        int y = screenHeight / 2 - 40;
        g2.drawString(text, x, y);

        g2.setColor(Color.white);
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 30F));
        text = "Final Score: " + (totalScore + score);
        x = getXforCenteredText(text, g2);
        y += 50;
        g2.drawString(text, x, y);

        // Menu Options
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 24F));

        text = "RETRY";
        x = getXforCenteredText(text, g2);
        y += tileSize * 3;
        g2.drawString(text, x, y);
        if (commandNum == 0) {
            g2.drawString(">", x - tileSize, y);
        }

        text = "CHANGE ADVENTURER";
        x = getXforCenteredText(text, g2);
        y += tileSize;
        g2.drawString(text, x, y);
        if (commandNum == 1) {
            g2.drawString(">", x - tileSize, y);
        }

        text = "MAIN MENU";
        x = getXforCenteredText(text, g2);
        y += tileSize;
        g2.drawString(text, x, y);
        if (commandNum == 2) {
            g2.drawString(">", x - tileSize, y);
        }
    }

    public int getXforCenteredText(String text, Graphics2D g2) {
        int length = (int)g2.getFontMetrics().getStringBounds(text, g2).getWidth();
        int x = screenWidth/2 - length/2;
        return x;
    }
}
