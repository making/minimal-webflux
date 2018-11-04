#!/bin/bash

SERVICE_INSTANCE_NAME=tweets-db
CREDS=$(echo $VCAP_SERVICES | jq -r ".[] | map(select(.name == \"${SERVICE_INSTANCE_NAME}\"))[0].credentials")

export URL=$(echo $CREDS | jq -r .uri)
export DB_HOST=$(echo $URL | awk -F '//' '{print $2}' | awk -F ':' '{print $2}' | awk -F '@' '{print $2}')
export DB_PORT=$(echo $URL | awk -F '//' '{print $2}' | awk -F ':' '{print $3}' | awk -F '/' '{print $1}')
export DB_USERNAME=$(echo $URL | awk -F '//' '{print $2}' | awk -F ':' '{print $1}')
export DB_PASSWORD=$(echo $URL | awk -F '//' '{print $2}' | awk -F ':' '{print $2}' | awk -F '@' '{print $1}')
export DB_DATABASE=$(echo $URL | awk -F '//' '{print $2}' | awk -F ':' '{print $3}' | awk -F '/' '{print $2}')
