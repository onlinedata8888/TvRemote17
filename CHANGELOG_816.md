# Remote816
- Bluetooth PC mouse fix: PC mode (Mouse page, page-1 pad, Trackpad page, Devices button) now always switches the saved Bluetooth profile back to Mouse+Keyboard. Before, choosing "Pad only" on the Game pad page left the app stuck in pad-only, so the PC cursor never moved.

## 17.1
- Fix: first-time (new) D-pad — Down press no longer hits the bottom scroll bar (`.navmode2 .wheel-h::after`).
- New: Settings > Quick controls: Floating button + Notification buttons (Up/Down/Left/Right/OK/Back). Native: FloatService, NotifControl, KeyReceiver, TvBridge.floatSet/notifSet; manifest/layout added in patch/patch_apk.py.
