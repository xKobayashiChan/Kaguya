@echo off
chcp 65001 >nul
title Kaguya Client - Add User

echo ============================================
echo   Kaguya Client - ユーザー登録ツール
echo ============================================
echo.

set /p USERID=ユーザーID:
set /p PASSWORD=パスワード:

echo.
echo 登録中...

powershell -Command ^
  "$pw = '%PASSWORD%';" ^
  "$bytes = [System.Text.Encoding]::UTF8.GetBytes($pw);" ^
  "$sha = [System.Security.Cryptography.SHA256]::Create();" ^
  "$hash = -join ($sha.ComputeHash($bytes) | ForEach-Object { $_.ToString('x2') });" ^
  "$json = '{\"passwordHash\":\"' + $hash + '\",\"hwid\":\"\",\"username\":\"%USERID%\"}';" ^
  "$url = 'https://kaguya-auth-default-rtdb.firebaseio.com/users/%USERID%.json?auth=x4oa1n8gabAWUBj3VUCw8C3xnfxSFquxux7WDuyg';" ^
  "try {" ^
  "  $r = Invoke-RestMethod -Uri $url -Method Put -Body $json -ContentType 'application/json';" ^
  "  Write-Host '';" ^
  "  Write-Host '✓ 登録成功!' -ForegroundColor Green;" ^
  "  Write-Host '';" ^
  "  Write-Host \"  ユーザーID: %USERID%\";" ^
  "  Write-Host \"  パスワード: %PASSWORD%\";" ^
  "  Write-Host '';" ^
  "  Write-Host '※ 初回ログイン時にHWIDが自動で紐付けされます';" ^
  "} catch {" ^
  "  Write-Host '';" ^
  "  Write-Host '✗ 登録失敗:' $_.Exception.Message -ForegroundColor Red;" ^
  "}"

echo.
pause

