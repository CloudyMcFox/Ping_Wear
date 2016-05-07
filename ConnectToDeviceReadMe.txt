adb forward tcp:4444 localabstract:/adb-hub
adb connect 127.0.0.1:4444


To connect real phone to emulator watch
On phone:
Turn on developer mode and usb debugging
Run: 	
adb -d forward tcp:5601 tcp:5601
On phone:
open andoroid wear and connect to emulator

