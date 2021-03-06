package org.sar.ppi;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import org.junit.Assume;
import org.junit.Test;
import org.sar.ppi.dispatch.MessageHandler;
import org.sar.ppi.events.Message;
import org.sar.ppi.mpi.MpiRunner;
import org.sar.ppi.peersim.PeerSimRunner;

public class MutexTest extends RedirectedTest {
	Integer father = 0;
	Integer next = null;
	boolean token = false;
	boolean requesting = false;
	int nbCS = 0;

	// terminaison
	int nbEnd = 0;

	@Override
	public void init(String[] args) {
		if (infra.getId() == father) {
			father = null;
			token = true;
		}
	}

	public void doSomething() {
		try {
			infra.wait(() -> requesting == false);
			request();
			infra.wait(() -> token == true);
			cs();
			release();

			// terminaison
			if (nbCS == 2) {
				System.out.printf("%d Had 2 critical sections\n", infra.getId());
				infra.send(new End(infra.getId(), 0));
			}
		} catch (InterruptedException e) {
			System.out.printf("%d Was interrupted while waiting\n", infra.getId());
		}
	}

	public void request() {
		requesting = true;
		if (father != null) {
			infra.send(new Request(infra.getId(), father));
			father = null;
		}
		System.out.printf("%d Requested critical section\n", infra.getId());
	}

	public void cs() {
		System.out.printf("%d Entered critical section\n", infra.getId());
		nbCS++;
	}

	public void release() {
		requesting = false;
		System.out.printf("%d Left critical section\n", infra.getId());
		if (next != null) {
			System.out.printf("%d Send token to %d\n", infra.getId(), next);
			infra.send(new Token(infra.getId(), next));
			token = false;
			next = null;
		}
	}

	@MessageHandler
	public void processRequest(Request request) {
		int host = infra.getId();
		System.out.printf("%d Received request from %d\n", host, request.getIdsrc());
		if (father == null) {
			if (requesting == true) {
				System.out.printf("%d Set %d as next\n", host, request.getIdsrc());
				next = request.getIdsrc();
			} else {
				System.out.printf("%d Send token to %d\n", host, request.getIdsrc());
				infra.send(new Token(host, request.getIdsrc()));
				token = false;
			}
		} else {
			System.out.printf("%d Pass request to %d\n", host, father);
			infra.send(new Request(request.getIdsrc(), father));
		}
		father = request.getIdsrc();
	}

	@MessageHandler
	public void processToken(Token t) {
		int host = infra.getId();
		System.out.printf("%d Received token from %d\n", host, t.getIdsrc());
		token = true;
	}

	public static class Request extends Message {
		private static final long serialVersionUID = 1L;

		public Request(int idsrc, int iddest) {
			super(idsrc, iddest);
		}
	}

	public static class Token extends Message {
		private static final long serialVersionUID = 1L;

		public Token(int idsrc, int iddest) {
			super(idsrc, iddest);
		}
	}

	// terminaison
	@MessageHandler
	public void processEnd(End end) {
		if (infra.getId() == 0) {
			nbEnd++;
			if (nbEnd == 5) {
				for (int i = 1; i < 6; i++) {
					infra.send(new End(0, i));
				}
				System.out.printf("%d Called exit\n", infra.getId());
				infra.exit();
			}
		} else {
			System.out.printf("%d Called exit\n", infra.getId());
			infra.exit();
		}
	}

	public static class End extends Message {
		private static final long serialVersionUID = 1L;

		public End(int idsrc, int iddest) {
			super(idsrc, iddest);
		}
	}

	@Test
	public void mpi() {
		Assume.assumeTrue(EnvUtils.mpirunExist());
		Ppi.main(
			this.getClass(),
			new MpiRunner(),
			new String[0],
			6,
			new File("src/test/resources/MutexTest.json")
		);
		String token;
		for (int i = 0; i < 5; i++) {
			token = getScanner().findWithinHorizon("[1-5] Requested critical section", 0);
			assertNotNull(token);
		}
		for (int i = 0; i < 5; i++) {
			token = getScanner().findWithinHorizon("[1-5] Had 2 critical sections", 0);
			assertNotNull(token);
		}
	}

	@Test
	public void peersim() {
		Ppi.main(
			this.getClass(),
			new PeerSimRunner(),
			new String[0],
			6,
			new File("src/test/resources/MutexTest.json")
		);
		System.out.print(outContent.toString());
		String token;
		for (int j = 0; j < 2; j++) {
			for (int i = 0; i < 5; i++) {
				token = getScanner().findWithinHorizon("[1-5] Requested critical section", 0);
				assertNotNull(token);
			}
			for (int i = 0; i < 5; i++) {
				token = getScanner().findWithinHorizon("[1-5] Received token", 0);
				assertNotNull(token);
				int node = Integer.valueOf(token.substring(0, 1));
				token = getScanner().findWithinHorizon(node + " Entered critical section", 0);
				assertNotNull(token);
				token = getScanner().findWithinHorizon(node + " Left critical section", 0);
				assertNotNull(token);
			}
			for (int i = 0; i < 5; i++) {
				token = getScanner().findWithinHorizon("[1-5] Requested critical section", 0);
				assertNotNull(token);
			}
			for (int i = 0; i < 5; i++) {
				token = getScanner().findWithinHorizon("[1-5] Received token", 0);
				assertNotNull(token);
				int node = Integer.valueOf(token.substring(0, 1));
				token = getScanner().findWithinHorizon(node + " Entered critical section", 0);
				assertNotNull(token);
				token = getScanner().findWithinHorizon(node + " Left critical section", 0);
				assertNotNull(token);
				token = getScanner().findWithinHorizon(node + " Had 2 critical sections", 0);
				assertNotNull(token);
			}
		}
	}
}
