package nl.multicode.gemeente.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.multicode.gemeente.dto.IngestRequest;
import nl.multicode.gemeente.service.ChunkService;
import nl.multicode.gemeente.service.OllamaService;
import nl.multicode.gemeente.service.QdrantService;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

@RestController
@RequestMapping("/ingest")
public class IngestController {

    private final QdrantService qdrant;
    private final OllamaService ollama;
    private final ChunkService chunker;
    private final ObjectMapper mapper = new ObjectMapper();

    public IngestController(QdrantService qdrant, OllamaService ollama, ChunkService chunker) {
        this.qdrant = qdrant;
        this.ollama = ollama;
        this.chunker = chunker;
    }

    @PostMapping
    public Map<String,Object> ingest(@RequestBody IngestRequest req) throws Exception {
        qdrant.ensureCollection();
        File f = new File(req.docsPath());
        if (!f.exists()) throw new IllegalArgumentException("docsPath not found: " + req.docsPath());

        int docs = 0;
        int chunks = 0;
        List<QdrantService.Point> batch = new ArrayList<>();

        try (var br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                JsonNode j = mapper.readTree(line);
                String id = j.get("id").asText();
                String url = j.path("url").asText("");
                String title = j.path("title").asText("");
                String published = j.path("published_at").asText("");
                String text = j.path("text").asText("");

                List<String> parts = chunker.chunk(text, 600, 60);
                if (parts.isEmpty()) continue;
                docs++;
                for (int i=0;i<parts.size();i++) {
                    String chunk = parts.get(i);
                    float[] vec = ollama.embed(chunk);
                    Map<String,Object> payload = new HashMap<>();
                    payload.put("url", url);
                    payload.put("title", title);
                    payload.put("published_at", published);
                    payload.put("text", chunk);
                    try {
                        payload.put("site", new java.net.URI(url).getHost());
                    } catch (Exception ignored) {}
                    batch.add(new QdrantService.Point(id + "-" + i, toList(vec), payload));
                    chunks++;
                    if (batch.size() >= 128) {
                        qdrant.upsert(batch);
                        batch.clear();
                    }
                }
            }
        }
        if (!batch.isEmpty()) qdrant.upsert(batch);

        return Map.of("status","ok","docs",docs,"chunks",chunks);
    }

    private static List<Float> toList(float[] vec) {
        List<Float> out = new ArrayList<>(vec.length);
        for (float v : vec) out.add(v);
        return out;
    }
}
