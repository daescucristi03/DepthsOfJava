import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * The KeyHandler class implements the KeyListener interface to handle keyboard input.
 * It tracks the state of various keys used for game control and menu navigation.
 */
public class KeyHandler implements KeyListener {

    // Movement and Action Keys
    public boolean upPressed, downPressed, leftPressed, rightPressed;
    public boolean enterPressed, escPressed, spacePressed, shiftPressed;
    
    // Text Input
    public boolean charTyped = false;
    public char lastChar;
    public boolean backspacePressed = false;

    /**
     * Invoked when a key has been typed.
     * Used for capturing character input for the player name.
     * 
     * @param e The KeyEvent.
     */
    @Override
    public void keyTyped(KeyEvent e) {
        lastChar = e.getKeyChar();
        charTyped = true;
    }

    /**
     * Invoked when a key has been pressed.
     * Updates the state of control keys.
     * 
     * @param e The KeyEvent.
     */
    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();

        if (code == KeyEvent.VK_W || code == KeyEvent.VK_UP) {
            upPressed = true;
        }
        if (code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN) {
            downPressed = true;
        }
        if (code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT) {
            leftPressed = true;
        }
        if (code == KeyEvent.VK_D || code == KeyEvent.VK_RIGHT) {
            rightPressed = true;
        }
        if (code == KeyEvent.VK_ENTER) {
            enterPressed = true;
        }
        if (code == KeyEvent.VK_ESCAPE) {
            escPressed = true;
        }
        if (code == KeyEvent.VK_SPACE) {
            spacePressed = true;
        }
        if (code == KeyEvent.VK_SHIFT) {
            shiftPressed = true;
        }
        if (code == KeyEvent.VK_BACK_SPACE) {
            backspacePressed = true;
        }
    }

    /**
     * Invoked when a key has been released.
     * Resets the state of control keys.
     * 
     * @param e The KeyEvent.
     */
    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();

        if (code == KeyEvent.VK_W || code == KeyEvent.VK_UP) {
            upPressed = false;
        }
        if (code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN) {
            downPressed = false;
        }
        if (code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT) {
            leftPressed = false;
        }
        if (code == KeyEvent.VK_D || code == KeyEvent.VK_RIGHT) {
            rightPressed = false;
        }
        if (code == KeyEvent.VK_ENTER) {
            enterPressed = false;
        }
        if (code == KeyEvent.VK_ESCAPE) {
            escPressed = false;
        }
        if (code == KeyEvent.VK_SPACE) {
            spacePressed = false;
        }
        if (code == KeyEvent.VK_SHIFT) {
            shiftPressed = false;
        }
        if (code == KeyEvent.VK_BACK_SPACE) {
            backspacePressed = false;
        }
    }
}
