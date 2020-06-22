package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang.StringUtils;

import base.MsgConst;
import base.NetUtils;
import net.sf.json.JSONObject;


/**
 * 聊天服务 
 */
public class ChatServer {
	
	/**
	 * 端口
	 */
	private int port;
	
	/**
	 * 是否运行
	 */
	private volatile boolean run;
	
	private Map<String, ChatClientSocket> userMap;

	public ChatServer(int port) {
		this.port = port;
		run = true;
		userMap = new ConcurrentHashMap<String, ChatClientSocket>();
	}


	private void start() throws IOException {
		//创建线程池
		ExecutorService executorService = Executors.newCachedThreadPool();
		
		try(ServerSocket serverSocket = new ServerSocket(this.port)){
			System.out.println("启动服务器成功。");
			while(run) {
				Socket socket = serverSocket.accept();
				//在其他线程上处理socket
				executorService.execute(() -> handle(socket));
			}
		}
		
		//停止线程池
		executorService.shutdownNow();
	}

	/**
	 * 处理socket
	 * @param socket
	 */
	private void handle(Socket socket) {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
	         PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "utf-8"))) {
			
			ChatClientSocket clientSocket = new ChatClientSocket(socket, reader, writer);
			while (run) {
				String line = reader.readLine();
				if (line == null) {
					// 客户端已断开
					printClientInfoLog(socket, "断开连接。");
					return;
				}
				
				printClientInfoLog(socket, "发送请求-" + line);
				JSONObject jsonObj = getJsonObj(line);
				if(null == jsonObj) {
					continue;
				}
				
				//处理命令
				deaWithCmd(jsonObj, clientSocket);
			}

		} catch (Exception e) {
			e.printStackTrace();
			printClientInfoLog(socket, "断开连接。");
		}
	}


	/**
	 * 处理客户端的命令
	 * @param socket
	 * @param jsonObj
	 * @param writer 
	 * @param socket 
	 * @return
	 */
	private void deaWithCmd(JSONObject jsonObj, ChatClientSocket clientSocket) {
		try {
			String cmd = jsonObj.getString(MsgConst.FIELD_CMD);
			switch (cmd) {
			// 登录
			case MsgConst.CMD_LOGIN:
				doLogin(jsonObj, clientSocket);
				break;
			// 登出
			case MsgConst.CMD_LOGOUT:
				doLogout(jsonObj, clientSocket);
				break;
			case MsgConst.CMD_CHAT_TO:
				doChat(jsonObj, clientSocket);
				break;
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 退出
	 * @param jsonObj
	 * @param clientSocket
	 */
	private void doLogout(JSONObject jsonObj, ChatClientSocket clientSocket) {
		removeOnlineUser(clientSocket.getUserName());
	}


	/**
	 * 聊天处理
	 * @param jsonObj
	 * @param clientSocket
	 */
	private void doChat(JSONObject jsonObj, ChatClientSocket clientSocket) {
		String srcUser = clientSocket.getUserName();
		if(StringUtils.isEmpty(srcUser)) {
			//未登录
			return;
		}
		
		String tarUser = jsonObj.getString(MsgConst.FIELD_TARGET_USER);
		ChatClientSocket tarSocket = userMap.get(tarUser);
		if(tarSocket == null) {
			//通知该客户端该用户已下线
			notifyRefreshUserOnlineList(tarUser, MsgConst.USER_STATUS_LOGOUT, clientSocket);
			return;
		}
		
		
		String chatMsg = jsonObj.getString(MsgConst.FIELD_CHAT_MSG);
		//发送消息到目标客户端
		writeMsg(tarSocket, NetUtils.buildCmdLine(MsgConst.CMD_CHAT_TO, 
												 MsgConst.FIELD_CHAT_MSG, chatMsg,
												 MsgConst.FIELD_USER_NAME, srcUser));
	}



	/**
	 * 登录
	 * @param jsonObj 
	 * @param writer 
	 * @param socket 
	 */
	private void doLogin(JSONObject jsonObj, ChatClientSocket clientSocket) {
		String userName = jsonObj.optString(MsgConst.FIELD_USER_NAME, "");
		if(StringUtils.isEmpty(userName)) {
			writeMsg(clientSocket, NetUtils.buildCmdLine(MsgConst.CMD_LOGIN, MsgConst.FIELD_ERR_MSG, "用户名不能为空！"));
			return;
		}
		
		if(isUserNameExist(userName)) {
			writeMsg(clientSocket, NetUtils.buildCmdLine(MsgConst.CMD_LOGIN, MsgConst.FIELD_ERR_MSG, "该用户名已被使用！"));
			return;	
		}
		
		//获取所有在线的成员
		List<String> onlineUserName = getAllOnlineUsersName();
		//添加在线成员
		addOnlineUser(userName, clientSocket);
		
		//设置用户名
		clientSocket.setUserName(userName);
		writeMsg(clientSocket, NetUtils.buildCmdLine(MsgConst.CMD_LOGIN, MsgConst.FIELD_USER_NAME_LIST, onlineUserName));
	}
	
	/**
	 * 添加在线用户
	 * @param userName
	 * @param socket
	 */
	private void addOnlineUser(String userName, ChatClientSocket socket) {
		//通过所有用户有人上线了
		notifyRefreshUserOnlineList(userName, MsgConst.USER_STATUS_LOGIN);
		//添加到在线用户列表
		userMap.put(userName, socket);
	}
	
	private void removeOnlineUser(String userName) {
		userMap.remove(userName);
		//通过所有用户有人上线了
		notifyRefreshUserOnlineList(userName, MsgConst.USER_STATUS_LOGOUT);
	}

	/**
	 * 通知所有客户端更新在线用户列表
	 * @param userName
	 * @param status
	 */
	private void notifyRefreshUserOnlineList(String userName, String status) {
		String refreshCmd = NetUtils.buildCmdLine(MsgConst.CMD_UPDATE_ONLINE_USER_LIST, MsgConst.FIELD_USER_NAME, userName, MsgConst.FIELD_USER_STATUS, status);
		for(ChatClientSocket clientSocket : userMap.values()) {
			try {
				writeMsg(clientSocket, refreshCmd);
			}catch (Exception e) {
			}
		}
	}
	
	/**
	 * 通知单个客户端有用户下线
	 * @param tarUser
	 * @param userStatusLogout
	 * @param clientSocket
	 */
	private void notifyRefreshUserOnlineList(String userName, String status, ChatClientSocket clientSocket) {
		String refreshCmd = NetUtils.buildCmdLine(MsgConst.CMD_UPDATE_ONLINE_USER_LIST, MsgConst.FIELD_USER_NAME, userName, MsgConst.FIELD_USER_STATUS, status);
		try {
			writeMsg(clientSocket, refreshCmd);
		} catch (Exception e) {
		}
	}


	/**
	 * 获取所有在线用户的用户名
	 * @return
	 */
	private List<String> getAllOnlineUsersName() {
		List<String> onlineUserName = new ArrayList<>(userMap.size());
		userMap.keySet().stream().forEach(name -> {
			onlineUserName.add(name);
		});
		return onlineUserName;
	}


	/**
	 * 检查用户名是否存在
	 * @param userName
	 * @return
	 */
	private synchronized boolean isUserNameExist(String userName) {
		return userMap.containsKey(userName);
	}


	private void writeMsg(ChatClientSocket clientSocket, String msg) {
		PrintWriter writer = clientSocket.getWriter();
		writer.println(msg);
		writer.flush();
	}


	private JSONObject getJsonObj(String line) {
		try {
			return JSONObject.fromObject(line);
		}catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	/**
	 * 打印客户端的日志
	 * @param socket
	 * @param msg
	 */
	private void printClientInfoLog(Socket socket, String msg) {
		System.out.println(String.format("%s-%d: %s", socket.getInetAddress(), socket.getPort(), msg));
	}


	public static void main(String[] args) throws IOException {
		//绑定到9999端口
		ChatServer server = new ChatServer(9999);
		//启动服务器
		server.start();
	}
}
