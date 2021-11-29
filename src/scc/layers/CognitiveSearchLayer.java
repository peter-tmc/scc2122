package scc.layers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.azure.core.credential.*;
import com.azure.search.documents.SearchClient;
import com.azure.search.documents.SearchClientBuilder;
import com.azure.search.documents.SearchDocument;
import com.azure.search.documents.models.SearchOptions;
import com.azure.search.documents.util.SearchPagedIterable;
import com.azure.search.documents.util.SearchPagedResponse;

public class CognitiveSearchLayer {

    private static CognitiveSearchLayer instance;

	private SearchClient searchClient;

    public static synchronized CognitiveSearchLayer getInstance() {
		if( instance != null)
			return instance;

		String searchServiceQueryKey = System.getenv("SearchServiceQueryKey");
		String searchServiceUrl = System.getenv("SearchServiceUrl");
		String indexName = System.getenv("IndexName");

		SearchClient searchClient = new SearchClientBuilder()
			.credential(new AzureKeyCredential(searchServiceQueryKey))
			.endpoint(searchServiceUrl)
			.indexName(indexName)
			.buildClient();

		instance = new CognitiveSearchLayer(searchClient);
		return instance;
	}
	
	public CognitiveSearchLayer(SearchClient searchClient) {
		this.searchClient = searchClient;
	}

	public List<Object> query(String queryText, String filter, String searchField) {

		SearchOptions options = new SearchOptions()
			.setIncludeTotalCount(true)
			.setFilter(filter)
			.setSelect("id", "user", "text")
			.setTop(5);

		if(searchField != null)
			options = options.setSearchFields(searchField);

		SearchPagedIterable searchPagedIterable = searchClient.search(queryText, options, null);
		List<Object> results = new ArrayList<Object>();

		for(SearchPagedResponse resultResponse : searchPagedIterable.iterableByPage()) {
			resultResponse.getValue().forEach(searchResult -> {
				for (Map.Entry<String, Object> res : searchResult
						.getDocument(SearchDocument.class)
						.entrySet()) {
							results.add(res.getValue());
			}});
		}

		return results;
	}

    
}