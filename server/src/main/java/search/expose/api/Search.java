package search.expose.api;

import search.expose.ElasticSearch;
import org.json.JSONObject;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.io.IOException;

@Path("/search")
public class Search {

    /** when testing, this is reachable at http://localhost:8080/api/search?query=hello */
    @GET
    public Response getMsg(@QueryParam("query") String query, @QueryParam("language") String language,
                           @QueryParam("date") String date, @QueryParam("count") String count,
                           @QueryParam("offset") String offset) throws IOException {
        JSONObject results = new JSONObject();
        if (query == null) {
            return Response.status(400).type("application/json").entity("parameter is missing").build();
        }

        ElasticSearch elasticSearch = new ElasticSearch("es");

        results = elasticSearch.search(query, language, date, count, offset);

        return Response.status(200).type("application/json").entity(results.toString(4))
                // below header is for CORS
                .header("Access-Control-Allow-Origin", "*").build();
    }
}
