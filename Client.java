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

	/**
	 *
	 */
	public Client(String ip, String port) {
		
		this.server_ip = ip;
		this.server_port = port;
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
		
		System.out.println(port);
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

		// 客户端创建两类线程，一类用于与服务器的第二类线程交换心跳包，并且接
		// 收服务器的第一类线程发送的客户端地址列表；
		// 另一类用于监听用户的输入请求，向用户选定的主机发送rdp请求，建立远程
		// 桌面连接
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
					System.out.println("keep reading ...");
					length -= in.read(response, 0, length);
					body += new String(response);
				}
				System.out.println("done");
				System.out.println(body);
	
				Thread.sleep(1000);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
