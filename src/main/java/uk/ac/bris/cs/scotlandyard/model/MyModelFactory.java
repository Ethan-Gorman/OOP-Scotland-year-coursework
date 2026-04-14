package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


import com.google.common.collect.ImmutableSet;
import jakarta.annotation.Nonnull;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;


import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

/**
 * cw-model
 * Stage 2: Complete this class
 */
public final class MyModelFactory implements Factory<Model> {


	@Nonnull
	@Override
	public Model build(GameSetup setup,
					   Player mrX,
					   ImmutableList<Player> detectives) {
		GameState initialState = new MyGameStateFactory().build(setup, mrX, detectives);
		return new MyModel(initialState);
	}


	private final class MyModel implements Model {
		private GameState currentState;
		private final List<Observer> observers = new ArrayList<>();
		private MyModel(GameState initialState) {
			this.currentState = initialState;
		}

		@Override
		public Board getCurrentBoard() {
			return currentState;
		}

		@Override
		public void registerObserver(Observer observer) {
			if (observers.contains(observer)) throw new IllegalArgumentException("Already an Observer");
			observers.add(Objects.requireNonNull(observer));
		}

		@Override
		public void unregisterObserver(Observer observer) {
			Objects.requireNonNull(observer);
			if (!observers.contains(observer)) throw new IllegalArgumentException("Observer not registered");
			observers.remove(observer);

		}

		@Override
		public ImmutableSet<Observer> getObservers() {
			return ImmutableSet.copyOf(observers);
		}

		@Override
		public void chooseMove(Move move) {
			//moves game state ahead one move
			currentState = currentState.advance(move);
			//decides the cause of the event
			Observer.Event event = (currentState.getWinner().isEmpty()) ? Observer.Event.MOVE_MADE :  Observer.Event.GAME_OVER;

			//iterates over observers
			//updates them with why state changed and gives current state
			for(Observer observer: observers){
				observer.onModelChanged(getCurrentBoard(),event);
			}
		}
	}


}

