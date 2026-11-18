# Pause or resume the small-model training. Run once to pause, run again to resume:
#   powershell -ExecutionPolicy Bypass -File pause-training.ps1
# Training stops at the end of its current step (up to about a minute) and waits.
# While paused it uses no processor time, but still holds some graphics memory.
$flag = 'C:\Users\aries\tenvia-train\PAUSE'
if (Test-Path $flag) {
    Remove-Item $flag
    Write-Output 'RESUMED: training carries on.'
} else {
    New-Item -ItemType File $flag | Out-Null
    Write-Output 'PAUSING: training will stop at the end of its current step. Run this again to resume.'
}
