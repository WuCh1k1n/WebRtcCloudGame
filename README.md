# WebRtcCloudGame
This is a cloud game demo based on WebRTC.

## WebRTC Live Streaming

It is designed to demonstrate WebRTC screen share between androids and/or other clients.
![Cloud game architecture](https://upload-images.jianshu.io/upload_images/6640927-931a263f8951c8e5.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

## How To
You need [ProjectRTC](https://github.com/pchab/ProjectRTC) up and running, and it must be somewhere that your android can access. (You can quickly test this with your android browser). Modify the host string (in res/values/strings.xml) to the server IP.


Your stream should appear as "cloud_phone_stream" in ProjectRTC, so you can also use the view feature there.
