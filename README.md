# SyMP Framework running in Docker 
This repo automates the start of the SyMP framework within Docker

## 1. Getting started
The following services are started:
- Camunda BPMN 
- MySQL DB
- Simple FTP Server 

### 1.1 Prerequisites
- Docker-compose

### 1.2 Information about the containers

#### VSFTPD Server:
    - Only connections in ACTIVE mode are accepted
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

### 1.4 Remote Services
- All of the services are running currently on the Fraunhofer k8s cluster and can be accessed only from within the cluster or from the Gitlab CI pipeline
- The Camunda framework web inerface is accessible at https://symp-camunda.k8s.ilt-dmz.iosb.fraunhofer.de/camunda
- Filezilla Client is hosted as a webservice at https://symp-filezilla.k8s.ilt-dmz.iosb.fraunhofer.de/ making it possible to see the content of the **/upload** folder of the FTP server

## 2. Known Issues
1. The ftp container seams to not accept setfacl operations if docker storage driver "aufs" is configured. Since these operations are necessary to be performed on startup, the docker storage driver should be changed ("overlay2" has proven to be a good alternative).

2. PASSIVE mode doesn't work/is hard to configure in the k8s cluster, because of firewall issues. Therefore the server is set up to accept only connections in ACTIVE mode.
