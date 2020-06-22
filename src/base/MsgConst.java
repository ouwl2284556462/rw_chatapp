package base;

/**
 * 客户端、服务端通信的字段常量
 */
public class MsgConst {
	
	/**
	 * 命令字段
	 */
	public static final String FIELD_CMD = "cmd";
	/**
	 * 登录
	 */
	public static final String CMD_LOGIN = "login";
	
	/**
	 * 登出
	 */
	public static final String CMD_LOGOUT = "logout";
	
	
	/**
	 * 对某人聊天
	 */
	public static final String CMD_CHAT_TO = "chat_to";
	
	/**
	 * 更新用户在线列表
	 */
	public static final String CMD_UPDATE_ONLINE_USER_LIST = "update_online_users";
	
	
	/**
	 * 用户名
	 */
	public static final String FIELD_USER_NAME = "userName";
	
	/**
	 * 目标用户
	 */
	public static final String FIELD_TARGET_USER = "targetUserName";
	
	/**
	 * 用户状态
	 */
	public static final String FIELD_USER_STATUS = "userStatus";
	
	/**
	 * 用户状态-登录
	 */
	public static final String USER_STATUS_LOGIN = "login";
	
	/**
	 * 用户状态-登出
	 */
	public static final String USER_STATUS_LOGOUT = "logout";
	
	
	/**
	 * 错误信息
	 */
	public static final String FIELD_ERR_MSG = "errMsg";
	
	/**
	 * 用户名字列表
	 */
	public static final String FIELD_USER_NAME_LIST = "userNameList";
	
	/**
	 * 聊天内容
	 */
	public static final String FIELD_CHAT_MSG = "chatMsg";
}
