/*
 * @author: Sungil Ahn
 */

// checks asynchronously whether we're actually connected to a player, so the server can continue listening
// this has to be done to prevent players from being matched up to bots/crawlers
public class Bouncer {
	private static final double gracePeriod = 1.0;

	public Bouncer(Server server, ServerCommunicator comm) throws InterruptedException {
		boolean verified = false;
		double startTime = System.nanoTime();

		// give a little time to check the validity of the person connected
		while ((System.nanoTime() - startTime) / 1_000_000_000 < gracePeriod) {
			if (comm.getFirstMsgStatus() == true) {
				server.addToList(comm);
				verified = true;
				break;
			}
			// check every 0.1 s
			Thread.sleep(100);
		}
		
		// destroy the intruder
		if (!verified) {
			comm = null;
			System.out.println("Intruder detected!");
			server.getLog().warn("Intruder detected!");
		}

		// now kill itself
		server.killBouncer(this);
	}
}