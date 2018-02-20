#!/bin/bash
rm -r ./deploy
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
