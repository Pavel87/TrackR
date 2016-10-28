#TrackR
TrackR application offers GPS tracking functionality between two android devices. 

App can run in two modes:
- Location Transmitter
- Location Receiver

![Main Screen](https://github.com/Pavel87/TrackR/tree/master/screenshots/1.png)

TrackR Features:
- Last Known Location: Coordinates, Time of last update, Address (if available)
- Shows last location in map
- App watches for internet connection to download/upload last location
- Reduces the battery consumption once the battery hits 30 % by increasing times between updates
- Parental Lock which can lock the settings so user cannot switch off the tracking mode unless the user knows the password
- Supports Phones & Tablets
- API(16)+ (Jelly Bean+)

<p align="center">
  <img src="https://github.com/Pavel87/TrackR/tree/master/screenshots/5.png" width="350"/>
  <img src="https://github.com/Pavel87/TrackR/tree/master/screenshots/3.png" width="350"/>
</p>

#Get Started:

- Launch the app and go to Settings
- Set the unique tracking ID in "remote" device.
- Switch ON tracking mode
- You can enable parental lock by taping on padlock icon in bottom right corner.

Device starts transmitting its location once the tracking mode is ON every 30 minutes or 1 hour on lower battery levels if there is connectivity to internet

- Set up the Receiving device - Launch TrackR and go to Settings
- Configure Receiving ID which corresponds to Tracking ID from remote device
- Now you are all set and Receiving Device will get last known location of the other android device in given intervals

App has English and Czech localization


#Download from Play Store:
https://play.google.com/store/apps/details?id=com.pacmac.trackr
