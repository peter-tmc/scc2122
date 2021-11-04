package scc.data;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.util.CosmosPagedIterable;

public class CosmosDBLayer {
//	private static final String AZURE_PROPS_LOCATION = "azurekeys-westeurope.props";
	private static final String USERS_CONTAINER = "Users";
	private static final String MESSAGES_CONTAINER = "Messages";
	private static final String CHANNELS_CONTAINER = "Channels";
	

	private static CosmosDBLayer instance;

	public static synchronized CosmosDBLayer getInstance() {
		 if( instance != null)
			return instance;
		/*
		Properties properties = null;
		try {
			InputStream input = new FileInputStream(AZURE_PROPS_LOCATION);
			properties = new Properties();
			properties.load(input);
		} catch(Exception e) {
			System.out.println(e.getMessage());
		}
		
		String connURL = properties.getProperty("COSMOSDB_URL");
		String DBKey = properties.getProperty("COSMOSDB_KEY");
		DBName = properties.getProperty("COSMOSDB_DATABASE");
 */
		String DBKey = System.getenv("COSMOSDB_KEY");
		String dbName = System.getenv("COSMOSDB_DATABASE");
 		String connURL = System.getenv("COSMOSDB_URL");
		

		CosmosClient client = new CosmosClientBuilder()
		         .endpoint(connURL)
		         .key(DBKey)
		         .gatewayMode()		// comment this if not to use direct mode
		         .consistencyLevel(ConsistencyLevel.SESSION)
		         .connectionSharingAcrossClientsEnabled(true)
		         .contentResponseOnWriteEnabled(true)
		         .buildClient();
		instance = new CosmosDBLayer(client, dbName);
		return instance;
		
	}

	private String dbName;
	private CosmosClient client;
	private CosmosDatabase db;
	private CosmosContainer currentContainer;
	
	public CosmosDBLayer(CosmosClient client, String dbName) {
		this.client = client;
		this.dbName=dbName;
	}
	
	private synchronized void init(String container) {
		if( db == null)
			db = client.getDatabase(dbName);
		currentContainer = db.getContainer(container);
		
	}

/*
	public <T> CosmosItemResponse<T> delById(String id, Class<T> type) {
		init(USERS_CONTAINER);
		PartitionKey key = new PartitionKey( id);
		//query q muda o id do user q foi eliminado e mete o id do user default -1 
		currentContainer.queryItems("UPDATE messages set userIDSender = -1 where messages.userIDSender == userID", new CosmosQueryRequestOptions(), UserDAO.class);
		return type.cast(currentContainer.deleteItem(id, key, new CosmosItemRequestOptions()));
	}
*/
	public CosmosItemResponse<Object> del(UserDAO user) {
		init(USERS_CONTAINER);
		return currentContainer.deleteItem(user, new CosmosItemRequestOptions());
	}

	public CosmosItemResponse<UserDAO> put(UserDAO user) {
		init(USERS_CONTAINER);
		return currentContainer.createItem(user);
	}
	
	public CosmosPagedIterable<UserDAO> getById(String id) {
		init(USERS_CONTAINER);
		return currentContainer.queryItems("SELECT * FROM Users WHERE Users.id=\"" + id + "\"", new CosmosQueryRequestOptions(), UserDAO.class);
	}

	public CosmosPagedIterable<UserDAO> get() {
		init(USERS_CONTAINER);
		return currentContainer.queryItems("SELECT * FROM Users ", new CosmosQueryRequestOptions(), UserDAO.class);
	}

	public void close() {
		client.close();
	}
	
}
