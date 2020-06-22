package client.ui;

import javax.swing.JOptionPane;

/**
 * UI工具类 
 */
public class UiUtils {

	public static void showMsg(String msg){
		JOptionPane.showMessageDialog(null, msg, "", JOptionPane.INFORMATION_MESSAGE);
	}
}
