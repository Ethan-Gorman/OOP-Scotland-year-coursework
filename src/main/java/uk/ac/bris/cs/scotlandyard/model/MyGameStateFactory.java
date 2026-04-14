package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import jakarta.annotation.Nonnull;
import uk.ac.bris.cs.scotlandyard.model.Move.*;
import uk.ac.bris.cs.scotlandyard.model.Piece.*;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.HashSet;


import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

/**
 * cw-model
 * Stage 1: Complete this class
 */
public final class MyGameStateFactory implements Factory<GameState> {


	@Nonnull
	@Override
	public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {

		// validation tests

		//check if detectives contain a mrX piece
		if (detectives.stream().anyMatch(p -> p.piece().equals(mrX.piece()))) throw new IllegalArgumentException("More than 1 mrX");
		//No swapped mr X
		if (mrX.isDetective()) throw new IllegalArgumentException("No mrX");
		// throws if duplicate detectives or equal detective location
		Hashtable ht = new Hashtable();
		for (Player player : detectives) {
			if (ht.containsKey(player)) throw new IllegalArgumentException("Duplicate detectives");
			ht.put(player, player);
		}

		// throws if detective locations overlap
		HashSet<Integer> OccupiedLocations = new HashSet<>();
		for (Player player : detectives) {
			if (!OccupiedLocations.add(player.location()))
				throw new IllegalArgumentException("Detective location overlap");
		}

		// throws if detectives have x2 ticket or secret ticket
		for (Player player : detectives) {
			if (player.has(Ticket.DOUBLE) || player.has(Ticket.SECRET))
				throw new IllegalArgumentException("Illegal detective ticket");
		}

		if (setup.moves.isEmpty()) throw new IllegalArgumentException("No moves");
		if (setup.graph.nodes().isEmpty()) throw new IllegalArgumentException("No graph");

		return new MyGameState(setup, ImmutableSet.of(MrX.MRX), ImmutableList.of(), mrX, detectives);
	}

	private final class MyGameState implements GameState {
		private GameSetup setup;
		private ImmutableSet<Piece> remaining;
		private ImmutableList<LogEntry> log;
		private Player mrX;
		private ImmutableList<Player> detectives;
		private ImmutableSet<Move> moves;
		private ImmutableSet<Piece> winner;


		private MyGameState(final GameSetup setup, final ImmutableSet<Piece> remaining, final ImmutableList<LogEntry> log, final Player mrX, final ImmutableList<Player> detectives) {
			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = Objects.requireNonNull(mrX);
			this.detectives = Objects.requireNonNull(detectives);
			this.moves = ImmutableSet.copyOf(getAvailableMoves());


		}

		//code for get functions
		@Override
		public GameSetup getSetup() {
			return this.setup;
		}

		//concatenates a stream of all detectives and mrX mapping detectives to their pieces
		@Override
		public ImmutableSet<Piece> getPlayers() {
			return Stream.concat(Stream.of(mrX.piece()), detectives.stream().map(Player::piece)).collect(ImmutableSet.toImmutableSet());
		}

		//filter for requested detective and return location
		@Override
		public Optional<Integer> getDetectiveLocation(Detective detective) {
			return detectives.stream().filter(p -> p.piece().equals(detective)).map(Player::location).findFirst();
		}

		//decides if piece is mrX or detective
		//returns an optional of that players tickets (optional as can be none)
		@Override
		public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			if (mrX.piece().equals(piece)) return Optional.of(ticket -> mrX.tickets().getOrDefault(ticket, 0));

			for (Player p : detectives) {
				if (p.piece().equals(piece)) return Optional.of(ticket -> p.tickets().getOrDefault(ticket, 0));
			}
			return Optional.empty();
		}

		@Override
		public ImmutableList<LogEntry> getMrXTravelLog() {
			return this.log;
		}

		//helper functions for game over state
		//mrX loses if he cannot move on his turn or a detective is on the same location
		private boolean mrXlose() {if (makeSingleMoves(setup, detectives, mrX, mrX.location()).isEmpty() && makeDoubleMoves(setup, detectives, mrX, mrX.location()).isEmpty() && remaining.contains(mrX.piece()) && remaining.size() == 1) return true;
			if (detectives.stream().anyMatch(d -> d.location() == mrX.location())) return true;
			return false;
		}

		//detectives lose if travel log is full or they run out of tickets to make valid moves
		private boolean detectivesLose() {
			if (log.size() == setup.moves.size() && remaining.contains(mrX.piece())) return true;
			if (detectives.stream().allMatch(p -> p.tickets().values().stream().allMatch(count -> count == 0))) return true;
			return false;
		}

		//decides when game is over and returns set of winning pieces
		@Override
		public ImmutableSet<Piece> getWinner() {
			if (mrXlose()) return detectives.stream().map(Player::piece).collect(ImmutableSet.toImmutableSet());
			if (detectivesLose()) return ImmutableSet.of(mrX.piece());
			return ImmutableSet.of();
		}

		// helper functions for getavailablemoves
		private static HashSet<SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source) {
			HashSet<SingleMove> singleMoves = new HashSet<>();
			//check detectives cannot move onto another detective
			//checks all adjacent nodes for detectives
			for (int destination : setup.graph.adjacentNodes(source)) {
				if (detectives.stream().anyMatch(p -> p.location() == destination)) continue;
				//check each transport option on node and check if player has the corresponding ticket or secret
				for (Transport t : Objects.requireNonNull(setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of()))) {
					if (player.has(t.requiredTicket()) || player.has(Ticket.SECRET)) {

						if (player.has(t.requiredTicket()))
							singleMoves.add(new SingleMove(player.piece(), source, t.requiredTicket(), destination));
						if (player.has(Ticket.SECRET))
							singleMoves.add(new SingleMove(player.piece(), source, Ticket.SECRET, destination));
					}
				}

			}
			return singleMoves;


		}

		// gives all possible double moves
		// omits moves which land on detective or have detective on intermediate location
		private Set<DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player player, int source) {

			//ensures mrX is able to make a double move at this point in the game
			if (player.has(Ticket.DOUBLE) && log.size() + 1 < setup.moves.size()) {
				HashSet<DoubleMove> doubleMoves = new HashSet<>();
				//use single move function to get all possible first moves
				HashSet<SingleMove> FirstMove = makeSingleMoves(setup, detectives, player, source);

				//iterate through possible first moves performing single move on them again to get double
				for (SingleMove first : FirstMove) {

					Player afterFirst = player.use(first.ticket);
					HashSet<SingleMove> SecondMove = makeSingleMoves(setup, detectives, afterFirst, first.destination);

					for (SingleMove second : SecondMove) {

						doubleMoves.add(new DoubleMove(player.piece(), first.source(), first.ticket, first.destination, second.ticket, second.destination));

					}
				}
				return doubleMoves;
			}

			return ImmutableSet.of();
		}

		@Override
		public ImmutableSet<Move> getAvailableMoves() {

			//ensures there's no available moves if game is in game over state
			if (!getWinner().isEmpty()) return ImmutableSet.of();

			//gives all moves for detectives who need to move
			Set<Move> moves = new HashSet<>();
			for (Player player : detectives) {
				if (remaining.contains(player.piece())) {
					moves.addAll(makeSingleMoves(setup, detectives, player, player.location()));
				}
			}

			//returns moves for mrX when it's his turn
			if (remaining.contains(mrX.piece())) {
				moves.addAll(makeSingleMoves(setup, detectives, mrX, mrX.location()));
				moves.addAll(makeDoubleMoves(setup, detectives, mrX, mrX.location()));
			}

			return ImmutableSet.copyOf(moves);
		}

		//helper function for log entries
		private ImmutableList<LogEntry> determineLogEntryType(Ticket ticket, Integer location) {

			if (setup.moves.get(log.size())) return ImmutableList.of(LogEntry.reveal(ticket, location));
			else return ImmutableList.of(LogEntry.hidden(ticket));
		}
		private ImmutableList<LogEntry> determineLogEntryTypeForDouble(Ticket t1, Ticket t2, Integer location1, Integer location2) {
			LogEntry firstMove = (setup.moves.get(log.size())) ? (LogEntry.reveal(t1, location1)) : (LogEntry.hidden(t1));
			LogEntry secondMove = (setup.moves.get(log.size()+1)) ? (LogEntry.reveal(t2, location2)) : (LogEntry.hidden(t2));
			return ImmutableList.of(firstMove,secondMove);

		}


		@Override public GameState advance(Move move) {
			//check if the players move is valid
			if (getAvailableMoves().stream().noneMatch(m -> m.equals(move))) {
				throw new IllegalArgumentException("Illegal move: " + move);
			}

			//visitor pattern

			return move.accept(new Move.Visitor<>(){
				public GameState visit(SingleMove m){
					//new game state if mrX goes
					//returns all detective pieces for remaining, adds move to log
					//changes destination and uses ticket
					if (m.commencedBy().isMrX()){
						//creates a variable concatenating old log and new move
						ImmutableList<LogEntry> newLog = Stream.concat(log.stream(), determineLogEntryType(m.ticket,m.destination).stream()).collect(ImmutableList.toImmutableList());
						return new MyGameState(
							setup,
							detectives.stream().map(Player::piece).collect(ImmutableSet.toImmutableSet()),
							newLog,
							mrX.at(m.destination).use(m.ticket),
							detectives);
					}

					//finds all detectives who are able to move and have a turn left
					Set<Piece> DetectivesWithMoves = new HashSet<>();
					Set<Move> moves = getAvailableMoves();

					for (Player player : detectives) {
						for (Move move : moves) {
							if (move.commencedBy().equals(player.piece())) {
								DetectivesWithMoves.add(player.piece());
							}
						}
					}

					//removes detective who just went
					ImmutableSet<Piece> newRemaining = DetectivesWithMoves.stream()
									.filter(p -> !p.equals(m.commencedBy()))
									.collect(ImmutableSet.toImmutableSet());
					//if all detectives move then it's the next round and mrX has to move
					if (newRemaining.isEmpty()) newRemaining = ImmutableSet.of(mrX.piece());
					return new MyGameState(
							setup,
							newRemaining,
							log,
							mrX.give(m.ticket),
							detectives.stream().map(p -> p.piece().equals(m.commencedBy()) ? p.at(m.destination).use(m.ticket) : p).collect(ImmutableList.toImmutableList()));
					}


				public GameState visit(DoubleMove m){
					//creates variable concatenating the old log and adding the double move
					ImmutableList<LogEntry> newLog = Stream.concat(log.stream(), determineLogEntryTypeForDouble(m.ticket1,m.ticket2,m.destination1,m.destination2).stream())
							.collect(ImmutableList.toImmutableList());

					return new MyGameState(
							setup,
							detectives.stream().map(Player::piece).collect(ImmutableSet.toImmutableSet()),
							newLog,
							mrX.at(m.destination2).use(m.ticket1).use(m.ticket2).use(Ticket.DOUBLE),
							detectives);


				}

				});
		}
    }

}







