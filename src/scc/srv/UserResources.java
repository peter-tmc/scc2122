package scc.srv;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.util.CosmosPagedIterable;

import redis.clients.jedis.JedisPool;

import java.util.*;
import javax.ws.rs.*;

import scc.cache.RedisCache;
import scc.data.*;
import scc.layers.*;

@Path(UserResources.PATH)
public class UserResources {

    public static final String PATH = "/users";
    private CosmosDBLayer db = CosmosDBLayer.getInstance();
    private RedisCache cache = RedisCache.getInstance();

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
        String id = user.getId();
        /* String id = String.format("user_%s", UUID.randomUUID()); */
        /*
         * UUID uuid = UUID.randomUUID(); user.setId(uuid);
         */
        if (id != null || id.equals(""))
            throw new WebApplicationException(Status.BAD_REQUEST);
        for (String c : user.getChannelIds())
            if (db.getById(c, ChannelDAO.class) == null)
                throw new WebApplicationException(Status.BAD_REQUEST);
        if (db.getById(id, UserDAO.class) != null)
            throw new WebApplicationException(Status.CONFLICT);
        else {
            db.put(new UserDAO(user));
            /* cache.setValue(id, user); */
        }
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
        if (db.getById(id, UserDAO.class) != null) {
            db.delById(id, UserDAO.class);
            cache.delete(id, User.class);
            /*
             * if (response.getStatusCode() != Status.NO_CONTENT.getStatusCode()) throw new
             * WebApplicationException(response.getStatusCode());
             */
            new Thread(() -> {
                db.updateDelUserMessages(id, User.class);
            }).start();
            //TODO fazer uma funcao que da update as msgs q tao em cache,
            // para isto sq metemos o nome de quem enviou a msg no ID da msg, 
            //para depois ser simples de procurar 
        }
    }

    /**
     * Updates a user, given its object
     * 
     * @param id - id of the user to be updated
     */
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateUser(@PathParam("id") String id, User user) {
        if (db.getById(id, UserDAO.class) != null) {
            db.delById(id, UserDAO.class);
            db.put(new UserDAO(user));
            /* cache.setValue(id, user); */
        } else
            throw new WebApplicationException(Status.BAD_REQUEST);
        /*
         * if (response.getStatusCode() != Status.OK.getStatusCode()) throw new
         * WebApplicationException(response.getStatusCode());
         */
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
        User u = null;
        /*
         * User u = cache.getValue(id, User.class); if(u == null) return u;
         */
        UserDAO response = db.getById(id, UserDAO.class);
        if (response == null)
            throw new WebApplicationException(Status.NOT_FOUND);
        else {
            u = new User(response);
            /* cache.setValue(id, u); */
            return u;
        }
    }

    /**
     * Gets a user, given its id
     * 
     * @param id - id of the user to retrieve
     * @return the user
     */
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public List<User> listUsers() {
        CosmosPagedIterable<UserDAO> response = db.getAll(UserDAO.class);
        if (response == null)
            throw new WebApplicationException(Status.NOT_FOUND);
        List<User> l = new ArrayList<>();
        Iterator<UserDAO> it = response.iterator();
        while (it.hasNext()) {
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
