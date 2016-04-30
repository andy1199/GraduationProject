package ServerSide;

import CommonPackage.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

class DatebaseQuery{
	/*
	 * 这个类的操作只有数据库查询，至于数据库的增删则放到DatabaseSync类里进行
	 * */
	private String dbName = "VideoInfo";
	private String dbUsername = "root";
	private String dbPassword = "MyNewPass4!";
	private Connection connection = null;
	private String sql = null;
	private Statement stmt = null;
	private ResultSet resultSet = null;
	private String url = "jdbc:mysql://localhost:3306/";//VideoInfo?"
            //+ "user=root&password=MyNewPass4!&useSSL=false";
	private String vodCategoryTable = "vodcategory";
	private String liveCategoryTable = "livecategory";
	private String vodTable = "vod";
	private String liveTable = "live";
	
	//构造函数
	public DatebaseQuery() {
		Config config = new Config();
		this.dbName = config.readConfig("dbName");
		this.dbUsername = config.readConfig("dbUsername");
		this.dbPassword = config.readConfig("dbPassword");
		this.url = url
				+this.dbName
				+"?user="+this.dbUsername
				+"&password="+this.dbPassword
				+"&useSSL=false";
		this.vodCategoryTable = config.readConfig("vodCategoryTable");
		this.liveCategoryTable = config.readConfig("liveCategoryTable");
		this.vodTable = config.readConfig("vodTable");
		this.liveTable = config.readConfig("liveTable");
	}
	
	/*
	 * 取出所有分类名称
	 * */
	public ArrayList<String> getCategory(int mode) {
		ArrayList<String> categoryList = new ArrayList<>();
		try {
			Class.forName("com.mysql.jdbc.Driver");
			System.out.println("成功加载MySQL驱动程序");
			connection = DriverManager.getConnection(url);
			stmt = connection.createStatement();
			sql = "select * from ";
			if(mode == DefineConstant.MODE_VOD)
				sql += vodCategoryTable;
			else sql += liveCategoryTable;
			resultSet = stmt.executeQuery(sql);
			while (resultSet.next()) {
				System.out.println(resultSet.getString(1));
				categoryList.add(resultSet.getString(1));
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}finally {
			try {
				if(resultSet!=null)resultSet.close();
				if(stmt!=null)stmt.close();
				if(connection!=null)connection.close();
			} catch (SQLException e) {e.printStackTrace();}
		}//finally
		return categoryList;
	}

	/*
	 * 取出指定数量的视频记录
	 */
	public ArrayList<VideoInfo> getVideoSet(int mode, String category, 
							int videoDisplayStart,int videoDisplayStep) {
		ArrayList<VideoInfo> videoInfoList = new ArrayList<>();
		try {
			Class.forName("com.mysql.jdbc.Driver");
			System.out.println("成功加载MySQL驱动程序");
			connection = DriverManager.getConnection(url);
			stmt = connection.createStatement();
			//SELECT * FROM vod WHERE Category="游戏" LIMIT 0,2
			sql = "SELECT * FROM ";//拼接初始化
			if(mode == DefineConstant.MODE_VOD)
				sql += vodTable;
			else sql += liveTable;
			sql += " WHERE Category="+"\""+category+"\" "
						+"LIMIT "+videoDisplayStart+","+videoDisplayStep;
			resultSet = stmt.executeQuery(sql);
			while (resultSet.next()) {
				VideoInfo videoInfo = new VideoInfo(
						resultSet.getString("VideoName"),
						resultSet.getString("Duration"), 
						resultSet.getString("Resolution"), 
						resultSet.getString("Category"), 
						resultSet.getString("RelativePurePath"));
				videoInfoList.add(videoInfo);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}finally {
			try {
				if(resultSet!=null)resultSet.close();
				if(stmt!=null)stmt.close();
				if(connection!=null)connection.close();
			} catch (SQLException e) {e.printStackTrace();}
		}//finally
		return videoInfoList;//包含了videoDisplayStep个视频的详细信息，每个视频占一条
	}

}