# Go Hello World Demo

A simple Go HTTP server for Jenkins pipeline testing.

## Build & Run

```bash
go build -o hello .
./hello
```

## Docker

```bash
docker build -t hello-world .
docker run -p 8080:8080 hello-world
```

## Endpoints
- `GET /` - Hello World
- `GET /health` - Health check