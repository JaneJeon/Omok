import sun.audio.AudioStream;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
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

	public static AudioStream getSfx(String path) {
		try {
			return new AudioStream(LoadResource.class.getClassLoader().getResourceAsStream(path));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
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
}