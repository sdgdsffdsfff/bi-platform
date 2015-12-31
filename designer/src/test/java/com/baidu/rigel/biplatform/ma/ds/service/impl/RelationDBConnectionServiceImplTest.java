package com.baidu.rigel.biplatform.ma.ds.service.impl;

import java.sql.Connection;
import java.sql.DriverManager;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.rigel.biplatform.ac.util.AesUtil;
import com.baidu.rigel.biplatform.ma.ds.exception.DataSourceConnectionException;
import com.baidu.rigel.biplatform.ma.ds.service.DataSourceConnectionService;
import com.baidu.rigel.biplatform.ma.model.ds.DataSourceDefine;
import com.baidu.rigel.biplatform.ma.model.ds.DataSourceType;

/**
 * 测试RelationDBConnectionServiceImpl
 * @author jiangyichao
 *
 */
public class RelationDBConnectionServiceImplTest {

	/**
	 * 日志对象
	 */
	private static final Logger LOG = LoggerFactory.getLogger(RelationDBConnectionServiceImplTest.class);
	/**
	 * 数据库连接服务
	 */
	private DataSourceConnectionService<Connection> dsConnService = new RelationDBConnectionServiceImpl();
	
	/**
	 * 
	 */
	@Test
	public void test() throws Exception {
    	// 数据库连接基本信息，并构建内存数据库H2
    	String username = "test";
    	String password = "test";
    	String dbInstance = "testDB";
    	String securityKey = "0000000000000000";
    	String passwordEncrypt = AesUtil.getInstance().encryptAndUrlEncoding(password, securityKey);
    	String url = "jdbc:h2:mem:" + dbInstance + ";";
    	try {
    		Class.forName("org.h2.Driver");
    		Connection conn = DriverManager.getConnection(url,username,password); 
    		if (conn != null) {
    			LOG.error("get H2 datasource by username:" + username);    			
    		}
    	} catch (Exception e) {
    		e.printStackTrace();
    	} 
    	
    	DataSourceDefine ds = new DataSourceDefine();
    	DataSourceType.H2.setPrefix("jdbc:h2:mem://");
    	ds.setDataSourceType(DataSourceType.H2);
    	ds.setDbInstance(dbInstance);
    	ds.setDbPwd(passwordEncrypt);
    	ds.setDbUser(username);
    	ds.setHostAndPort("127.0.0.1:3306");
    	// 测试打开和关闭连接
    	Connection conn = dsConnService.createConnection(ds, securityKey);
    	Assert.assertNotNull(conn);
    	Assert.assertTrue(dsConnService.closeConnection(conn));
    	
    	try {
    		dsConnService.getDataSourceConnUrl(null);    		
    	} catch (DataSourceConnectionException e) {
    		Assert.assertNotNull(e);
    	}
    	
    	String connUrl =  DataSourceType.H2.getPrefix() + ds.getHostAndPort() + 
    			DataSourceType.H2.getDiv() + ds.getDbInstance();
    	Assert.assertEquals(connUrl, dsConnService.getDataSourceConnUrl(ds));
    	
    	
    	// 测试数据源连接是否有效   	
    	Assert.assertTrue(dsConnService.isValidateDataSource(ds, securityKey));
    	DataSourceType.H2.setDriver("this is not database driver");
    	ds.setDataSourceType(DataSourceType.H2);
    	Assert.assertFalse(dsConnService.isValidateDataSource(ds, securityKey));
    	// 设置正确的数据库前缀
    	DataSourceType.H2.setPrefix("jdbc:h2:tcp://");  
    	DataSourceType.H2.setDriver("org.h2.Driver");
	}
	
	/**
	 * 
	 */
	@Test
	public void testGetConnUrl() throws Exception {
    	DataSourceDefine ds = new DataSourceDefine();
    	ds.setDataSourceType(DataSourceType.H2);
    	ds.setDbInstance("test");
    	ds.setHostAndPort("127.0.0.1:3306");
    	String connUrl =  DataSourceType.H2.getPrefix() + ds.getHostAndPort() + 
    			DataSourceType.H2.getDiv() + ds.getDbInstance();
    	Assert.assertEquals(connUrl, dsConnService.getDataSourceConnUrl(ds));
    	
    	ds.setDataSourceType(DataSourceType.MYSQL);
    	ds.setDbInstance("test");
    	ds.setHostAndPort("127.0.0.1:3306");
    	ds.setEncoding("utf8");
    	connUrl =  DataSourceType.MYSQL.getPrefix() + ds.getHostAndPort() + 
    			DataSourceType.MYSQL.getDiv() + ds.getDbInstance() + "?useUniCode=true&characterEncoding=" + ds.getEncoding()
    			+ "&zeroDateTimeBehavior=convertToNull&noAccessToProcedureBodies=true";
    	Assert.assertEquals(connUrl, dsConnService.getDataSourceConnUrl(ds));
    	
	}
	
	/**
	 * 
	 */
	@Test
	public void testParseToDataSourceInfo() throws Exception {
    	// 数据库连接基本信息，并构建内存数据库H2
    	String username = "test";
    	String password = "test";
    	String dbInstance = "testDB";
    	String securityKey = "0000000000000000";
    	String passwordEncrypt = AesUtil.getInstance().encryptAndUrlEncoding(password, securityKey);

    	DataSourceDefine ds = new DataSourceDefine();
    	DataSourceType.H2.setPrefix("jdbc:h2:mem://");
    	ds.setDataSourceType(DataSourceType.H2);
    	ds.setDbInstance(dbInstance);
    	ds.setDbPwd(passwordEncrypt);
    	ds.setDbUser(username);
    	ds.setHostAndPort("127.0.0.1:3306");
    	
    	Assert.assertNotNull(dsConnService.parseToDataSourceInfo(ds, securityKey));
    	
    	ds.setDataSourceType(DataSourceType.MYSQL);
    	ds.setDbInstance(dbInstance);
    	ds.setDbPwd(passwordEncrypt);
    	ds.setDbUser(username);
    	ds.setHostAndPort("127.0.0.1:3306");
    	
    	Assert.assertNotNull(dsConnService.parseToDataSourceInfo(ds, securityKey));
    	
    	
    	ds.setDataSourceType(DataSourceType.ORACLE);
    	ds.setDbInstance(dbInstance);
    	ds.setDbPwd(passwordEncrypt);
    	ds.setDbUser(username);
    	ds.setHostAndPort("127.0.0.1:3306");
    	
    	Assert.assertNotNull(dsConnService.parseToDataSourceInfo(ds, securityKey));
    	
    	
    	ds.setDataSourceType(DataSourceType.TXT);
    	ds.setDbInstance(dbInstance);
    	ds.setDbPwd(passwordEncrypt);
    	ds.setDbUser(username);
    	ds.setHostAndPort("127.0.0.1:3306");
    	
    	Assert.assertNotNull(dsConnService.parseToDataSourceInfo(ds, securityKey));
        // 设置正确的数据库前缀
        DataSourceType.H2.setPrefix("jdbc:h2:tcp://");  
        DataSourceType.H2.setDriver("org.h2.Driver");
	}
}
