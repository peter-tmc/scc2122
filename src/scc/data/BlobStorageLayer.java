package scc.data;

import java.util.ArrayList;
import java.util.List;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.*;
import com.azure.storage.blob.models.BlobItem;

public class BlobStorageLayer {
	private static BlobStorageLayer instance;
	private String connection;

	public static synchronized BlobStorageLayer getInstance() {
		if( instance != null)
			return instance;

 		String connection = System.getenv("BlobStoreConnection");
		instance = new BlobStorageLayer(connection);
		return instance;
	}
	
	public BlobStorageLayer(String connection) {
		this.connection = connection;
	}

	public void upload(byte[] media, String key) {

		BinaryData data = BinaryData.fromBytes(media);

		BlobContainerClient containerClient = new BlobContainerClientBuilder()
		.connectionString(connection)
		.containerName("media")
		.buildClient();
	
		BlobClient blob = containerClient.getBlobClient(key);

		blob.upload(data);
	}

	public byte[] download(String id) {
        
        BlobContainerClient containerClient = new BlobContainerClientBuilder()
		.connectionString(connection)
		.containerName("media")
		.buildClient();

		BlobClient blob = containerClient.getBlobClient(id);

		BinaryData data = blob.downloadContent();
		byte[] media = data.toBytes();

        return media;
    }

	public List<String> list() {

		BlobContainerClient containerClient = new BlobContainerClientBuilder()
		.connectionString(connection)
		.containerName("media")
		.buildClient();

		List<String> images = new ArrayList<String>();
		for (BlobItem blobItem : containerClient.listBlobs()) {
			images.add(blobItem.getName());
		}

		return images;
	}

}
