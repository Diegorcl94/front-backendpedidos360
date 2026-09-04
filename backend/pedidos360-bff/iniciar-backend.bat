@echo off
REM Antes de ejecutar, configura estas dos variables con tus IDs reales.
if "%AZURE_TENANT_ID%"=="" echo FALTA AZURE_TENANT_ID
if "%AZURE_API_CLIENT_ID%"=="" echo FALTA AZURE_API_CLIENT_ID
mvn spring-boot:run
pause
