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
			data.delChannelMessages(c.getId(), Message.class, MessageDAO.class);

			List<String> membersList = Arrays.asList(c.getMembers());

			for(String memberId : c.getMembers()) {
				data.patchRemove(memberId, User.class, UserDAO.class, "/channelIds", membersList.indexOf(memberId));
			}
		
			data.delete(c.getId(), Channel.class, ChannelDAO.class, true);
		}
		
    }
}
