#!/bin/bash
mtcdatetime=`date +%Y%m%d%H%M%S`
echo $mtcdatetime
echo This script will preserve the contents of /opt/mtc in the following TAR file: /tmp/${mtcdatetime}_opt_mtc.tar
tar cvf /tmp/${mtcdatetime}_opt_mtc.tar /opt/mtc
sudo mkdir -p /opt/mtc/out
sudo chmod 775 /opt/mtc/out
sudo mkdir -p /opt/mtc/config
sudo chmod 775 /opt/mtc/config
sudo cp ./config/* /opt/mtc/config
sudo chmod 775 /opt/mtc/config/*
/usr/bin/sudo chown -R flaredev:flaredev /opt/mtc
sudo rm -rf ./deploy
mvn clean package
mkdir -p ./deploy/config
cp ./target/*.jar ./deploy
cp ./config/* ./deploy/config
cp ./bin/* ./deploy
cd ./deploy
/usr/bin/sudo chown -R flaredev:flaredev ./*
tar cvf /tmp/FLAREmispService.tar ./*
mv /tmp/FLAREmispService.tar .
/usr/bin/sudo chown flaredev:flaredev  ./FLAREmispService.tar .
echo FLAREmispService.tar has been created.
