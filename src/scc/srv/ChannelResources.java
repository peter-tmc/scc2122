package scc.srv;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import com.azure.cosmos.models.CosmosItemResponse;

import java.util.Arrays;
import java.util.UUID;

import javax.ws.rs.*;
import scc.data.*;
import scc.layers.*;

@Path(ChannelResources.PATH)
public class ChannelResources {

    public static final String PATH = "channels";
    private CosmosDBLayer db = CosmosDBLayer.getInstance();

    /**
     * Creates a channel, given its object.
     * @param channel - channel object
     * @return the generated id
     */
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public String createChannel(Channel channel) {
        String id = channel.getId();

        if(id == null || channel.getName() == null || channel.getOwner() == null) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        if(db.getById(id, ChannelDAO.class) != null) {
            throw new WebApplicationException(Status.CONFLICT);
        }

        for(String member : channel.getMembers()) {
            if(db.getById(member, UserDAO.class) == null) {
                throw new WebApplicationException(Status.BAD_REQUEST);
            }
        }

        db.put(new ChannelDAO(channel));
        return id;
    }

    /**
     * Deletes a channel, given its id.
     * @param id - id of the channel to be deleted
     */
    @DELETE
    @Path("/{id}")
    public void deleteChannel(@PathParam("id") String id) {
        if(db.getById(id, ChannelDAO.class) == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }        
        db.del(id);
    }

    /**
     * Updates a channel, given its object.
     * @param id - id of the channel to be updated
     */
    @PATCH
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateChannel(@PathParam("id") String id, Channel channel){
        deleteChannel(id);
        createChannel(channel);
    }

    /**
     * Gets a channel, given its id
     * @param id - id of the channel to retrieve
     * @return the channel
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Channel getChannel(@PathParam("id") String id) {
        ChannelDAO channel = db.getById(id, ChannelDAO.class);
        if(channel == null)
            throw new WebApplicationException(Status.NOT_FOUND);

        return new Channel(channel);
    }

    /**
     * 
     */
    @PUT
    @Path("/{channelId}/members/add/{userId}")
    public void subChannel(@PathParam("channelId") String channelId, @PathParam("userId") String userId) {
        ChannelDAO channel = db.getById(channelId, ChannelDAO.class);

        if(channel == null || db.getById(userId, UserDAO.class) == null)
            throw new WebApplicationException(Status.NOT_FOUND);

        String[] members = channel.getMembers();

        if(Arrays.asList(members).contains(userId)) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        String[] newMembers = Arrays.copyOf(members, members.length+1);
        newMembers[members.length] = userId;
        db.patch(channelId, ChannelDAO.class, "/members", newMembers.toString());
    }

    /**
     *
     */
    @PUT
    @Path("/{channelId}/members/remove/{userId}")
    public void unsubChannel(@PathParam("channelId") String channelId, @PathParam("userId") String userId){
        ChannelDAO channel = db.getById(channelId, ChannelDAO.class);

        if(channel == null || db.getById(userId, UserDAO.class) == null)
        throw new WebApplicationException(Status.NOT_FOUND);

        String[] members = channel.getMembers();

        if(!Arrays.asList(members).contains(userId)) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        String[] newMembers = Arrays.stream(members).filter((val) -> val!=userId)/* .map((x) -> x) */.toArray(String[]::new);
        db.patch(channelId, ChannelDAO.class, "/members", newMembers.toString());
    }

    /**
     * @return the list of the trending channels
     */
    @GET
    @Path("/trending")
    @Produces(MediaType.APPLICATION_JSON)
    public String[] trendingChannels() {
        //TODO: morrer
        return null;
    }
}
