# Fleet Navigator - Database Setup

## 📊 Datenbank-Optionen

Fleet Navigator unterstützt mehrere Datenbank-Systeme. Wähle die passende Option für deine Anforderungen.

---

## Option 1: H2 File-Based (Standard) ⭐

**Empfohlen für:** Development, Testing, Single-User

### Vorteile
✅ Keine separate Installation nötig
✅ Embedded in der Anwendung
✅ Daten bleiben persistent gespeichert
✅ Schnell und einfach

### Nachteile
❌ Nicht für Multi-User geeignet
❌ Weniger Performance als PostgreSQL/MySQL

### Konfiguration

**Ist bereits aktiv!** Keine Änderungen nötig.

**Datei:** `src/main/resources/application.properties`
```properties
spring.datasource.url=jdbc:h2:file:./data/fleetnavdb
```

### Datenbank-Speicherort
```
Fleet-Navigator/
└── data/
    ├── fleetnavdb.mv.db      # Datenbank-Datei
    └── fleetnavdb.trace.db   # Log-Datei
```

### H2 Console (Web-UI)
```
URL: http://localhost:8080/h2-console

JDBC URL: jdbc:h2:file:./data/fleetnavdb
Username: sa
Password: (leer)
```

---

## Option 2: PostgreSQL (Production) 🚀

**Empfohlen für:** Production, Multi-User, High Performance

### Installation PostgreSQL

**Ubuntu/Debian:**
```bash
sudo apt update
sudo apt install postgresql postgresql-contrib
```

**Start Service:**
```bash
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

### Datenbank erstellen

```bash
# Als postgres User
sudo -u postgres psql

# In psql Shell
CREATE DATABASE fleetnavdb;
CREATE USER fleetuser WITH PASSWORD 'fleetpassword';
GRANT ALL PRIVILEGES ON DATABASE fleetnavdb TO fleetuser;
\q
```

### Fleet Navigator konfigurieren

**Option A: Profile verwenden**
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

**Option B: application.properties ändern**
```properties
# Ersetze H2 mit:
spring.datasource.url=jdbc:postgresql://localhost:5432/fleetnavdb
spring.datasource.driverClassName=org.postgresql.Driver
spring.datasource.username=fleetuser
spring.datasource.password=fleetpassword
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
```

### Verbindung testen
```bash
psql -h localhost -U fleetuser -d fleetnavdb
```

---

## Option 3: MySQL (Alternative) 🐬

**Empfohlen für:** Wenn PostgreSQL nicht verfügbar

### Installation MySQL

```bash
sudo apt update
sudo apt install mysql-server
```

### Datenbank erstellen

```bash
sudo mysql

# In MySQL Shell
CREATE DATABASE fleetnavdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'fleetuser'@'localhost' IDENTIFIED BY 'fleetpassword';
GRANT ALL PRIVILEGES ON fleetnavdb.* TO 'fleetuser'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

### application.properties ändern

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/fleetnavdb?useSSL=false&serverTimezone=Europe/Berlin
spring.datasource.driverClassName=com.mysql.cj.jdbc.Driver
spring.datasource.username=fleetuser
spring.datasource.password=fleetpassword
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect
```

### pom.xml ändern

```xml
<!-- MySQL Dependency hinzufügen -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

---

## 🔄 Datenbank-Migration

### Von H2 zu PostgreSQL migrieren

**1. Daten exportieren (H2 Console)**
```sql
SCRIPT TO 'data/export.sql';
```

**2. PostgreSQL anpassen und importieren**
- H2-spezifische Syntax anpassen
- In PostgreSQL importieren: `psql -U fleetuser -d fleetnavdb < export.sql`

**3. Spring Boot neu starten mit PostgreSQL-Profil**

---

## 🗂️ Datenbank-Schema

### Tabellen

**chat**
- id (BIGINT, PK)
- title (VARCHAR)
- model (VARCHAR)
- created_at (TIMESTAMP)
- updated_at (TIMESTAMP)

**message**
- id (BIGINT, PK)
- chat_id (BIGINT, FK)
- role (VARCHAR) - USER/ASSISTANT/SYSTEM
- content (TEXT)
- tokens (INTEGER)
- created_at (TIMESTAMP)

**context_item**
- id (BIGINT, PK)
- chat_id (BIGINT, FK)
- filename (VARCHAR)
- content (TEXT)
- tokens (INTEGER)
- created_at (TIMESTAMP)

**global_stats**
- id (BIGINT, PK)
- total_tokens (BIGINT)
- total_messages (INTEGER)
- updated_at (TIMESTAMP)

---

## 🛠️ Troubleshooting

### H2: "Database may be already in use"

**Problem:** Zwei Instanzen versuchen auf dieselbe H2-Datei zuzugreifen

**Lösung:**
```bash
# Stoppe alle laufenden Instanzen
pkill -f "fleet-navigator"

# Oder lösche Lock-Datei
rm data/fleetnavdb.lock.db
```

### PostgreSQL: "Connection refused"

**Problem:** PostgreSQL läuft nicht

**Lösung:**
```bash
sudo systemctl start postgresql
sudo systemctl status postgresql
```

### PostgreSQL: "Authentication failed"

**Problem:** Falsche Credentials

**Lösung:**
```bash
# Password zurücksetzen
sudo -u postgres psql
ALTER USER fleetuser WITH PASSWORD 'neues_passwort';
```

---

## 📊 Performance-Vergleich

| Feature | H2 File | PostgreSQL | MySQL |
|---------|---------|------------|-------|
| Setup | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ |
| Performance | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| Concurrent Users | ⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| Production Ready | ⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| Backup Tools | ⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |

---

## 🎯 Empfehlungen

**Development:**
- ✅ H2 File-Based (Standard)

**Single-User Production:**
- ✅ H2 File-Based (ausreichend)

**Multi-User Production:**
- ✅ PostgreSQL (beste Wahl)
- ✅ MySQL (Alternative)

**Enterprise:**
- ✅ PostgreSQL mit Replication
- ✅ Regelmäßige Backups
- ✅ Connection Pooling

---

## 🔒 Backup-Strategie

### H2 Backup
```bash
# Einfach kopieren
cp -r data/ backup_$(date +%Y%m%d)/
```

### PostgreSQL Backup
```bash
# Dump erstellen
pg_dump -U fleetuser fleetnavdb > backup_$(date +%Y%m%d).sql

# Restore
psql -U fleetuser fleetnavdb < backup_20251031.sql
```

---

**Standard-Konfiguration:** H2 File-Based (persistent)
**Upgrade-Pfad:** PostgreSQL bei Bedarf

🚢 **Deine Daten bleiben jetzt erhalten!**
