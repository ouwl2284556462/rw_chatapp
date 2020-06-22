package client.net;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.apache.commons.lang.StringUtils;

import base.MsgConst;
import base.NetUtils;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * 聊天客户端
 */
public class ChatClient implements Closeable {
	private Socket socket;
	private PrintWriter socketWriter;
	private BufferedReader socketReader;
	
	/**
	 * 线程池
	 */
	private ExecutorService threadPool;
	/**
	 * 是否运行
	 */
	private volatile boolean run;
	
	/**
	 * 成功登录的回调
	 */
	private volatile BiConsumer<Boolean, String> loginSuccessCallback;
	
	/**
	 * 监听聊天信息
	 */
	private volatile BiConsumer<String, String> chatMsgListener;
	
	/**
	 * 用户状态改变监听
	 */
	private volatile BiConsumer<Boolean, String> userChgListener;
	
	/**
	 * 在线用户名字
	 */
	private Set<String> onlineUserNameSet;
	
	/**
	 * 当前用户名
	 */
	private String curUserName;

	public ChatClient(String host, int port) throws UnknownHostException, IOException {
		socket = new Socket(host, port);
		socketWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "utf-8"));
		socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
		
		//限定两个线程
		threadPool = Executors.newFixedThreadPool(2);
		//监听服务端消息
		threadPool.execute(this::listenServer);
		
		run = true;
		
		onlineUserNameSet = new HashSet<String>();
	}
	
	
	/**
	 * 登录
	 * @param userName
	 */
	public void login(String userName, BiConsumer<Boolean, String> callBack) {
		curUserName = userName;
		loginSuccessCallback = callBack;
		writeToServerAsy(NetUtils.buildCmdLine(MsgConst.CMD_LOGIN, MsgConst.FIELD_USER_NAME, userName));
	}
	

	
	/**
	 * 监听服务端消息
	 */
	private void listenServer() {
		while(run) {
			try {
				String line = socketReader.readLine();
				System.out.println("收到服务器的信息：" + line);
				JSONObject jsonObj = getJsonObjFromStr(line);
				if(null == jsonObj) {
					continue;
				}
				
				String cmd = jsonObj.optString(MsgConst.FIELD_CMD, "");
				switch (cmd) {
				// 登录
				case MsgConst.CMD_LOGIN:
					dealLogin(jsonObj);
					break;
				// 登出
				case MsgConst.CMD_LOGOUT:
					break;
				//处理聊天
				case MsgConst.CMD_CHAT_TO:
					dealChatTo(jsonObj);
					break;
				//更新用户列表
				case MsgConst.CMD_UPDATE_ONLINE_USER_LIST:
					dealUpdateUserList(jsonObj);
					break;
				}
				
				
			} catch (IOException e) {
			}
		}
	}
	
	/**
	 * 更新用户列表
	 * @param jsonObj
	 */
	private void dealUpdateUserList(JSONObject jsonObj) {
		String userName = jsonObj.getString(MsgConst.FIELD_USER_NAME);
		String status = jsonObj.getString(MsgConst.FIELD_USER_STATUS);
		
		boolean isLogin = status.equals(MsgConst.USER_STATUS_LOGIN);
		//有用户登录
		if(isLogin) {
			onlineUserNameSet.add(userName);
		}else {
			//有用户退出
			onlineUserNameSet.remove(userName);
		}
		
		if(null != userChgListener) {
			userChgListener.accept(isLogin, userName);
		}
	}

	/**
	 * 设置用户监听
	 * @param userChgListener
	 */
	public void setUserChgListener(BiConsumer<Boolean, String> userChgListener) {
		this.userChgListener = userChgListener;
	}


	/**
	 * 聊天处理
	 * @param jsonObj
	 */
	private void dealChatTo(JSONObject jsonObj) {
		if(chatMsgListener == null) {
			return;
		}
		
		chatMsgListener.accept(jsonObj.getString(MsgConst.FIELD_USER_NAME), jsonObj.getString(MsgConst.FIELD_CHAT_MSG));
	}
	

	public void setChatMsgListener(BiConsumer<String, String> chatMsgListener) {
		this.chatMsgListener = chatMsgListener;
	}


	/**
	 * 登录
	 * @param jsonObj 
	 */
	private void dealLogin(JSONObject jsonObj) {
		String errMsg = jsonObj.optString(MsgConst.FIELD_ERR_MSG, "");
		boolean isSucess = StringUtils.isEmpty(errMsg);
		if(isSucess) {
			//所有在线用户名字
			JSONArray nameList = jsonObj.optJSONArray(MsgConst.FIELD_USER_NAME_LIST);
			for(int i = 0; i < nameList.size(); ++i) {
				onlineUserNameSet.add(nameList.getString(i));
			}
		}
		
		
		if(loginSuccessCallback != null) {
			loginSuccessCallback.accept(isSucess, errMsg);
		}
	}


	private JSONObject getJsonObjFromStr(String line) {
		return JSONObject.fromObject(line);
	}


	/**
	 * 异步写消息到服务端
	 * @param msg
	 */
	private void writeToServerAsy(String msg) {
		threadPool.execute(() ->{
			socketWriter.println(msg);
			socketWriter.flush();
		});
	}
	

	@Override
	public void close() throws IOException {
		run = false;
		if(threadPool != null) {
			try {
				threadPool.shutdownNow();
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		if(socket != null) {
			try {
				socket.close();
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 登出
	 */
	public void logout() {
		if(StringUtils.isEmpty(curUserName)) {
			return;
		}
		
		writeToServerAsy(NetUtils.buildCmdLine(MsgConst.CMD_LOGOUT));
	}


	/**
	 * 获取所有用户列表
	 * @return
	 */
	public List<String> getOnlineUserList() {
		return new ArrayList<>(onlineUserNameSet);
	}


	/**
	 * 发送消息到指定对象
	 * @param curChatToUserName
	 * @param msg
	 */
	public void sendChatMsg(String curChatToUserName, String msg) {
		writeToServerAsy(NetUtils.buildCmdLine(MsgConst.CMD_CHAT_TO, 
												MsgConst.FIELD_TARGET_USER, curChatToUserName,
												MsgConst.FIELD_CHAT_MSG, msg));
	}

	/**
	 * 获取当前用户名
	 * @return
	 */
	public String getCurUserName() {
		return curUserName;
	}
}
