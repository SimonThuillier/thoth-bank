# script to use upon container restart to relaunch greenplum cluster
# must be executed by user greenplum
export MASTER_DATA_DIRECTORY=/home/greenplum/data/master/gpsne-1
source /opt/greenplum-db-*/greenplum_path.sh
gpstart -y