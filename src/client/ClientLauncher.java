package client;

import java.io.IOException;
import java.net.UnknownHostException;

import client.net.ChatClient;
import client.ui.LoginFrame;

/**
 * 客户端入口
 */
public class ClientLauncher {
	
	/**
	 * 服务端地址
	 */
	private final static String SERVER_HOST = "127.0.0.1";
	/**
	 * 服务端端口
	 */
	private final static int SERVER_PORT = 9999;
	
	public static ChatClient netClient;

	public static void main(String[] args) throws UnknownHostException, IOException {
		netClient = new ChatClient(SERVER_HOST, SERVER_PORT);
		// 打开窗口
		new LoginFrame();
	}

}
