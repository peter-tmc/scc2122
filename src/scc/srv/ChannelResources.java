package scc.srv;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import com.azure.cosmos.models.CosmosItemResponse;

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
    @Path("")
    @Consumes(MediaType.APPLICATION_JSON)
    public String createChannel(Channel channel) {

        CosmosItemResponse<ChannelDAO> response = db.put(new ChannelDAO(channel));
        if(response != null)
            if(response.getStatusCode() != Status.OK.getStatusCode())
                throw new WebApplicationException(Status.fromStatusCode(response.getStatusCode()));

        return channel.getId();
    }

    /**
     * Deletes a channel, given its id.
     * @param id - id of the channel to be deleted
     */
    @DELETE
    @Path("{id}")
    public void deleteChannel(@PathParam("id") String id){
        CosmosItemResponse<Object> response = db.del(id);
        throw new WebApplicationException(response.getStatusCode());
    }

    /**
     * Updates a channel, given its object.
     * @param id - id of the channel to be updated
     */
    @PATCH
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateChannel(@PathParam("id") String id, Channel channel){
        CosmosItemResponse<Object> response = db.put(new ChannelDAO(channel));
        if (response.getStatusCode() != Status.OK.getStatusCode())
            throw new WebApplicationException(response.getStatusCode());
    }

    /**
     * Gets a channel, given its id
     * @param id - id of the channel to retrieve
     * @return the channel
     */
    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Channel getChannel(@PathParam("id") String id) {
        return null;
    }

    /**
     * @return the list of the trending channels
     */
    @GET
    @Path("trending")
    @Produces(MediaType.APPLICATION_JSON)
    public String[] trendingChannels() {
        return null;
    }
}
