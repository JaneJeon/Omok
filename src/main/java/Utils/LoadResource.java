package Utils;

import AI.Jack;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Scanner;

/*
 * @author: Sungil Ahn
 */

// Loads resources in a nice, clean way
public class LoadResource {
	public static String getString(String path) {
		return new Scanner(LoadResource.class.getClassLoader().getResourceAsStream(path)).next();
	}

	// https://www.ntu.edu.sg/home/ehchua/programming/java/J8c_PlayingSound.html
	public static void play(String path) {
		try {
			URL url = LoadResource.class.getClassLoader().getResource(path);
			AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(url);
			Clip clip = AudioSystem.getClip();
			clip.open(audioInputStream);
			clip.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static BufferedImage getImage(String path) {
		try {
			return ImageIO.read(LoadResource.class.getClassLoader().getResourceAsStream(path));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	// date time in a nice format, as yyyy-mm-dd hh:mm:ss
	public static String getTime() {
		return LocalDateTime.now().getYear() + "-" +
			String.format("%02d", LocalDateTime.now().getMonthValue()) + "-" +
			String.format("%02d", LocalDateTime.now().getDayOfMonth()) + " " +
			String.format("%02d", LocalDateTime.now().getHour()) + ":" +
			String.format("%02d", LocalDateTime.now().getMinute()) + ":" +
			String.format("%02d", LocalDateTime.now().getSecond());
	}

	// because I hate seeing java.awt.Point cluttering up on my error logs
	public static String printPoint(Point p) {
		return "(" + p.x + "," + p.y + ")";
	}

	// streamlines the AI creating process
	// only 1, 2, or 3 should be passed in
	public static Jack getAI(boolean headless, boolean debug, int difficulty) {
		switch (difficulty) {
			case 1:
				return new Jack(headless).defenseWeight(0.95).threshold(2.0 / 3).pieceScore(2).evalMethod(1)
					.branchLimit(7).depth(9).debug(debug);
			case 2:
				return new Jack(headless).defenseWeight(0.95).threshold(2.0 / 3.2).pieceScore(2).evalMethod(1)
					.branchLimit(7).depth(11).debug(debug);
			default:
				return new Jack(headless).defenseWeight(0.92).threshold(2.0 / 3.6).pieceScore(2).evalMethod(1)
					.branchLimit(7).depth(13).debug(debug);
		}
	}
}