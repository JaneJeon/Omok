/*
 * @author: Sungil Ahn
 */

// singleton database access
public class DBAccess {
	private static DBAccess instance = new DBAccess();

	private DBAccess() {
		// open connection to database
	}

	public static DBAccess getConnection() {
		return instance;
	}
}