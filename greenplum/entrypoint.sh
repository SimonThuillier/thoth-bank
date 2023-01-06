#!/bin/bash
cd /home/greenplum
sed -i "s/MASTER_HOSTNAME=.*/MASTER_HOSTNAME=$(hostname)/" ./gpinitsystem_singlenode
touch hostlist_singlenode && sed -i "s/.*/$(hostname)/" ./hostlist_singlenode
echo "$(hostname)" >> ./hostlist_singlenode

# start ssh
sudo service ssh start

source /opt/greenplum-db-*/greenplum_path.sh
# generating gp nodes ssh keys
gpssh-exkeys -f /home/greenplum/hostlist_singlenode
# starting gp db service
gpinitsystem -c /home/greenplum/gpinitsystem_singlenode

echo "test"
tail -f >> /dev/null
