/** This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.util.*;
import java.text.*;

public class ExactClock extends JDialog implements ActionListener, MouseListener, MouseMotionListener
{
	//constants
	private static final String VERSION_NO = "1.7";
	private static final Font f12 = new Font("Microsoft Jhenghei", Font.BOLD, 12);
	private static final Font f13 = new Font("Microsoft Jhenghei", Font.PLAIN, 13);
	private static final Font f14 = new Font("Microsoft Jhenghei", Font.BOLD, 14);
	private static final Font f40 = new Font("Microsoft Jhenghei", Font.BOLD, 40);	
	private static final int WIDTH = (int)Toolkit.getDefaultToolkit().getScreenSize().getWidth();
	private static final int HEIGHT = (int)Toolkit.getDefaultToolkit().getScreenSize().getHeight();
	//settings	
	private static final File settingFile = new File(getJARPath() + "\\EXACTCLOCKRPREF.PROPERTIES\\");
	private static final Properties prop = new Properties();
	//Timer
	private javax.swing.Timer timer;
	private Date date;
	private int timeZone;
	private int displayColor;
	private int hourColor, minuteColor, secondColor;
	//components
	private MyLabel day = new MyLabel();
	private MyLabel hour = new MyLabel();
	private MyLabel minute = new MyLabel();
	private MyLabel second = new MyLabel();
	private MyLabel month = new MyLabel();
	private MyLabel aboutLabel = new MyLabel("          ~~~ExactClock " + VERSION_NO + "~~~          ");
	private JPopupMenu framePopup = new JPopupMenu();
	//DateFormat
	private static final DateFormat monthFormat = new SimpleDateFormat("MM");
	private static final DateFormat dateFormat = new SimpleDateFormat("dd");	
	private static final DateFormat hourFormat = new SimpleDateFormat("HH");
	private static final DateFormat minuteFormat = new SimpleDateFormat("mm");
	private static final DateFormat secondFormat = new SimpleDateFormat("ss");	
	private static final DateFormat dayFormat = new SimpleDateFormat("EEE",Locale.US);
	//tray icon	
	private static boolean useTray = SystemTray.isSupported();
	private JPopupMenu trayPopup = new JPopupMenu();
	private MyTrayIcon trayIcon;
	//drag
	private ResizeButton top = new ResizeButton(1);
	private ResizeButton bottom = new ResizeButton(2);
	private ResizeButton left = new ResizeButton(3);
	private ResizeButton right = new ResizeButton(4);
	private ResizeButton topLeft = new ResizeButton(5);
	private ResizeButton topRight = new ResizeButton(6);
	private ResizeButton bottomLeft = new ResizeButton(7);
	private ResizeButton bottomRight = new ResizeButton(8);
	private int startMouseX, startMouseY;
	private int startFrameX, startFrameY;
	private Point p;
	private Dimension d;
	//dialog	
	private static ExactClock w;	
	public static void main(String[] args)
	{
		ExactClock.setUI();
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				ExactClock.initialize();
				w = new ExactClock("ExactClock " + VERSION_NO);
				w.setVisible(true);
			}
		});
	}
	
	public void addTrayIcon()
	{		
		if (useTray)
		{
			trayIcon = new MyTrayIcon((new ImageIcon(ExactClock.class.getResource("ICON.JPG"))).getImage(), "ExactClock " + VERSION_NO, trayPopup);
			trayIcon.setImageAutoSize(true);
			trayIcon.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseReleased(MouseEvent ev)
				{
					if (!ev.isPopupTrigger())
					{
						ExactClock.this.setVisible(!ExactClock.this.isVisible());
					}
				}
			});
			try
			{
				SystemTray.getSystemTray().add(trayIcon);
				trayPopup.add(new MyMenuItem("Show/Hide", 1));
				trayPopup.add(new MyMenuItem("About", 2));
				trayPopup.add(new MyMenuItem("Settings", 3));
				trayPopup.addSeparator();
				trayPopup.add(new MyMenuItem("Close", 4));
			}
			catch (Exception ex)
			{
				useTray = false;
			}
		}
		/*
		 * now setup frame popup
		 */
		framePopup.add(new MyMenuItem("About", 2));
		framePopup.add(new MyMenuItem("Settings", 3));
		if (useTray)
		{
			framePopup.add(new MyMenuItem("Close to tray", 1));
		}
		framePopup.add(new MyMenuItem("Close", 4));
	}
	
	protected static void initialize()
	{
		//create settings file
		if (!settingFile.exists())
		{
			try (PrintWriter writer = new PrintWriter(settingFile, "UTF-8")) {} catch (IOException ex) {}
			writeConfig("Size.x", "229");
			writeConfig("Size.y", "67");
			writeConfig("Location.x", "0");
			writeConfig("Location.y", "0");
			writeConfig("Timezone", "0");
			writeConfig("Color", "Yellow");
			writeConfig("Opacity","80");
			saveConfig();
		}
		else
		{
			loadConfig();
		}
	}
	
	public ExactClock(String title)
	{
		super();
		this.setTitle(title);
		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.setAlwaysOnTop(true);
		this.getContentPane().setBackground(Color.BLACK);
		this.addComponent();
		this.addMouseListener(this);
		this.addMouseMotionListener(this);
		try
		{
			this.setIconImage((new ImageIcon(ExactClock.class.getResource("ICON.JPG"))).getImage());
		}
		catch (Exception ex)
		{
		}	
	}
	
	void addComponent()
	{
		/*
		 * already loaded config
		 */
		this.setUndecorated(true);
		this.setMinimumSize(new Dimension(229,65));
		this.setLayout(new BorderLayout());
		/*
		 * 
		 */
		JPanel topPanel = new JPanel(new BorderLayout());
		topPanel.setOpaque(false);
		topPanel.add(topLeft, BorderLayout.LINE_START);
		topPanel.add(top, BorderLayout.CENTER);
		topPanel.add(topRight, BorderLayout.LINE_END);
		this.add(topPanel, BorderLayout.PAGE_START);
		JPanel bottomPanel = new JPanel(new BorderLayout());
		bottomPanel.setOpaque(false);
		bottomPanel.add(bottomLeft, BorderLayout.LINE_START);
		bottomPanel.add(bottom, BorderLayout.CENTER);
		bottomPanel.add(bottomRight, BorderLayout.LINE_END);
		this.add(bottomPanel, BorderLayout.PAGE_END);
		this.add(left, BorderLayout.LINE_START);
		this.add(right, BorderLayout.LINE_END);
		/*
		 * 
		 */
		JPanel center = new JPanel();
		center.setOpaque(false);
		center.setLayout(new BorderLayout(0,-7));
		JPanel top = new JPanel();
		top.setOpaque(false);
		top.add(aboutLabel);
		center.add(top, BorderLayout.PAGE_START);
		JPanel center2 = new JPanel(new FlowLayout(FlowLayout.LEFT,5,-2));
		center2.setOpaque(false);
		center2.add(day);
		center2.add(hour);
		center2.add(minute);
		center2.add(second);
		center2.add(month);
		center.add(center2, BorderLayout.CENTER);
		this.add(center, BorderLayout.CENTER);
		center.addMouseListener(this);
		center.addMouseMotionListener(this);
		JFrame.setDefaultLookAndFeelDecorated(true);
		/*
		 * opacity
		 */
		try
		{
			this.setOpacity(Integer.parseInt(getConfig("Opacity"))/100f);
		}
		catch (Exception ex)
		{
		}
		hour.setFont(f40);
		minute.setFont(f40);
		/*
		 * size
		 */
		int x=0, y=0, width=229, height=65;
		try
		{
			width = (int)Double.parseDouble(getConfig("Size.x"));
			height = (int)Double.parseDouble(getConfig("Size.y"));
		}
		catch (Exception ex)
		{
		}
		finally
		{
			this.setSize((int)Math.max(width,getMinimumSize().getWidth()),(int)Math.max(height,getMinimumSize().getHeight()));
		}
		/*
		 * location
		 */
		try
		{
			x = (int)Double.parseDouble(getConfig("Location.x"));
			y = (int)Double.parseDouble(getConfig("Location.y"));
		}
		catch (Exception ex)
		{
		}
		finally
		{		
			this.setLocation(Math.min(x,WIDTH-width), Math.min(y,HEIGHT-height));
		}
		/*
		 * system tray
		 */
		if (useTray)
		{
			this.addTrayIcon();
		}
		else
		{
			JOptionPane.showMessageDialog(this, "System tray is not supported!", "Error", JOptionPane.ERROR_MESSAGE);
		}
		/*
		 * timezone
		 */
		try
		{
			timeZone = Integer.parseInt(getConfig("Timezone"));
		}
		catch (NumberFormatException ex)
		{
			timeZone = 0;
		}
		/*
		 * color
		 */
		displayColor = fromColorString(getConfig("Color"));
		/*
		 * timer
		 */
		timer = new javax.swing.Timer(200,this);
		timer.start();
	}
	
	@Override
	public void actionPerformed(ActionEvent ev)
	{
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.HOUR_OF_DAY, timeZone);
		date = cal.getTime();		
		hourColor = 0;
		minuteColor = 0;
		secondColor = 0;
		//
		String formatted = hourFormat.format(date);
		int i = Integer.parseInt(formatted);
		hour.setText(formatted + ":");
		if (i == 0)
		{
			hourColor = 240;
		}
		else
		{
			hourColor = 240 - 5*i;
		}
		//
		formatted = minuteFormat.format(date);
		i = Integer.parseInt(formatted);
		minute.setText(formatted + "");
		if (i == 0)
		{
			minuteColor = 240;
		}
		else
		{
			minuteColor = 240 - 2*i;
		}
		//
		formatted = secondFormat.format(date);
		i = Integer.parseInt(formatted);
		second.setText(formatted + "");
		if (i == 0)
		{
			secondColor = 240;
		}
		else
		{
			secondColor = 240 - 2*i;
		}
		switch (displayColor)
		{			
			case 1:
			default:
			hour.setForeground(new Color(255,255,hourColor));
			minute.setForeground(new Color(255,255,minuteColor));
			second.setForeground(new Color(255,255,secondColor));
			break;
			
			case 2:
			hour.setForeground(new Color(hourColor,255,hourColor));
			minute.setForeground(new Color(minuteColor,255,minuteColor));
			second.setForeground(new Color(secondColor,255,secondColor));
			break;
			
			case 3:
			hour.setForeground(new Color(hourColor,255,255));
			minute.setForeground(new Color(minuteColor,255,255));
			second.setForeground(new Color(secondColor,255,255));
			break;
			
			case 4:
			hour.setForeground(new Color(255,hourColor,hourColor));
			minute.setForeground(new Color(255,minuteColor,minuteColor));
			second.setForeground(new Color(255,secondColor,secondColor));
			break;
		}
		day.setText(dayFormat.format(date));
		month.setText(monthFormat.format(date) + "-" + dateFormat.format(date));				
	}
	
	class MyMenuItem extends JMenuItem implements ActionListener
	{
		private int x;
		private JSpinner spinner;
		public MyMenuItem(String str, int x)
		{
			super(str);
			this.setFont(f13);
			this.setBackground(Color.WHITE);
			this.addActionListener(this);
			this.x = x;
		}
		
		@Override
		public void actionPerformed(ActionEvent ev)
		{
			Object source = ev.getSource();
			if (source instanceof MyMenuItem)
			{
				switch (x)
				{
					//show/hide
					case 1:
					if (!ExactClock.this.isVisible())
					{
						ExactClock.this.setVisible(true);
					}
					else if (useTray)
					{
						ExactClock.this.setVisible(false);
					}
					else
					{
						ExactClock.this.exit();
					}
					break;
					
					//about
					case 2:
					ImageIcon icon;
					try
					{
						icon = new ImageIcon(ExactClock.class.getResource("ICON48.JPG"));
					}
					catch (Exception ex)
					{
						icon = null;
					}
					JOptionPane.showMessageDialog(ExactClock.this, "ExactClock " + VERSION_NO + " -- a desktop clock utility written in Java by tony200910041.\nDistributed under MPL 2.0.", "About ExactClock", JOptionPane.INFORMATION_MESSAGE, icon);
					break;
					
					//settings
					case 3:
					loadConfig();
					OptionDialog dialog = new OptionDialog();
					dialog.pack();
					dialog.setLocationRelativeTo(ExactClock.this);
					dialog.setVisible(true);
					break;
								
					//close
					case 4:
					ExactClock.this.exit();
					break;
				}
			}
		}
	}
	
	class OptionDialog extends JDialog
	{
		//color
		private JRadioButton yellow = createRadio("Yellow");
		private JRadioButton green = createRadio("Green");
		private JRadioButton blue = createRadio("Blue");
		private JRadioButton red = createRadio("Red");
		private ButtonGroup group = new ButtonGroup();
		//timezone
		private JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, -100, 100, 1));
		//opacity
		private JSlider slider1 = new JSlider(0,100);
		OptionDialog()
		{
			super(ExactClock.this, "ExactClock settings", true);
			this.getContentPane().setLayout(new BoxLayout(this.getContentPane(),BoxLayout.Y_AXIS));
			//
			JPanel panel1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
			panel1.add(createLabel("Time color: "));
			panel1.add(yellow);
			panel1.add(green);
			panel1.add(blue);
			panel1.add(red);
			//
			group.add(yellow);
			group.add(green);
			group.add(blue);
			group.add(red);
			switch (displayColor)
			{
				case 1:
				default:
				yellow.setSelected(true);
				break;
				
				case 2:
				green.setSelected(true);
				break;
				
				case 3:
				blue.setSelected(true);
				break;
				
				case 4:
				red.setSelected(true);
				break;
			}
			this.add(panel1);
			//
			JPanel panel2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
			panel2.add(createLabel("Timezone:"));
			spinner.setFont(f13);
			try
			{
				spinner.setValue(Integer.parseInt(getConfig("Timezone")));
			}
			catch (Exception ex)
			{
			}
			panel2.add(spinner);
			this.add(panel2);
			//
			JPanel panel3 = new JPanel(new FlowLayout(FlowLayout.LEFT));
			panel3.add(createLabel("Opacity: "));
			try
			{
				slider1.setValue(Integer.parseInt(getConfig("Opacity")));
			}
			catch (Exception ex)
			{
				slider1.setValue(100);
			}
			Hashtable<Integer,JLabel> labelTable = new Hashtable<>();
			labelTable.put(0, createLabel("0%"));
			labelTable.put(50, createLabel("50%"));
			labelTable.put(100, createLabel("100%"));
			slider1.setPaintLabels(true);
			slider1.setLabelTable(labelTable);
			slider1.setMajorTickSpacing(10);
			slider1.setSnapToTicks(true);
			slider1.setOpaque(false);
			panel3.add(slider1);
			this.add(panel3);
			this.addWindowListener(new WindowAdapter()
			{
				@Override
				public void windowClosing(WindowEvent ev)
				{
					/*
					 * closed
					 */
					OptionDialog.this.setVisible(false);
					//color
					for (Enumeration<AbstractButton> buttons = group.getElements(); buttons.hasMoreElements();)
					{
						JRadioButton radio = (JRadioButton)(buttons.nextElement());
						if (radio.isSelected())
						{
							String color = radio.getText();
							displayColor = fromColorString(color);
							writeConfig("Color", color);
							break;
						}
					}
					//timezone
					try
					{
						spinner.commitEdit();
					}
					catch (ParseException ex)
					{
					}
					try
					{
						ExactClock.this.timeZone = Integer.parseInt(spinner.getValue().toString());
						writeConfig("Timezone", timeZone+"");
					}
					catch (NumberFormatException ex)
					{
					}
					//opacity
					int opacity = slider1.getValue();
					writeConfig("Opacity",opacity+"");
					try
					{
						ExactClock.this.setOpacity(Integer.parseInt(getConfig("Opacity"))/100f);
					}
					catch (Exception ex)
					{
					}
					saveConfig();
				}
			});
		}
		
		private JLabel createLabel(String text)
		{
			JLabel label = new JLabel(text);
			label.setFont(f13);
			return label;
		}
		
		private JRadioButton createRadio(String text)
		{
			JRadioButton radio = new JRadioButton(text, false);
			radio.setFont(f13);
			radio.setFocusPainted(false);
			return radio;
		}
	}
	
	void exit()
	{
		loadConfig();
		Point p = ExactClock.this.getLocation();
		Dimension d = ExactClock.this.getSize();
		writeConfig("Location.x", p.x + "");
		writeConfig("Location.y", p.y + "");
		writeConfig("Size.x", d.width + "");
		writeConfig("Size.y", d.height + "");
		saveConfig();
		System.exit(0);
	}
		
	@Override
	public void mousePressed(MouseEvent ev)
	{
		p = this.getLocation();
		d = this.getSize();
		startFrameX = (int)p.getX();
		startFrameY = (int)p.getY();
		if (ev.getSource() instanceof ExactClock||ev.getSource() instanceof JPanel)
		{
			startMouseX = ev.getXOnScreen();
			startMouseY = ev.getYOnScreen();				
		}
		else if (ev.getSource() instanceof ResizeButton)
		{
			ResizeButton src = (ResizeButton)(ev.getSource());
			src.startMouseX = ev.getXOnScreen();
			src.startMouseY = ev.getYOnScreen();
		}
	}
	
	@Override
	public synchronized void mouseDragged(MouseEvent ev)
	{
		if (ev.getSource() instanceof JDialog||ev.getSource() instanceof JPanel)
		{
			int newX = startFrameX+ev.getXOnScreen()-startMouseX;
			if (newX<0) newX=0;
			if (newX+this.getWidth()>ExactClock.WIDTH) newX = ExactClock.WIDTH-this.getWidth();
			int newY = startFrameY+ev.getYOnScreen()-startMouseY;
			if (newY<0) newY=0;
			if (newY+this.getHeight()>ExactClock.HEIGHT) newY = ExactClock.HEIGHT-this.getHeight();
			this.setLocation(newX, newY);
		}
		else if (ev.getSource() instanceof ResizeButton)
		{
			ResizeButton src = (ResizeButton)(ev.getSource());
			int x1=0,y1=0,width=0,height=0;
			int minWidth = (int)this.getMinimumSize().getWidth();
			int minHeight = (int)this.getMinimumSize().getHeight();
			switch (src.x)
			{
				case 1: //top
				{
					int dy = ev.getYOnScreen()-p.y;
					x1 = p.x;
					width = d.width;
					if (d.height-dy>=minHeight)
					{
						y1 = p.y+dy;
						height = d.height-dy;
					}
					else return;
				}
				break;
				
				case 2: //bottom
				{
					int dy = ev.getYOnScreen()-(p.y+d.height);
					x1 = p.x;
					width = d.width;
					if (d.height+dy>=minHeight)
					{
						y1 = p.y;
						height = d.height+dy;
					}
					else return;
				}
				break;
				
				case 3: //left
				{
					int dx = ev.getXOnScreen()-(p.x);
					y1 = p.y;
					height = d.height;
					if (d.width-dx>=minWidth)
					{
						x1 = p.x+dx;
						width = d.width-dx;
					}
					else return;
				}
				break;
				
				case 4: //right
				{
					int dx = ev.getXOnScreen()-(p.x+d.width);
					y1 = p.y;
					height = d.height;
					if (d.width+dx>=minWidth)
					{
						x1 = p.x;
						width = d.width+dx;
					}
					else return;
				}
				break;
				
				case 5: //topLeft
				{
					int dx = ev.getXOnScreen()-p.x;
					int dy = ev.getYOnScreen()-p.y;
					if (d.width-dx>=minWidth)
					{
						x1 = p.x+dx;
						width = d.width-dx;
					}
					else
					{
						x1 = (int)w.getLocation().getX();
						width = minWidth;
					}
					if (d.height-dy>=minHeight)
					{
						y1 = p.y+dy;
						height = d.height-dy;
					}
					else
					{
						y1 = (int)w.getLocation().getY();
						height = minHeight;
					}
				}
				break;
				
				case 6: //topRight
				{
					int dx = ev.getXOnScreen()-(p.x+d.width);
					int dy = ev.getYOnScreen()-p.y;
					if (d.width+dx>=minWidth)
					{
						x1 = p.x;
						width = d.width+dx;
					}
					else
					{
						x1 = (int)this.getLocation().getX();
						width = minWidth;
					}
					if (d.height-dy>=minHeight)
					{
						y1 = p.y+dy;
						height = d.height-dy;
					}
					else
					{
						y1 = (int)w.getLocation().getY();
						height = minHeight;
					}
				}
				break;
				
				case 7: //bottomLeft
				{
					int dx = ev.getXOnScreen()-p.x;
					int dy = ev.getYOnScreen()-(p.y+d.height);
					if (d.width-dx>=minWidth)
					{
						x1 = p.x+dx;
						width = d.width-dx;
					}
					else
					{
						x1 = (int)w.getLocation().getX();
						width = minWidth;
					}
					if (d.height+dy>=minHeight)
					{
						y1 = p.y;
						height = d.height+dy;
					}
					else
					{
						y1 = (int)w.getLocation().getY();
						height = minHeight;
					}
				}
				break;
				
				case 8: //bottomRight
				{
					int dx = ev.getXOnScreen()-(p.x+d.width);
					int dy = ev.getYOnScreen()-(p.y+d.height);
					if (d.width+dx>=minWidth)
					{
						x1 = p.x;
						width = d.width+dx;
					}
					else
					{
						x1 = (int)w.getLocation().getX();
						width = minWidth;
					}
					if (d.height+dy>=minHeight)
					{
						y1 = p.y;
						height = d.height+dy;
					}
					else
					{
						y1 = (int)w.getLocation().getY();
						height = minHeight;
					}
				}
				break;
			}
			w.setSize(width,height);
			w.setLocation(x1,y1);
		}
	}
	
	@Override
	public void mouseReleased(MouseEvent ev)
	{
		if (ev.isPopupTrigger())
		{
			framePopup.show(w,ev.getX(),ev.getY());
		}
	}
	
	class ResizeButton extends JButton
	{
		int x;
		int startMouseX, startMouseY;
		public ResizeButton(int x)
		{
			super("");
			this.x = x;
			this.setBackground(Color.BLACK);
			this.setBorder(null);
			this.setPreferredSize(new Dimension(2,2));
			this.addMouseListener(ExactClock.this);
			this.addMouseMotionListener(ExactClock.this);
			this.setFocusPainted(false);
			int cursor = Cursor.DEFAULT_CURSOR;
			switch (x)
			{
				case 1:
				case 2:
				cursor = Cursor.N_RESIZE_CURSOR;
				break;
				
				case 3:
				case 4:
				cursor = Cursor.E_RESIZE_CURSOR;
				break;
				
				case 5:
				case 8:
				cursor = Cursor.NW_RESIZE_CURSOR;
				break;
				
				case 6:
				case 7:
				cursor = Cursor.NE_RESIZE_CURSOR;
				break;
			}
			this.setCursor(Cursor.getPredefinedCursor(cursor));
		}
	}
	
	class MyLabel extends JLabel
	{
		public MyLabel()
		{
			super();
			this.setFont(f14);
			this.setForeground(Color.WHITE);
		}
		
		public MyLabel(String str)
		{
			super(str);
			this.setFont(f12);
			this.setForeground(Color.WHITE);
		}
	}
	
	static void setUI()
	{
		UIManager.put("OptionPane.buttonFont", f13);
		UIManager.put("OptionPane.messageFont", f13);
		UIManager.put("Button.background", Color.WHITE);
		UIManager.put("OptionPane.okButtonText", "OK");	
	}
	
	static String toColorString(int color)
	{
		switch (color)
		{
			case 1:
			default:
			return "Yellow";
			
			case 2:
			return "Green";
			
			case 3:
			return "Blue";
			
			case 4:
			return "Red";
		}
	}
	
	static int fromColorString(String s)
	{
		if (s == null) return 1;
		else
		{
			switch (s)
			{
				case "Yellow":
				default:
				return 1;
				
				case "Green":
				return 2;
				
				case "Blue":
				return 3;
				
				case "Red":
				return 4;
			}
		}
	}
	
	static void loadConfig()
	{
		try
		{
			prop.load(new FileInputStream(settingFile));
		}
		catch (Exception ex)
		{
		}
	}
	
	static String getConfig(String name)
	{
		return prop.getProperty(name);
	}
	
	static void writeConfig(String key, String value)
	{
		prop.setProperty(key, value);		
	}
	
	static void saveConfig()
	{
		try
		{
			prop.store(new FileOutputStream(settingFile), null);
		}
		catch (Exception ex)
		{
		}
	}
	
	static File getJARPath()
	{
		try
		{			
			return new File((new File(ExactClock.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath())).getParentFile().getPath());
		}
		catch (Exception ex)
		{
			return null;
		}
	}
	
	@Override
	public void mouseExited(MouseEvent ev)
	{
	}
	
	@Override
	public void mouseEntered(MouseEvent ev)
	{
	}
	
	@Override
	public void mouseClicked(MouseEvent ev)
	{
	}
	
	@Override
	public void mouseMoved(MouseEvent ev)
	{
	}
}
