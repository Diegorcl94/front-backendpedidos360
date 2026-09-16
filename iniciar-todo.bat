@echo off
setlocal
title Pedidos360

echo ==========================================
echo        INICIANDO PEDIDOS360
echo ==========================================
echo.

set "ROOT=%~dp0"

echo [1/2] Iniciando Frontend Angular...
pushd "%ROOT%frontend\pedidos360-frontend" || exit /b 1
start "Pedidos360 - Frontend" cmd /k npx ng serve
popd

echo [2/2] Iniciando Backend Spring Boot...
set "AZURE_TENANT_ID=bb5324af-c266-41ed-b36c-a971641c7af2"
set "AZURE_API_CLIENT_ID=07f91b77-8d9f-46a3-964f-5220af9bb815"
pushd "%ROOT%backend\pedidos360-bff" || exit /b 1
start "Pedidos360 - Backend" cmd /k call "C:\Tools\apache-maven-3.9.16\bin\mvn.cmd" spring-boot:run
popd

echo.
echo Frontend: http://localhost:4200
echo Backend:  http://localhost:8080
echo Health:   http://localhost:8080/api/health
echo.
echo Se abrieron dos terminales.
pause
endlocal
