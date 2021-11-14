package scc.layers;

import java.util.Iterator;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosPatchOperations;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.util.CosmosPagedIterable;

import scc.data.*;

public class CosmosDBLayer {

	private static final String USERS_CONTAINER = "Users";
	private static final String MESSAGES_CONTAINER = "Messages";
	private static final String CHANNELS_CONTAINER = "Channels";

	private static CosmosDBLayer instance;

	private String dbName;
	private CosmosClient client;
	private CosmosDatabase db;
	private CosmosContainer currentContainer;

	public static synchronized CosmosDBLayer getInstance() {
		if (instance != null)
			return instance;

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

	public CosmosDBLayer(CosmosClient client, String dbName) {
		this.client = client;
		this.dbName = dbName;
	}

	private synchronized <T> void init(Class<T> type) {
		if (db == null)
			db = client.getDatabase(dbName);

		if (type.equals(UserDAO.class))
			currentContainer = db.getContainer(USERS_CONTAINER);
		else if (type.equals(MessageDAO.class))
			currentContainer = db.getContainer(MESSAGES_CONTAINER);
		else if (type.equals(ChannelDAO.class))
			currentContainer = db.getContainer(CHANNELS_CONTAINER);
	}

	public <T> CosmosItemResponse<Object> delById(String id, Class<T> type) {
		init(type);
		PartitionKey key = new PartitionKey(id);
		return currentContainer.deleteItem(id, key, new CosmosItemRequestOptions());
	}

	public <T> CosmosItemResponse<Object> del(T item) {
		init(item.getClass());
		return currentContainer.deleteItem(item, new CosmosItemRequestOptions());
	}

	public <T> CosmosItemResponse<T> put(T item) {
		init(item.getClass());
		return currentContainer.createItem(item);
	}

	public <T> T getById(String id, Class<T> type) {
		init(type);
		String container =type.getSimpleName().replace("DAO", "s");
		CosmosPagedIterable<T> response = currentContainer.queryItems("SELECT * FROM " + container + " WHERE " + container + ".id=\"" + id + "\"", new CosmosQueryRequestOptions(), type);
        Iterator<T> it = response.iterator();
        if(it.hasNext())
            return it.next();
		return null;
	}

	public <T> CosmosPagedIterable<T> getAll(Class<T> type) {
		init(type);
		String container = type.getSimpleName().replace("DAO", "s");
		return currentContainer.queryItems("SELECT * FROM " + container, new CosmosQueryRequestOptions(), type);
	}

	public <T> CosmosItemResponse<T> patchAdd(String id, Class<T> type, String field, String change) {
		init(type);
		
		PartitionKey key = new PartitionKey(id);

		CosmosPatchOperations patchOps = CosmosPatchOperations.create().add(field+"/-", change);
		return currentContainer.patchItem(id, key, patchOps, type);
	}

	public <T> CosmosItemResponse<T> patchRemove(String id, Class<T> type, String field, int index) {
		init(type);
		
		PartitionKey key = new PartitionKey(id);

		//CosmosItemResponse<T> item = currentContainer.readItem(id, key, type);
		CosmosPatchOperations patchOps = CosmosPatchOperations.create().remove(field+"/"+index);
		return currentContainer.patchItem(id, key, patchOps, type);
	}

	public <T> void updateDelUserMessages(String id, Class<T> type) {
		init(type);	
		currentContainer.queryItems("UPDATE messages set userIDSender = -1 where messages.userIDSender == "+id, new CosmosQueryRequestOptions(), type);
	}

	public void close() {
		client.close();
	}

}
