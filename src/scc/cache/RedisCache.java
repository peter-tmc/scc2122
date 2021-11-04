package scc.cache;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import scc.data.User;

public class RedisCache {
	
	private static JedisPool instance;
	
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

	public void setValue(String id, User u) {
		ObjectMapper mapper = new ObjectMapper();

		try (Jedis jedis = RedisCache. getCachePool().getResource()) {
			jedis.set("user:"+id, mapper.writeValueAsString(u));
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	public String getValue(String id) {
		try (Jedis jedis = RedisCache.getCachePool().getResource()) {
			return jedis.get("user:" + id);
		}
	}

	public void add(User u) {
		ObjectMapper mapper = new ObjectMapper();

		try (Jedis jedis = RedisCache. getCachePool().getResource()) {
			Long cnt = jedis.lpush("MostRecentUsers", mapper.writeValueAsString(u));
			if (cnt > 5)
				jedis.ltrim("MostRecentUsers", 0, 4);
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	public List<String> list() {
		try (Jedis jedis = RedisCache. getCachePool().getResource()) {
			List<String> lst = jedis.lrange("MostRecentUsers", 0, -1);
			return lst;
		}
	}

	public long incr() {
		try (Jedis jedis = RedisCache. getCachePool().getResource()) {
			return jedis.incr("NumUsers");
		}
	}


	
}