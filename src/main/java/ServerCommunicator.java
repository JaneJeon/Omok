import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/*
 * @author: Sungil Ahn
 */
public class ServerCommunicator extends Thread {
	private final Socket sock; // to talk with client
	private final Server server; // handling communication for
	private final String key;
	private BufferedReader in; // from client
	private PrintWriter out; // to client
	private int id;
	private boolean firstMessage;

	public ServerCommunicator(Socket sock, Server server, String key) {
		this.sock = sock;
		this.server = server;
		this.key = key;
	}

	public void send(String msg) {
		out.println(msg);
	}

	public int getCommId() {
		return id;
	}

	public void setCommId(int id) {
		this.id = id;
	}

	public boolean getFirstMsgStatus() {
		return firstMessage;
	}

	public void close() {
		try {
			sock.close();
			out.close();
			in.close();
			server.removeCommunicator(this);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		try {
			// communication channel
			in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			out = new PrintWriter(sock.getOutputStream(), true);
			System.out.println("Someone found!");
			server.getLog().info(LoadResource.getTime() + " > " + "Someone found!");
			String msg;
			String[] part;
			while ((msg = in.readLine()) != null) {
				if (firstMessage) {
					server.printMsg("Received: " + msg);
					server.printMsg("Turn: " + server.getTurn(id) + ", id: " + id);
					int turn = server.getTurn(id);
					if (turn != -1) {
						part = msg.split(" ");
						if (part[0].equals("add")) {
							if (id % 2 == turn) { // can only place when it's the person's turn
								server.broadcast(msg, this);
								server.setTurn(id);
							} else {
								server.broadcast("failed to add", this);
							}
						} else if (part[0].equals("undo")) {
							if ((id + 1) % 2 == turn) { // can only undo their own colors
								server.broadcast(msg, this);
								server.setTurn(id);
							} else {
								server.broadcast("failed to undo", this);
							}
						}
					}
				} else {
					if (msg.equals(key)) firstMessage = true;
				}
			}
		} catch (IOException e) {
			server.getLog().error(LoadResource.getTime() + " > " + e);
		} finally {
			close();
		}
	}
}