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
- FTP Server
    - Runs on port 21
    - Accepts anonymous connection 
    - Currently allows read-only access (The image is explicitly configured for anonymouse read-only acces. Changening configurations in the vsftpd.conf leads to a failing server at startup.)
    - The "ftp_dir" folder is mounted to the server, so files can be copied by hand there if they need to be accessed from any of the SyMP framework services
    
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
