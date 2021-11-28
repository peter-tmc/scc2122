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
public class ManageDeletedMessages {

	private DataLayer data = DataLayer.getInstance();
	
    @FunctionName("manage-deleted-messages")
    public void cosmosFunction( @TimerTrigger(name = "periodicSetTime", 
    								schedule = "30 */1 * * * *") 
    				String timerInfo,
    				ExecutionContext context) {
		try (Jedis jedis = RedisCache.getCachePool().getResource()) {
			jedis.incr("cnt:timer");
			jedis.set("serverless-time", new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z").format(new Date()));
		}

		CosmosPagedIterable<MessageDAO> deletedMessages = data.getAll(MessageDAO.class, true);
		for(MessageDAO m : deletedMessages) {
			data.updateDelMessageReplies(m.getId(), Message.class, MessageDAO.class); //change message's replyTo to a deleted message (-1)
		
			data.delete(m.getId(), m.getId(), Message.class, MessageDAO.class, true); //remove message from list of deleted messages
		}
		
    }
}
