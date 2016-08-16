#!/bin/sh

MONGO_PATH="/usr/local/bin"

ABSPATH=$(cd "$(dirname "$0")"; pwd)

echo “starting mongo..”

${MONGO_PATH}/mongod -dbpath ${ABSPATH}/../data/mongodb --smallfiles