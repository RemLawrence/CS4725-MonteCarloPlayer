import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.Stack;
import java.util.List;
import java.util.LinkedList;
import java.util.Objects;

@SuppressWarnings("unchecked")
public class MCTreeNode {
    private UCTPlayer uctPlayer = new UCTPlayer();
    PokerSquaresPointSystem system; /* System will be passed as a parameter */
    public Random random = new Random(); // pseudorandom number generator for Monte Carlo simulation 
    public final double epsilon = 1e-6; 

    public Card[][] board;
    private int numberOfActions; // number of actions made so far in this simulation
    public MCTreeNode[] children; /* This node's children, empty for now */
    private MCTreeNode parent; /* This node's parent. If it has a parent, then this node has already being chosen as the best move among all children */
    private double nVisits = 0;
    private double totValue = 0;
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
     * Check if this current node is leaf already. If leaf, then prepares 
     * backpropagation.
     * @return
     */
    public boolean isLeaf() {
        return children == null;
    }

    /**
     * Do trials for the current Deck.
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
        LinkedList<Card> deckForTrial = (LinkedList<Card>) temporaryDeck.clone();

        List<MCTreeNode> visited = new LinkedList<MCTreeNode>();
        MCTreeNode currentNode = this;
        
        // tree policy
        visited.add(this);
        //System.out.println(cur.isLeaf());
        while (!currentNode.isLeaf()) {
            currentNode = currentNode.bestUCTValue();
            deckForTrial.pop();
            visited.add(currentNode);
        }

        /* Node Expansion */
        Card card = deckForTrial.pop();
        currentNode.nodeExpansion(card);

        // select
        MCTreeNode bestChild;
        /* if numberOfActions == 25, just return the currentNode */
        if (currentNode.numberOfActions == uctPlayer.NUM_POS) {
            bestChild = currentNode;
        }
        else {
            /* Select the child with the best UCT value (being visited the most times, potentially :)) */
            bestChild = currentNode.bestUCTValue();
            visited.add(bestChild);
        }
        
        // roll out and update stats for each node
        double value = 0;
        for(int i = 0; i < numSimulationsPerRollout; i++) {
            value = value + bestChild.rollOut(deckForTrial);
        }
        for (MCTreeNode node : visited) {
            node.updateStats(value);
        }
    }

    /**
     * Find all the possibilities of the cnext card's potential position,
     * store them as children, append it to the currentNode.
     * @param card
     */
    public void nodeExpansion(Card card) {
        if (numberOfActions == uctPlayer.NUM_POS) {
            children = null;
            return;
        }
        else {
            int cardPos = 0; // Used for record the card position for each children */
            /* You're gonna have 25-1 children for that root node! */
            MCTreeNode[] children = new MCTreeNode[uctPlayer.NUM_POS - numberOfActions];

            for (int i = 0; i < uctPlayer.NUM_POS - numberOfActions; i++) {

                Card[][] tempBoard = new Card[uctPlayer.SIZE][uctPlayer.SIZE];

                /* Copy whatever in the already played board */
                for(int row = 0; row < uctPlayer.SIZE; row++) {
                    for(int col = 0; col < uctPlayer.SIZE; col++) {
                        tempBoard[row][col] = board[row][col];
                    }
                }
                
                /* From 0,0 -> 0,1 -> 0,2.... find the position that is not null */
                while (this.board[cardPos / uctPlayer.SIZE][cardPos % uctPlayer.SIZE] != null) {
                    cardPos++;
                }
                /* Place the card in that first null position */
                tempBoard[cardPos / uctPlayer.SIZE][cardPos % uctPlayer.SIZE] = card;
                
                /* children numba i (range: 0-24) shall be added for the root */
                /* This demonstrates uctPlayer.NUM_POS - numberOfActions of possibilities of this card's potential position in the board */
                children[i] = new MCTreeNode(this, tempBoard, system);
                cardPos++;
            }
            /* Assign those (uctPlayer.NUM_POS - numberOfActions) # of children to the root node */
            this.children = children;
        }
    }

    /**
     * Choose the child node (partially filled board) that has the max UCB1(S)
     * (Tiebreaker if the first time)
     */
    public MCTreeNode bestUCTValue() {
        MCTreeNode bestNode = null;
        double bestValue = Double.MIN_VALUE;
        //System.out.println(nVisits);
        // go through each child and select best
        for (MCTreeNode child : children) {
            double uctValue = child.totValue / (child.nVisits + epsilon) + 
            selectionConstant * (Math.sqrt(Math.log(nVisits+1) / (child.nVisits + epsilon))) +
            uctPlayer.random.nextDouble() * epsilon; // small random number to break ties randomly in unexpanded nodes
            if (uctValue > bestValue) {
                bestNode = child;
                bestValue = uctValue;
            }
        }
        return bestNode;
    }
    
    public double rollOut(LinkedList<Card> temporaryDeck) {
        LinkedList<Card> deckForRollout = (LinkedList<Card>) temporaryDeck.clone();
        
        // copy current board to new board to fill for rollout
        Card[][] boardToFill = new Card[uctPlayer.SIZE][uctPlayer.SIZE];
        for(int j = 0; j < uctPlayer.NUM_POS; j++) {
            boardToFill[j/uctPlayer.SIZE][j%uctPlayer.SIZE] = board[j/uctPlayer.SIZE][j%uctPlayer.SIZE];
        }//JANKY CODE
        Stack<Integer> emptyPositions = new Stack<Integer>();
        
        // find all empty positions of board
        for(int i = 0; i < uctPlayer.NUM_POS; i++) {
            if (boardToFill[i/uctPlayer.SIZE][i % uctPlayer.SIZE] == null) {
                emptyPositions.push(i);
            }
            
        }
        
        // get an empty position randomly to put next card in
        Collections.shuffle(emptyPositions, uctPlayer.random);
        while(!emptyPositions.empty()) {
            
            int square = emptyPositions.pop().intValue();
            boardToFill[square / uctPlayer.SIZE][square %uctPlayer.SIZE] = deckForRollout.pop();
        }
        double finalscore = system.getScore(boardToFill);
        return finalscore;
        
    }
    
    public void updateStats(double value) {
        nVisits++;
        totValue += value;
    }
    
}
