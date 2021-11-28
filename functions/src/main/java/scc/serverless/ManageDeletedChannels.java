package scc.serverless;

import java.text.SimpleDateFormat;
import java.util.*;
import com.microsoft.azure.functions.annotation.*;
import redis.clients.jedis.Jedis;
import scc.cache.RedisCache;
import scc.layers.DataLayer;
import scc.data.*;

import com.azure.cosmos.util.CosmosPagedIterable;
import com.microsoft.azure.functions.*;

/**
 * Azure Functions with Timer Trigger.
 */
public class ManageDeletedChannels {

	private DataLayer data = DataLayer.getInstance();
	
    @FunctionName("manage-deleted-channels")
    public void cosmosFunction( @TimerTrigger(name = "periodicSetTime", 
    								schedule = "30 */1 * * * *") 
    				String timerInfo,
    				ExecutionContext context) {
		try (Jedis jedis = RedisCache.getCachePool().getResource()) {
			jedis.incr("cnt:timer");
			jedis.set("serverless-time", new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z").format(new Date()));
		}

		CosmosPagedIterable<ChannelDAO> deletedChannels = data.getAll(ChannelDAO.class, true);
		for(ChannelDAO c : deletedChannels) {
			data.delChannelMessages(c.getId(), Message.class, MessageDAO.class); //delete messages of the deleted channel

			for(String memberId : c.getMembers()) {
				User u = data.get(memberId, User.class, UserDAO.class, false);
				if(u != null) {
					List<String> userChannels = Arrays.asList(u.getChannelIds());
					if(userChannels.contains(c.getId())) {
						data.patchRemove(memberId, User.class, UserDAO.class, "/channelIds", userChannels.indexOf(c.getId())); //remove channel from all the users that were in it
					}
				}
			}
		
			data.delete(c.getId(), c.getId(), Channel.class, ChannelDAO.class, true); //remove channel from list of deleted channels
		}
		
    }
}
