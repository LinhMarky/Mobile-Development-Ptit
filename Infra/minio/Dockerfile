# Build the pinned official source because the former public images are unavailable.
FROM golang:1.24-alpine AS build
ARG MINIO_VERSION=RELEASE.2025-04-22T22-12-26Z
ENV CGO_ENABLED=0
ENV GOMAXPROCS=2
RUN go install -p 2 github.com/minio/minio@${MINIO_VERSION}

FROM alpine:3.22
RUN apk add --no-cache ca-certificates \
    && addgroup -S minio && adduser -S -G minio minio \
    && mkdir /data && chown minio:minio /data
COPY --from=build /go/bin/minio /usr/local/bin/minio
USER minio
EXPOSE 9000 9001
ENTRYPOINT ["/usr/local/bin/minio"]
CMD ["server", "/data", "--console-address", ":9001"]
