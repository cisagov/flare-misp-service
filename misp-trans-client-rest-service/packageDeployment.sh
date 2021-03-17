#!/bin/bash
mtcdatetime=`date +%Y%m%d%H%M%S`
echo $mtcdatetime
echo This script will preserve the contents of /opt/mtc in the following TAR file: /tmp/${mtcdatetime}_opt_mtc.tar

#BACKUP any existing files in /opt/mtc/* to the following TAR file with current DateTimeStamp 
tar cvf /tmp/${mtcdatetime}_opt_mtc.tar /opt/mtc

#Supporting /out subdir
sudo mkdir -p /opt/mtc/out
sudo chmod 775 /opt/mtc/out

#Supporting /logs subdir
sudo mkdir -p /opt/mtc/logs
sudo chmod 775 /opt/mtc/logs

#Supporting /avro subdir
sudo mkdir -p /opt/mtc/avro
sudo chmod 775 /opt/mtc/avro

#Supporting /config subdir
sudo mkdir -p /opt/mtc/config
sudo chmod 775 /opt/mtc/config
sudo cp ./config/* /opt/mtc/config
sudo chmod 775 /opt/mtc/config/*


/usr/bin/sudo chown -R flaredev:flaredev /opt/mtc

sudo rm -rf ./deploy

mvn clean package -DskipTests

mkdir -p ./deploy/config
cp ./target/*.jar ./deploy
cp ./config/* ./deploy/config


#Empty the development "logs" subdir for /mtc and create a placeholder file.  Production should not get development environment log files.
rm ./logs/*
echo "# This is a dummy log in the /logs subdirectory."  > ./logs/mtc-rest-service.log
echo "# This insures the subdir is not empty, " >> ./logs/mtc-rest-service.log
echo "# Will be contained within the Tarball, " >> ./logs/mtc-rest-service.log
echo "# And created on the target environment when the tarball is unpacked."  >> ./logs/mtc-rest-service.log
mkdir -p ./deploy/logs
cp ./logs/* ./deploy/logs


#Empty the development "out" subdir for /mtc and create a placeholder file.  Production should not get development environment out files.
rm ./out/*
echo "# Create an throw-away file in the /out subdirectory. " > ./out/throw_away_temp_file.txt
echo "# This insures the subdir is not empty, " >> ./out/throw_away_temp_file.txt
echo "# Will be contained within the Tarball, " >> ./out/throw_away_temp_file.txt
echo "# And created on the target environment when the tarball is unpacked. " >> ./out/throw_away_temp_file.txt
mkdir -p ./deploy/out
cp ./out/* ./deploy/out

#Create avro directory
mkdir -p ./deploy/avro

#Copy all JAR files and dependencies to the 'deploy' subdirectory.
cp ./bin/* ./deploy

#Enter the 'deploy' subdirectory
cd ./deploy

#Modify the ownerships of all files in the 'deploy' directory structure.
/usr/bin/sudo chown -R flaredev:flaredev ./*

#Tar up all files in the 'deploy' directory structure
tar cvf /tmp/FLAREmispService.tar ./*

#Move the new tarball to the 'tmp' directory
mv /tmp/FLAREmispService.tar .

#Change the ownership of the new tarball
/usr/bin/sudo chown flaredev:flaredev  ./FLAREmispService.tar .

echo FLAREmispService.tar has been created with the following content:
tar -tvf ./FLAREmispService.tar
