<#
.SYNOPSIS
    Backup load-test script that keeps sending traffic during Nacos hot reload (pure PowerShell, no wrk needed).
.DESCRIPTION
    Sends concurrent requests to the probe endpoint GET /api/resume/test/probe for a given duration,
    counts total/success/failed requests and error rate, collects the set of returned value values,
    to verify the error rate stays 0 during Nacos config hot reload and value switches smoothly.
.EXAMPLE
    .\probe_hotreload.ps1 -DurationSeconds 60 -Concurrency 20
#>
param(
    [string]$Url = "http://localhost:8083/api/resume/test/probe",
    [int]$DurationSeconds = 60,
    [int]$Concurrency = 20
)

Write-Host "Target endpoint : $Url"
Write-Host "Duration        : $DurationSeconds s"
Write-Host "Concurrency     : $Concurrency"
Write-Host "Load test running. Please go to Nacos, modify test.dynamic-value and publish..." -ForegroundColor Yellow

$deadline = (Get-Date).AddSeconds($DurationSeconds)

# One background Job per concurrent worker keeps sending traffic
$jobs = 1..$Concurrency | ForEach-Object {
    Start-Job -ScriptBlock {
        param($u, $end)
        $ok = 0; $fail = 0; $values = @{}
        while ((Get-Date) -lt $end) {
            try {
                $resp = Invoke-RestMethod -Uri $u -Method Get -TimeoutSec 5
                $ok++
                $v = [string]$resp.value
                if (-not $values.ContainsKey($v)) { $values[$v] = 0 }
                $values[$v]++
            } catch {
                $fail++
            }
        }
        [pscustomobject]@{ Ok = $ok; Fail = $fail; Values = $values }
    } -ArgumentList $Url, $deadline
}

$results = $jobs | Wait-Job | Receive-Job
$jobs | Remove-Job

$totalOk   = ($results | Measure-Object -Property Ok   -Sum).Sum
$totalFail = ($results | Measure-Object -Property Fail -Sum).Sum
$total     = $totalOk + $totalFail
$errRate   = if ($total -gt 0) { [math]::Round($totalFail / $total * 100, 4) } else { 0 }

# Aggregate occurrence count for each value
$valueAgg = @{}
foreach ($r in $results) {
    foreach ($k in $r.Values.Keys) {
        if (-not $valueAgg.ContainsKey($k)) { $valueAgg[$k] = 0 }
        $valueAgg[$k] += $r.Values[$k]
    }
}

$errColor = if ($errRate -eq 0) { 'Green' } else { 'Red' }

Write-Host ""
Write-Host "========== Hot Reload Load Test Result ==========" -ForegroundColor Cyan
Write-Host ("Total requests : {0}" -f $total)
Write-Host ("Success        : {0}" -f $totalOk)
Write-Host ("Failed         : {0}" -f $totalFail)
Write-Host ("Error rate     : {0}%  (expected 0%)" -f $errRate) -ForegroundColor $errColor
Write-Host "Observed value distribution (verify switch before/after hot reload):"
$valueAgg.GetEnumerator() | Sort-Object Value -Descending | ForEach-Object {
    Write-Host ("   {0} : {1} times" -f $_.Key, $_.Value)
}
Write-Host "=================================================" -ForegroundColor Cyan
