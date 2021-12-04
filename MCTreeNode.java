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

    private MCTreeNode(MCTreeNode parent, Card[][] board, PokerSquaresPointSystem system) { //for tree branches, called by nodes
        this.numberOfActions = parent.numberOfActions + 1; /* An action has been taken */
        this.parent = parent;
        this.board = board;
        this.system = system;
    }

    public void trial(Card card, LinkedList<Card> temporaryDeck) {
        LinkedList<Card> deckForSelectAction = (LinkedList<Card>) temporaryDeck.clone();

        List<MCTreeNode> visited = new LinkedList<MCTreeNode>();
        MCTreeNode cur = this;
        
        // tree policy
        visited.add(this);
        //System.out.println(cur.isLeaf());
        // TODOwhile (!cur.isLeaf()) {
        //     cur = cur.selectBestValue();
        //     deckForSelectAction.pop();
        //     visited.add(cur);
        // }

        // expand
        cur.createChildren(card);
        
        // select
        MCTreeNode newNode;
        if (cur.numberOfActions == uctPlayer.NUM_POS) {
            newNode = cur;
        }
        else {
            //TODOnewNode = cur.selectBestValue();
            //TODOvisited.add(newNode);
        }
        
        // roll out and update stats for each node
        double value = 0;
        for(int i = 0; i<uctPlayer.numSimulationsPerRollout; i++) {
            //TODOvalue = value + newNode.rollOut(deckForSelectAction);
        }
        for (MCTreeNode node : visited) {
            
            //TODOnode.updateStats(value);
        }
    }

    public void createChildren(Card card) {
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
    
}
