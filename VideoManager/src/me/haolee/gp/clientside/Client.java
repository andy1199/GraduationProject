package me.haolee.gp.clientside;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import me.haolee.gp.common.CommandWord;
import me.haolee.gp.common.Config;

// main function
public class Client {
	private ExecutorService executorService = null;
	/*事件监听里面只能用final变量或类成员变量，在此定义好成员变量*/
	private JFrame mainFrame = null;
	private JPanel mainPanel = null;//子线程要用，静态方便
	private JList<String> categoryList = null;
	private DefaultListModel<String> categoryListModel = null;
	private CommandWord mode = CommandWord.MODE_LIVE;//播放模式初始值
	//用于显示总记录条数的标签
	private JLabel lblTotalNumber = null;
	//分类下总数
	private int totalNumber = 0;
	//起始序号和步长
	private int videoListStart = 0;//行数默认从0计
	private int videoListStep = 9;//默认步长
	
	private JTextField tfPageNo;
	
	public static void main(String[] args) {
		Client client = new Client();
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				client.createMainInterface();
			}
		});
	}// main

	// 构造函数，同时初始化线程池
	public Client() {
		// create a ThreadPool
		this.executorService = Executors.newCachedThreadPool();
		//读取配置文件
		Config.readConfigFile("client.config");
	}

	// 创建客户端主界面
	private void createMainInterface() {
		int windowWidth = 1065;//宽度
		int windowHeight = 650;//高度
		int mainPanelHeight = VideoPanel.getTotalHeight()+4*5;//行间距为5
		// user-interface
		try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
		}catch (Exception e) {e.printStackTrace();}
		mainFrame = new JFrame("流媒体客户端");
		mainFrame.setResizable(false);
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainFrame.setBounds(100, 0, windowWidth, windowHeight);
		
		/* frame的内容面板 */
		JPanel contentPane = new JPanel(null);//绝对布局
		mainFrame.setContentPane(contentPane);
		/*
		 * 内容面板上 可以添加 按钮面板 和 滚动面板（主面板）以及 菜单条
		 * */
		/*设置菜单控件*/
		JMenuBar menuBar = new JMenuBar();
		menuBar.setBounds(0, 0, windowWidth, 25);//大小和位置
		contentPane.add(menuBar);
		
		/*新建按钮上面板，存放分类列表、刷新按钮*/
		JPanel upPanel = new JPanel();//绝对布局
		/* 将按钮上面板加到内容面板上 */
		contentPane.add(upPanel);
		upPanel.setBounds(0, 25, windowWidth, 40);//大小和位置
		
		/*新建按钮下面板，存放播放按钮、上下翻页按钮*/
		JPanel downPanel = new JPanel();
		downPanel.setLayout(null);
		downPanel.setBounds(0, 565, windowWidth, 50);//大小和位置
		contentPane.add(downPanel);
		
		/*新建滚动面板,滚动面板唯一地做用是为主面板提供滚动条功能*/
		JScrollPane jScrollPane = new JScrollPane();
		/* 将滚动面板加到内容面板上 */
		contentPane.add(jScrollPane);
		/*滚动面板和内容面板（去掉菜单剩下的）一样高即可*/
		jScrollPane.setBounds(0, 65, windowWidth-4, 500);//大小和位置
		
		/*
		 * 新建主面板，并使之具备滚动功能
		 * */
		mainPanel = new JPanel(new FlowLayout
				(FlowLayout.LEFT, 5, 5));/*主面板上可以添加显示块，注意每行有5像素间隔*/
		jScrollPane.setViewportView(mainPanel);//具备滚动功能
		jScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		jScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		/*主面板不用锁定位置，因为有背后的滚动面板来协调，这里秩序根据实际内容的高度设置主面板高度即可*/
		mainPanel.setPreferredSize(new Dimension(windowWidth,mainPanelHeight));
		/*视频播放模式，直播和点播*/
		ButtonGroup buttonGroup = new ButtonGroup();
		JRadioButton liveRButton = new JRadioButton("直播");
		liveRButton.setFont(new Font("Dialog", Font.BOLD, 15));
		liveRButton.setBounds(89, 5, 69, 30);
		JRadioButton vodRButton = new JRadioButton("点播");
		vodRButton.setFont(new Font("Dialog", Font.BOLD, 15));
		vodRButton.setBounds(19, 5, 66, 30);
		buttonGroup.add(liveRButton);
		upPanel.setLayout(null);
		buttonGroup.add(vodRButton);
		upPanel.add(vodRButton);
		upPanel.add(liveRButton);
		liveRButton.setSelected(true);
		
		/*视频分类列表*/
		JLabel lblCategoryLabel = new JLabel("视频分类");
		lblCategoryLabel.setFont(new Font("Dialog", Font.BOLD, 15));
		lblCategoryLabel.setBounds(180, 5, 86, 30);
		upPanel.add(lblCategoryLabel);
		categoryListModel = new DefaultListModel<>();
		categoryList = new JList<>(categoryListModel);
		categoryList.setFont(new Font("Dialog", Font.BOLD, 20));
		categoryList.setBounds(270, 5, 600, 30);
		categoryList.setLayoutOrientation(JList.HORIZONTAL_WRAP);//水平显示，可以折行
		categoryList.setVisibleRowCount(1);//最多折两行
		categoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		upPanel.add(categoryList);
		
		/*
		 * 获取分类
		 * */
		getCategoryList();
		
		/*
		 * 显示块要追加到主面板mainPanel上，
		 * 不要搞成contentPane，contentPane使命到此完成
		 * */
		//更换分类时，刷新视频记录总数
		categoryList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				/*
				 * http://stackoverflow.com/questions/12461627/jlist-fires-valuechanged-twice-when-a-value-is-changed-via-mouse
				 * 点击选择某个表项会触发两次valueChanged事件，为了避免这样，
				 * 用了链接中的方法，也就是!e.getValueIsAdjusting()才进行处理
				 * ，这样就能只响应一次了。
				 * 但是，当模式切换，分类刷新时，!e.getValueIsAdjusting()没法拦住没用的事件
				 * 还是会响应两次，所以再加上categoryList.getSelectedValue来拦掉分类刷新时的无谓触发
				 * */
				if(!e.getValueIsAdjusting() && categoryList.getSelectedValue()!=null){
					String selectedCategory = categoryList.getSelectedValue();//取被选目录
					if(selectedCategory == null){//这是分类切换事件，所以这个检测提示应该不会发生
						JOptionPane.showMessageDialog(null, "这个提示应该不会发生"
								, "提示", JOptionPane.INFORMATION_MESSAGE);
						return;
					}
					//开线程
					TotalNumberCallable totalNumberCallable = new TotalNumberCallable(mode, selectedCategory);
					Future<Integer> future = executorService.submit(totalNumberCallable);
					try {
						totalNumber = future.get();
						int numberOfPages = (totalNumber-1)/videoListStep+1;//页数自1计算
						lblTotalNumber.setText("/"+numberOfPages+"页");
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}
		});
		/*
		 * 单选按钮，设置播放模式
		 * */
		/*注意，不管点击哪个按钮，两个按钮的itemStateChanged事件都会触发，
		 * 所以只需监听一个即可*/
		liveRButton.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange() == ItemEvent.DESELECTED)//liveRButton被取消，切换到点播
					mode = CommandWord.MODE_VOD;
				else								//liveRButton被选择，切换成直播
					mode = CommandWord.MODE_LIVE;
				getCategoryList();//重新获取分类
			}
		});
		
		/*
		 * 刷新视频列表
		 * */
		JButton btnRefreshVideoList = new JButton(new ImageIcon(((new ImageIcon(
	            "refresh.png").getImage().getScaledInstance(40, 40,
	                    java.awt.Image.SCALE_SMOOTH)))));
		btnRefreshVideoList.setBounds(900, 0, 40, 40);
		//btnRefreshVideoList.setContentAreaFilled(false);
		upPanel.add(btnRefreshVideoList);
		btnRefreshVideoList.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				/*获取被选择的列表是哪个，没选择不允许刷新视频列表*/
				String selectedCategory = categoryList.getSelectedValue();//取被选目录
				if(selectedCategory == null){//没选择分类
					JOptionPane.showMessageDialog(null, "请选择分类"
							, "提示", JOptionPane.INFORMATION_MESSAGE);
					return;
				}
				/*先刷新主面板*/
				mainPanel.removeAll();
				mainPanel.revalidate();
				mainPanel.repaint();
				
				/*复位“记忆被选择视频块”的全局变量为null*/
				SelectedVideoPanel.resetSelectedBlock();
				
				int currentPageNo = videoListStart/videoListStep+1;//页数自1计算
				tfPageNo.setText(String.valueOf(currentPageNo));
				VideoListCallable videoListCallable = new VideoListCallable(mode
						, selectedCategory,videoListStart,videoListStep,mainPanel);
				executorService.submit(videoListCallable);
			}
		});

		/*
		 * 首页
		 * */
		JButton btnFirst = new JButton(new ImageIcon(((new ImageIcon(
	            "firstpage.png").getImage().getScaledInstance(35, 35,
	                    java.awt.Image.SCALE_SMOOTH)))));
		btnFirst.setBounds(205, 8, 35, 35);
		downPanel.add(btnFirst);
		btnFirst.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				/*获取被选择的列表是哪个，没选择不允许翻页*/
				String selectedCategory = categoryList.getSelectedValue();//取被选目录
				if(selectedCategory == null){//没选择分类
					JOptionPane.showMessageDialog(null, "请选择分类"
							, "提示", JOptionPane.INFORMATION_MESSAGE);
					return;
				}
				/*先刷新主面板*/
				mainPanel.removeAll();
				mainPanel.revalidate();
				mainPanel.repaint();
				/*复位“记忆被选择视频块”的全局变量为null*/
				SelectedVideoPanel.resetSelectedBlock();
				videoListStart = 0;//起点0
				int currentPageNo = videoListStart/videoListStep+1;//页数自1计算
				tfPageNo.setText(String.valueOf(currentPageNo));
				
				VideoListCallable videoListCallable = new VideoListCallable(mode
						, selectedCategory,videoListStart,videoListStep,mainPanel);
				executorService.submit(videoListCallable);
			}
		});
		
		/*
		 * 末页
		 * */
		JButton btnLast = new JButton(new ImageIcon(((new ImageIcon(
	            "lastpage.png").getImage().getScaledInstance(35, 35,
	                    java.awt.Image.SCALE_SMOOTH)))));
		btnLast.setBounds(810, 8, 35, 35);
		downPanel.add(btnLast);
		btnLast.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				/*获取被选择的列表是哪个，没选择不允许翻页*/
				String selectedCategory = categoryList.getSelectedValue();//取被选目录
				if(selectedCategory == null){//没选择分类
					JOptionPane.showMessageDialog(null, "请选择分类"
							, "提示", JOptionPane.INFORMATION_MESSAGE);
					return;
				}
				/*先刷新主面板*/
				mainPanel.removeAll();
				mainPanel.revalidate();
				mainPanel.repaint();
				/*复位“记忆被选择视频块”的全局变量为null*/
				SelectedVideoPanel.resetSelectedBlock();
				
				//根据总记录数和步长，算出最后一页的起点
				int numberOfPages = (totalNumber-1)/videoListStep+1;//总页数
				videoListStart = (numberOfPages-1)*videoListStep;
				//至此，最后一页的起点已经确定
				int currentPageNo = videoListStart/videoListStep+1;//页数自1计算
				tfPageNo.setText(String.valueOf(currentPageNo));
				
				VideoListCallable videoListCallable = new VideoListCallable(mode
						, selectedCategory,videoListStart,videoListStep,mainPanel);
				executorService.submit(videoListCallable);
			}
		});
		
		/*
		 * 上一页
		 * */
		downPanel.setLayout(null);
		JButton btnPrevious = new JButton(new ImageIcon(((new ImageIcon(
	            "previouspage.gif").getImage().getScaledInstance(100, 35,
	                    java.awt.Image.SCALE_SMOOTH)))));
		btnPrevious.setBounds(320, 8, 100, 35);
		downPanel.add(btnPrevious);
		btnPrevious.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				/*获取被选择的列表是哪个，没选择不允许翻页*/
				String selectedCategory = categoryList.getSelectedValue();//取被选目录
				if(selectedCategory == null){//没选择分类
					JOptionPane.showMessageDialog(null, "请选择分类"
							, "提示", JOptionPane.INFORMATION_MESSAGE);
					return;
				}
				
				if(videoListStart == 0){
					JOptionPane.showMessageDialog(null, "已经是第一页"
							, "提示", JOptionPane.INFORMATION_MESSAGE);
					return;
				}else 
					;
				/*先刷新主面板*/
				mainPanel.removeAll();
				mainPanel.revalidate();
				mainPanel.repaint();
				
				/*复位“记忆被选择视频块”的全局变量为null*/
				SelectedVideoPanel.resetSelectedBlock();
				
				videoListStart -= videoListStep;//起点减少
				int currentPageNo = videoListStart/videoListStep+1;//页数自1计算
				tfPageNo.setText(String.valueOf(currentPageNo));
				
				VideoListCallable videoListCallable = new VideoListCallable(mode
						, selectedCategory,videoListStart,videoListStep,mainPanel);
				executorService.submit(videoListCallable);
			}
		});
		
		
		/*
		 * 下一页
		 * */
		JButton btnNext = new JButton(new ImageIcon(((new ImageIcon(
	            "nextpage.gif").getImage().getScaledInstance(100, 35,
	                    java.awt.Image.SCALE_SMOOTH)))));
		btnNext.setBounds(630, 8, 100, 35);
		downPanel.add(btnNext);
		btnNext.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				/*获取被选择的列表是哪个，没选择不允许翻页*/
				String selectedCategory = categoryList.getSelectedValue();//取被选目录
				if(selectedCategory == null){//没选择分类
					JOptionPane.showMessageDialog(null, "请选择分类"
							, "提示", JOptionPane.INFORMATION_MESSAGE);
					return;
				}
				
				if(videoListStart+videoListStep-1 >= totalNumber-1){
					JOptionPane.showMessageDialog(null, "已经是最后一页"
							, "提示", JOptionPane.INFORMATION_MESSAGE);
					return;
				}else 
					;
				
				/*先刷新主面板*/
				mainPanel.removeAll();
				mainPanel.revalidate();
				mainPanel.repaint();
				
				/*复位“记忆被选择视频块”的全局变量为null*/
				SelectedVideoPanel.resetSelectedBlock();
				videoListStart += videoListStep;//起点增加
				int currentPageNo = videoListStart/videoListStep+1;//页数自1计算
				tfPageNo.setText(String.valueOf(currentPageNo));
				
				VideoListCallable videoListCallable = new VideoListCallable(mode
						, selectedCategory,videoListStart,videoListStep,mainPanel);
				executorService.submit(videoListCallable);
			}
		});

		/*
		 * 播放视频
		 */
		JButton btnPlayVideo = new JButton(new ImageIcon(((new ImageIcon(
	            "play.png").getImage().getScaledInstance(50, 50,
	                    java.awt.Image.SCALE_SMOOTH)))));
		btnPlayVideo.setBounds(500, 0, 50, 50);
		//btnPlayVideo.setContentAreaFilled(false);
		downPanel.add(btnPlayVideo);
		btnPlayVideo.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			//检测是否有视频被选择
			VideoPanel selectedVideoPanel = SelectedVideoPanel.getSelectedVideoPanel();
			if(selectedVideoPanel == null){
				JOptionPane.showMessageDialog(null, "没有视频被选择"
						, "提示", JOptionPane.INFORMATION_MESSAGE);
				return;
			}

			if(mode==CommandWord.MODE_VOD){
				VodCallable vodCallable = new VodCallable(selectedVideoPanel);
				executorService.submit(vodCallable);
			}
			else{//live
				LiveCallable liveCallable = new LiveCallable(selectedVideoPanel);
				executorService.submit(liveCallable);
			}
			
		}// actionPerformed
		});// addActionListener
		
		/*
		 * 总记录数
		 * */
		lblTotalNumber = new JLabel("/ 页");
		lblTotalNumber.setFont(new Font("Dialog", Font.BOLD, 15));
		lblTotalNumber.setBounds(1000, 8, 50, 35);
		downPanel.add(lblTotalNumber);
		
		//跳页
		JButton btnJumpPage = new JButton("跳到");
		btnJumpPage.setFont(new Font("Dialog", Font.BOLD, 15));
		btnJumpPage.setBounds(890, 8, 66, 35);
		downPanel.add(btnJumpPage);
		
		//跳页输入框
		tfPageNo = new JTextField();
		tfPageNo.setFont(new Font("Dialog", Font.BOLD, 15));
		tfPageNo.setBounds(965, 8, 35, 35);
		downPanel.add(tfPageNo);
		tfPageNo.setColumns(10);
		
		btnJumpPage.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				/*获取被选择的列表是哪个，没选择不允许翻页*/
				String selectedCategory = categoryList.getSelectedValue();//取被选目录
				if(selectedCategory == null){//没选择分类
					JOptionPane.showMessageDialog(null, "请选择分类"
							, "提示", JOptionPane.INFORMATION_MESSAGE);
					return;
				}
				String pageNoText = tfPageNo.getText();
				if(pageNoText.equals("")){
					JOptionPane.showMessageDialog(null, "请输入页号"
							, "提示", JOptionPane.INFORMATION_MESSAGE);
					return;
				}
				
				int pageNo = Integer.valueOf(pageNoText);
				//pageNo是用户填入的页号，numberOfPages是总页数，都从1计算
				int numberOfPages = (totalNumber-1)/videoListStep+1;//页数自1计算
				if(pageNo > numberOfPages || pageNo < 1){
					JOptionPane.showMessageDialog(null, "页号过大或过小"
							, "提示", JOptionPane.INFORMATION_MESSAGE);
					return;
				}
				
				/*先刷新主面板*/
				mainPanel.removeAll();
				mainPanel.revalidate();
				mainPanel.repaint();
				
				/*复位“记忆被选择视频块”的全局变量为null*/
				SelectedVideoPanel.resetSelectedBlock();
				
				videoListStart = (pageNo-1) * videoListStep;//定位起点
				VideoListCallable videoListCallable = new VideoListCallable(mode
						, selectedCategory,videoListStart,videoListStep,mainPanel);
				executorService.submit(videoListCallable);
			}
		});
		
		/*
		 * 菜单设置
		 * */
		
		JMenu menuMain = new JMenu("主菜单");
		menuMain.setFont(new Font("Dialog", Font.BOLD, 18));
		menuBar.add(menuMain);
		
		JMenuItem menuItemExit = new JMenuItem("退出");
		menuItemExit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		menuItemExit.setFont(new Font("Dialog", Font.BOLD, 18));
		menuMain.add(menuItemExit);
		
		/*
		 * 显示主窗口
		 */
		mainFrame.setVisible(true);
	}// create

	//获取分类
	private void getCategoryList(){
		categoryListModel.removeAllElements();
		categoryList.revalidate();
		categoryList.repaint();
		CatagoryListCallable catagoryListCallable = new CatagoryListCallable(mode,categoryListModel);
		executorService.submit(catagoryListCallable);// 不需要收集返回值
	}
}// class