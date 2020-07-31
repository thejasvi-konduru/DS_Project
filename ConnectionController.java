import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.TimerTask;
import java.io.*;

public class ConnectionController extends UnicastRemoteObject implements ChatInterface {
	private static final long serialVersionUID = 1L;
	public String clientName [] = {};
	private String clientPass [] = {};
	private String message_list [] = {};
	public ArrayList<ChatInterface> clientList;

	protected ConnectionController() throws RemoteException {
		clientList = new ArrayList<ChatInterface>();
	}

	public synchronized boolean checkClientCredintials(ChatInterface chatinterface,String clientname,String password) throws RemoteException {
		boolean chkLog = false;
			for(int i=0; i<clientName.length; i++) {
				if(clientName[i].equals(clientname) ) {
					chkLog = true;
					
					if(this.clientList.size() > i){
						this.clientList.set(i,chatinterface);
					}	
					else if (this.clientList.size() == i){
						this.clientList.add(chatinterface);
					}
					else{
						//Add all nulls, 
						while(this.clientList.size()!=i){
							this.clientList.add(null);	
						}
						this.clientList.add(chatinterface);
					}
				}
			}
		return chkLog;
	}    

	public void fifo(String source_client,String dest_client,String message, int isCS) throws RemoteException{
		System.out.println("ippo error?"+dest_client);
		for(int i=0;i<clientName.length;i++)
		{
			System.out.println("trying appa --"+clientName[i]);
			System.out.println("ClientName:"+clientName[i]);
			System.out.println("ClientName:"+clientName[i].length());
			System.out.println("dest_client:"+dest_client);
			System.out.println("dest_client:"+dest_client.length());
			System.out.println("bool"+clientName[i].equals(dest_client));

			if(clientName[i].equals(dest_client)&&clientList.get(i)!=null)
			{
				System.out.println("YA GOT, IT");
				clientList.get(i).sendMessageToClient(source_client.toUpperCase() + " : "+ message,isCS);
				break;
			}
		} 
	}
	class ArbitraryMsg extends TimerTask{
		private String source_client;
		private String message;
		private String dest_client;
		private int isCS;
		ArbitraryMsg(String source_client, String dest_client, String message, int isCS){
			this.source_client = source_client;
			this.message = message;
			this.dest_client = dest_client;
			this.isCS = isCS;
		}	
		@Override
			public void run(){
				for(int i=0;i<clientName.length;i++)
				{

					if(clientName[i].equals(this.dest_client)&&clientList.get(i)!=null)
					{
						System.out.println("YA ASYNC GOT, IT");
						try{
						clientList.get(i).sendMessageToClient(this.source_client.toUpperCase() + " : "+ this.message,this.isCS);
						break;
						}
						catch(Exception e){
							System.out.println("HMM ARBITRARY MSG FAILED");
							e.printStackTrace();
						}
					}
				} 
			}
	}
	public void non_fifo(String source_client,String dest_client,String message,int num_messages,int isCS) throws RemoteException{
		//For each message generate r between 0,5000
		Random rand = new Random();
		Integer r = rand.nextInt(10000);
		System.out.println("Should come after r seconds:"+r+":"+num_messages);

		new java.util.Timer().schedule(new ArbitraryMsg(source_client, dest_client, message, isCS),r);
	}

	public void broadcastMessage(String source_clientname , String message) throws RemoteException {
		for(int i=0; i<clientList.size(); i++) {
			if( clientName[i].equals(source_clientname))
				continue;	
			clientList.get(i).sendMessageToClient(source_clientname.toUpperCase() + " : "+ message, 0);
		}
	}
	public void fifo_broadcastMessage(String source_clientname , String message, int isCS) throws RemoteException {
		for(int i=0; i<clientList.size(); i++) {
			if( clientName[i].equals(source_clientname))
				continue;
			System.out.println("error in server kanna");
			clientList.get(i).sendMessageToClient(source_clientname.toUpperCase() + " : "+ message, isCS);
			System.out.println("end - error in server kanna");
		}
	}

	public void non_fifo_broadcastMessage(String source_clientname,String message,int num_messages, int isCS) throws RemoteException{
			for(int i=0;i<clientName.length;i++)
			{
				if( clientName[i].equals(source_clientname))
					continue;
		Random rand = new Random();
		Integer r = rand.nextInt(10000);
		System.out.println("Should come after r seconds:"+r+":"+num_messages);

		new java.util.Timer().schedule(new ArbitraryMsg(source_clientname, clientName[i], message, isCS),r);
			} 
	}
	public void sendMessageToClient(String message, int isCS) throws RemoteException{}

	public static void main(String[] arg) throws RemoteException, MalformedURLException {
		ConnectionController msgs = new ConnectionController();
		//Read the file. Add clientnames and stuff to msgs
		File allnodes = new File("list_of_nodes.txt");
		if (!allnodes.exists()){
			System.out.println("Distributed Config file doesn't exist");
		}
		try{
		BufferedReader br = new BufferedReader(new FileReader(allnodes));
		String st;
		while((st = br.readLine())!=null){
			//Add node to the fellow. 
			msgs.clientName = Arrays.copyOf(msgs.clientName,msgs.clientName.length + 1);
			msgs.clientName[msgs.clientName.length-1] = st;
		}
		
		for (String ele: msgs.clientName){
			System.out.println(ele);
		}

		Naming.rebind("RMIConnectionController", msgs);
		}
		catch(Exception e){
			System.out.println("File Not found");
		}
	} 
}
