/*
 * @author: Sungil Ahn
 */

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

// singleton database access
public class DBAccess {
	private static DBAccess instance = new DBAccess();
	private Connection conn;

	private DBAccess() {
		// open connection to database
		try {
			Class.forName("org.h2.Driver");
			conn = DriverManager.getConnection("jdbc:h2:records.db;INIT=runscript from 'initialize.sql'");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// used to run application code & queries
	public static Connection getConnection() {
		return instance.conn;
	}

	public static void close() {
		try {
			instance.conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}