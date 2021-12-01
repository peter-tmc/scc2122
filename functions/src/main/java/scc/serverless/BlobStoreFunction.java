package scc.serverless;

import com.microsoft.azure.functions.annotation.*;

import redis.clients.jedis.Jedis;
import scc.cache.RedisCache;

import com.microsoft.azure.functions.*;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.*;

/**
 * Azure Functions with Blob Trigger.
 */
public class BlobStoreFunction
{
	@FunctionName("blobMonitor")
	public void setLastBlobInfo(@BlobTrigger(name = "blobMonitor", 
									dataType = "binary", 
									path = "images/{name}", 
									connection = "BlobStoreConnection") 
								byte[] content,
								@BindingName("name") String blobname, 
								final ExecutionContext context) {
		try (Jedis jedis = RedisCache.getCachePool().getResource()) {
			jedis.incr("cnt:blob");
			jedis.set("serverless::blob::name",
					"Blob name : " + blobname + " ; size = " + (content == null ? "0" : content.length));
		}

		String connectionRemote = System.getenv("BlobStoreConnectionRemote");
		
		BlobContainerClient containerClient = new BlobContainerClientBuilder()
				.connectionString(connectionRemote)
				.containerName("images")
				.buildClient();

		BlobClient blob = containerClient.getBlobClient(blobname);
		if (!blob.exists()) {
			blob.upload(BinaryData.fromBytes(content));
		}
	}
}
