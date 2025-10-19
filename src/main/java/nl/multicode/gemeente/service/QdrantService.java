package nl.multicode.gemeente.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class QdrantService {

    private final OkHttpClient http = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${rag.qdrant.host}") private String host;
    @Value("${rag.qdrant.port}") private int port;
    @Value("${rag.qdrant.collection}") private String collection;
    @Value("${rag.qdrant.vectorSize}") private int vectorSize;

    private String base() { return "http://" + host + ":" + port; }

    public void ensureCollection() throws IOException {
        var payload = mapper.createObjectNode();
        var vectors = mapper.createObjectNode();
        vectors.put("size", vectorSize);
        vectors.put("distance", "Cosine");
        payload.set("vectors", vectors);
        RequestBody body = RequestBody.create(payload.toString(), MediaType.parse("application/json"));
        Request req = new Request.Builder()
                .url(base() + "/collections/" + collection)
                .put(body).build();
        try (var res = http.newCall(req).execute()) {
            if (!res.isSuccessful() && res.code() != 409) {
                throw new IOException("Failed to ensure collection: " + res.code());
            }
        }
    }

    public void upsert(List<Point> points) throws IOException {
        var root = mapper.createObjectNode();
        var ps = mapper.createArrayNode();
        for (Point p : points) {
            var obj = mapper.createObjectNode();
            obj.put("id", p.id());
            obj.set("vector", mapper.valueToTree(p.vector()));
            obj.set("payload", mapper.valueToTree(p.payload()));
            ps.add(obj);
        }
        root.set("points", ps);
        RequestBody body = RequestBody.create(root.toString(), MediaType.parse("application/json"));
        Request req = new Request.Builder().url(base()+"/collections/"+collection+"/points").post(body).build();
        try (var res = http.newCall(req).execute()) {
            if (!res.isSuccessful()) throw new IOException("Upsert failed: " + res.code() + " " + res.message());
        }
    }

    public List<SearchHit> search(float[] query, int k, String site) throws IOException {
        var root = mapper.createObjectNode();
        root.set("vector", mapper.valueToTree(query));
        root.put("limit", k);
        root.put("with_payload", true);
        if (site != null && !site.isBlank()) {
            var filter = mapper.createObjectNode();
            var must = mapper.createArrayNode();
            var cond = mapper.createObjectNode();
            cond.put("key", "site");
            var m = mapper.createObjectNode();
            m.putArray("any").add(site);
            cond.set("match", m);
            must.add(cond);
            filter.set("must", must);
            root.set("filter", filter);
        }
        RequestBody body = RequestBody.create(root.toString(), MediaType.parse("application/json"));
        Request req = new Request.Builder().url(base()+"/collections/"+collection+"/points/search").post(body).build();
        try (var res = http.newCall(req).execute()) {
            if (!res.isSuccessful()) throw new IOException("Search failed: " + res.code());
            var list = new ArrayList<SearchHit>();
            JsonNode arr = new ObjectMapper().readTree(res.body().string());
            if (arr.isArray()) {
                for (JsonNode it : arr) {
                    var payload = it.get("payload");
                    list.add(new SearchHit(
                            payload.path("title").asText(""),
                            payload.path("url").asText(""),
                            payload.path("published_at").asText(""),
                            payload.path("text").asText(""),
                            it.path("score").asDouble(0.0)
                    ));
                }
            }
            return list;
        }
    }

    public record Point(String id, List<Float> vector, Map<String,Object> payload) {}
    public record SearchHit(String title, String url, String publishedAt, String text, double score) {}
}
