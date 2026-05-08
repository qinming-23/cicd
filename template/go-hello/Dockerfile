FROM golang:1.21-alpine AS builder
WORKDIR /app
COPY go.mod main.go ./
RUN go build -o hello .

FROM alpine:3.19
WORKDIR /app
COPY --from=builder /app/hello .
EXPOSE 8080
CMD ["./hello"]