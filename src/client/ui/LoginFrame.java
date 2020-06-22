package client.ui;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.UnknownHostException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.apache.commons.lang.StringUtils;

import client.ClientLauncher;


public class LoginFrame extends JFrame{
	
	public LoginFrame() throws UnknownHostException, IOException {
		
		setTitle("我的聊天");
		// 设置大小
		setSize(400, 200);
		// 关闭窗口
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				super.windowClosing(e);
				try {
					ClientLauncher.netClient.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				System.exit(0);
			}
		});    

		initPanelCont();
		initMenuBar();
		// 设置窗口屏幕居中
		setLocationRelativeTo(null);
		// 设置可见
		setVisible(true);
		// 不可改变大小
		setResizable(false);
	}
	
	private void initMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		JMenu helpMenu = new JMenu("帮助");
		menuBar.add(helpMenu);

		JMenuItem aboutMenuItem = new JMenuItem("关于");
		JMenuItem exitMenuItem = new JMenuItem("退出");

		helpMenu.add(aboutMenuItem);
		helpMenu.add(exitMenuItem);

		aboutMenuItem.addActionListener(e -> {
			UiUtils.showMsg("软件技术（软件） 3班 201952180305 柴江豪");
		});

		exitMenuItem.addActionListener( e -> {
			dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
		});
	}
	
	private void initPanelCont() {
		JPanel contPanel = new JPanel();
		setContentPane(contPanel);
		// 清空默认布局
		contPanel.setLayout(null);
		
		JLabel userNameLabel = new JLabel("用户名");
		userNameLabel.setBounds(80, 30, 80, 30);
		contPanel.add(userNameLabel);
		
		JTextField userNameTxt = new JTextField();
		userNameTxt.setBounds(140, 30, 150, 30);
		contPanel.add(userNameTxt);

		JButton loginBtn = new JButton("登录");
		loginBtn.setBounds(140, 80, 70, 40);
		contPanel.add(loginBtn);
		loginBtn.addActionListener(e -> {
			String userName = userNameTxt.getText();
			if(StringUtils.isEmpty(userName)) {
				UiUtils.showMsg("请输入名字！");
				return;
			}
			
			loginBtn.setEnabled(false);
			loginBtn.setText("登录中..");
			
			ClientLauncher.netClient.login(userName, (isSuccess, msg)->{
				SwingUtilities.invokeLater(() -> {
					loginBtn.setEnabled(true);
					loginBtn.setText("登录");
					
					if(!isSuccess) {
						UiUtils.showMsg(msg);
						return;
					}
					
					System.out.println("登录成功");
					//登录成功
					//隐藏当前窗口
					setVisible(false);
					//打开聊天窗口
					new ChatFrame(this);
				});
			});
		});
	}
}
