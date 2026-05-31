
```shell
# public route
curl localhost:8181

# should return 401 Unauthorized when JWT is not provided or is invalid
curl localhost:8181/protected

# should return 200 OK with a valid JWT
curl localhost:8181/protected -H "Authorization: eyJhbGc....."

# should return the authenticated user ID
curl localhost:8181/protected/whoami -H "Authorization: eyJhbGc....."
```
