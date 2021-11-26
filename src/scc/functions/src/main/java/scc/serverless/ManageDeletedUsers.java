package scc.functions.src.main.java.scc.serverless;

import java.text.SimpleDateFormat;
import java.util.*;
import com.microsoft.azure.functions.annotation.*;
import redis.clients.jedis.Jedis;
import scc.cache.RedisCache;
import scc.functions.src.main.java.scc.layers.DataLayer;
import scc.data.*;

import com.azure.cosmos.util.CosmosPagedIterable;
import com.microsoft.azure.functions.*;


/**
 * Azure Functions with Timer Trigger.
 */
public class ManageDeletedUsers {

	private DataLayer data = DataLayer.getInstance();

    @FunctionName("manage-deleted-users")
    public void cosmosFunction( @TimerTrigger(name = "periodicDeletedUser", 
    								schedule = "30 */2 * * * *") //"30 3 */24 * * *" a cada 24 horas ao segundo 30 vai correr a funcao
									//mas para efeitos de testagem corremos a cada 2 minutos
    				String timerInfo,
    				ExecutionContext context) {
		try (Jedis jedis = RedisCache.getCachePool().getResource()) {
			jedis.incr("cnt:timer");
			jedis.set("serverless-time", new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z").format(new Date()));
		}

		CosmosPagedIterable<UserDAO> deletedUsers = data.getAll(UserDAO.class, true);
		for(UserDAO u : deletedUsers) {
			for(String channelId : u.getChannelIds()) {
				Channel chan = data.get(channelId, Channel.class, ChannelDAO.class, false);
				if(chan.getOwner().equals(u.getId())) {
					data.delete(channelId, Channel.class, ChannelDAO.class, false);
					data.delChannelMessages(channelId, Message.class, MessageDAO.class);
				}
			}
			
			data.updateDelUserMessages(u.getId(), User.class, UserDAO.class);
		
			data.delete(u.getId(), User.class, UserDAO.class, true);
		}
    }
}
