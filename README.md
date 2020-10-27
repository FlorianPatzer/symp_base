# SyMP Framework running in Docker 
This repo automates the start of the SyMP framework within Docker

## 1. Getting started
The following services are started:
- Camunda BPMN 
- MySQL DB
- Simple Read-Only FTP Server 

### 1.1 Prerequisites
- Docker-compose

### 1.2 Information about the containers

#### VSFTP Server:
    - Anonymous connections are enabled 
    - Upload are allowed in the /upload directory 
    - Connection is SSL encrypted with a self signed certificate
    
#### Camunda BPMN:
    - Custom .war webapps are to be added in /camunda/webapps folder
    - Each webapp needs a "COPY" entry in the Dockerfile of camunda

#### MySQL:
    - Runs on port 3306
    - Default password: 0b53c4f 

### 1.3 Running the application

After cloning the repository, start a terminal in it's direcotry and execute: 

```
docker-compose up
```

## 2. Known Issues
The ftp container seams to not accept setfacl operations if docker storage driver "aufs" is configured. Since these operations are necessary to be performed on startup, the docker storage driver should be changed ("overlay2" has proven to be a good alternative).