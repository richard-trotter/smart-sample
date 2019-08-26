### Introduction

This is a sample SMART on FHIR app, demonstrating OAuth2 authorization. The code itself is derived from a publicly available sample.

	https://docs.smarthealthit.org/tutorials/authorization/

The app is intended to be launched from the EPIC sandbox LaunchPad:

	https://open.epic.com/Launchpad/Oauth2Sso

### Build

Build the app distribution archive.

	cd /Users/rtrotter/Documents/GitHub/smart-sample/rht.samples.smart.one  
	./gradlew clean build dist

### Test in a local Docker container

Extract the app archive.

	rm -rf tmp; mkdir tmp; cd tmp  
	tar -xzvf ../build/dist/smart-one-svc*z  
	cd smart-one-svc  

Build and run the docker image, directing docker to expose the port identified in the Dockerfile.

	docker build -t smart-one-svc .  
	docker run smart-one-svc  
	docker run -P smart-one-svc  

Get the container ID assigned by Docker and determine the external port number selected by Docker, and verify connection. 
   
	docker ps  
	docker port 22aceebb7f0b  
	curl http://127.0.0.1:32768/samples/launch.html  

Stop the docker container. 

	docker stop 22aceebb7f0b  

### Test in IBM Cloud Kubernetes cluster

Create a Kubernetes cluster

	https://cloud.ibm.com/kubernetes/clusters

Login to to IBM Cloud and list clusters
	
	$ ibmcloud login --sso
	$ ibmcloud ks clusters
	
	OK
	Name        ID                     State    Created      Workers   Location   Version       Resource Group Name   Provider   
	mycluster   blbgg54d0rqlgvrkfeu0   normal   3 days ago   1         hou02      1.13.9_1532   default               classic   

Setup the local env to access this IBM Cloud cluster

	$ ibmcloud ks cluster-config --cluster mycluster
	
	OK
	The configuration for mycluster was downloaded successfully.
	
	Export environment variables to start using Kubernetes.
	
	export KUBECONFIG=/Users/rtrotter/.bluemix/plugins/container-service/clusters/mycluster/kube-config-hou02-mycluster.yml

As indicated, set the KUBECONFIG env var

	$ export KUBECONFIG=/Users/rtrotter/.bluemix/plugins/container-service/clusters/mycluster/kube-config-hou02-mycluster.yml

Verify KUBECONFIG.

	$ kubectl get nodes
	NAME            STATUS   ROLES    AGE     VERSION
	10.76.202.202   Ready    <none>   3d17h   v1.13.8+IKS
	
If necessary, create a IBM Cloud container registry.  
See: https://cloud.ibm.com/docs/services/Registry?topic=registry-getting-started#gs_registry_namespace_add

Login to the IBM Cloud container registry.

	$ ibmcloud cr login 	
	$ ibmcloud cr namespace-list
	
	Listing namespaces for account 'Rick Trotter's Account' in registry 'us.icr.io'...
	
	Namespace   
	ns-rtrotter   

Build the sample app docker image, and push the image to this IBM Cloud container registry, using the registry name and namespace name identified from above.

	docker build -t us.icr.io/ns-rtrotter/smart-one-svc:latest .
	docker push us.icr.io/ns-rtrotter/smart-one-svc:latest

Verify that the image was pushed successfully by running the following command. Note that IBM Cloud runs a security scan on docker images pushed to the registry, and this may take several minutes. Wait for 'SECURITY STATUS' to no longer show 'Scanning...'.

	$ ibmcloud cr images
	
	REPOSITORY                            TAG      DIGEST         NAMESPACE     CREATED        SIZE     SECURITY STATUS   
	us.icr.io/ns-rtrotter/smart-one-svc   latest   2d31fdd20206   ns-rtrotter   19 hours ago   100 MB   No Issues   

Deploy this image to the cluster

	apply -f smart-one-svc.yml --grace-period=1 --force=true
  
Query the deployed service. Note that there is no 'EXTERNAL-IP'. 

	$ kubectl get service -o wide smart-one-svc
	NAME            TYPE       CLUSTER-IP      EXTERNAL-IP   PORT(S)          AGE     SELECTOR
	smart-one-svc   NodePort   172.21.191.90   <none>        8081:30081/TCP   7m29s   app=smart-one-svc
  
Note that the deployment pod spec template specifies a pod selector of "app=smart-one-svc". 

	selector:
	    matchLabels:
	      app: smart-one-svc

Query the pods created for this service, using the pod selector label, and note the 'NODE' value.

	$ kubectl get pods -o wide -l app=smart-one-svc
	NAME                                        READY   STATUS    RESTARTS   AGE   IP            NODE         
	smart-one-svc-deployment-86c6665fd5-dtznp   1/1     Running   0          15m   172.30.30.7   10.76.202.202
  
Query for the 'EXTERNAL-IP' of this node.
   
	$ kubectl get nodes -o wide 10.76.202.202
	NAME            STATUS   ROLES    AGE     VERSION       INTERNAL-IP     EXTERNAL-IP     OS-IMAGE          
	10.76.202.202   Ready    <none>   3d21h   v1.13.8+IKS   10.76.202.202   173.193.82.54   Ubuntu 16.04.6 LTS

Now verify access to the app "launch" page. 

	$ curl http://173.193.82.54:30081/samples/launch.html | head -4
	
	<!DOCTYPE html>
	<html>
	    <head>
	        <title>Simple SMART Auth App - Launch</title>
	

