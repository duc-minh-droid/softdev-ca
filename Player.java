import java.util.*;

public class Player extends Thread{
    private final int id;
    private final List<Card> hand = new ArrayList<>();
    private final Deck deck;
    private final CardGame game;
    private final Logger logger;

    public Player(int id, Deck deck, CardGame game) {
        this.id = id;
        this.deck = deck;
        this.game = game;
        this.logger = new Logger("gameOutput", "player" + id + "_output.txt",true);
    }

    public List<Card> getHand() {
        return hand;
    }
    public Deck getDeck() {
        return deck;
    }

    public long getId() {
        return this.id;
    }
    public void logInitialHand() {
        logger.log("initial hand " + logger.cardsToString(hand), id);
    }

    public synchronized void addCard(Card card) {
        hand.add(card);
        notifyAll();
    }

    public Boolean checkWinningHand() {
        if (hand.isEmpty()) {
            return false; // Cannot win with an empty hand
        }
        int firstCardValue = hand.get(0).getValue();
        for (int i = 1; i < hand.size(); i++) {
            if (hand.get(i).getValue() != firstCardValue) {
                return false; 
            }
        }
        return true;
    }
    
    public void playTurn() {
        Deck nextPlayerDeck = game.getNextPlayer(this).deck;
        Card drawedCard = deck.removeCard();
        if (drawedCard != null) {
            hand.add(drawedCard);
            logger.log("draws a " + drawedCard.getValue() + " from deck " + deck.getId(), id);
        } else {
            logger.log("draws no card as deck is empty", id);
        }
    
        int index = findDiscardIndex();
        Card cardToDiscard = hand.remove(index);
        nextPlayerDeck.addCard(cardToDiscard);
        logger.log("discards a " + cardToDiscard.getValue() + " to deck " + nextPlayerDeck.getId(), id);
    
        logger.log("current hand " + logger.cardsToString(hand));
    }
    

    private int findDiscardIndex() {
        Map<Integer, Integer> frequencyMap = new HashMap<>();
        for (Card card : hand) {
            frequencyMap.put(card.getValue(), frequencyMap.getOrDefault(card.getValue(), 0) + 1);
        }
        int preferredValue = hand.get(0).getValue();
        int maxFrequency = 0;
        for (Map.Entry<Integer, Integer> entry : frequencyMap.entrySet()) {
            if (entry.getValue() > maxFrequency) {
                preferredValue = entry.getKey();
                maxFrequency = entry.getValue();
            }
        }
        int discardIndex = -1;
        for (int i = 0; i < hand.size(); i++) {
            if (hand.get(i).getValue() != preferredValue) {
                discardIndex = i;
                break;
            }
        }
        if (discardIndex == -1) {
            Random random = new Random();
            discardIndex = random.nextInt(hand.size());
        }
        return discardIndex;
    }

    @Override
    public void run() {
        try {
            while (game.winningPlayer.get() == 0) {

                if (deck.isEmpty()) {
                    synchronized (deck) {
                        try {
                            deck.wait();
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                } else {
                    // only play when left deck is not starved and right deck is not overpopulated
                    if (deck.size() > 3 && game.getNextPlayer(this).getDeck().size() < 5) {
                        playTurn();
                    }
                    if (checkWinningHand()) {
                        if (game.winningPlayer.compareAndSet(0, id)) {
                            // if player won
                            logger.log("wins", id);
                            game.notifyAllPlayers();
                            break;
                        }
                    }
                }
            }
        } finally {
            if (game.winningPlayer.get() == id) {
                logger.log("exits", id);
                logger.log("final hand: " + logger.cardsToString(hand), id);
            } else {
                int winnerId = game.winningPlayer.get();
                logger.log("has informed player " + id + " that player " + winnerId + " has won", winnerId);
                logger.log("exits", id);
                logger.log("final hand: " + logger.cardsToString(hand), id);
            }

            // Final notify to prevent deadlock
            synchronized (deck) {
                deck.notify();
            }
        }
    }

}
