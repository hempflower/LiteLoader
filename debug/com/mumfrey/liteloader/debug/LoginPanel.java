package com.mumfrey.liteloader.debug;

import javax.swing.JPanel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.BorderLayout;

import javax.swing.JLabel;

import java.awt.Font;

import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.JDialog;
import javax.swing.UIManager;
import javax.swing.JCheckBox;

import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.FocusTraversalPolicy;
import java.awt.TextField;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;

import javax.swing.JButton;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;

/**
 * JPanel displayed in a JDialog to prompt the user for login credentials for minecraft
 * 
 * @author Adam Mummery-Smith
 */
public class LoginPanel extends JPanel
{
	private static final long serialVersionUID = 1L;
	private TextField txtUsername;
	private TextField txtPassword;
	private JButton btnLogin;
	private JButton btnCancel;
	
	private boolean loginClicked = false;

	private JDialog dialog;
	private JCheckBox chkOffline;
	private JLabel lblLogIn;
	private JPanel panelLogin;
	private JPanel panelButtons;
	private JPanel panelPadding;
	private JLabel lblNewLabel;
	private JLabel lblPassword;
	
	private CustomFocusTraversal tabOrder = new CustomFocusTraversal();
	private JLabel lblMessage;
	
	public LoginPanel(String username, String password, String error)
	{
		this.setFocusable(false);
		this.setPreferredSize(new Dimension(400, 260));
		this.setBackground(new Color(105, 105, 105));
		this.setLayout(new BorderLayout(0, 0));
		
		this.lblLogIn = new JLabel("Log In");
		this.lblLogIn.setFocusable(false);
		this.lblLogIn.setBorder(new EmptyBorder(10, 16, 10, 16));
		this.lblLogIn.setOpaque(true);
		this.lblLogIn.setBackground(new Color(119, 136, 153));
		this.lblLogIn.setFont(new Font("Tahoma", Font.BOLD, 18));
		this.lblLogIn.setForeground(new Color(255, 255, 255));
		this.lblLogIn.setPreferredSize(new Dimension(400, 64));
		this.add(this.lblLogIn, BorderLayout.NORTH);
		
		this.panelButtons = new JPanel();
		this.panelButtons.setFocusable(false);
		this.panelButtons.setBackground(new Color(112, 128, 144));
		this.panelButtons.setPreferredSize(new Dimension(400, 32));
		this.add(this.panelButtons, BorderLayout.SOUTH);
		this.panelButtons.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		this.chkOffline = new JCheckBox("Never ask me to log in (always run offline)");
		this.chkOffline.setPreferredSize(new Dimension(386, 23));
		this.chkOffline.setForeground(new Color(255, 255, 255));
		this.chkOffline.setOpaque(false);
		this.panelButtons.add(this.chkOffline);
		
		this.panelPadding = new JPanel();
		this.panelPadding.setFocusable(false);
		this.panelPadding.setBorder(new EmptyBorder(4, 8, 8, 8));
		this.panelPadding.setOpaque(false);
		this.add(this.panelPadding, BorderLayout.CENTER);
		this.panelPadding.setLayout(new BorderLayout(0, 0));
		
		this.panelLogin = new JPanel();
		this.panelLogin.setFocusable(false);
		this.panelPadding.add(this.panelLogin);
		this.panelLogin.setOpaque(false);
		this.panelLogin.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Yggdrasil Login", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(255, 255, 255)));
		GridBagLayout gbl_panelLogin = new GridBagLayout();
		gbl_panelLogin.columnWidths = new int[] {30, 80, 120, 120, 30};
		gbl_panelLogin.rowHeights = new int[] {24, 32, 32, 32};
		gbl_panelLogin.columnWeights = new double[]{0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		gbl_panelLogin.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0};
		this.panelLogin.setLayout(gbl_panelLogin);
		
		this.btnLogin = new JButton("Log in");
		this.btnLogin.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				LoginPanel.this.onLoginClick();
			}
		});
		
		this.btnCancel = new JButton("Cancel");
		this.btnCancel.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				LoginPanel.this.onCancelClick();
			}
		});
		
		this.lblMessage = new JLabel("Enter your login details for minecraft.net");
		this.lblMessage.setForeground(new Color(255, 255, 255));
		GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
		gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_1.gridwidth = 2;
		gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_1.gridx = 2;
		gbc_lblNewLabel_1.gridy = 0;
		this.panelLogin.add(this.lblMessage, gbc_lblNewLabel_1);
		
		this.lblNewLabel = new JLabel("User name");
		this.lblNewLabel.setFocusable(false);
		this.lblNewLabel.setForeground(new Color(255, 255, 255));
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel.fill = GridBagConstraints.VERTICAL;
		gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel.gridx = 1;
		gbc_lblNewLabel.gridy = 1;
		this.panelLogin.add(this.lblNewLabel, gbc_lblNewLabel);
		
		this.txtUsername = new TextField();
		this.txtUsername.setPreferredSize(new Dimension(200, 22));
		this.txtUsername.setText(username);
		GridBagConstraints gbc_txtUsername = new GridBagConstraints();
		gbc_txtUsername.gridwidth = 2;
		gbc_txtUsername.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtUsername.insets = new Insets(0, 0, 5, 0);
		gbc_txtUsername.gridx = 2;
		gbc_txtUsername.gridy = 1;
		this.panelLogin.add(this.txtUsername, gbc_txtUsername);
		
		this.lblPassword = new JLabel("Password");
		this.lblPassword.setFocusable(false);
		this.lblPassword.setForeground(Color.WHITE);
		GridBagConstraints gbc_lblPassword = new GridBagConstraints();
		gbc_lblPassword.anchor = GridBagConstraints.WEST;
		gbc_lblPassword.fill = GridBagConstraints.VERTICAL;
		gbc_lblPassword.insets = new Insets(0, 0, 5, 5);
		gbc_lblPassword.gridx = 1;
		gbc_lblPassword.gridy = 2;
		this.panelLogin.add(this.lblPassword, gbc_lblPassword);
		
		this.txtPassword = new TextField();
		this.txtPassword.setEchoChar('*');
		this.txtPassword.setPreferredSize(new Dimension(200, 22));
		this.txtPassword.setText(password);
		GridBagConstraints gbc_txtPassword = new GridBagConstraints();
		gbc_txtPassword.gridwidth = 2;
		gbc_txtPassword.insets = new Insets(0, 0, 5, 0);
		gbc_txtPassword.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtPassword.gridx = 2;
		gbc_txtPassword.gridy = 2;
		this.panelLogin.add(this.txtPassword, gbc_txtPassword);
		GridBagConstraints gbc_btnCancel = new GridBagConstraints();
		gbc_btnCancel.anchor = GridBagConstraints.EAST;
		gbc_btnCancel.insets = new Insets(0, 0, 0, 5);
		gbc_btnCancel.gridx = 2;
		gbc_btnCancel.gridy = 3;
		this.panelLogin.add(this.btnCancel, gbc_btnCancel);
		GridBagConstraints gbc_btnLogin = new GridBagConstraints();
		gbc_btnLogin.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnLogin.gridx = 3;
		gbc_btnLogin.gridy = 3;
		this.panelLogin.add(this.btnLogin, gbc_btnLogin);
		
		this.tabOrder.add(this.txtUsername);
		this.tabOrder.add(this.txtPassword);
		this.tabOrder.add(this.btnLogin);
		this.tabOrder.add(this.btnCancel);
		this.tabOrder.add(this.chkOffline);
		
		if (error != null)
		{
			this.lblMessage.setText(error);
			this.lblMessage.setForeground(new Color(255, 180, 180));
		}
	}
	
	protected void onShowDialog()
	{
		if (this.txtUsername.getText().length() > 0)
		{
			if (this.txtPassword.getText().length() > 0)
				this.txtUsername.select(0, this.txtUsername.getText().length());
			else
				this.txtPassword.requestFocusInWindow();
		}
	}
	
	protected void onLoginClick()
	{
		this.loginClicked = true;
		this.dialog.setVisible(false);
	}

	protected void onCancelClick()
	{
		this.dialog.setVisible(false);
	}
	
	/**
	 * @param dialog
	 * @param panel
	 */
	public void setDialog(JDialog dialog)
	{
		this.dialog = dialog;

		this.dialog.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowOpened(WindowEvent e)
			{
				LoginPanel.this.onShowDialog();
			}
		});
		
		this.dialog.getRootPane().setDefaultButton(this.btnLogin);
		this.dialog.setFocusTraversalPolicy(this.tabOrder);
	}
	
	public boolean showModalDialog()
	{
		this.dialog.setVisible(true);
		this.dialog.dispose();
		return this.loginClicked;
	}
	
	public String getUsername()
	{
		return this.txtUsername.getText();
	}
	
	public String getPassword()
	{
		return this.txtPassword.getText();
	}
	
	public boolean workOffline()
	{
		return this.chkOffline.isSelected();
	}
	
	public static LoginPanel getLoginPanel(String username, String password, String error)
	{
		if (username == null) username = "";
		if (password == null) password = "";
		
		final JDialog dialog = new JDialog();
		final LoginPanel panel = new LoginPanel(username, password, error);
		panel.setDialog(dialog);
		
		dialog.setContentPane(panel);
		dialog.setTitle("Yggdrasil Login");
		dialog.setResizable(false);
		dialog.pack();
		dialog.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		dialog.setLocationRelativeTo(null);
		dialog.setModal(true);
		
		return panel;
	}
	
	class CustomFocusTraversal extends FocusTraversalPolicy
	{
		private final List<Component> components = new ArrayList<Component>();
		
		void add(Component component)
		{
			this.components.add(component);
		}
		
		@Override
		public Component getComponentAfter(Container container, Component component)
		{
			int index = this.components.indexOf(component) + 1;
			if (index >= this.components.size()) return this.components.get(0);
			return this.components.get(index);
		}
		
		@Override
		public Component getComponentBefore(Container container, Component component)
		{
			int index = this.components.indexOf(component) - 1;
			if (index < 0) return this.getLastComponent(container);
			return this.components.get(index);
		}
		
		@Override
		public Component getFirstComponent(Container container)
		{
			return this.components.get(0);
		}
		
		@Override
		public Component getLastComponent(Container container)
		{
			return this.components.get(this.components.size() - 1);
		}
		
		@Override
		public Component getDefaultComponent(Container container)
		{
			return this.getFirstComponent(container);
		}
	}
}
