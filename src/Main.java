import javax.swing.JFrame;

/**
 * The Main class serves as the entry point for the application.
 * It sets up the main window (JFrame) and initializes the GamePanel.
 */
public class Main {
    /**
     * The main method that starts the application.
     * 
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args) {
        // Create the main application window
        JFrame window = new JFrame();
        
        // Ensure the application closes when the window is closed
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Allow the window to be resized by the user
        window.setResizable(true);
        
        // Set the title of the window
        window.setTitle("Depths of Java");

        // Create the game panel which contains the game logic and rendering
        GamePanel gamePanel = new GamePanel();
        window.add(gamePanel);

        // Size the window to fit the preferred size of its subcomponents (GamePanel)
        window.pack();

        // Center the window on the screen
        window.setLocationRelativeTo(null);
        
        // Make the window visible
        window.setVisible(true);

        // Initialize game state and start the game loop
        gamePanel.setupGame();
        gamePanel.startGameThread();
    }
}
