#!/bin/bash
. ~/.bash_profile
docker run postgres_quantal_users & mvn package && docker-compose -f docker/compose/docker-compose.yml build --no-cache
