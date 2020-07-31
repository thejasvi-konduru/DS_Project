import java.rmi.RemoteException;
import java.util.*;

public interface ChatInterface extends java.rmi.Remote {
	String clientName [] = {};
	ArrayList<ChatInterface> clientList = new ArrayList();
//	String[] getClientNames();	
    boolean checkClientCredintials(ChatInterface ci,String name, String pass) throws RemoteException;
    void broadcastMessage(String name,String message) throws RemoteException;
    void fifo(String source_client,String dest_client,String message, int isCS) throws RemoteException;
    void non_fifo(String source_client,String dest_client,String message,int num_messages, int isCS) throws RemoteException;
    void fifo_broadcastMessage(String source_clientName,String message, int isCS) throws RemoteException;
    void non_fifo_broadcastMessage(String source_clientName,String message,int num_messages, int isCS) throws RemoteException;
    void sendMessageToClient(String message, int isCS) throws RemoteException;
}
