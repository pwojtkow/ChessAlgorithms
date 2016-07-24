package com.capgemini.chess.algorithms.implementation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.capgemini.chess.algorithms.data.Coordinate;
import com.capgemini.chess.algorithms.data.Move;
import com.capgemini.chess.algorithms.data.enums.BoardState;
import com.capgemini.chess.algorithms.data.enums.Color;
import com.capgemini.chess.algorithms.data.enums.MoveType;
import com.capgemini.chess.algorithms.data.enums.Piece;
import com.capgemini.chess.algorithms.data.enums.PieceType;
import com.capgemini.chess.algorithms.data.generated.Board;
import com.capgemini.chess.algorithms.implementation.exceptions.InvalidMoveException;
import com.capgemini.chess.algorithms.implementation.exceptions.KingInCheckException;
import com.capgemini.chess.algorithms.strategy.BishopMoveStrategyValidation;
import com.capgemini.chess.algorithms.strategy.KnightMoveStrategyValidation;
import com.capgemini.chess.algorithms.strategy.MoveStrategyValidation;
import com.capgemini.chess.algorithms.strategy.StrategiesHolder;
import com.capgemini.chess.algorithms.strategy.PawnMoveStrategyValidation;
import com.capgemini.chess.algorithms.strategy.QueenMoveStrategyValidation;
import com.capgemini.chess.algorithms.strategy.RookMoveStrategyValidation;

/**
 * Class for managing of basic operations on the Chess Board.
 *
 * @author Michal Bejm
 *
 */
public class BoardManager {

	private static final int ALL_SQUARES_IN_BOARD = 64;
	private Board board = new Board();

	public BoardManager() {
		initBoard();
	}

	public BoardManager(List<Move> moves) {
		initBoard();
		for (Move move : moves) {
			addMove(move);
		}
	}

	public BoardManager(Board board) {
		this.board = board;
	}

	/**
	 * Getter for generated board
	 *
	 * @return board
	 */
	public Board getBoard() {
		return this.board;
	}

	/**
	 * Performs move of the chess piece on the chess board from one field to
	 * another.
	 *
	 * @param from
	 *            coordinates of 'from' field
	 * @param to
	 *            coordinates of 'to' field
	 * @return move object which includes moved piece and move type
	 * @throws InvalidMoveException
	 *             in case move is not valid
	 */
	public Move performMove(Coordinate from, Coordinate to) throws InvalidMoveException {

		Move move = validateMove(from, to);
		addMove(move);

		return move;
	}

	/**
	 * Calculates state of the chess board.
	 *
	 * @return state of the chess board
	 */
	public BoardState updateBoardState() {

		Color nextMoveColor = calculateNextMoveColor();

		boolean isKingInCheck = isKingInCheck(nextMoveColor);
		boolean isAnyMoveValid = isAnyMoveValid(nextMoveColor);

		BoardState boardState;
		if (isKingInCheck) {
			if (isAnyMoveValid) {
				boardState = BoardState.CHECK;
			} else {
				boardState = BoardState.CHECK_MATE;
			}
		} else {
			if (isAnyMoveValid) {
				boardState = BoardState.REGULAR;
			} else {
				boardState = BoardState.STALE_MATE;
			}
		}
		this.board.setState(boardState);
		return boardState;
	}

	/**
	 * Checks threefold repetition rule (one of the conditions to end the chess
	 * game with a draw).
	 *
	 * @return true if current state repeated at list two times, false otherwise
	 */
	public boolean checkThreefoldRepetitionRule() {

		// there is no need to check moves that where before last capture/en
		// passant/castling
		int lastNonAttackMoveIndex = findLastNonAttackMoveIndex();
		List<Move> omittedMoves = this.board.getMoveHistory().subList(0, lastNonAttackMoveIndex);
		BoardManager simulatedBoardManager = new BoardManager(omittedMoves);

		int counter = 0;
		for (int i = lastNonAttackMoveIndex; i < this.board.getMoveHistory().size(); i++) {
			Move moveToAdd = this.board.getMoveHistory().get(i);
			simulatedBoardManager.addMove(moveToAdd);
			boolean areBoardsEqual = Arrays.deepEquals(this.board.getPieces(),
					simulatedBoardManager.getBoard().getPieces());
			if (areBoardsEqual) {
				counter++;
			}
		}

		return counter >= 2;
	}

	/**
	 * Checks 50-move rule (one of the conditions to end the chess game with a
	 * draw).
	 *
	 * @return true if no pawn was moved or not capture was performed during
	 *         last 50 moves, false otherwise
	 */
	public boolean checkFiftyMoveRule() {

		// for this purpose a "move" consists of a player completing his turn
		// followed by his opponent completing his turn
		if (this.board.getMoveHistory().size() < 100) {
			return false;
		}

		for (int i = this.board.getMoveHistory().size() - 1; i >= this.board.getMoveHistory().size() - 100; i--) {
			Move currentMove = this.board.getMoveHistory().get(i);
			PieceType currentPieceType = currentMove.getMovedPiece().getType();
			if (currentMove.getType() != MoveType.ATTACK || currentPieceType == PieceType.PAWN) {
				return false;
			}
		}

		return true;
	}

	// PRIVATE

	private void initBoard() {

		this.board.setPieceAt(Piece.BLACK_ROOK, new Coordinate(0, 7));
		this.board.setPieceAt(Piece.BLACK_KNIGHT, new Coordinate(1, 7));
		this.board.setPieceAt(Piece.BLACK_BISHOP, new Coordinate(2, 7));
		this.board.setPieceAt(Piece.BLACK_QUEEN, new Coordinate(3, 7));
		this.board.setPieceAt(Piece.BLACK_KING, new Coordinate(4, 7));
		this.board.setPieceAt(Piece.BLACK_BISHOP, new Coordinate(5, 7));
		this.board.setPieceAt(Piece.BLACK_KNIGHT, new Coordinate(6, 7));
		this.board.setPieceAt(Piece.BLACK_ROOK, new Coordinate(7, 7));

		for (int x = 0; x < Board.SIZE; x++) {
			this.board.setPieceAt(Piece.BLACK_PAWN, new Coordinate(x, 6));
		}

		this.board.setPieceAt(Piece.WHITE_ROOK, new Coordinate(0, 0));
		this.board.setPieceAt(Piece.WHITE_KNIGHT, new Coordinate(1, 0));
		this.board.setPieceAt(Piece.WHITE_BISHOP, new Coordinate(2, 0));
		this.board.setPieceAt(Piece.WHITE_QUEEN, new Coordinate(3, 0));
		this.board.setPieceAt(Piece.WHITE_KING, new Coordinate(4, 0));
		this.board.setPieceAt(Piece.WHITE_BISHOP, new Coordinate(5, 0));
		this.board.setPieceAt(Piece.WHITE_KNIGHT, new Coordinate(6, 0));
		this.board.setPieceAt(Piece.WHITE_ROOK, new Coordinate(7, 0));

		for (int x = 0; x < Board.SIZE; x++) {
			this.board.setPieceAt(Piece.WHITE_PAWN, new Coordinate(x, 1));
		}
	}

	private void addMove(Move move) {

		addRegularMove(move);

		if (move.getType() == MoveType.CASTLING) {
			addCastling(move);
		} else if (move.getType() == MoveType.EN_PASSANT) {
			addEnPassant(move);
		}

		this.board.getMoveHistory().add(move);
	}

	private void addRegularMove(Move move) {
		Piece movedPiece = this.board.getPieceAt(move.getFrom());
		this.board.setPieceAt(null, move.getFrom());
		this.board.setPieceAt(movedPiece, move.getTo());

		performPromotion(move, movedPiece);
	}

	private void performPromotion(Move move, Piece movedPiece) {
		if (movedPiece == Piece.WHITE_PAWN && move.getTo().getY() == (Board.SIZE - 1)) {
			this.board.setPieceAt(Piece.WHITE_QUEEN, move.getTo());
		}
		if (movedPiece == Piece.BLACK_PAWN && move.getTo().getY() == 0) {
			this.board.setPieceAt(Piece.BLACK_QUEEN, move.getTo());
		}
	}

	private void addCastling(Move move) {
		if (move.getFrom().getX() > move.getTo().getX()) {
			Piece rook = this.board.getPieceAt(new Coordinate(0, move.getFrom().getY()));
			this.board.setPieceAt(null, new Coordinate(0, move.getFrom().getY()));
			this.board.setPieceAt(rook, new Coordinate(move.getTo().getX() + 1, move.getTo().getY()));
		} else {
			Piece rook = this.board.getPieceAt(new Coordinate(Board.SIZE - 1, move.getFrom().getY()));
			this.board.setPieceAt(null, new Coordinate(Board.SIZE - 1, move.getFrom().getY()));
			this.board.setPieceAt(rook, new Coordinate(move.getTo().getX() - 1, move.getTo().getY()));
		}
	}

	private void addEnPassant(Move move) {
		Move lastMove = this.board.getMoveHistory().get(this.board.getMoveHistory().size() - 1);
		this.board.setPieceAt(null, lastMove.getTo());
	}

	private Move validateMove(Coordinate from, Coordinate to) throws InvalidMoveException, KingInCheckException {
		StrategiesHolder strategiesHolder = new StrategiesHolder();

		Piece actualPiece = board.getPieceAt(from);

		MoveStrategyValidation validator = strategiesHolder.findValidationStrategy(actualPiece.getType());
		MoveType checkingMoveType = validator.checkMoveValidation(from, to, board);
		Move checkedMove = creatingMoveToReturn(from, to, actualPiece, checkingMoveType);
		return checkedMove;
	}

	private Move creatingMoveToReturn(Coordinate from, Coordinate to, Piece actualPiece, MoveType checkingMoveType) {
		Move checkingMove = new Move();
		checkingMove.setFrom(from);
		checkingMove.setTo(to);
		checkingMove.setMovedPiece(actualPiece);
		checkingMove.setType(checkingMoveType);
		return checkingMove;
	}

	//ZMIANA NA PRIVATE
	public boolean isKingInCheck(Color kingColor) {
		Map<Piece, Coordinate> piecesAtBoard = new HashMap<Piece, Coordinate>();
		Coordinate kingsCoordinate = null;

		getAllEnemyPiecesAtBoard(kingColor, piecesAtBoard);

		return checkEveryPieceForCheck(piecesAtBoard, kingsCoordinate);
	}

	private boolean checkEveryPieceForCheck(Map<Piece, Coordinate> piecesAtBoard, Coordinate kingsCoordinate) {
		for (Piece piece : piecesAtBoard.keySet()) {
			try {
				Move validatedMove = validateMove(piecesAtBoard.get(piece), kingsCoordinate);
				if (validatedMove.getType() == MoveType.CAPTURE) {
					return true;
				} else {
					return false;
				}
			} catch (Exception ex) {
				return false;
			}
		}
		return false;
	}

	private void getAllEnemyPiecesAtBoard(Color kingColor, Map<Piece, Coordinate> piecesAtBoard) {
		for (int i = 0; i < board.getPieces().length; i++) {
			for (int j = 0; j < board.getPieces()[i].length; j++) {
				setKingsCoordinate(kingColor, i, j);
				if (isSquareNotEmpty(i, j) && isPieceInDifferentColor(kingColor, i, j)) {
					Coordinate actualCoordinate = new Coordinate(i, j);
					piecesAtBoard.put(board.getPieceAt(actualCoordinate), actualCoordinate);
				}
			}
		}
	}

	private void setKingsCoordinate(Color kingColor, int i, int j) {
		Coordinate kingsCoordinate;
		if (board.getPieceAt(new Coordinate(i, j)).getType() == PieceType.KING) {
			if (board.getPieceAt(new Coordinate(i, j)).getColor() == kingColor) {
				kingsCoordinate = new Coordinate(i, j);
			}
		}
	}

	private boolean isPieceInDifferentColor(Color kingColor, int i, int j) {
		return board.getPieceAt(new Coordinate(i, j)).getColor() != kingColor;
	}

	private boolean isSquareNotEmpty(int i, int j) {
		return board.getPieceAt(new Coordinate(i, j)) != null;
	}

	private boolean isAnyMoveValid(Color nextMoveColor) {
		List<Coordinate> piecesAtBoard = new ArrayList<Coordinate>();
		int counter = 0;
		addEnemyPiecesToList(nextMoveColor, piecesAtBoard);
		counter = countNumberOfValidMoves(piecesAtBoard, counter);
		if (counter == 0) {
			return false;
		} else {
			return true;
		}
	}

	private int countNumberOfValidMoves(List<Coordinate> piecesAtBoard, int counter) {
		for (int i = 0; i < piecesAtBoard.size(); i++) {
			for (int j = 0; j < ALL_SQUARES_IN_BOARD; j++) {
				try {
					if (validateMove(piecesAtBoard.get(i), new Coordinate(i, j)) != null) {
						counter++;
					}
				} catch (Exception ex) {
				}
			}
		}
		return counter;
	}

	private void addEnemyPiecesToList(Color nextMoveColor, List<Coordinate> piecesAtBoard) {
		for (int i = 0; i < board.getPieces().length; i++) {
			for (int j = 0; j < board.getPieces()[i].length; j++) {
				if (isSquareNotEmpty(i, j) && isPieceInDifferentColor(nextMoveColor, i, j)) {
					Coordinate actualCoordinate = new Coordinate(i, j);
					piecesAtBoard.add(actualCoordinate);
				}
			}
		}
	}

	private Color calculateNextMoveColor() {
		if (this.board.getMoveHistory().size() % 2 == 0) {
			return Color.WHITE;
		} else {
			return Color.BLACK;
		}
	}

	private int findLastNonAttackMoveIndex() {
		int counter = 0;
		int lastNonAttackMoveIndex = 0;

		for (Move move : this.board.getMoveHistory()) {
			if (move.getType() != MoveType.ATTACK) {
				lastNonAttackMoveIndex = counter;
			}
			counter++;
		}

		return lastNonAttackMoveIndex;
	}

}
