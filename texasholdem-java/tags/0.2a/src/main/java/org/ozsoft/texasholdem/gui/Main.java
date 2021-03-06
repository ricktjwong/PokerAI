package org.ozsoft.texasholdem.gui;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;

import org.ozsoft.texasholdem.Action;
import org.ozsoft.texasholdem.Card;
import org.ozsoft.texasholdem.Client;
import org.ozsoft.texasholdem.Player;
import org.ozsoft.texasholdem.Table;
import org.ozsoft.texasholdem.bots.DummyBot;

/**
 * The game's main frame.
 * 
 * This is the core class of the Swing UI client application.
 * 
 * @author Oscar Stigter
 */
public class Main extends JFrame implements Client {
    
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    
    /** The starting cash per player. */
    private static final int STARTING_CASH = 100;
    
    /** The size of the big blind. */
    private static final int BIG_BLIND = 2;
    
    /** The table. */
    private final Table table;
    
    /** The players at the table. */
    private final Map<String, Player> players;
    
    /** The GridBagConstraints. */
    private final GridBagConstraints gc;
    
    /** The board panel. */
    private final BoardPanel boardPanel;
    
    /** The control panel. */
    private final ControlPanel controlPanel;
    
    /** The player panels. */
    private final Map<String, PlayerPanel> playerPanels;
    
    /** The current dealer's name. */
    private String dealerName; 

    /** The current actor's name. */
    private String actorName; 

    /**
     * Constructor.
     */
    public Main() {
        super("Limit Texas Hold'em poker");
        
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(UIConstants.TABLE_COLOR);
        setLayout(new GridBagLayout());

        gc = new GridBagConstraints();
        
        controlPanel = new ControlPanel();
        
        boardPanel = new BoardPanel(controlPanel);        
        addComponent(boardPanel, 1, 1, 1, 1);
        
        players = new LinkedHashMap<String, Player>();
        players.put("Player", new Player("Player", STARTING_CASH, this));
        players.put("Joe",    new Player("Joe",    STARTING_CASH, new DummyBot()));
        players.put("Mike",   new Player("Mike",   STARTING_CASH, new DummyBot()));
        players.put("Eddie",  new Player("Eddie",  STARTING_CASH, new DummyBot()));

        table = new Table(BIG_BLIND);
        for (Player player : players.values()) {
            table.addPlayer(player);
        }
        
        playerPanels = new HashMap<String, PlayerPanel>();
        int i = 0;
        for (Player player : players.values()) {
            PlayerPanel panel = new PlayerPanel();
            playerPanels.put(player.getName(), panel);
            switch (i++) {
                case 0:
                    // North position.
                    addComponent(panel, 1, 0, 1, 1);
                    break;
                case 1:
                    // East position.
                    addComponent(panel, 2, 1, 1, 1);
                    break;
                case 2:
                    // South position.
                    addComponent(panel, 1, 2, 1, 1);
                    break;
                case 3:
                    // West position.
                    addComponent(panel, 0, 1, 1, 1);
                    break;
                default:
                    // Do nothing.
            }
        }
        
        // Show the frame.
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
        setVisible(true);

        // Start the game.
        table.start();
    }
    
    /**
     * The application's entry point.
     * 
     * @param args
     *            The command line arguments.
     */
    public static void main(String[] args) {
        new Main();
    }

    /*
     * (non-Javadoc)
     * @see org.ozsoft.texasholdem.Client#joinedTable(int, java.util.List)
     */
    @Override
    public void joinedTable(int bigBlind, List<Player> players) {
        for (Player player : players) {
            PlayerPanel playerPanel = playerPanels.get(player.getName());
            if (playerPanel != null) {
                playerPanel.update(player);
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see org.ozsoft.texasholdem.Client#messageReceived(java.lang.String)
     */
    @Override
    public void messageReceived(String message) {
        boardPanel.setMessage(message);
        boardPanel.waitForUserInput();
    }

    /*
     * (non-Javadoc)
     * @see org.ozsoft.texasholdem.Client#handStarted(org.ozsoft.texasholdem.Player)
     */
    @Override
    public void handStarted(Player dealer) {
        setDealer(false);
        dealerName = dealer.getName();
        setDealer(true);
    }

    /*
     * (non-Javadoc)
     * @see org.ozsoft.texasholdem.Client#actorRotated(org.ozsoft.texasholdem.Player)
     */
    @Override
    public void actorRotated(Player actor) {
        setActorInTurn(false);
        actorName = actor.getName();
        setActorInTurn(true);
    }

    /*
     * (non-Javadoc)
     * @see org.ozsoft.texasholdem.Client#boardUpdated(java.util.List, int, int)
     */
    @Override
    public void boardUpdated(List<Card> cards, int bet, int pot) {
        boardPanel.update(cards, bet, pot);
    }

    /*
     * (non-Javadoc)
     * @see org.ozsoft.texasholdem.Client#playerUpdated(org.ozsoft.texasholdem.Player)
     */
    @Override
    public void playerUpdated(Player player) {
        PlayerPanel playerPanel = playerPanels.get(player.getName());
        if (playerPanel != null) {
            playerPanel.update(player);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.ozsoft.texasholdem.Client#playerActed(org.ozsoft.texasholdem.Player)
     */
    @Override
    public void playerActed(Player player) {
        String name = player.getName();
        PlayerPanel playerPanel = playerPanels.get(name);
        if (playerPanel != null) {
            playerPanel.update(player);
            Action action = player.getAction();
            if (action != null) {
                boardPanel.setMessage(String.format("%s %s.", name, action.getVerb()));
                //FIXME: Determine actor is the human player.
                if (!name.equals("Player")) {
                    boardPanel.waitForUserInput();
                }
            }
        } else {
            throw new IllegalStateException(
                    String.format("No PlayerPanel found for player '%s'", name));
        }
    }

    /*
     * (non-Javadoc)
     * @see org.ozsoft.texasholdem.Client#act(java.util.Set)
     */
    @Override
    public Action act(Set<Action> allowedActions) {
        boardPanel.setMessage("Please select an action.");
        return controlPanel.getUserInput(allowedActions);
    }

    /**
     * Adds an UI component.
     * 
     * @param component
     *            The component.
     * @param x
     *            The column.
     * @param y
     *            The row.
     * @param width
     *            The number of columns to span.
     * @param height
     *            The number of rows to span.
     */
    private void addComponent(Component component, int x, int y, int width, int height) {
        gc.gridx = x;
        gc.gridy = y;
        gc.gridwidth = width;
        gc.gridheight = height;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        gc.anchor = GridBagConstraints.CENTER;
        gc.fill = GridBagConstraints.NONE;
        getContentPane().add(component, gc);
    }

    /**
     * Sets whether the actor  is in turn.
     * 
     * @param isInTurn
     *            Whether the actor is in turn.
     */
    private void setActorInTurn(boolean isInTurn) {
        if (actorName != null) {
            PlayerPanel playerPanel = playerPanels.get(actorName);
            if (playerPanel != null) {
                playerPanel.setInTurn(isInTurn);
            }
        }
    }

    /**
     * Sets the dealer.
     * 
     * @param isDealer
     *            Whether the player is the dealer.
     */
    private void setDealer(boolean isDealer) {
        if (dealerName != null) {
            PlayerPanel playerPanel = playerPanels.get(dealerName);
            if (playerPanel != null) {
                playerPanel.setDealer(isDealer);
            }
        }
    }

}
