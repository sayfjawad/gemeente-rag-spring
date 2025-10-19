# Gemeente RAG — Spring Boot (Maven)

Local Spring Boot API that:
- Ingests crawler output (`out/docs.jsonl`) into **Qdrant** using **Ollama** embeddings
- Answers questions via `/ask` using retrieved chunks + **Ollama** chat

## Prereqs (macOS)
- Docker (Desktop or Colima) and `docker compose`
- Ollama running locally with models:
  ```bash
  brew install ollama
  ollama serve &
  ollama pull llama3.1:8b
  ollama pull nomic-embed-text
  ```
- Qdrant:
  ```bash
  docker compose up -d
  ```

## Build & run
```bash
./mvnw spring-boot:run
```

## Endpoints
- **POST** `/ingest` — body: `{"docsPath": "PATH/TO/out/docs.jsonl"}`  
  Reads JSONL, chunks (600/60), embeds, upserts into Qdrant.
- **POST** `/ask` — body: `{"question":"Wat zijn de parkeertarieven?", "k": 6, "site": "www.voorbeeld-gemeente.nl"}`  
  Returns grounded answer + sources.

## Config (env overrides)
See `src/main/resources/application.yml`:
- `QDRANT_HOST` (default 127.0.0.1)
- `QDRANT_PORT` (6333)
- `QDRANT_COLLECTION` (gemeente_docs)
- `VECTOR_SIZE` (768 for nomic-embed-text)
- `OLLAMA_URL` (http://127.0.0.1:11434)
- `EMBED_MODEL` (nomic-embed-text)
- `CHAT_MODEL` (llama3.1:8b)

## Notes
- Pair this with the Python crawler from earlier (`docs.jsonl` output).
- You can later replace the ingest to parse HTML/PDF directly in Java (Apache Tika is already included).
