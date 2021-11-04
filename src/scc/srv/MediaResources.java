package scc.srv;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import java.util.ArrayList;
import java.util.List;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.*;
import com.azure.storage.blob.models.BlobItem;

import javax.ws.rs.*;
import scc.layers.*;
import scc.utils.*;

@Path(MediaResources.PATH)
public class MediaResources {

    public static final String PATH = "/media";

    String connection = System.getenv("BlobStoreConnection");

    BlobContainerClient containerClient = new BlobContainerClientBuilder()
        .connectionString(connection)
        .containerName("images")
        .buildClient();

    /**
     * Post a new image.The id of the image is its hash.
     */
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public String upload(byte[] contents) {
        String id = Hash.of(contents);
        BlobClient blob = containerClient.getBlobClient(id);
        if(blob.exists()){
            throw new WebApplicationException(Status.CONFLICT);
        }
        blob.upload(BinaryData.fromBytes(contents));
        return id;
    }

    /**
     * Return the contents of an image. Throw an appropriate error message if
     * id does not exist.
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public byte[] download(@PathParam("id") String id) {
        BlobClient blob = containerClient.getBlobClient(id);
        if(!blob.exists()){
            throw new WebApplicationException(Status.NOT_FOUND);
        }
        BinaryData data = blob.downloadContent();
        return data.toBytes();
    }

    /**
     * Lists the ids of images stored.
     */
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> list() {
        List<String> list = new ArrayList<>();
        for (BlobItem image : containerClient.listBlobs()) {
            list.add(image.getName());
        }
        return list;
    }
}