package scc.srv;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.util.CosmosPagedIterable;

import java.util.*;
import javax.ws.rs.*;
import scc.data.*;
import scc.layers.*;

@Path(UserResources.PATH)
public class UserResources {

    public static final String PATH = "/users";
    private CosmosDBLayer db = CosmosDBLayer.getInstance();

    /**
     * Creates a new user, given its object
     * 
     * @param user - user object
     * @return the generated ID
     */
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String createUser(User user) {

        CosmosItemResponse<UserDAO> response = null;
        try{
            response = db.put(new UserDAO(user));
        } catch(Exception e){
             throw new WebApplicationException("418");
        }
        // if (response.getStatusCode() != Status.CREATED.getStatusCode())
        //     throw new WebApplicationException(response.getStatusCode());

        return user.getId();
    }

    /**
     * Deletes a user, given its ID
     * 
     * @param id - id of the user to be deleted
     */
    @DELETE
    @Path("/{id}")
    public void deleteUser(@PathParam("id") String id) {
        CosmosItemResponse<Object> response = db.delById(id, User.class);
        if (response.getStatusCode() != Status.NO_CONTENT.getStatusCode())
            throw new WebApplicationException(response.getStatusCode());

        new Thread(() -> {
            db.updateDelUserMessages(id, User.class);
        }).start();
    }

    /**
     * Updates a user, given its object
     * 
     * @param id - id of the user to be updated
     */
    @PATCH
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateUser(@PathParam("id") String id, User user) {
        CosmosItemResponse<Object> response = db.put(new UserDAO(user));
        if (response.getStatusCode() != Status.OK.getStatusCode())
            throw new WebApplicationException(response.getStatusCode());
    }

    /**
     * Gets a user, given its id
     * 
     * @param id - id of the user to retrieve
     * @return the user
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public User getUser(@PathParam("id") String id) {
        UserDAO response = db.getById(id, UserDAO.class);
        if (response == null)
            throw new WebApplicationException(Status.NOT_FOUND);
        else
            return new User(response);
    }

    /**
     * Gets a user, given its id
     * @param id - id of the user to retrieve
     * @return the user
     */
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public List<User> listUsers() {
        CosmosPagedIterable<UserDAO> response = db.getAll(UserDAO.class);
        if(response == null) 
            throw new WebApplicationException(Status.NOT_FOUND);
        else {
            //Arrays.stream(destinations.split(",")).map(s -> parseSocketAddress(s)).collect(Collectors.toSet());
        }

        List<User> l = new ArrayList<>();
        Iterator<UserDAO> it = response.iterator();
        while(it.hasNext()) {
            l.add(new User(it.next()));
        }
        return l;
    }

    /**
     * Lists the channels of a certain user, given its id.
     * 
     * @return a list with all the channels
     */
    @GET
    @Path("/{id}/channels/list")
    @Produces(MediaType.APPLICATION_JSON)
    public String[] listChannelsOfUser(@PathParam("id") String id) {
        User u = getUser(id);
        return u.getChannelIds();
    }
}
