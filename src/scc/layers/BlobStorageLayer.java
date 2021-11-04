package scc.layers;

import java.util.ArrayList;
import java.util.List;

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

	public void upload(byte[] media, String key) {

		BinaryData data = BinaryData.fromBytes(media);
		BlobClient blob = containerClient.getBlobClient(key);

		blob.upload(data);
	}

	public byte[] download(String id) {

		BlobClient blob = containerClient.getBlobClient(id);
		BinaryData data = blob.downloadContent();
		byte[] media = data.toBytes();

        return media;
    }

	public List<String> list() {

		List<String> images = new ArrayList<String>();
		for (BlobItem blobItem : containerClient.listBlobs()) {
			images.add(blobItem.getName());
		}

		return images;
	}

}
