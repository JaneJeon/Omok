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
		this.out.println(msg);
	}

	public int getCommId() {
		return this.id;
	}

	public void setCommId(int id) {
		this.id = id;
	}

	public boolean getFirstMsgStatus() {
		return this.firstMessage;
	}

	public void close() {
		try {
			this.sock.close();
			this.out.close();
			this.in.close();
			this.server.removeCommunicator(this);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		try {
			// communication channel
			this.in = new BufferedReader(new InputStreamReader(this.sock.getInputStream()));
			this.out = new PrintWriter(this.sock.getOutputStream(), true);
			System.out.println("Someone found!");
			this.server.getLog().info(LoadResource.getTime() + " > " + "Someone found!");
			String msg;
			String[] part;
			while ((msg = this.in.readLine()) != null) {
				if (this.firstMessage) {
					this.server.printMsg("Received: " + msg);
					this.server.printMsg("Turn: " + this.server.getTurn(this.id) + ", id: " + this.id);
					int turn = this.server.getTurn(this.id);
					if (turn != -1) {
						part = msg.split(" ");
						if (part[0].equals("add")) {
							if (this.id % 2 == turn) { // can only place when it's the person's turn
								this.server.broadcast(msg, this);
								this.server.setTurn(this.id);
							} else {
								this.server.broadcast("failed to add", this);
							}
						} else if (part[0].equals("undo")) {
							if ((this.id + 1) % 2 == turn) { // can only undo their own colors
								this.server.broadcast(msg, this);
								this.server.setTurn(this.id);
							} else {
								this.server.broadcast("failed to undo", this);
							}
						}
					}
				} else {
					if (msg.equals(this.key)) this.firstMessage = true;
				}
			}
		} catch (IOException e) {
			this.server.getLog().error(LoadResource.getTime() + " > " + e);
		} finally {
			this.close();
		}
	}
}