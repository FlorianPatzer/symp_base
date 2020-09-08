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
    
- Camunda BPM
    - In order to run custom apps, they have to be added under the volume sub-section of the camunda section in the **docker-compose.yml** file. 
    A concrete example is to be found there.
    - Mounting the whole **webapps* folder will lead to a clean Camunda distro withouth a cockpit and examples.([explained in the official repo](https://github.com/camunda/docker-camunda-bpm-platform#add-own-process-application))

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
