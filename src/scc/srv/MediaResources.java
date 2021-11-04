package scc.srv;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.*;
import scc.data.*;
import scc.utils.*;

@Path(MediaResources.PATH)
public class MediaResources {

    public static final String PATH = "/media";
    private CosmosDBLayer db = CosmosDBLayer.getInstance();
    private BlobStorageLayer blob = BlobStorageLayer.getInstance();

   /**
     * Uploads media content
     * @param media - media to be uploaded
     * @return the generated id
     */
    @POST
    @Path("")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    private String uploadMedia(byte[] media) {
        String key = Hash.of(media);
        blob.upload(media, key);
		return key;
    }

    /**
     * Downloads the media content
     * @param id - id of the media content to be downloaded
     * @return the media content
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    private byte[] downloadMedia(@PathParam("id") String id) {
        return blob.download(id);
    }

}
