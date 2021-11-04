package scc.srv;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.*;
import scc.data.*;
import scc.utils.*;

@Path(UserResources.PATH)
public class UserResources {

    public static final String PATH = "/users";
    private CosmosDBLayer db = CosmosDBLayer.getInstance();
    private BlobStorageLayer blob = BlobStorageLayer.getInstance();

    /**
     * Creates a new user, given its object
     * @param user - user object
     * @return the generated ID
     */
    @POST
    @Path("")
    @Consumes(MediaType.APPLICATION_JSON)
    public String createUser(User user) {
        String id = user.getId();
        //db.putUser(new UserDAO(user));
        return id;
    }

    /**
     * Deletes a user, given its ID
     * @param id - id of the user to be deleted
     */
    @DELETE
    @Path("{id}")
    public void deleteUser(@PathParam("id") String id) {
        //db.delUserById(id);
        //change messages from user to Deleted User
    }

    /**
     * Updates a user, given its object
     * @param id - id of the user to be updated
     */
    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateUser(@PathParam("id") String id, User user) {
        //db.putUser(new UserDAO(user));
    }

    /**
     * Gets a user, given its id
     * @param id - id of the user to retrieve
     * @return the user
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public User getUser(@PathParam("id") String id) {
        /*TODO se calhar passar esta logica para a layer
		CosmosPagedIterable<UserDAO> response = db.getById(id);
        Iterator<UserDAO> it = response.iterator();
        if(it.hasNext())
            return new User(it.next());*/
		return null;
    }

    /**
     * Lists the channels of a certain user, given its id.
     * @return a list with all the channels
     */
    @GET
    @Path("/{id}/channels/list")
    @Produces(MediaType.APPLICATION_JSON)
    public String[] listChannelsOfUser(@PathParam("id") String id) {
        User u = getUser(id);
        if(u != null)
            return getUser(id).getChannelIds();
        return null;
    }
}
