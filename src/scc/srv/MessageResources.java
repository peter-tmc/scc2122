package scc.srv;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import com.azure.cosmos.CosmosException;

import java.util.Arrays;
import java.util.UUID;

import javax.ws.rs.*;

import scc.cache.RedisCache;
import scc.data.*;
import scc.layers.*;

@Path(MessageResources.PATH)
public class MessageResources {

    public static final String PATH = "/messages";
    private CosmosDBLayer db = CosmosDBLayer.getInstance();
    private BlobStorageLayer blob = BlobStorageLayer.getInstance();
    private RedisCache cache = RedisCache.getInstance();

    /**
     * Creates a message given its id.
     * 
     * @param message - message object
     * @return the generated id
     */
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public String createMessage(Message message) {
        if (message.getChannel() == null || message.getUser() == null) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        try {
            UserDAO user = db.getById(message.getUser(), UserDAO.class);
            ChannelDAO channel = db.getById(message.getChannel(), ChannelDAO.class);

            if (user == null || channel == null)
                throw new WebApplicationException(Status.BAD_REQUEST);
            
            if(!Arrays.asList(channel.getMembers()).contains(user.getId()))
                throw new WebApplicationException(Status.UNAUTHORIZED);

            if (message.getReplyTo() != null && db.getById(message.getReplyTo(), MessageDAO.class) == null)
                throw new WebApplicationException(Status.BAD_REQUEST);

            if(message.getImageId() != null && !blob.blobExists(message.getImageId()))
                throw new WebApplicationException(Status.BAD_REQUEST);
            
            String randID = UUID.randomUUID().toString();
            message.setId(randID);
            db.put(new MessageDAO(message));
            MessageDAO m = db.getById(randID, MessageDAO.class);
            if (m != null)
                cache.setValue(randID, new Message(m));
            return randID;
        } catch (CosmosException e) {
            throw new WebApplicationException(e.getStatusCode());
        }
    }

    /**
     * Delete the message, given its id.
     * 
     * @param id - id of the message to be deleted
     */
    @DELETE
    @Path("/{id}")
    public void deleteMessage(@PathParam("id") String id) {
        try {
            db.delById(id, MessageDAO.class);
            cache.delete(id, Message.class);
        } catch (CosmosException e) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
    }

    /**
     * Gets a message, given its id
     * 
     * @param id - id of the message to retrieve
     * @return the message
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Message getMessage(@PathParam("id") String id) {
        Message m = cache.getValue(id, Message.class);
        if(m != null) {
            return m;
        }
        MessageDAO message = null;
        try {
            message = db.getById(id, MessageDAO.class);
        } catch (CosmosException e) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
        if (message == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }
        else {
            m = new Message(message);
            cache.setValue(id, m);
        }
        return m;
    }

    // optional - Azure Cognitive Search
    // TODO
    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    public String[] searchMessages(@QueryParam("query") String query) {
        return null;
    }

    /* private void addToCache(Message m) {
        String cacheId = String.format("message_%s", m.getId());
        cache.setValue(cacheId, m);
    }

    private void deleteFromCache(String id) {
        String cacheId = String.format("message_%s", id);
        cache.delete(cacheId, Message.class);
    } */
}
