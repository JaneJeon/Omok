import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

// The server infrastructure is just for the message routing.
// all the validation is done client-side
// It connects to each client on individual sockets and pairs two clients together
public class Server {
	private ServerSocket listen; // for accepting connections
	private List<ServerCommunicator> waitingList;
	private List<ServerCommunicator[]> players;
	private List<Integer> turn;
	// How to log off from the server in a way that doesn't cut off the server:
	// https://unix.stackexchange.com/a/488
	// $ nohup java -jar /home/me/Server.jar &
	// $ exit
	// TODO: automatic running of jar upon startup, cutting connections upon removing communicator
	// TODO: bug where one client can double click restart and get connected to himself
	// TODO: bug where both can press at the same time and both would register at the same point
	// TODO: automatic saving over the server
	// TODO: clearing out cache so that empty spot is filled up from the bottom up

	public Server(ServerSocket listen) {
		this.listen = listen;
		waitingList = new ArrayList<>();
		players = new ArrayList<>();
		turn = new ArrayList<>();
		System.out.println("Awaiting connections...");
	}

	public void getConnections() throws IOException, InterruptedException {
		while (true) {
			ServerCommunicator comm = new ServerCommunicator(listen.accept(), this);
			comm.setDaemon(true);
			comm.start();
			waitingList.add(comm);
			if (waitingList.size() >= 2) {
				ServerCommunicator[] temp = new ServerCommunicator[2];
				for (int i=0; i<2; i++) {
					temp[i] = waitingList.get(0);
					waitingList.remove(0);
					temp[i].setCommId(players.size() * 10 + i + 1);
				}
				players.add(temp);
				turn.add(1);
				synchronized (Server.this) { // TODO: find a better way
					wait(500);
				}
				broadcast("connected", temp[0]);
				System.out.println("Total pairs: "+players.size());
			}
		}
	}

	public synchronized void removeCommunicator(ServerCommunicator comm) {
		if (!waitingList.contains(comm)) {
			for (ServerCommunicator[] set : players) {
				if (set[0] != null) {
					if (set[0].equals(comm) || set[1].equals(comm)) {
						set[0] = null;
						set[1] = null;
						System.out.println("removed a pair");
					}
				}
			}
		} else {
			waitingList.remove(comm);
			System.out.println("removed from waiting list");
		}
	}

	public synchronized void broadcast(String msg, ServerCommunicator comm) {
		for (ServerCommunicator[] set : players) {
			if (set[0] != null) {
				if (set[0].equals(comm) || set[1].equals(comm)) {
					System.out.println("Broadcast: "+msg+" to: "+set[0].getCommId()+" & "+set[1].getCommId());
					for (int i=0; i<2; i++) {
						set[i].send(msg);
					}
				}
			}
		}
	}

	public int getTurn(int id) {
		for (ServerCommunicator[] set : players) {
			if (set[0] != null) {
				if (set[0].getCommId() == id || set[1].getCommId() == id) {
					return turn.get(Math.floorDiv(id, 10));
				}
			}
		}
		return -1;
	}

	public void setTurn(int id) {
		turn.set(Math.floorDiv(id, 10), 1 - turn.get(Math.floorDiv(id, 10)));
	}

	public void printMsg(String msg) {
		System.out.println(msg);
	}

	public static void main(String[] cheese) throws Exception {
		new Server(new ServerSocket(8080)).getConnections();
	}
}