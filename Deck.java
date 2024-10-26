import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Deck {
    private final int id;
    private final Queue<Integer> cards = new LinkedList<>();
    // private final Lock lock = new ReentrantLock();

    public Deck(int id) {
        this.id = id;
    }

    public void addCard(int card) {
        cards.add(card);
    }

    public int drawCard() {
        return cards.poll();
    }

    public int getId() {
        return id;
    }
}
