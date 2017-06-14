import sun.audio.AudioPlayer;

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

/*
 * @author: Sungil Ahn
 */

// this the actual board, and yes, there's a full page of instance variables that are all actually necessary.
public class 오목 extends JFrame {
	private static final int offset = 20; // how much space between end of board and boundary
	private static final int square = 40; // size of square
	private static final int pieceSize = 15; // radius of pieces
	private static final int fontSize = 20;
	private static final double lastPieceScale = 1.12;
	private static final String filePath = "background.jpg";
	private static final String audioPath1 = "sf1.aiff", audioPath2 = "sf2.aiff", audioPath3 = "sfx3.aiff",
		audioPath4 = "sfx4.aiff";
	private static final boolean TEST = false, ENGLISH = true;
	private static final String serverIP = LoadResource.getString("serverConfig.txt");
	private final String key = LoadResource.getString("Key.txt");
	private final BufferedImage image;
	private final String whiteWinString = 오목.ENGLISH ? "White wins!" : "백 승리!";
	private final String blackWinString = 오목.ENGLISH ? "Black wins!" : "흑 승리!";
	private final String endString = 오목.ENGLISH ? "Game over" : "게임 종료";
	private final String errorString = 오목.ENGLISH ? "Error" : "에러";
	public int[] testParamsInt = {2, 1, 5, 13};
	public double[] testParamsDouble = {1, 2 / 3.5};
	private Point click3, created;
	private List<Point> pieces;
	private List<Set<Point>> set34;
	private int mouseX, mouseY, show;
	private int bUndo, wUndo, startState = 1, undoErrorCount, difficulty = 1;
	private String font = "Lucina Grande";
	private boolean ifWon, showNum, calculating, AIMode, online,
		connecting, sound = true, AIblack;
	private double startTime, endTime;
	private Jack AI;
	private ClientCommunicator comm;
	// TODO: timer dropdown
	// TODO: update to Javadoc style, experiment with loading partially completed games' interaction with Jack
	// TODO: splash screen to let her know that game is loading
	// TODO: loading bar or icon for when calculating

	// constructor
	public 오목() {
		super("오목");
		// load in background here and not at paintComponent to greatly boost FPS
		if (!오목.TEST) this.image = LoadResource.getImage(오목.filePath);
		// Helpers to create the canvas and GUI (buttons, etc.)
		JComponent canvas = this.setupCanvas();
		JComponent gui = this.setupGUI();
		JMenuBar menu = this.setUpMenu();
		// Put the buttons and canvas together into the window
		Container cp = this.getContentPane();
		cp.setLayout(new BorderLayout());
		cp.add(canvas, BorderLayout.CENTER);
		cp.add(gui, BorderLayout.NORTH);
		setJMenuBar(menu);
		// Usual initialization
		this.setLocationRelativeTo(null);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.pack();
		setVisible(true);
		// initialize game
		this.pieces = new ArrayList<>();
		this.difficulty = 1;
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

	private JComponent setupCanvas() {
		JComponent canvas = new JComponent() {
			public void paintComponent(Graphics g) {
				super.paintComponent(g);
				// set background image to scale with window size
				if (!오목.this.connecting) {
					if (!오목.TEST) {
						g.drawImage(오목.this.image, 0, 0, 오목.offset * 2 + 오목.square * 18, 오목.offset * 2 + 오목.square * 18, null);
						for (int i = 0; i < 19; i++) { // draw base grid - horizontal, then vertical lines
							g.setColor(Color.black);
							g.drawLine(오목.offset, 오목.offset + i * 오목.square, 오목.offset + 18 * 오목.square, 오목.offset + i * 오목.square);
							g.drawLine(오목.offset + i * 오목.square, 오목.offset, 오목.offset + i * 오목.square, 오목.offset + 18 * 오목.square);
						}
						for (int x = 0; x < 3; x++) { // draw guiding dots
							for (int y = 0; y < 3; y++) {
								// dot size is fixed at 3
								g.fillOval(오목.offset + 오목.square * (6 * x + 3) - 3, 오목.offset + 오목.square * (6 * y + 3) - 3, 6, 6);
							}
						}
					}
					오목.this.drawPieces(g);
					오목.this.drawOverlay(g);
				} else {
					// splash screen for when you're connected to the server and waiting for an opponent
					FontMetrics metrics = g.getFontMetrics(new Font(오목.this.font, Font.PLAIN, 오목.fontSize * 3));
					g.setFont(new Font(오목.this.font, Font.PLAIN, 오목.fontSize * 3));
					String connecting = 오목.ENGLISH ? "Connecting..." : "연결중...";
					g.drawString(connecting, 오목.offset + 오목.square * 9 - (metrics.stringWidth(connecting) / 2),
						오목.offset + 오목.square * 9 - (metrics.getHeight() / 2) + metrics.getAscent());
				}
			}
		};
		canvas.setPreferredSize(new Dimension(오목.offset * 2 + 오목.square * 18, 오목.offset * 2 + 오목.square * 18));
		canvas.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				오목.this.play(e.getPoint());
			}
		});
		canvas.addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseMoved(MouseEvent e) {
				오목.this.mouseX = e.getPoint().x;
				오목.this.mouseY = e.getPoint().y;
				오목.this.repaint();
			}
		});
		return canvas;
	}

	private JComponent setupGUI() {
		// setting up the row of buttons beneath the menu bar
		String undoString = 오목.ENGLISH ? "Undo" : "한수 무르기";
		JButton undo = new JButton(undoString);
		undo.addActionListener(e -> {
			this.undo();
			if (this.AIMode) this.undo();
		});
		JButton clear;
		String restartString = 오목.ENGLISH ? "Restart" : "재시작";
		clear = new JButton(restartString);
		clear.addActionListener(e -> this.clear());
		String[] states = new String[4];
		if (!오목.ENGLISH) {
			states[0] = "로컬 2인용"; states[1] = "온라인 2인용"; states[2] = "컴퓨터 - 백"; states[3] = "컴퓨터 - 흑";
		} else {
			states[0] = "Local 2P"; states[1] = "Online 2P";
			states[2] = "CPU - White"; states[3] = "CPU - Black";
		}
		JComboBox<String> stateB = new JComboBox<>(states);
		stateB.addActionListener(e -> {
			if (((JComboBox<String>) e.getSource()).getSelectedItem() == "로컬 2인용" ||
				((JComboBox<String>) e.getSource()).getSelectedItem() == "Local 2P") {
				this.startState = 1;
			} else if (((JComboBox<String>) e.getSource()).getSelectedItem() == "컴퓨터 - 백" ||
				((JComboBox<String>) e.getSource()).getSelectedItem() == "CPU - White") {
				this.startState = 2;
			} else if (((JComboBox<String>) e.getSource()).getSelectedItem() == "컴퓨터 - 흑" ||
				((JComboBox<String>) e.getSource()).getSelectedItem() == "CPU - Black") {
				this.startState = 3;
			} else {
				this.startState = 4;
			}
			if (this.show == 0) this.clear();
		});
		JButton first = new JButton("<<");
		first.addActionListener(e -> {
			if (this.pieces.size() > 0) {
				this.show = 1;
				this.repaint();
			}
		});
		JButton prev = new JButton("<");
		prev.addActionListener(e -> {
			if (this.show > 1) {
				this.show--;
				this.repaint();
			}
		});
		JButton next = new JButton(">");
		next.addActionListener(e -> {
			if (this.show < this.pieces.size()) {
				this.show++;
				this.repaint();
			}
		});
		JButton last = new JButton(">>");
		last.addActionListener(e -> {
			this.show = this.pieces.size();
			this.repaint();
		});
		String soundString = 오목.ENGLISH ? "Sound" : "소리";
		JButton toggleSound = new JButton(soundString);
		toggleSound.addActionListener(e -> this.sound = !this.sound);
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
		if (오목.TEST) {
			JButton test = new JButton("test");
			test.addActionListener(e -> {
				System.out.println("Show = " + this.show);
				System.out.println("Pieces: " + this.pieces);
				this.AI.test();
			});
			gui.add(test);
		}
		return gui;
	}

	private JMenuBar setUpMenu() {
		JMenuBar menuBar = new JMenuBar();
		String fileString = 오목.ENGLISH ? "File" : "파일";
		JMenu fileMenu = new JMenu(fileString);
		String openString = 오목.ENGLISH ? "Open" : "열기";
		JMenuItem openMi = new JMenuItem(openString);
		openMi.addActionListener((ActionEvent e) -> {
			if (this.startState != 4) this.load();
		});
		String saveString = 오목.ENGLISH ? "Save" : "저장";
		JMenuItem saveMi = new JMenuItem(saveString);
		saveMi.addActionListener((ActionEvent e) -> this.save());
		fileMenu.add(openMi);
		fileMenu.add(saveMi);
		String numString = 오목.ENGLISH ? "Moves" : "번호";
		JMenu numMenu = new JMenu(numString);
		String showString = 오목.ENGLISH ? "Show" : "보이기";
		JCheckBoxMenuItem showMi = new JCheckBoxMenuItem(showString);
		showMi.setMnemonic(KeyEvent.VK_S);
		showMi.addItemListener((ItemEvent e) -> {
			this.showNum = !this.showNum;
			this.repaint();
		});
		String fontString = 오목.ENGLISH ? "Font" : "글꼴";
		JMenu fontMenu = new JMenu(fontString);
		fontMenu.setMnemonic(KeyEvent.VK_F);
		ButtonGroup fontGroup = new ButtonGroup();
		JRadioButtonMenuItem font1RMi = new JRadioButtonMenuItem("Arial");
		fontMenu.add(font1RMi);
		font1RMi.addItemListener((ItemEvent e) -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				this.font = "Arial";
				this.repaint();
			}
		});
		JRadioButtonMenuItem font2RMi = new JRadioButtonMenuItem("Courier");
		fontMenu.add(font2RMi);
		font2RMi.addItemListener((ItemEvent e) -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				this.font = "Courier";
				this.repaint();
			}
		});
		JRadioButtonMenuItem font3RMi = new JRadioButtonMenuItem("Helvetica Neue");
		fontMenu.add(font3RMi);
		font3RMi.addItemListener((ItemEvent e) -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				this.font = "Helvetica Neue";
				this.repaint();
			}
		});
		JRadioButtonMenuItem font4RMi = new JRadioButtonMenuItem("Lucina Grande");
		font4RMi.setSelected(true);
		fontMenu.add(font4RMi);
		font4RMi.addItemListener((ItemEvent e) -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				this.font = "Lucina Grande";
				this.repaint();
			}
		});
		fontGroup.add(font1RMi);
		fontGroup.add(font2RMi);
		fontGroup.add(font3RMi);
		fontGroup.add(font4RMi);
		numMenu.add(showMi);
		numMenu.add(fontMenu);
		String difficultyString = 오목.ENGLISH ? "Difficulty" : "난이도";
		JMenu difficultyMenu = new JMenu(difficultyString);
		fontMenu.setMnemonic(KeyEvent.VK_I);
		ButtonGroup difficultyGroup = new ButtonGroup();
		String highString = 오목.ENGLISH ? "Hard" : "상";
		JRadioButtonMenuItem difficulty1RMi = new JRadioButtonMenuItem(highString);
		difficultyMenu.add(difficulty1RMi);
		difficulty1RMi.addItemListener((ItemEvent e) -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				this.difficulty = 3;
				if (this.show == 0) this.clear();
			}
		});
		String medString = 오목.ENGLISH ? "Medium" : "중";
		JRadioButtonMenuItem difficulty2RMi = new JRadioButtonMenuItem(medString);
		difficultyMenu.add(difficulty2RMi);
		difficulty2RMi.addItemListener((ItemEvent e) -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				this.difficulty = 2;
				if (this.show == 0) this.clear();
			}
		});
		String lowString = 오목.ENGLISH ? "Easy" : "하";
		JRadioButtonMenuItem difficulty3RMi = new JRadioButtonMenuItem(lowString);
		difficulty3RMi.setSelected(true);
		difficultyMenu.add(difficulty3RMi);
		difficulty3RMi.addItemListener((ItemEvent e) -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				this.difficulty = 1;
				if (this.show == 0) this.clear();
			}
		});
		if (오목.TEST) {
			JRadioButtonMenuItem difficultyXRMi = new JRadioButtonMenuItem("Test");
			difficultyMenu.add(difficultyXRMi);
			difficultyXRMi.addItemListener((ItemEvent e) -> {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					this.difficulty = 42;
					String s = JOptionPane.showInputDialog(
						getFrames()[0], // I have no idea why this works
						"Def weight, score threshold, multiplier(2), eval(1), branch, depth", "Value input",
						JOptionPane.PLAIN_MESSAGE);
					System.out.println(s);
					String[] param = s.split(" ");
					this.testParamsDouble[0] = Double.parseDouble(param[0]);
					this.testParamsDouble[1] = Double.parseDouble(param[1]);
					this.testParamsInt[0] = Integer.parseInt(param[2]);
					this.testParamsInt[1] = Integer.parseInt(param[3]);
					this.testParamsInt[2] = Integer.parseInt(param[4]);
					this.testParamsInt[3] = Integer.parseInt(param[5]);
				}
			});
		}
		difficultyGroup.add(difficulty1RMi);
		difficultyGroup.add(difficulty2RMi);
		difficultyGroup.add(difficulty3RMi);
		String helpString = 오목.ENGLISH ? "About" : "설명 도움이";
		JMenu explain = new JMenu(helpString);
		String message = 오목.ENGLISH ? "            ✤ Feeling lucky? This version is **enhanced** with a sprinkle of RNG. "
			+ "Enjoy! ✤\n\n" +
			"* Use the File menu to open or save games\n" +
			"* Use the 'Moves' menu to display the order of moves as numbers, and modify the font\n" +
			"* You can select the game mode - make sure to press 'restart' after changing modes\n" +
			"* In addition, you can change the difficulty of the AI when playing single-player\n" +
			"-- again, make sure to press restart to apply the changes\n" +
			"* In any game mode, each color can only undo 3 moves. After that, you won't be able to undo\n" +
			"* You can click the arrows to browse through the moves - it won't affect the game state\n" +
			"* During online multiplayer, you can only undo your own color\n" +
			"* Press the sound button to turn sfx on/off\n\n" +
			"                                              ✿ Program written by Sungil Ahn ✿" :
			"1. 파일 메뉴를 눌러 게임을 저장하거나 열기\n2. 번호 메뉴를 눌러 수 보이기\n3. 번호 메뉴 안에 글꼴 바꾸기\n" +
				"4. 메뉴 밑에 오목 모드를 설정하기\n5. 모드나 난이도를 설정한후, 그 모드/난이도로 시작하려면 재시작을 누루기\n6. " +
				"흑/백 판 마다 최대 3번만 무를수 있음\n7. 온라인 2인용 일떼는 자기의 색깔만 되돌맀수 있음\n8. " +
				"언제든지 화살표들을 클릭헤서 앞으로나 뒤로 수를 보기\n9. << 는 제일 처음으로, < 는 지난 수로, > 는 다음 수로, "+
				"그리고 >> 은 현제/제일 마지막 수로\n10. 소리 버튼으로 sfx 크기/끄기\n\n 저자 - 안성일";
		String aboutBar = 오목.ENGLISH ? "Manual" : "사용설명서";
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
		FontMetrics metrics = g.getFontMetrics(new Font(this.font, Font.PLAIN, 오목.fontSize));
		FontMetrics metrics2 = g.getFontMetrics(new Font(this.font, Font.PLAIN, 오목.fontSize - 4));
		// only allow user to look back to the very first move
		if (this.show >= 1) {
			if ((this.show - 1) % 2 == 0) {
				g.setColor(Color.black);
			} else {
				g.setColor(Color.white);
			}
			// drawing the big piece that indicates the last piece
			if (!this.ifWon && this.show == this.pieces.size()) {
				g.fillOval(오목.offset + 오목.square * this.pieces.get(this.show - 1).x - (int) (오목.pieceSize * 오목.lastPieceScale), 오목.offset + 오목.square
					* this.pieces.get(this.show - 1).y - (int) (오목.pieceSize * 오목.lastPieceScale), (int) (오목.pieceSize * 2 *
					오목.lastPieceScale), (int) (오목.pieceSize * 2 * 오목.lastPieceScale));
			} else {
				g.fillOval(오목.offset + 오목.square * this.pieces.get(this.show - 1).x - 오목.pieceSize, 오목.offset + 오목.square * this.pieces.get(this.show - 1).y -
					오목.pieceSize, 오목.pieceSize * 2, 오목.pieceSize * 2);
			}
			if ((this.show - 1) % 2 == 0) {
				g.setColor(Color.white);
			} else {
				g.setColor(Color.black);
			}
			if (this.showNum) {
				g.setFont(new Font(this.font, Font.PLAIN, 오목.fontSize));
				g.drawString(Integer.toString(this.show), 오목.offset + 오목.square * this.pieces.get(this.show - 1).x
					- (metrics.stringWidth(Integer.toString(this.show))) / 2, 오목.offset + 오목.square * this.pieces.get(this.show - 1).y
					- (metrics.getHeight()) / 2 + metrics.getAscent());
			}
		}
		// drawing the regular pieces
		for (int i = 0; i < this.show - 1; i++) {
			if (i % 2 == 0) { // black's pieces
				g.setColor(Color.black);
				g.fillOval(오목.offset + 오목.square * this.pieces.get(i).x - 오목.pieceSize, 오목.offset + 오목.square * this.pieces.get(i).y - 오목.pieceSize,
					오목.pieceSize * 2, 오목.pieceSize * 2);
				if (this.showNum) {
					g.setColor(Color.white);
				}
			} else {
				g.setColor(Color.white);
				g.fillOval(오목.offset + 오목.square * this.pieces.get(i).x - 오목.pieceSize, 오목.offset + 오목.square * this.pieces.get(i).y - 오목.pieceSize,
					오목.pieceSize * 2, 오목.pieceSize * 2);
				if (this.showNum) {
					g.setColor(Color.black);
				}
			}
			if (this.showNum) { // drawing numbers
				if (i < 99) {
					g.setFont(new Font(this.font, Font.PLAIN, 오목.fontSize));
					g.drawString(Integer.toString(i + 1), 오목.offset + 오목.square * this.pieces.get(i).x
						- (metrics.stringWidth(Integer.toString(i + 1))) / 2, 오목.offset + 오목.square * this.pieces.get(i).y
						- (metrics.getHeight()) / 2 + metrics.getAscent());
				} else {
					g.setFont(new Font(this.font, Font.PLAIN, 오목.fontSize - 4)); // 3-digits getString decreased font size
					g.drawString(Integer.toString(i + 1), 오목.offset + 오목.square * this.pieces.get(i).x
						- (metrics2.stringWidth(Integer.toString(i + 1))) / 2, 오목.offset + 오목.square * this.pieces.get(i).y
						- (metrics2.getHeight()) / 2 + metrics2.getAscent());
				}
			}
		}
		// colored scores to see relative scores of every potential threat space
		if (오목.TEST) {
			g.setFont(new Font(this.font, Font.PLAIN, 오목.fontSize - 4));
			int[][] scores = this.AI.getScores();
			for (int i = 0; i < 19; i++) {
				for (int j = 0; j < 19; j++) {
					if (scores[i][j] > 0) {
						g.setColor(Color.blue);
					} else if (scores[i][j] < 0) {
						g.setColor(Color.red);
					} else {
						g.setColor(Color.gray);
					}
					g.drawString(Integer.toString(scores[i][j]), 오목.offset + 오목.square * i
						- (metrics2.stringWidth(Integer.toString(scores[i][j]))) / 2, 오목.offset + 오목.square * j
						- (metrics2.getHeight()) / 2 + metrics2.getAscent());
				}
			}
		}
	}

	private void drawOverlay(Graphics g) {
		if (!this.calculating) {
			if (!this.ifWon) { // disable mouse overlays if the game is over
				int px = Math.round((this.mouseX - 오목.offset + 오목.square / 2) / 오목.square);
				int py = Math.round((this.mouseY - 오목.offset + 오목.square / 2) / 오목.square);
				if (this.created == null) {
					if (this.click3 != null) {
						if ((this.click3.x - px) * (this.click3.x - px) + (this.click3.y - py) * (this.click3.y - py) >= 1) {
							this.click3 = null;
							return;
						}
						// if someone clicks on the illegal space, then warn the user with the red color
						g.setColor(new Color(220, 83, 74));
						g.fillOval(오목.offset + 오목.square * px - 오목.pieceSize, 오목.offset + 오목.square * py - 오목.pieceSize,
							오목.pieceSize * 2, 오목.pieceSize * 2);
						return;
					}
					// red color for when the user hovers over an existing piece
					for (int i = 0; i < this.pieces.size(); i++) {
						Point p = this.pieces.get(i);
						if ((p.x - px) * (p.x - px) + (p.y - py) * (p.y - py) < 1) {
							g.setColor(new Color(220, 83, 74));
							if (i != this.pieces.size() - 1) {
								g.fillOval(오목.offset + 오목.square * px - 오목.pieceSize, 오목.offset + 오목.square * py - 오목.pieceSize,
									오목.pieceSize * 2, 오목.pieceSize * 2);
							} else {
								g.fillOval(오목.offset + 오목.square * px - (int) (오목.pieceSize * 오목.lastPieceScale), 오목.offset + 오목.square
										* py - (int) (오목.pieceSize * 오목.lastPieceScale), (int) (오목.pieceSize * 2 * 오목.lastPieceScale),
									(int) (오목.pieceSize * 2 * 오목.lastPieceScale));
							}
							return;
						}
					}
					// restore color when the mouse moves out of prohibited spaces
					if (this.pieces.size() % 2 == 0) {
						g.setColor(new Color(0, 0, 0, 127));
						g.fillOval(오목.offset + 오목.square * px - 오목.pieceSize, 오목.offset + 오목.square * py - 오목.pieceSize,
							오목.pieceSize * 2, 오목.pieceSize * 2);
					} else {
						g.setColor(new Color(255, 255, 255, 127));
						g.fillOval(오목.offset + 오목.square * px - 오목.pieceSize, 오목.offset + 오목.square * py - 오목.pieceSize,
							오목.pieceSize * 2, 오목.pieceSize * 2);
					}
					return;
				}
				if ((this.created.x - px) * (this.created.x - px) + (this.created.y - py) * (this.created.y - py) >= 1) {
					this.created = null;
				}
			}
		}
	}

	// TODO: play error sound when online and playing in the wrong turn, with a warning pane
	private void play(Point p) {
		if (!this.ifWon) {
			int px = Math.round((p.x - 오목.offset + 오목.square / 2) / 오목.square);
			int py = Math.round((p.y - 오목.offset + 오목.square / 2) / 오목.square);
			Point pt = new Point(px, py);
			if (!this.pieces.contains(pt)) {
				List<Point> piecesCopy = new ArrayList<>(this.pieces);
				piecesCopy.add(pt);
				this.set34 = this.open3(piecesCopy);
				if (this.AIMode || this.legalMove(pt)) { // legitimate move by the user
					if (오목.TEST || this.AIMode) this.AI.addPoint(px, py);
					if (this.online) {
						this.comm.send("add " + px + " " + py);
					} else {
						this.pieces.add(pt);
					}
					this.show = this.pieces.size();
					this.playSound(this.show);
					this.created = pt;
					if (!this.online) { // when offline, check for win
						if (this.won()) {
							this.ifWon = true;
							this.playSound(-5);
							if (this.pieces.size() % 2 == 0) {
								JOptionPane.showMessageDialog(this, this.whiteWinString,
									this.endString, JOptionPane.INFORMATION_MESSAGE);
							} else {
								JOptionPane.showMessageDialog(this, this.blackWinString,
									this.endString, JOptionPane.INFORMATION_MESSAGE);
							}
							this.repaint();
						} else {
							this.repaint(); // update the board
							if (this.AIMode) { // let AI make the move
								this.calculating = true;
								this.startTime = System.nanoTime();
								new Thread(() -> this.AI.winningMove()).start();
							}
						}
					}
				} else {
					// illegal move!
					this.click3 = pt;
					this.pieces.remove(this.click3);
					this.repaint();
					this.playSound(-1);
					String illegalString = (오목.ENGLISH) ? "Illegal 3x3 move" : "삼삼!";
					JOptionPane.showMessageDialog(this, illegalString, this.errorString, JOptionPane.ERROR_MESSAGE);
				}
			}
		}
	}
	
	public void callback(Point p) {
		System.out.println("Called back");
		Point tmp = p;
		this.pieces.add(tmp);
		this.set34 = this.open3(this.pieces);
		this.AI.addPoint(tmp.x, tmp.y);
		this.endTime = System.nanoTime();
		double duration = (this.endTime - this.startTime) / 1000000;
		System.out.println("It took " + duration + " ms to calculate the best move");
		this.calculating = false;
		this.show = this.pieces.size();
		this.playSound(this.show);
		if (this.won()) {
			this.ifWon = true;
			this.playSound(-5);
			String cpuString = (오목.ENGLISH) ? "CPU wins!" : "컴퓨터 승리";
			JOptionPane.showMessageDialog(this, cpuString, this.endString,
				JOptionPane.INFORMATION_MESSAGE);
		}
		this.repaint();
	}

	// checks for conditions to undo (# of undo's, turn, and number of pieces on the board)
	private void undo() {
		if ((!this.ifWon || 오목.TEST) && this.pieces.size() > 0) {
			String backString = (오목.ENGLISH) ? "Can't undo" : "수 되돌리기 불가능!";
			if ((this.pieces.size() % 2 == 1 && this.bUndo < 3) || (this.pieces.size() % 2 == 0 && this.wUndo < 3)) {
				if (!this.online) {
					if (!this.AIMode) {
						this.pieces.remove(this.pieces.size() - 1);
						if (this.pieces.size() % 2 == 1) {
							this.wUndo++;
							System.out.println("wUndo: " + this.wUndo);
						} else {
							this.bUndo++;
							System.out.println("bUndo: " + this.bUndo);
						}
						if (오목.TEST) {
							this.AI.undo();
							System.out.println("AI undo");
						}
					} else {
						if (!this.AIblack || this.pieces.size() > 1) {
							this.pieces.remove(this.pieces.size() - 1);
							if (this.pieces.size() % 2 == 1) {
								this.wUndo++;
							} else {
								this.bUndo++;
							}
							this.AI.undo();
							System.out.println("AI undo");
						} else {
							if (this.undoErrorCount % 2 == 0) {
								this.playSound(-1);
								JOptionPane.showMessageDialog(this, backString, this.errorString,
									JOptionPane.ERROR_MESSAGE);
							}
							this.undoErrorCount++;
						}
					}
				} else {
					this.comm.send("undo");
				}
				this.set34 = this.open3(this.pieces);
				this.show = this.pieces.size();
				this.playSound(this.show);
			} else {
				if (!this.AIMode || this.undoErrorCount % 2 == 0) {
					this.playSound(-1);
					JOptionPane.showMessageDialog(this, backString, this.errorString, JOptionPane.ERROR_MESSAGE);
				}
				this.undoErrorCount++;
			}
			this.repaint();
		}
	}

	// resets board, AI, and all the associated instance variables and starts a new game
	private void clear() {
		this.pieces = new ArrayList<>();
		this.set34 = new ArrayList<>();
		this.bUndo = this.wUndo = 0;
		this.show = 0;
		this.connecting = this.online = this.ifWon = this.calculating = false;
		this.created = null;
		this.click3 = null;
		this.AI = this.newAI(this.difficulty);
		this.undoErrorCount = 0;
		if (this.startState == 1) { // 2P
			this.AIMode = false;
		} else if (this.startState == 2) { // COM WHITE
			this.AIMode = true;
			this.AIblack = false;
		} else if (this.startState == 3) { // COM BLACK
			this.AIMode = true;
			this.AIblack = true;
			this.pieces.add(new Point(9, 9));
			this.show++;
			this.AI.addPoint(9, 9);
			this.playSound(1);
		} else { // Online multi-player
			try {
				this.comm = new ClientCommunicator(오목.serverIP, this);
				this.comm.setDaemon(true);
				this.comm.start();
				this.comm.send(this.key);
				this.online = true;
				this.setConnecting(true);
			} catch (Exception e) {
				// TODO: handle exception when cannot connect to server in a way that doesn't crash the game
				this.playSound(-1);
				JOptionPane.showMessageDialog(this, "서버에 연결 불가능", "에러", JOptionPane.ERROR_MESSAGE);
			}
		}
		this.repaint();
	}

	// pops up save dialog, automatically add .txt extension to the save file
	private void save() {
		BufferedWriter output = null;
		JFileChooser fileChooser = new JFileChooser();
		FileNameExtensionFilter filter = new FileNameExtensionFilter("TEXT FILES", "txt", "text");
		fileChooser.setFileFilter(filter);
		if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			File file = fileChooser.getSelectedFile();
			// add .txt extension if I don't already have it
			if (!fileChooser.getSelectedFile().getAbsolutePath().endsWith(".txt")) {
				file = new File(fileChooser.getSelectedFile() + ".txt");
			}
			try {
				output = new BufferedWriter(new FileWriter(file));
				String s = "";
				for (Point piece : this.pieces) {
					s += "(" + piece.x + "," + piece.y + ")";
				}
				output.write(s + "|" + this.bUndo + "|" + this.wUndo);
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
		if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION && !this.AIMode && !this.online) {
			File file = fileChooser.getSelectedFile();
			try {
				input = new BufferedReader(new FileReader(file));
				this.clear();
				line = input.readLine();
				part = line.split("\\|");
				this.bUndo = Integer.parseInt(part[1]);
				this.wUndo = Integer.parseInt(part[2]);
				frags = part[0].split("[\\W]"); // splits on any non-alphabetical character
				for (int i = 1; i < frags.length - 1; i = i + 3) {
					this.pieces.add(new Point(Integer.parseInt(frags[i]), Integer.parseInt(frags[i + 1])));
					// save some computational resources by NOT calculating threat spaces and shit if we don't have to
					if (오목.TEST) this.AI.addPoint(Integer.parseInt(frags[i]), Integer.parseInt(frags[i + 1]));
				}
				this.show = this.pieces.size();
				this.set34 = this.open3(this.pieces); // for winning check
				System.out.println("set34: " + this.set34);
				this.ifWon = this.won();
				this.repaint();
			} catch (IOException e) {
				this.playSound(-1);
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
		for (Set<Point> set : this.set34) {
			if (set.contains(p) && set.size() == 3) {
				for (Point neighbor : set) {
					for (Set<Point> set2 : this.set34) {
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
		if (this.pieces.size() < 9) {
			return false;
		}
		for (Set<Point> set : this.set34) {
			if (set.size() == 4) {
				List<Point> points = new ArrayList<>();
				points.addAll(set);
				if (points.get(0).x == points.get(1).x) { // they are on vertical line
					points.sort(Comparator.comparingInt(o -> o.y));
				} else { // either horizontal or diagonal line
					points.sort(Comparator.comparingInt(o -> o.x));
				}
				for (int i = (this.pieces.size() % 2 + 1) % 2; i < this.pieces.size(); i = i + 2) {
					if (this.pieces.get(i).equals(new Point(2 * points.get(0).x - points.get(1).x, 2 * points.get(0).y
						- points.get(1).y)) || this.pieces.get(i).equals(new Point(2 * points.get(3).x - points.get(2).x,
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
		return this.pieces;
	}

	public void setShow(int show) {
		this.show = show;
	}

	public void incrementUndo(int i) {
		if (i == 1) {
			this.wUndo++;
			System.out.println("wUndo: " + this.wUndo);
		} else {
			this.bUndo++;
			System.out.println("bUndo: " + this.bUndo);
		}
	}

	// for online use
	public void checkWin() {
		this.set34 = this.open3(this.pieces);
		if (this.won()) {
			this.ifWon = true;
			if (this.pieces.size() % 2 == 0) {
				this.playSound(-5);
				JOptionPane.showMessageDialog(this, this.whiteWinString, this.endString, JOptionPane.INFORMATION_MESSAGE);
			} else {
				this.playSound(-5);
				JOptionPane.showMessageDialog(this, this.blackWinString, this.endString, JOptionPane.INFORMATION_MESSAGE);
			}
		}
	}

	public void setConnecting(boolean b) {
		this.connecting = b;
		this.repaint();
	}

	private void playSound(int turn) {
		if (this.sound) {
			if (turn >= 0) {
				// black's move
				if (turn % 2 == 1) {
					AudioPlayer.player.start(LoadResource.getSfx(오목.audioPath1));
				} else { // white's move
					AudioPlayer.player.start(LoadResource.getSfx(오목.audioPath2));
				}
			} else {
				if (turn != -1) {
					// win sound
					AudioPlayer.player.start(LoadResource.getSfx(오목.audioPath3));
				} else {
					// error sound
					AudioPlayer.player.start(LoadResource.getSfx(오목.audioPath4));
				}
			}
		}
	}

	/**
	 *  AI difficulty settings
	 * @param: defense weight
	 * @param: score threshold
	 * @param: score multiplier
	 * @param: clash evaluation method (1 works the best)
	 * @param: branch limit (the higher the better, but it takes a LOT more time)
	 * @param: depth (must be supplemented by large enough branch limit of >=5)
	 */
	private Jack newAI(int difficulty) {
		if (difficulty != 4) {
			return LoadResource.getAI(false, 오목.TEST, difficulty).board(this);
		} else {
			System.out.println("Jack with " + this.testParamsDouble[0] + "," + this.testParamsDouble[1] + "," +
				this.testParamsInt[0] + "," + this.testParamsInt[1] + "," + this.testParamsInt[2] + "," + this.testParamsInt[3]);
			return new Jack(false).defenseWeight(this.testParamsDouble[0]).threshold(this.testParamsDouble[1])
				.pieceScore(this.testParamsInt[0]).evalMethod(this.testParamsInt[1])
				.branchLImit(this.testParamsInt[2]).depth(this.testParamsInt[3]).board(this);
		}
	}
}