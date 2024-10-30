# Inter Profile Sharing

This is a purposefully simple, native App that allows forwarding media and text to another Android User Profile as easily as sharing it with another App.

No complicated setup. No fancy hacks. Well-established dependencies. Free Open Source Code.

Although this App will run on any Android 14+ phone, it was designed and tested for GrapheneOS which comes with various [**improvements to Android's User Profiles**](https://grapheneos.org/features#improved-user-profiles). They allow isolating Apps from each other such that users are able to keep sensitive Applications separate from less trustworthy ones. Although GrapheneOS encourages their use, so much so that they raised the limit from 4 to 32 profiles per device, actually using them can be quite inconvenient. This App attempts to alleviate the arguably biggest pain point: The isolation of the file system between profiles, which requires less than optimal methods to bypass such as using USB Sticks, 3rd party Cloud Synchronization or Messaging Apps to move files between profiles.

## Usage

If you haven't, allow *multiple users* on your device by going into *Settings &raquo; System &raquo; Users*. You could create separate profiles for various types of Apps: Social Media, Encrypted Messengers, Banking, and maybe one for the bullshit App your gym forces you to have. Note though that switching between profiles can take several seconds. It's more convenient to have a profile containing all the Apps you often use during the day, and separate the others into different profiles.

Install this App within each profile that you want to share data with (no way around this, sorry). If you begin by installing this App within the Owner profile, you can easily install it into other profiles via *Install available apps* when editing a User.

### Permissions

* **Network**: When installing new applications on GrapheneOS, you'll be asked whether to allow it the *Network* permission. Although this App never connects to the Internet, it still requires this permission as its inter-profile communication depends on connecting over localhost (127.0.0.1).
* **Notifications**: This App makes use of notifications to inform you about information being shared by another User Profile. In fact, there's no UI from which you could obtain shared information other than notifications. This is why they're required. The App will never post any spam notifications unrelated to the information you're sharing.
* **Foreground Service**: The App will automatically obtain this permission on installation, allowing it to serve shared information without getting killed by the system to save battery. Foreground Services are only used by this App while data is explicitly being shared. The user has full control over the running Service via a pinned notification.
* **Clipboard Write**: The App will automatically obtain this permission on installation, allowing it to write to the clipboard. In fact it even reads your clipboard, but it only ever does so when you click buttons explicitly stating this as a fact. This is used to conveniently share clipboard contents between User Profiles.

### Encryption

You can instantly start sharing files between profiles after installation, but it's recommended to turn on encryption within the App's settings. This requires setting **the same** *Sharing Password* within the App **in each profile**. Once configured, other applications running on your phone will not be able to access any of the data your sharing.

This feature is arguably paranoid. For another application to access information shared through this App, it would need to be explicitly programmed to do so. Enabling encryption will make data transfers slower, possibly less reliable, but it will certainly prevent such malicious Apps from accessing what you're sharing without your permission. Assuming you chose a nice, long password, that is.

## Troubleshooting

#### I'm sharing something but I get no notification about it in the other profile

Often the background service checking for shared information from other profiles gets stopped by the system to save battery. In order to trigger the check manually, open the *Inter Profile Sharing* App and wait a second, or two.

Make sure that the sharing service actually keeps running after switching to another User Profile. If the *"something is being shared"* notification disappears after switching away and back, it might be that the user you're sharing from is not allowed to keep running in the background. You can change this within the user's settings at *Settings &raquo; System &raquo; Users*.

Note that the App can only communicate with itself in another profile, if they are both configured to use the same *Server Port* and, if you're using encryption, the same *Shared Password*.

#### The file I'm attempting to share keeps failing to download in the other profile

Most likely this happens because your file is very large. If you're using encryption, it might be worth temporarily disabling it for transferring large files.

#### When I attempt to share something, the notification about it being shared immediately disappears again

This is most likely happening because it's aborting, due to a lack of permission to use notifications.

Another reason may be that the port is currently blocked by something else. When a new share is requested, but the port is already taken, the App sends a shutdown request to the sharing service of that other profile. This usually prevents forgotten shares from blocking new ones.

In the most rare cases it might be that the default port *2411* is being used by something else. In that case you may set a custom port within the App's settings. Note that this port needs to be set within all instances of this App, ie. in all profiles.

## Technical Details

Source files are located at `/app/src/main/java/digital/ventral/ips`:

* [**MainActivity.kt**](https://github.com/VentralDigital/InterProfileSharing/blob/main/app/src/main/java/digital/ventral/ips/MainActivity.kt) contains the UI shown when the App is opened.

The application supports sharing two basic types of items: Arbitrary files (*FILE* type) and short strings (*TEXT* type) such as clipboard contents or URLs. These are sourced either directly from the App's UI (buttons for picking a file, or copying from clipboard) or via Intents sent to the App from other applications (for example by sharing photos from the Gallery App or URLs from the Browser).

Upon receiving such a request to share information, the *MainActivity* invokes the *ServerService* which begins to listen for connection on the configured TCP Port.

* [**SettingsActivity.kt**](https://github.com/VentralDigital/InterProfileSharing/blob/main/app/src/main/java/digital/ventral/ips/SettingsActivity.kt) contains the UI shown when navigating to the App's settings.

Upon setting a *Shared Password* the *SettingsActivity* passes it to *EncryptionUtils* to derive an encryption key from it. The cleartext password is never actually stored. The settings user interface is build based on the specifications of the [*root_preferences.xml*](https://github.com/VentralDigital/InterProfileSharing/blob/main/app/src/main/res/xml/root_preferences.xml) file. 

* [**EncryptionUtils.kt**](https://github.com/VentralDigital/InterProfileSharing/blob/main/app/src/main/java/digital/ventral/ips/EncryptionUtils.kt) contains logic for creating and storing the encryption key derived from the *Shared Password*. It also contains the logic responsible for encrypting the inter-profile communication of this App.

The encryption key itself is stored within the App's preferences in an encrypted manner, mostly to prevent leaks when the device is rooted. The key is derived from the *Shared Password* string using hashing: `key = sha256(sha256(password) + DOMAIN_SEPARATOR)`.

The encryption itself is of type AES-GCM. It's mostly intended for the purpose of authentication, but it also prevents eavesdroppers from gaining any information from the traffic. Google's Tink library is used for efficient, hardware-accelerated encryption.

* [**ServerService**](https://github.com/VentralDigital/InterProfileSharing/blob/main/app/src/main/java/digital/ventral/ips/ServerService.kt) can also be referred to as the *sharing service* as it contains the logic actively sharing information from the profile its running in to other profiles.

The *ServerService* is intended to only be running while information is explicitly being shared, but during that, it runs as a foreground service to prevent the system from killing it to save battery. When the service boots it first checks whether the configured TCP port is available, and if not, it immediately sends a STOP_SHARING request to whatever is blocking it, in an attempt to obtain the port. This normally succeeds.

The *ServerService* then begins listening on the configured port for connections. In order to avoid the bloat of an HTTP server, the request-response based communication simply uses JSON encoded strings which are framed by newline characters. Currently 3 actions, or "methods" are supported by the server:

1. `SHARES_SINCE`: Sent by the client to check for new shared items. Each item contains the timestamp of when it was requested to be shared by the user. This timestamp is used during these requests to filter out items already seen by the client. Response is a JSON encoded array.
1. `FETCH_FILE`: Sent by the client to fetch the contents of a shared file. Response are the raw contents of the file.
1. `STOP_SHARING`: Sent by another server to request shutting down and freeing the port. Items shared by the server receiving this will become unavailable. No response.

As a foreground service, *ServerService* is required to maintain an active notification in order to prevent getting killed by the system. The service can be stopped either by swiping away this notification, or by explicitly tapping on it's *"Stop Sharing"* action button.

* [**ClientService**](https://github.com/VentralDigital/InterProfileSharing/blob/main/app/src/main/java/digital/ventral/ips/ClientService.kt) can also be referred to as *polling service* as it contains the logic that checks whether there's a *ServerService* running on the configured port and fetches lists and contents of shared items.

Checks for new items are skipped when (1) a check already was already triggered before within the same second, (2) the App is lacking the permission to post notifications, (3) the *ServerService* is running within the same profile, (4) it is unable to connect to the configured port.

Most of the other client logic is attempting to create pretty notifications for the information shared by another profile, and handling user interactions with these notifications.

If the user desires to do something with a shared file, it will always first be added to the Downloads folder. The App does not make use of any temporary directories or caches.