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
public class ManageDeletedUsers {

	private DataLayer data = DataLayer.getInstance();

	//"30 3 */24 * * *" - every 24 hours at :30 seconds the function will run
	// for testing purposes we run it every 2 minutes
    @FunctionName("manage-deleted-users")
    public void cosmosFunction( @TimerTrigger(name = "periodicDeletedUser", 
    								schedule = "30 */2 * * * *")
    				String timerInfo,
    				ExecutionContext context) throws Exception {
		try (Jedis jedis = RedisCache.getCachePool().getResource()) {
			jedis.incr("cnt:timer");
			jedis.set("serverless-time", new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z").format(new Date()));
		}

		CosmosPagedIterable<UserDAO> deletedUsers = data.getAll(UserDAO.class, true);

		for(UserDAO u : deletedUsers) {
			for(String channelId : u.getChannelIds()) {

				Channel c = data.get(channelId, Channel.class, ChannelDAO.class, false);
				if(c != null) {
					List<String> membersList = Arrays.asList(c.getMembers());
					if(membersList.contains(u.getId())) {
						data.patchRemove(channelId, Channel.class, ChannelDAO.class, "/members", membersList.indexOf(u.getId())); //remove user from channels
					}

					if(c.getOwner().equals(u.getId())) {
						data.delete(channelId, channelId, Channel.class, ChannelDAO.class, false); //delete channels that user owns
						data.delChannelMessages(channelId, Message.class, MessageDAO.class); //delete messages from the deleted channels
					}
				}	
			}
			
			data.updateDelUserMessages(u.getId(), Message.class, MessageDAO.class); //change owner of this user's message to a deleted user (-1)
			data.delete(u.getId(), u.getId(), User.class, UserDAO.class, true); //remove user from list of deleted users
		}
    }
}
