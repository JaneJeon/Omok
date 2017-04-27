import java.util.Scanner;

/*
 * @author: Sungil Ahn
 */
// I want to differentiate this from Functions class just so that calling it is nicer
// ie. Functions.getString() < LoadString.get()
public class LoadString {
	public static String get(String path) {
		return new Scanner(LoadString.class.getClassLoader().getResourceAsStream(path)).next();
	}
}