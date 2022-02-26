package search.expose;

import org.json.JSONArray;
import org.json.JSONObject;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpMethod;

import java.io.IOException;
import java.util.*;

public class ElasticSearch extends AwsSignedRestRequest{

    /**
     * @param serviceName would be "es" for Elasticsearch
     */
    public ElasticSearch(String serviceName) {
        super(serviceName);
    }

    public void createIndex() throws IOException {
        JSONObject jsonObject = new JSONObject()
                .put("settings", new JSONObject().put("number_of_shards", 3)
                        .put("number_of_replicas", 2));
        Optional<JSONObject> json = Optional.of(jsonObject);
        HttpExecuteResponse response = this.restRequest(SdkHttpMethod.PUT,
                System.getenv("ELASTIC_SEARCH_HOST"),
                System.getenv("ELASTIC_SEARCH_INDEX"),
                Optional.empty(), json);
        response.responseBody().get().close();
    }

    public void postDocument(Article key) throws IOException {
        JSONObject jsonObject = new JSONObject()
                .put("url", key.getUrl())
                .put("title", key.getTitle())
                .put("txt", key.getTxt());
        Optional<JSONObject> json = Optional.of(jsonObject);
        HttpExecuteResponse response = this.restRequest(SdkHttpMethod.POST,
                System.getenv("ELASTIC_SEARCH_HOST"),
                System.getenv("ELASTIC_SEARCH_INDEX") + "/_doc/",
                Optional.empty(), json);
        response.responseBody().get().close();
    }

    public JSONObject search(String query, String language, String date, String count, String offset) throws IOException {
        Map<String, String> map = new HashMap<>();
        StringBuilder stringBuilder = new StringBuilder("txt:");

        String[] queries = query.split(" ");
        for (String s : queries) {
            stringBuilder.append(s).append("+");
        }
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);

        if (language != null) {
            stringBuilder.append(" AND lang:").append(language);
        }

        if (date != null) {
            stringBuilder.append(" AND date:").append(date);
        }

        map.put("q", stringBuilder.toString());
        if (count != null)
            map.put("size", count);
        if (offset != null)
            map.put("from", offset);

        HttpExecuteResponse response = this.restRequest(SdkHttpMethod.GET,
                Config.getParam("ELASTIC_SEARCH_HOST"),
                Config.getParam("ELASTIC_SEARCH_INDEX") + "/_search",
                Optional.of(map));
        AbortableInputStream inputStream = response.responseBody().get();
        byte[] b = new byte[2];
        StringBuilder rs = new StringBuilder("");
        while (inputStream.read(b) != -1) {
            rs.append(new String(b));
        }
        inputStream.close();

        JSONObject rsJson = new JSONObject(rs.toString()).getJSONObject("hits");

        JSONObject results = new JSONObject();
        results.put("returned_results", rsJson.getJSONArray("hits").length());
        results.put("total_results", rsJson.getJSONObject("total").getInt("value"));

        JSONArray jsonArray = rsJson.getJSONArray("hits");
        List<Article> articles = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject t = jsonArray.getJSONObject(i);
            if (t.has("_source")) {
                JSONObject item = t.getJSONObject("_source");
                Article article = new Article(item.getString("title"),
                        item.getString("url"), item.getString("txt"),
                        item.has("date") && item.getString("date") != null ? item.getString("date") : "",
                        item.has("lang") && item.getString("lang") != null ? item.getString("lang") : "");
                articles.add(article);
            }
        }
        results.put("articles", new JSONArray(articles));

        return results;
    }
}
