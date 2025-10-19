package nl.multicode.gemeente.controller;

import nl.multicode.gemeente.dto.AskRequest;
import nl.multicode.gemeente.dto.AskResponse;
import nl.multicode.gemeente.service.OllamaService;
import nl.multicode.gemeente.service.QdrantService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

@RestController
@RequestMapping("/ask")
public class AskController {

    private final OllamaService ollama;
    private final QdrantService qdrant;

    public AskController(OllamaService ollama, QdrantService qdrant) {
        this.ollama = ollama;
        this.qdrant = qdrant;
    }

    @PostMapping
    public AskResponse ask(@RequestBody AskRequest req) throws Exception {
        int k = (req.k() == null || req.k() <= 0) ? 6 : req.k();
        float[] query = ollama.embed(req.question());
        var hits = qdrant.search(query, k, req.site());

        var ctx = new StringBuilder();
        var sources = new ArrayList<String>();
        int i = 1;
        for (var h : hits) {
            var snippet = h.text();
            if (snippet.length() > 1200) snippet = snippet.substring(0, 1200);
            ctx.append("[").append(i).append("] Title: ").append(h.title()).append("\n")
               .append("URL: ").append(h.url()).append("\n")
               .append("Date: ").append(h.publishedAt()).append("\n")
               .append("Text: ").append(snippet).append("\n\n");
            sources.add("[" + i + "] " + h.title() + " — " + h.url() + " (" + h.publishedAt() + ")");
            i++;
        }

        String system = "Je bent een behulpzame assistent voor gemeentelijke informatie. " +
                "Antwoord uitsluitend op basis van de context. Als iets niet in de context staat, " +
                "zeg dan eerlijk dat het onbekend is. Geef een kort en feitelijk antwoord in het Nederlands " +
                "en sluit af met 'Bronnen' met de genoemde nummers.";
        String prompt = system + "\n\nContext:\n" + ctx + "\nVraag: " + req.question() + "\nAntwoord:";
        String answer = ollama.generate(prompt);
        answer += "\n\nBronnen:\n" + String.join("\n", sources);
        return new AskResponse(answer, hits.size());
    }
}
