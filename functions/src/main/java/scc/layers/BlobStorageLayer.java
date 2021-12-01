package scc.layers;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.*;
import com.azure.storage.blob.models.BlobItem;

public class BlobStorageLayer {
	private static BlobStorageLayer instance;
	
	private BlobContainerClient containerClient;

	public static synchronized BlobStorageLayer getInstance() {
		if( instance != null)
			return instance;

 		String connection = System.getenv("BlobStoreConnection");

		BlobContainerClient containerClient = new BlobContainerClientBuilder()
		 		.connectionString(connection)
		 		.containerName("images")
		 		.buildClient();

		instance = new BlobStorageLayer(containerClient);
		return instance;
	}
	
	public BlobStorageLayer(BlobContainerClient containerClient ) {
		this.containerClient = containerClient;
	}

	public void upload(byte[] media, String id) {

		BlobClient blob = containerClient.getBlobClient(id);
        if(!blob.exists()){
			blob.upload(BinaryData.fromBytes(media));
        }
	}

	public byte[] download(String id) {

		BlobClient blob = containerClient.getBlobClient(id);
        if(!blob.exists()){
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        BinaryData data = blob.downloadContent();

        return data.toBytes();
    }

	public List<String> list() {
		List<String> list = new ArrayList<>();
        for (BlobItem image : containerClient.listBlobs()) {
            list.add(image.getName());
        }

		return list;
	}

	public boolean blobExists(String id) {
		BlobClient blob = containerClient.getBlobClient(id);
        return blob.exists();
	}

}
