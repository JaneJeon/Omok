import MyDataStructures.History;
import MyDataStructures.IB;
import MyDataStructures.MyPQ;
import MyDataStructures.PI;
import MyDataStructures.ReturningValues; // MAVEN CAN'T FIND CLASSES IF I DON'T IMPORT THEM INDIVIDUALLY
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.awt.Point;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

// WARNING: currently very unoptimized in performance, pruning, and the node choices!
public class Jack {
	private static final int SUFFICIENTLY_LARGE_NUMBER = 100_000_000;
	private static final int DEPTH_LIMIT = 9; // keep this odd!
	private static final int BRANCH_LIMIT = 5; // decrease to massively improve performance at cost of accuracy
	private static final double DEFENSE_WEIGHT = 0.92; // to encourage prioritizing offense over defense
	private static final double THRESHOLD = ((double)(2))/3; // need to cast to double first
	private static final int UNDO_LIMIT = 6;
	private int turn = 1, nodes; // turn: -1 for white, 1 for black. count is used for the first move only
	private int[][] board; // actual board for storing pieces
	private IB scores; // for storing board space scores
	private Map<Point, List<List<PI>>> threatSpaces; // threat -> threat space lines -> space & score
	private Map<Point, List<List<Point>>> lookup; // threat space (incl. 0) -> list of threat sequences
	private History history;
	private Map<Integer, Point> latestVisits;
	// TODO: fix bugs with pq out of range && with threat space/lookup error when CPU is white
	// TODO: fix double-docking issue in step
	// TODO: lazy parallelization - using the fact that there are at max 4 or 5 on the first level,
	// streamify the first level and parallelize it
	// TODO: physically force AI to ONLY consider defense moves when it detects an attack (override pq)
	// TODO: obvious optimization - remove "dead branches" in both threatspaces and lookup
	// TODO: optimization -  prioritize only the directly neighboring spaces when there's immediate threat
	// TODO: optimization - should make threat detection much less lengthy (don't go over entire lookup again)
	// TODO: optimization - just ignore scores & spaces that are insignificant, in both alternating and tallying up
	// TODO: slim down JAR size by only taking in what classes I need from dependencies
	// TODO: reduce object creation rate by monitoring memory heap
	// TODO: try replacing point objects with a single int (or long) that contains two numbers for performance reasons

	// constructor
	public Jack() {
		board = new int[19][19];
		threatSpaces = new Object2ObjectOpenHashMap<>();
		lookup = new Object2ObjectOpenHashMap<>();
		scores = new IB(new int[19][19], false);
		history = new History(UNDO_LIMIT);
	}

	// officially adds point, modifying the actual threatSpaces and lookup
	public void addPoint(int x, int y) {
		history.add(board, scores, threatSpaces, lookup);
		board[x][y] = turn;
		turn = -turn;
		// add point to lookup and threatSpaces
		threatSpaces = step(x, y, threatSpaces, lookup, turn, board);
		lookup = hash(lookup, x, y, board, turn);
		scores = calculateScores(lookup, threatSpaces, board, turn);
	}

	public void undo() {
		ReturningValues val = history.pop();
		System.out.println("MyDataStructures.History size: "+history.getSize());
		board = val.board;
		scores = val.scores;
		threatSpaces = val.threats;
		lookup = val.lookup;
		turn = -turn;
	}

	// modifies sequences, threat spaces, and scores given a new point
	private Map<Point, List<List<PI>>> step(int x, int y, Map<Point, List<List<PI>>> threatSpaces, Map<Point,
			List<List<Point>>> lookup, int turn, int[][] board) {
		Map<Point, List<List<PI>>> result = new Object2ObjectOpenHashMap<>();
		// first, alternate scores as ones that are affected and not affected both need to alternate scores
		for (Point threat : threatSpaces.keySet()) {
			List<List<PI>> updatedList = new ObjectArrayList<>();
			for (List<PI> threatLine : threatSpaces.get(threat)) {
				List<PI> newList = copyList(threatLine);
				// if color of sequence is the same as the color of whoever just put down their stone
				if (board[threat.x][threat.y] == turn) {
					for (PI spaceScore : newList) {
						// multiply all scores by 2
						spaceScore.setI(spaceScore.getI() * 2);
					}
				} else {
					for (PI spaceScore : newList) {
						spaceScore.setI(spaceScore.getI() / 2);
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
							System.out.println("Error in step. Threat: "+threat.toString()+", access: "+temp[0]+
									", "+temp[1]+", latest point: ("+x+","+y+")");
						}
					} else {
						// TODO: opposing color - check other side for <5 then remove threat spaces accordingly
						boolean blocked = false;
						for (int i=0; i<8; i++) {
							for (int j=0; j<result.get(threat).get(i).size(); j++) {
								if (!blocked && result.get(threat).get(i).get(j).getP().equals(latestPoint)) {
									List<PI> newLine = new ObjectArrayList<>();
									for (int k=0; k<j; k++) {
										newLine.add(result.get(threat).get(i).get(k));
									}
									if (j > 0) { // reduce score of the stone adjacent to the opposing piece by 1/4th
										newLine.set(j-1, new PI(newLine.get(j-1).getP(), newLine.get(j-1).getI() / 4));
									} else if (j == 0) { // reduce score of other side by 1/4
										int opposite = (i + 4) % 8;
										for (int k=0; k<result.get(threat).get(opposite).size(); k++) {
											result.get(threat).get(opposite).get(k).setI(result.get(threat)
													.get(opposite).get(k).getI() / 4);
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
		List<List<PI>> threatLines = new ObjectArrayList<>();
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
									threatLine.add(new PI(new Point(xt, yt), -4 * turn)); // starting value
								} else { // 0-space
									threatLine.add(new PI(new Point(xt, yt), 0));
								}
							} else { // same color
								threatLine.add(new PI(new Point(xt, yt), 0));
							}
						} else {
							if (!threatLine.isEmpty()) { // out of bounds or differing color
								threatLine.get(k - 2).setI(threatLine.get(k - 2).getI() / 4);
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
			int[] temp = Arrays.copyOf(xFactor,2);
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
							.setI(result.get(latestPoint).get(oppositeSide).get(i).getI() / 4);
				}
			}
		}
		return result;
	}

	private int[] threatSpaceFinder(Point threat, Point threatSpace) {
		int[] result = new int[2];
		if (threat.x > threatSpace.x) {
			// to the left
			if (threat.y > threatSpace.y) {
				// NW
				result[0] = 5;
			} else if (threat.y < threatSpace.y) {
				// SW
				result[0] = 3;
			} else {
				// left
				result[0] = 4;
			}
			result[1] = threat.x - threatSpace.x - 1;
		} else if (threat.x < threatSpace.x) {
			// to the right
			if (threat.y > threatSpace.y) {
				// NE
				result[0] = 7;
			} else if (threat.y < threatSpace.y) {
				// SE
				result[0] = 1;
			} else {
				// right
				result[0] = 0;
			}
			result[1] = threatSpace.x - threat.x - 1;
		} else {
			if (threat.y > threatSpace.y) {
				// up top
				result[0] = 6;
			} else {
				// down low
				result[0] = 2;
			}
			result[1] = Math.abs(threatSpace.y - threat.y) - 1;
		}
		return result;
	}

	// keeps track of all the sequences for each threat space
	private Map<Point, List<List<Point>>> hash(Map<Point, List<List<Point>>> lookup, int x, int y, int[][] board,
											   int turn) {
		Map<Point, List<List<Point>>> result = copyLookup(lookup);
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
													if (sequence.size() == 1) {
														if (closer(threatSpace, sequence.get(0), x, y)) {
															sequence.add(0, latestPoint);
														} else {
															sequence.add(latestPoint);
														}
													} else {
														boolean out = false;
														for (Point p : sequence) {
															if (!inRange(latestPoint, p)) {
																out = true;
															}
														}
														if (!out) {
															sequence.add(position(sequence, latestPoint,
																length(sequence)), latestPoint);
														} else {
															List<Point> temp = splitOff(latestPoint, sequence);
															result.get(threatSpace).add(temp);
														}
													}
												} else {
													// the sequence has different color from the new threat point, so
													// trim the existing sequence as necessary
													List<Point> opposite = oppositeSide(latestPoint, threatSpace,
														sequence);
													if (!opposite.isEmpty()) {
														for (Point p : opposite) {
															sequence.remove(p);
														}
														boolean flag = false;
														if (!sequence.isEmpty()) {
															for (List<Point> branch : result.get(threatSpace)) {
																if (!branch.equals(sequence) &&
																		board[branch.get(0).x][branch.get(0).y] == turn
																	&& inLine(branch.get(0), threatSpace,
																	sequence.get(0))) {
																	for (Point p : sequence) {
																		if (!branch.contains(p)) flag = true;
																	}
																	if (!flag) {
																		result.get(threatSpace).remove(sequence);
																		break;
																	}
																}
															}
														} else {
															sequence.add(latestPoint);
														}
													} else if (!blocking) {
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
									List<Point> sequence = new ObjectArrayList<>();
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

	// returns true if new point is closer than existing point to threat space
	private boolean closer(Point origin, Point existing, int x, int y) {
		return (origin.x - x) * (origin.x - x) + (origin.y - y) * (origin.y - y) <
				(origin.x - existing.x) * (origin.x - existing.x) + (origin.y - existing.y) * (origin.y - existing.y);
	}

	// if three points are in line, returns true
	private boolean inLine(Point A, Point B, Point C) {
		return A.x * (B.y - C.y) + B.x * (C.y - A.y) + C.x * (A.y - B.y) == 0;
	}

	// returns true if a point is within range of 4
	private boolean inRange(Point o, Point toCheck) {
		return toCheck.x >= o.x - 4 && toCheck.x <= o.x + 4 && toCheck.y >= o.y - 4 && toCheck.y <= o.y + 4;
	}

	// returns relative position of latest point in relation to existing sequence
	private int position(List<Point> sequence, Point latestPoint, int length) {
		// should now directly return the number which should be put in the sequence
		Point start = sequence.get(0), end = sequence.get(sequence.size() - 1);
		int xt = (end.x - start.x) / length, yt = (end.y - start.y) / length;
		if (xt != 0) {
			int absPosition = (latestPoint.x - start.x) / xt;
			if (absPosition < 0) {
				return 0;
			} else if (absPosition > length) {
				return sequence.size();
			} else {
				int relPosition = 1;
				while (absPosition > (sequence.get(relPosition).x - start.x) / xt) {
					relPosition++;
				}
				return relPosition;
			}
		} else {
			// only deal with yt
			int absPosition = (latestPoint.y - start.y) / yt;
			if (absPosition < 0) {
				return 0;
			} else if (absPosition > length) {
				return sequence.size();
			} else {
				int relPosition = 1;
				while (absPosition > (sequence.get(relPosition).y - start.y) / yt) {
					relPosition++;
				}
				return relPosition;
			}
		}
	}

	// checks if there are no opposing colors between the end point and the new point
	private boolean isClear(Point end, Point latestPoint, int turn, int[][] board) {
		if (end.x == latestPoint.x) {
			// vertical
			for (int i = 1; i < Math.abs(end.y - latestPoint.y); i++) {
				if (board[end.x][Math.min(end.y, latestPoint.y) + i] == turn) return false;
			}
		} else if (end.y == latestPoint.y) {
			// horizontal
			for (int i = 1; i < Math.abs(end.x - latestPoint.x); i++) {
				if (board[Math.min(end.x, latestPoint.x) + i][end.y] == turn) return false;
			}
		} else if ((end.y - latestPoint.y)/(end.x - latestPoint.x) == 1){
			// right up diagonal (slope = 1)
			for (int i = 1; i < Math.abs(end.x - latestPoint.x); i++) {
				if (board[Math.min(end.x, latestPoint.x) + i][Math.min(end.y, latestPoint.y) + i] == turn) return false;
			}
		} else {
			// right down diagonal (slope = -1)
			for (int i = 1; i < Math.abs(end.x - latestPoint.x); i++) {
				if (board[Math.min(end.x, latestPoint.x) + i][Math.max(end.y, latestPoint.y) - i] == turn) return false;
			}
		}
		return true;
	}

	// given a sequence in which at least one element is out of range and a point, splits off a new sequence
	private List<Point> splitOff(Point latestPoint, List<Point> sequence) {
		List<Point> result = new ObjectArrayList<>();
		result.add(latestPoint);
		Map<Integer, Point> distance = new Int2ObjectOpenHashMap<>();
		for (Point p : sequence) {
			distance.put((p.x - latestPoint.x) * (p.x - latestPoint.x) +
					(p.y - latestPoint.y) * (p.y - latestPoint.y), p);
		}
		for (int i=1; i<5; i++) {
			for (Integer d : distance.keySet()) {
				if (d == i * i || d == i * i * 2) {
					result.add(distance.get(d));
				}
			}
		}
		return result;
	}

	// lists all points in the sequence that are on the opposite side of threat space, with latest point as axis
	// also, can assume that the sequence and point are in line
	private List<Point> oppositeSide(Point latestPoint, Point threatSpace, List<Point> sequence) {
		List<Point> result = new ObjectArrayList<>();
		if (latestPoint.x == threatSpace.x) {
			// vertical
			if (latestPoint.y > threatSpace.y) {
				for (Point p : sequence) {
					if (p.y > latestPoint.y) result.add(p);
				}
			} else {
				for (Point p : sequence) {
					if (p.y < latestPoint.y) result.add(p);
				}
			}
		} else if (latestPoint.x > threatSpace.x) {
			for (Point p : sequence) {
				if (p.x > latestPoint.x) result.add(p);
			}
		} else {
			for (Point p : sequence) {
				if (p.x < latestPoint.x) result.add(p);
			}
		}
		return result;
	}

	// calculate scores on the board
	private IB calculateScores(Map<Point, List<List<Point>>> lookup, Map<Point, List<List<PI>>> threatSpaces,
									int[][] board, int turn) {
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
					try { // for bug fixing
						if (board[threatSequence.get(0).x][threatSequence.get(0).y] == 1) {
							if (blackCount == 0) {
								seqScore = threatSpaces.get(threat).get(temp[0]).get(temp[1]).getI();
							} else {
								if (turn == 1) {
									seqScore *= (threatSpaces.get(threat).get(temp[0]).get(temp[1]).getI() / 2);
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
									seqScore *= (threatSpaces.get(threat).get(temp[0]).get(temp[1]).getI() / -2);
								}
							}
							whiteCount++;
						}
					} catch (Exception e) {
						System.out.println("Error in calc. Threat: "+threat.toString()+
							", threatSpace: "+threatSpace.toString());
						List<PI> log = new ObjectArrayList<>();
						for (Point p : threatSpaces.keySet()) {
							if (!this.threatSpaces.containsKey(p)) log.add(new PI(p, 0));
						}
						for (PI pi : log) {
							pi.setI(board[pi.getP().x][pi.getP().y]);
						}
						System.out.println("Log: "+log.toString());
					}
				}
				// then add up the scores of the sequences
				if (board[threatSequence.get(0).x][threatSequence.get(0).y] == 1) {
					if (blackCount == 4) {
						if (turn == 1) {
							// winning condition - tell minimax to return early
							finalResult.setBool(true);
							black = 2 * SUFFICIENTLY_LARGE_NUMBER;
						} else {
							// major threat that only needs one more move to finish!
							black = SUFFICIENTLY_LARGE_NUMBER;
						}
					} else {
						black += seqScore;
					}
				} else {
					if (whiteCount == 4) {
						if (turn == -1) {
							finalResult.setBool(true);
							white = -2 * SUFFICIENTLY_LARGE_NUMBER;
						} else {
							white = -SUFFICIENTLY_LARGE_NUMBER;
						}
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
				// TODO: account for number of branches that are contributing to the score
				// should account for who's blocking what - score will be the way of determining that
				// if they turn out to be the same, turn will be the tie-breaker
				if (-white < black) {
					result[threatSpace.x][threatSpace.y] = black - (int)(DEFENSE_WEIGHT * white);
				} else if (-white > black) {
					result[threatSpace.x][threatSpace.y] = white - (int)(DEFENSE_WEIGHT * black);
				} else {
					// turn will be the tie-breaker
					if (turn == 1) {
						result[threatSpace.x][threatSpace.y] = black - (int)(DEFENSE_WEIGHT * white);
					} else {
						result[threatSpace.x][threatSpace.y] = white - (int)(DEFENSE_WEIGHT * black);
					}
				}
			}
		}
		finalResult.setArray(result);
		return finalResult;
	}

	// returns the best move using alpha beta minimax pruning
	public Point winningMove() {
		nodes = 0;
		latestVisits = new Int2ObjectOpenHashMap<>();
		Point result = new Point();
		int best;
		List<Point> toVisit;
		if (threatSpaces.size() != 1) {
			toVisit = filter(scores.getArray());
		} else {
			// TODO: opening book for at least the first 1~2 moves
			toVisit = new ObjectArrayList<>(BRANCH_LIMIT);
			Point first = new Point();
			for (Point p : threatSpaces.keySet()) {
				first = p;
			}
			if (first.x <= 9) {
				// left side of the board
				if (first.y <= 9) {
					// quadrant 2
					for (int i=0; i<2; i++) {
						// adding diagonals
						toVisit.add(threatSpaces.get(first).get(1).get(i).getP());
					}
					if (first.x > first.y) {
						for (int i=0; i<2; i++) {
							toVisit.add(threatSpaces.get(first).get(2).get(i).getP());
						}
					} else {
						for (int i=0; i<2; i++) {
							toVisit.add(threatSpaces.get(first).get(0).get(i).getP());
						}
					}
				} else {
					// quadrant 3
					for (int i=0; i<2; i++) {
						toVisit.add(threatSpaces.get(first).get(7).get(i).getP());
					}
					if (first.x > 19 - first.y) {
						for (int i=0; i<2; i++) {
							toVisit.add(threatSpaces.get(first).get(6).get(i).getP());
						}
					} else {
						for (int i=0; i<2; i++) {
							toVisit.add(threatSpaces.get(first).get(0).get(i).getP());
						}
					}
				}
			} else {
				// right side of the board
				if (first.y <= 9) {
					// quadrant 1
					for (int i=0; i<2; i++) {
						toVisit.add(threatSpaces.get(first).get(3).get(i).getP());
					}
					if (19 - first.x > first.y) {
						for (int i=0; i<2; i++) {
							toVisit.add(threatSpaces.get(first).get(2).get(i).getP());
						}
					} else {
						for (int i=0; i<2; i++) {
							toVisit.add(threatSpaces.get(first).get(4).get(i).getP());
						}
					}
				} else {
					// quadrant 4
					for (int i=0; i<2; i++) {
						toVisit.add(threatSpaces.get(first).get(5).get(i).getP());
					}
					if (first.x > first.y) {
						for (int i=0; i<2; i++) {
							toVisit.add(threatSpaces.get(first).get(4).get(i).getP());
						}
					} else {
						for (int i=0; i<2; i++) {
							toVisit.add(threatSpaces.get(first).get(6).get(i).getP());
						}
					}
				}
			}
		}
		if (turn == 1) {
			best = Integer.MIN_VALUE;
			for (Point p : toVisit) {
				nodes++;
				//latestVisits.put(0, p);
				//System.out.println(latestVisits.toString());
				int[][] newBoard = addBoard(board, p.x, p.y, turn);
				int newTurn = -turn;
				Map<Point, List<List<PI>>> nextThreats = step(p.x, p.y, threatSpaces, lookup, newTurn, newBoard);
				Map<Point, List<List<Point>>> nextLookup = hash(lookup, p.x, p.y, newBoard, newTurn);
				IB nextScores = calculateScores(nextLookup, nextThreats, newBoard, newTurn);
				int val = alphaBeta(newBoard, nextThreats, Integer.MIN_VALUE, Integer.MAX_VALUE, nextLookup,
						nextScores, 1, newTurn);
				//System.out.println("Final score for ("+p.x+","+p.y+") is "+val);
				if (val >= best) {
					best = val;
					result = p;
				}
			}
		} else {
			best = Integer.MAX_VALUE;
			for (Point p : toVisit) {
				nodes++;
				//latestVisits.put(0, p);
				//System.out.println(latestVisits.toString());
				int[][] newBoard = addBoard(board, p.x, p.y, turn);
				int newTurn = -turn;
				Map<Point, List<List<PI>>> nextThreats = step(p.x, p.y, threatSpaces, lookup, newTurn, newBoard);
				Map<Point, List<List<Point>>> nextLookup = hash(lookup, p.x, p.y, newBoard, newTurn);
				IB nextScores = calculateScores(nextLookup, nextThreats, newBoard, newTurn);
				int val = alphaBeta(newBoard, nextThreats, Integer.MIN_VALUE, Integer.MAX_VALUE, nextLookup,
						nextScores, 1, newTurn);
				//System.out.println("Final score for ("+p.x+","+p.y+") is "+val);
				if (val <= best) {
					best = val;
					result = p;
				}
			}
		}
		System.out.println("Searched "+nodes+" nodes");
		return result;
	}

	// the minimax depth-first search with alphabeta pruning
	// "node" is the combination of board, threats, lookup, scores, and turn
	private int alphaBeta(int[][] board, Map<Point, List<List<PI>>> threatSpaces, int alpha, int beta,
						  Map<Point, List<List<Point>>> lookup, IB scores, int depth, int turn) {
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
				//latestVisits.put(depth, p);
				//System.out.println(latestVisits.toString());
				int[][] newBoard = addBoard(board, p.x, p.y, turn);
				int newTurn = -turn;
				Map<Point, List<List<PI>>> nextThreats = step(p.x, p.y, threatSpaces, lookup, newTurn, newBoard);
				Map<Point, List<List<Point>>> nextLookup = hash(lookup, p.x, p.y, newBoard, newTurn);
				IB nextScores = calculateScores(nextLookup, nextThreats, newBoard, newTurn);
				val = Math.max(val, alphaBeta(newBoard, nextThreats, alpha, beta,
					nextLookup, nextScores, depth + 1, newTurn));
				alpha = Math.max(alpha, val);
				if (alpha >= beta) break;
			}
			return val;
		} else {
			int val = Integer.MAX_VALUE;
			// minimizing player
			for (Point p : toVisit) {
				//latestVisits.put(depth, p);
				//System.out.println(latestVisits.toString());
				int[][] newBoard = addBoard(board, p.x, p.y, turn);
				int newTurn = -turn;
				Map<Point, List<List<PI>>> nextThreats = step(p.x, p.y, threatSpaces, lookup, newTurn, newBoard);
				Map<Point, List<List<Point>>> nextLookup = hash(lookup, p.x, p.y, newBoard, newTurn);
				IB nextScores = calculateScores(nextLookup, nextThreats, newBoard, newTurn);
				val = Math.min(val, alphaBeta(newBoard, nextThreats, alpha, beta,
					nextLookup, nextScores, depth + 1, newTurn));
				beta = Math.min(beta, val);
				if (alpha >= beta) break;
			}
			return val;
		}
	}

	// deep copies a board while adding a point to it at the same time
	private int[][] addBoard(int[][] board, int x, int y, int turn) {
		int[][] result = new int[19][19];
		for (int i=0; i<19; i++) {
			for (int j=0; j<19; j++) {
				result[i][j] = board[i][j];
			}
		}
		result[x][y] = turn;
		return result;
	}

	// returns length of a threat sequence - not the number of pieces, but rather the physical space it takes up
	private int length(List<Point> threatSequence) {
		Point start = threatSequence.get(0), end = threatSequence.get(threatSequence.size() - 1);
		int len2 = (start.x - end.x) * (start.x - end.x) + (start.y - end.y) * (start.y - end.y);
		int len1 = (int)Math.sqrt(len2);
		if (len2 == len1 * len1) {
			return len1;
		} else {
			return (int)Math.sqrt(len2 / 2);
		}
	}

	// returns the top points to visit
	private List<Point> filter(int[][] board) {
		List<Point> result = new ObjectArrayList<>(BRANCH_LIMIT);
		MyPQ pq = new MyPQ(BRANCH_LIMIT);
		for (int i=0; i<19; i++) {
			for (int j=0; j<19; j++) {
				if (board[i][j] != 0) {
					pq.push(board[i][j], new Point(i, j));
				}
			}
		}
		int largest = Math.abs(pq.peek());
		try {
			while (result.size() < BRANCH_LIMIT && Math.abs(pq.peek()) > (int)(largest * THRESHOLD)) {
				result.add(pq.pop());
			}
		} catch (Exception e) {
			System.out.println("toVisit: "+result.toString()+", pq: "+pq.toString());
		}
		return result;
	}

	// eval function - simply add up all the scores on the board
	private int total(int[][] array) {
		int total = 0;
		for (int i=0; i<19; i++) {
			for (int j=0; j<19; j++) {
				if (array[i][j] != 0) total += array[i][j];
			}
		}
		return total;
	}

	public void test() {
		System.out.println("<--------test-------->");
		System.out.println("threatSpaces: "+ threatSpaces.toString());
		System.out.println("lookup: "+lookup.toString());
		System.out.println("number of threat spaces: "+lookup.keySet().size());
		Point p = winningMove();
		System.out.println("Best point is: ("+p.x+", "+p.y+")");
	}

	public int[][] getScores() {
		return scores.getArray();
	}

	// even though I can use DeepCopy.copy to copy the list, apparently this is miles faster...?
	private List<PI> copyList(List<PI> toCopy) {
		List<PI> result = new ObjectArrayList<>();
		for (int i=0; i<toCopy.size(); i++) {
			result.add(i, new PI(new Point(toCopy.get(i).getP().x, toCopy.get(i).getP().y), toCopy.get(i).getI()));
		}
		return result;
	}

	private Map<Point, List<List<Point>>> copyLookup(Map<Point, List<List<Point>>> original) {
		Map<Point, List<List<Point>>> result = new Object2ObjectOpenHashMap<>();
		for (Point threatSpace : original.keySet()) {
			List<List<Point>> branchCopy = new ObjectArrayList<>();
			for (List<Point> threats : original.get(threatSpace)) {
				List<Point> threatsCopy = new ObjectArrayList<>();
				for (Point threat : threats) {
					int x = threat.x;
					int y = threat.y;
					threatsCopy.add(new Point(x, y));
				}
				branchCopy.add(threatsCopy);
			}
			int xp = threatSpace.x;
			int yp = threatSpace.y;
			result.put(new Point(xp, yp), branchCopy);
		}
		return result;
	}
}
