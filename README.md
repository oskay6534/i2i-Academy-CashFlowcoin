# CryptoPal

CryptoPal, i2i Academy kapsamında geliştirilmiş full-stack bir kripto para al-sat simülasyonudur. Kullanıcılar kayıt olabilir, sanal bakiye alabilir, kripto fiyatlarını takip edebilir, alım-satım yapabilir, portföyünü inceleyebilir ve yapay zekâ destekli piyasa yorumu isteyebilir.

> Bu proje bir simülasyondur. Gerçek para veya gerçek varlık işlemi gerçekleştirmez; finansal tavsiye vermez.

## Canlı bağlantılar

- Frontend: https://cashflowcoin-web.vercel.app/
- Backend sağlık kontrolü: https://cashflowcoin-api-production.up.railway.app/actuator/health
- Yerel Swagger UI: http://localhost:8080/swagger-ui.html

## Özellikler

- Redis tabanlı oturum yönetimi ile kayıt, giriş ve çıkış
- Yeni kullanıcıya 50.000 TL sanal başlangıç bakiyesi
- BTC, ETH, SOL, BNB, XRP, ADA, DOGE, AVAX, DOT ve LINK desteği
- Her 15 saniyede fiyat dalgalanması oluşturan Ticker Engine
- Güncel fiyatların Redis üzerinde düşük gecikmeli saklanması
- Alım-satım, cüzdan, portföy ve işlem geçmişi akışları
- PostgreSQL üzerinde kalıcı kullanıcı, bakiye, portföy, işlem ve fiyat geçmişi
- Gemini ile portföy ve piyasa analizine yönelik AI sohbet ekranı
- Opsiyonel Elasticsearch indeksleme ve Kibana dashboardları
- JUnit, entegrasyon ve Selenium E2E testleri
- Jenkins CI pipeline ile backend testleri ve frontend build doğrulaması

## Mimari

~~~text
React + Vite SPA
       |
       v
Spring Boot modular monolith
  |          |           |
  v          v           v
PostgreSQL  Redis   Gemini / Elasticsearch (opsiyonel)
~~~

| Katman | Görev |
| --- | --- |
| React, TypeScript, Vite | SPA arayüzü, fiyat polling, al-sat modalları ve AI chat |
| Java 17, Spring Boot | REST API, kimlik doğrulama, iş kuralları ve işlem yönetimi |
| PostgreSQL | Kullanıcı, cüzdan, portföy, işlem ve fiyat geçmişi |
| Redis | Oturum tokenları ve güncel volatil fiyatlar |
| Flyway | Sürümlü PostgreSQL schema migration |
| Elasticsearch ve Kibana | Opsiyonel arama ve analitik görselleştirme |
| Jenkins | Otomatik test, build ve artifact arşivleme |

## Klasör yapısı

~~~text
backend/                              Spring Boot API
  src/main/java/                      Modüler backend paketleri
  src/main/resources/db/migration/    Flyway migration dosyaları
frontend/                             React + Vite SPA
tests/e2e/                            Selenium Maven test modülü
infra/observability/                  Elasticsearch ve Kibana altyapısı
docs/                                 Teknik dokümantasyonlar
docker-compose.yml                    Yerel PostgreSQL ve Redis
Jenkinsfile                           CI pipeline
~~~

## Backend modülleri

~~~text
auth/         kayıt, giriş, çıkış ve session endpointleri
user/         kullanıcı entity ve repository yapısı
wallet/       nakit bakiye yönetimi
portfolio/    sahip olunan kripto varlıklar
trade/        alış, satış ve işlem geçmişi
market/       Ticker Engine, güncel fiyatlar ve fiyat geçmişi
ai/           Gemini istemcisi ve bağlamsal prompt üretimi
analytics/    Elasticsearch indexer ve backfill işleri
common/       CORS, exception handling ve paylaşılan yapılandırma
~~~

## Gereksinimler

- Java 17+
- Node.js 20+
- Docker Desktop
- Google Gemini API key (AI sohbet özelliği kullanılacaksa)

Maven Wrapper projeye dahildir; ayrıca Maven kurmak gerekmez.

## Yerelde çalıştırma

### 1. Ortam değişkenleri

Kök dizinde örnek dosyadan .env oluşturun:

~~~powershell
Copy-Item .env.example .env
~~~

Örnek yerel değerler:

~~~properties
POSTGRES_HOST=localhost
POSTGRES_PORT=55432
POSTGRES_DB=cryptopal
POSTGRES_USER=cryptopal
POSTGRES_PASSWORD=cryptopal123
REDIS_HOST=localhost
REDIS_PORT=6379
GEMINI_API_KEY=your_key_here
ELASTICSEARCH_ENABLED=false
~~~



### 2. PostgreSQL ve Redis'i başlatma

~~~powershell
docker compose up -d
~~~

| Servis | Yerel adres |
| --- | --- |
| PostgreSQL | localhost:55432 |
| Redis | localhost:6379 |

### 3. Backend'i başlatma

~~~powershell
cd backend
.\mvnw.cmd spring-boot:run
~~~

Backend: http://localhost:8080

### 4. Frontend'i başlatma

Başka bir terminalde:

~~~powershell
cd frontend
npm ci
npm run dev
~~~

Frontend: http://localhost:5173

## Veritabanı ve Flyway

Şemanın kaynağı:

~~~text
backend/src/main/resources/db/migration/V1__initial_schema.sql
~~~

Flyway yeni bir PostgreSQL veritabanında migration'ı otomatik uygular. Hibernate şemayı validate eder. Kalıcı finansal veriler PostgreSQL'de tutulur; Redis yalnızca session ve en güncel fiyat cache'i içindir.

Yerel Docker verilerini sıfırlamak için:

~~~powershell
docker compose down -v
docker compose up -d
~~~

> Bu komut yerel PostgreSQL ve Redis verilerini siler.

## API dokümantasyonu

Backend çalışırken:

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Health: http://localhost:8080/actuator/health

| Alan | Base path | Açıklama |
| --- | --- | --- |
| Auth | /api/auth | Register, login, session ve logout |
| Market | /api/market | Güncel Redis fiyatları ve fiyat geçmişi |
| Trade | /api/trades | Buy, sell ve işlem geçmişi |
| Portfolio | /api/portfolio | Cüzdan, varlıklar ve portföy özeti |
| AI | /api/ai | Gemini destekli analiz |

## Fiyat akışı

Ödev dokümanında izin verilen iki dış veri sağlayıcı yaklaşımından Ticker Engine uygulanmıştır; 

1. Background worker her 15 saniyede Redis'teki son fiyatları okur.
2. MarketDataProvider gerçekçi simüle edilmiş fiyat değişimleri üretir.
3. Yeni değerler Redis'teki ilgili fiyat anahtarlarının üzerine yazılır.
4. Frontend market endpointini her 15 saniyede poll eder; ayrıca manuel yenileme sunar.
5. Fiyat snapshotları PostgreSQL'e periyodik kaydedilir ve geçmiş analizlerde kullanılır.

## Testler

### Backend

~~~powershell
cd backend
.\mvnw.cmd test
~~~

### Frontend production build

~~~powershell
cd frontend
npm ci
npm run build
~~~

### Selenium E2E

Önce PostgreSQL, Redis, backend ve frontend çalışır durumda olmalıdır.

~~~powershell
docker run -d --rm --name cryptopal-selenium --add-host=host.docker.internal:host-gateway -p 4444:4444 selenium/standalone-chrome:latest
$env:APP_URL = 'http://host.docker.internal:5173'
$env:SELENIUM_GRID_URL = 'http://localhost:4444/wd/hub'
mvn -f tests/e2e/pom.xml test
~~~

Selenium testleri giriş sayfası, kayıt formu geçişi ve parola görünürlüğü akışlarını kapsar.

## Jenkins CI/CD

Jenkinsfile aşağıdaki aşamaları içerir:

1. Docker Compose söz dizimini doğrulama
2. İsteğe bağlı PostgreSQL ve Redis başlatma
3. Backend testleri
4. Frontend production build
5. Selenium E2E testleri
6. Surefire raporları ve frontend dist artifact arşivleme

| Parametre | Açıklama |
| --- | --- |
| START_DEPENDENCIES | İzole Jenkins agent üzerinde PostgreSQL ve Redis başlatır |
| RUN_SELENIUM | Selenium E2E aşamasını etkinleştirir |
| APP_URL | Selenium'ın açacağı frontend adresi |
| SELENIUM_GRID_URL | Selenium Grid Hub adresi |

## Elasticsearch ve Kibana

Elasticsearch opsiyoneldir; ana al-sat akışını engellemez. Etkinleştirildiğinde kullanıcı, işlem ve fiyat verileri indekslenebilir. Kibana üzerinde işlem dağılımı, toplam işlem tutarları, kullanıcı verileri ve piyasa verileri görselleştirilebilir.

Ayrıntılı kurulum için [ELASTICSEARCH.md](ELASTICSEARCH.md) dosyasına bakın.

## Deployment

| Katman | Platform |
| --- | --- |
| Frontend | Vercel |
| Spring Boot API | Railway |
| PostgreSQL | Railway PostgreSQL |
| Redis / Valkey | Railway |

Vercel frontend environment variable:

~~~text
VITE_API_BASE_URL=https://cashflowcoin-api-production.up.railway.app
~~~

Backend'de DATABASE_URL, database credentials, REDIS_HOST, REDIS_PORT, REDIS_PASSWORD, PORT, JAVA_OPTS ve opsiyonel GEMINI_API_KEY environment variable olarak tanımlanmalıdır.

## Sorun giderme

| Problem | Kontrol edilmesi gereken |
| --- | --- |
| PostgreSQL bağlantısı yok | Docker çalışıyor mu, 55432 portu boş mu? |
| Redis health DOWN | Redis container ve parola ayarları eşleşiyor mu? |
| AI chat fallback dönüyor | GEMINI_API_KEY yerel .env veya Railway Variables'a ekli mi? |
| Selenium bağlanamıyor | Backend, frontend ve Selenium Grid açık mı? |
| Frontend API çağrılamıyor | VITE_API_BASE_URL ve backend CORS ayarları doğru mu? |

## Ekip ve katkılar

Bu proje üç kişilik ekip çalışmasıyla geliştirilmiştir.

- Mustafa Oskay: frontend, test otomasyonu, Elasticsearch/Kibana, AI arayüzü ve deployment entegrasyonu
- Ali Koçer: Spring Boot backend, REST API, veri modeli, Flyway ve al-sat iş kuralları
- Berke Odabaş: Docker, Jenkins CI/CD pipeline ve DevOps yapılandırması

Tüm ekip üyelerine katkılarından dolayı teşekkür ederiz.
