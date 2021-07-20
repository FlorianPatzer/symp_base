## About The Project
Base services on which the SyMP framework depends.

### Built With

* [Camunda](https://camunda.com/)
* [MySQL](https://www.mysql.com/)
* [VSFTPD](https://security.appspot.com/vsftpd.html)
* [FileZilla](https://filezilla-project.org/)

## Getting Started

### Prerequisites

This project uses Docker to automatically build and setup the created services. Please make sure that you have Docker isntalled or follow the download instructions [here](https://docs.docker.com/docker-for-windows/install/).


### Installation
   - One-Liner: Use `docker-compose up --build` to build the containers and start them directly
   - Use `docker-compose build` to build the containers.
   - Use `docker-compose up` to start the containers.

## Usage

* VSFTPD Server:
    - Anonymous connections are enabled 
    - Upload are allowed in the /upload directory 
    - Connection is SSL encrypted with a self signed certificate
    
* Camunda BPMN:
    - Custom .war webapps are to be added in `/camunda/webapps` folder
    - Each webapp needs a `COPY` entry in the Dockerfile of camunda

* MySQL:
    - Default password: 0b53c4f 


## Additional

* Known issues:
    1. The ftp container seams to not accept setfacl operations if docker storage driver "aufs" is configured. Since these operations are necessary to be performed on startup, the docker storage driver should be changed ("overlay2" has proven to be a good alternative).