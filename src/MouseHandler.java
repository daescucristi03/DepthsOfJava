import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

/**
 * The MouseHandler class implements the MouseWheelListener interface to handle mouse wheel input.
 * Note: In the current version of the game, mouse input is not actively used for gameplay mechanics
 * (inventory scrolling was removed), but this class is kept for potential future use.
 */
public class MouseHandler implements MouseWheelListener {

    /** The amount the mouse wheel has been rotated. */
    public int scrollAmount = 0;

    /**
     * Invoked when the mouse wheel is rotated.
     * Updates the scroll amount.
     * 
     * @param e The MouseWheelEvent.
     */
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        scrollAmount = e.getWheelRotation();
    }
}
