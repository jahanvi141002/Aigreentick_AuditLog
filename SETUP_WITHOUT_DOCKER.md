# Setting Up Without Docker

This guide explains how to set up and run the audit service without Docker. You'll need to install MongoDB and Kafka locally.

**Note:** This project uses Kafka in KRaft mode (without Zookeeper). Modern Kafka (2.8.0+) supports KRaft mode which eliminates the need for Zookeeper.

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Windows, macOS, or Linux

## Installation Steps

### 1. MongoDB Installation

#### Windows:
1. Download MongoDB Community Server from [https://www.mongodb.com/try/download/community](https://www.mongodb.com/try/download/community)
2. Run the installer and follow the setup wizard
3. MongoDB will be installed as a Windows service by default
4. Verify installation:
   ```powershell
   mongod --version
   ```

#### macOS (using Homebrew):
```bash
brew tap mongodb/brew
brew install mongodb-community
brew services start mongodb-community
```

#### Linux (Ubuntu/Debian):
```bash
# Import MongoDB public GPG key
wget -qO - https://www.mongodb.org/static/pgp/server-7.0.asc | sudo apt-key add -

# Add MongoDB repository
echo "deb [ arch=amd64,arm64 ] https://repo.mongodb.org/apt/ubuntu focal/mongodb-org/7.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-7.0.list

# Update package database
sudo apt-get update

# Install MongoDB
sudo apt-get install -y mongodb-org

# Start MongoDB
sudo systemctl start mongod
sudo systemctl enable mongod
```

#### Verify MongoDB is Running:
```bash
# Connect to MongoDB
mongosh

# Or use the legacy client
mongo
```

In the MongoDB shell:
```javascript
use audit_db
db.version()
```

### 2. Kafka Installation

#### Kafka with KRaft (No Zookeeper Required) ⭐

KRaft mode uses Kafka's internal Raft consensus algorithm instead of Zookeeper. This is simpler and requires fewer components.

**Requirements:** Kafka 2.8.0+ (KRaft is production-ready in 3.3.0+)

**Note:** The configuration below is a minimal setup based on the official Kafka KRaft template. For production, you may want to add additional settings like log retention policies, thread counts, and internal topic replication factors. See the official Kafka documentation or the `config/kraft/server.properties` template included with your Kafka installation for a complete configuration.

##### Windows:
1. Download Kafka 4.1.0 or later from [https://kafka.apache.org/downloads](https://kafka.apache.org/downloads)
   - Binary download: `kafka_2.13-4.1.0.tgz` (or latest version)
   - **Optional:** Verify download using `.asc` (signature) and `.sha512` (checksum) files from the downloads page
2. Extract to a directory (e.g., `C:\Kafka`)
   - After extraction, you'll have `C:\Kafka\kafka_2.13-4.1.0` (adjust paths below if you extracted elsewhere)
3. Create a KRaft configuration file `config\kraft\server.properties`:
   ```properties
   ############################# Server Basics #############################
   # The role of this server. Setting this puts us in KRaft mode
   process.roles=broker,controller
   
   # The node id associated with this instance's roles
   node.id=1
   
   # List of controller endpoints used connect to the controller cluster
   # For single node setup, use controller.quorum.voters
   controller.quorum.voters=1@localhost:9093
   
   ############################# Socket Server Settings #############################
   # The address the socket server listens on
   listeners=PLAINTEXT://:9092,CONTROLLER://:9093
   
   # Name of listener used for communication between brokers
   inter.broker.listener.name=PLAINTEXT
   
   # Listener name, hostname and port the broker will advertise to clients
   advertised.listeners=PLAINTEXT://localhost:9092,CONTROLLER://localhost:9093
   
   # A comma-separated list of the names of the listeners used by the controller
   controller.listener.names=CONTROLLER
   
   # Maps listener names to security protocols
   listener.security.protocol.map=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,SSL:SSL,SASL_PLAINTEXT:SASL_PLAINTEXT,SASL_SSL:SASL_SSL
   
   ############################# Log Basics #############################
   # A comma separated list of directories under which to store log files
   log.dirs=C:/kafka/kraft-combined-logs
   ```
4. Format the storage (first time only):

   **Using PowerShell:**
   ```powershell
   cd C:\Kafka\kafka_2.13-4.1.0
   # Generate a cluster ID
   $clusterId = .\bin\windows\kafka-storage.bat random-uuid
   # Format the storage (use --standalone for single-node setup)
   .\bin\windows\kafka-storage.bat format -t $clusterId -c .\config\kraft\server.properties --standalone
   ```
   
   **Using CMD (Command Prompt) - Method 1 (Recommended):**
   ```cmd
   cd C:\Kafka\kafka_2.13-4.1.0
   REM Generate a cluster ID and format the storage in one command (use --standalone for single-node)
   for /f "tokens=*" %i in ('bin\windows\kafka-storage.bat random-uuid') do bin\windows\kafka-storage.bat format -t %i -c config\kraft\server.properties --standalone
   ```
   
   **Using CMD (Command Prompt) - Method 2 (Manual):**
   ```cmd
   cd C:\Kafka\kafka_2.13-4.1.0
   REM First, generate a UUID by running:
   bin\windows\kafka-storage.bat random-uuid
   REM Note: You may see an error message, but the UUID will be displayed at the end
   REM Copy the UUID that is displayed (look for a long string like: ok9Or7YERaWxcCSgXlUQrA)
   REM Then run (use --standalone for single-node setup):
   bin\windows\kafka-storage.bat format -t ok9Or7YERaWxcCSgXlUQrA -c config\kraft\server.properties --standalone
   ```
   Replace the UUID in the format command with the UUID from the previous command (or generate one at https://www.uuidgenerator.net/)
   
   **Note:** If you see an error message like "No configuration found" when running `random-uuid`, you can safely ignore it. The UUID will still be generated and displayed at the end of the output.

5. Start Kafka:
   **Using PowerShell:**
   ```powershell
   cd C:\Kafka\kafka_2.13-4.1.0
   .\bin\windows\kafka-server-start.bat .\config\kraft\server.properties
   ```
   
   **Using CMD (Command Prompt):**
   ```cmd
   cd C:\Kafka\kafka_2.13-4.1.0
   bin\windows\kafka-server-start.bat config\kraft\server.properties
   ```

##### macOS (using Homebrew):
```bash
# Install Kafka (latest version supports KRaft)
brew install kafka

# Create KRaft server.properties
cd /opt/homebrew/var/lib/kafka
cat > server.properties << EOF
############################# Server Basics #############################
# The role of this server. Setting this puts us in KRaft mode
process.roles=broker,controller

# The node id associated with this instance's roles
node.id=1

# List of controller endpoints used connect to the controller cluster
controller.quorum.bootstrap.servers=localhost:9093

############################# Socket Server Settings #############################
# The address the socket server listens on
listeners=PLAINTEXT://:9092,CONTROLLER://:9093

# Name of listener used for communication between brokers
inter.broker.listener.name=PLAINTEXT

# Listener name, hostname and port the broker will advertise to clients
advertised.listeners=PLAINTEXT://localhost:9092,CONTROLLER://localhost:9093

# A comma-separated list of the names of the listeners used by the controller
controller.listener.names=CONTROLLER

# Maps listener names to security protocols
listener.security.protocol.map=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,SSL:SSL,SASL_PLAINTEXT:SASL_PLAINTEXT,SASL_SSL:SASL_SSL

############################# Log Basics #############################
# A comma separated list of directories under which to store log files
log.dirs=/opt/homebrew/var/lib/kafka/kraft-combined-logs
EOF

# Format storage
kafka-storage format -t $(kafka-storage random-uuid) -c server.properties

# Start Kafka
kafka-server-start server.properties
```

##### Linux:
```bash
# Download Kafka 4.1.0 or later
# Visit https://kafka.apache.org/downloads for the latest version
wget https://archive.apache.org/dist/kafka/4.1.0/kafka_2.13-4.1.0.tgz

# Optional: Verify download (download .asc and .sha512 files and verify)
# wget https://archive.apache.org/dist/kafka/4.1.0/kafka_2.13-4.1.0.tgz.asc
# wget https://archive.apache.org/dist/kafka/4.1.0/kafka_2.13-4.1.0.tgz.sha512
# sha512sum -c kafka_2.13-4.1.0.tgz.sha512

# Extract
tar -xzf kafka_2.13-4.1.0.tgz
cd kafka_2.13-4.1.0

# Create KRaft server.properties
cat > config/kraft/server.properties << EOF
############################# Server Basics #############################
# The role of this server. Setting this puts us in KRaft mode
process.roles=broker,controller

# The node id associated with this instance's roles
node.id=1

# List of controller endpoints used connect to the controller cluster
controller.quorum.bootstrap.servers=localhost:9093

############################# Socket Server Settings #############################
# The address the socket server listens on
listeners=PLAINTEXT://:9092,CONTROLLER://:9093

# Name of listener used for communication between brokers
inter.broker.listener.name=PLAINTEXT

# Listener name, hostname and port the broker will advertise to clients
advertised.listeners=PLAINTEXT://localhost:9092,CONTROLLER://localhost:9093

# A comma-separated list of the names of the listeners used by the controller
controller.listener.names=CONTROLLER

# Maps listener names to security protocols
listener.security.protocol.map=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,SSL:SSL,SASL_PLAINTEXT:SASL_PLAINTEXT,SASL_SSL:SASL_SSL

############################# Log Basics #############################
# A comma separated list of directories under which to store log files
log.dirs=/tmp/kraft-combined-logs
EOF

# Format storage (first time only)
bin/kafka-storage.sh format -t $(bin/kafka-storage.sh random-uuid) -c config/kraft/server.properties

# Start Kafka
bin/kafka-server-start.sh config/kraft/server.properties
```

#### Verify Kafka is Running:
```bash
# List topics
bin/kafka-topics.sh --list --bootstrap-server localhost:9092

# Or on Windows
bin\windows\kafka-topics.bat --list --bootstrap-server localhost:9092
```

## Starting Services

### Start Order:
1. **MongoDB** (must be running first)
2. **Kafka** (KRaft mode - no Zookeeper needed!)

### Quick Start Commands:

#### Windows (PowerShell):
```powershell
# Start MongoDB (if installed as service, it should already be running)
# Check status: Get-Service MongoDB

# Start Kafka in KRaft mode (no Zookeeper needed!)
cd C:\kafka
.\bin\windows\kafka-server-start.bat .\config\kraft\server.properties
```

#### macOS/Linux:
```bash
# Start MongoDB (if using Homebrew)
brew services start mongodb-community

# Or manually
mongod --dbpath /path/to/data

# Start Kafka in KRaft mode (no Zookeeper needed!)
cd /path/to/kafka
bin/kafka-server-start.sh config/kraft/server.properties
```


## Running the Application

Once all services are running:

```bash
# Build the application
mvn clean install

# Run the application
mvn spring-boot:run
```

The application will connect to:
- MongoDB: `localhost:27017`
- Kafka: `localhost:9092`

## Verification

### Check MongoDB:
```bash
mongosh
use audit_db
show collections
```

### Check Kafka:
```bash
# List topics
kafka-topics.sh --list --bootstrap-server localhost:9092

# Consume messages from audit-logs topic
kafka-console-consumer.sh --topic audit-logs --from-beginning --bootstrap-server localhost:9092
```

### Test the Application:
```bash
# Create a test user (generates audit log)
curl -X POST http://localhost:8081/api/demo/users \
  -H "Content-Type: application/json" \
  -H "X-Username: test.user" \
  -d '{"username":"testuser","email":"test@example.com","fullName":"Test User","role":"USER"}'

# Query audit logs
curl http://localhost:8081/api/audit-logs
```

## Troubleshooting

### MongoDB Connection Issues:
- Verify MongoDB is running: `mongosh` or check service status
- Check firewall settings for port 27017
- Verify `application.properties` has: `spring.data.mongodb.uri=mongodb://localhost:27017/audit_db`

### Kafka Connection Issues:
- Check Kafka logs for errors
- Verify `application.properties` has: `spring.kafka.bootstrap-servers=localhost:9092`
- Test Kafka connectivity: `kafka-topics.sh --list --bootstrap-server localhost:9092`
- Ensure Kafka storage is formatted (first time setup)

## Service Management Scripts (Optional)

You can create scripts to start/stop all services:

### Windows (start-services.ps1):
```powershell
# Start MongoDB service
Start-Service MongoDB

# Start Kafka in KRaft mode (no Zookeeper needed!)
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd C:\kafka; .\bin\windows\kafka-server-start.bat .\config\kraft\server.properties"
```

### Linux/Mac (start-services.sh):
```bash
#!/bin/bash

# Start MongoDB
mongod --dbpath /path/to/data &

# Wait for MongoDB
sleep 3

# Start Kafka in KRaft mode (no Zookeeper needed!)
cd /path/to/kafka
bin/kafka-server-start.sh config/kraft/server.properties &
```


## Alternative: Using Package Managers

### Windows (using Chocolatey):
```powershell
choco install mongodb
choco install apache-kafka
# No Zookeeper needed!
```

### macOS (using Homebrew):
```bash
brew install mongodb-community
brew install kafka
# No Zookeeper needed!

# Start services
brew services start mongodb-community
# Start Kafka manually in KRaft mode (see KRaft setup above)
```


### Linux (using apt/yum):
```bash
# MongoDB (see official MongoDB installation guide)
# Kafka typically needs manual installation from Apache
```

## Notes

- The application configuration in `application.properties` already points to `localhost`, so no changes are needed
- This setup uses KRaft mode, which only requires MongoDB and Kafka - simpler setup!
- Make sure ports 27017 (MongoDB), 9092 (Kafka broker), and 9093 (Kafka controller) are available
- On Windows, you may need to run terminals as Administrator
- Kafka runs standalone without Zookeeper (production-ready since Kafka 3.3.0)
- Consider setting up these services to start automatically on system boot

## Why KRaft Mode?

- ✅ **Simpler:** One less service to manage (no Zookeeper)
- ✅ **Faster:** Better performance and lower latency
- ✅ **Scalable:** Better scalability for large clusters
- ✅ **Modern:** The future of Kafka (Zookeeper mode is being deprecated)
- ✅ **Production-Ready:** Since Kafka 3.3.0 (March 2023)

