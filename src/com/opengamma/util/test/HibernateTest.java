package com.opengamma.util.test;


import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public abstract class HibernateTest {
  private static final Logger s_logger = LoggerFactory.getLogger(HibernateTest.class);
  private static final String PROPS_FILE_NAME = "tests.properties";
  protected ApplicationContext _context;
  private static Properties _props;
  private static String _jdbcDriverClass;
  private static boolean _isDerby;
  private static int testCount = 0;
  
  private static void recursiveDelete(File file) {
    if (file.isDirectory()) {
      File[] list = file.listFiles();
      for (File entry : list) {
        if (entry.isDirectory()) {
          recursiveDelete(entry);
        }
        if (!entry.delete()) {
          s_logger.warn("Could not delete file:"+file.getAbsolutePath());
          //throw new OpenGammaRuntimeException("Could not delete file:"+entry.getAbsolutePath());
        } else {
          System.err.println("Deleted "+entry.getAbsolutePath());
        }
      }
    }
    if (!file.delete()) {
      s_logger.warn("Could not delete file:"+file.getAbsolutePath());   
    } else {
      System.err.println("Deleted "+file.getAbsolutePath());
    }
    
  }
  
  private String getDBUrl(boolean createFrom) {
    if (_isDerby) {
      File blankDbDir = new File("blank");
      String core = "/"+this.getClass().getSimpleName()+"-test-"+testCount;
      if (createFrom) {
        return core + ";createFrom="+blankDbDir.getAbsolutePath();
      } else {
        return core;
      }
    
    } else {
      return "/OpenGammaTests"; 
    }
  }
  
  @BeforeClass
  public static void setUpClass() throws Exception {
    Properties props = new Properties();
    File file = new File(PROPS_FILE_NAME);
    System.err.println(file.getAbsoluteFile());
    props.load(new FileInputStream(file)); 
    _props = props;
    
    _jdbcDriverClass = ((String) _props.get("jdbc.driver.classname")).trim();
    _isDerby = _jdbcDriverClass.equals("org.apache.derby.jdbc.EmbeddedDriver");
    
    if (_isDerby) {
      recursiveDelete(new File("derby-db"));
    }
    
    Class.forName(_jdbcDriverClass).newInstance(); // load driver.
  }
  
  public abstract String getConfigLocation();
  
  @Before
  public void setUp() throws Exception {
    String createFromUrl = _props.getProperty("jdbc.url") + getDBUrl(true);
    System.err.println("Connecting with data source URL "+createFromUrl);
    
    String user = _props.getProperty("jdbc.username");
    String password =  _props.getProperty("jdbc.password");

    if (_isDerby) {
      Connection conn = DriverManager.getConnection(createFromUrl, user, password);
      // this will create a copy of the blank database, using
      conn.close(); // that should do the copy...  we do it like this because I'm unsure if we can be sure the App Context will release the resources if we used that.
      System.err.println("closed connection, starting App Context");
    }
    
    System.setProperty("jdbc.url", _props.getProperty("jdbc.url") + getDBUrl(false)); 
    System.setProperty("jdbc.driver.classname", _props.getProperty("jdbc.driver.classname"));
    System.setProperty("jdbc.username", _props.getProperty("jdbc.username"));
    System.setProperty("jdbc.password", _props.getProperty("jdbc.password"));
    System.setProperty("hibernate.dialect", _props.getProperty("hibernate.dialect"));
    
    // the idea is that this SYSTEM property (the others above are not system props) is picked up by the PropertyPlaceholderConfigurer during startup
    // and injected into the 
    _context = new ClassPathXmlApplicationContext(getConfigLocation());

    BasicDataSource dataSource = (BasicDataSource) _context.getBean("myDataSource");
    System.err.println(ToStringBuilder.reflectionToString(dataSource));
    
    testCount++;
  }
}
