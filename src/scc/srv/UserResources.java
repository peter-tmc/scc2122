package scc.srv;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.azure.cosmos.CosmosException;
import com.azure.cosmos.util.CosmosPagedIterable;

import java.util.*;
import javax.ws.rs.*;

import scc.authentication.CookieAuth;
import scc.cache.RedisCache;
import scc.data.*;
import scc.layers.*;

@Path(UserResources.PATH)
public class UserResources {

    public static final String PATH = "/users";
    private CosmosDBLayer db = CosmosDBLayer.getInstance();
    private BlobStorageLayer blob = BlobStorageLayer.getInstance();
    private RedisCache cache = RedisCache.getInstance();
    private CookieAuth auth = CookieAuth.getInstance();
    private boolean cacheActive = true;

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
    public User createUser(User user) {

        String id = user.getId();
        if (id == null || (user.getPhotoId() != null && !blob.blobExists(user.getPhotoId())) || (user.getChannelIds().length != 0))
            throw new WebApplicationException(Status.BAD_REQUEST);

        User u = null;
        if(cacheActive) {
            // check if it's in cache so we know it was created
            u = cache.getValue(id, User.class);
            if (u != null) {
                throw new WebApplicationException(Status.CONFLICT);
            } 
        }

        try {
            db.put(new UserDAO(user));

            if(cacheActive) {
                UserDAO udao = db.getById(id, UserDAO.class);
                if (udao != null) {
                    u = new User(udao);
                    cache.setValue(u.getId(), u);
                }
            }
        } catch (CosmosException e) {
            throw new WebApplicationException(e.getStatusCode());
        }

        return user;
    }

    /**
     * Deletes a user, given its ID
     * 
     * @param id - id of the user to be deleted
     */
    @DELETE
    @Path("/{id}")
    public void deleteUser(@CookieParam("scc:session") Cookie session, @PathParam("id") String id) {

        auth.checkCookie(session, id);

        try {
            db.delById(id, UserDAO.class);
            cache.delete(id, User.class);
            cache.deleteCookie(session.getValue());
        } catch (CosmosException e) {
            throw new WebApplicationException(e.getStatusCode());
        }

        new Thread(() -> {
            db.updateDelUserMessages(id, User.class);
            // TODO fazer uma funcao que da update as msgs q tao em cache,
            // para isto sq metemos o nome de quem enviou a msg no ID da msg,
            // para depois ser simples de procurar
        }).start();
    }

    /**
     * Updates a user, given its object
     * 
     * @param id - id of the user to be updated
     */
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateUser(@CookieParam("scc:session") Cookie session, @PathParam("id") String id, User user) {
        auth.checkCookie(session, id);
        
        UserDAO u = null;
        try {
            if(cacheActive) {
                cache.getValue(id, User.class);
            }
            u = db.getById(id, UserDAO.class);
            if(!user.getId().equals(u.getId()) || !Arrays.equals(user.getChannelIds(), u.getChannelIds())) {
                throw new WebApplicationException(Status.FORBIDDEN);
            } 
            db.delById(id, UserDAO.class);
            db.put(new UserDAO(user));
            
            if(cacheActive)
                u = db.getById(id, UserDAO.class);
        } catch (CosmosException e) {
            throw new WebApplicationException(e.getStatusCode());
        }

        if(cacheActive && u != null) {
            cache.setValue(id, new User(u));
        }
            
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
        User user = cache.getValue(id, User.class);
        if (user != null)
            return user;
        UserDAO u = null;
        try {
            u = db.getById(id, UserDAO.class);
        } catch (CosmosException e) {
            throw new WebApplicationException(e.getStatusCode());
        }

        if (u == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        user = new User(u);
        cache.setValue(u.getId(), user);

        return user;
    }

    /**
     * Lists all users
     * 
     * @param id - id of the user to retrieve
     * @return the user
     */
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public List<User> listUsers() {
        CosmosPagedIterable<UserDAO> users = null;

        try {
            users = db.getAll(UserDAO.class);
        } catch (CosmosException e) {
            throw new WebApplicationException(e.getStatusCode());
        }

        if (users == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        List<User> l = new ArrayList<>();
        Iterator<UserDAO> it = users.iterator();
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

    /**
     * Subscribes given user to a given channel
     * 
     * @param id - id of the user that will subscribe
     * @param id - id of the channel the user will subscribe to
     */
    @PUT
    @Path("{userId}/subscribe/{channelId}")
    public void subChannel(@CookieParam("scc:session") Cookie session, @PathParam("userId") String userId, @PathParam("channelId") String channelId) {
        
        ChannelDAO channel; UserDAO user;
        auth.checkCookie(session, userId);

        try {
            channel = db.getById(channelId, ChannelDAO.class);
            user = db.getById(userId, UserDAO.class);
        } catch (CosmosException e) {
            throw new WebApplicationException(e.getStatusCode());
        }

        if (channel == null || user == null)
            throw new WebApplicationException(Status.BAD_REQUEST);

        if(!channel.isPublicChannel()) {
            throw new WebApplicationException(Status.FORBIDDEN);
        }

        memberAddition(channel, user);
    }

    /**
     * Unsubscribes given user to a given channel
     * 
     * @param id - id of the user that will subscribe
     * @param id - id of the channel the user will subscribe to
     */
    @PUT
    @Path("/{userId}/unsubscribe/{channelId}")
    public void unsubChannel(@CookieParam("scc:session") Cookie session, @PathParam("userId") String userId, @PathParam("channelId") String channelId) {

        ChannelDAO channel; UserDAO user;
        auth.checkCookie(session, userId);

        try {
            channel = db.getById(channelId, ChannelDAO.class);
            user = db.getById(userId, UserDAO.class);
        } catch (CosmosException e) {
            throw new WebApplicationException(e.getStatusCode());
        }

        if (channel == null || user == null)
            throw new WebApplicationException(Status.BAD_REQUEST);

        memberRemoval(channel, user);
    }

    @PUT
    @Path("/channels/{channelId}/add/{userId}")
    public void addMember(@CookieParam("scc:session") Cookie session, @PathParam("channelId") String channelId, @PathParam("userId") String userId) {
        
        UserDAO user = null; ChannelDAO channel = null;

        try {
            channel = db.getById(channelId, ChannelDAO.class);
            if(channel != null) 
                auth.checkCookie(session, channel.getOwner());
            user = db.getById(userId, UserDAO.class);
        } catch (CosmosException e) {
            throw new WebApplicationException(e.getStatusCode());
        }

        if (channel == null || user == null)
            throw new WebApplicationException(Status.BAD_REQUEST);

        memberAddition(channel, user);
    }

    @PUT
    @Path("/channels/{channelId}/remove/{userId}")
    public void removeMember(@CookieParam("scc:session") Cookie session, @PathParam("channelId") String channelId, @PathParam("userId") String userId){
        
        UserDAO user = null; ChannelDAO channel = null;
 
        try {
            channel = db.getById(channelId, ChannelDAO.class);
            if(channel != null) 
                auth.checkCookie(session, channel.getOwner());
            
            user = db.getById(userId, UserDAO.class);
        } catch (CosmosException e) {
            throw new WebApplicationException(e.getStatusCode());
        }

        if (channel == null || user == null)
            throw new WebApplicationException(Status.NOT_FOUND);
        
        memberRemoval(channel, user);
    }

    private void memberAddition(ChannelDAO channel, UserDAO user) {
        String channelId = channel.getId();
        String userId = user.getId();

        if (Arrays.asList(channel.getMembers()).contains(userId)) 
            throw new WebApplicationException(Status.BAD_REQUEST);

        db.patchAdd(channelId, ChannelDAO.class, "/members", userId);
        db.patchAdd(userId, UserDAO.class, "/channelIds", channelId);

        user = db.getById(userId, UserDAO.class);
        if (user != null)
            cache.setValue(userId, new User(user));

        channel = db.getById(channelId, ChannelDAO.class);
        if(channel != null)
            cache.setValue(channelId, new Channel(channel));
    }

    private void memberRemoval(ChannelDAO channel, UserDAO user) {
        String userId = user.getId();
        String channelId = channel.getId();

        List<String> membersList = Arrays.asList(channel.getMembers());
        List<String> channelIds = Arrays.asList(user.getChannelIds());

        if (!membersList.contains(userId) || channel.getOwner().equals(userId)) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        db.patchRemove(channelId, ChannelDAO.class, "/members", membersList.indexOf(userId));
        db.patchRemove(userId, UserDAO.class, "/channelIds", channelIds.indexOf(channelId));

        user = db.getById(userId, UserDAO.class);
        if (user != null)
            cache.setValue(userId, new User(user));

        channel = db.getById(channelId, ChannelDAO.class);
        if(channel != null)
            cache.setValue(channelId, new Channel(channel));
    }

    
    @POST
    @Path("/auth")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response authenticate(Login login) {
        UserDAO user = null;
        String id = login.getId(); 
        String pwd = login.getPwd();
        //TODO CACHE
        if (id == null || pwd == null)
            throw new WebApplicationException(Status.BAD_REQUEST);

        try {
            user = db.getById(id, UserDAO.class);
        } catch (CosmosException e) {
            throw new WebApplicationException(e.getStatusCode());
        }

        if(user != null && pwd.equals(user.getPwd())) {
            String uid = UUID.randomUUID().toString();
            NewCookie cookie = new NewCookie("scc:session", uid, "/", null, "sessionid", 3600, false, true);
            cache.putSession(uid, id);
            return Response.ok().cookie(cookie).build();
        } else {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }        
    }

}
