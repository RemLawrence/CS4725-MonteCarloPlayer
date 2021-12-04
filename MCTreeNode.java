import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.Stack;
import java.util.List;
import java.util.LinkedList;
import java.util.Objects;

public class MCTreeNode {
    private UCTPlayer uctPlayer = new UCTPlayer();
    PokerSquaresPointSystem system; /* System will be passed as a parameter */
    public Card[][] board;
    private int numberOfActions; // number of actions made so far in this simulation
    public MCTreeNode[] children; /* This node's children, empty for now */
    private MCTreeNode parent; /* This node's parent. If it has a parent, then this node has already being chosen as the best move among all children */
    // private double nVisits = 0;
    // private double totValue = 0;

    public MCTreeNode(int numPlays, Card[][] board, PokerSquaresPointSystem system) { //For tree root, called by MCTS player
        numberOfActions = numPlays;
        this.board = board;
        this.system = system;
        this.parent = null;
    }

    public void trail(Card card, LinkedList<Card> temporaryDeck) {
        LinkedList<Card> deckForSelectAction = (LinkedList<Card>) temporaryDeck.clone();

        List<MCTreeNode> visited = new LinkedList<MCTreeNode>();
        MCTreeNode cur = this;
        
        // tree policy
        visited.add(this);
        //System.out.println(cur.isLeaf());
        while (!cur.isLeaf()) {
            cur = cur.selectBestValue();
            deckForSelectAction.pop();
            visited.add(cur);
        }

        // expand
        cur.createChildren(card);
        
        // select
        MCTreeNode newNode;
        if (cur.numberOfActions == mctsPlayer.NUM_POS) {
            newNode = cur;
        }
        else {
            newNode = cur.selectBestValue();
            visited.add(newNode);
        }
        
        // roll out and update stats for each node
        double value = 0;
        for(int i = 0; i<mctsPlayer.numSimulationsPerRollout; i++) {
            value = value + newNode.rollOut(deckForSelectAction);
        }
        for (MCTreeNode node : visited) {
            
            node.updateStats(value);
        }
    }

    public void createChildren(Card card) {
        if (numberOfActions == mctsPlayer.NUM_POS) {
            children = null;
            return;
        }
        else {
            int cardPos = 0; // Used for record the card position for each children */
            /* You're gonna have 25-1 children for that root node! */
            MCTreeNode[] children = new MCTreeNode[mctsPlayer.NUM_POS - numberOfActions];

            for (int i = 0; i < mctsPlayer.NUM_POS - numberOfActions; i++) {

                Card[][] tempBoard = new Card[mctsPlayer.SIZE][mctsPlayer.SIZE];

                /* Copy whatever in the already played board */
                for(int row = 0; row < mctsPlayer.SIZE; row++) {
                    for(int col = 0; col < mctsPlayer.SIZE; col++) {
                        tempBoard[row][col] = board[row][col];
                    }
                }
                
                /* From 0,0 -> 0,1 -> 0,2.... find the position that is not null */
                while (this.board[cardPos / mctsPlayer.SIZE][cardPos % mctsPlayer.SIZE] != null) {
                    cardPos++;
                } 
                /* Place the card in that first null position */
                tempBoard[cardPos / mctsPlayer.SIZE][cardPos % mctsPlayer.SIZE] = card;
                
                /* children numba i (range: 0-24) shall be added for the root */
                /* This demonstrates mctsPlayer.NUM_POS - numberOfActions of possibilities of this card's potential position in the board */
                children[i] = new MCTreeNode(this, tempBoard, system);
                cardPos++;
            }
            /* Assign those (mctsPlayer.NUM_POS - numberOfActions) # of children to the root node */
            this.children = children;
        }
    }
    
}
