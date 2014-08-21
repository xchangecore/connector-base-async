connector-base-async
====================

This is an example of a full XchangeCore incident lifecycle, creation of a Resource Profile and Resource Instance,
processing of notifications, and processing asynchronous responses from the core.

Dependencies:
connector-base-util

To Build:
1. Build all the dependencies
2. Run "mvn clean install" in the java directory of the client code to
   build com.saic.uicds.client.async.
3. Run "mvn clean install -Dmaven.test.skip=true" to skip the tests which need a running XchangeCore to pass.

