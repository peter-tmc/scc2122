package scc.srv;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import com.azure.cosmos.CosmosException;

import java.util.Arrays;
import java.util.UUID;

import javax.ws.rs.*;

import scc.authentication.CookieAuth;
import scc.cache.RedisCache;
import scc.data.*;
import scc.layers.*;

@Path(MessageResources.PATH)
public class MessageResources {

    public static final String PATH = "/messages";
    private CosmosDBLayer db = CosmosDBLayer.getInstance();
    private BlobStorageLayer blob = BlobStorageLayer.getInstance();
    private RedisCache cache = RedisCache.getInstance();
    private CookieAuth auth = CookieAuth.getInstance();

    /**
     * Creates a message given its id.
     * 
     * @param message - message object
     * @return the generated id
     */
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public String createMessage(@CookieParam("scc:session") Cookie session, Message message) {
        if (message.getChannel() == null || message.getUser() == null) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
        User user = cache.getValue(message.getUser(), User.class);
        Channel channel = cache.getValue(message.getChannel(), Channel.class);
        try {
            if (user == null) {
                UserDAO userD = db.getById(message.getUser(), UserDAO.class);
                if (userD == null)
                    throw new WebApplicationException(Status.BAD_REQUEST);
                user = new User(userD);
            }

            if (channel == null) {
                ChannelDAO channelD = db.getById(message.getChannel(), ChannelDAO.class);
                if (channelD == null)
                    throw new WebApplicationException(Status.BAD_REQUEST);
                channel = new Channel(channelD);
            }

            /*
             * if (user == null || channel == null) throw new
             * WebApplicationException(Status.BAD_REQUEST);
             */

            auth.checkCookie(session, user.getId());

            if (!Arrays.asList(channel.getMembers()).contains(user.getId()))
                throw new WebApplicationException(Status.UNAUTHORIZED);

            if (message.getReplyTo() != null && db.getById(message.getReplyTo(), MessageDAO.class) == null)
                throw new WebApplicationException(Status.BAD_REQUEST);

            if (message.getImageId() != null && !blob.blobExists(message.getImageId()))
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
    public void deleteMessage(@CookieParam("scc:session") Cookie session, @PathParam("id") String id) {
        try {
            Message message = cache.getValue(id, Message.class);
            if (message == null) {
                MessageDAO m = db.getById(id, MessageDAO.class);
                if (m != null) {
                    message = new Message(m);
                } else {
                    throw new WebApplicationException(Status.NOT_FOUND);
                }
            }
            auth.checkCookie(session, message.getUser());
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
    public Message getMessage(@CookieParam("scc:session") Cookie session, @PathParam("id") String id) {
        /* Message m = cache.getValue(id, Message.class); */
        /*
         * if(m != null) { auth.checkCookie(session, m.getUser()); return m; }
         */
        Message message = cache.getValue(id, Message.class);
        Channel channel = cache.getValue(id, Channel.class);
        try {
            if (message == null) {
                MessageDAO m = db.getById(id, MessageDAO.class);
                if (m == null)
                    throw new WebApplicationException(Status.NOT_FOUND);
                message = new Message(m);
            }
            if (channel == null) {
                ChannelDAO c = db.getById(id, ChannelDAO.class);
                if (c == null)
                    throw new WebApplicationException(Status.NOT_FOUND);
                channel = new Channel(c);
            }
        } catch (CosmosException e) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
        
        String userId = cache.getSession(session.getValue());

        if (userId == null || !Arrays.asList(channel.getMembers()).contains(userId))
            throw new WebApplicationException(Status.UNAUTHORIZED);

        cache.setValue(id, message);
        return message;
    }

    // OPTIONAL - Azure Cognitive Search
    // TODO
    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    public String[] searchMessages(@QueryParam("query") String query) {
        return null;
    }
}
