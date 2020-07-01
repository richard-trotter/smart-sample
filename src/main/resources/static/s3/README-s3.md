### Introduction

This is a sample SMART on FHIR app, demonstrating OAuth2 authorization with the SMART on FHIR JavaScript Library.

	http://docs.smarthealthit.org/client-js
	
The app makes two FHIR server queries: one to get the context Patient resource, and another to get that Patient's MedicationOrders. Note that some sample patients may not have a very interesting set of MedicationOrder. 

The app is intended to be launched from the EPIC sandbox LaunchPad:

	https://open.epic.com/Launchpad/Oauth2Sso

