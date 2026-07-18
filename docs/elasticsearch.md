# Elasticsearch ve Kibana

Bu stack yalnýzca yerel geliþtirme için tasarlanmýþtýr ve parola korumalýdýr. Önce `.env.example` dosyasýný `.env` olarak kopyalayýn, ardýndan `ELASTIC_PASSWORD` ve `KIBANA_PASSWORD` deðerlerini farklý, güçlü parolalarla deðiþtirin.

Baþlatma:

```bash
docker compose -f docker-compose.yml -f infra/observability/docker-compose.yml --profile observability up -d
```

- Elasticsearch API: `http://localhost:9200`
- Kibana: `http://localhost:5601`
- Kibana kullanýcý adý: `elastic`
- Kibana parolasý: `.env` içindeki `ELASTIC_PASSWORD`

Elasticsearch ve Kibana sürümleri ayný tutulur (`9.4.2`). Portlar yalnýzca localhost üzerinde dinler. Kibana'da indeksler oluþturulduktan sonra **Discover** ile verileri inceleyebilir, **Dashboard** ile iþlem hacmi, coin daðýlýmý ve hata trendi görselleþtirmeleri ekleyebilirsiniz.
## Demo data and Kibana dashboard

Set the following values in `.env` before starting the backend:

```properties
ELASTICSEARCH_ENABLED=true
APP_DEMO_DATA_ENABLED=true
APP_DEMO_DATA_PASSWORD=DemoCrypto2026!
```

On its first start, the backend creates these persistent PostgreSQL users and trades:

| User | Email | Initial demo holding |
| --- | --- | --- |
| demo.btc | demo.btc@cryptopal.local | BTC 0.05000000 |
| demo.eth | demo.eth@cryptopal.local | ETH 1.25000000 |
| demo.sol | demo.sol@cryptopal.local | SOL 10.00000000 |

The backend writes every completed trade to the `cryptopal-trades` Elasticsearch index. At startup it also backfills previously stored trade records, so restarting the backend after Elasticsearch becomes available is enough to populate Kibana.

Open Kibana at `http://localhost:5601`, sign in with `elastic` and the `ELASTIC_PASSWORD` value from `.env`, then:

1. Open **Discover**.
2. Create a data view named `cryptopal-trades`.
3. Select `createdAt` as the time field.
4. Use `symbol`, `type`, `username` and `totalAmount` for filters and dashboard charts.

For Render deployment, do not expose local Kibana. Use a managed Elasticsearch service such as Elastic Cloud and configure `ELASTICSEARCH_ENABLED`, `ELASTICSEARCH_URL`, `ELASTICSEARCH_USERNAME` and `ELASTIC_PASSWORD` as Render environment variables.