FROM python:3.14-slim

ENV POETRY_VERSION=2.3.2 \
    PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    POETRY_NO_INTERACTION=1 \
    POETRY_VIRTUALENVS_CREATE=false

WORKDIR /app

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
RUN pip install "poetry==$POETRY_VERSION"
COPY pyproject.toml poetry.lock* ./
RUN poetry install --no-root --only main
COPY . .

EXPOSE 5000

CMD ["gunicorn", "-b", "0.0.0.0:5000", "smartgym_flask.app:app"]
