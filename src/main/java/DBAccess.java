/*
 * @author: Sungil Ahn
 */
public class DBAccess {
	private static DBAccess instance = new DBAccess();

	private DBAccess() {
		// open connection to database
	}

	public static DBAccess getConnection() {
		return instance;
	}

	public static void close() {
		// close connection to database
		System.out.println("Closing database!");
	}
}
