# SmartGym Simulator

Simulatore eventi MQTT per SmartGym. L'entrypoint (`main.go`) delega a `internal/app` che coordina due worker separati:

- `business-loop`: genera eventi gym/area/machine
- `health-loop`: pubblica lo stato dei device

## Variabili ambiente

- `MQTT_BROKER_HOST` (default: `mosquitto`)
- `MQTT_BROKER_PORT` (default: `1883`)
- `MQTT_BROKER_PROTOCOL` (default: `tcp`)
- `MQTT_USERNAME` (opzionale)
- `MQTT_PASSWORD` (opzionale)
- `MQTT_TOPIC` (default: `smartgym`)
- `SIM_BUSINESS_TICK` (default: `4s`)
- `SIM_HEALTH_TICK` (default: `30s`)
- `SIM_SEED` (opzionale, per run ripetibili)

## Run locale

```bash
go run .
```

## Test

```bash
go test ./...
```
