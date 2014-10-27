/**
 *
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

				SocketAddress remoteAddress = conn_socket.getRemoteSocketAddress();

				synchronized(LOCK) {
					// store
					clientsList.add(remoteAddress);
					socketsList.add(conn_socket);
				}

				// 服务器创建两类线程，一类用于检查socket链表中无效的socket将
				// 其移除；另一类用于与客户端交互，这里的交互只涉及到包的响应
				// ；
				// 为了简化客户端的设计，服务端每次将所有的客户端信息发送给客
				// 户端；
	
				new ClientProcessThread(conn_socket).start();
	
				System.out.println("Server is listening ... ");
				Thread.sleep(2000);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}


	/**
	 * generate the clients' <address,port> info, not including current
	 * client. only one thread can enter this function to avoid read/write
	 * conflict for the shared data.
	 * @clientAddr current client's SocketAddress
	 * @return return other clients' addresses info
	 */
	synchronized public static String getClientsAddrInfo(SocketAddress clientAddr) {

		String info = "";
		ArrayList<Integer> markedAsRemovedList = null;

		for(SocketAddress addr : clientsList) {

			if(addr == clientAddr)
				continue;

			int index = clientsList.indexOf(addr);
			Socket socket = socketsList.get(index);

			if(socket.isClosed()) {

				if(markedAsRemovedList==null) 
					markedAsRemovedList = new ArrayList<Integer>();

				markedAsRemovedList.add(index);
			}
			else {
				String clientInfo = ((InetSocketAddress)addr).getAddress().toString();
				int port = ((InetSocketAddress)addr).getPort();
				info += clientInfo+":"+port+";";
			}
		}

		// remove offline clients
		removeOfflineClients(markedAsRemovedList);

		return info;
	}

	/**
	 * remove offline clients
	 * @param removeList which stores the index that the client waiting to be
	 *					 removed located at
	 */
	synchronized public static void removeOfflineClients(ArrayList<Integer> removeList) {
		
		if(removeList==null || removeList.size()==0)
			return;

		for(Integer index: removeList) {

			System.out.println( "client removed: ip:"+((InetSocketAddress)clientsList.get(index)).getAddress().toString());

			clientsList.remove(index);
			socketsList.remove(index);
		}

	}

}

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
	 *
	 */
	public void run() {

		try {
			if(connSocket==null) {
	
				System.out.println("Socket is invalid");
				return;
			}
	
			while(!connSocket.isClosed()) {
		
				byte [] buf = new byte[100];
				in.read(buf);
	
				synchronized(Server.LOCK) {
					SocketAddress remoteAddress = connSocket.getRemoteSocketAddress();
					out.write(("client ip:"
								+((InetSocketAddress)remoteAddress).getAddress().toString())
								.getBytes() );
					out.write( ("client echo:"+buf).getBytes() ) ;
			
					String info = Server.getClientsAddrInfo(remoteAddress);
					out.write(("START:"+buf).getBytes());
				}
		
				Thread.sleep(3000);
				System.out.println("...");
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}

	}
}
