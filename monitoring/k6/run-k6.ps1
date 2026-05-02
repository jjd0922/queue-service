param(
  [ValidateSet("smoke", "load", "stress", "spike")]
  [string]$Scenario = "smoke",
  [string]$BaseUrl = "http://localhost:8081",
  [string]$QueueId = "product:100",
  [int]$StartUserId = 100000
)

$env:SCENARIO = $Scenario
$env:BASE_URL = $BaseUrl
$env:QUEUE_ID = $QueueId
$env:START_USER_ID = "$StartUserId"

$scriptPath = Join-Path $PSScriptRoot "queue-enter.js"

k6 run `
  --summary-trend-stats="avg,min,med,max,p(90),p(95),p(99)" `
  $scriptPath
