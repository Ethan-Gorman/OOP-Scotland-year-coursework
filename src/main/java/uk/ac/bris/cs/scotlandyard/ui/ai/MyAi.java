package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import jakarta.annotation.Nonnull;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

public class MyAi implements Ai {

	@Nonnull @Override public String name() { return "Destroyer of Worlds"; }

	@Override
	public void onStart() {
	}

	@Override
	public void onTerminate() {
	}

	//simple djikstra algorithm inspiration from princeton version in lectures
	Map<Integer,Integer> djikstra(Board board, int src) {
		ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> g = board.getSetup().graph;
		int numOfNodes = g.nodes().size() + 1;

		//priority queue storing next node
		PriorityQueue<Integer> pq = new PriorityQueue<>((a, b) -> a - b);

		//hashmap that stores shortest distance from source
		Map<Integer, Integer> distanceFromSource = new HashMap<>();

		for (int node : g.nodes()) {
			distanceFromSource.put(node, 999);
		}

		distanceFromSource.put(src, 0);
		pq.add(src);

		//go through queue until all vertices are processed
		while (!pq.isEmpty()) {
			int currentNode = pq.poll();

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

@Nonnull @Override public Move pickMove(
		@Nonnull Board board,
		Pair<Long, TimeUnit> timeoutPair) {

	Set<Move> possibleMoves = board.getAvailableMoves().stream()
			.filter(m -> m.commencedBy().isMrX())
			.collect(Collectors.toSet());

	Map<Piece, Map<Integer, Integer>> distanceToDetective = new HashMap<>();
	Set<Piece> detectives = board.getPlayers().stream().filter(p -> p.isDetective()).collect(Collectors.toSet());


	//goes through every detective and finds shortest distance to every node from current node
	for (Piece piece : detectives) {
			Piece.Detective detective = (Piece.Detective) piece;
			Optional<Integer> location = board.getDetectiveLocation(detective);

			if (location.isPresent()) {
				Map<Integer, Integer> distances = djikstra(board, location.get());
				distanceToDetective.put(piece,distances);
			}
	}

	Move bestMove = null;
	int bestScore = -1;
	//finds move that maximises distance from closest detective
	for (Move move : possibleMoves) {

		int minDistance = Integer.MAX_VALUE;

		int destination = move.accept(new Move.Visitor<>() {

			public Integer visit(Move.SingleMove m) {
				return m.destination;
			}

			public Integer visit(Move.DoubleMove m) {
				return m.destination2;
			}
		});

		for (Piece detective : detectives) {
			int newDistance = distanceToDetective.get(detective).get(destination);
			minDistance = Math.min(minDistance, newDistance);
		}

		if (minDistance > bestScore) {
			bestScore = minDistance;
			bestMove = move;
		}
	}

	if (bestMove == null) {
		var moves = board.getAvailableMoves().asList();
		return moves.get(new Random().nextInt(moves.size()));
	}
	return bestMove;
}}
