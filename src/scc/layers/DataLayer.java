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

    public <T, U> T get(String id, Class<T> type, Class<U> typeDB) {
        T item = null;

        if (cacheActive)
            item = cache.getValue(id, type);

        if (item == null) {
            U dbItem = db.getById(id, typeDB);
            if (dbItem == null) {
                return null;
            }
            item = constructItem(dbItem, type);
            cache.setValue(id, item);
        }
        return item;
    }

    public <T> CosmosPagedIterable<T> getAll(Class<T> typeDB) {
        return db.getAll(typeDB);
    }

    public <T, U> void put(String id, T item, U itemDB, Class<T> type, Class<U> typeDB) {
        db.put(itemDB);
        
        if (cacheActive) {
            itemDB = db.getById(id, typeDB);
            if (itemDB != null)
                cache.setValue(id, constructItem(itemDB, type));
        }
    }

    public <T, U> void delete(String id, Class<T> type, Class<U> typeDB) {
        db.delById(id, typeDB);

        if (cacheActive)
            cache.delete(id, type);
    }

    public <T, U> void patchAdd(String id, Class<T> type, Class<U> typeDB, String field, String change) {
        db.patchAdd(id, typeDB, field, change);

        if (cacheActive) {
            U itemDB = db.getById(id, typeDB);
            if (itemDB != null)
                cache.setValue(id, constructItem(itemDB, type));
        }

    }

    public <T, U> void patchRemove(String id, Class<T> type, Class<U> typeDB, String field, int index) {
        db.patchRemove(id, typeDB, field, index);

        if (cacheActive) {
            U itemDB = db.getById(id, typeDB);
            if (itemDB != null)
                cache.setValue(id, constructItem(itemDB, type));
        }
    }

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
