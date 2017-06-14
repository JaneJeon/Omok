import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

/*
 * @author: Sungil Ahn
 */

// The server infrastructure is just for the message routing. All the validation is done client-side
// It connects to each client on individual sockets and pairs two clients together
public class Server {
	static final Logger log = LogManager.getLogger(Server.class.getName());
	private final ServerSocket listen; // for accepting connections
	private final List<ServerCommunicator> waitingList;
	private final List<ServerCommunicator[]> players;
	private final List<Integer> turn;
	private final String key;
	// How to log off from the server in a way that doesn't cut off the server:
	// https://unix.stackexchange.com/a/488
	// $ nohup java -jar /home/me/Server.jar &
	// $ exit
	// TODO: automatic running of jar upon startup, cutting connections upon removing communicator
	// https://askubuntu.com/a/99582
	// TODO: bug where one client can double click restart and getString connected to himself
	// TODO: bug where both can press at the same time and both would register at the same point
	// TODO: clearing out cache so that empty spot is filled up from the bottom up

	public Server(ServerSocket listen) {
		this.key = LoadResource.getString("Key.txt");
		this.listen = listen;
		this.waitingList = new ArrayList<>();
		this.players = new ArrayList<>();
		this.turn = new ArrayList<>();
		this.printMsg("Awaiting connections...");
	}

	public static void main(String[] cheese) {
		try {
			new Server(new ServerSocket(8080)).getConnections();
		} catch (Exception e) {
			Server.log.error(LoadResource.getTime() + " > " + e);
		}
	}

	// is this the 'event loop' the js people are creaming their pants about?
	public void getConnections() {
		while (true) {
			try {
				ServerCommunicator comm = new ServerCommunicator(this.listen.accept(), this, this.key);
				comm.setDaemon(true);
				comm.start();
				new Bouncer(this, comm);
			} catch (Exception e) {
				Server.log.error(LoadResource.getTime() + " > " + e);
			}
		}
	}

	public void addToList(ServerCommunicator comm) {
		this.waitingList.add(comm);
		if (this.waitingList.size() >= 2) {
			ServerCommunicator[] temp = new ServerCommunicator[2];
			for (int i = 0; i < 2; i++) {
				temp[i] = this.waitingList.get(0);
				this.waitingList.remove(0);
				temp[i].setCommId(this.players.size() * 10 + i + 1);
			}
			this.players.add(temp);
			this.turn.add(1);
			this.broadcast("connected", temp[0]);
			this.printMsg("Total pairs: " + this.players.size());
		}
	}

	public synchronized void removeCommunicator(ServerCommunicator comm) {
		if (!this.waitingList.contains(comm)) {
			for (ServerCommunicator[] set : this.players) {
				if (set[0] != null) {
					if (set[0].equals(comm) || set[1].equals(comm)) {
						set[0] = null;
						set[1] = null;
						this.printMsg("removed a pair");
					}
				}
			}
		} else {
			this.waitingList.remove(comm);
			this.printMsg("removed from waiting list");
		}
	}

	public synchronized void broadcast(String msg, ServerCommunicator comm) {
		for (ServerCommunicator[] set : this.players) {
			if (set[0] != null) {
				if (set[0].equals(comm) || set[1].equals(comm)) {
					this.printMsg("Broadcast: " + msg + " to: " + set[0].getCommId() + " & " + set[1].getCommId());
					for (int i = 0; i<2; i++) {
						set[i].send(msg);
					}
				}
			}
		}
	}

	public int getTurn(int id) {
		for (ServerCommunicator[] set : this.players) {
			if (set[0] != null) {
				if (set[0].getCommId() == id || set[1].getCommId() == id) {
					return this.turn.get(Math.floorDiv(id, 10));
				}
			}
		}
		return -1;
	}

	public void setTurn(int id) {
		this.turn.set(Math.floorDiv(id, 10), 1 - this.turn.get(Math.floorDiv(id, 10)));
	}

	public void killBouncer(Bouncer bouncer) {
		bouncer = null; // wait to be garbage collected
		this.printMsg("Bouncer killed.");
	}

	public void printMsg(String msg) {
		Server.log.info(LoadResource.getTime() + " > " + msg);
		System.out.println(LoadResource.getTime() + " > " + msg);
	}

	public Logger getLog() {
		return Server.log;
	}
}