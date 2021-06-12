#!/bin/bash
yes | docker image prune
yes | docker container prune
yes | docker volume prune
