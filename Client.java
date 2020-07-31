import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.net.MalformedURLException;
import java.util.*;
import java.lang.*;
import java.io.*;

public class Client extends UnicastRemoteObject implements ChatInterface , Runnable {
	private static final long serialVersionUID = 1L;
	private ChatInterface server;
	private String ClientName;
	boolean chkExit = true;
	boolean chkLog = false;
	private boolean isExecutingCS = false;
	public String clientNames [] = {};
	public ArrayList<ChatInterface> clientList = new ArrayList();
	public Integer num_nodes = 3;

	private HashMap<String,String> channel_type = new HashMap<String,String>();
	
//	public String[] getClientNames(){ return this.clientNames;};

	protected Client(ChatInterface chatinterface,String clientname,String password) throws RemoteException {
	this.server = chatinterface;
	this.ClientName = clientname;
	chkLog = server.checkClientCredintials(this,clientname,password);
	//Add names to clientNames and channel_type
	}

	private PriorityQueue<Integer> my_requests = new PriorityQueue<Integer>();
	private HashMap<String,ArrayList<Integer>> should_send_replies = new HashMap<String,ArrayList<Integer>>();

	private HashMap<Integer, Integer> my_sent_requests = new HashMap<Integer,Integer>();

	public void sendMessageToClient(String message, int isCS) throws RemoteException {
	if(isCS == 0){
		System.out.println(message); 
	}
	else if(isCS==1){ //I got a request
		//Split message to timestamp and client 
		Integer least = my_requests.peek();	
		String[] arrOfStr = message.split(" : ",3);
		String requesting_party= arrOfStr[0].toLowerCase();
		Integer flag = 0;
		//System.out.println("Hmm1, arrOfStr :"+requesting_party+arrOfStr[1]+", Least = "+least+"myreuests size :"+ my_requests.size());
		Integer timestamp = Integer.parseInt(arrOfStr[1]);
		//System.out.println("Hmm2, arrOfStr :"+arrOfStr[0]+arrOfStr[1]+", Least = "+least+"myreuests size :"+ my_requests.size());
		
		if(timestamp == least){
		//Need to have some PID stuff, 
		//System.out.println("Need to have PID based ordering");
			//Compare Client Name and requesting party
			if(ClientName.compareTo(requesting_party) < 0)
				flag = 1;
			else if(ClientName.compareTo(requesting_party) > 0)
				flag = -1;
		}

		if(isExecutingCS == true){
		if( should_send_replies.containsKey(requesting_party)){
		ArrayList<Integer> value = should_send_replies.get(requesting_party);
		value.add(timestamp);
		should_send_replies.replace(requesting_party,value);
		}
		else{
		ArrayList<Integer> value = new ArrayList<Integer>();
		value.add(timestamp);
		should_send_replies.put(requesting_party,value);
		}
		}
		else if(flag ==-1 || my_requests.size()==0 || timestamp < least){
		//Send Reply
		String reply_msg = arrOfStr[1];
		//System.out.println("Hmm, arrOfStr :"+arrOfStr[0]+arrOfStr[1]+", Least = "+least);
		server.fifo(ClientName,requesting_party,reply_msg,2);
			
		//System.out.println("Hmm, arrOfStr :"+arrOfStr[0]+arrOfStr[1]+", Least = "+least);
		}
		else if(flag == 1 || timestamp > least){
		//Nope won't send reply
		//Create a need to send replies arr
		if( should_send_replies.containsKey(requesting_party)){
		ArrayList<Integer> value = should_send_replies.get(requesting_party);
		value.add(timestamp);
		should_send_replies.replace(requesting_party,value);
		}
		else{
		ArrayList<Integer> value = new ArrayList<Integer>();
		value.add(timestamp);
		should_send_replies.put(requesting_party,value);

		
		}
			
		}

	}
	else if(isCS==2){//I got a reply - should also specify for which timestamp's request I am sending this shit. 
		//Store number of requests sent for each timestamp;
		//
		String[] arrOfStr = message.split(" : ",3);
		Integer timestamp = Integer.parseInt(arrOfStr[1]);
		System.out.println(ClientName.toUpperCase()+": Received a Reply from "+arrOfStr[0]);
		my_sent_requests.replace(timestamp,my_sent_requests.get(timestamp) - 1);
		if(my_sent_requests.get(timestamp) == 0){
		//Print that I am executing CS
		isExecutingCS = true;
		System.out.println("NODE "+ClientName.toUpperCase()+" is executing CS currently");
		//Do sleep() shit
		try{
		Thread.sleep(15000);
		}
		catch(Exception e){
			e.printStackTrace();
		}
		//After that send replies to others
		//For all elements in should_send-replies, send replies - I did not send replies since their timestamp was greater
			for (String key: should_send_replies.keySet()){
				//For all values in should_send_replies.get(key)
				ArrayList<Integer> value = should_send_replies.get(key);
				String requesting_party= key.toLowerCase();
				for(Integer ts : value){
					String reply_msg = ts.toString();
					//Send replies
					server.fifo(ClientName,requesting_party,reply_msg,2);
				}
			}
			//Make should_send_replies empty
			should_send_replies = new HashMap<String,ArrayList<Integer>>();
			my_requests.poll();
			isExecutingCS = false;
		System.out.println("NODE "+ClientName.toUpperCase()+" has finished CS EXECUTION");
		}
	}
	}

	public void broadcastMessage(String clientname,String message) throws RemoteException {}
	public void fifo_broadcastMessage(String source_clientname,String message,int isCS) throws RemoteException {}
	public void non_fifo_broadcastMessage(String source_clientname,String message,int num_message, int isCS) throws RemoteException {}
	public void fifo(String source_client,String dest_client,String message,int isCS) throws RemoteException{}

	public void non_fifo(String source_client,String dest_client,String message,int num_messages,int isCS) throws RemoteException{}

	public boolean checkClientCredintials(ChatInterface chatinterface ,String clientname,String password) throws RemoteException {
	return true;
	}
	public void run() {
	if(chkLog) 
	{
		System.out.println("The Application is up");
		System.out.println("NOTE : Type LOGOUT to Exit From The Service");
		System.out.println("Type CRITICAL SECTION to request for critical section");
		System.out.println("Type BROADCAST to broadcast the message");
		System.out.println("Type ONE TO ONE to chat with a particular client\n");
		System.out.println("Now Your Online To Chat\n");
		Scanner scanner = new Scanner(System.in);
		String message;
		String broadcast_message;
		String dest_client;
		String one_to_one_message;
		String source_client;
		String channel;
		int num_messages;
		int message_no;

		while(chkExit) 
		{
		message = scanner.nextLine();
		if(message.equals("LOGOUT")) {
			chkExit = false;
		}
		else {
			try {
			if(message.equals("CRITICAL SECTION")){
				//System.out.println("Enter the type of channel(FIFO or NON_FIFO)...\n");
				source_client=ClientName;
				num_messages=1;
				while(num_messages>0){
				System.out.println("Enter the timestamp..\n");
				broadcast_message=scanner.nextLine();
				Integer timestamp = Integer.parseInt(broadcast_message);
				my_sent_requests.put(timestamp, clientNames.length-1);
				my_requests.add(timestamp);	
				for (int i=0;i<clientNames.length;i++){
					if(ClientName.equals(clientNames[i]))
						continue;
					System.out.println(ClientName.toUpperCase()+": Sent a Request to "+clientNames[i].toUpperCase());
					channel=channel_type.get(clientNames[i]);
					if(channel.equals("FIFO"))
					{
							server.fifo(ClientName,clientNames[i],broadcast_message, 1);
					}
					else if(channel.equals("NON_FIFO"))
					{
						message_no=num_messages;
							server.non_fifo(ClientName ,clientNames[i], broadcast_message, message_no,1);
					}
				}
				num_messages=num_messages-1;
				}
			}
			else if (message.equals("BROADCAST")){
				//System.out.println("Enter the type of channel(FIFO or NON_FIFO)...\n");
				source_client=ClientName;
				System.out.println("Enter the number of messages you want to send...\n");
				num_messages=Integer.parseInt(scanner.nextLine());
				while(num_messages>0){
				System.out.println("Enter the message..\n");
				broadcast_message=scanner.nextLine();
				
				for (int i=0;i<clientNames.length;i++){
					if(ClientName.equals(clientNames[i]))
						continue;
					System.out.println("clientNames[i]"+clientNames[i]);
					channel=channel_type.get(clientNames[i]);
					System.out.println("CHANELLE ="+channel);
					if(channel.equals("FIFO"))
					{
							server.fifo(ClientName,clientNames[i],broadcast_message, 0);
					}
					else if(channel.equals("NON_FIFO"))
					{
						message_no=num_messages;
							server.non_fifo(ClientName ,clientNames[i], broadcast_message, message_no,0);
					}
				}
				num_messages=num_messages-1;
				}
			}
			else if(message.equals("ONE TO ONE"))
			{
				//System.out.println("Enter the type of channel(FIFO or NON_FIFO)...\n");
				System.out.println("Enter the client you want to send to...\n");
				dest_client=scanner.nextLine();
				source_client=ClientName;
				channel=channel_type.get(dest_client);
				if(channel.equals("FIFO"))
				{
				System.out.println("Enter the number of messages you want to send...\n");
				num_messages=Integer.parseInt(scanner.nextLine());
				while(num_messages>0)
				{
					System.out.println("Enter the message..\n");
					one_to_one_message=scanner.nextLine();
					server.fifo(source_client,dest_client,one_to_one_message,0);
					num_messages=num_messages-1;
				}
				}
				else if(channel.equals("NON_FIFO"))
				{
				System.out.println("Enter the number of messages you want to send...\n");
				num_messages=Integer.parseInt(scanner.nextLine());
				message_no=num_messages;
				System.out.println(num_messages);
				while(num_messages>0)
				{
					System.out.println("Enter the message..\n");
					one_to_one_message=scanner.nextLine();
					server.non_fifo(source_client,dest_client,one_to_one_message,message_no,0);
					num_messages=num_messages-1;
				}
				}
			}
			}
			catch(RemoteException e) {
			e.printStackTrace();
			}
		}  
		} 
		System.out.println("\nSuccessfully Logout From The RMI Chat Program\nThank You Using...");
	}
	else 
	{
		System.out.println("\nClient Name or  Incorrect...");
		System.exit(1);
	}  
	}

	public static void main(String[] args) throws MalformedURLException,RemoteException,NotBoundException {
	Scanner scanner = new Scanner(System.in);
	String clientName = "";
	String clientPassword = "";

	System.out.print("Enter The Name : ");
	clientName = scanner.nextLine();
//	System.out.print("Enter The Password : ");
//	clientPassword = scanner.nextLine();

	ChatInterface chatinterface = (ChatInterface)Naming.lookup("rmi://localhost/RMIConnectionController");
	
//	for(String ele : chatinterface.clientName){
//		System.out.println(ele);
//	}
	Client msgs = new Client(chatinterface, clientName, clientPassword);
	File allnodes = new File("list_of_nodes.txt");
	if (!allnodes.exists()){
		System.out.println("Distributed Config file doesn't exist");
	}
	try{
		BufferedReader br = new BufferedReader(new FileReader(allnodes));
		String st;
		while((st = br.readLine())!=null){
			//Add node to the fellow. 
			msgs.clientNames = Arrays.copyOf(msgs.clientNames,msgs.clientNames.length + 1);
			msgs.clientNames[msgs.clientNames.length-1] = st;
		}
	
		System.out.println("Please Specify if you want FIFO/NON_FIFO for each of the following node channels");	
		for (String ele: msgs.clientNames){
			if(ele.equals(clientName))
				continue;
			System.out.println(ele.toUpperCase()+" : ");
			String channel=scanner.nextLine();
			
			msgs.channel_type.put(ele,channel);	
		}
	
		System.out.println("THE CONFIGURATION IS SET");	
		}
		catch(Exception e){
			System.out.println("File Not found");
		}
	new Thread(msgs).start();
	}

}
