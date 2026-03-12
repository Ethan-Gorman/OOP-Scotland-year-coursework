package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import jakarta.annotation.Nonnull;
import uk.ac.bris.cs.scotlandyard.model.Move.*;
import uk.ac.bris.cs.scotlandyard.model.Piece.*;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.*;
import java.util.*;
import java.util.stream.Stream;
import java.util.HashSet;


import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

/**
 * cw-model
 * Stage 1: Complete this class
 */
public final class MyGameStateFactory implements Factory<GameState> {


	@Nonnull @Override public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {

		// validation tests

		// throws if Mr X or detectives are null
		Objects.requireNonNull(mrX);
		Objects.requireNonNull(detectives);
		//check if detectives contain a mrX piece
		if (detectives.stream().anyMatch(p -> p.piece().equals(mrX.piece()))) {
			throw new IllegalArgumentException("More than 1 mrX");
		}
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
			if (!OccupiedLocations.add(player.location())) throw new IllegalArgumentException("Detective location overlap");

		}

		// throws if detectives have x2 ticket or secret ticket
		for (Player player : detectives) {
			if (player.has(Ticket.DOUBLE) || player.has(Ticket.SECRET)) throw new IllegalArgumentException("Illegal detective ticket");
		}

		if (setup.moves.isEmpty()) throw new IllegalArgumentException("No moves");

		return new MyGameState(setup, ImmutableSet.of(MrX.MRX), ImmutableList.of(), mrX, detectives);
	}

	private final class MyGameState implements GameState {
		private GameSetup setup;
		private ImmutableSet<Piece> remaining;
		private ImmutableList<LogEntry> log;
		private Player mrX;
		private List<Player> detectives;
		private ImmutableSet<Move> moves;
		private ImmutableSet<Piece> winner;



		private MyGameState(final GameSetup setup,final ImmutableSet<Piece> remaining, final ImmutableList<LogEntry> log, final Player mrX, final List<Player> detectives){
			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;

		}


		@Override public GameSetup getSetup() {
			return this.setup;
		}

		@Override  public ImmutableSet<Piece> getPlayers() {
			return Stream.concat(Stream.of(mrX.piece()), detectives.stream().map(Player::piece)).collect(ImmutableSet.toImmutableSet());
		}

		@Override
		public Optional<Integer> getDetectiveLocation(Detective detective) {
			for(Player p : detectives) {
				if(p.piece().equals(detective)) {
					return Optional.of(p.location());
				}
			}
			return Optional.empty();
		}

		@Override
		public Optional<TicketBoard> getPlayerTickets(Piece piece) {

            return Optional.empty();
		}

		@Override
		public ImmutableList<LogEntry> getMrXTravelLog() {
			return null;
		}

		@Override
		public ImmutableSet<Piece> getWinner() {
			return winner;
		}

		@Override
		public ImmutableSet<Move> getAvailableMoves() {
			return null;
		}

		@Override public GameState advance(Move move) {  return null;  }
	}

}
