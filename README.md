When running the azure functions in a geo-replicated setting, the sh files should be changed to add a variable with the name BlobStoreConnectionRemote with the value being the connection to the blob store of the other region

We also created several pom files to deploy the app and the functions in two regions (WestEurope and WestUS)