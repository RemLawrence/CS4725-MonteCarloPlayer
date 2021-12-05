/*****
 * Everything we need for the Monte Carlo Search Tree.
 * Including the helper functions for: 
 * 1. do a single trial 
 * 2. node expansion
 * 3. calculate UCB value
 * 4. Rollout
 * 5. Update stats for each node
 * 
 * This is exactly the same implementation as the video below.
 * Reference Video: https://www.youtube.com/watch?v=UXW2yZndl7U&t=6s
 * 
 * Author: Zhangliang Ma, Micah Hanmin Wang
 * Date: 2021-12-02
 */

import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.Stack;
import java.util.List;
import java.util.LinkedList;
import java.util.Objects;

@SuppressWarnings("unchecked")
public class MCTreeNode {
    private ZMPlayer zmPlayer = new ZMPlayer();
    PokerSquaresPointSystem system; /* System will be passed as a parameter */
    public Random random = new Random(); // pseudorandom number generator for Monte Carlo simulation 
    public final double epsilon = 1e-6; 

    public Card[][] board;
    private int numberOfActions; // number of actions made so far in this simulation
    public MCTreeNode[] children; /* This node's children, empty for now */
    private MCTreeNode parent; /* This node's parent. If it has a parent, then this node has already being chosen as the best move among all children */
    private double visit = 0;
    private double totalValue = 0;
    public int numSimulationsPerRollout = 1;
    public double selectionConstant = 10;

    /**
     * This constructor is ONLY for the root node of MCTree
     * @param numPlays
     * @param board
     * @param system
     */
    public MCTreeNode(int numPlays, Card[][] board, PokerSquaresPointSystem system) {
        numberOfActions = numPlays;
        this.board = board;
        this.system = system;
        this.parent = null;
    }

    /**
     * This constructor is for any other node except the root node of MCTree.
     * It must be initialized with a parent node.
     * @param parent
     * @param board
     * @param system
     */
    private MCTreeNode(MCTreeNode parent, Card[][] board, PokerSquaresPointSystem system) { //for tree branches, called by nodes
        this.numberOfActions = parent.numberOfActions + 1; /* An action has been taken */
        this.parent = parent;
        this.board = board;
        this.system = system;
    }

    /**
     * Step 1: Do trials for the current Deck.
     * First, visit this currentNode and check if it is leaf already. 
     * If not, keep choosing its children that has the best UCB value.
     * Then, expand this leaf node to let it has the children with all possibilites of the position that
     * the next card could possibly be placed.
     * Pick the children with the best UCB value (if first time, then tiebreaker), visit it, and do the rollout.
     * Finally, update the stats of the children to be used for the next trail.
     * @param tempCard
     * @param temporaryDeck
     */
    public void trial(Card tempCard, LinkedList<Card> temporaryDeck) {
        List<MCTreeNode> visited = new LinkedList<MCTreeNode>();
        MCTreeNode currentNode = this;
        visited.add(this);

        LinkedList<Card> deckForTrial = (LinkedList<Card>) temporaryDeck.clone();
        /* Loop until the leaf node */
        while (currentNode.children != null) {
            currentNode = currentNode.bestUCTValue();
            visited.add(currentNode);
            deckForTrial.pop();
        }

        /* Node Expansion */
        Card card = deckForTrial.pop();
        currentNode.nodeExpansion(card);

        /* Select the child with max UCB Value for the rollout */
        MCTreeNode bestChild;
        /* if numberOfActions == 25, just return the currentNode */
        if (currentNode.numberOfActions == zmPlayer.NUM_POS) {
            bestChild = currentNode;
        }
        else {
            /* Select the child with the best UCT value (being visited the most times, potentially :)) */
            bestChild = currentNode.bestUCTValue();
            visited.add(bestChild);
        }
        
        /* Do roll out, then update the stats for each node */
        double backpropagationValue = 0;
        for(int i = 0; i < numSimulationsPerRollout; i++) {
            backpropagationValue = backpropagationValue + bestChild.rollOut(deckForTrial);
        }
        for (MCTreeNode node : visited) {
            node.updateStats(backpropagationValue);
        }
    }

    /**
     * Step 2: Find all the possibilities of the cnext card's potential position,
     * store them as children, append it to the currentNode.
     * @param card
     */
    public void nodeExpansion(Card card) {
        if (numberOfActions == zmPlayer.NUM_POS) {
            children = null;
            return;
        }
        else {
            int cardPos = 0; // Used for record the card position for each children */
            /* You're gonna have 25-1 children for that root node! */
            MCTreeNode[] children = new MCTreeNode[zmPlayer.NUM_POS - numberOfActions];

            for (int i = 0; i < zmPlayer.NUM_POS - numberOfActions; i++) {

                Card[][] tempBoard = new Card[zmPlayer.SIZE][zmPlayer.SIZE];

                /* Copy whatever in the already played board */
                for(int row = 0; row < zmPlayer.SIZE; row++) {
                    for(int col = 0; col < zmPlayer.SIZE; col++) {
                        tempBoard[row][col] = board[row][col];
                    }
                }
                
                /* From 0,0 -> 0,1 -> 0,2.... find the position that is not null */
                while (this.board[cardPos / zmPlayer.SIZE][cardPos % zmPlayer.SIZE] != null) {
                    cardPos++;
                }
                /* Place the card in that first null position */
                tempBoard[cardPos / zmPlayer.SIZE][cardPos % zmPlayer.SIZE] = card;
                
                /* children numba i (range: 0-24) shall be added for the root */
                /* This demonstrates zmPlayer.NUM_POS - numberOfActions of possibilities of this card's potential position in the board */
                children[i] = new MCTreeNode(this, tempBoard, system);
                cardPos++;
            }
            /* Assign those (zmPlayer.NUM_POS - numberOfActions) # of children to the root node */
            this.children = children;
        }
    }

    /**
     * Step 3: Choose the child node (partially filled board) that has the max UCB1(S)
     * (Tiebreaker if the first time)
     * May the best child win.
     */
    public MCTreeNode bestUCTValue() {
        MCTreeNode bestNode = null;
        double bestValue = Double.MIN_VALUE;
        //System.out.println(visit);

        /* May the best child win. */
        for (MCTreeNode child : children) {
            /* small random number to break ties randomly in unexpanded nodes */
            double uctValue = child.totalValue / (child.visit + epsilon) + selectionConstant * (Math.sqrt(Math.log(visit+1) / (child.visit + epsilon))) + zmPlayer.random.nextDouble() * epsilon;
            if (uctValue > bestValue) {
                bestNode = child;
                bestValue = uctValue;
            }
        }
        return bestNode;
    }
    
    /**
     * Step 4: Rollout, randomly (Hey Mike, if you're reading this line, the rollout can actually 
     * could be improved better to always select the best potential move each time using an evaluation
     * function, but no time to do it):)
     */
    public double rollOut(LinkedList<Card> temporaryDeck) {
        LinkedList<Card> deckForRollout = (LinkedList<Card>) temporaryDeck.clone();
        
        /* copy current board to new board to fill for rollout */
        Card[][] boardToFill = new Card[zmPlayer.SIZE][zmPlayer.SIZE];
        for(int row = 0; row < zmPlayer.SIZE; row++) {
            for(int col = 0; col < zmPlayer.SIZE; col++) {
                boardToFill[row][col] = board[row][col];
            }
        }

        /* A stack recording the empty positions on the current board */
        Stack<Integer> emptyPositions = new Stack<Integer>();
        /* Find all empty positions of board */
        for(int i = 0; i < zmPlayer.NUM_POS; i++) {
            if (boardToFill[i / zmPlayer.SIZE][i % zmPlayer.SIZE] == null) {
                //System.out.println(boardToFill[i / zmPlayer.SIZE][i % zmPlayer.SIZE]);
                emptyPositions.push(i);
            }
        }
        
        /* get an empty position randomly to put next card in */
        Collections.shuffle(emptyPositions, zmPlayer.random);
        while(!emptyPositions.empty()) {
            int square = emptyPositions.pop().intValue();
            //System.out.println(square);
            /* pop a random card for it */
            boardToFill[square / zmPlayer.SIZE][square % zmPlayer.SIZE] = deckForRollout.pop();
        }
        double finalscore = system.getScore(boardToFill);
        //System.out.println(finalScore);
        return finalscore;
        
    }
    
    /**
     * Step 5: Update # of visits for this node, and it's total value across all the simulations
     * for future use.
     * @param value
     */
    public void updateStats(double value) {
        totalValue += value;
        visit++;
    }
    
}
