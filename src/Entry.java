import java.util.Scanner;

public class Entry {
	public static void main(String[] args){
		Scanner sc = new Scanner(System.in);
		System.out.println("master node? and node IP\n");
		String[] temp = sc.nextLine().split(" ");
		if(temp[0].equals("1")) 
			new NodeMaster(temp[1]).initiator();
		else new Node(temp[1]).initiator();

		
	}
}
