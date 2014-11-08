Gear2cam
========
Gear2cam is an app that let's you use a smart watch as a view finder and remote trigger for your phone's camera.

Currently only the Samsung's Gear watches are supported with an Android phone.

-------------
Usage Scenarios
---------------

Gear2cam is ideal for wildlife photogrpahy where the phone can be left on a tripod close to a place where animals might be spotted. When an animal approches, the smart watch can be used to compose the picture and shoot.

Gear2cam may also be used while taking group shots where a phone is placed at a distance and then the watch is used to confirm everyone is in the frame.

More scenarios are descibed on the product web page:

[http://www.gear2cam.com](http://www.gear2cam.com)

-------------
Setting Up
----------

To compile this app you need Android Studio and Tizen IDE.

There are are two parts to this app.

1. Android App - This contains the a minimal UI and a service running on the phone that allows communication between the app and the watch. The app also contains the compiled widget which will be installed on the watch when a user installs the Android app.
2. Tizen Widget - This component contains the UI elements for the watch app as well as the javascript code that communicates various commands from the watch to the service running on the phone. Once this component is compiled to a .wgt file, it must be placed in the assets folder of the Android app.


### Parse Setup
Gear2cam uses Parse for push notifications and analytics. You will need to create your own Parse account and then substitute your application_id and client_key in Gear2camApplication.java

### Facebook Setup
Gear2cam also allows users to (optionally) login through Facebook and then later post photos to Facebook directly from the watch. Create your Facebook app at:

[http://developers.facebook.com/apps](http://developers.facebook.com/apps)

Then change /res/values/strings/app_id to the application id obtained after registering your Facebook app. You can easily Google on how to integrate a Facebook app in Android using the Facebook SDK. You will also need to register your development key hashes for the app as is described [here](https://developers.facebook.com/docs/android/getting-started)

### Running widget on a Gear device
Please see the following article to get you going:

[http://denvycom.com/blog/step-by-step-guide-to-build-your-first-samsung-gear2-app-tizen/](http://denvycom.com/blog/step-by-step-guide-to-build-your-first-samsung-gear2-app-tizen/)

-----------

License
--------
The MIT License (MIT)

Copyright (c) 2014 Varun Chatterji

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.