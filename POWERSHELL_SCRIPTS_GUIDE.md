# PowerShell Scripts Guide

Quick reference for all PowerShell scripts in this project.

---

## 🚀 Quick Start (Easiest Way)

**Just run this one command:**
```powershell
.\quick-start.ps1
```

This will:
1. ✅ Start MongoDB
2. ✅ Start Kafka
3. ✅ Start Spring Boot Application
4. ✅ Verify all services
5. ✅ Optionally run tests

---

## 📋 All Available Scripts

### 1. **quick-start.ps1** ⭐ RECOMMENDED
**Purpose:** One-command solution to start everything and run tests

**Usage:**
```powershell
.\quick-start.ps1
```

**What it does:**
- Starts MongoDB (if not running)
- Starts Kafka (if not running)
- Starts Spring Boot application (if not running)
- Verifies all services are ready
- Optionally runs test suite

**Best for:** First-time setup or when you want everything automated

---

### 2. **check-services.ps1**
**Purpose:** Quick status check of all services

**Usage:**
```powershell
.\check-services.ps1
```

**What it does:**
- Checks if MongoDB is running (port 27017)
- Checks if Kafka is running (port 9092)
- Checks if Spring Boot is running (port 8081)
- Tests API endpoint
- Shows summary with recommendations

**Best for:** Quick health check before running tests

---

### 3. **start-all-services.ps1**
**Purpose:** Start all services in separate windows

**Usage:**
```powershell
.\start-all-services.ps1
```

**What it does:**
- Starts MongoDB service
- Launches Kafka in new PowerShell window
- Launches Spring Boot in new PowerShell window
- Waits and verifies each service

**Best for:** When you want to see all service logs in separate windows

---

### 4. **setup-and-test.ps1**
**Purpose:** Comprehensive setup with detailed checks

**Usage:**
```powershell
.\setup-and-test.ps1
```

**What it does:**
- Checks MongoDB (6 steps)
- Checks Kafka (with retries)
- Checks Spring Boot (with retries)
- Verifies MongoDB connection
- Verifies Kafka topics
- Tests API endpoint
- Optionally runs test suite

**Best for:** Detailed setup with full verification

---

### 5. **test-everything.ps1**
**Purpose:** Run complete test suite

**Usage:**
```powershell
.\test-everything.ps1
```

**What it does:**
- Creates user with headers (userId, organizationId)
- Verifies audit log creation
- Triggers exception
- Verifies exception log creation
- Tests all query endpoints
- Shows summary

**Best for:** Testing all functionality after services are running

**Note:** Make sure all services are running first!

---

### 6. **start-kafka-kraft.ps1**
**Purpose:** Start Kafka in KRaft mode (no Zookeeper)

**Usage:**
```powershell
.\start-kafka-kraft.ps1
```

**What it does:**
- Formats Kafka storage (first time only)
- Starts Kafka broker on port 9092
- Starts Kafka controller on port 9093
- Verifies Kafka is running

**Best for:** Starting Kafka manually

---

## 🎯 Common Workflows

### Workflow 1: First Time Setup
```powershell
# Step 1: Start everything
.\quick-start.ps1

# Step 2: Wait for all services to start (watch the windows)

# Step 3: Run tests
.\test-everything.ps1
```

### Workflow 2: Daily Development
```powershell
# Quick check
.\check-services.ps1

# If services not running, start them
.\start-all-services.ps1

# Wait for services, then test
.\test-everything.ps1
```

### Workflow 3: Troubleshooting
```powershell
# Check what's running
.\check-services.ps1

# Start missing services
.\start-all-services.ps1

# Detailed setup with verification
.\setup-and-test.ps1
```

---

## ⚠️ Troubleshooting

### Issue: "Unable to connect to the remote server"
**Cause:** Spring Boot application is not running

**Solution:**
```powershell
# Option 1: Use quick-start
.\quick-start.ps1

# Option 2: Start manually
mvn spring-boot:run
```

### Issue: "Kafka did not start"
**Cause:** Kafka takes 20-30 seconds to start, especially on first run

**Solution:**
```powershell
# Wait longer and check
.\check-services.ps1

# Or start manually and watch the window
.\start-kafka-kraft.ps1
```

### Issue: "MongoDB connection failed"
**Cause:** MongoDB service is not running

**Solution:**
```powershell
Start-Service MongoDB
```

### Issue: Services start but tests fail
**Cause:** Services may still be initializing

**Solution:**
```powershell
# Wait a bit longer, then check
Start-Sleep -Seconds 10
.\check-services.ps1

# If all green, run tests
.\test-everything.ps1
```

---

## 📊 Service Ports

| Service | Port | Check Command |
|---------|------|---------------|
| MongoDB | 27017 | `Test-NetConnection localhost -Port 27017` |
| Kafka | 9092 | `Test-NetConnection localhost -Port 9092` |
| Spring Boot | 8081 | `Test-NetConnection localhost -Port 8081` |

---

## 🔍 Manual Verification

### Check MongoDB
```powershell
mongosh --eval "db.version()"
```

### Check Kafka
```powershell
cd C:\Kafka\kafka_2.13-4.1.0
.\bin\windows\kafka-topics.bat --list --bootstrap-server localhost:9092
```

### Check Spring Boot API
```powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/audit-logs"
```

---

## 💡 Tips

1. **First Run:** Services take longer to start (30-60 seconds)
2. **Watch Windows:** Service windows show startup progress
3. **Check Logs:** Look for "Started AuditApplication" in Spring Boot window
4. **Kafka Topics:** Auto-created when first used (no manual creation needed)
5. **Retry Logic:** Scripts have retry logic, but you may need to wait longer

---

## 🎯 Recommended Approach

**For beginners:**
```powershell
.\quick-start.ps1
```

**For daily use:**
```powershell
.\check-services.ps1
# If needed:
.\start-all-services.ps1
# Then:
.\test-everything.ps1
```

**For troubleshooting:**
```powershell
.\setup-and-test.ps1
```

---

## 📝 Summary

| Script | When to Use | Time |
|--------|-------------|------|
| `quick-start.ps1` | First time or full restart | 2-3 min |
| `check-services.ps1` | Quick status check | 5 sec |
| `start-all-services.ps1` | Start services manually | 1-2 min |
| `setup-and-test.ps1` | Detailed setup | 2-3 min |
| `test-everything.ps1` | Run tests | 30 sec |

**Remember:** Always wait for "Started AuditApplication" before running tests!

