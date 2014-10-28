/**
 * Server side programme
 * @author zhangjie
 * @date 2014-10-28
 *
 */

import java.lang.*;
import java.net.*;
import java.io.*;
import java.util.*;

/**
 * server class will listen the client's heartbeats and send it back, also,
 * server class will send the incoming heartbeat's source <ip,port> to the
 * other clients, so they know each other's <ip>, then they can start a rdp
 * request as needed.
 */
class Server {

	public ServerSocket listen_socket = null;
	// store client address
	public static ArrayList<SocketAddress> clientsList = null;
	// store socket connected to client
	public static ArrayList<Socket> socketsList = null;
	// lock
	public static String LOCK = "MUTEX";

	// hard coded to 20000
	public int PORT = 20000;

	public static void main (String [] args)
	{
		Server server = new Server();
		server.run();
	}

	// server entry
	public void run() {

		try {

			listen_socket = new ServerSocket(PORT);
			Socket conn_socket = null;
	
			System.out.println("Server is listening ...");

			while((conn_socket=listen_socket.accept())!=null) {

				if(clientsList==null)
					clientsList = new ArrayList<SocketAddress>();
				if(socketsList==null)
					socketsList = new ArrayList<Socket>();

				SocketAddress remoteAddress = 
					conn_socket.getRemoteSocketAddress();

				synchronized(LOCK) {
					// store
					clientsList.add(remoteAddress);
					socketsList.add(conn_socket);
				}

				// create a new thread used to process cilent heartbeat
				new ClientProcessThread(conn_socket).start();

				// create a new thread used to clean offline clients and send
				// clients' data to clients
				new ClientCleanThread().start();
	
				Thread.sleep(1000);
				System.out.println("Server is listening ...");
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}

/**
 * this class is used to interact with clients with the heartbeats
 */
class ClientProcessThread extends Thread {

	public Socket connSocket = null;

	public InputStream in = null;
	public OutputStream out = null;


	/**
	 *
	 */
	public ClientProcessThread(Socket connSocket) {

		this.connSocket = connSocket;

		if(connSocket!=null) {
	
			try {
				in = connSocket.getInputStream();
				out = connSocket.getOutputStream();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * generate the clients' <address,port> info, not including current
	 * client. only one thread can enter this function to avoid read/write
	 * conflict for the shared data.
	 * @clientAddr current client's SocketAddress
	 * @return return other clients' addresses info
	 */
	public String getClientsAddrInfo(SocketAddress clientAddr) {

		String info = "";

		for(SocketAddress addr : Server.clientsList) {

			if(addr == clientAddr)
				continue;

			int index = Server.clientsList.indexOf(addr);
			Socket socket = Server.socketsList.get(index);

			if(socket.isClosed())
				continue;
			else {
				String clientInfo = ((InetSocketAddress)addr).getAddress().toString();
				int port = ((InetSocketAddress)addr).getPort();
				info += clientInfo+":"+port+";";
			}
		}

		return info;
	}


	/**
	 * write the length and msg to the client
	 */
	public void write(String msg, OutputStream out) {
		
		int ct = 0;

		if(out==null || msg==null || msg.length()==0)
			return;

		int length = msg.length();
		String s = null;
		if(length<=1000)
			s = String.format("%04d", length)+new String(msg);
		else
			s = "1000"+new String(msg.substring(0,999));

		try {
			System.out.println(s);
			out.write(s.getBytes());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * thread entry
	 */
	public void run() {

		try {
			if(connSocket==null) {
	
				System.out.println("Socket is invalid");
				return;
			}
	
			while(!connSocket.isClosed()) {

				System.out.println("Thread "+Thread.currentThread().getId()+" waiting ...");
		
				byte [] buf = new byte[100];
				String heartbeat = "";

				//while(in.read(buf)>=0)
				//while(in.read(buf)>0)
				in.read(buf);
				heartbeat += new String(buf);

				// heartbeat
				String msg = "Thread "+Thread.currentThread().getId()+" ACKED, "+heartbeat;
				write(msg, out);

				// client address
				SocketAddress remoteAddress = connSocket.getRemoteSocketAddress();
				msg = "Thread "+Thread.currentThread().getId()
							+" client ip:"
							+((InetSocketAddress)remoteAddress).getAddress().toString();
				write(msg, out);

				// other clients' address info
				msg = getClientsAddrInfo(remoteAddress);
				msg = new Date().toString()+" CLIENTSADDR:"+msg;
				write(msg, out);

				Thread.sleep(1000);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}

	}
}

/**
 * this class is used to clean the offline cilents
 */
class ClientCleanThread extends Thread {
	
	// mark the client index in Server.socketsList, Server.clientsList
	public ArrayList<Integer> markedAsRemovedList = null;

	/**
	 *
	 */
	public ClientCleanThread() {
		markedAsRemovedList = new ArrayList<Integer>();
	}

	/**
	 * remove offline clients
	 * @param removeList which stores the index that the client waiting to be
	 *					 removed located at
	 */
	public void removeOfflineClients(ArrayList<Integer> removeList) {
		
		if(removeList==null || removeList.size()==0)
			return;

		for(Integer index: removeList) {

			System.out.println( "client removed: ip:"
					+((InetSocketAddress)Server.clientsList.get(index)).getAddress().toString());

			Server.clientsList.remove(index);
			Server.socketsList.remove(index);
		}

	}

	/**
	 * thread entry
	 */
	public void run() {

		try {

			markedAsRemovedList.clear();
	
			for(SocketAddress addr : Server.clientsList) {
				
				int index = Server.clientsList.indexOf(addr);
				Socket socket = Server.socketsList.get(index);
	
				if(socket.isClosed()) {
					markedAsRemovedList.add(index);
					continue;
				}
			}
	
			synchronized(Server.LOCK) {
	
				removeOfflineClients(markedAsRemovedList);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
