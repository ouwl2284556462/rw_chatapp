package client.ui;

import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import org.apache.commons.lang.StringUtils;

import client.ClientLauncher;

public class ChatFrame extends JFrame {
	
	/**
	 * 用户列表
	 */
	private DefaultListModel<UserListItem> userListModel;
	
	/**
	 * 用户列表滚动面板
	 */
	private JScrollPane userListScrollPane;
	
	/**
	 * 聊天滚动面板
	 */
	private JScrollPane chatScrollPane;
	
	/**
	 * 聊天文本框
	 */
	private JTextArea chatTextArea;
	
	/**
	 * 聊天输入文本框
	 */
	private JTextArea chatInputArea;
	
	/**
	 * 当前的聊天对象
	 */
	private String curChatToUserName;
	
	/**
	 * 保存每个人的聊天记录
	 */
	private Map<String, String> chatContentMap;
	
	/**
	 * 未读名单
	 */
	private Set<String> unReadName;
	
	private JList<UserListItem> userList;
	
	

	public ChatFrame(JFrame parent) {
		chatContentMap = new HashMap<String, String>();
		unReadName = new HashSet<>();
		//监听聊天信息
		ClientLauncher.netClient.setChatMsgListener(this::chatMsgCallBack);
		ClientLauncher.netClient.setUserChgListener(this::userChgCallBack);

		setTitle(ClientLauncher.netClient.getCurUserName());
		// 设置大小
		setSize(1000, 500);
		// 关闭窗口
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				super.windowClosing(e);
				try {
					ClientLauncher.netClient.logout();
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				// 让父窗口显示
				parent.setVisible(true);
			}
		});

		initPanelCont();
		// 设置窗口屏幕居中
		setLocationRelativeTo(null);
		// 设置可见
		setVisible(true);
		// 不可改变大小
		setResizable(false);
	}
	
	/**
	 * 用户变化监听
	 * @param isLogin
	 * @param userName
	 */
	private void userChgCallBack(boolean isLogin, String userName) {
		SwingUtilities.invokeLater(() -> {
			if(isLogin) {
				addNewUser(userName);
			}else {
				if(userName.equals(curChatToUserName)) {
					chatScrollPane.setColumnHeaderView(new JLabel(userName + "  (已下线)"));
				}
				
				removeUserListItem(userName);
				removeUnRead(userName);
				chatContentMap.remove(userName);
			}
		});
	}
	
	private void removeUserListItem(String userName) {
		for(int i = 0; i < userListModel.size(); ++i) {
			UserListItem item = userListModel.get(i);
			if(item.getUserName().equals(userName)) {
				userListModel.remove(i);
				return;
			}
		}
	}
	
	/**
	 * 监听聊天信息
	 * @param userName
	 * @param chatMsg
	 */
	private void chatMsgCallBack(String userName, String chatMsg) {
		SwingUtilities.invokeLater(() ->{
			//当前聊天框是对方时,直接添加
			if(StringUtils.isNotEmpty(curChatToUserName) && curChatToUserName.equals(userName)) {
				addMsgToCurChatContentTextArea(userName, chatMsg);
				return;
			}
			
			//添加信息到目标的聊天记录里
			addMsgToChatMsgMap(userName, chatMsg);
			//添加未读名单
			addUnRead(userName);
		});
	}

	/**
	 * 添加未读名单
	 * @param userName
	 */
	private void addUnRead(String userName) {
		unReadName.add(userName);
		userListScrollPane.setColumnHeaderView(new JLabel("在线用户-新消息！"));
		
		removeUserListItem(userName);
		addNewUser(userName);
	}
	
	private void removeUnRead(String userName) {
		unReadName.remove(userName);
		if(unReadName.isEmpty()) {
			userListScrollPane.setColumnHeaderView(new JLabel("在线用户"));
		}
		userList.revalidate();
	}

	/**
	 * 添加信息到目标的聊天记录里
	 * @param userName
	 * @param chatMsg
	 */
	private void addMsgToChatMsgMap(String userName, String chatMsg) {
		String lastContent = chatContentMap.getOrDefault(userName, "");
		lastContent += buildChatContentToShow(userName, chatMsg);
		chatContentMap.put(userName, lastContent);
	}

	/**
	 * 初始化页面内容
	 */
	private void initPanelCont() {
		JPanel contPanel = new JPanel();
		setContentPane(contPanel);
		// 清空默认布局
		contPanel.setLayout(null);

		userListModel = new DefaultListModel<UserListItem>();
		// 用户列表
		userList = new JList<>(userListModel);
		userList.addListSelectionListener(e -> {
			UserListItem item = userList.getSelectedValue();
			if(item == null) {
				return;
			}
			
			String userName = item.getUserName();
			changeCurUserToChat(userName);
		});
		userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		userListScrollPane = new JScrollPane(userList);
		userListScrollPane.setColumnHeaderView(new JLabel("在线用户"));
		userListScrollPane.setBounds(5, 5, 200, 465);
		contPanel.add(userListScrollPane);

		
		//初始化用户列表
		initUserList();
		
		//聊天框
		chatTextArea = new JTextArea();
		chatTextArea.setEditable(false);
		chatTextArea.setLineWrap(true);
		chatTextArea.setBackground(new Color(255, 255, 255));
		chatScrollPane = new JScrollPane(chatTextArea);
		chatScrollPane.setBounds(210, 5, 780, 310);
		contPanel.add(chatScrollPane);
		
		//输入框
		chatInputArea = new JTextArea();
		chatInputArea.setLineWrap(true);
		JScrollPane chatInputScrollPane = new JScrollPane(chatInputArea);
		chatInputScrollPane.setBounds(210, 320, 780, 110);
		contPanel.add(chatInputScrollPane);
		
		JButton sendBtn = new JButton("发送");
		sendBtn.setBounds(885, 435, 100, 30);
		sendBtn.addActionListener(e -> {
			sendChatMsg();
		});
		contPanel.add(sendBtn);
	}

	/**
	 * 发送消息
	 */
	private void sendChatMsg() {
		if(StringUtils.isEmpty(curChatToUserName)) {
			UiUtils.showMsg("请选择用户");
			return;
		}
		
		
		String msg = chatInputArea.getText();
		if(StringUtils.isEmpty(msg)) {
			UiUtils.showMsg("请输入");
			return;
		}
		
		chatInputArea.setText("");
		addMsgToCurChatContentTextArea(ClientLauncher.netClient.getCurUserName(), msg);
		//发送
		ClientLauncher.netClient.sendChatMsg(curChatToUserName, msg);
	}
	
	/**
	 * 添加聊天信息到文本框
	 * @param userName
	 * @param msg
	 */
	private void addMsgToCurChatContentTextArea(String userName, String msg) {
		String content = buildChatContentToShow(userName, msg);
		chatTextArea.append(content);
	}

	/**
	 * 构建显示的内容
	 * @param userName
	 * @param msg
	 * @return
	 */
	private String buildChatContentToShow(String userName, String msg) {
		String content = String.format(" %s %s\n             %s\n\n", userName, getCurTimeFormat(), msg);
		return content;
	}
	
	/**
	 * 获取当前时间
	 * @return
	 */
	private String getCurTimeFormat() {
		return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss"));
	}

	/**
	 * 初始化用户列表
	 */
	private void initUserList() {
		List<String> userList = ClientLauncher.netClient.getOnlineUserList();
		
		for(String name : userList) {
			addNewUser(name);
		}
	}

	/**
	 * 往用户列表中添加新用户
	 * @param name
	 */
	private void addNewUser(String name) {
		UserListItem item = new UserListItem(name, unReadName);
		userListModel.add(0, item);
	}
	
	/**
	 * 切换当前聊天对象
	 * @param userName
	 */
	private void changeCurUserToChat(String userName) {
		if(curChatToUserName != null) {
			chatContentMap.put(curChatToUserName, chatTextArea.getText());
		}
		
		chatScrollPane.setColumnHeaderView(new JLabel(userName));
		curChatToUserName = userName;
		
		String lastChatContent = chatContentMap.get(curChatToUserName);
		if(StringUtils.isNotEmpty(lastChatContent)) {
			chatTextArea.setText(lastChatContent);
		}else {
			chatTextArea.setText("");
		}
		
		removeUnRead(userName);
	}
	

}
