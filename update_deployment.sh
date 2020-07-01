#/bin/sh

cd /Users/rtrotter/Documents/GitHub/smart-sample/rht.samples.smart.one

./gradlew clean build dist
rm -rf tmp; mkdir tmp; cd tmp
tar -zxf ../build/dist/smart*z

cd smart-one-svc/
docker build -t us.icr.io/ns-rtrotter/smart-one-svc:latest .

#ibmcloud ks cluster-config --cluster mycluster
docker push us.icr.io/ns-rtrotter/smart-one-svc:latest

#ic cr images
kubectl apply -f smart-one-svc.yml

kubectl get service -o wide smart-one-svc
kubectl get pods -o wide -l app=smart-one-svc
kubectl get nodes -o wide 10.76.202.202

curl http://173.193.82.54:30081/samples/launch.html | head -4


