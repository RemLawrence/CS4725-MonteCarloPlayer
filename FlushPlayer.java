import java.util.Collections;
import java.util.Stack;

/**
 * FlushPlayer - a simple example implementation of the player interface for PokerSquares that 
 * attempts to get flushes in the first four columns.
 * Author: ________, based on code provided by Todd W. Neller and Michael Fleming
 */
public class FlushPlayer implements PokerSquaresPlayer {

        private final int SIZE = 5; // number of rows/columns in square grid
        private final int NUM_POS = SIZE * SIZE; // number of positions in square grid
        private final int NUM_CARDS = Card.NUM_CARDS; // number of cards in deck
        private Card[][] grid = new Card[SIZE][SIZE]; // grid with Card objects or null (for empty positions)

	
	/* (non-Javadoc)
	 * @see PokerSquaresPlayer#setPointSystem(PokerSquaresPointSystem, long)
	 */
	@Override
	public void setPointSystem(PokerSquaresPointSystem system, long millis) {
		// The FlushPlayer, like the RandomPlayer, does not worry about the scoring system.	
	}
	
	/* (non-Javadoc)
	 * @see PokerSquaresPlayer#init()
	 */
	@Override
	public void init() { 
                // clear grid
                for (int row = 0; row < SIZE; row++)
                        for (int col = 0; col < SIZE; col++)
                                grid[row][col] = null;

	}

	/* (non-Javadoc)
	 * @see PokerSquaresPlayer#getPlay(Card, long)
	 */
	@Override
	public int[] getPlay(Card card, long millisRemaining) {
                int cardrow = 0;
                int cardcol = 0;
				boolean inserted = false;

                int cardrank = card.getRank();
                int cardsuit = card.getSuit();

				//System.out.println("cardrank " + cardrank);
				//System.out.println("cardsuit " + cardsuit);
				
				if(cardsuit == 0 || cardsuit == 1 || cardsuit == 2 || cardsuit == 3) {
					switch(cardsuit) {
						case 0:
							// if card suit is clubs
							cardrow = 0;
							cardcol = 0;
							break;
						case 1:
							// if card suit is diamonds
							cardrow = 0;
							cardcol = 1;
							break;
						case 2:
							// if card suit is hearts
							cardrow = 0;
							cardcol = 2;
							break;
						case 3:
							// if card suit is spades
							cardrow = 0;
							cardcol = 3;
							break;
					}

					if(grid[cardrow][cardcol] != null){
						for(int row = 0; row <= 4 && !inserted; row++){
							if(grid[row][cardcol] == null){
								cardrow = row;
								inserted = !inserted;
							}
						}
					}
					else {
						inserted = !inserted;
					}
				}

				if(!inserted) {
					//If the column you wanna insert is full already
					cardrow = 0;
					cardcol = 4;
					if(grid[cardrow][cardcol] != null) {
						for(int row = 0; row <= 4 && !inserted; row++) {
							if(grid[row][cardcol] == null){
								cardrow = row;
								inserted = !inserted;
							}
						}

						if(!inserted){
							// If col 4 is full, then scan the first 4 columns for a space
							for(int col = 0; col <= 3 && !inserted; col++){
								for(int row = 0; row <= 4 && !inserted; row++) {
									if(grid[row][col] == null){
										cardcol = col;
										cardrow = row;
										inserted = !inserted;
									}
								}
							}
						}
					}
					else {
						inserted = !inserted;
					}
				}

                grid[cardrow][cardcol] = card;

                int[] playPos = {cardrow, cardcol};

                return playPos;
	}

	/* (non-Javadoc)
	 * @see PokerSquaresPlayer#getName()
	 */
	@Override
	public String getName() {
		return "FlushPlayer";
	}

	/**
	 * Demonstrate FlushPlayer play with British point system.
	 * @param args (not used)
	 */
	public static void main(String[] args) {
		PokerSquaresPointSystem system = PokerSquaresPointSystem.getBritishPointSystem();
		System.out.println(system);
		new PokerSquares(new FlushPlayer(), system).play(); // play a single game
	}

}
