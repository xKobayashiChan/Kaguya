@echo off
chcp 65001 >nul
title Kaguya Client - Reset HWID

echo ============================================
echo   Kaguya Client - HWIDリセットツール
echo ============================================
echo.
echo ユーザーがPCを変えた時に使います。
echo HWIDをリセットすると、次回ログイン時に
echo 新しいPCのHWIDが自動登録されます。
echo.

set /p USERID=リセットするユーザーID:

echo.
echo リセット中...

powershell -Command ^
  "$json = '{\"hwid\":\"\"}';" ^
  "$url = 'https://kaguya-auth-default-rtdb.firebaseio.com/users/%USERID%.json?auth=x4oa1n8gabAWUBj3VUCw8C3xnfxSFquxux7WDuyg';" ^
  "try {" ^
  "  $headers = @{ 'X-HTTP-Method-Override' = 'PATCH' };" ^
  "  $r = Invoke-RestMethod -Uri $url -Method Post -Body $json -ContentType 'application/json' -Headers $headers;" ^
  "  Write-Host '';" ^
  "  Write-Host '✓ HWIDリセット成功!' -ForegroundColor Green;" ^
  "  Write-Host \"  ユーザー: %USERID%\";" ^
  "  Write-Host '  次回ログイン時に新しいHWIDが登録されます';" ^
  "} catch {" ^
  "  Write-Host '';" ^
  "  Write-Host '✗ リセット失敗:' $_.Exception.Message -ForegroundColor Red;" ^
  "}"

echo.
pause

