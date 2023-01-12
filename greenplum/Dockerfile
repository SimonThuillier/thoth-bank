# Greenplum MPP database docker image based on ubuntu 18.04
# see https://greenplum.org/install-greenplum-oss-on-ubuntu/
# also see https://docs.vmware.com/en/VMware-Tanzu-Greenplum/6/greenplum-database/GUID-landing-index.html

FROM ubuntu:bionic

# update apt packages, add greenplum ppa and install it
RUN apt update && apt install -y software-properties-common locales sudo && \
    add-apt-repository ppa:greenplum/db && \
    apt update
RUN apt install -y greenplum-db-6

RUN sed -i -e 's/# en_US.UTF-8 UTF-8/en_US.UTF-8 UTF-8/' /etc/locale.gen && \
    sed -i -e 's/# fr_FR.UTF-8 UTF-8/fr_FR.UTF-8 UTF-8/' /etc/locale.gen && \
    dpkg-reconfigure --frontend=noninteractive locales

# creating user and data directories
RUN adduser greenplum
RUN usermod -aG sudo greenplum && echo "greenplum:greenplum"|chpasswd
# allow greenplum to run service as sudo without password
RUN echo "greenplum ALL = NOPASSWD: /usr/sbin/service" >> /etc/sudoers
RUN chown -R greenplum:greenplum /opt/greenplum-*
WORKDIR /home/greenplum
RUN mkdir ./data
COPY gpinitsystem_singlenode .
COPY entrypoint.sh .
COPY postgresql.conf .
COPY pg_hba.conf .

# copying and customizing config files needs container runtime hostname, hence will be done in entrypoint.sh
RUN chown -R greenplum:greenplum /home/greenplum
RUN chmod +x /home/greenplum/entrypoint.sh

EXPOSE 5432/tcp
EXPOSE 5432/udp

USER greenplum
ENTRYPOINT ["/home/greenplum/entrypoint.sh"]
# docker build -t gp:latest .
# docker stop gp-1 && docker rm gp-1 && docker run --name=gp-1 -p 6432:5432 --ulimit n 65535 gp:latest
# docker stop gp-1 && docker rm gp-1
# docker run --name=gp-1 -p 6432:5432 --volume=/home/simon/projects/kafka/thoth-bank/greenplum/gpdata:/home/greenplum/data gp:latest
# docker exec -it -u 0 gp-1 bash
# docker rmi $(docker images -f “dangling=true” -q)

# PGPASSWORD=greenplum psql -U greenplum -h localhost -p 5657 -W gpdb
# docker start gp-1
# docker exec -it -u greenplum gp-1 source /opt/greenplum-db-*/greenplum_path.sh && gpstart