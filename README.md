# Система управления банковскими картами

## 📋 О проекте

Приложение для управления банковскими картами с возможностью:

- Создания и управления картами
- Просмотра списка карт
- Выполнения переводов между своими картами

## 🔧 Требования к окружению

### Необходимое программное обеспечение

#### Java Development Kit (JDK) 17 или выше

```bash
java -version
```

##### Ожидаемый вывод: openjdk version "17.x.x"

#### Apache Maven 3.8.x или выше

```bash
mvn -version
```

##### Ожидаемый вывод: Apache Maven 3.8.x

#### PostgreSQL 8.0 или выше

```bash
psql --version
```

##### Ожидаемый вывод: psql (PostgreSQL) 8.0.x

#### Git

```bash
git --version
```

##### Ожидаемый вывод: git version 2.x.x

#### Docker

```bash
docker --version
```

##### Ожидаемый вывод: Docker version 20.x.x

#### Docker compose

```bash
docker-compose --version
```

##### Ожидаемый вывод: docker-compose version 1.29.x

## 🗄️ Настройка базы данных

### 1. Подключение к PostgreSQL

```bash
# Подключение к PostgreSQL от имени пользователя postgres
sudo -u postgres psql

# Или если используете локальную установку с паролем
psql -U postgres -h localhost
```

### 2. Создание базы данных

Выполните SQL скрипт для создания базы данных:

```sql
CREATE DATABASE bank_rest_local
    WITH
    OWNER = postgres
    ENCODING = 'UTF8'
    LOCALE_PROVIDER = 'libc'
    CONNECTION LIMIT = -1
    IS_TEMPLATE = False;
```

Для выхода из PostgreSQL выполните:

```sql
\q
```
## :package: Склонируйте репозиторий  
Откройте терминал и перейдите в папку, куда хотите скачать проект.

Введите команду
```bash
git clone https://github.com/vorobevkovrov/bank_rest
```
## ⚙️ Конфигурация приложения

### Настройка переменных окружения

1. **Создайте файл `.env` в корне проекта**
   ```bash
   # Из корня проекта выполните:
   cp  .env
   ```
2. **Или отредактируйте файл `.env.example`** со следующими параметрами:
   ```env
   # Настройки базы данных
   DB_HOST=localhost
   DB_PORT=5432
   DB_NAME=bank_rest_local
   DB_USERNAME=postgres
   DB_PASSWORD=your_password

   # Настройки JWT
   JWT_SECRET=your_jwt_secret_key_here_minimum_32_chars_long
   JWT_EXPIRATION=86400000

   # Ключ шифрования (для AES, должен быть 16, 24 или 32 символа)
   ENCRYPTION_KEY=your_16_24_or_32_char_key
   ```

3. **Проверьте настройки в `src/main/resources/application.yml`**

   Убедитесь, что файл application.yml ссылается на переменные из .env:
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
       username: ${DB_USERNAME}
       password: ${DB_PASSWORD}
   ```

## 🚀 Запуск приложения

#### Способ 1: Запуск через Maven (рекомендуется)

```bash
# Перейдите в корневую директорию проекта
cd /path/to/bank_rest

# Очистка и сборка проекта
mvn clean compile

# Запуск приложения
mvn spring-boot:run
```

#### Способ 2: Запуск через IDE

1. Откройте проект в вашей IDE (IntelliJ IDEA, Eclipse, VS Code)
2. Дождитесь загрузки зависимостей Maven
3. Найдите главный класс приложения: `BankCards.java`
   ```
   src/main/java/com/example/bankcards/BankCards.java
   ```
4. Запустите метод `main()` (зеленая стрелка рядом с классом)
5.

#### Способ 3: Запуск через Docker compose

##### Из корневой директории проекта

```bash
docker-compose up --build
```

#### Остановка контейнера

```bash
docker-compose down
```

### ✅ Признаки успешного запуска

После запуска в консоли должны появиться логи:

```
2026-03-02 17:02:02 [main] INFO  com.example.bankcards.BankCards - Started BankCards in 3.456 seconds (process running for 3.521)
```

### 🔍 Проверка работоспособности

### 1. Проверка Health Check

```bash
curl http://localhost:8080/actuator/health
```

**Ожидаемый ответ:**

```json
{
  "status": "UP"
}
```

### 2. Доступ к Swagger UI

Откройте в браузере: http://localhost:8080/swagger-ui.html

Swagger UI предоставляет:

- 📚 Документацию всех API endpoints
- 🔍 Возможность тестировать запросы прямо в браузере
- 🔐 Информацию о требуемой аутентификации

### 📁 Структура .env.example

```env
# База данных
DB_HOST=localhost
DB_PORT=5432
DB_NAME=bank_rest_local
DB_USERNAME=postgres
DB_PASSWORD=postgres

# JWT
JWT_SECRET=your_32_character_jwt_secret_key_here
JWT_EXPIRATION=86400000

# Шифрование
ENCRYPTION_KEY=your_16_24_or_32_char_key
```
