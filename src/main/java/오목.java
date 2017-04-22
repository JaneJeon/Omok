import sun.audio.AudioPlayer;
import sun.audio.AudioStream;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;

public class 오목 extends JFrame {
	private static final int offset = 20; // how much space between end of board and boundary
	private static final int square = 40; // size of square
	private static final int pieceSize = 15; // radius of pieces
	private static final int fontSize = 20;
	private static final double lastPieceScale = 1.12;
	private static final String filePath = "background.png";
	private static final String audioPath1 = "sf1.aiff", audioPath2 = "sf2.aiff", audioPath3 = "sfx3.aiff",
		audioPath4 = "sfx4.aiff";
	private static String serverIP;
	static {
		try {
			// serverConfig.txt, placed in resources folder, should only contain the IP address as a string
			serverIP = new Scanner(오목.class.getClassLoader().getResourceAsStream("serverConfig.txt")).next();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private static final boolean TEST = true, ENGLISH = true;
	private Point click3, created;
	private List<Point> pieces;
	private List<Set<Point>> set34;
	private int mouseX, mouseY, show;
	private int bUndo = 0, wUndo = 0, startState = 1, undoErrorCount = 0, difficulty;
	private String font = "Lucina Grande";
	private boolean ifWon = false, showNum = false, calculating = false, AIMode = false, online = false,
		connecting = false, sound = true, AIblack = false;
	private BufferedImage image;
	private Jack AI;
	private ClientCommunicator comm;
	private String whiteWinString = ENGLISH ? "White wins!" : "백 승리!";
	private String blackWinString = ENGLISH ? "Black wins!" : "흑 승리!";
	private String endString = ENGLISH ? "Game over" : "게임 종료";
	private String errorString = ENGLISH ? "Error" : "에러";
	public int[] testParamsInt = {2, 1, 5, 13};
	public double[] testParamsDouble = {1, 2/3.5};
	// TODO: handle exception when cannot connect to server in a way that doesn't crash the game
	// TODO: timer dropdown, autosave when game is done
	// TODO: update to Javadoc style, experiment with loading partially completed games' interaction with Jack
	// TODO: splash screen to let her know that game is loading
	// TODO: loading bar or icon for when calculating
	// TODO: play error sound when online and playing in the wrong turn

	// constructor
	public 오목() {
		super("오목");
		// load in background here and not at paintComponent to greatly boost FPS
		if (!TEST) {
			try {
				image = ImageIO.read(getClass().getClassLoader().getResourceAsStream(filePath));
			} catch (IOException e) {
				System.err.println("이미지가 " + filePath + "' 에 존재하지 않습니다");
				System.exit(-1);
			}
		}
		// Helpers to create the canvas and GUI (buttons, etc.)
		JComponent canvas = setupCanvas();
		JComponent gui = setupGUI();
		JMenuBar menu = setUpMenu();
		// Put the buttons and canvas together into the window
		Container cp = getContentPane();
		cp.setLayout(new BorderLayout());
		cp.add(canvas, BorderLayout.CENTER);
		cp.add(gui, BorderLayout.NORTH);
		this.setJMenuBar(menu);
		// Usual initialization
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		pack();
		this.setVisible(true);
		// initialize game
		pieces = new ArrayList<>();
		difficulty = 1;
		AI = newAI(2);
		System.out.println("Server IP: "+serverIP);
	}

	private JComponent setupCanvas() {
		JComponent canvas = new JComponent() {
			public void paintComponent(Graphics g) {
				super.paintComponent(g);
				// set background image to scale with window size
				if (!connecting) {
					if (!TEST) {
						g.drawImage(image, 0, 0, offset * 2 + square * 18, offset * 2 + square * 18, null);
						for (int i = 0; i < 19; i++) { // draw base grid - horizontal, then vertical lines
							g.setColor(Color.black);
							g.drawLine(offset, offset + i * square, offset + 18 * square, offset + i * square);
							g.drawLine(offset + i * square, offset, offset + i * square, offset + 18 * square);
						}
						for (int x = 0; x < 3; x++) { // draw guiding dots
							for (int y = 0; y < 3; y++) {
								// dot size is fixed at 3
								g.fillOval(offset + square * (6 * x + 3) - 3, offset + square * (6 * y + 3) - 3, 6, 6);
							}
						}
					}
					drawPieces(g);
					drawOverlay(g);
				} else {
					// splash screen for when you're connected to the server and waiting for an opponent
					FontMetrics metrics = g.getFontMetrics(new Font(font, Font.PLAIN, fontSize * 3));
					g.setFont(new Font(font, Font.PLAIN, fontSize * 3));
					String connecting = ENGLISH ? "Connecting..." : "연결중...";
					g.drawString(connecting, offset + square * 9 - (metrics.stringWidth(connecting) / 2),
							offset + square * 9 - (metrics.getHeight() / 2) + metrics.getAscent());
				}
			}
		};
		canvas.setPreferredSize(new Dimension(offset * 2 + square * 18, offset * 2 + square * 18));
		canvas.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (inRange(e.getPoint())) play(e.getPoint());
			}
		});
		canvas.addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseMoved(MouseEvent e) {
				if (inRange(e.getPoint())) {
					mouseX = e.getPoint().x;
					mouseY = e.getPoint().y;
					repaint();
				}
			}
		});
		return canvas;
	}

	private JComponent setupGUI() {
		// setting up the row of buttons beneath the menu bar
		String undoString = ENGLISH ? "Undo" : "한수 무르기";
		JButton undo = new JButton(undoString);
		undo.addActionListener(e -> {
			undo();
			if (AIMode) undo();
		});
		JButton clear;
		if (!ENGLISH) {
			clear = new JButton("재시작");
		} else {
			clear = new JButton("Restart");
		}
		clear.addActionListener(e -> clear());
		String[] states = new String[4];
		if (!ENGLISH) {
			states[0] = "로컬 2인용"; states[1] = "온라인 2인용"; states[2] = "컴퓨터 - 백"; states[3] = "컴퓨터 - 흑";
		} else {
			states[0] = "Local 2P"; states[1] = "Online 2P"; 
			states[2] = "CPU - White"; states[3] = "CPU - Black";
		}
		JComboBox<String> stateB = new JComboBox<>(states);
		stateB.addActionListener(e -> {
			if (((JComboBox<String>) e.getSource()).getSelectedItem() == "로컬 2인용" ||
				((JComboBox<String>) e.getSource()).getSelectedItem() == "Local 2P") {
				startState = 1;
			} else if (((JComboBox<String>) e.getSource()).getSelectedItem() == "컴퓨터 - 백" ||
				       ((JComboBox<String>) e.getSource()).getSelectedItem() == "CPU - White") {
				startState = 2;
			} else if (((JComboBox<String>) e.getSource()).getSelectedItem() == "컴퓨터 - 흑" ||
				       ((JComboBox<String>) e.getSource()).getSelectedItem() == "CPU - Black") {
				startState = 3;
			} else {
				startState = 4;
			}
			if (show == 0) clear();
			System.out.println("Start state changed to: " + startState);
		});
		JButton first = new JButton("<<");
		first.addActionListener(e -> {
			if (pieces.size() > 0) {
				show = 1;
				repaint();
			}
		});
		JButton prev = new JButton("<");
		prev.addActionListener(e -> {
			if (show > 1) {
				show--;
				repaint();
			}
		});
		JButton next = new JButton(">");
		next.addActionListener(e -> {
			if (show < pieces.size()) {
				show++;
				repaint();
			}
		});
		JButton last = new JButton(">>");
		last.addActionListener(e -> {
			show = pieces.size();
			repaint();
		});
		String soundString = ENGLISH ? "Sound" : "소리";
		JButton toggleSound = new JButton(soundString);
		toggleSound.addActionListener(e -> sound = !sound);
		JComponent gui = new JPanel();
		gui.add(stateB);
		gui.add(undo);
		gui.add(clear);
		gui.add(first);
		gui.add(prev);
		gui.add(next);
		gui.add(last);
		gui.add(toggleSound);
		// button only for test mode
		if (TEST) {
			JButton test = new JButton("test");
			test.addActionListener(e -> {
				System.out.println("Show = " + show);
				System.out.println("Pieces: " + pieces.toString());
				AI.test();
			});
			gui.add(test);
		}
		return gui;
	}

	private JMenuBar setUpMenu() {
		JMenuBar menuBar = new JMenuBar();
		String fileString = ENGLISH ? "File" : "파일";
		JMenu fileMenu = new JMenu(fileString);
		String openString = ENGLISH ? "Open" : "열기";
		JMenuItem openMi = new JMenuItem(openString);
		openMi.addActionListener((ActionEvent e) -> {
			if (startState != 4) load();
		});
		String saveString = ENGLISH ? "Save" : "저장";
		JMenuItem saveMi = new JMenuItem(saveString);
		saveMi.addActionListener((ActionEvent e) -> save());
		fileMenu.add(openMi);
		fileMenu.add(saveMi);
		String numString = ENGLISH ? "Moves" : "번호";
		JMenu numMenu = new JMenu(numString);
		String showString = ENGLISH ? "Show" : "보이기";
		JCheckBoxMenuItem showMi = new JCheckBoxMenuItem(showString);
		showMi.setMnemonic(KeyEvent.VK_S);
		showMi.addItemListener((ItemEvent e) -> {
			showNum = !showNum;
			repaint();
		});
		String fontString = ENGLISH ? "Font" : "글꼴";
		JMenu fontMenu = new JMenu(fontString);
		fontMenu.setMnemonic(KeyEvent.VK_F);
		ButtonGroup fontGroup = new ButtonGroup();
		JRadioButtonMenuItem font1RMi = new JRadioButtonMenuItem("Arial");
		fontMenu.add(font1RMi);
		font1RMi.addItemListener((ItemEvent e) -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				font = "Arial";
				repaint();
			}
		});
		JRadioButtonMenuItem font2RMi = new JRadioButtonMenuItem("Courier");
		fontMenu.add(font2RMi);
		font2RMi.addItemListener((ItemEvent e) -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				font = "Courier";
				repaint();
			}
		});
		JRadioButtonMenuItem font3RMi = new JRadioButtonMenuItem("Helvetica Neue");
		fontMenu.add(font3RMi);
		font3RMi.addItemListener((ItemEvent e) -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				font = "Helvetica Neue";
				repaint();
			}
		});
		JRadioButtonMenuItem font4RMi = new JRadioButtonMenuItem("Lucina Grande");
		font4RMi.setSelected(true);
		fontMenu.add(font4RMi);
		font4RMi.addItemListener((ItemEvent e) -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				font = "Lucina Grande";
				repaint();
			}
		});
		fontGroup.add(font1RMi);
		fontGroup.add(font2RMi);
		fontGroup.add(font3RMi);
		fontGroup.add(font4RMi);
		numMenu.add(showMi);
		numMenu.add(fontMenu);
		String difficultyString = ENGLISH ? "Difficulty" : "난이도";
		JMenu difficultyMenu = new JMenu(difficultyString);
		fontMenu.setMnemonic(KeyEvent.VK_I);
		ButtonGroup difficultyGroup = new ButtonGroup();
		String highString = ENGLISH ? "Hard" : "상";
		JRadioButtonMenuItem difficulty1RMi = new JRadioButtonMenuItem(highString);
		difficultyMenu.add(difficulty1RMi);
		difficulty1RMi.addItemListener((ItemEvent e) -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				difficulty = 3;
				System.out.println("Setting difficulty to hard");
			}
		});
		String medString = ENGLISH ? "Medium" : "중";
		JRadioButtonMenuItem difficulty2RMi = new JRadioButtonMenuItem(medString);
		difficultyMenu.add(difficulty2RMi);
		difficulty2RMi.addItemListener((ItemEvent e) -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				difficulty = 2;
				System.out.println("Setting difficulty to medium");
			}
		});
		String lowString = ENGLISH ? "Easy" : "하";
		JRadioButtonMenuItem difficulty3RMi = new JRadioButtonMenuItem(lowString);
		difficulty3RMi.setSelected(true);
		difficultyMenu.add(difficulty3RMi);
		difficulty3RMi.addItemListener((ItemEvent e) -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				difficulty = 1;
				System.out.println("Setting difficulty to easy");
			}
		});
		if (TEST) {
			JRadioButtonMenuItem difficultyXRMi = new JRadioButtonMenuItem("Test");
			difficultyMenu.add(difficultyXRMi);
			difficultyXRMi.addItemListener((ItemEvent e) -> {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					difficulty = 42;
					String s = JOptionPane.showInputDialog(
						오목.getFrames()[0], // I have no idea why this works
						"Def weight, score threshold, multiplier(2), eval(1), branch, depth", "Value input",
						JOptionPane.PLAIN_MESSAGE);
					System.out.println(s);
					String[] param = s.split(" ");
					testParamsDouble[0] = Double.parseDouble(param[0]);
					testParamsDouble[1] = Double.parseDouble(param[1]);
					testParamsInt[0] = Integer.parseInt(param[2]);
					testParamsInt[1] = Integer.parseInt(param[3]);
					testParamsInt[2] = Integer.parseInt(param[4]);
					testParamsInt[3] = Integer.parseInt(param[5]);
				}
			});
		}
		difficultyGroup.add(difficulty1RMi);
		difficultyGroup.add(difficulty2RMi);
		difficultyGroup.add(difficulty3RMi);
		String helpString = ENGLISH ? "About" : "설명 도움이";
		JMenu explain = new JMenu(helpString);
		String message = ENGLISH ? "* Use the File menu to open or save games\n" +
			"* Use the 'Moves' menu to display the order of moves as numbers, and modify the font\n" +
			"* You can select the game mode - make sure to press 'restart' after changing modes\n" +
			"* In addition, you can change the difficulty of the AI when playing single-player\n" +
			"-- again, make sure to press restart to apply the changes\n" +
			"* In any game mode, each color can only undo 3 moves. After that, you won't be able to undo\n" +
			"* You can click the arrows to browse through the moves - it won't affect the game state\n" +
			"* During online multiplayer, you can only undo your own color\n" +
			"* Press the sound button to turn sfx on/off\n" +
			"Program written by Sungil Ahn" :
			"1. 파일 메뉴를 눌러 게임을 저장하거나 열기\n2. 번호 메뉴를 눌러 수 보이기\n3. 번호 메뉴 안에 글꼴 바꾸기\n" +
			"4. 메뉴 밑에 오목 모드를 설정하기\n5. 모드나 난이도를 설정한후, 그 모드/난이도로 시작하려면 재시작을 누루기\n6. " +
			"흑/백 판 마다 최대 3번만 무를수 있음\n7. 온라인 2인용 일떼는 자기의 색깔만 되돌맀수 있음\n8. " +
			"언제든지 화살표들을 클릭헤서 앞으로나 뒤로 수를 보기\n9. << 는 제일 처음으로, < 는 지난 수로, > 는 다음 수로, "+
			"그리고 >> 은 현제/제일 마지막 수로\n10. 소리 버튼으로 sfx 크기/끄기\n\n 저자 - 안성일";
		String aboutBar = ENGLISH ? "Manual" : "사용설명서";
 		explain.addMenuListener(new MenuListener() {
			public void menuSelected(MenuEvent e) {
				JOptionPane.showMessageDialog(오목.this, message, aboutBar, JOptionPane.PLAIN_MESSAGE);
			}

			public void menuDeselected(MenuEvent e) {
			}

			public void menuCanceled(MenuEvent e) {
			}
		});
		menuBar.add(fileMenu);
		menuBar.add(numMenu);
		menuBar.add(difficultyMenu);
		menuBar.add(Box.createHorizontalGlue());
		menuBar.add(explain);
		return menuBar;
	}

	private void drawPieces(Graphics g) {
		// used to center the move numbers on each piece
		FontMetrics metrics = g.getFontMetrics(new Font(font, Font.PLAIN, fontSize));
		FontMetrics metrics2 = g.getFontMetrics(new Font(font, Font.PLAIN, fontSize - 4));
		// only allow user to look back to the very first move
		if (show >= 1) {
			if ((show-1)%2 == 0) {
				g.setColor(Color.black);
			} else {
				g.setColor(Color.white);
			}
			// drawing the big piece that indicates the last piece
			if (!ifWon && show == pieces.size()) {
				g.fillOval(offset + square * pieces.get(show-1).x - (int)(pieceSize * lastPieceScale), offset + square
						* pieces.get(show-1).y - (int)(pieceSize * lastPieceScale), (int)(pieceSize * 2 *
					lastPieceScale), (int)(pieceSize * 2 * lastPieceScale));
			} else {
				g.fillOval(offset + square * pieces.get(show-1).x - pieceSize, offset + square * pieces.get(show-1).y -
					pieceSize, pieceSize * 2, pieceSize * 2);
			}
			if ((show-1)%2 == 0) {
				g.setColor(Color.white);
			} else {
				g.setColor(Color.black);
			}
			if (showNum) {
				g.setFont(new Font(font, Font.PLAIN, fontSize));
				g.drawString(Integer.toString(show), offset + square * pieces.get(show-1).x
					- (metrics.stringWidth(Integer.toString(show))) / 2, offset + square * pieces.get(show-1).y
					- (metrics.getHeight()) / 2 + metrics.getAscent());
			}
		}
		// drawing the regular pieces
		for (int i = 0; i < show-1; i++) {
			if (i % 2 == 0) { // black's pieces
				g.setColor(Color.black);
				g.fillOval(offset + square * pieces.get(i).x - pieceSize, offset + square * pieces.get(i).y - pieceSize,
						pieceSize * 2, pieceSize * 2);
				if (showNum) {
					g.setColor(Color.white);
				}
			} else {
				g.setColor(Color.white);
				g.fillOval(offset + square * pieces.get(i).x - pieceSize, offset + square * pieces.get(i).y - pieceSize,
						pieceSize * 2, pieceSize * 2);
				if (showNum) {
					g.setColor(Color.black);
				}
			}
			if (showNum) { // drawing numbers
				if (i < 99) {
					g.setFont(new Font(font, Font.PLAIN, fontSize));
					g.drawString(Integer.toString(i + 1), offset + square * pieces.get(i).x
							- (metrics.stringWidth(Integer.toString(i + 1))) / 2, offset + square * pieces.get(i).y
							- (metrics.getHeight()) / 2 + metrics.getAscent());
				} else {
					g.setFont(new Font(font, Font.PLAIN, fontSize - 4)); // 3-digits get decreased font size
					g.drawString(Integer.toString(i + 1), offset + square * pieces.get(i).x
							- (metrics2.stringWidth(Integer.toString(i + 1))) / 2, offset + square * pieces.get(i).y
							- (metrics2.getHeight()) / 2 + metrics2.getAscent());
				}
			}
		}
		// colored scores to see relative scores of every potential threat space
		if (TEST) {
			g.setFont(new Font(font, Font.PLAIN, fontSize - 4));
			int[][] scores = AI.getScores();
			for (int i = 0; i < 19; i++) {
				for (int j = 0; j < 19; j++) {
					if (scores[i][j] > 0) {
						g.setColor(Color.blue);
					} else if (scores[i][j] < 0) {
						g.setColor(Color.red);
					} else {
						g.setColor(Color.gray);
					}
					g.drawString(Integer.toString(scores[i][j]), offset + square * i
							- (metrics2.stringWidth(Integer.toString(scores[i][j]))) / 2, offset + square * j
							- (metrics2.getHeight()) / 2 + metrics2.getAscent());
				}
			}
		}
	}

	private void drawOverlay(Graphics g) {
		if (!calculating) {
			if (!ifWon) { // disable mouse overlays if the game is over
				int px = Math.round((mouseX - offset + square / 2) / square);
				int py = Math.round((mouseY - offset + square / 2) / square);
				if (created == null) {
					if (click3 != null) {
						if ((click3.x - px) * (click3.x - px) + (click3.y - py) * (click3.y - py) >= 1) {
							click3 = null;
							return;
						}
						// if someone clicks on the illegal space, then warn the user with the red color
						g.setColor(new Color(220, 83, 74));
						g.fillOval(offset + square * px - pieceSize, offset + square * py - pieceSize,
								pieceSize * 2, pieceSize * 2);
						return;
					}
					// red color for when the user hovers over an existing piece
					for (int i=0; i<pieces.size(); i++) {
						Point p = pieces.get(i);
						if ((p.x - px) * (p.x - px) + (p.y - py) * (p.y - py) < 1) {
							g.setColor(new Color(220, 83, 74));
							if (i != pieces.size() - 1) {
								g.fillOval(offset + square * px - pieceSize, offset + square * py - pieceSize,
									pieceSize * 2, pieceSize * 2);
							} else {
								g.fillOval(offset + square * px - (int)(pieceSize * lastPieceScale), offset + square
									* py - (int)(pieceSize * lastPieceScale), (int)(pieceSize * 2 * lastPieceScale),
									(int)(pieceSize * 2 * lastPieceScale));
							}
							return;
						}
					}
					// restore color when the mouse moves out of prohibited spaces
					if (pieces.size() % 2 == 0) {
						g.setColor(new Color(0, 0, 0, 127));
						g.fillOval(offset + square * px - pieceSize, offset + square * py - pieceSize,
								pieceSize * 2, pieceSize * 2);
					} else {
						g.setColor(new Color(255, 255, 255, 127));
						g.fillOval(offset + square * px - pieceSize, offset + square * py - pieceSize,
								pieceSize * 2, pieceSize * 2);
					}
					return;
				}
				if ((created.x - px) * (created.x - px) + (created.y - py) * (created.y - py) >= 1) {
					created = null;
				}
			}
		}
	}

	private void play(Point p) {
		if (!ifWon) {
			int px = Math.round((p.x - offset + square / 2) / square);
			int py = Math.round((p.y - offset + square / 2) / square);
			Point pt = new Point(px, py);
			if (!pieces.contains(pt)) {
				List<Point> piecesCopy = new ArrayList<>(pieces);
				piecesCopy.add(pt);
				set34 = open3(piecesCopy);
				if (AIMode || legalMove(pt)) { // legitimate move by the user
					if (TEST || AIMode) AI.addPoint(px, py);
					if (online) {
						comm.send("add " + px + " " + py);
					} else {
						pieces.add(pt);
					}
					show = pieces.size();
					playSound(show);
					created = pt;
					if (!online) { // when offline, check for win
						if (won()) {
							ifWon = true;
							playSound(-5);
							if (pieces.size() % 2 == 0) {
								JOptionPane.showMessageDialog(오목.this, whiteWinString,
										endString, JOptionPane.INFORMATION_MESSAGE);
							} else {
								JOptionPane.showMessageDialog(오목.this, blackWinString,
										endString, JOptionPane.INFORMATION_MESSAGE);
							}
							repaint();
						} else {
							repaint(); // update the board
							if (AIMode) { // let AI make the move
								calculating = true;
								double startTime = System.nanoTime();
								Point tmp = AI.winningMove();
								pieces.add(tmp);
								set34 = open3(pieces);
								AI.addPoint(tmp.x, tmp.y);
								double endTime = System.nanoTime();
								double duration = (endTime - startTime) / 1000000;
								System.out.println("It took " + duration + " ms to calculate the best move");
								calculating = false;
								show = pieces.size();
								playSound(show);
								if (won()) {
									ifWon = true;
									playSound(-5);
									String cpuString = (ENGLISH) ? "CPU wins!" : "컴퓨터 승리";
									JOptionPane.showMessageDialog(오목.this, cpuString, endString,
											JOptionPane.INFORMATION_MESSAGE);
								}
								repaint();
							}
						}
					}
				} else {
					// illegal move!
					click3 = pt;
					pieces.remove(click3);
					repaint();
					playSound(-1);
					String illegalString = (ENGLISH) ? "Illegal 3x3 move" : "삼삼!";
					JOptionPane.showMessageDialog(오목.this, illegalString, errorString, JOptionPane.ERROR_MESSAGE);
				}
			}
		}
	}

	// checks for conditions to undo (# of undo's, turn, and number of pieces on the board)
	private void undo() {
		if ((!ifWon || TEST) && pieces.size() > 0) {
			String backString = (ENGLISH) ? "Can't undo" : "수 되돌리기 불가능!";
			if ((pieces.size() % 2 == 1 && bUndo < 3) || (pieces.size() %2 == 0 && wUndo < 3)) {
				if (!online) {
					if (!AIMode) {
						pieces.remove(pieces.size() - 1);
						if (pieces.size() % 2 == 1) {
							wUndo++;
							System.out.println("wUndo: " + wUndo);
						} else {
							bUndo++;
							System.out.println("bUndo: " + bUndo);
						}
						if (TEST) {
							AI.undo();
							System.out.println("AI undo");
						}
					} else {
						if (!AIblack || pieces.size() > 1) {
							pieces.remove(pieces.size() - 1);
							if (pieces.size() % 2 == 1) {
								wUndo++;
							} else {
								bUndo++;
							}
							AI.undo();
							System.out.println("AI undo");
						} else {
							if (undoErrorCount % 2 == 0) {
								playSound(-1);
								JOptionPane.showMessageDialog(오목.this, backString, errorString,
									JOptionPane.ERROR_MESSAGE);
							}
							undoErrorCount++;
						}
					}
				} else {
					comm.send("undo");
				}
				set34 = open3(pieces);
				show = pieces.size();
				playSound(show);
			} else {
				if (!AIMode || undoErrorCount % 2 == 0) {
					playSound(-1);
					JOptionPane.showMessageDialog(오목.this, backString, errorString, JOptionPane.ERROR_MESSAGE);
				}
				undoErrorCount++;
			}
			repaint();
		}
	}

	// resets board, AI, and all the associated instance variables and starts a new game
	private void clear() {
		pieces = new ArrayList<>();
		set34 = new ArrayList<>();
		bUndo = wUndo = 0;
		show = 0;
		connecting = online = ifWon = calculating = false;
		created = null;
		click3 = null;
		AI = newAI(difficulty);
		undoErrorCount = 0;
		if (startState == 1) { // 2P
			AIMode = false;
		} else if (startState == 2) { // COM WHITE
			AIMode = true;
			AIblack = false;
		} else if (startState == 3) { // COM BLACK
			AIMode = true;
			AIblack = true;
			pieces.add(new Point(9, 9));
			show++;
			AI.addPoint(9, 9);
			playSound(1);
		} else { // Online multi-player
			try {
				comm = new ClientCommunicator(serverIP, this);
				comm.setDaemon(true);
				comm.start();
				online = true;
				setConnecting(true);
			} catch (Exception e) {
				playSound(-1);
				JOptionPane.showMessageDialog(오목.this, "서버에 연결 불가능", "에러", JOptionPane.ERROR_MESSAGE);
			}
		}
		repaint();
	}

	// pops up save dialog, automatically add .txt extension to the save file
	private void save() {
		BufferedWriter output = null;
		JFileChooser fileChooser = new JFileChooser();
		FileNameExtensionFilter filter = new FileNameExtensionFilter("TEXT FILES", "txt", "text");
		fileChooser.setFileFilter(filter);
		if (fileChooser.showSaveDialog(오목.this) == JFileChooser.APPROVE_OPTION) {
			File file = fileChooser.getSelectedFile();
			// add .txt extension if I don't already have it
			if (!fileChooser.getSelectedFile().getAbsolutePath().endsWith(".txt")) {
				file = new File(fileChooser.getSelectedFile() + ".txt");
			}
			try {
				output = new BufferedWriter(new FileWriter(file));
				String s = "";
				for (Point piece : pieces) {
					s += "(" + piece.x + "," + piece.y + ")";
				}
				output.write(s + "|" + bUndo + "|" + wUndo);
				System.out.println("저장 성공!");
			} catch (IOException e) {
				System.out.println("저장 불가능\n" + e.getMessage());
			} finally {
				if (output != null) {
					try {
						output.close();
					} catch (IOException e) {
						System.out.println("저장 파일 체크!\n" + e.getMessage());
					}
				}
			}
		}
	}

	// parse through the save file format (list of points and number of undo's) and reinitializes board accordingly
	private void load() {
		String line;
		String[] part, frags;
		BufferedReader input = null;
		JFileChooser fileChooser = new JFileChooser();
		FileNameExtensionFilter filter = new FileNameExtensionFilter("TEXT FILES", "txt", "text");
		fileChooser.setFileFilter(filter);
		if (fileChooser.showOpenDialog(오목.this) == JFileChooser.APPROVE_OPTION && !AIMode && !online) {
			File file = fileChooser.getSelectedFile();
			try {
				input = new BufferedReader(new FileReader(file));
				clear();
				line = input.readLine();
				part = line.split("\\|");
				bUndo = Integer.parseInt(part[1]);
				wUndo = Integer.parseInt(part[2]);
				frags = part[0].split("[\\W]"); // splits on any non-alphabetical character
				for (int i = 1; i < frags.length - 1; i = i + 3) {
					pieces.add(new Point(Integer.parseInt(frags[i]), Integer.parseInt(frags[i + 1])));
					// save some computational resources by NOT calculating threat spaces and shit if we don't have to
					if (TEST) AI.addPoint(Integer.parseInt(frags[i]), Integer.parseInt(frags[i + 1]));
				}
				show = pieces.size();
				set34 = open3(pieces); // for winning check
				System.out.println("set34: " + set34.toString());
				ifWon = won();
				repaint();
			} catch (IOException e) {
				playSound(-1);
				System.out.println("파일을 제대로 선택했는지 점검\n" + e.getMessage());
			} finally {
				if (input != null) {
					try {
						input.close();
					} catch (IOException e) {
						System.out.println("저장 파일 체크\n" + e.getMessage());
					}
				}
			}
		}
	}

	// house rule: bans a move that simultaneously forms two open rows of three stones
	private boolean legalMove(Point p) {
		// the rules go out the window when fighting AI.
		// TODO: Turn it back on when I figure out how to make the AI check if it is making legal moves
		for (Set<Point> set : set34) {
			if (set.contains(p) && set.size() == 3) {
				for (Point neighbor : set) {
					for (Set<Point> set2 : set34) {
						if (!set.equals(set2) && set2.contains(neighbor) && set2.size() == 3) {
							return false;
						}
					}
				}
			}
		}
		return true;
	}

	// checking win is done by checking if any sequence of 4 has a piece on the either end
	private boolean won() {
		if (pieces.size() < 9) {
			return false;
		}
		for (Set<Point> set : set34) {
			if (set.size() == 4) {
				List<Point> points = new ArrayList<>();
				points.addAll(set);
				if (points.get(0).x == points.get(1).x) { // they are on vertical line
					points.sort(Comparator.comparingInt(o -> o.y));
				} else { // either horizontal or diagonal line
					points.sort(Comparator.comparingInt(o -> o.x));
				}
				for (int i = (pieces.size() % 2 + 1) % 2; i < pieces.size(); i = i + 2) {
					if (pieces.get(i).equals(new Point(2 * points.get(0).x - points.get(1).x, 2 * points.get(0).y
							- points.get(1).y)) || pieces.get(i).equals(new Point(2 * points.get(3).x - points.get(2).x,
							2 * points.get(3).y - points.get(2).y))) {
						return true;
					}
				}
			}
		}
		return false;
	}

	// finds list of all sets of 3 adjacent points without any blockages and 4 that have at least one opening
	// quick and dirty way of finding open sets of 3 and 4, for checking for users' legal move and win conditions
	private List<Set<Point>> open3(List<Point> points) {
		List<Set<Point>> result = new ArrayList<>();
		for (int i = (points.size() % 2 + 1) % 2; i < points.size(); i = i + 2) {
			Point p1 = points.get(i);
			for (int j = i + 2; j < points.size(); j = j + 2) {
				Point p2 = points.get(j);
				if ((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y) <= 2) { // if p1 and p2 are adjacent
					for (int k = (points.size() % 2 + 1) % 2; k < points.size(); k = k + 2) {
						if (k != i && k != j) {
							Point p3 = points.get(k);
							boolean passed = false;
							boolean blocked = false;
							Point p41, p42;
							p41 = p42 = new Point();
							if (p3.x != 0 && p3.x != 18 && p3.y != 0 && p3.y != 18) {
								if (p3.equals(new Point(2 * p1.x - p2.x, 2 * p1.y - p2.y))) { // p3 p1 p2
									if (p2.x != 0 && p2.x != 18 && p2.y != 0 && p2.y != 18) { // boundary check
										p41 = new Point(2 * p2.x - p1.x, 2 * p2.y - p1.y); // checking both ends
										p42 = new Point(2 * p3.x - p1.x, 2 * p3.y - p1.y);
										passed = true;
									}
								} else if (p3.equals(new Point(2 * p2.x - p1.x, 2 * p2.y - p1.y))) { // p3 p2 p1
									if (p1.x != 0 && p1.x != 18 && p1.y != 0 && p1.y != 18) {
										p41 = new Point(2 * p1.x - p2.x, 2 * p1.y - p2.y);
										p42 = new Point(2 * p3.x - p2.x, 2 * p3.y - p2.y);
										passed = true;
									}
								}
								if (passed) {
									// if either is of other color, throw it out
									for (int n = points.size() % 2; n < points.size(); n = n + 2) {
										if (points.get(n).equals(p41) || points.get(n).equals(p42)) {
											passed = false;
											blocked = true;
										}
									}
									if (!blocked) {
										for (int n = (points.size() % 2 + 1) % 2; n < points.size(); n = n + 2) {
											if (points.get(n).equals(p41) || points.get(n).equals(p42)) {
												Set<Point> halfOpenSet4 = new HashSet<>();
												halfOpenSet4.add(p1);
												halfOpenSet4.add(p2);
												halfOpenSet4.add(p3);
												halfOpenSet4.add(points.get(n));
												if (!result.contains(halfOpenSet4)) {
													result.add(halfOpenSet4);
												}
												passed = false;
											}
										}
									}
								}
							}
							if (passed) {
								Set<Point> openSet3 = new HashSet<>();
								openSet3.add(p1);
								openSet3.add(p2);
								openSet3.add(p3);
								if (!result.contains(openSet3)) {
									result.add(openSet3);
								}
							}
						}
					}
				}
			}
		}
		return result;
	}

	public List<Point> getPieces() {
		return pieces;
	}

	public void setShow(int show) {
		this.show = show;
	}

	public void incrementUndo(int i) {
		if (i == 1) {
			wUndo++;
			System.out.println("wUndo: " + wUndo);
		} else {
			bUndo++;
			System.out.println("bUndo: " + bUndo);
		}
	}

	// for online use
	public void checkWin() {
		set34 = open3(pieces);
		if (won()) {
			ifWon = true;
			if (pieces.size() % 2 == 0) {
				playSound(-5);
				JOptionPane.showMessageDialog(오목.this, whiteWinString, endString, JOptionPane.INFORMATION_MESSAGE);
			} else {
				playSound(-5);
				JOptionPane.showMessageDialog(오목.this, blackWinString, endString, JOptionPane.INFORMATION_MESSAGE);
			}
		}
	}

	public void setConnecting(boolean b) {
		connecting = b;
		repaint();
	}

	private void playSound(int turn) {
		if (sound) {
			try {
				if (turn >= 0) {
					// black's move
					if (turn % 2 == 1) {
						AudioStream sfx1 = new AudioStream(getClass().getClassLoader().getResourceAsStream(audioPath1));
						AudioPlayer.player.start(sfx1);
					} else { // white's move
						AudioStream sfx2 = new AudioStream(getClass().getClassLoader().getResourceAsStream(audioPath2));
						AudioPlayer.player.start(sfx2);
					}
				} else {
					if (turn != -1) {
						// win sound
						AudioStream sfx3 = new AudioStream(getClass().getClassLoader().getResourceAsStream(audioPath3));
						AudioPlayer.player.start(sfx3);
					} else {
						// error sound
						AudioStream sfx4 = new AudioStream(getClass().getClassLoader().getResourceAsStream(audioPath4));
						AudioPlayer.player.start(sfx4);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private boolean inRange(Point p) {
		return (p.x < offset * 2 + square * 18 && p.y < offset * 2 + square * 18);
	}

	/* AI difficulty settings
	 * @param: defense weight
	 * @param: score threshold
	 * @param: score multiplier
	 * @param: clash evaluation method (1 works the best)
	 * @param: branch limit (the higher the better, but it takes a LOT more time)
	 * @param: depth (must be supplemented by large enough branch limit of >=5)
	 */
	private Jack newAI(int difficulty) {
		if (difficulty == 1) {
			return new Jack(0.95, 2.0/3, 2, 1, 5, 9);
		} else if (difficulty == 2) {
			return new Jack(0.95, 2.0/3.2, 2, 1, 5, 11);
		} else if (difficulty == 3) {
			return new Jack(0.92, 2.0/3.5, 2, 1, 6, 13);
		} else {
			System.out.println("Jack with "+testParamsDouble[0]+","+testParamsDouble[1]+","+
				testParamsInt[0]+","+testParamsInt[1]+","+testParamsInt[2]+","+testParamsInt[3]);
			return new Jack(testParamsDouble[0], testParamsDouble[1], testParamsInt[0], 
				testParamsInt[1], testParamsInt[2], testParamsInt[3]);
		}
	}

	public static void main(String[] cheese) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		new 오목();
		Test.warmup();
	}
}