# myke-bike-service
This service provides the back-end for the _My Bike_ (Myke) sample project. The service is a light weight HTTP server built using the _vertx_ (https://vertx.io/) framework. Vertx makes it easy to leverage reactive programming techniques and non-blocking IO combined with very low resource footprint.   
As a result, the final service would be well suited to be containerized/orchestrated in a micro-services environment.

Myke also provides a react UI that you will find here: https://github.com/DieBue/myke-ui .

# Install
## Prereqs
- You need to have Java JDK verion 8 or higher installed on your machine
- Internet access
- Access to a Acoustic Content API key 

## Install steps 
* Clone or download the repo. As a result you will have a `/myke-bike-service` folder containing all project files.
* Locate and and edit the file `/myke-bike-service/myke-config-json` (you can copy this file to your user home directory if you like)	  
	* The file already contains data for the `host` and `tenantId` properties.  
	* Copy your Acoustic Content API key to the `apiKey` property in the file and save it.  
* Open command line in `/myke-bike-service` folder (I will assume a windows cmd shell for this documentation)
* run `gradlew clean build buildDist` 
	* This will build the code and run the test cases. You should get a `BUILD SUCCESSFUL` message at the end. 
	* Please be aware, this step requires a Internet connection and a valid Acoustic Content API key being configured in the `myke-config.json` file.
* run `java -Dvertx.disableDnsResolver=true -jar dist\myke-3.8.4.jar`
	* This will start the server. You should get a `Service ready. Listening on port: 3001` message at the end. 
   
## Try the server
When the server is running, you should see the REST API documentation being served at `http://localhost:3001/swagger/`. You can invoke the APIs using the
`Try it out` button as usual.
The test cases can be executed either using `gradlew test`. 

## Try the UI
Leave the service running and install the Myke UI as documented here https://github.com/DieBue/myke-ui.      