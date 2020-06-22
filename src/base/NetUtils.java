package base;

import java.util.HashMap;
import java.util.Map;

import net.sf.json.JSONObject;

public class NetUtils {
	
	/**
	 * 构造网络传输的数据行
	 * @param cmd
	 * @param otherField
	 * @return
	 */
	public static String buildCmdLine(String cmd, Object...otherField) {
		Map<String, Object> map = new HashMap<>();
		map.put(MsgConst.FIELD_CMD, cmd);
		for (int i = 0; i < otherField.length; i += 2) {
			map.put((String) otherField[i], otherField[i + 1]);
		}
		
		return JSONObject.fromObject(map).toString();
	}
}
