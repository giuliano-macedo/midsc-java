# docker pull openjdk
# ant
# docker build -t openjdk:dc3 -f dockerfile .
FROM openjdk
WORKDIR /
MAINTAINER lsd200
COPY docker.jar /home/docker.jar