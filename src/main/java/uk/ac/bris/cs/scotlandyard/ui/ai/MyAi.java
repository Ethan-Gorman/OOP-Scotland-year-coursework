package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import jakarta.annotation.Nonnull;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

public class myAi implements Ai {

	@Nonnull @Override public String name() { return "Destroyer of Worlds"; }

	@Override
	public void onStart() {
	}

	@Override
	public void onTerminate() {
	}

	Map<Integer,Integer> djikstra(Board board, int src) {
		ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> g = board.getSetup().graph;
		int numOfNodes = g.nodes().size() + 1;

		//priority queue storing next node to process
		PriorityQueue<Integer> pq = new PriorityQueue<>((a, b) -> a - b);

		//Distance array that stores shortest distance from source
		Map<Integer, Integer> distanceFromSource = new HashMap<>();

		for (int node : g.nodes()) {
			distanceFromSource.put(node, 999);
		}

		//Distance from source to itself is 0
		distanceFromSource.put(src, 0);
		pq.add(src);

		//Process the queue until all vertices are processed
		while (!pq.isEmpty()) {
			int currentNode = pq.poll();

			// Explore all adjacent vertices
			for (int adjacentNode : g.adjacentNodes(currentNode)) {
				//all edges are of weight 1 at the moment
				int newDistance = distanceFromSource.get(currentNode) + 1;

				//if new distance is better than old one we add that as new distance and put it on priority queue
				//then we go through its adjacent nodes
				if (newDistance < distanceFromSource.get(adjacentNode)) {
					distanceFromSource.put(adjacentNode, newDistance);
					pq.add(adjacentNode);
				}
			}
		}

		return distanceFromSource;
	}
}

	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {

		Set<Move> possibleMoves = board.getAvailableMoves().stream()
				.filter(m -> m.commencedBy().isMrX())
				.collect(Collectors.toSet());

		Map<Integer, Map<Integer, Integer>> distanceToDetective = new HashMap<>();


		for (Piece piece : board.getPlayers()) {
			if (piece.isDetective()) {
				Piece.Detective detective = (Piece.Detective) piece;
				Optional<Integer> location = board.getDetectiveLocation(detective);

				if (location.isPresent()) {
					Map<Integer, Integer> distances = djikstra(board, location.get());
				}
			}
		}
			var moves = board.getAvailableMoves().asList();
			return moves.get(new Random().nextInt(moves.size()));
		}
	}