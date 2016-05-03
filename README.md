AmstelBrightServer
==================

This project is essentially all of the Android Services that need to be
running at all times on an OG box:

- UDP Beacon
- HTTP Server for the HTML/JS apps
- Bluetooth iBeacon and Eddystone
- Audio streamer

The parent service is "AmstelBrightService" and it starts all the child
services. The Handler/Messenger pattern is used for inter-server and
inter-app communications so that it will scale to the UI app talking to
it while running in a separate process.