package client.ui;

import java.util.Set;

/**
 * 用户列表项 
 */
public class UserListItem {
	
	private String userName;
	private Set<String> newMsgUser;
	
	public UserListItem(String userName, Set<String> newMsgUser) {
		this.userName = userName;
		this.newMsgUser = newMsgUser;
	}

	
	public String getUserName() {
		return userName;
	}

	@Override
	public String toString() {
		String result = userName;
		if(newMsgUser.contains(userName)) {
			result += "  (新消息)";
		}
		return result;
	}
	
	
}
