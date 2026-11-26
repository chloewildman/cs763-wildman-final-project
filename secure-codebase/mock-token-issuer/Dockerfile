FROM python:3.12-slim

# Pinned to the version the tests were run against, so rebuilds behave the same.
RUN pip install --no-cache-dir cryptography==48.0.0

# Run as a non-root user. The keys folder must belong to that user.
RUN useradd --create-home --uid 10001 issuer \
 && mkdir -p /app/keys \
 && chown issuer /app/keys

WORKDIR /app
COPY mock_issuer.py .

ENV KEYS_DIR=/app/keys \
    ISSUER=http://localhost:9000 \
    AUDIENCE=cs763-api \
    ALG=RS256 \
    PORT=9000 \
    HOST=0.0.0.0

USER issuer
EXPOSE 9000

# Keys are generated on first start if /app/keys is empty.
# Mount a volume at /app/keys to keep the same keys across restarts.
# Uses PORT, so the check still works if you override it.
HEALTHCHECK --interval=10s --timeout=3s --retries=3 \
  CMD python -c "import os,sys,urllib.request; sys.exit(0 if urllib.request.urlopen('http://127.0.0.1:%s/health' % os.environ.get('PORT', '9000'), timeout=2).status == 200 else 1)"

CMD ["python", "mock_issuer.py", "serve"]
