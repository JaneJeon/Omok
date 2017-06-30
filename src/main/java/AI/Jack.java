package AI;

import GUI.오목;
import MyDataStructures.*;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;

import static Utils.Functions.*;
import static Utils.LoadResource.printPoint;
import static Utils.ManualCopy.*;

/*
 * @author: Sungil Ahn
 */
public class Jack {
	private static final int SUFFICIENTLY_LARGE_NUMBER = 297_000_000;
	private static final int UNDO_LIMIT = 6;
	private final History history; // for undo functionality
	private final Map<Integer, String> visited; // for testing purposes & keeping track of the order of pieces
	private final boolean headless;
	private boolean DEBUG, stop = false;
	private int clashEvalMethod = 1;
	private int DEPTH_LIMIT = 9; // keep this odd!
	private int BRANCH_LIMIT = 5; // decrease to massively improve performance at cost of accuracy
	private int M = 2, M2 = 4; // multiplier for scores - should be either 2 or 3
	private double DEFENSE_WEIGHT = 0.92; // to encourage prioritizing offense over defense
	private double THRESHOLD = 2.0 / 3; // the effect seems to be most significant for high depths
	private int turn = 1, nodes, error; // turn: -1 for white, 1 for black
	private int[][] board; // main board for storing pieces
	private IB scores; // for storing board space scores for each branch
	private Map<Point, List<List<PI>>> threatSpaces; // threat -> threat space lines -> space & score
	private Map<Point, List<List<Point>>> lookup; // threat space (incl. 0) -> list of threat sequences
	private 오목 player;
	private List<DoubleInt> perf;
	// TODO: fix double-docking issue in step
	// TODO: optimization - just ignore scores & spaces that are insignificant, in step and calculate score
	// TODO: reduce object creation rate by monitoring memory heap
	// TODO: read off the book for the first few moves, especially on hard difficulty
	// TODO: make AI take the shortest KO. On a related note,
	// TODO: physically force AI to ONLY consider defense moves when it detects an attack (override pq)
	// TODO: reach deeper for better moves?
	
	// Performance notes: while fastutil seems to be faster than the reference Java Collection in most cases,
	// it seems that HashMap is actually faster than Object2ObjectOpenHashMap by a bit.
	// In addition, while int2obj and obj2int fastutil collections seem slower and/or less consistent at first,
	// it has notable performance increase in deeper depths, with less warmup
	
	// RNG adds performance cost, and somehow this version is ~60% slower than my previous versions.
	// TODO: Why?

	// constructor
	public Jack(boolean headless) {
		// if headless, prevent callback
		this.headless = headless;
		board = new int[19][19];
		threatSpaces = new HashMap<>();
		lookup = new HashMap<>();
		scores = new IB(new int[19][19], false);
		history = new History(UNDO_LIMIT);
		error = 0;
		visited = new HashMap<>();
//		perf = new ObjectArrayList<>();								// TEST
//		for (int i = 0; i < 6; i++) perf.add(new DoubleInt());		// TEST
	}

	public Jack board(오목 client) {
		player = client;
		return this;
	}

	public Jack defenseWeight(double weight) {
		DEFENSE_WEIGHT = weight;
		return this;
	}

	public Jack threshold(double threshold) {
		THRESHOLD = threshold;
		return this;
	}

	public Jack pieceScore(int M) {
		this.M = M;
		M2 = M * M;
		return this;
	}

	public Jack evalMethod(int eval) {
		clashEvalMethod = eval;
		return this;
	}

	public Jack branchLimit(int limit) {
		BRANCH_LIMIT = limit;
		return this;
	}

	public Jack debug(boolean test) {
		DEBUG = test;
		return this;
	}

	public Jack depth(int depth) {
		DEPTH_LIMIT = depth;
		return this;
	}
	
	public void stop() {
		stop = true;
	}

	// officially adds point, modifying the actual threatSpaces and lookup
	public void addPoint(int x, int y) {
		history.add(board, scores, threatSpaces, lookup);
		board[x][y] = turn;
		turn = -turn;
		// add point to lookup and threatSpaces
		threatSpaces = step(x, y, threatSpaces, lookup, turn, board);
		lookup = hash(lookup, x, y, board, turn);
		visited.put(visited.size(), printPoint(new Point(x, y)));
		scores = calculateScores(lookup, threatSpaces, board, turn, visited);
	}

	public void undo() {
		Object[] val = history.pop();
		System.out.println("MyDataStructures.History size: " + history.getSize());
		board = (int[][]) val[0];
		scores = (IB) val[1];
		threatSpaces = (Map) val[2];
		lookup = (Map) val[3];
		turn = -turn;
		visited.remove(visited.size() - 1);
	}

	// modifies sequences, threat spaces, and scores given a new point
	private Map<Point, List<List<PI>>> step(int x, int y, Map<Point, List<List<PI>>> threatSpaces, Map<Point,
			List<List<Point>>> lookup, int turn, int[][] board) {
		Map<Point, List<List<PI>>> result = new HashMap<>(threatSpaces.size() + 1);
		// first, alternate scores as ones that are affected and not affected both need to alternate scores
		for (Point threat : threatSpaces.keySet()) {
			List<List<PI>> updatedList = new ObjectArrayList<>(threatSpaces.get(threat).size());
			for (List<PI> threatLine : threatSpaces.get(threat)) {
//				double startTime = System.nanoTime();					// TEST - 5
				List<PI> newList = copyList(threatLine);
//				perf.get(5).incrementI();								// TEST - 5
//				perf.get(5).incrementD(System.nanoTime() - startTime);	// TEST - 5
				// if color of sequence is the same as the color of whoever just put down their stone
				if (board[threat.x][threat.y] == turn) {
					for (PI spaceScore : newList) {
						// multiply all scores by M
						spaceScore.setI(spaceScore.getI() * M);
					}
				} else {
					for (PI spaceScore : newList) {
						spaceScore.setI(spaceScore.getI() / M);
					}
				}
				updatedList.add(newList);
			}
			// copy threat
			Point newThreat = new Point(threat.x, threat.y);
			result.put(newThreat, updatedList);
		}
		// lookup the point and see which ones it affect
		Point latestPoint = new Point(x, y); // the point that is affecting
		if (lookup.containsKey(latestPoint)) {
			for (List<Point> threatSequence : lookup.get(latestPoint)) {
				for (Point threat : threatSequence) {
					if (board[threat.x][threat.y] != turn) { // same color
						// first, update the score of affected threat space
						int[] temp = threatSpaceFinder(threat, latestPoint);
						try {
							result.get(threat).get(temp[0]).get(temp[1]).setI(0);
						} catch (Exception e) {
							error++;
							System.out.println("Error in step. Threat: " + threat + ", access: " + temp[0] +
									", "+temp[1]+", latest point: ("+x+","+y+")");
						}
					} else {
						// TODO: opposing color - check other side for <5 then remove threat spaces accordingly
						boolean blocked = false;
						for (int i=0; i<8; i++) {
							for (int j=0; j<result.get(threat).get(i).size(); j++) {
								if (!blocked && result.get(threat).get(i).get(j).getP().equals(latestPoint)) {
									List<PI> newLine = new ObjectArrayList<>(result.get(threat).get(i).size());
									for (int k=0; k<j; k++) {
										newLine.add(result.get(threat).get(i).get(k));
									}
									if (j > 0) { // reduce score of the stone adjacent to the opposing piece by 1/4th
										newLine.set(j-1, new PI(newLine.get(j-1).getP(), newLine.get(j-1).getI() / M2));
									} else if (j == 0) { // reduce score of other side by 1/4
										int opposite = (i + 4) % 8;
										for (int k=0; k<result.get(threat).get(opposite).size(); k++) {
											result.get(threat).get(opposite).get(k).setI(result.get(threat)
												.get(opposite).get(k).getI() / M2);
										}
									}
									result.get(threat).set(i, newLine);
									blocked = true;
								}
							}
						}
					}
				}
			}
		}
		// this is a new threat 'sequence' containing only one point (goes 8-way)
		// insert the threat point - expand until board boundary or opposite color is reached. when same color, score 0
		int[] xFactor = {1, 1}, yFactor = {0, 1};
		List<List<PI>> threatLines = new ObjectArrayList<>(8);
		List<Integer> blocked = new IntArrayList();
		for (int i=0; i<4; i++) {
			for (int j=0; j<=1; j++) {
				List<PI> threatLine = new ObjectArrayList<>();
				boolean clash = false;
				for (int k=1; k<=5; k++) {
					if (!clash) {
						int xt = x + k * xFactor[j];
						int yt = y + k * yFactor[j];
						if (0<=xt && xt<19 && 0<=yt && yt<19 && board[xt][yt] != turn) {
							if (board[xt][yt] == 0) {
								if (k != 5) { // actual threat space
									threatLine.add(new PI(new Point(xt, yt), -M2 * turn)); // starting value
								} else { // 0-space
									threatLine.add(new PI(new Point(xt, yt), 0));
								}
							} else { // same color
								threatLine.add(new PI(new Point(xt, yt), 0));
							}
						} else {
							if (!threatLine.isEmpty()) { // out of bounds or differing color
								threatLine.get(k - 2).setI(threatLine.get(k - 2).getI() / M2);
							}
							if (k == 1) {
								blocked.add(2 * i + j);
							}
							clash = true;
						}
					}
				}
					/*if (!threatLine.isEmpty())*/ threatLines.add(threatLine);
			}
			// rotate 90° left
			int[] temp = Arrays.copyOf(xFactor, 2);
			for (int j=0; j<=1; j++) {
				xFactor[j] = 0 - yFactor[j];
				yFactor[j] = temp[j];
			}
		}
		result.put(latestPoint, threatLines);
		// adjusting score of threat spaces of opposite side of wherever an opposing piece is directly touching
		if (!blocked.isEmpty()) {
			for (int side : blocked) {
				int oppositeSide = (side + 4) % 8;
				for (int i=0; i<result.get(latestPoint).get(oppositeSide).size(); i++) {
					result.get(latestPoint).get(oppositeSide).get(i)
						.setI(result.get(latestPoint).get(oppositeSide).get(i).getI() / M2);
				}
			}
		}
		return result;
	}

	// keeps track of all the sequences for each threat space
	private Map<Point, List<List<Point>>> hash(Map<Point, List<List<Point>>> lookup, int x, int y, int[][] board,
											   int turn) {
//		double startTime = System.nanoTime();							// TEST - 4
		Map<Point, List<List<Point>>> result = copyLookup(lookup);
//		perf.get(4).incrementI();										// TEST - 4
//		perf.get(4).incrementD(System.nanoTime() - startTime);			// TEST - 4
		Point latestPoint = new Point(x, y);
		if (result.containsKey(latestPoint)) result.remove(latestPoint);
		int[] xFactor = {1, 1}, yFactor = {0, 1};
		for (int i=0; i<4; i++) {
			for (int j=0; j<2; j++) {
				boolean blocking = false;
				for (int k = 1; k<5; k++) {
					// check all threat spaces that could be affected by the latest point
					int xt = x + k * xFactor[j];
					int yt = y + k * yFactor[j];
					if (0<=xt && xt<19 && 0<=yt && yt<19) {
						if (board[xt][yt] == 0) {
							// a valid threat space
							Point threatSpace = new Point(xt, yt);
							boolean exists = false, inRangeExists = false, found = false, toAdd = false;
							if (result.containsKey(threatSpace)) {
								// modify existing sequence that has this threat space
								for (List<Point> seq : lookup.get(threatSpace)) {
									// this is a hack to allow concurrent modification of the list we're going through
									int index = result.get(threatSpace).indexOf(seq);
									if (index != -1) {
										exists = true;
										List<Point> sequence = result.get(threatSpace).get(index);
										// check if the sequence is valid
										if (inRange(latestPoint, sequence.get(0)) ||
											inRange(latestPoint, sequence.get(sequence.size() - 1))) {
											inRangeExists = true;
											// and check if the sequence, threat space, and the new threat all line up
											if (inLine(sequence.get(0), threatSpace, latestPoint)) {
												found = true;
												if (board[sequence.get(0).x][sequence.get(0).y] != turn &&
													isClear(sequence.get(sequence.size() - 1), latestPoint, turn,
														board)) {
													// adding to unobstructed sequence of the same color
													if (sequence.size() == 1) {
														if (closer(threatSpace, sequence.get(0), x, y)) {
															sequence.add(0, latestPoint);
														} else {
															sequence.add(latestPoint);
														}
													} else {
														boolean out = false;
														// see if the new point can be absorbed wholly into
														// the existing sequence without extending it > 5
														for (Point p : sequence) {
															if (!inRange(latestPoint, p)) {
																out = true;
															}
														}
														if (!out) {
															sequence.add(position(sequence, latestPoint,
																length(sequence)), latestPoint);
														} else {
															// if it can't be added wholly, split off the 
															// part of the sequence that can
															boolean contained = false;
															List<Point> temp = splitOff(latestPoint, sequence);
															temp.remove(latestPoint);
															for (List<Point> branch : result.get(threatSpace)) {
																// but before that, check whether the part that's split
																// off is contained wholly within another sequence
																if (!branch.equals(sequence) &&
																	board[branch.get(0).x][branch.get(0).y] != turn
																	&& inLine(branch.get(0), threatSpace, 
																	sequence.get(0))) {
																	boolean in = true;
																	for (Point p : temp) {
																		if (!branch.contains(p)) {
																			in = false;
																			break;
																		}
																	}
																	if (in) {
																		contained = true;
																		break;
																	}
																}
															}
															if (!contained) {
																List<Point> newBranch = splitOff(latestPoint, sequence);
																result.get(threatSpace).add(newBranch);
															}
														}
													}
												} else {
													// the sequence has different color from the new threat point, 
													// so trim the existing sequence as necessary
													List<Point> opposite = oppositeSide(latestPoint, threatSpace,
														sequence);
													if (!opposite.isEmpty()) {
														sequence.removeAll(opposite);
														boolean flag = false;
														if (!sequence.isEmpty()) {
															for (List<Point> branch : result.get(threatSpace)) {
																if (!branch.equals(sequence) &&
																	board[branch.get(0).x][branch.get(0).y] == turn
																	&& inLine(branch.get(0), threatSpace,
																	sequence.get(0))) {
																	// if a sequence is wholly contained in another
																	// branch, delete the unnecessary sequence
																	for (Point p : sequence) {
																		if (!branch.contains(p)) {
																			// 18 indentations. Whoo boy
																			flag = true;
																			break;
																		}
																	}
																	if (!flag) {
																		result.get(threatSpace).remove(sequence);
																		break;
																	}
																}
															}
														} else {
															sequence.add(latestPoint);
															// first, see if there is another branch of same color
															// on the other side that this branch can merge with
															for (List<Point> branch : result.get(threatSpace)) {
																if (board[branch.get(0).x][branch.get(0).y] != turn &&
																	inLine(branch.get(0), threatSpace, latestPoint) &&
																	!branch.get(0).equals(latestPoint)) {
																	// a special case for branches of size 2 since
																	// those could have the latest point in either
																	// at the beginning or at the end
																	if (branch.size() == 2 && 
																		branch.get(1).equals(latestPoint)) break;
																	for (Point branchPoint : branch) {
																		if (inRange(latestPoint, branchPoint)) {
																			sequence.add(branchPoint);
																		}
																	}
																	break;
																}
															}
														}
													} else if (!blocking) {
														// the newest point does not affect this sequence
														toAdd = true;
													}
												}
											}
										}
									}
								}
							}
							// threat space doesn't exist or none of the sequences affect latest point
							if (!blocking && (!inRangeExists || !found || toAdd)) {
								boolean alreadyIn = false;
								if (result.containsKey(threatSpace)) {
									for (List<Point> branch : result.get(threatSpace)) {
										if (branch.contains(latestPoint)) {
											alreadyIn = true;
											break;
										}
									}
								}
								if (!alreadyIn) {
									// add in new sequence
									List<Point> sequence = new ObjectArrayList<>(4);
									sequence.add(latestPoint);
									if (!exists) {
										List<List<Point>> sequenceList = new ObjectArrayList<>();
										sequenceList.add(sequence);
										result.put(threatSpace, sequenceList);
									} else {
										result.get(threatSpace).add(sequence);
									}
								}
							}
						} else if (board[xt][yt] == turn) {
							blocking = true;
						}
					}
				}
			}
			// rotate 90° left
			int[] temp = Arrays.copyOf(xFactor,2);
			for (int j=0; j<=1; j++) {
				xFactor[j] = 0 - yFactor[j];
				yFactor[j] = temp[j];
			}
		}
		return result;
	}

	// calculate scores on the board
	private IB calculateScores(Map<Point, List<List<Point>>> lookup, Map<Point, List<List<PI>>> threatSpaces,
									int[][] board, int turn, Map<Integer, String> newVisits) {
		int[][] result = new int[19][19];
		IB finalResult = new IB(new int[19][19], false);
		for (Point threatSpace : lookup.keySet()) {
			int black = 0, white = 0;
			// sequence-based scoring
			for (List<Point> threatSequence : lookup.get(threatSpace)) {
				int seqScore = 0, blackCount = 0, whiteCount = 0;
				// for points within a sequence, multiply them
				for (Point threat : threatSequence) {
					int[] temp = threatSpaceFinder(threat, threatSpace);
					try {
						// getting the score of each threat sequence
						if (board[threatSequence.get(0).x][threatSequence.get(0).y] == 1) {
							if (blackCount == 0) {
								seqScore = threatSpaces.get(threat).get(temp[0]).get(temp[1]).getI();
							} else {
								if (turn == 1) {
									seqScore *= (threatSpaces.get(threat).get(temp[0]).get(temp[1]).getI() / M);
								} else {
									seqScore *= threatSpaces.get(threat).get(temp[0]).get(temp[1]).getI();
								}
							}
							blackCount++;
						} else {
							if (whiteCount == 0) {
								seqScore = threatSpaces.get(threat).get(temp[0]).get(temp[1]).getI();
							} else {
								if (turn == 1) {
									seqScore *= (threatSpaces.get(threat).get(temp[0]).get(temp[1]).getI() * -1);
								} else {
									seqScore *= (threatSpaces.get(threat).get(temp[0]).get(temp[1]).getI() / -M);
								}
							}
							whiteCount++;
						}
					} catch (Exception e) {
						error++;
						if (DEBUG) {
							System.out.println("Error in calc. Threat: " + printPoint(threat) +
								", threatSpace: " + printPoint(threatSpace));
							String s = "";
							for (int i = 0; i < visited.size(); i++) s += visited.get(i);
							for (int i = 0; i < newVisits.size(); i++) s += newVisits.get(i);
							s += "|0|0";
							String path = "src/main/logs/error_log_" + error + "_" + System.currentTimeMillis() + "_" +
										   threat.x+"n"+threat.y+".txt";
							File file = new File(path);
							try {
								BufferedWriter output = new BufferedWriter(new FileWriter(file));
								output.write(s);
								output.close();
							} catch (IOException e2) {
								e2.printStackTrace();
							}
						}
					}
				}
				// then add up the scores of the sequences
				if (board[threatSequence.get(0).x][threatSequence.get(0).y] == 1) {
					if (blackCount == 4) {
						if (turn == 1) {
							// winning condition - tell minimax to return early
							finalResult.setBool(true);
							black = M * SUFFICIENTLY_LARGE_NUMBER;
						} else {
							// major threat that only needs one more move to finish!
							black = SUFFICIENTLY_LARGE_NUMBER;
						}
						// however, if it's blocked on either end, it takes 1 more move to finish, so decrease the score
						if (blocked(threatSequence, this.board)) black /= M2;
					} else {
						black += seqScore;
					}
				} else {
					if (whiteCount == 4) {
						if (turn == -1) {
							finalResult.setBool(true);
							white = -M * SUFFICIENTLY_LARGE_NUMBER;
						} else {
							white = -SUFFICIENTLY_LARGE_NUMBER;
						}
						if (blocked(threatSequence, this.board)) white /= M2;
					} else {
						white += seqScore;
					}
				}
			}
			if (black == 0) {
				result[threatSpace.x][threatSpace.y] = white;
			} else if (white == 0) {
				result[threatSpace.x][threatSpace.y] = black;
			} else {
				if (clashEvalMethod == 1) {
					// TODO: account for number of branches that are contributing to the score
					// should account for who's blocking what - score will be the way of determining that
					// if they turn out to be the same, turn will be the tie-breaker
					if (-white < black) {
						result[threatSpace.x][threatSpace.y] = black - (int) (DEFENSE_WEIGHT * white);
					} else if (-white > black) {
						result[threatSpace.x][threatSpace.y] = white - (int) (DEFENSE_WEIGHT * black);
					} else {
						// turn will be the tie-breaker
						if (turn == 1) {
							result[threatSpace.x][threatSpace.y] = black - (int) (DEFENSE_WEIGHT * white);
						} else {
							result[threatSpace.x][threatSpace.y] = white - (int) (DEFENSE_WEIGHT * black);
						}
					}
				} else if (clashEvalMethod == 2) {
					// original clash method
					if (turn == -1) {
						// white's turn
						result[threatSpace.x][threatSpace.y] = white - 1;
					} else {
						result[threatSpace.x][threatSpace.y] = black + 1;
					}
				} else {
					// should have very low weight for this one
					if (turn == -1) {
						result[threatSpace.x][threatSpace.y] = -white + (int) (DEFENSE_WEIGHT * black);
					} else {
						result[threatSpace.x][threatSpace.y] = -black + (int) (DEFENSE_WEIGHT * white);
					}
				}
			}
		}
		finalResult.setArray(result);
		return finalResult;
	}
	
	// returns the best move using alpha beta minimax pruning
	public Point winningMove() {
//		perf = new ObjectArrayList<>();								// TEST
//		for (int i = 0; i < 6; i++) perf.add(new DoubleInt());		// TEST
		error = nodes = 0;
		Point result = new Point(50, 50);
		List<Point> toVisit;
		// this is to allow for emergency exits
		while (!stop) {
			if (threatSpaces.size() != 1) {
				toVisit = filter(scores.getArray());
			} else {
				// TODO: opening book for at least the first 1~2 moves
				toVisit = new ObjectArrayList<>(BRANCH_LIMIT);
				Point first = new Point();
				for (Point p : threatSpaces.keySet()) first = p;
				toVisit = readBook(first, threatSpaces);
			}
			// mapping score to integer using streams and parallel
			Map<Point, Integer> finalScores = new Object2IntOpenHashMap<>();
			toVisit.stream()
				   .parallel()
				   .forEach(p -> finalScores.put(p, scoreOf(p)));
			if (error == 0) {
				if (turn == 1) {
					result = finalScores.entrySet().stream()
						.max(Comparator.comparingInt(Entry::getValue))
						.get().getKey();
				} else {
					result = finalScores.entrySet().stream()
						.min(Comparator.comparingInt(Entry::getValue))
						.get().getKey();
				}
			}
			System.out.println(numNodes() + " nodes searched.");
			if (!headless) player.callback(result);
			break;
		}
//		System.out.println("Time spent copying board: "+perf.get(0).getAvg());	// TEST
//		System.out.println("Time spent on step: "+perf.get(1).getAvg());		// TEST
//		System.out.println("Time spent on hash: "+perf.get(2).getAvg());		// TEST
//		System.out.println("Time spent on calc: "+perf.get(3).getAvg());		// TEST
//		System.out.println("Time spent copying lookup: "+perf.get(4).getAvg());	// TEST
//		System.out.println("Time spent copying list: "+perf.get(5).getAvg());	// TEST
		return result;
	}

	// the minimax depth-first search with alphabeta pruning
	// "node" is the combination of board, threats, lookup, scores, and turn
	private int alphaBeta(int[][] board, Map<Point, List<List<PI>>> threatSpaces, int alpha, int beta,
				  Map<Point, List<List<Point>>> lookup, IB scores, int depth, int turn, Map<Integer, String> visited) {
		nodes++;
		if (depth == DEPTH_LIMIT || scores.getBool()) {
			// end node - evaluate and return score
			int total = total(scores.getArray());
			//System.out.println("Returning "+total+" at depth "+depth);
			return total;
		}
		List<Point> toVisit = filter(scores.getArray());
		if (turn == 1) {
			int val = Integer.MIN_VALUE;
			// maximizing player - should prefer the totals that have higher positive value
			// visit all the places in order and do alpha beta pruning
			for (Point p : toVisit) {
//				double startTime = System.nanoTime();							// TEST - 0
				int[][] newBoard = addBoard(board, p.x, p.y, 1);
//				perf.get(0).incrementI();										// TEST - 0
//				perf.get(0).incrementD(System.nanoTime() - startTime);			// TEST - 0
				int newTurn = -1;
//				startTime = System.nanoTime();									// TEST - 1
				Map<Point, List<List<PI>>> nextThreats = step(p.x, p.y, threatSpaces, lookup, newTurn, newBoard);
//				perf.get(1).incrementI();										// TEST - 1
//				perf.get(1).incrementD(System.nanoTime() - startTime);			// TEST - 1
//				startTime = System.nanoTime();									// TEST - 2
				Map<Point, List<List<Point>>> nextLookup = hash(lookup, p.x, p.y, newBoard, newTurn);
//				perf.get(2).incrementI();										// TEST - 2
//				perf.get(2).incrementD(System.nanoTime() - startTime);			// TEST - 2
				Map<Integer, String> newVisits = null;
				if (DEBUG) {
					newVisits = copyVisits(visited);
					newVisits.put(depth, printPoint(p));
				}
//				startTime = System.nanoTime();									// TEST - 3
				IB nextScores = calculateScores(nextLookup, nextThreats, newBoard, newTurn, newVisits);
//				perf.get(3).incrementI();										// TEST - 3
//				perf.get(3).incrementD(System.nanoTime() - startTime);			// TEST - 3
				val = Math.max(val, alphaBeta(newBoard, nextThreats, alpha, beta,
					nextLookup, nextScores, depth + 1, newTurn, newVisits));
				alpha = Math.max(alpha, val);
				if (alpha >= beta) break;
			}
			return val;
		} else {
			int val = Integer.MAX_VALUE;
			// minimizing player
			for (Point p : toVisit) {
//				double startTime = System.nanoTime();							// TEST - 0
				int[][] newBoard = addBoard(board, p.x, p.y, turn);
//				perf.get(0).incrementI();										// TEST - 0
//				perf.get(0).incrementD(System.nanoTime() - startTime);			// TEST - 0
				int newTurn = -turn;
//				startTime = System.nanoTime();									// TEST - 1
				Map<Point, List<List<PI>>> nextThreats = step(p.x, p.y, threatSpaces, lookup, newTurn, newBoard);
//				perf.get(1).incrementI();										// TEST - 1
//				perf.get(1).incrementD(System.nanoTime() - startTime);			// TEST - 1
//				startTime = System.nanoTime();									// TEST - 2
				Map<Point, List<List<Point>>> nextLookup = hash(lookup, p.x, p.y, newBoard, newTurn);
//				perf.get(2).incrementI();										// TEST - 2
//				perf.get(2).incrementD(System.nanoTime() - startTime);			// TEST - 2
				Map<Integer, String> newVisits = null;
				if (DEBUG) {
					newVisits = copyVisits(visited);
					newVisits.put(depth, printPoint(p));
				}
//				startTime = System.nanoTime();									// TEST - 3
				IB nextScores = calculateScores(nextLookup, nextThreats, newBoard, newTurn, newVisits);
//				perf.get(3).incrementI();										// TEST - 3
//				perf.get(3).incrementD(System.nanoTime() - startTime);			// TEST - 3
				val = Math.min(val, alphaBeta(newBoard, nextThreats, alpha, beta,
					nextLookup, nextScores, depth + 1, newTurn, newVisits));
				beta = Math.min(beta, val);
				if (alpha >= beta) break;
			}
			return val;
		}
	}

	// returns the top points to visit
	private List<Point> filter(int[][] board) {
		List<Point> result = new ObjectArrayList<>(BRANCH_LIMIT);
		MyPQ pq = new MyPQ(BRANCH_LIMIT, DEBUG);
		for (int i=0; i<19; i++) {
			for (int j=0; j<19; j++) {
				if (board[i][j] != 0) {
					pq.push(board[i][j], new Point(i, j));
				}
			}
		}
		int largest = Math.abs(pq.peek());
		try {
			while (result.size() < BRANCH_LIMIT && Math.abs(pq.peek()) > (int) (largest * THRESHOLD)) {
				result.add(pq.pop());
			}
		} catch (Exception e) {
			error++;
			System.out.println("toVisit: " + result + ", pq: " + pq);
		}
		return result;
	}

	public void test() {
		System.out.println("<--------test-------->");
		System.out.println("Depth: " + DEPTH_LIMIT);
		System.out.println("threatSpaces: " + threatSpaces);
		System.out.println("lookup: " + lookup);
		System.out.println("number of threat spaces: " + lookup.keySet().size());
		//Point p = winningMove();
		//System.out.println("Best point is: ("+p.x+", "+p.y+")");
	}

	public int[][] getScores() {
		return scores.getArray();
	}

	// for use in testing
	public boolean won() {
		return scores.getBool();
	}

	// standardized way of getting scores for use in parallelization at depth 0
	private int scoreOf(Point p) {
		nodes++;
		int[][] newBoard = addBoard(board, p.x, p.y, turn);
		int newTurn = -turn;
		Map<Point, List<List<PI>>> nextThreats = step(p.x, p.y, threatSpaces, lookup, newTurn, newBoard);
		Map<Point, List<List<Point>>> nextLookup = hash(lookup, p.x, p.y, newBoard, newTurn);
		Map<Integer, String> newVisits = null;
		if (DEBUG) {
			newVisits = new HashMap<>();
			newVisits.put(0, printPoint(p));
		}
		IB nextScores = calculateScores(nextLookup, nextThreats, newBoard, newTurn, newVisits);
		int val = alphaBeta(newBoard, nextThreats, Integer.MIN_VALUE, Integer.MAX_VALUE, nextLookup,
			nextScores, 1, newTurn, newVisits);
		//System.out.println("Final score for ("+p.x+","+p.y+") is "+val);
		return val;
	}

	public int numNodes() {
		return nodes;
	}
}