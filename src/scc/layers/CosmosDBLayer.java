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

	private static final String DELETED_USERS_CONTAINER = "DeletedUsers";
	private static final String DELETED_MESSAGES_CONTAINER = "DeletedMessages";
	private static final String DELETED_CHANNELS_CONTAINER = "DeletedChannels";

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

		CosmosClient client = new CosmosClientBuilder().endpoint(connURL).key(DBKey).gatewayMode() // comment this if
																									// not to use direct
																									// mode
				.consistencyLevel(ConsistencyLevel.SESSION).connectionSharingAcrossClientsEnabled(true)
				.contentResponseOnWriteEnabled(true).buildClient();

		instance = new CosmosDBLayer(client, dbName);
		return instance;
	}

	public CosmosDBLayer(CosmosClient client, String dbName) {
		this.client = client;
		this.dbName = dbName;
	}

	private synchronized <T> void init(Class<T> type, boolean isDeleted) {
		if (db == null)
			db = client.getDatabase(dbName);
		if (!isDeleted) {
			if (type.equals(UserDAO.class))
				currentContainer = db.getContainer(USERS_CONTAINER);
			else if (type.equals(MessageDAO.class))
				currentContainer = db.getContainer(MESSAGES_CONTAINER);
			else if (type.equals(ChannelDAO.class))
				currentContainer = db.getContainer(CHANNELS_CONTAINER);
		} else {
			if (type.equals(UserDAO.class))
				currentContainer = db.getContainer(DELETED_USERS_CONTAINER);
			else if (type.equals(MessageDAO.class))
				currentContainer = db.getContainer(DELETED_MESSAGES_CONTAINER);
			else if (type.equals(ChannelDAO.class))
				currentContainer = db.getContainer(DELETED_CHANNELS_CONTAINER);
		}
	}

	public <T> CosmosItemResponse<Object> delById(String id, String partKey, Class<T> type, boolean isFromDeleted) {
		init(type, isFromDeleted);
		PartitionKey key = new PartitionKey(partKey);
		return currentContainer.deleteItem(id, key, new CosmosItemRequestOptions());
	}

	public <T> CosmosItemResponse<Object> del(T item, boolean isFromDeleted) {
		init(item.getClass(), isFromDeleted);
		return currentContainer.deleteItem(item, new CosmosItemRequestOptions());
	}

	public <T> CosmosItemResponse<T> put(T item, boolean isFromDeleted) {
		init(item.getClass(), isFromDeleted);
		return currentContainer.createItem(item);
	}

	public <T> T getById(String id, Class<T> type, boolean isFromDeleted) {
		init(type, isFromDeleted);
		String container = type.getSimpleName().replace("DAO", "s");
		container = isFromDeleted ? String.format("Deleted%s", container) : container;
		CosmosPagedIterable<T> response = currentContainer.queryItems(
				"SELECT * FROM " + container + " WHERE " + container + ".id=\"" + id + "\"",
				new CosmosQueryRequestOptions(), type);
		Iterator<T> it = response.iterator();
		if (it.hasNext())
			return it.next();
		return null;
	}

	public <T> CosmosPagedIterable<T> getAll(Class<T> type, boolean isFromDeleted) {
		init(type, isFromDeleted);
		String container = type.getSimpleName().replace("DAO", "s");
		container = isFromDeleted ? String.format("Deleted%s", container) : container;
		return currentContainer.queryItems("SELECT * FROM " + container, new CosmosQueryRequestOptions(), type);
	}

	public <T> CosmosItemResponse<T> patch(String id, String partKey, Class<T> type, String field, String change) {
		init(type, false);

		PartitionKey key = new PartitionKey(partKey);

		CosmosPatchOperations patchOps = CosmosPatchOperations.create().replace(field, change);
		return currentContainer.patchItem(id, key, patchOps, type);
	}

	public <T> CosmosItemResponse<T> patchAdd(String id, Class<T> type, String field, String change) {
		init(type, false);

		PartitionKey key = new PartitionKey(id);

		CosmosPatchOperations patchOps = CosmosPatchOperations.create().add(field + "/-", change);
		return currentContainer.patchItem(id, key, patchOps, type);
	}

	public <T> CosmosItemResponse<T> patchRemove(String id, Class<T> type, String field, int index) {
		init(type, false);

		PartitionKey key = new PartitionKey(id);

		// CosmosItemResponse<T> item = currentContainer.readItem(id, key, type);
		CosmosPatchOperations patchOps = CosmosPatchOperations.create().remove(field + "/" + index);
		return currentContainer.patchItem(id, key, patchOps, type);
	}

	public <T> void deleteAllInPartition(Class<T> type, String partKey, boolean isFromDeleted) {
		init(type, isFromDeleted);
		PartitionKey partitionKey = new PartitionKey(partKey);
		currentContainer.deleteAllItemsByPartitionKey(partitionKey, new CosmosItemRequestOptions());
	}

	public <T> CosmosPagedIterable<T> getAllByPartitionKey(Class<T> type, String partKey, boolean isFromDeleted) {
		init(type, isFromDeleted);
		PartitionKey partitionKey = new PartitionKey(partKey);
		return currentContainer.readAllItems(partitionKey, type);
	}

	public CosmosPagedIterable<MessageDAO> getAllMessagesByUser(String userId) {
		init(MessageDAO.class, false);
		return currentContainer.queryItems("SELECT * FROM Messages WHERE Messages.user = \"" + userId + "\"", new CosmosQueryRequestOptions(), MessageDAO.class); 
	}

	public CosmosPagedIterable<MessageDAO> getMessageReplies(String messageId) {
		init(MessageDAO.class, false);
		return currentContainer.queryItems("SELECT * FROM Messages WHERE Messages.replyTo = \"" + messageId + "\"", new CosmosQueryRequestOptions(), MessageDAO.class); 
	}

	public void close() {
		client.close();
	}

}
