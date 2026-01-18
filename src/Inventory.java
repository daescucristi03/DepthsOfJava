import java.util.ArrayList;

/**
 * The Inventory class manages the player's items.
 * Note: In the current version of the game, the inventory system has been replaced by direct stat upgrades.
 * This class is kept for potential future features or legacy support.
 */
public class Inventory {
    /** List of item names in the inventory. */
    public ArrayList<String> items = new ArrayList<>();
    /** Maximum number of slots in the inventory. */
    public int maxSlots = 5;
    /** The currently selected slot index. */
    public int currentSlot = 0;

    /**
     * Constructor for Inventory.
     * Initializes the inventory with empty slots.
     */
    public Inventory() {
        // Initialize empty slots
        for(int i=0; i<maxSlots; i++) {
            items.add("Empty");
        }
    }

    /**
     * Adds an item to the first available empty slot.
     * 
     * @param itemName The name of the item to add.
     */
    public void addItem(String itemName) {
        for(int i=0; i<maxSlots; i++) {
            if(items.get(i).equals("Empty")) {
                items.set(i, itemName);
                return;
            }
        }
        System.out.println("Inventory full!");
    }

    /**
     * Scrolls the selected slot by a given amount.
     * Handles wrapping around the inventory slots.
     * 
     * @param amount The amount to scroll (positive or negative).
     */
    public void scrollSlot(int amount) {
        currentSlot += amount;
        if(currentSlot < 0) currentSlot = maxSlots - 1;
        if(currentSlot >= maxSlots) currentSlot = 0;
    }
}
