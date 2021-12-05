/*****
 * ZMPlayer - Zhangliang + Micah's player.
 * This is an implementation of the Monte Carlo Player using 
 * Upper Condidence Bounds Applied to Trees (UCT) to do the evaluation for
 * each tree node.
 * First, create a root node of the Monte Carlo Search Tree, then do a node expansion to have a number of emptyPosition 
 * of children append to it, then calculating each UCT values of them, select the one that has the highest
 * value, marked it as visited, do a rollout for it, get the value when reaching leaf, send back the value
 * back to the node itself.
 * Repeat the process, until the time allowed for this play is elapsed. During the limited time, do as many trials as
 * we can to make sure we're making the best play.
 * Generally, the more you visited one child node, the more likely it'll be chosen as the next play.
 * 
 * This is exactly the same implementation as the video listed below.
 * Reference Video: https://www.youtube.com/watch?v=UXW2yZndl7U&t=6s
 * 
 * Author: Zhangliang Ma, Micah Hanmin Wang
 * Date: 2021-12-02
 */

import java.util.ArrayList;
import java.util.Random;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.LinkedList;
import java.util.stream.*;

@SuppressWarnings("unchecked")
public class ZMPlayer implements PokerSquaresPlayer {
    public final int SIZE = 5; // number of rows/columns in square grid
	public final int NUM_POS = SIZE * SIZE; // number of positions in square grid
	public final int NUM_CARDS = Card.NUM_CARDS; // number of cards in deck
	public Random random = new Random(); // pseudorandom number generator for Monte Carlo simulation 
	/* range: 0-24, means 25 cards to play 
		e.g. play(3,4) = 3*5+4 = 19, 20th card */
	public int[] plays = new int[NUM_POS]; // positions of plays so far (index 0 through numPlays - 1) recorded as integers using row-major indices.
	// row-major indices: play (r, c) is recorded as a single integer r * SIZE + c (See http://en.wikipedia.org/wiki/Row-major_order)
	// From plays index [numPlays] onward, we maintain a list of yet unplayed positions.
	public int numPlays = 0; // number of Cards played into the grid so far
	public PokerSquaresPointSystem system; // point system
	public int depthLimit = 2; // default depth limit for Random Monte Carlo (MC) play
	public Card[][] grid = new Card[SIZE][SIZE]; // grid with Card objects or null (for empty positions)
	public Card[] simDeck = Card.getAllCards(); // a list of all Cards. As we learn the index of cards in the play deck,
	                                             // we swap each dealt card to its correct index.  Thus, from index numPlays 
												 // onward, we maintain a list of undealt cards for MC simulation.
	public int[][] legalPlayLists = new int[NUM_POS][NUM_POS]; // stores legal play lists indexed by numPlays (depth)
	// (This avoids constant allocation/deallocation of such lists during the selections of MC simulations.)
	public int trialsPerDeck = 10; 
	public List<Card> list = Arrays.asList(simDeck);
    public LinkedList<Card> deck = new LinkedList<Card>();

	// //TODO: Don't just do random MC, do it according to each points
	// public struct Score {
	// 	int high_card = 0;
	// 	int one_pair = 1;
	// 	int two_pair = 3;
	// 	int three_of_a_kind = 6;
	// 	int straight = 12;
	// 	int flush = 5;
	// 	int full_house = 10;
	// 	int four_of_a_kind = 16;
	// 	int straight_flush = 30;
	// 	int royal_flush = 30;
	// }

    /**
	 * Create a Monte Carlo player that uses UCT to evaluate each playout's value
	 */
	public ZMPlayer() {
	}

    /**
	 * Create a Random Monte Carlo player that simulates random play to a given depth limit.
	 * @param depthLimit depth limit for random simulated play
	 */
	// public RandomMCPlayer(int depthLimit) {
	// 	this.depthLimit = depthLimit;
	// }

    /* (non-Javadoc)
	 * @see PokerSquaresPlayer#init()
     * Implement Init function to initialize the
	 */
	@Override
	public void init() {
		// clear grid, all initialized to NULL
		for (int row = 0; row < SIZE; row++)
			for (int col = 0; col < SIZE; col++)
				grid[row][col] = null;
		// reset numPlays, we played 0 card so far.
		numPlays = 0;
		// (re)initialize list of play positions (row-major ordering)
		for (int i = 0; i < NUM_POS; i++)
			plays[i] = i;
	}

    /* (non-Javadoc)
	 * @see PokerSquaresPlayer#setPointSystem(PokerSquaresPointSystem, long)
	 */
	@Override
	public void setPointSystem(PokerSquaresPointSystem system, long millis) {
		this.system = system;
	}

    /* (non-Javadoc)
	 * @see PokerSquaresPlayer#getName()
	 */
	@Override
	public String getName() {
		return "Zhangliang Ma, Micah Hanmin Wang, ZMPlayer";
	}

    /* (non-Javadoc)
	 * @see PokerSquaresPlayer#getPlay(Card, long)
	 */
	@Override
	public int[] getPlay(Card card, long millisRemaining) {
        // (This avoids constant allocation/deallocation of such lists during the greedy selections of MC simulations.)
        int[] playPos = new int[2];

		ArrayList<Integer> Rowlist = new ArrayList<Integer>(Arrays.asList(0,1,2,3,4));
		ArrayList<Integer> Collist = new ArrayList<Integer>(Arrays.asList(0,1,2,3,4));
        
        if (numPlays == 0) {
            /* Initialize everything when playing first */
            deck.addAll(list);
            /* Remove the card from the current deck */
            deck.remove(card);
            /* Always place the first card at the upper left corner aka grid[0][0] */
            grid[0][0] = card;
            
            playPos[0] = 0;
            playPos[1] = 0;
        }
        else if (numPlays > 0 & numPlays < 24) { // not the forced last play
			// not the first play either

            // compute average time per move evaluation
            /* remainingPlays = how many cards left? First round: 25-0 */
            int remainingPlays = NUM_POS - numPlays; // ignores triviality of last play to keep a conservative margin for game completion
            /* millisPerPlay = Average time allowed per remaining play */
            long millisPerPlay = (millisRemaining - 1000) / remainingPlays; // dividing time evenly with future getPlay() calls
            //System.out.println(playPos + "\n");
            long startTime = System.currentTimeMillis();
			/* Time allowed to play at each round */
            long endTime = startTime + millisPerPlay;

			/* The currentNode of the Monte Carlo Search Tree */
			MCTreeNode currentNode = new MCTreeNode(numPlays, grid, system);
            
			/* remove the card from our deck */
            deck.remove(card);

			

			/* While in the allowed time, perform as many simulations as possible :) */
			while (System.currentTimeMillis() < endTime) { // perform as many MC simulations as possible through the allotted time
				/* This is a new shuffled deck. Usage: simulations */
				// (This avoids constant allocation/deallocation of such lists during the greedy selections of MC simulations.)
                LinkedList<Card> tempDeck = new LinkedList<Card>();
                
                // create new shuffled sim deck to use
                tempDeck = (LinkedList<Card>)deck.clone();
                Collections.shuffle(tempDeck, random);
                tempDeck.push(card);
                
                /* create and do the trials */
                for(int t = 0; t < trialsPerDeck; t++) {
                    currentNode.trial(card, tempDeck);
                }
                
                /* eliminate (reset) the nodes added in the trials */
                for(MCTreeNode node: currentNode.children) {
                    node.children = null;
                }
                tempDeck.clear();
            }
            
            MCTreeNode bestNode = currentNode.bestUCTValue();
            
			/* Place the new card in the bestNode in the correct position */
			Rowlist.parallelStream().forEach((row) -> {
				Collist.parallelStream().forEach((col) -> {
					if (bestNode.board[row][col] == card) {
						grid[row][col] = card;
                        playPos[0] = row;
                        playPos[1] = col;
                    }
				});
			});
			
			
			/*
            for(int row = 0; row < SIZE; row++) {
                for (int col = 0; col < SIZE; col++) {
                    if (bestNode.board[row][col] == card) {
						grid[row][col] = card;
                        playPos[0] = row;
                        playPos[1] = col;
                    }
                }
            } 
			*/
			
			
			
		}
		else {
			/* numPlays == 24 */
			/* If last card, just insert in the only null place in the grid and return that playPos */
			Rowlist.parallelStream().forEach((row) -> {
				Collist.parallelStream().forEach((col) -> {
					if(grid[row][col] == null) {
						// Just place the card in the only empty pos 
                        grid[row][col] = card;
                        playPos[0] = row;
                        playPos[1] = col;
                    }
				});
			});

			/*
			for(int row = 0; row < SIZE; row++) {
                for(int col = 0; col < SIZE; col++) {
                    if(grid[row][col] == null) {
						// Just place the card in the only empty pos 
                        grid[row][col] = card;
                        playPos[0] = row;
                        playPos[1] = col;
                    }
                }
            }
			*/
			
        }
		numPlays++;

		return playPos;
    }
    /**
	 * Demonstrate MCPlay with British point system.
	 * @param args (not used)
	 */
    public static void main(String[] args) {
        /* Using British System */
        PokerSquaresPointSystem system = PokerSquaresPointSystem.getBritishPointSystem();
        System.out.println(system);
        new PokerSquares(new ZMPlayer(), system).play(); // play a single game
    }
}
