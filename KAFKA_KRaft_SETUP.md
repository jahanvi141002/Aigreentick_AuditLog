# Kafka KRaft Mode Setup (Without Zookeeper)

This guide explains how to set up and run Kafka in KRaft mode (without Zookeeper) on Windows.

---

## What is KRaft Mode?

**KRaft** (Kafka Raft) is a metadata management mode that allows Kafka to run **without Zookeeper**. This simplifies the setup and reduces resource usage.

**Benefits:**
- ✅ No Zookeeper needed
- ✅ Simpler setup (one process instead of two)
- ✅ Faster startup
- ✅ Better performance
- ✅ Production-ready (since Kafka 3.3+)

---

## Prerequisites

- **Kafka 2.8.0+** (KRaft mode is available)
- **Windows PowerShell**
- **Java 17+** (required for Kafka)

---

## Step-by-Step Setup

### Step 1: Verify Kafka Installation

```powershell
cd C:\Kafka\kafka_2.13-4.1.0
.\bin\windows\kafka-server-start.bat --version
```

**Expected Output:**
```
Kafka version: 2.13-4.1.0
```

---

### Step 2: Create KRaft Configuration

Create the KRaft configuration file:

**Path:** `C:\Kafka\kafka_2.13-4.1.0\config\kraft\server.properties`

**Content:**
```properties
############################# Server Basics #############################
# The role of this server. Setting this puts us in KRaft mode
process.roles=broker,controller

# The node id associated with this instance's roles
node.id=1

# List of controller endpoints used to connect to the controller cluster
# Format: nodeId1@host1:port1,nodeId2@host2:port2
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
log.dirs=C:\Kafka\kafka_2.13-4.1.0\kraft-logs

############################# Log Retention Policy #############################
# The minimum age of a log file to be eligible for deletion
log.retention.hours=168

# The maximum size of a log before it is deleted
log.retention.bytes=1073741824

# The maximum time before a log is rolled
log.roll.hours=168

# The maximum size of a log segment file
log.segment.bytes=1073741824
```

**Or use the automated script** (see Step 4 below).

---

### Step 3: Format Storage (First Time Only)

Before starting Kafka for the first time, you need to format the storage with a cluster ID.

**Generate a cluster ID:**
```powershell
# Generate a unique cluster ID (UUID)
[System.Guid]::NewGuid().ToString()
```

**Example output:**
```
a1b2c3d4-e5f6-7890-abcd-ef1234567890
```

**Format the storage:**
```powershell
cd C:\Kafka\kafka_2.13-4.1.0

# Replace <CLUSTER_ID> with your generated UUID
.\bin\windows\kafka-storage.bat format -t <CLUSTER_ID> -c .\config\kraft\server.properties
```

**Example:**
```powershell
.\bin\windows\kafka-storage.bat format -t a1b2c3d4-e5f6-7890-abcd-ef1234567890 -c .\config\kraft\server.properties
```

**Expected Output:**
```
Formatting C:\Kafka\kafka_2.13-4.1.0\kraft-logs with cluster.id a1b2c3d4-e5f6-7890-abcd-ef1234567890
```

**⚠️ Important:** This step only needs to be done **once** (first time setup). After that, Kafka will use the existing formatted storage.

---

### Step 4: Start Kafka (Easy Way - Using Script)

**Use the provided PowerShell script:**

```powershell
cd C:\audit
.\start-kafka-kraft.ps1
```

This script will:
- ✅ Check if KRaft config exists (create if not)
- ✅ Format storage if needed (first time only)
- ✅ Start Kafka in KRaft mode
- ✅ Test the connection

**Keep the window open!** Kafka must be running.

---

### Step 4: Start Kafka (Manual Way)

**Open PowerShell:**

```powershell
cd C:\Kafka\kafka_2.13-4.1.0

# Start Kafka in KRaft mode
.\bin\windows\kafka-server-start.bat .\config\kraft\server.properties
```

**Expected Output:**
```
[INFO] Kafka version: 2.13-4.1.0
[INFO] Kafka commitId: ...
[INFO] Kafka startTimeMs: ...
[INFO] [KafkaServer id=1] started
```

**Keep this window open!** Kafka must be running.

---

### Step 5: Verify Kafka is Running

**Open a new PowerShell window:**

```powershell
cd C:\Kafka\kafka_2.13-4.1.0

# List topics
.\bin\windows\kafka-topics.bat --list --bootstrap-server localhost:9092
```

**Expected Output:**
```
__consumer_offsets
```

If you see topics listed (even just `__consumer_offsets`), Kafka is working! ✅

---

## Quick Start Script

The easiest way is to use the provided script:

```powershell
cd C:\audit
.\start-kafka-kraft.ps1
```

This script automatically:
1. Creates KRaft configuration if needed
2. Formats storage if needed (first time only)
3. Starts Kafka in KRaft mode
4. Tests the connection

---

## Configuration Details

### Ports Used

- **9092** - Kafka Broker (for clients)
- **9093** - Kafka Controller (for metadata management)

### Log Directory

- **Default:** `C:\Kafka\kafka_2.13-4.1.0\kraft-logs`
- **Configurable:** Edit `log.dirs` in `server.properties`

### Cluster ID

- Generated once during storage formatting
- Stored in `kraft-logs/meta.properties`
- Used for cluster identification

---

## Troubleshooting

### Issue: "Storage not formatted"

**Solution:**
```powershell
cd C:\Kafka\kafka_2.13-4.1.0
$clusterId = [System.Guid]::NewGuid().ToString()
.\bin\windows\kafka-storage.bat format -t $clusterId -c .\config\kraft\server.properties
```

### Issue: "Port 9092 or 9093 already in use"

**Solution:**
```powershell
# Find what's using the port
netstat -ano | findstr :9092
netstat -ano | findstr :9093

# Kill the process (replace <PID> with actual process ID)
taskkill /PID <PID> /F
```

### Issue: "No configuration found for 'xxx'"

**Solution:**
- Make sure you're using the KRaft config: `.\config\kraft\server.properties`
- Not the Zookeeper config: `.\config\server.properties`

### Issue: "Timed out waiting for a node assignment"

**Solution:**
- Make sure Kafka is actually running
- Check if port 9092 is accessible
- Wait a few more seconds for Kafka to fully start

### Issue: "Failed to format storage"

**Solution:**
- Make sure the log directory path exists or can be created
- Check file permissions
- Try deleting the `kraft-logs` directory and formatting again

---

## Comparison: KRaft vs Zookeeper Mode

| Feature | KRaft Mode | Zookeeper Mode |
|---------|-----------|----------------|
| **Setup** | One process | Two processes (Zookeeper + Kafka) |
| **Startup** | Faster | Slower |
| **Ports** | 9092 (broker), 9093 (controller) | 9092 (broker), 2181 (Zookeeper) |
| **Storage** | Requires formatting | No formatting needed |
| **Configuration** | `config/kraft/server.properties` | `config/server.properties` |
| **Recommended** | ✅ Yes (Kafka 3.3+) | ⚠️ Legacy mode |

---

## Testing Your Setup

### Test 1: List Topics

```powershell
cd C:\Kafka\kafka_2.13-4.1.0
.\bin\windows\kafka-topics.bat --list --bootstrap-server localhost:9092
```

### Test 2: Create a Topic

```powershell
.\bin\windows\kafka-topics.bat --create ^
  --topic test-topic ^
  --bootstrap-server localhost:9092 ^
  --partitions 1 ^
  --replication-factor 1
```

### Test 3: Send a Message

```powershell
.\bin\windows\kafka-console-producer.bat --bootstrap-server localhost:9092 --topic test-topic
```

Type a message and press Enter, then Ctrl+C to exit.

### Test 4: Consume Messages

```powershell
.\bin\windows\kafka-console-consumer.bat --bootstrap-server localhost:9092 --topic test-topic --from-beginning
```

---

## Using with Your Application

Once Kafka is running in KRaft mode, your Spring Boot application will work exactly the same:

```properties
# application.properties
spring.kafka.bootstrap-servers=localhost:9092
```

**No changes needed!** The application doesn't care if Kafka is running in KRaft or Zookeeper mode - it just connects to port 9092.

---

## Stopping Kafka

**To stop Kafka:**
1. Go to the PowerShell window where Kafka is running
2. Press `Ctrl+C`
3. Wait for Kafka to shut down gracefully

**Or close the window** (less graceful, but works).

---

## Summary

**KRaft Mode Setup:**
1. ✅ Create KRaft config: `config\kraft\server.properties`
2. ✅ Format storage: `kafka-storage.bat format -t <CLUSTER_ID> -c ...`
3. ✅ Start Kafka: `kafka-server-start.bat config\kraft\server.properties`
4. ✅ Test: `kafka-topics.bat --list --bootstrap-server localhost:9092`

**Or use the script:**
```powershell
.\start-kafka-kraft.ps1
```

**No Zookeeper needed!** 🎉

---

## Next Steps

1. ✅ Start Kafka in KRaft mode
2. ✅ Verify it's working
3. ✅ Start your Spring Boot application
4. ✅ Test audit logs using Postman

Your audit log system will work exactly the same as with Zookeeper mode!

