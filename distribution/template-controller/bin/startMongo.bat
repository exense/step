rem @echo off

SET MONGO_PATH=
rem if mongod.exe isn't on your path, then set your own as follows (watch for the backslash and double quote at the end):
rem SET MONGO_PATH="D:\Program Files\MongoDB\Server\3.0\bin"\

for /f "skip=1" %%x in ('wmic os get localdatetime') do if not defined mydate set mydate=%%x

%MONGO_PATH%mongod.exe -dbpath ..\data\mongodb > mongod_%mydate%.log 2>&1
