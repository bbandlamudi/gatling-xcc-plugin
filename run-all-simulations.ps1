$simulations = @(
    "AdvancedSimulation",
    "BasicSimulation",
    "ChecksSimulation",
    "FeederWithSessionSimulation",
    "GenerateTestDataSimulation",
    "JavaScriptSimulation",
    "JsonPathSimulation",
    "MapResultSimulation",
    "ModuleInvocationSimulation",
    "MultipleDocumentsSimulation",
    "ProtocolConfigurationSimulation",
    "QuickTestSimulation",
    "SaveItemsByIndexSimulation",
    "XPathExtractAndReuseSimulation",
    "XccsSecureSimulation",
    "XmlChainSimplifiedSimulation",
    "XmlResponseChainSimulation"
)

$results = @()

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Running All Gatling Simulations" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$totalCount = $simulations.Count
$currentCount = 0

foreach ($sim in $simulations) {
    $currentCount++
    $fullClassName = "com.marklogic.gatling.xcc.example.$sim"
    
    Write-Host "[$currentCount/$totalCount] Running: $sim" -ForegroundColor Yellow
    Write-Host "----------------------------------------" -ForegroundColor Gray
    
    $output = & mvn gatling:test "-Dgatling.simulationClass=$fullClassName" 2>&1
    
    $success = $output -match "BUILD SUCCESS"
    $failed = $output -match "BUILD FAILURE"
    
    # Extract request count
    $requestLine = $output | Select-String -Pattern "> request count\s+\|\s+(\d+)\s+\|\s+(\d+)"
    $totalRequests = 0
    $okRequests = 0
    
    if ($requestLine) {
        if ($requestLine.Matches[0].Groups[1].Value) {
            $totalRequests = [int]$requestLine.Matches[0].Groups[1].Value
            $okRequests = [int]$requestLine.Matches[0].Groups[2].Value
        }
    }
    
    $status = if ($success) { "PASS" } elseif ($failed) { "FAIL" } else { "UNKNOWN" }
    
    $result = [PSCustomObject]@{
        Simulation = $sim
        Status = $status
        TotalRequests = $totalRequests
        SuccessfulRequests = $okRequests
    }
    
    $results += $result
    
    $color = if ($status -eq "PASS") { "Green" } else { "Red" }
    Write-Host "Status: $status | Requests: $okRequests/$totalRequests" -ForegroundColor $color
    Write-Host ""
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$results | Format-Table -AutoSize

$passCount = ($results | Where-Object { $_.Status -eq "PASS" }).Count
$failCount = ($results | Where-Object { $_.Status -eq "FAIL" }).Count

Write-Host ""
Write-Host "Total Simulations: $totalCount" -ForegroundColor Cyan
Write-Host "Passed: $passCount" -ForegroundColor Green
Write-Host "Failed: $failCount" -ForegroundColor $(if ($failCount -gt 0) { "Red" } else { "Green" })
Write-Host ""

if ($failCount -eq 0) {
    Write-Host "All simulations passed successfully!" -ForegroundColor Green
} else {
    Write-Host "Some simulations failed. Check the output above for details." -ForegroundColor Red
}
