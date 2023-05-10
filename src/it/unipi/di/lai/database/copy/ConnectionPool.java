package it.unipi.di.lai.database.copy;



import java.sql.Connection;
import java.sql.SQLException;
import javax.naming.NamingException;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;

import it.unipi.di.lai.utils.XacmlWsConstants;



public class ConnectionPool {
	
	private static DataSource datasource = null;
	
	// restituisce una connessione libera dal pool di connessioni
	public static synchronized Connection getConnection() 
			throws NullPointerException, NamingException, SQLException, ClassNotFoundException {
		
		if(datasource == null)
			inizialize();
		
		return datasource.getConnection();
	}
	
	
	// inizializza il datasource
	private static void inizialize() throws NamingException, NullPointerException, ClassNotFoundException {
		
		if(datasource == null){
			
			PoolProperties p = new PoolProperties();
            p.setUrl("jdbc.url=jdbc:mysql://localhost:3306/"+XacmlWsConstants.DATABASE_NAME);
            p.setDriverClassName("com.mysql.jdbc.Driver");
            p.setUsername("root");
            p.setPassword("root");
            p.setJmxEnabled(true);
            p.setTestWhileIdle(false);
            p.setTestOnBorrow(true);
            p.setValidationQuery("SELECT 1");
            p.setTestOnReturn(false);
            p.setValidationInterval(30000);
            p.setTimeBetweenEvictionRunsMillis(30000);
            p.setMaxActive(100);
            p.setInitialSize(10);
            p.setMaxWait(10000);
            p.setRemoveAbandonedTimeout(60);
            p.setMinEvictableIdleTimeMillis(30000);
            p.setMinIdle(10);
            p.setLogAbandoned(true);
            p.setRemoveAbandoned(true);
            p.setJdbcInterceptors("org.apache.tomcat.jdbc.pool.interceptor.ConnectionState;"+
              "org.apache.tomcat.jdbc.pool.interceptor.StatementFinalizer");
            
            datasource = new DataSource();
            datasource.setPoolProperties(p); 
            
		}
	}
		
}
