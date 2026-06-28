# E2E chain smoke test (gateway :8080)
# Usage: powershell -NoProfile -File scripts/verify_e2e_chain.ps1

$ErrorActionPreference = 'Stop'
$base = 'http://localhost:8080'
$passed = 0
$failed = 0

function Test-Endpoint {
    param(
        [string]$Name,
        [string]$Url,
        [hashtable]$Headers = @{},
        [scriptblock]$Assert
    )
    try {
        $resp = Invoke-RestMethod -Uri $Url -Headers $Headers -TimeoutSec 20
        & $Assert $resp
        Write-Host "[OK] $Name" -ForegroundColor Green
        $script:passed++
    } catch {
        Write-Host "[FAIL] $Name - $($_.Exception.Message)" -ForegroundColor Red
        $script:failed++
    }
}

Write-Host "`n=== Login ===" -ForegroundColor Cyan
$login = Invoke-RestMethod -Uri "$base/api/auth/login" -Method POST `
    -Body 'username=hr@company.com&password=123456' `
    -ContentType 'application/x-www-form-urlencoded' -TimeoutSec 20
if ($login.code -ne 200 -or -not $login.token) { throw 'HR login failed' }
Write-Host "[OK] HR login" -ForegroundColor Green
$passed++

$headers = @{ Authorization = "Bearer $($login.token)" }

Write-Host "`n=== HR analytics ===" -ForegroundColor Cyan
Test-Endpoint 'dashboard' "$base/api/analytics/hr/dashboard" $headers {
    param($r)
    if ($r.code -ne 200) { throw $r.msg }
    if ($r.data.activeJobs -lt 1) { throw 'activeJobs is 0' }
    if ($r.data.placeholderFields -and $r.data.placeholderFields.Count -gt 0) {
        throw ('placeholderFields: ' + ($r.data.placeholderFields -join ','))
    }
}

Test-Endpoint 'trend' "$base/api/analytics/hr/trend" $headers {
    param($r)
    if ($r.code -ne 200) { throw $r.msg }
    if ($r.data.Count -lt 6) { throw 'trend points < 6' }
    $last = $r.data[-1]
    if ($last.applications -lt 1) { throw 'current month applications is 0' }
}

Test-Endpoint 'department-progress' "$base/api/analytics/hr/department-progress" $headers {
    param($r)
    if ($r.code -ne 200) { throw $r.msg }
    if ($r.data.Count -lt 1) { throw 'no department data' }
}

Test-Endpoint 'ai-insights' "$base/api/ai/hr/insights" $headers {
    param($r)
    if ($r.code -ne 200) { throw $r.msg }
    if ($null -eq $r.data.healthScore) { throw 'missing healthScore' }
}

Write-Host "`n=== HR todos ===" -ForegroundColor Cyan
Test-Endpoint 'pending-resumes' "$base/api/resume/hr/page?current=1&size=1&screenStatus=1" $headers {
    param($r)
    if ($r.code -ne 200) { throw $r.msg }
}

Test-Endpoint 'pending-offers' "$base/api/offer/list?current=1&size=5&status=1" $headers {
    param($r)
    if ($r.code -ne 200) { throw $r.msg }
    if ($r.data.total -lt 1) { throw 'no pending offer' }
}

Test-Endpoint 'interview-stats' "$base/api/interview/hr/stats" $headers {
    param($r)
    if ($r.code -ne 200) { throw $r.msg }
}

Write-Host "`n=== Candidate login ===" -ForegroundColor Cyan
try {
    $cand = Invoke-RestMethod -Uri "$base/api/auth/login" -Method POST `
        -Body 'username=13800138099&password=123456' `
        -ContentType 'application/x-www-form-urlencoded' -TimeoutSec 20
    if ($cand.code -ne 200) { throw $cand.msg }
    Write-Host "[OK] candidate login" -ForegroundColor Green
    $passed++
} catch {
    Write-Host "[FAIL] candidate login - $($_.Exception.Message)" -ForegroundColor Red
    $failed++
}

Write-Host "`nResult: $passed passed, $failed failed" -ForegroundColor $(if ($failed -eq 0) { 'Green' } else { 'Yellow' })
if ($failed -gt 0) { exit 1 }
