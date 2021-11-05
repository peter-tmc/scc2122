package scc.cache;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import scc.data.User;

public class RedisCache {
	
	private static JedisPool instance;
	private static RedisCache cache;
	
	public synchronized static JedisPool getCachePool() {
		if( instance != null)
			return instance;
		final JedisPoolConfig poolConfig = new JedisPoolConfig();

        String redisHostname = System.getenv("REDIS_HOSTNAME");
        String redisKey = System.getenv("REDIS_KEY");

		poolConfig.setMaxTotal(128);
		poolConfig.setMaxIdle(128);
		poolConfig.setMinIdle(16);
		poolConfig.setTestOnBorrow(true);
		poolConfig.setTestOnReturn(true);
		poolConfig.setTestWhileIdle(true);
		poolConfig.setNumTestsPerEvictionRun(3);
		poolConfig.setBlockWhenExhausted(true);
		instance = new JedisPool(poolConfig, redisHostname, 6380, 1000, redisKey, true);
		return instance;
		
	}

	public synchronized static RedisCache getInstance() {
		if(cache!=null)
			return cache;
		cache = new RedisCache();
		return cache;
	}

	public RedisCache() {
	}

	public <T> void setValue(String id, T item) {
		ObjectMapper mapper = new ObjectMapper();
		try (Jedis jedis = RedisCache.getCachePool().getResource()) {
			jedis.set(item.getClass().getSimpleName()+":"+id, mapper.writeValueAsString(item));
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	public <T> T getValue(String id, Class<T> type) {
		try (Jedis jedis = RedisCache.getCachePool().getResource()) {
			String str = jedis.get(type.getSimpleName()+":" + id);
			ObjectMapper mapper = new ObjectMapper();
			T item = null;
			try {
				item = mapper.readValue(str, type);
			}
			catch (Exception e) {
				System.out.println(e.getMessage());
			}
			return item;
		}
	}

	public <T> void add(T item) {
		ObjectMapper mapper = new ObjectMapper();
		String listName = "MostRecent"+item.getClass().getSimpleName();
		try (Jedis jedis = RedisCache. getCachePool().getResource()) {
			Long cnt = jedis.lpush(listName, mapper.writeValueAsString(item));
			if (cnt > 5)
				jedis.ltrim(listName, 0, 4);
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	public <T> List<String> list(Class<T> type) {
		try (Jedis jedis = RedisCache. getCachePool().getResource()) {
			List<String> lst = jedis.lrange("MostRecent"+type.getSimpleName(), 0, -1);
			return lst;
		}
	}

	public <T> long incr(Class<T> type) {
		try (Jedis jedis = RedisCache. getCachePool().getResource()) {
			return jedis.incr("Num"+type.getSimpleName());
		}
	}

	public <T> void delete(String id, Class<T> type) {
		try (Jedis jedis = RedisCache. getCachePool().getResource()) {
			jedis.del(type.getSimpleName()+":" + id);
		}
	}
}