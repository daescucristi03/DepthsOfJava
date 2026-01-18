import java.io.*;
import java.util.ArrayList;
import java.util.Collections;

/**
 * The Leaderboard class manages the high scores of the game.
 * It handles loading, saving, adding, and sorting scores.
 * Scores are persisted to a local file named "leaderboard.txt".
 */
public class Leaderboard {
    
    private final String filePath = "leaderboard.txt";
    private ArrayList<ScoreEntry> scores;

    /**
     * Constructor for Leaderboard.
     * Initializes the scores list and loads existing scores from the file.
     */
    public Leaderboard() {
        scores = new ArrayList<>();
        loadScores();
    }

    /**
     * Adds a new score to the leaderboard.
     * If the player already exists, their score is updated only if the new score is higher.
     * The leaderboard is then sorted and saved.
     * 
     * @param name The player's name.
     * @param score The score achieved.
     */
    public void addScore(String name, int score) {
        // Check if player already exists
        boolean updated = false;
        for (ScoreEntry entry : scores) {
            if (entry.name.equals(name)) {
                if (score > entry.score) {
                    entry.score = score; // Update high score
                }
                updated = true;
                break;
            }
        }
        
        if (!updated) {
            scores.add(new ScoreEntry(name, score));
        }

        sortScores();
        saveScores();
    }

    /**
     * Sorts the scores in descending order and keeps only the top 10.
     */
    private void sortScores() {
        Collections.sort(scores);
        // Keep only top 10
        if (scores.size() > 10) {
            scores.remove(scores.size() - 1);
        }
    }

    /**
     * Loads scores from the "leaderboard.txt" file.
     */
    private void loadScores() {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    scores.add(new ScoreEntry(parts[0], Integer.parseInt(parts[1])));
                }
            }
            Collections.sort(scores);
        } catch (IOException e) {
            // File might not exist yet, which is fine
        }
    }

    /**
     * Saves the current scores to the "leaderboard.txt" file.
     */
    private void saveScores() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
            for (ScoreEntry entry : scores) {
                bw.write(entry.name + ":" + entry.score);
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the list of high scores.
     * 
     * @return An ArrayList of ScoreEntry objects.
     */
    public ArrayList<ScoreEntry> getScores() {
        return scores;
    }

    /**
     * Inner class representing a single score entry.
     * Implements Comparable to allow sorting by score.
     */
    public class ScoreEntry implements Comparable<ScoreEntry> {
        String name;
        int score;

        public ScoreEntry(String name, int score) {
            this.name = name;
            this.score = score;
        }

        @Override
        public int compareTo(ScoreEntry other) {
            return other.score - this.score; // Descending order
        }
    }
}
