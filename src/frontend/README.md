# smartgym-frontend

Frontend Flask del progetto SmartGym Monitor.

## Struttura

```text
smartgym-flask/
├── smartgym_flask/
│   ├── __init__.py
│   ├── app.py
│   ├── config.py
│   ├── extensions.py
│   ├── routes/
│   │   ├── __init__.py
│   │   ├── auth.py
│   │   ├── dashboard.py
│   │   └── api.py
│   ├── services/
│   │   ├── __init__.py
│   │   └── user_service.py
│   ├── templates/
│   │   ├── base.html
│   │   └── ...
│   └── static/
│       ├── css/
│       ├── js/
│       └── img/
├── tests/
│   ├── __init__.py
│   ├── test_auth.py
│   └── test_api.py
├── pyproject.toml
├── poetry.toml
├── poetry.lock
└── README.md
```

## Avvio locale

```zsh
poetry install
poetry run pytest -q
poetry run gunicorn -b 0.0.0.0:5000 smartgym_flask.app:app
```

## Variabili ambiente

- `FLASK_SECRET_KEY` (default: `dev`)
- `AUTH_SERVICE_BASE_URL` (default: `http://localhost:8081`)
- `AUTH_TIMEOUT_SECONDS` (default: `5`)
