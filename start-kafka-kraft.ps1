# PowerShell script to start Kafka in KRaft mode (without Zookeeper) on Windows

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Starting Kafka in KRaft Mode (No Zookeeper)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$kafkaHome = "C:\Kafka\kafka_2.13-4.1.0"
$kraftConfig = "$kafkaHome\config\kraft\server.properties"
$logDir = "C:\Kafka\kafka_2.13-4.1.0\kraft-logs"

# Check if Kafka directory exists
if (-not (Test-Path $kafkaHome)) {
    Write-Host "ERROR: Kafka directory not found at: $kafkaHome" -ForegroundColor Red
    Write-Host "Please update the kafkaHome variable in this script" -ForegroundColor Yellow
    exit 1
}

# Check if KRaft config exists, if not create it
if (-not (Test-Path $kraftConfig)) {
    Write-Host "`nKRaft configuration not found. Creating it..." -ForegroundColor Yellow
    
    # Create kraft directory if it doesn't exist
    $kraftDir = "$kafkaHome\config\kraft"
    if (-not (Test-Path $kraftDir)) {
        New-Item -ItemType Directory -Path $kraftDir -Force | Out-Null
    }
    
    # Create server.properties for KRaft
    $configContent = @"
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
log.dirs=$logDir

############################# Log Retention Policy #############################
# The minimum age of a log file to be eligible for deletion
log.retention.hours=168

# The maximum size of a log before it is deleted
log.retention.bytes=1073741824

# The maximum time before a log is rolled
log.roll.hours=168

# The maximum size of a log segment file
log.segment.bytes=1073741824
"@
    
    $configContent | Out-File -FilePath $kraftConfig -Encoding UTF8
    Write-Host "✅ KRaft configuration created at: $kraftConfig" -ForegroundColor Green
}

# Verify config file has correct log directory
if (Test-Path $kraftConfig) {
    $configContent = Get-Content $kraftConfig -Raw
    if ($configContent -match "log\.dirs=(.+)$") {
        $configuredLogDir = $matches[1].Trim()
        if ($configuredLogDir -ne $logDir) {
            Write-Host "`n⚠️  WARNING: Config file has different log directory!" -ForegroundColor Yellow
            Write-Host "  Config says: $configuredLogDir" -ForegroundColor Yellow
            Write-Host "  Script expects: $logDir" -ForegroundColor Yellow
            Write-Host "`n  This may cause issues. Run .\fix-kafka-log-directory.ps1 to fix this." -ForegroundColor Red
            $continue = Read-Host "Continue anyway? (y/n)"
            if ($continue -ne "y") {
                exit 1
            }
        }
    }
}

# Check for failed log directories (kraft-combined-logs vs kraft-logs)
Write-Host "`nChecking for log directory issues..." -ForegroundColor Yellow
$possibleLogDirs = @(
    "$kafkaHome\kraft-combined-logs",
    "$kafkaHome\kraft-logs"
)

foreach ($possibleDir in $possibleLogDirs) {
    if (Test-Path $possibleDir) {
        # Check if this is the configured directory
        if ($possibleDir -ne $logDir) {
            Write-Host "  Found alternative log directory: $possibleDir" -ForegroundColor Yellow
            Write-Host "  This may be causing issues. Run .\fix-kafka-log-directory.ps1 to fix." -ForegroundColor Yellow
        }
    }
}

# Check for stray log directories and clean them up
Write-Host "`nChecking for stray log directories..." -ForegroundColor Yellow
$auditLogsDir = Join-Path $logDir "audit-logs-0"
if (Test-Path $auditLogsDir) {
    Write-Host "Found old audit-logs-0 directory. Attempting to clean up..." -ForegroundColor Yellow
    try {
        Remove-Item -Path $auditLogsDir -Recurse -Force -ErrorAction Stop
        Write-Host "✅ Cleaned up old audit-logs directory" -ForegroundColor Green
    } catch {
        Write-Host "⚠️  Warning: Could not delete old audit-logs directory" -ForegroundColor Yellow
        Write-Host "   Error: $_" -ForegroundColor Yellow
        Write-Host "   You may need to run as Administrator or stop Kafka first." -ForegroundColor Yellow
        Write-Host "   Or run: .\fix-kafka-log-directory.ps1" -ForegroundColor Cyan
    }
}

# Ensure log directory exists
if (-not (Test-Path $logDir)) {
    Write-Host "`nCreating log directory: $logDir" -ForegroundColor Yellow
    New-Item -ItemType Directory -Path $logDir -Force | Out-Null
    Write-Host "✅ Created log directory" -ForegroundColor Green
}

# Check if storage is formatted
$metaProperties = "$logDir\meta.properties"
if (-not (Test-Path $metaProperties)) {
    Write-Host "`n⚠️  KRaft storage not formatted yet!" -ForegroundColor Yellow
    Write-Host "Formatting storage (first time only)..." -ForegroundColor Cyan
    
    # Generate cluster ID
    $clusterId = [System.Guid]::NewGuid().ToString()
    Write-Host "Cluster ID: $clusterId" -ForegroundColor Cyan
    
    # Format storage (use --standalone for single-node setup)
    cd $kafkaHome
    $formatResult = & .\bin\windows\kafka-storage.bat format -t $clusterId -c $kraftConfig --standalone 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Storage formatted successfully!" -ForegroundColor Green
    } else {
        Write-Host "❌ ERROR: Failed to format storage" -ForegroundColor Red
        Write-Host "Error: $formatResult" -ForegroundColor Red
        Write-Host "`nTry manually:" -ForegroundColor Yellow
        Write-Host "  cd $kafkaHome" -ForegroundColor White
        Write-Host "  .\bin\windows\kafka-storage.bat format -t $clusterId -c $kraftConfig --standalone" -ForegroundColor White
        exit 1
    }
}

# Check if ports are already in use
Write-Host "`nChecking if ports are available..." -ForegroundColor Yellow

$port9092 = Get-NetTCPConnection -LocalPort 9092 -ErrorAction SilentlyContinue
$port9093 = Get-NetTCPConnection -LocalPort 9093 -ErrorAction SilentlyContinue

if ($port9092) {
    Write-Host "WARNING: Port 9092 (Kafka Broker) is already in use!" -ForegroundColor Yellow
    Write-Host "You may need to stop the existing Kafka process first" -ForegroundColor Yellow
    $continue = Read-Host "Continue anyway? (y/n)"
    if ($continue -ne "y") {
        exit 1
    }
}

if ($port9093) {
    Write-Host "WARNING: Port 9093 (Kafka Controller) is already in use!" -ForegroundColor Yellow
    Write-Host "You may need to stop the existing Kafka process first" -ForegroundColor Yellow
    $continue = Read-Host "Continue anyway? (y/n)"
    if ($continue -ne "y") {
        exit 1
    }
}

# Start Kafka in KRaft mode
Write-Host "`nStarting Kafka in KRaft mode..." -ForegroundColor Green
Write-Host "  Broker port: 9092" -ForegroundColor Cyan
Write-Host "  Controller port: 9093" -ForegroundColor Cyan
Write-Host "  Log directory: $logDir" -ForegroundColor Cyan

Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$kafkaHome'; Write-Host 'Starting Kafka in KRaft mode (No Zookeeper)...' -ForegroundColor Green; Write-Host 'Broker: localhost:9092, Controller: localhost:9093' -ForegroundColor Cyan; .\bin\windows\kafka-server-start.bat $kraftConfig"

# Wait for Kafka to start
Write-Host "Waiting 15 seconds for Kafka to start..." -ForegroundColor Yellow
Start-Sleep -Seconds 15

# Test Kafka connection
Write-Host "`nTesting Kafka connection..." -ForegroundColor Cyan
cd $kafkaHome

try {
    $topics = & .\bin\windows\kafka-topics.bat --list --bootstrap-server localhost:9092 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "`n✅ SUCCESS: Kafka is running in KRaft mode!" -ForegroundColor Green
        Write-Host "Topics found:" -ForegroundColor Cyan
        if ($topics -and $topics.Count -gt 0) {
            $topics | ForEach-Object { 
                if ($_ -notmatch "ERROR" -and $_ -notmatch "WARN") {
                    Write-Host "  - $_" -ForegroundColor White 
                }
            }
        } else {
            Write-Host "  (No topics yet - they will be created automatically)" -ForegroundColor Gray
        }
    } else {
        Write-Host "`n⚠️  WARNING: Kafka might still be starting up" -ForegroundColor Yellow
        Write-Host "Error output: $topics" -ForegroundColor Yellow
        Write-Host "Please wait a few more seconds and test manually:" -ForegroundColor Yellow
        Write-Host "  .\bin\windows\kafka-topics.bat --list --bootstrap-server localhost:9092" -ForegroundColor Cyan
    }
} catch {
    Write-Host "`n⚠️  Could not test Kafka connection" -ForegroundColor Yellow
    Write-Host "Error: $_" -ForegroundColor Red
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Kafka started in KRaft mode (No Zookeeper needed!)" -ForegroundColor Green
Write-Host "Keep the window open while using Kafka!" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Cyan

Write-Host "`nTo verify everything is working, run:" -ForegroundColor Cyan
Write-Host "  cd C:\Kafka\kafka_2.13-4.1.0" -ForegroundColor White
Write-Host "  .\bin\windows\kafka-topics.bat --list --bootstrap-server localhost:9092" -ForegroundColor White

Write-Host "`nTo stop Kafka, close the PowerShell window that opened." -ForegroundColor Yellow
Write-Host "`nNote: No Zookeeper needed in KRaft mode! 🎉" -ForegroundColor Green

