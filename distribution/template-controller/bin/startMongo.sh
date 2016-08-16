MONGO_PATH="/usr/bin"

${MONGO_PATH}/mongod -dbpath ../data/mongodb --smallfiles > ./mongod_$(date +"%s").log 2>&1
