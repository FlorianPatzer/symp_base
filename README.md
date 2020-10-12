# SyMP Framework running in Docker 
This repo automates the start of the SyMP framework within Docker

## Getting started
The following services are started:
- Camunda BPM 
- MySQL DB
- Simple Read-Only FTP Server 

### Prerequisites
- Docker-compose

### Configuration
- VSFTP Server
    - Runs on port 21
    - Anonymous connections are enabled 
    - Upload are allowed in the /upload directory 
    - Connection is SSL encrypted with a self signed certificate
    
- Camunda BPM
    - Custom webapps are be added in /camunda/webapps and a COPY command must be added to the Dockerfile 

- MySQL
    - Runs on port 3306
    - Default password: 0b53c4f

### Running the application

After cloning the repository, start a terminal in it's direcotry and execute: 

```
docker-compose up
```

## License
MIT
