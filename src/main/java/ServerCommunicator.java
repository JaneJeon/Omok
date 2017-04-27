import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/*
 * @author: Sungil Ahn
 */
public class ServerCommunicator extends Thread {
	private Socket sock; // to talk with client
	private BufferedReader in; // from client
	private PrintWriter out; // to client
	private Server server; // handling communication for
	private int id;
	private boolean firstMessage = false;
	private String key;

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
			// clean up
			out.close();
			in.close();
			sock.close();
		} catch (IOException e) {
			server.getLog().error(LoadResource.getTime() + " > " + e);
		} finally {
			server.removeCommunicator(this);
		}
	}
}