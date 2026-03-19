FROM --platform=$BUILDPLATFORM golang:1.26-alpine AS builder
WORKDIR /app
RUN apk add --no-cache git ca-certificates

ARG TARGETOS
ARG TARGETARCH

COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 GOOS=${TARGETOS} GOARCH=${TARGETARCH} go build -o simulator .

FROM alpine:latest
WORKDIR /app
RUN apk --no-cache add ca-certificates tzdata
COPY --from=builder /app/simulator .
CMD ["./simulator"]