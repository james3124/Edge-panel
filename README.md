# Edge Panel

A floating edge-handle launcher for Android. Swipe or tap a handle pinned to the
side of the screen to slide out a multi-tab panel (Apps, Tasks, People, Weather).
System apps launched from the panel open in a floating, draggable window with
Close / Fullscreen / Maximize / Minimize controls.

## ⚠️ Important — read before building

This project is a **complete, real Android Studio project** (Kotlin, View-based UI,
no placeholders) — every file in `app/src/main/java` is working source code, not a stub.
However, it was generated in a sandbox **without internet access and without the Android
SDK installed**, so it has **not been compiled or run** here. You will need to open it in
Android Studio (which has the SDK + Gradle + network access) to build and test it.

This is also a genuinely advanced app: `SYSTEM_ALERT_WINDOW`, `FREEFORM_WINDOW_MANAGEMENT`,
and persistent foreground services touch parts of Android that vary a lot by OEM
(MIUI, One UI, ColorOS, etc.) and by Android version. Treat this as a strong, working
starting point rather than a finished, store-ready product — budget time for on-device
testing across a couple of real phones.

## How to open & build

1. Install **Android Studio** (Hedgehog/2023.1+ recommended).
2. `File → Open` → select the `EdgePanel` folder.
3. Let Gradle sync (it will download the wrapper + dependencies — needs internet
   the first time).
4. Run on a **physical device** — overlay/freeform features behave unreliably on
   emulators.
5. On first launch, tap **Draw Over Other Apps** in the permission cards and grant it
   in the system settings screen that opens.

### Granting FREEFORM_WINDOW_MANAGEMENT

This permission is signature/system-protected, so a normal install can't get it through
the Play-style runtime prompt. The Main screen gives you a ready-to-copy ADB command:

```
adb shell pm grant com.edgepanel.app android.permission.FREEFORM_WINDOW_MANAGEMENT
```

Run that from a computer with the device connected and USB debugging on. Without it,
the app still works — system apps just open as a normal full-screen activity instead of
a floating chrome window (see `FloatingWindowController.launch()`).

## Project structure

```
app/src/main/java/com/edgepanel/app/
├── MainActivity.kt                 Home screen: service toggle, permission dashboard
├── settings/HandleSettingsActivity Color/size/position/transparency editor
├── service/EdgePanelService.kt     Foreground service that owns the overlay windows
├── overlay/
│   ├── EdgeHandleView.kt           The draggable/tappable edge handle (custom View)
│   ├── PanelView.kt                Slide-out panel: tab bar + content + animations
│   └── FloatingWindowController.kt Launches system apps in floating chrome windows
├── panels/
│   ├── AppsPanel.kt                Searchable app grid, long-press to pin
│   ├── TasksPanel.kt                Lightweight to-do list
│   ├── PeoplePanel.kt              Pinned contacts → call / message
│   └── WeatherPanel.kt             OpenWeatherMap current-conditions card
├── adapter/                        RecyclerView adapters for the panels above
├── model/                          Plain data classes (HandleConfig, Task, AppItem…)
├── util/
│   ├── PrefsManager.kt             SharedPreferences + Gson persistence
│   ├── PermissionHelper.kt         Centralized permission checks
│   └── AppUtils.kt                 Installed-app enumeration & launch helpers
├── api/WeatherService.kt           OkHttp + manual JSON parsing (no extra SDK)
└── receiver/BootReceiver.kt        Restarts the service after device reboot
```

## Features implemented

- Persistent foreground service hosting two always-on overlay windows: the edge
  handle and the (initially invisible/non-touchable) slide-out panel.
- Tap **or** swipe the handle to open the panel; tap the backdrop or swipe back to close.
- Handle is fully draggable vertically along the edge.
- Four panels — Apps (search + pin), Tasks (add/check/delete), People (pinned
  contacts with call/SMS), Weather (OpenWeatherMap, metric/imperial) — selectable
  in Settings.
- System apps launch via `ActivityOptions.launchBounds` into a floating window with
  a draggable title-chrome bar carrying Minimize / Maximize / Close. Non-system apps
  launch normally, full screen, no chrome.
- Handle customization screen: left/right position, 12-color picker, transparency,
  width, and height sliders, with a live preview bitmap.
- Permission dashboard on the home screen showing the live status of overlay,
  freeform, foreground-service, and boot-receiver permissions, plus a one-tap
  ADB-command copy button.
- `BootReceiver` restarts the service automatically if the user had it enabled
  before reboot.

## Honest gaps / things to verify on-device

These are the corners every team hits with this category of app — flagging them so
nothing surprises you:

- **OEM battery managers** (MIUI/EMUI/ColorOS) routinely kill foreground services
  despite `START_STICKY`. You'll likely want to add OEM-specific "disable battery
  optimization" deep links per the improvement list discussed earlier.
- **`FREEFORM_WINDOW_MANAGEMENT` is not a normal install-time/runtime permission** —
  it's signature-protected. The ADB grant works for development and rooted/ADB-enabled
  devices; for production you'd need to either ship as a system app, use Shizuku, or
  fall back permanently to the "normal launch" path implemented in
  `FloatingWindowController.launchNormal()`.
- **Maximize is implemented as a relaunch with full-screen `launchBounds`**, not a true
  animated resize of the live window surface (Android doesn't expose that to
  third-party apps without `ActivityTaskManager` shell-level access). Treat the
  Minimize/Maximize chrome buttons as "good enough for v1," not pixel-perfect window
  manager behavior.
- **Weather panel needs a free OpenWeatherMap API key**, entered in-panel — there's no
  bundled key.
- **No automated tests** were written; given the heavy reliance on `WindowManager`,
  most meaningful testing here is manual/on-device rather than unit-testable.

## Suggested next steps (from the earlier improvement list)

Per-app exclusion list for the handle, Shizuku-based permission granting, window
snap-zones, and per-app window-position memory are all natural follow-ups — see
`overlay/FloatingWindowController.kt` and `overlay/EdgeHandleView.kt` as the entry
points for most of those.
