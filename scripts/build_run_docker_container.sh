#!/bin/bash

# THIS SCRIPT SHOULD BE RUN FROM THE 'scripts' directory

# If BUILD_CONTAINER variable is not set to 'true', then the containers will only be run and not built
# By default if there is no argument passed to this script, the BUILD_CONTAINER variable will set to 'true'
# This means that default behaviour of this script is to build and run the container
# i.e. `$ ./build_run_docker_containers.sh` without any arguments will build and run the container
# If you dont want to build the container and you only want to run the container, you can run the
# script with the first argument as anything other than 'true'. For instance pass 'false' as the first argument
# e.g. `$ ./build_run_docker_containers.sh false`

BUILD_CONTAINER=
COMPOSE_UP_OPTS=$2

# Source the variables
. variables.sh

BUILD_CONTAINER_COMMAND="&"
# We set BUILD_CONTAINER to true if $1 is empty because by default we want to build the container

if [ -z "$1" ]; then
    BUILD_CONTAINER=false
else
    # Covert input to lower case and trim leading and trailing white spaces
    BUILD_CONTAINER=$(echo "$1" | tr '[:upper:]' '[:lower:] | xargs')
fi

echo "BUILD_CONTAINER: ${BUILD_CONTAINER}"

if [ "${BUILD_CONTAINER}" == "true" ]; then
    BUILD_CONTAINER_COMMAND="${BUILD_CONTAINER_COMMAND} DB_PORT=${DB_PORT_EXPOSED_ON_HOST} cd ../ && mvn package && cd ./scripts && WEB_APP_PORT=${WEB_APP_PORT} docker-compose -f ../docker/compose/docker-compose.yml build --no-cache &&"
fi

COMMAND="WEB_APP_PORT=${WEB_APP_PORT} docker-compose -f ../docker/compose/docker-compose.yml up ${DB_CONTAINER_NAME} ${BUILD_CONTAINER_COMMAND} DB_PORT=${DB_PORT_EXPOSED_ON_DB_CONTAINER} WEB_APP_PORT=${WEB_APP_PORT} docker-compose -f ../docker/compose/docker-compose.yml up ${COMPOSE_UP_OPTS}"

echo "executing command: ${COMMAND}"

eval ${COMMAND}