#!/bin/bash

#. variables.sh

source ./../../scripts/docker-scripts/common_funcs.sh
check_and_source_file ~/.bash_profile

BUILD_CONTAINER=$1
COMPOSE_UP_OPTS=$2
MS_SERVICE_TYPE=java
build_start_microservice_containers variables.sh ${MS_SERVICE_TYPE} ${BUILD_CONTAINER} ${COMPOSE_UP_OPTS}