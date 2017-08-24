# CameraBroadcast fork  

## Rtmp link edit  
See private String ffmpeg_link, please correct IP and port.  

## Rtmp server  
* See nginx-rtmp-win32
https://github.com/illuspas/nginx-rtmp-win32  
* My clone is here  
https://github.com/weimingtom/nginx-rtmp-win32  
**Notice port 1935 and 8080 is used, see nginx.conf**   
> rtmp {
>     server {
>         listen 1935;
> 
>         application live {
>             live on;
>         }

## Push to nginx-rtmp-win32 from pc screen  
* Use Open Broadcaster Software / OBS (https://obsproject.com/download)  
* See obs_usage.txt  
* 菜单->设定->广播设定->FMS URL: rtmp://127.0.0.1:1935/live  

## Push to nginx-rtmp-win32 from ffmpeg  
* download ffmpeg for win32  
http://www.ffmpeg.org/download.html  
http://ffmpeg.zeranoe.com/builds/   
* Execute command line to push test.mp4 to rtmp server    
see http://stackoverflow.com/questions/29018606/android-stream-camera-as-rtmp-stream  
ffmpeg -re -i ../test.mp4 -vcodec h264 -ar 44100 -f flv rtmp://localhost:1935/live/stream  

## Use nginx-rtmp-win32 flash client  
* Open http://127.0.0.1:8080/, and change address to rtmp://127.0.0.1:1935/live/stream    
* https://github.com/illuspas/nginx-rtmp-win32  
* https://github.com/NodeMedia/NodeMediaDevClient  
