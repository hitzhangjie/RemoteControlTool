import java.lang.*;
import java.io.*;
import java.util.*;

class T {

	public static void main (String [] args) {
		try {
			String [] cmd_ssh = {"/bin/bash", "-c", "ssh zhangjie@localhost &>1"};
			//String cmd = "remmina";
			Runtime runtime = Runtime.getRuntime();
			Process p = runtime.exec(cmd_ssh);

			// in_p_stdout is conected to the normal output of sub-process
			InputStream in_p_stdout = p.getInputStream();
			// in_p_stderr is connected to the error output of sub-process
			InputStream in_p_stderr = p.getErrorStream();
			// out_p_stdin is conected to the normal input of sub-process
			OutputStream out_p_stdin = p.getOutputStream();

			try {
				Thread.currentThread().sleep(1000);
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}

			// handle the subprocess input
			new StreamHandlerThread(null, null, out_p_stdin, true, "whois@hit").start();
			// handle the subprocess stdout
			new StreamHandlerThread(in_p_stdout, null, null, false, null).start();
			// handle the subprocess stderr 
			new StreamHandlerThread(null, in_p_stderr, null, false, null).start();
			
			if(p.waitFor()==0)
				System.out.println("sub process exited normally");
			else
				System.out.println("sub process exited unnormally");

		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
	}
}

class StreamHandlerThread extends Thread {

	InputStream in_p_stdout = null;
	InputStream in_p_stderr = null;
	OutputStream out_p_stdin = null;
	boolean isSudoRquested = false;
	String rootPassword = null;

	public StreamHandlerThread(InputStream in_p_stdout, 
								InputStream in_p_stderr, 
								OutputStream out_p_stdin,
								boolean isSudoRquested,
								String rootPassword) {

		this.in_p_stdout = in_p_stdout;
		this.in_p_stderr = in_p_stderr;
		this.out_p_stdin = out_p_stdin;
		this.isSudoRquested = isSudoRquested;
		this.rootPassword = rootPassword;
	}


	public void run() {
	
		try {
			// echo root password to the subprocess's stdin
			if(isSudoRquested) {
				out_p_stdin.write((rootPassword+"\n").getBytes());
			}
	
			try {
				Thread.currentThread().sleep(1000);
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
	
			// display the stdout output of subprocess
			if(in_p_stdout!=null) {
				byte [] buf = new byte[1000];
				while(in_p_stdout.read(buf)>=0) {
					System.out.print(new String(buf));
				}
			}

			// display the stderr output of subprocess
			if(in_p_stderr!=null) {
				byte [] buf = new byte[1000];
				while(in_p_stderr.read(buf)>=0) {
					System.out.print(new String(buf));
				}
			}
	
			// handle current process's input, redirect to out_p_stdin
			Scanner scanner = new Scanner(System.in);
			while(scanner.hasNextLine()) {
				String input = scanner.nextLine();
				System.out.println(input);
				out_p_stdin.write(input.getBytes());
			}

		}
		catch (Exception e) {
			e.printStackTrace();
		}

	}
}
