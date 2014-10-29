/**
 * client which will send heartbeat to the server, this client can receive
 * server's response and notification of other client's info, also, it can
 * start the rdp connection request to other clients.
 * @author zhangjie	
 * @date   2014/10/27
 */

import java.lang.*;
import java.net.*;
import java.io.*;
import java.util.*;

class Client {

	public Socket conn_socket = null;

	public String server_ip = null;
	public String server_port = null;

	// indicate os on which this program runs
	public static String client_os = null;

	// store other clients' ip address
	public static ArrayList<String> neighboursList = null;

	/**
	 *
	 */
	public Client(String ip, String port) {
		
		this.server_ip = ip;
		this.server_port = port;

		Properties props = System.getProperties();
		String osname = props.getProperty("os.name").toLowerCase();
		if(osname.contains("windows"))
			this.client_os = "win";
		else if(osname.contains("linux"))
			this.client_os = "linux";

		this.neighboursList = new ArrayList<String>();
	}

	/**
	 *
	 */
	public void run() {

		// if ip/port is not valid, exit the program
		if(!isValidIpAddress(server_ip) || !isValidPort(server_port)) {
			usage();
			return;
		}

		// ready to work
		try {
			conn_socket = new Socket(server_ip, Integer.parseInt(server_port));
	
			// send heartbeat to the server
			new HeartbeatTimerTask(conn_socket).start();
			// display the clients' list and handle the user selection
			new RDP2ClientThread().start();	

		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * echo the usage of current program
	 */
	public static void usage() {
		System.out.println("Usage:");
		System.out.println("	java Client <ip> <port>");
	}

	/**
	 * check whether the parameter is a valid ip address
	 * @param ip ip input
	 * @return return true if ip is valid, otherwise false
	 */
	public static boolean isValidIpAddress(String ip) {

		if(ip==null || ip.length()==0)
			return false;

		String [] dottedNums = ip.split("\\.");

		if(dottedNums.length==4) {
			for(String num_s : dottedNums) {
				try {
					int num_i = Integer.parseInt(num_s);
					if(num_i>=0 && num_i<=255)
						continue;
					else
						return false;
				}
				catch (NumberFormatException e) {
					return false;
				}
			}
		}
		else
			return false;

		return true;
	}

	/**
	 * check whether the parameter is a valid port on the host
	 * @param port port input
	 * @return return true if port is valid, otherwise false
	 */
	public static boolean isValidPort(String port) {
		
		if(port==null || port.length()==0)
			return false;

		try {
			int port_i = Integer.parseInt(port);
			if(port_i>=1 && port_i<=65535)
				return true;
			else
				return false;
		}
		catch (NumberFormatException e) {
			return false;
		}
	}

	public static void main (String [] args)
	{
		// invalid parameters 
		if(args.length!=2) {
			usage();
			return;
		}

		Client client = new Client(args[0], args[1]);
		client.run();
	}

}

/**
 * task to send heartbeat packet to the server
 */
class HeartbeatTimerTask extends Thread {
	
	public Socket connSocket = null;
	public OutputStream out = null;
	public InputStream in = null;

	public HeartbeatTimerTask(Socket connSocket) {

		this.connSocket = connSocket;

		if(connSocket!=null) {
			try {
				this.out = connSocket.getOutputStream();
				this.in = connSocket.getInputStream();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	public void run () {

		if(connSocket==null)
			return;

		try {
			while(!connSocket.isClosed()) {
	
				byte [] heartbeat = (new Date().toString()+"  hello, i am client").getBytes();
				byte [] response = new byte[1000];
				out.write(heartbeat);

				// 4-bytes number
				in.read(response, 0, 4);
				int length = 
					Integer.parseInt(new String(response).trim());
					
				String body = "";
				while(length>0) {
					//System.out.println("keep reading ...");
					length -= in.read(response, 0, length);
					body += new String(response);
				}
				//System.out.println("done");
				body = body.trim();
				//System.out.println(body);

				// parse the body part to extract the other clients' addresses
				// into neighboursList
				if(body.contains("CLIENTSADDR:")) {
					String body_tmp = body.substring(body.indexOf("/"));
					String [] ip_tmp = body_tmp.split(";");
				
					Client.neighboursList.clear();
					for(String client : ip_tmp)	{
						Client.neighboursList.add(client.split(":")[0].substring(1));
						//System.out.print(Client.neighboursList.get(Client.neighboursList.size()-1)+" ");
					}
					//System.out.println("");
				}

				Thread.sleep(1000);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}

/**
 *
 */
class RDP2ClientThread extends Thread {
	
	/**
	 *
	 */
	public RDP2ClientThread() {
	}

	/**
	 *
	 */
	public void run() {

		try {

			// reset current shell content
			Runtime runtime = Runtime.getRuntime();
	
			String cmd = null;
	
			if(Client.client_os.equals("win"))
				cmd = "cls";
			else if(Client.client_os.equals("linux"))
				cmd = "clear";
			else
				cmd = "clear";

			// read user input
			//Scanner scanner = new Scanner(System.in);

			while(true) {
				Process process = runtime.exec(cmd);
	
				if(process.waitFor()!=0)
					System.out.println("sub-process to clear the console exited abnormally");
	
				// display the clients list
				int index = 0;
				System.out.println(String.format("%5s %20s", "client", "ipaddress"));
				for(String client : Client.neighboursList) {
					System.out.println(String.format("%5d %20s", index++, client));
				}

				// scanner
				/*
				scanner.reset();
				if(scanner.hasNextInt()) {
					int sel = scanner.nextInt();
					String rdpRequest = "krdc rdp://"+Client.neighboursList.get(sel);
					Process p = runtime.exec(rdpRequest);
					// p.waitFor
				}
				*/

				Thread.currentThread().sleep(2000);

			}

		}
		catch (Exception e) {
			e.printStackTrace();
		}
			
				
	}
}
