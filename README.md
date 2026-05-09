# Civil - социальная сеть с фильтрацией токсичности

Простая социальная платформа: каждая публикация и каждый комментарий
проходит через классификатор токсичности перед сохранением. Весь стек
поднимается одной командой `docker compose up`.

## Архитектура

```
┌──────────────┐    ┌──────────────────┐    ┌──────────────────┐
│   Браузер    │───▶│ frontend (nginx, │───▶│  backend (Spring │
│  (HTML/JS)   │    │ проксирует /api/)│    │  Boot, Kotlin)   │
└──────────────┘    └──────────────────┘    └────────┬─────────┘
                                                     │
                              ┌──────────────────────┼─────────────────────┐
                              │                      │                     │
                              ▼                      ▼                     ▼
                       ┌────────────┐         ┌────────────┐       ┌──────────────────┐
                       │ PostgreSQL │         │   Redis    │       │ toxicity-service │
                       │ (хранение) │         │ (кэш ленты)│       │ (FastAPI + HF)   │
                       └────────────┘         └────────────┘       └──────────────────┘
```

| Сервис             | Технология                | Порт |
| ------------------ | ------------------------- | ---- |
| `frontend`         | nginx + HTML/JS           | 3000 |
| `backend`          | Spring Boot 3.5 / Kotlin  | 8080 |
| `toxicity-service` | FastAPI + transformers    | 8000 |
| `postgres`         | PostgreSQL 16             | 5432 |
| `redis`            | Redis 7                   | 6379 |

### Детекция токсичности

Модерация работает на одной модели - бинарный классификатор
явной токсичности, обученный на русскоязычных токсичных комментариях.
Ноутбуки обучения лежат в `model/` (`data_preperation.ipynb`,
`eda.ipynb`, `baseline.ipynb`, `experiments.ipynb`); веса лежат в
`toxic_model/` в корне репозитория и монтируются в FastAPI-сервис.
Модель покрывает два языка (русский и английский) и блокирует пост при `prob_toxic >= 0.5`.

## Возможности

- **Авторизация.** Регистрация, вход, JWT-токены (HS256), хэширование
  паролей BCrypt.
- **Посты.** Создание, просмотр, удаление (только своих). Токсичный
  контент отвергается с HTTP 400.
- **Комментарии.** Добавление и просмотр под любым постом. Токсичные
  комментарии отвергаются.
- **Лайки.** Поставить и убрать; в каждом посте возвращаются счётчик
  и флаг "лайкнул ли я".
- **Подписки.** Per-user social graph (follow / unfollow).
- **Лента.** Посты тех, на кого вы подписаны (плюс свои), новые сверху,
  с пагинацией.
- **Профили.** Любой пользователь, его посты, счётчики подписок;
  редактирование своего профиля.

### Как работает лента

Per-user ленты лежат в Redis как sorted sets с ключом `feed:{userId}`,
score - epoch-секунда создания поста, member - id поста. Размер
ограничен (`feed.fanout-cap`, по умолчанию 200).

- **При создании поста** бэкенд раскладывает id нового поста в ZSET
  каждого подписчика автора (и в его собственный).
- **При чтении ленты** бэкенд берёт окно из Redis и подгружает посты
  пакетом из PostgreSQL.
- **Если в Redis ничего нет или Redis недоступен** - срабатывает
  фолбэк, читающий напрямую из PostgreSQL и наполняющий ZSET заново.
- **При подписке/отписке** недавние посты целевого пользователя
  добавляются в ленту подписчика или удаляются оттуда.

## Локальный запуск

```bash
docker compose up --build
```

Первый запуск:

- скачает образы Postgres, Redis, nginx, JDK 21, Python 3.11;
- скачает Gradle и соберёт jar бэкенда;
- через pip установит `torch` (CPU), `transformers` и т.д. для
  toxicity-service;
- Hibernate с `ddl-auto=update` развернёт схему в пустой Postgres.

После запуска:

| URL                                   | Что вы увидите                              |
| ------------------------------------- | ------------------------------------------- |
| <http://localhost:3000/>              | Лента (редирект на login, если не авторизованы) |
| <http://localhost:3000/login.html>    | Вход                                        |
| <http://localhost:3000/register.html> | Регистрация                                 |
| <http://localhost:8080/api/v1/...>    | Бэкенд (доступен также через `/api/` фронтенда) |
| <http://localhost:8000/predict>       | Toxicity-service напрямую                   |

Остановка:

```bash
docker compose down
```

Полная очистка БД между запусками:

```bash
docker compose down -v
```

## Структура проекта

```
.
├── docker-compose.yml
├── frontend/                  # статический сайт + nginx (проксирует /api/ на бэкенд)
│   ├── Dockerfile
│   ├── nginx.conf
│   ├── *.html
│   └── static/
│       ├── styles.css
│       ├── api.js             # обёртка над fetch, auth-хелперы, рендер постов
│       ├── auth.js            # формы входа и регистрации
│       ├── feed.js            # страница ленты
│       ├── profile.js         # страница профиля
│       └── post.js            # отдельный пост + комментарии
├── toxicity/                  # бэкенд на Spring Boot / Kotlin
│   └── toxicity/
│       ├── Dockerfile
│       ├── build.gradle.kts
│       └── src/main/kotlin/com/example/toxicity/
│           ├── ToxicityApplication.kt
│           ├── adapter/         # HTTP-клиент к toxicity-service
│           ├── config/          # CORS, RestTemplate, password encoder, JWT properties
│           ├── controller/      # REST-эндпойнты
│           ├── domain/          # JPA-сущности
│           ├── dto/             # DTO запросов/ответов + валидация
│           ├── repository/      # репозитории Spring Data JPA
│           ├── security/        # JWT-сервис, фильтр аутентификации, CurrentUser
│           ├── service/         # бизнес-логика (Auth, User, Post, Comment, Like, Follow, Feed)
│           └── web/             # @RestControllerAdvice
├── toxicity-service/          # FastAPI-обёртка, монтирует обученные веса
│   ├── Dockerfile
│   ├── requirements.txt
│   └── app.py
├── toxic_model/
└── model/
    ├── data.csv
    ├── train_data.csv
    ├── test_data.csv
    ├── data_preperation.ipynb
    ├── eda.ipynb
    ├── baseline.ipynb
    └── experiments.ipynb
```

## Поверхность API (`/api/v1`)

```
POST   /auth/register                 -> { token, user }
POST   /auth/login                    -> { token, user }
GET    /auth/me                       -> user

GET    /feed?limit=&before=           -> [posts]      (auth)

POST   /posts                         -> post         (auth, проверка токсичности)
GET    /posts/{id}                    -> post
DELETE /posts/{id}                    -> 200          (auth, только владельцу)
GET    /posts/{id}/comments           -> [comments]
POST   /posts/{id}/comments           -> comment      (auth, проверка токсичности)
POST   /posts/{id}/like               -> { likesCount, likedByMe } (auth)
DELETE /posts/{id}/like               -> { likesCount, likedByMe } (auth)

GET    /users/{username}              -> profile
GET    /users/{username}/posts        -> [posts]
PUT    /users/me                      -> user         (auth)
POST   /users/{username}/follow       -> 200          (auth)
DELETE /users/{username}/follow       -> 200          (auth)
```

Все эндпойнты с пометкой `(auth)` требуют заголовок `Authorization:
Bearer <token>`. Ошибки возвращаются в виде
`{ "error": "..." }` с соответствующим HTTP-статусом.

## Конфигурация

Бэкенд читает всё из переменных окружения. Они уже расставлены в
`docker-compose.yml`; чтобы переопределить, экспортируйте их перед
`docker compose up` или поправьте файл.

| Переменная                          | Значение по умолчанию                        | Назначение                          |
| ----------------------------------- | -------------------------------------------- | ----------------------------------- |
| `SPRING_DATASOURCE_URL`             | `jdbc:postgresql://postgres:5432/social`     | JDBC URL                            |
| `SPRING_DATASOURCE_USERNAME`        | `social`                                     | пользователь БД                     |
| `SPRING_DATASOURCE_PASSWORD`        | `social`                                     | пароль БД                           |
| `SPRING_DATA_REDIS_HOST`            | `redis`                                      | хост Redis                          |
| `SPRING_DATA_REDIS_PORT`            | `6379`                                       | порт Redis                          |
| `SERVICE_TOXICITY_DETECTION_URL`    | `http://toxicity-service:8000/predict`       | endpoint классификатора             |
| `JWT_SECRET`                        | `civil-local-dev-jwt-signing-secret-for-localhost` | ключ подписи HS256                  |
| `JWT_EXPIRATION_SECONDS`            | `604800`                                     | время жизни токена (сек, по умолч. 7д) |
| `OBVIOUS_TOXICITY_THRESHOLD`        | `0.5`                                        | порог для активного детектора       |

## Поведение при недоступности toxicity-service

Если бэкенд не может достучаться до `toxicity-service`, запрос на
публикацию отклоняется с HTTP 503 - `"Moderation service is temporarily
unavailable"`. Поведение намеренно консервативное: лучше отказать в
публикации, чем тихо пропустить токсичный контент.
