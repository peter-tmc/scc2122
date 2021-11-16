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

@Path(ChannelResources.PATH)
public class ChannelResources {

    public static final String PATH = "channels";
    private CosmosDBLayer db = CosmosDBLayer.getInstance();
    private RedisCache cache = RedisCache.getInstance();
    private CookieAuth auth = CookieAuth.getInstance();

    /**
     * Creates a channel, given its object.
     * 
     * @param channel - channel object
     * @return the generated id
     */
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Channel createChannel(@CookieParam("scc:session") Cookie session, Channel channel) {

        String id = UUID.randomUUID().toString();
        channel.setId(id);
        String owner = channel.getOwner();

        if (channel.getName() == null || owner == null)
            throw new WebApplicationException(Status.BAD_REQUEST);

        auth.checkCookie(session, owner);

        try {
            if (channel.getMembers().length != 1 || !channel.getMembers()[0].equals(owner)
                    || db.getById(channel.getOwner(), UserDAO.class) == null)
                throw new WebApplicationException(Status.BAD_REQUEST);
            db.patchAdd(owner, UserDAO.class, "/channelIds", id);
            UserDAO u = db.getById(owner, UserDAO.class);
            if (u != null) {
                cache.setValue(owner, new User(u));
            }

            db.put(new ChannelDAO(channel));
            ChannelDAO cdao = db.getById(id, ChannelDAO.class);
            if (cdao != null) {
                Channel c = new Channel(cdao);
                cache.setValue(c.getId(), c);
            }
            
            // TODO adicionar o channel a channelList dos membros com um HTTP trigger
            return channel;
        } catch (CosmosException e) {
            int status = (e.getStatusCode() == Status.NOT_FOUND.getStatusCode()) ? Status.BAD_REQUEST.getStatusCode()
                    : e.getStatusCode();
            throw new WebApplicationException(status);
        }
    }

    /**
     * Deletes a channel, given its id.
     * 
     * @param id - id of the channel to be deleted
     */
    @DELETE
    @Path("/{id}")
    public void deleteChannel(@CookieParam("scc:session") Cookie session, @PathParam("id") String id) {
        
        
        try {
            // TODO timer trigger para eliminar da list dos users e etc
            Channel channel = cache.getValue(id, Channel.class);
            if(channel == null) {
                ChannelDAO chan = db.getById(id, ChannelDAO.class);
                if(chan != null) {
                    channel = new Channel(chan);
                }
                else {
                    throw new WebApplicationException(Status.NOT_FOUND);
                }
            }
            auth.checkCookie(session, channel.getOwner());
            db.delById(id, ChannelDAO.class);
        } catch (CosmosException e) {
            throw new WebApplicationException(e.getStatusCode());
        }
        cache.delete(id, Channel.class);
    }

    /**
     * Updates a channel, given its object.
     * 
     * @param id - id of the channel to be updated
     */
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateChannel(@CookieParam("scc:session") Cookie session, @PathParam("id") String id, Channel channel) {
        ChannelDAO preChannel;

        try {
            preChannel = db.getById(id, ChannelDAO.class);
        } catch (CosmosException e) {
            throw new WebApplicationException(e.getStatusCode());
        }

        if (preChannel == null || channel.getId() == null || channel.getName() == null || channel.getOwner() == null
                || !channel.getId().equals(preChannel.getId()) || !channel.getOwner().equals(preChannel.getOwner())
                || !Arrays.equals(channel.getMembers(), preChannel.getMembers())) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        auth.checkCookie(session, preChannel.getOwner());

        try {
            db.delById(id, ChannelDAO.class);
            db.put(new ChannelDAO(channel));

            ChannelDAO cdao = db.getById(id, ChannelDAO.class);
            if (cdao != null) {
                Channel c = new Channel(cdao);
                cache.setValue(c.getId(), c);
            }
        } catch (CosmosException e) {
            throw new WebApplicationException(e.getStatusCode());
        }

    }

    /**
     * Gets a channel, given its id
     * 
     * @param id - id of the channel to retrieve
     * @return the channel
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Channel getChannel(@CookieParam("scc:session") Cookie session, @PathParam("id") String id) {
        Channel c = cache.getValue(id, Channel.class);
        try {
            if(c == null) {
                ChannelDAO channel = db.getById(id, ChannelDAO.class);
                if (channel == null)
                    throw new WebApplicationException(Status.NOT_FOUND);
                c = new Channel(channel);
                cache.setValue(id, c);
            }

            if(!c.isPublicChannel()) {
                String userId = cache.getSession(session.getValue());

                if (userId == null || !Arrays.asList(c.getMembers()).contains(userId))
                    throw new WebApplicationException(Status.UNAUTHORIZED);
            }
 
            return c;
        } catch (CosmosException e) {
            throw new WebApplicationException(e.getStatusCode());
        }
    }

    /**
     * @return the list of the trending channels
     */
    @GET
    @Path("/trending")
    @Produces(MediaType.APPLICATION_JSON)
    public String[] trendingChannels() {
        return null;
    }

}
