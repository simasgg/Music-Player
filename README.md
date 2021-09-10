# Music-Player-Android-app

Android app made with Kotlin programming language. It's a music player which allows user to play/pause/stop music (mp3 files), as well as, it is also implemented as a foreground service 
using notification commands (the music keeps playing even when the screen is locked or another app is activated). Additionaly, play and pause commands can be used with motion sensing (SensorEventListener interface). To enable motion sensing, press G-on, to disable press G-off.
When horizontal motion is detected, app calls Pause; when vertical motion is detected, app calls Play; idle state does nothing. It also implements ProgressBar and track title and track duration
are updated (both on our mainActivity and foreground notification) every second with the help of Coroutines. On pressed Play button it plays a random track.

<img src="https://github.com/simasgg/Music-Player-Android-app/blob/master/app1.jpg" align="left" width="300" height="500">
<img src="https://github.com/simasgg/Music-Player-Android-app/blob/master/app2.jpg" align="left" width="300" height="500">
<img src="https://github.com/simasgg/Music-Player-Android-app/blob/master/app3.jpg" align="left" width="300" height="500">
