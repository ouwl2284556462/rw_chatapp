package server;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * 客户端的Socket信息
 */
public class ChatClientSocket {

	/**
	 * 用户名名
	 */
	private String userName;
	
	


	private Socket socket;
	private BufferedReader reader;
	private PrintWriter writer;

	public ChatClientSocket(Socket socket, BufferedReader reader, PrintWriter writer) {
		this.socket = socket;
		this.reader = reader;
		this.writer = writer;
	}

	public Socket getSocket() {
		return socket;
	}

	public BufferedReader getReader() {
		return reader;
	}

	public PrintWriter getWriter() {
		return writer;
	}
	
	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

}
