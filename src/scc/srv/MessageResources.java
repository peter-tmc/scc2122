package scc.srv;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.*;
import scc.data.*;
import scc.layers.*;

@Path(MessageResources.PATH)
public class MessageResources {

    public static final String PATH = "/messages";
    private CosmosDBLayer db = CosmosDBLayer.getInstance();

    /**
     * Creates a message given its id.
     * @param message - message object
     * @return the generated id
     */
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public String createMessage(Message message) {
        db.put(message);
        return message.getId();
    }

    /**
     * Delete the message, given its id.
     * @param id - id of the message to be deleted
     */
    @DELETE
    @Path("/{id}")
    public void deleteMessage(@PathParam("id") String id) {
        db.delById(id, MessageDAO.class);
    }

    /**
     * Gets a message, given its id
     * @param id - id of the message to retrieve
     * @return the message
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Message getMessage(@PathParam("id") String id) {
        MessageDAO m = db.getById(id, MessageDAO.class);
        if(m == null)
            throw new WebApplicationException(Status.NOT_FOUND);
        return new Message(m);
    }

    // optional - Azure Cognitive Search
    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    public String[] searchMessages(@QueryParam("query") String query) {
        return null;
    }
}
