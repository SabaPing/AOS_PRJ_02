import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;

public class NodeMaster extends Node {
	ArrayList<String> alreadyInP2P = new ArrayList<String>();
	
	public NodeMaster(String ss){
		super(ss);
		alreadyInP2P.add("dc30.utdallas.edu");
		isP2PMember = true;
	}

	//masternode need override node’s handler inner class
	class MasterNodeHandler extends Node.Handler{
		public MasterNodeHandler(Socket s){
			super(s);
		}
		
		void masterJoinHandler(Msg m, ObjectOutputStream out) throws Exception{
			Random rdm = new Random(233);
			Msg tempp = new Msg(7, ipAddress, m.from);
			tempp.neighborPair[0] = alreadyInP2P.get(rdm.nextInt(alreadyInP2P.size()));
			tempp.neighborPair[1] = m.from;
			out.writeObject(tempp);
			out.flush();
			alreadyInP2P.add(m.from);
			if(!tempp.neighborPair[0].equals(ipAddress)){
				Socket ss = new Socket(tempp.neighborPair[0], PORT);
				ObjectOutputStream outt = new ObjectOutputStream(ss.getOutputStream());
				tempp.to = tempp.neighborPair[0];
				outt.writeObject(tempp);
				outt.flush();
			}
		}
		void masterDropHandler(Msg m, ObjectOutputStream out){
			if(neighbors.contains(m.from)){
				neighbors.remove(m.from);
				System.out.println("Node " + m.from + " was removed from neighbors.\n");
			}
			if(alreadyInP2P.contains(m.from)){
				alreadyInP2P.remove(m.from);
				System.out.println("Node " + m.from + " was removed from P2P group.\n");
			}
		}
	}
}
