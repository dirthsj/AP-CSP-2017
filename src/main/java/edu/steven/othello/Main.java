package edu.steven.othello;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

/**
 * Created by steve on 3/27/2017.
 */
class Main extends Canvas {
    private static final int BOARD_CELL_SIZE = 100;
    private static final int BOARD_LINE_THICKNESS = 10;
    private static final int WINDOW_BOUNDS = BOARD_CELL_SIZE * 8 + BOARD_LINE_THICKNESS;
    private static final Color[][] gamepieces = new Color[8][8];
    private static final Color BACKGROUND_COLOR = new Color( 0, 100, 0 );
    private static final int GAME_PIECE_DIAMETER = 80;
    private static Color currentPlayer = Color.WHITE;
    private static boolean canLastPlayerMove = true;
    private static final JFrame gameFrame = new JFrame();

    public static void main( String[] args ) {
        //setup game pieces as background color
        for( int i = 0; i < 8; i++ ){
            for( int i1 = 0; i1 < 8; i1++ ){
                gamepieces[i][i1] = BACKGROUND_COLOR;
            }
        }
        //setup initial center game pieces
        gamepieces[3][3] = Color.WHITE;
        gamepieces[4][4] = Color.WHITE;
        gamepieces[3][4] = Color.BLACK;
        gamepieces[4][3] = Color.BLACK;
        gameFrame.setTitle( "Othello" );
        //make sure the process exits when window is closed
        gameFrame.setDefaultCloseOperation( WindowConstants.EXIT_ON_CLOSE );
        //make a pane to display everything in
        JPanel pane = new JPanel(){
            @Override
            protected void paintComponent( Graphics g ){
                super.paintComponent(g);
                //draw the game board
                drawGameboard( g );
                //draw individual game pieces
                g.setColor( Color.BLACK );
                for( int row = 0; row < 8; row++){
                    for( int column = 0; column < 8; column ++ ){
                        if (gamepieces[ row ][ column ] == BACKGROUND_COLOR && isMoveValid(row, column)) {
                            //show available moves
                            drawGamePiece( g, row, column, Color.BLUE );
                        }else {
                            drawGamePiece(g, row, column, gamepieces[row][column]);
                        }
                    }
                }
            }
        };

        pane.addMouseListener( new MouseListener(){
            final ActionListener aiMove = new ActionListener(){
                public void actionPerformed(ActionEvent e) {
                    //we don't need to do this more than once in a row; stop the timer
                    timer.stop();
                    //get all the available moves
                    ArrayList<Point> possibleMoves = getCurrentlyValidMoves();
                    canLastPlayerMove = false;
                    try {
                        //check if we can move
                        if( possibleMoves.size() == 0 ){
                            return;
                        }
                        //make an array to store the best moves available
                        ArrayList<Point> bestMoves = new ArrayList<Point>();
                        //the lower the level, the higher priority is assigned to the move
                        //a move of level 10 is the absolute lowest
                        //this heuristic follows the strategy I use, omitting situation-specific content
                        int level = 10;
                        for (Point p : possibleMoves) {
                            int x = (int) p.getX();
                            int y = (int) p.getY();
                            if ((x == 0 || x == 7) && (y == 0 || y == 7)) {
                                if (level > 0) {
                                    bestMoves.clear();
                                    level = 0;
                                }
                                bestMoves.add( p );
                            } else if (level >= 1 && (x == 0 || y == 0 || x == 7 || y == 7)) {
                                if (level > 1) {
                                    bestMoves.clear();
                                    level = 1;
                                }
                                bestMoves.add( p );
                            } else if (level >= 2 && (x > 2 && x < 6 && y > 2 && y < 6)) {
                                if ( level > 2) {
                                    bestMoves.clear();
                                    level = 2;
                                }
                                bestMoves.add( p );
                            } else if (level >= 3 && x != 1 && x != 6 && y != 1 && y != 6) {
                                if (level > 3) {
                                    bestMoves.clear();
                                    level = 3;
                                }
                                bestMoves.add(p);
                            } else if (level >= 4) {
                                bestMoves.add(p);
                            }
                        }
                        //for debugging purposes, output the level of move chosen by the ai
                        System.out.println(level);
                        //select a random move from the pool of best moves
                        Point move = bestMoves.get((int) (Math.random() * bestMoves.size()));
                        int aix = (int) move.getX();
                        int aiy = (int) move.getY();
                        //move there
                        attemptMove(aix, aiy);
                        gamepieces[aix][aiy] = currentPlayer;
                        //the ai moved, so this is true
                        canLastPlayerMove = true;
                    } finally { //if the ai moved or if it didn't
                        //change the player
                        currentPlayer = Color.WHITE;
                        gameFrame.repaint();
                        //if the human player has no moves left
                        if( getCurrentlyValidMoves().size() == 0 ){
                            if( canLastPlayerMove ){ //... and the ai could move
                                //switch players, enable the ai to move again in 1 second
                                currentPlayer = Color.BLACK;
                                timer.start();
                            }else{ //... and the ai couldn't move
                                gameOver();
                            }
                        }
                    }
                }
            };
            //timer allows the ai to move 1 second after the player
            private final Timer timer = new Timer( 1000, aiMove );

            public void mouseClicked(MouseEvent e) {

            }

            //mousePressed is used to avoid clicks not registering if the mouse is moving
            public void mousePressed(MouseEvent e) {
                //transpose the coordinates to the gameboard (each tile is 100x100 px)
                int x = (int)(e.getX() * 0.01 );
                int y = (int)(e.getY() * 0.01 );
                //if the ai isn't moving and the move is valid
                if( !timer.isRunning() && attemptMove( x, y ) ) {
                    //move there
                    gamepieces[x][y] = currentPlayer;
                    gameFrame.repaint();
                    //switch to the ai
                    currentPlayer = Color.BLACK;
                    timer.start();
                    //the player could move
                    canLastPlayerMove = true;
                }
            }

            public void mouseReleased(MouseEvent e) {

            }

            public void mouseEntered(MouseEvent e) {

            }

            public void mouseExited(MouseEvent e) {

            }
        });
        //add the pane to the frame and set size
        gameFrame.add( pane );
        gameFrame.setSize( WINDOW_BOUNDS, WINDOW_BOUNDS );
        gameFrame.setVisible( true );
        //the insets take up room, so we have to account for them
        //this includes the draggable edges of the window, and the title bar
        Insets insets = gameFrame.getInsets();
        gameFrame.setSize( WINDOW_BOUNDS + insets.right + insets.left, WINDOW_BOUNDS + insets.top + insets.bottom );
    }

    @SuppressWarnings("ConstantConditions")
    private static boolean attemptMove(int x, int y ){
        if( gamepieces[x][y] == BACKGROUND_COLOR){ //if it's currently empty
            int path[] = new int[8]; //make an array to store distances form each path
            for( int distance = 1; distance < 8; distance ++ ){
                //check each diagonal, horizontal, and vertical
                checkPath( path, 0, x + distance, y, distance );
                checkPath( path, 1, x + distance, y + distance, distance );
                checkPath( path, 2, x, y + distance, distance );
                checkPath( path, 3, x - distance, y, distance );
                checkPath( path, 4, x - distance, y - distance, distance );
                checkPath( path, 5, x, y - distance, distance );
                checkPath( path, 6, x + distance, y - distance, distance );
                checkPath( path, 7, x - distance, y + distance, distance );
            }
            //register the flipped pieces
            for( int distance = 8; distance > 0; distance -- ){
                if( path[ 0 ] >= distance ){
                    gamepieces[ x + distance ][ y ] = currentPlayer;
                }
                if( path[ 1 ] >= distance ){
                    gamepieces[ x + distance ][ y + distance ] = currentPlayer;
                }
                if( path[ 2 ] >= distance ){
                    gamepieces[ x ][ y + distance ] = currentPlayer;
                }
                if( path[ 3 ] >= distance ){
                    gamepieces[ x - distance ][ y ] = currentPlayer;
                }
                if( path[ 4 ] >= distance ){
                    gamepieces[ x - distance ][ y - distance ] = currentPlayer;
                }
                if( path[ 5 ] >= distance ){
                    gamepieces[ x ][ y - distance ] = currentPlayer;
                }
                if( path[ 6 ] >= distance ){
                    gamepieces[ x + distance ][ y - distance ] = currentPlayer;
                }
                if( path[ 7 ] >= distance ){
                    gamepieces[ x - distance ][ y + distance ] = currentPlayer;
                }
            }
            //check if any pieces were actually flipped; if they were, the move was valid
            for( int i = 0; i < 8; i++ ){
                if( path[ i ] > 1 ){
                    return true;
                }
            }
        }
        return false;
    }
    
    private static void checkPath( int[] path, int index, int x, int y, int distance ){
        //this function attempts to find the first piece of the current player along a path
        //if it is successful, the distance is recorded
        //if unsuccessful, -1 is recorded, or in the case of hitting the edge of the board, 0

        //check bounds; it is possible for the parameters to be outside the gameboard
        if( x < 0 || x > 7 || y < 0 || y > 7 ){
            return;
        }
        //if the current path's length is 0 and we have found the board
        if( path[ index ] == 0 && gamepieces[ x ][ y ] == BACKGROUND_COLOR){
            //set the index to -1 to signify no flips are possible
            path[ index ] = -1;
        }else if( path[ index ] == 0 && gamepieces[ x ][ y ] == currentPlayer ){
            //if the path's length is 0 but we found a piece from the current player
            //note that location in the path
            path[ index ] = distance;
        }
    }

    private static boolean isMoveValid( int x, int y ){
        //this checks the move in the same way attemptMove does, but does not change the gameboard
        int path[] = new int[8];
        for( int distance = 1; distance < 8; distance ++ ){
            checkPath( path, 0, x + distance, y, distance );
            checkPath( path, 1, x + distance, y + distance, distance );
            checkPath( path, 2, x, y + distance, distance );
            checkPath( path, 3, x - distance, y, distance );
            checkPath( path, 4, x - distance, y - distance, distance );
            checkPath( path, 5, x, y - distance, distance );
            checkPath( path, 6, x + distance, y - distance, distance );
            checkPath( path, 7, x - distance, y + distance, distance );
        }
        for( int i = 0; i < 8; i++ ){
            if( path[ i ] > 1 ){
                return true;
            }
        }
        return false;
    }

    private static ArrayList<Point> getCurrentlyValidMoves(){
        //construct a list to store our moves
        ArrayList<Point> currentlyValidMoves = new ArrayList<Point>();
        //iterate through the gameboard, checking if the move is valid
        for( int row = 0; row < 8; row++ ){
            for( int column = 0; column < 8; column++ ){
                if( gamepieces[ row ][ column ] == BACKGROUND_COLOR && isMoveValid( row, column ) ){
                    //if it is valid, add it to our list
                    currentlyValidMoves.add( new Point( row, column ) );
                }
            }
        }
        return currentlyValidMoves;
    }

    private static void gameOver() {
        System.out.println( "Game Over Detected!" );
        int black = 0;
        int white = 0;
        //count the black and white tiles
        for( Color[] column: gamepieces ){
            for( Color c: column ){
                if( c == Color.BLACK ){
                    black++;
                }else if( c == Color.WHITE ){
                    white++;
                }
            }
        }
        JFrame gameOver = new JFrame( "Game Over" );
        JLabel label = new JLabel( "Game Over: " + (black > white ? "Black" : "White") + " Wins!" );
        if( black == white ){
            label.setText( "Game Over: It's a Tie!" );
        }
        //set the font size to 100
        Font f = label.getFont();
        label.setFont(f.deriveFont(f.getStyle(),100));
        //center align the label
        label.setHorizontalAlignment(JLabel.CENTER);
        gameOver.add( label );
        //set the size of the window to accommodate the text (note: 60 is an approximation)
        gameOver.setSize( label.getText().length() * 60, 300 );
        gameOver.setVisible( true );
        //ensure closing the game over window will close the application
        gameOver.setDefaultCloseOperation( WindowConstants.EXIT_ON_CLOSE );
        //remove the game frame from view
        gameFrame.setVisible( false );
    }

    private static void drawGameboard( Graphics g ){
        g.setColor(BACKGROUND_COLOR);
        g.fillRect( 0, 0, BOARD_CELL_SIZE * 8, BOARD_CELL_SIZE * 8 );
        g.setColor( Color.BLACK );
        for( int i = 0; i<9; i++ ){
            g.fillRect( i * BOARD_CELL_SIZE, 0, BOARD_LINE_THICKNESS, WINDOW_BOUNDS );
            g.fillRect( 0, i * BOARD_CELL_SIZE, WINDOW_BOUNDS, BOARD_LINE_THICKNESS );
        }
    }

    private static void drawGamePiece( Graphics g, int x, int y, Color color ){
        g.setColor( color );
        g.fillOval( BOARD_LINE_THICKNESS + 5 + x * BOARD_CELL_SIZE, BOARD_LINE_THICKNESS + 5 + y * BOARD_CELL_SIZE, GAME_PIECE_DIAMETER, GAME_PIECE_DIAMETER );
    }

}