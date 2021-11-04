package scc.srv;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.*;
import scc.data.*;
import scc.utils.*;

@Path(ChannelResources.PATH)
public class ChannelResources {

    public static final String PATH = "channels";
    private CosmosDBLayer db = CosmosDBLayer.getInstance();
    private BlobStorageLayer blob = BlobStorageLayer.getInstance();

    public ChannelResources() {
        db = CosmosDBLayer.getInstance();
        blob = BlobStorageLayer.getInstance();
    }

    /**
     * Creates a channel, given its object.
     * @param channel - channel object
     * @return the generated id
     */
    @POST
    @Path("")
    @Consumes(MediaType.APPLICATION_JSON)
    public String createChannel(Channel channel) {
        return null;
    }

    /**
     * Deletes a channel, given its id.
     * @param id - id of the channel to be deleted
     */
    @DELETE
    @Path("{id}")
    public void deleteChannel(@PathParam("id") String id){

    }

    /**
     * Updates a channel, given its object.
     * @param id - id of the channel to be updated
     */
    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateChannel(@PathParam("id") String id){

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
