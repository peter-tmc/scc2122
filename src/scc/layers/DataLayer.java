package scc.layers;

import com.azure.cosmos.util.CosmosPagedIterable;

import scc.cache.RedisCache;
import scc.data.*;

public class DataLayer {

    private boolean cacheActive;
    private RedisCache cache;
    private CosmosDBLayer db;

    private static DataLayer instance;

    public static synchronized DataLayer getInstance() {
        if (instance != null)
            return instance;

        instance = new DataLayer(true);
        return instance;
    }

    public DataLayer(boolean cacheActive) {
        this.cache = RedisCache.getInstance();
        this.db = CosmosDBLayer.getInstance();
        this.cacheActive = cacheActive;
    }

    public <T, U> T get(String id, Class<T> type, Class<U> typeDB, boolean isFromDeleted) {
        T item = null;

        if (cacheActive)
            item = cache.getValue(id, type);

        if (item == null) {
            U dbItem = db.getById(id, typeDB, isFromDeleted);
            if (dbItem == null) {
                return null;
            }
            item = constructItem(dbItem, type);
            cache.setValue(id, item);
        }
        return item;
    }

    public <T> CosmosPagedIterable<T> getAll(Class<T> typeDB, boolean isFromDeleted) {
        return db.getAll(typeDB, isFromDeleted);
    }

    public <T, U> void put(String id, T item, U itemDB, Class<T> type, Class<U> typeDB, boolean isFromDeleted) {
        db.put(itemDB, isFromDeleted);
        
        if (cacheActive && !isFromDeleted) {
            itemDB = db.getById(id, typeDB, isFromDeleted);
            if (itemDB != null)
                cache.setValue(id, constructItem(itemDB, type));
        }
    }

    public <T, U> void delete(String id, String partKey, Class<T> type, Class<U> typeDB, boolean isFromDeleted) {
        db.delById(id, partKey, typeDB, isFromDeleted);

        if (cacheActive)
            cache.delete(id, type);
    }

    public <T, U> void patch(String id, String partKey ,Class<T> type, Class<U> typeDB, String field, String change) {
        db.patch(id, partKey, typeDB, field, change);

        if (cacheActive) {
            U itemDB = db.getById(id, typeDB, false);
            if (itemDB != null)
                cache.setValue(id, constructItem(itemDB, type));
        }

    }

    public <T, U> void patchAdd(String id, Class<T> type, Class<U> typeDB, String field, String change) {
        db.patchAdd(id, typeDB, field, change);

        if (cacheActive) {
            U itemDB = db.getById(id, typeDB, false);
            if (itemDB != null)
                cache.setValue(id, constructItem(itemDB, type));
        }

    }

    public <T, U> void patchRemove(String id, Class<T> type, Class<U> typeDB, String field, int index) {
        db.patchRemove(id, typeDB, field, index);

        if (cacheActive) {
            U itemDB = db.getById(id, typeDB, false);
            if (itemDB != null)
                cache.setValue(id, constructItem(itemDB, type));
        }
    }

    public <T, U> CosmosPagedIterable<U> getPartition(String channelId, Class<T> type, Class<U> typeDB) {
        return db.getAllByPartitionKey(typeDB, channelId, false);
    }

    public CosmosPagedIterable<MessageDAO> getMessagesFromChannel(String channelId, int st, int len) {
        return db.getMessagesFromChannel(channelId, st, len);
    }

    public <T, U> void delChannelMessages(String channelId, Class<T> type, Class<U> typeDB) {
        CosmosPagedIterable<U> messages = db.getAllByPartitionKey(typeDB, channelId, false);
        for(U m : messages) {
            cache.delete(constructItem(m, Message.class).getId(), type);
        }
        db.deleteAllInPartition(typeDB, channelId, false);
    }

    public <T, U> void updateDelUserMessages(String userId, Class<T> type, Class<U> typeDB) {
        CosmosPagedIterable<MessageDAO> messages = db.getAllMessagesByUser(userId);
        for(MessageDAO m : messages) {
            db.patch(m.getId(), m.getChannel(), typeDB, "/user", "-1");
        }  
    }

    public <T, U> void updateDelMessageReplies(String messageId, Class<T> type, Class<U> typeDB) {
        CosmosPagedIterable<MessageDAO> messages = db.getMessageReplies(messageId);
        for(MessageDAO m : messages) {
            db.patch(m.getId(), m.getChannel(), typeDB, "/replyTo", "-1");
        }  
    }

    @SuppressWarnings("unchecked")
    private <T> T constructItem(Object item, Class<T> type) {
        if (type.equals(User.class))
			return (T) new User((UserDAO)item);
		else if (type.equals(Message.class))
			return (T) new Message((MessageDAO)item);
		else if (type.equals(Channel.class))
			return (T) new Channel((ChannelDAO)item);
        return null;
    }
}
