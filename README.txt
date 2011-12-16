FixMyStreet Android application
==============================

The Fix My Street directory contains a complete application that should open
directly in Eclipse.

However, to compile it as an .apk, you will need to add the following JAR file
to your build path - it makes the multipart messages work.

- httpmime-4.1.1.jar

This file is a part of HttpClient 4.1.1 from http://hc.apache.org/downloads.cgi

Hopefully it will no longer be needed in future versions of Android.

I've compiled it against version 1.6.

Note that the app has to be signed to go onto the Android Market
Contact anna@mysociety.org for signing a new british version
Contact hildenae@gmail.com for a new norwegian version