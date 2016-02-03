import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import static java.nio.file.StandardOpenOption.*;

public class Node {
	static final int PORT = 23333;
	static String ipAddress;
	ArrayList<String> neighbors = new ArrayList<String>();
	static boolean isP2PMember = false;
	// Fi -- need further work

	public Node(String s) {
		ipAddress = s;
	}

	class Listener extends Thread {
		public void run() {
			ExecutorService executor = Executors.newFixedThreadPool(20);
			try {
				ServerSocket listener = new ServerSocket(PORT);
				while (true) {
					Future<String> future = executor.submit(new Handler(listener.accept()));
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {

			}
		}
	}

	class Handler implements Callable<String> {
		Socket socket;

		public Handler(Socket s) {
			socket = s;
		}

		public String call() throws Exception {
			try {
				ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
				Msg temp = (Msg) in.readObject();
				while (!temp.close) {
					if (temp.isJoinRequestToMasterReply)
						replyFromMasterHandler(temp, out);
					if (temp.isJoinRequestToMaster)
						masterJoinHandler(temp, out);
					if (temp.isDropRequestToMaster)
						masterDropHandler(temp, out);
					if (temp.isDropRequestToNeighbor)
						neighborDropHandler(temp, out);
					if (temp.isSearchRequest)
						SearchRequestHandler(temp, out);
					temp = (Msg) in.readObject();
				}
				return "Msg Handler was closed.\n";
			} catch (Exception e) {
				e.printStackTrace();
				return "Msg handler failed.\n";
			}

		}

		void SearchRequestHandler(Msg m, ObjectOutputStream out) {
			if (m.hopCount < 1) {
				System.out.println("Unvalid hop count. Denied.\n");
				return;
			}
			FileInputStream fout;
			try {
				Msg tmpm;
				fout = new FileInputStream("./test/index");
				ObjectInputStream in = new ObjectInputStream(fout);
				FileObj tmpO = (FileObj) in.readObject();
				boolean flag = false;
				while (!tmpO.EOF) {
					for (String keyword : m.searchKeywords) {
						if (keyword.equals(tmpO.name) || tmpO.keyWords.contains(keyword)) {
							flag = true;
							tmpm = new Msg(2, ipAddress, "", tmpO.name, tmpO.dir, keyword);

							Socket sss = new Socket(m.hopTrace.get(0), PORT);
							ObjectOutputStream oout = new ObjectOutputStream(sss.getOutputStream());
							oout.writeObject(tmpm);
							oout.flush();
						}
					}
					tmpO = (FileObj) in.readObject();
				}
				if (!flag) {
					m.hopTrace.add(ipAddress);
					m.from = ipAddress;
					for (String tmpIP : neighbors) {
						if (!m.hopTrace.contains(tmpIP)) {
							m.to = tmpIP;
							m.hopTrace.add(ipAddress);
							m.hopCount--;
							Socket ssa = new Socket(tmpIP, PORT);
							ObjectOutputStream ouut = new ObjectOutputStream(ssa.getOutputStream());
							ouut.writeObject(m);
							ouut.flush();
						}
					}
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		void masterDropHandler(Msg m, ObjectOutputStream out) {
		}

		void neighborDropHandler(Msg m, ObjectOutputStream out) {
			if (neighbors.contains(m.from)) {
				neighbors.remove(m);
				System.out.println("Node " + m.from + "was removed from neighbors.\n");
			}
		}

		void replyFromMasterHandler(Msg m, ObjectOutputStream out) {
			for (int i = 0; i < 2; i++) {
				if (!m.neighborPair[i].equals(ipAddress)) {
					neighbors.add(m.neighborPair[i]);
					isP2PMember = true;
				}
			}
			System.out.println("Node " + ipAddress + "joined P2P group.\n");
		}

		void masterJoinHandler(Msg m, ObjectOutputStream out) throws Exception {
		}
	}

	
	void buildIndex(Scanner sc){
		File fff = new File("./test/index");
		try {
			fff.createNewFile();
			ObjectOutputStream out2 = new ObjectOutputStream(new FileOutputStream(fff));
			System.out.println("Next file? y/n \n");
			FileObj tempf = new FileObj();
			System.out.println("Name: ");
			String s = sc.nextLine();
			while(s.equals("quit")){
				System.out.println("Name: ");
				tempf.name = s;
				System.out.println("\nKeywords(split by ,): ");
				tempf.keyWords.addAll(Arrays.asList(sc.nextLine().split(",")));
				out2.writeObject(tempf);
				System.out.println("Name: ");
				s = sc.nextLine();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	
	
	
	public void initiator() {
		new Listener().start();
		Scanner sc = new Scanner(System.in);
		System.out.println("Server started listening.\n" + "Input 1 to jion P2P group.\n"
				+ "Input 2 to drop from P2P group.\n" + "Input 3 to search file.\n" + "Input 0 to quit.\n\n");
		int temp = sc.nextInt();
		while (temp != 0) {
			if (temp == 1)
				joinProcess();
			if (temp == 2)
				dropProcess();
			if (temp == 3)
				searchProcess();
			System.out.println("Server started listening.\n" + "Input 1 to jion P2P group.\n"
					+ "Input 2 to drop from P2P group.\n" + "Input 3 to search file.\n" + "Input 0 to quit.\n\n");
			temp = sc.nextInt();
		}
	}

	void joinProcess() {
		try {
			if (isP2PMember) {
				System.out.println("This node is already in P2P group.\n");
				return;
			}
			Socket socket = new Socket("dc30.utdallas.edu", PORT);
			ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

			Msg tempMsg = new Msg(6, ipAddress, "dc30.utdallas.edu");
			out.writeObject(tempMsg);
			out.flush();
			System.out.println("send joining p2p request to master node.\n");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void dropProcess() {
		try {
			if (!isP2PMember) {
				System.out.println("This node is not in P2P group.\n");
				return;
			}
			// request to server
			Socket socket = new Socket("dc30.utdallas.edu", PORT);
			ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
			Msg tempMsg = new Msg(4, ipAddress, "dc30.utdallas.edu");
			out.writeObject(tempMsg);
			out.flush();
			// request to its neighbor
			while (neighbors.size() > 0) {
				String tempIP = neighbors.remove(0);
				Socket sss = new Socket(tempIP, PORT);
				ObjectOutputStream outtt = new ObjectOutputStream(sss.getOutputStream());
				Msg tempM = new Msg(5, ipAddress, tempIP);
				outtt.flush();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void searchProcess() {
		System.out.println("Pls enter searching key words.\n:q to end.");
		Scanner sc = new Scanner(System.in);
		Msg tmpM = new Msg(1, ipAddress);
		tmpM.hopTrace.add(ipAddress);
		String tmpS = sc.nextLine();
		while (!tmpS.equals(":q")) {
			tmpM.searchKeywords.add(tmpS);
			tmpS = sc.nextLine();
		}
		System.out.println("Pls enter hop count.\n");
		tmpM.hopCount = sc.nextInt();

		FileInputStream fout;
		try {
			fout = new FileInputStream("./test/index");
			ObjectInputStream in = new ObjectInputStream(fout);
			FileObj tmpO = (FileObj) in.readObject();
			boolean flag = false;
			while (!tmpO.EOF) {
				for (String keyword : tmpM.searchKeywords) {
					if (keyword.equals(tmpO.name) || tmpO.keyWords.contains(keyword)) {
						flag = true;
						System.out.println("File is on the local disk.\n");
					}
				}
				tmpO = (FileObj) in.readObject();
			}
			if (!flag) {
				for (String neighbor : neighbors) {
					tmpM.to = neighbor;

					Socket so = new Socket(neighbor, PORT);
					ObjectOutputStream out = new ObjectOutputStream(so.getOutputStream());
					tmpM.hopCount--;
					out.writeObject(tmpM);
					out.flush();
					System.out.println("Sent searching request to neighbors.\n");

				}
			}
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (ClassNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}
}

class Msg implements Serializable {
	String from;
	String to;
	boolean isSearchRequest = false;
	boolean isSearchSuccessReply = false;
	boolean isDropRequestToMaster = false;
	boolean isDropRequestToNeighbor = false;
	boolean isJoinRequestToMaster = false;
	boolean isJoinRequestToMasterReply = false;
	boolean close = false;

	public Msg(int flag, String from, String to) {
		if (flag == 1)
			isSearchRequest = true;
		if (flag == 2)
			isSearchSuccessReply = true;
		if (flag == 4)
			isDropRequestToMaster = true;
		if (flag == 5)
			isDropRequestToNeighbor = true;
		if (flag == 6)
			isJoinRequestToMaster = true;
		if (flag == 7)
			isJoinRequestToMasterReply = true;
		if (flag == 0)
			close = true;
		this.from = from;
		this.to = to;
	}

	public Msg(int flag, String from) {
		if (flag == 1)
			isSearchRequest = true;
		if (flag == 2)
			isSearchSuccessReply = true;
		if (flag == 4)
			isDropRequestToMaster = true;
		if (flag == 5)
			isDropRequestToNeighbor = true;
		if (flag == 6)
			isJoinRequestToMaster = true;
		if (flag == 7)
			isJoinRequestToMasterReply = true;
		if (flag == 0)
			close = true;
		this.from = from;
	}

	public Msg(int flag, String from, String to, String n, String d, String s) {
		if (flag == 1)
			isSearchRequest = true;
		if (flag == 2)
			isSearchSuccessReply = true;
		if (flag == 4)
			isDropRequestToMaster = true;
		if (flag == 5)
			isDropRequestToNeighbor = true;
		if (flag == 6)
			isJoinRequestToMaster = true;
		if (flag == 7)
			isJoinRequestToMasterReply = true;
		if (flag == 0)
			close = true;
		this.from = from;
		this.to = to;
		found = new FileFound(n, d, s);
	}

	String[] neighborPair = new String[2];

	int hopCount = 1;
	ArrayList<String> hopTrace = new ArrayList<String>();
	ArrayList<String> searchKeywords = new ArrayList<String>();

	FileFound found;
}

class FileFound {
	String name;
	String dir;
	String searchKeyword;

	public FileFound(String n, String d, String s) {
		name = n;
		dir = d;
		searchKeyword = s;
	}
}

class FileObj implements Serializable {
	String name;
	String dir;
	boolean EOF = false;
	ArrayList<String> keyWords = new ArrayList<String>();
}
