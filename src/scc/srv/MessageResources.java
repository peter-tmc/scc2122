package scc.srv;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import java.util.UUID;

import javax.ws.rs.*;
import scc.data.*;
import scc.layers.*;

@Path(MessageResources.PATH)
public class MessageResources {

    public static final String PATH = "/messages";
    private CosmosDBLayer db = CosmosDBLayer.getInstance();

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
        String id = "-2";
        if (db.getById(message.getChannel(), ChannelDAO.class) == null)
            throw new WebApplicationException(Status.BAD_REQUEST);
        else if (db.getById(message.getReplyTo(), MessageDAO.class) == null)
            throw new WebApplicationException(Status.BAD_REQUEST);
        else {
            id = String.format("message_%s", UUID.randomUUID());
            message.setId(id);
            db.put(new MessageDAO(message));
        }

        return id;
    }

    /**
     * Delete the message, given its id.
     * 
     * @param id - id of the message to be deleted
     */
    @DELETE
    @Path("/{id}")
    public void deleteMessage(@PathParam("id") String id) {
        if(db.getById(id, MessageDAO.class) != null) {
            db.delById(id, MessageDAO.class);
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
        MessageDAO m = db.getById(id, MessageDAO.class);
        if (m == null)
            throw new WebApplicationException(Status.NOT_FOUND);
        return new Message(m);
    }

    // optional - Azure Cognitive Search
    //TODO 
    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    public String[] searchMessages(@QueryParam("query") String query) {
        return null;
    }
}
