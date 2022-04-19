#!/usr/bin/env bash
serverJar="server.jar"
if [[ ! -f $serverJar ]]; then
  echo "No server.jar found, aborting"
  exit 1
fi
if [[ ! `command -v java` ]]; then
  echo "Java not found, make sure java is on the path"
  exit 1
fi
java -jar $serverJar $@
