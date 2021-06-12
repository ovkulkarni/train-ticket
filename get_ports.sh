docker network inspect -f '{{json .Containers}}' 3a4205d9a390 | jq '.[] | .Name + : + .IPv4Address'
