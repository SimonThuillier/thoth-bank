#!/bin/bash
cd /home/greenplum
sed -i "s/MASTER_HOSTNAME=.*/MASTER_HOSTNAME=$(hostname)/" ./gpinitsystem_singlenode
touch hostlist_singlenode && sed -i "s/.*/$(hostname)/" ./hostlist_singlenode
echo "$(hostname)" >> ./hostlist_singlenode

if [ ! -d "/home/greenplum/data/master" ]; then
  mkdir /home/greenplum/data/master
fi
if [ ! -d "/home/greenplum/data/primary" ]; then
  mkdir /home/greenplum/data/primary
fi

# start ssh
sudo service ssh start

source /opt/greenplum-db-*/greenplum_path.sh
# generating glsp nodes ssh keys
gpssh-exkeys -f /home/greenplum/hostlist_singlenode
# starting gp db service
gpinitsystem -a -c /home/greenplum/gpinitsystem_singlenode

# tuning configuration and reloading service
cp /home/greenplum/postgresql.conf /home/greenplum/data/master/gpsne-1
cp /home/greenplum/pg_hba.conf /home/greenplum/data/master/gpsne-1
export MASTER_DATA_DIRECTORY=/home/greenplum/data/master/gpsne-1
gpstop -u

# create default greenplum database gpdb and set greenplum pwd
createdb gpdb
echo "ALTER USER greenplum WITH PASSWORD 'greenplum';" >> /tmp/alter_gp_pwd.sql
psql gpdb < /tmp/alter_gp_pwd.sql

echo "---------- Startup ended ----------"
echo "Test Greenplum DB correct boot with command : PGPASSWORD=greenplum psql -U greenplum -h localhost -p 6432 -W gpdb"

# run container forever
tail -f >> /dev/null
