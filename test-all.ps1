$ErrorActionPreference = 'Continue'
$originalLocation = Get-Location
$repoRoot = $PSScriptRoot
$backendStatus = 'FAILED'
$frontendStatuses = @{
    test = 'FAILED'
    lint = 'FAILED'
    build = 'FAILED'
}

function Test-CommandAvailable {
    param([Parameter(Mandatory = $true)][string]$Name)
    return $null -ne (Get-Command $Name -ErrorAction SilentlyContinue)
}

function Write-Preflight {
    param([Parameter(Mandatory = $true)][string]$Label, [Parameter(Mandatory = $true)][bool]$Passed)
    $status = if ($Passed) { 'SUCCESS' } else { 'FAILED' }
    Write-Host ("{0}: {1}" -f $Label, $status)
    return $Passed
}

Write-Host 'Sprawdzanie wymaganych narzedzi i srodowiska...'
$javaReady = $false
if (Test-CommandAvailable 'java') {
    $javaVersionOutput = & java -version 2>&1
    $javaVersionExit = $LASTEXITCODE
    $javaVersionText = $javaVersionOutput -join "`n"
    $javaReady = ($javaVersionExit -eq 0) -and ($javaVersionText -match '\b21(?:\.\d+)?\b')
}
$null = Write-Preflight 'Java 21' $javaReady

$wrapperReady = Test-Path -LiteralPath (Join-Path $repoRoot 'backend\mvnw.cmd') -PathType Leaf
$null = Write-Preflight 'Maven Wrapper' $wrapperReady

$nodeReady = Test-CommandAvailable 'node'
if ($nodeReady) {
    & node --version
    $nodeReady = $LASTEXITCODE -eq 0
}
$null = Write-Preflight 'Node.js' $nodeReady

$npmReady = Test-CommandAvailable 'npm.cmd'
if ($npmReady) {
    & npm.cmd --version
    $npmReady = $LASTEXITCODE -eq 0
}
$null = Write-Preflight 'npm' $npmReady

$dockerReady = $false
if (Test-CommandAvailable 'docker') {
    $dockerOutput = & docker info --format '{{.ServerVersion}}' 2>&1
    $dockerReady = ($LASTEXITCODE -eq 0) -and -not [string]::IsNullOrWhiteSpace(($dockerOutput -join ''))
}
$null = Write-Preflight 'Docker Engine' $dockerReady

if ($javaReady -and $wrapperReady -and $dockerReady) {
    Write-Host "`nUruchamianie testow backendu..."
    Push-Location -LiteralPath (Join-Path $repoRoot 'backend')
    try {
        & .\mvnw.cmd test
        $backendExitCode = $LASTEXITCODE
        if ($backendExitCode -eq 0) {
            $backendStatus = 'SUCCESS'
        }
    }
    catch {
        Write-Error $_
    }
    finally {
        Pop-Location
    }
}
else {
    Write-Host "`nPominieto backend: wymagane narzedzia lub Docker Engine sa niedostepne."
}

if ($nodeReady -and $npmReady) {
    Write-Host "`nUruchamianie weryfikacji frontendu..."
    Push-Location -LiteralPath (Join-Path $repoRoot 'frontend')
    try {
        $verifyOutput = & npm.cmd run verify 2>&1
        $verifyExitCode = $LASTEXITCODE
        foreach ($line in $verifyOutput) {
            Write-Host $line
            $lineText = [string]$line
            if ($lineText -match 'ROUTECOST_VERIFY_TEST=(SUCCESS|FAILED)') {
                $frontendStatuses.test = $Matches[1]
            }
            elseif ($lineText -match 'ROUTECOST_VERIFY_LINT=(SUCCESS|FAILED)') {
                $frontendStatuses.lint = $Matches[1]
            }
            elseif ($lineText -match 'ROUTECOST_VERIFY_BUILD=(SUCCESS|FAILED)') {
                $frontendStatuses.build = $Matches[1]
            }
        }
        if ($verifyExitCode -ne 0) {
            Write-Host 'npm run verify zakonczyl sie kodem bledu.'
        }
    }
    catch {
        Write-Error $_
    }
    finally {
        Pop-Location
    }
}
else {
    Write-Host "`nPominieto frontend: wymagane sa dostepne Node.js i npm."
}

Set-Location -LiteralPath $originalLocation.Path

Write-Host "`nPodsumowanie weryfikacji"
Write-Host ("Backend:         {0}" -f $backendStatus)
Write-Host ("Frontend tests:  {0}" -f $frontendStatuses.test)
Write-Host ("Frontend lint:   {0}" -f $frontendStatuses.lint)
Write-Host ("Frontend build:  {0}" -f $frontendStatuses.build)

$allSucceeded = ($backendStatus -eq 'SUCCESS') -and
    ($frontendStatuses.test -eq 'SUCCESS') -and
    ($frontendStatuses.lint -eq 'SUCCESS') -and
    ($frontendStatuses.build -eq 'SUCCESS')
if ($allSucceeded) {
    exit 0
}
exit 1
