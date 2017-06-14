import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/*
 * @author: Sungil Ahn
 */
public class ClientCommunicator extends Thread {
	private final 오목 client;
	private PrintWriter out;		// to server
	private BufferedReader in;		// from server
	private Socket sock;

	public ClientCommunicator(String serverIP, 오목 client) {
		this.client = client;
		System.out.println("connecting to " + serverIP + "...");
		try {
			this.sock = new Socket(serverIP, 8080);
			this.out = new PrintWriter(this.sock.getOutputStream(), true);
			this.in = new BufferedReader(new InputStreamReader(this.sock.getInputStream()));
			System.out.println("...connected");
		}
		catch (IOException e) {
			System.err.println("couldn't connect");
			System.exit(-1);
		}
	}

	// send message to server
	public void send(String msg) {
		this.out.println(msg);
	}

	public void run() {
		String line;
		try {
			while ((line = this.in.readLine()) != null) {
				String[] part = line.split(" ");
				switch (part[0]) {
					case "add":
						this.client.getPieces().add(new Point(Integer.parseInt(part[1]), Integer.parseInt(part[2])));
						this.client.setShow(this.client.getPieces().size());
						this.client.checkWin();
						this.client.getContentPane().repaint();
						break;
					case "undo":
						this.client.getPieces().remove(this.client.getPieces().size() - 1);
						this.client.incrementUndo(this.client.getPieces().size() % 2);
						this.client.setShow(this.client.getPieces().size());
						this.client.getContentPane().repaint();
						break;
					default:  // connected
						this.client.setConnecting(false);
						break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			System.out.println("server hung up");
			try {
				// cleanup
				this.out.close();
				this.in.close();
				this.sock.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}