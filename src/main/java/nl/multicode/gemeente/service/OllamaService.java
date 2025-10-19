package nl.multicode.gemeente.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class OllamaService {

    private final OkHttpClient http = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${rag.ollama.baseUrl}")
    private String baseUrl;

    @Value("${rag.ollama.embedModel}")
    private String embedModel;

    @Value("${rag.ollama.chatModel}")
    private String chatModel;

    public float[] embed(String text) throws IOException {
        RequestBody body = RequestBody.create(
                mapper.createObjectNode()
                        .put("model", embedModel)
                        .put("prompt", text)
                        .toString(),
                MediaType.parse("application/json")
        );
        Request req = new Request.Builder()
                .url(baseUrl + "/api/embeddings")
                .post(body)
                .build();
        try (Response res = http.newCall(req).execute()) {
            if (!res.isSuccessful()) throw new IOException("Embedding failed: " + res.code());
            JsonNode json = mapper.readTree(res.body().string());
            JsonNode arr = json.get("embedding");
            float[] vec = new float[arr.size()];
            for (int i=0;i<arr.size();i++) vec[i] = (float) arr.get(i).asDouble();
            return vec;
        }
    }

    public String generate(String prompt) throws IOException {
        var root = mapper.createObjectNode();
        root.put("model", chatModel);
        root.put("prompt", prompt);
        var opts = mapper.createObjectNode();
        opts.put("temperature", 0.2);
        root.set("options", opts);
        root.put("stream", false);
        RequestBody body = RequestBody.create(root.toString(), MediaType.parse("application/json"));
        Request req = new Request.Builder().url(baseUrl + "/api/generate").post(body).build();
        try (Response res = http.newCall(req).execute()) {
            if (!res.isSuccessful()) throw new IOException("Generate failed: " + res.code());
            JsonNode json = mapper.readTree(res.body().string());
            return json.get("response").asText();
        }
    }
}
