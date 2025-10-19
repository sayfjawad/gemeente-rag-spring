package nl.multicode.gemeente.service;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChunkService {
    public List<String> chunk(String text, int sizeTokens, int overlapTokens) {
        var words = text.split("\s+");
        var chunks = new ArrayList<String>();
        int step = sizeTokens - overlapTokens;
        for (int i = 0; i < words.length; i += step) {
            int end = Math.min(i + sizeTokens, words.length);
            if (end - i < 100) break;
            var sb = new StringBuilder();
            for (int j = i; j < end; j++) sb.append(words[j]).append(' ');
            chunks.add(sb.toString().trim());
        }
        return chunks;
    }
}
