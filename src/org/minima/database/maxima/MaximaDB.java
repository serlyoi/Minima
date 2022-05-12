package org.minima.database.maxima;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.minima.objects.base.MiniData;
import org.minima.utils.MinimaLogger;
import org.minima.utils.SqlDB;

public class MaximaDB extends SqlDB {

	/**
	 * PreparedStatements
	 */
	PreparedStatement SQL_INSERT_MAXIMA_HOST 		= null;
	PreparedStatement SQL_UPDATE_MAXIMA_HOST 		= null;
	PreparedStatement SQL_SELECT_MAXIMA_HOST 		= null;
	PreparedStatement SQL_SELECT_ALL_HOSTS 			= null;
	PreparedStatement SQL_DELETE_HOST 				= null;
	PreparedStatement SQL_DELETE_OLD_HOSTS 			= null;
	
	PreparedStatement SQL_INSERT_MAXIMA_CONTACT 	= null;
	PreparedStatement SQL_SELECT_ALL_CONTACTS 		= null;
	PreparedStatement SQL_SELECT_CONTACT 			= null;
	PreparedStatement SQL_UPDATE_CONTACT 			= null;
	
	/**
	 * A Cached list
	 */
	boolean mHostCacheValid 			= false;
	ArrayList<MaximaHost> mCachedHosts 	= null;
	
	public MaximaDB() {
		super();
	}
	
	@Override
	protected void createSQL() {
		try {
			
			//Create the various tables..
			Statement stmt = mSQLConnection.createStatement();

			//Create hosts table
			String hosts = "CREATE TABLE IF NOT EXISTS `hosts` ("
							+ "  `id` IDENTITY PRIMARY KEY,"
							+ "  `host` varchar(255) NOT NULL UNIQUE,"
							+ "  `publickey` blob NOT NULL,"
							+ "  `privatekey` blob NOT NULL,"
							+ "  `connected` int NOT NULL,"
							+ "  `lastseen` bigint NOT NULL"
							+ ")";
			
			//Run it..
			stmt.execute(hosts);

			//Create contacts table
			String contacts = "CREATE TABLE IF NOT EXISTS `contacts` ("
							+ "  `id` IDENTITY PRIMARY KEY,"
							+ "  `name` varchar(255) NOT NULL,"
							+ "  `extradata` blob NOT NULL,"
							+ "  `publickey` varchar(512) NOT NULL UNIQUE,"
							+ "  `currentaddress` varchar(512) NOT NULL,"
							+ "  `myaddress` varchar(512) NOT NULL"
							+ ")";
			
			//Run it..
			stmt.execute(contacts);
			
			//All done..
			stmt.close();
			
			//Create some prepared statements..
			SQL_SELECT_MAXIMA_HOST	= mSQLConnection.prepareStatement("SELECT * FROM hosts WHERE host=?");
			SQL_SELECT_ALL_HOSTS	= mSQLConnection.prepareStatement("SELECT * FROM hosts");
			SQL_INSERT_MAXIMA_HOST	= mSQLConnection.prepareStatement("INSERT IGNORE INTO hosts ( host, publickey, privatekey, connected, lastseen ) VALUES ( ?, ? , ? ,? ,? )");
			SQL_UPDATE_MAXIMA_HOST	= mSQLConnection.prepareStatement("UPDATE hosts SET publickey=?, privatekey=?, connected=?, lastseen=? WHERE host=?");
			SQL_DELETE_HOST			= mSQLConnection.prepareStatement("DELETE FROM hosts WHERE host=?");
			SQL_DELETE_OLD_HOSTS	= mSQLConnection.prepareStatement("DELETE FROM hosts WHERE lastseen < ?");
			
			SQL_INSERT_MAXIMA_CONTACT 	= mSQLConnection.prepareStatement("INSERT IGNORE INTO contacts "
					+ "( name, extradata, publickey, currentaddress, myaddress ) VALUES ( ?, ?, ?, ?, ? )");
			SQL_SELECT_ALL_CONTACTS		= mSQLConnection.prepareStatement("SELECT * FROM contacts");
			SQL_SELECT_CONTACT			= mSQLConnection.prepareStatement("SELECT * FROM contacts WHERE publickey=?");
			
			SQL_UPDATE_CONTACT			= mSQLConnection.prepareStatement("UPDATE contacts SET "
					+ "name=?, extradata=?, currentaddress=?, myaddress=? WHERE publickey=?");
			
			//Load all the hosts
			getAllHosts();
			
		} catch (SQLException e) {
			MinimaLogger.log(e);
		}
	}

	public synchronized boolean newHost(MaximaHost zHost) {
		try {
			
			//Cache no longer valid
			mHostCacheValid = false;
			
			//Get the Query ready
			SQL_INSERT_MAXIMA_HOST.clearParameters();
		
			//Set main params
			SQL_INSERT_MAXIMA_HOST.setString(1, zHost.getHost());
			SQL_INSERT_MAXIMA_HOST.setBytes(2, zHost.getPublicKey().getBytes());
			SQL_INSERT_MAXIMA_HOST.setBytes(3, zHost.getPrivateKey().getBytes());
			SQL_INSERT_MAXIMA_HOST.setInt(4, 1);
			SQL_INSERT_MAXIMA_HOST.setLong(5, zHost.getLastSeen());
			
			//Do it.
			SQL_INSERT_MAXIMA_HOST.execute();
			
			return true;
			
		} catch (SQLException e) {
			MinimaLogger.log(e);
		}
		
		return false;
	}
	
	public synchronized MaximaHost loadHost(String zHost) {
		
		//Is it cached
		if(mHostCacheValid) {
			
			//Cycle through our current list
			ArrayList<MaximaHost> allhosts = getAllHosts();
			for(MaximaHost host : allhosts) {
				if(host.getHost().equals(zHost)) {
					return host;
				}
			}
			
			return null;
		}
		
		try {
			
			//Set search params
			SQL_SELECT_MAXIMA_HOST.clearParameters();
			SQL_SELECT_MAXIMA_HOST.setString(1, zHost);
			
			//Run the query
			ResultSet rs = SQL_SELECT_MAXIMA_HOST.executeQuery();
			
			//Is there a valid result.. ?
			if(rs.next()) {
				
				//Get the details..
				MaximaHost mxhost = new MaximaHost(rs);
				
				return mxhost;
			}
			
		} catch (SQLException e) {
			MinimaLogger.log(e);
		}
		
		return null;
	}
	
	public synchronized MaximaHost loadHostFromPublicKey(String zPublicKey) {
		
		//Cycle through our current list
		ArrayList<MaximaHost> allhosts = getAllHosts();
		for(MaximaHost host : allhosts) {
			if(host.getPublicKey().to0xString().equals(zPublicKey)) {
				return host;
			}
		}
		
		return null;
	}
	
	public synchronized ArrayList<MaximaHost> getAllHosts() {
		
		//Is it cached
		if(mHostCacheValid) {
			return mCachedHosts;
		}
		
		//Get the current list
		ArrayList<MaximaHost> hosts = new ArrayList<>();
		
		try {
			
			//Set Search params
			SQL_SELECT_ALL_HOSTS.clearParameters();
			
			//Run the query
			ResultSet rs = SQL_SELECT_ALL_HOSTS.executeQuery();
			
			//Multiple results
			while(rs.next()) {
				
				//Get the details..
				MaximaHost mxhost = new MaximaHost(rs);
				
				//Add to our list
				hosts.add(mxhost);
			}
			
		} catch (SQLException e) {
			MinimaLogger.log(e);
		}
		
		//Now the cache is valid
		mCachedHosts 	= hosts;
		mHostCacheValid 	= true;
		
		return hosts;
	}
	
	public synchronized boolean updateHost(MaximaHost zMXHost) {
		
		try {
			
			mHostCacheValid 	= false;
			
			//Set search params
			SQL_UPDATE_MAXIMA_HOST.clearParameters();
			
			SQL_UPDATE_MAXIMA_HOST.setBytes(1, zMXHost.getPublicKey().getBytes());
			SQL_UPDATE_MAXIMA_HOST.setBytes(2, zMXHost.getPrivateKey().getBytes());
			SQL_UPDATE_MAXIMA_HOST.setInt(3, zMXHost.getConnected());
			SQL_UPDATE_MAXIMA_HOST.setLong(4, zMXHost.getLastSeen());
			SQL_UPDATE_MAXIMA_HOST.setString(5, zMXHost.getHost());
			
			//Run the query
			SQL_UPDATE_MAXIMA_HOST.execute();
			
			return true;
			
		} catch (SQLException e) {
			MinimaLogger.log(e);
		}
		
		return false;
	}
	
	public synchronized boolean deleteHost(String zHost) {
		
		try {
			
			mHostCacheValid 	= false;
			
			//Set search params
			SQL_DELETE_HOST.clearParameters();
			
			SQL_DELETE_HOST.setString(1, zHost);
			
			//Run the query
			SQL_DELETE_HOST.execute();
			
			return true;
			
		} catch (SQLException e) {
			MinimaLogger.log(e);
		}
		
		return false;
	}
	
	public synchronized boolean newContact(MaximaContact zContact) {
		try {
			
			//Get the Query ready
			SQL_INSERT_MAXIMA_CONTACT.clearParameters();
		
			//Set main params
			SQL_INSERT_MAXIMA_CONTACT.setString(1, zContact.getName());
			SQL_INSERT_MAXIMA_CONTACT.setBytes(2, zContact.getExtraData().getBytes());
			SQL_INSERT_MAXIMA_CONTACT.setString(3, zContact.getPublicKey());
			SQL_INSERT_MAXIMA_CONTACT.setString(4, zContact.getCurrentAddress());
			SQL_INSERT_MAXIMA_CONTACT.setString(5, zContact.getMyAddress());
			
			//Do it.
			SQL_INSERT_MAXIMA_CONTACT.execute();
			
			return true;
			
		} catch (SQLException e) {
			MinimaLogger.log(e);
		}
		
		return false;
	}
	
	public synchronized ArrayList<MaximaContact> getAllContacts() {
		
		//Get the current list
		ArrayList<MaximaContact> contacts = new ArrayList<>();
		
		try {
			
			//Set Search params
			SQL_SELECT_ALL_CONTACTS.clearParameters();
			
			//Run the query
			ResultSet rs = SQL_SELECT_ALL_CONTACTS.executeQuery();
			
			//Multiple results
			while(rs.next()) {
				
				//Get the details..
				MaximaContact mxcontact = new MaximaContact(rs);
				
				//Add to our list
				contacts.add(mxcontact);
			}
			
		} catch (SQLException e) {
			MinimaLogger.log(e);
		}
		
		return contacts;
	}

	public synchronized MaximaContact loadContact(String zPublicKey) {
		
		try {
			
			//Set search params
			SQL_SELECT_CONTACT.clearParameters();
			SQL_SELECT_CONTACT.setString(1, zPublicKey);
			
			//Run the query
			ResultSet rs = SQL_SELECT_CONTACT.executeQuery();
			
			//Is there a valid result.. ?
			if(rs.next()) {
				
				//Get the details..
				MaximaContact mxcontact = new MaximaContact(rs);
				
				return mxcontact;
			}
			
		} catch (SQLException e) {
			MinimaLogger.log(e);
		}
		
		return null;
	}
	
	public synchronized boolean updateContact(MaximaContact zContact) {
		
		try {
			
			//Set search params
			SQL_UPDATE_CONTACT.clearParameters();
			
			SQL_UPDATE_CONTACT.setString(1, zContact.getName());
			SQL_UPDATE_CONTACT.setBytes(2, zContact.getExtraData().getBytes());
			SQL_UPDATE_CONTACT.setString(3, zContact.getCurrentAddress());
			SQL_UPDATE_CONTACT.setString(4, zContact.getMyAddress());
			
			SQL_UPDATE_CONTACT.setString(5, zContact.getPublicKey());
			
			//Run the query
			SQL_UPDATE_CONTACT.execute();
			
			return true;
			
		} catch (SQLException e) {
			MinimaLogger.log(e);
		}
		
		return false;
	}
}
