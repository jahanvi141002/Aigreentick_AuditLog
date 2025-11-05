# PowerShell script to fix Kafka log directory issues

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Fixing Kafka Log Directory Issues" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$kafkaHome = "C:\Kafka\kafka_2.13-4.1.0"
$kraftConfig = "$kafkaHome\config\kraft\server.properties"

# Check if Kafka directory exists
if (-not (Test-Path $kafkaHome)) {
    Write-Host "ERROR: Kafka directory not found at: $kafkaHome" -ForegroundColor Red
    Write-Host "Please update the kafkaHome variable in this script" -ForegroundColor Yellow
    exit 1
}

Write-Host "`nChecking for running Kafka processes..." -ForegroundColor Yellow

# Check if Java/Kafka processes are running
$javaProcesses = Get-Process -Name "java" -ErrorAction SilentlyContinue
if ($javaProcesses) {
    Write-Host "⚠️  WARNING: Java processes found. Kafka might be running." -ForegroundColor Yellow
    Write-Host "Please stop Kafka first (Ctrl+C in the Kafka window) before running this script." -ForegroundColor Yellow
    $continue = Read-Host "Continue anyway? This may cause issues. (y/n)"
    if ($continue -ne "y") {
        exit 1
    }
}

Write-Host "`nStep 1: Checking log directories..." -ForegroundColor Cyan

# Check for both possible log directories
$possibleLogDirs = @(
    "$kafkaHome\kraft-combined-logs",
    "$kafkaHome\kraft-logs"
)

$logDirsFound = @()
foreach ($dir in $possibleLogDirs) {
    if (Test-Path $dir) {
        Write-Host "  Found log directory: $dir" -ForegroundColor Yellow
        $logDirsFound += $dir
    }
}

if ($logDirsFound.Count -eq 0) {
    Write-Host "  No existing log directories found. This is normal for a fresh setup." -ForegroundColor Green
} else {
    Write-Host "`nStep 2: Backing up and removing old log directories..." -ForegroundColor Cyan
    
    foreach ($logDir in $logDirsFound) {
        Write-Host "`n  Processing: $logDir" -ForegroundColor Yellow
        
        # Create backup directory
        $backupDir = "$logDir.backup.$(Get-Date -Format 'yyyyMMdd_HHmmss')"
        
        try {
            Write-Host "    Creating backup: $backupDir" -ForegroundColor Cyan
            Move-Item -Path $logDir -Destination $backupDir -Force -ErrorAction Stop
            Write-Host "    ✅ Moved to backup: $backupDir" -ForegroundColor Green
        } catch {
            Write-Host "    ⚠️  Could not move directory (may be locked): $($_.Exception.Message)" -ForegroundColor Yellow
            
            # Try to delete directly
            Write-Host "    Attempting to delete directly..." -ForegroundColor Cyan
            try {
                Remove-Item -Path $logDir -Recurse -Force -ErrorAction Stop
                Write-Host "    ✅ Deleted: $logDir" -ForegroundColor Green
            } catch {
                Write-Host "    ❌ Failed to delete: $($_.Exception.Message)" -ForegroundColor Red
                Write-Host "    You may need to:" -ForegroundColor Yellow
                Write-Host "      1. Stop Kafka completely" -ForegroundColor White
                Write-Host "      2. Close any file explorers showing the directory" -ForegroundColor White
                Write-Host "      3. Run PowerShell as Administrator" -ForegroundColor White
                Write-Host "      4. Run this script again" -ForegroundColor White
                exit 1
            }
        }
    }
}

Write-Host "`nStep 3: Checking KRaft configuration..." -ForegroundColor Cyan

# Check if config exists
if (-not (Test-Path $kraftConfig)) {
    Write-Host "  Config file not found. Creating it..." -ForegroundColor Yellow
    
    # Create kraft directory if it doesn't exist
    $kraftDir = "$kafkaHome\config\kraft"
    if (-not (Test-Path $kraftDir)) {
        New-Item -ItemType Directory -Path $kraftDir -Force | Out-Null
    }
    
    # Use kraft-logs as the standard directory
    $logDir = "$kafkaHome\kraft-logs"
    
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
    Write-Host "  ✅ Config file created: $kraftConfig" -ForegroundColor Green
} else {
    Write-Host "  Config file found: $kraftConfig" -ForegroundColor Green
    
    # Check what log directory is configured
    $configContent = Get-Content $kraftConfig -Raw
    if ($configContent -match "log\.dirs=(.+)$") {
        $configuredLogDir = $matches[1].Trim()
        Write-Host "  Configured log directory: $configuredLogDir" -ForegroundColor Cyan
        
        # Ensure the configured directory exists
        if (-not (Test-Path $configuredLogDir)) {
            Write-Host "  Creating configured log directory: $configuredLogDir" -ForegroundColor Yellow
            New-Item -ItemType Directory -Path $configuredLogDir -Force | Out-Null
            Write-Host "  ✅ Created: $configuredLogDir" -ForegroundColor Green
        }
    }
}

Write-Host "`nStep 4: Formatting storage (first time only)..." -ForegroundColor Cyan

# Create fresh log directory
$logDir = "$kafkaHome\kraft-logs"
if (-not (Test-Path $logDir)) {
    Write-Host "  Creating fresh log directory: $logDir" -ForegroundColor Yellow
    New-Item -ItemType Directory -Path $logDir -Force | Out-Null
}

# Check if already formatted
$metaProperties = "$logDir\meta.properties"
if (Test-Path $metaProperties) {
    Write-Host "  Storage already formatted. Removing old format to reinitialize..." -ForegroundColor Yellow
    Remove-Item -Path "$logDir\*" -Recurse -Force -ErrorAction SilentlyContinue
}

# Generate cluster ID
$clusterId = [System.Guid]::NewGuid().ToString()
Write-Host "  Cluster ID: $clusterId" -ForegroundColor Cyan

# Format storage
Write-Host "  Formatting storage..." -ForegroundColor Yellow
cd $kafkaHome

$formatResult = & .\bin\windows\kafka-storage.bat format -t $clusterId -c $kraftConfig --standalone 2>&1

if ($LASTEXITCODE -eq 0) {
    Write-Host "  ✅ Storage formatted successfully!" -ForegroundColor Green
} else {
    Write-Host "  ❌ ERROR: Failed to format storage" -ForegroundColor Red
    Write-Host "  Error output:" -ForegroundColor Red
    Write-Host $formatResult -ForegroundColor Red
    Write-Host "`n  Try manually:" -ForegroundColor Yellow
    Write-Host "    cd $kafkaHome" -ForegroundColor White
    Write-Host "    .\bin\windows\kafka-storage.bat format -t $clusterId -c $kraftConfig --standalone" -ForegroundColor White
    exit 1
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "✅ Fix Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan

Write-Host "`nYou can now start Kafka:" -ForegroundColor Yellow
Write-Host "  .\start-kafka-kraft.ps1" -ForegroundColor White
Write-Host "`nOr manually:" -ForegroundColor Yellow
Write-Host "  cd $kafkaHome" -ForegroundColor White
Write-Host "  .\bin\windows\kafka-server-start.bat .\config\kraft\server.properties" -ForegroundColor White
Write-Host ""

