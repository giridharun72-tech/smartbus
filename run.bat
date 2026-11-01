@echo off
echo ========================================================
echo  Starting SmartBus Web Application...
echo  Default profile: dev (H2 in MySQL mode for instant run)
echo  To run with MySQL: run.bat --spring.profiles.active=mysql
echo ========================================================
call mvnw.cmd spring-boot:run %*
