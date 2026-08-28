# D&D 5e Dice Lab Android Build

Android WebView wrapper for the single-file D&D 5e Dice Lab (2014 rules).

- Offline HTML app bundled in `assets/index.html` at build time.
- Portrait and landscape are both enabled.
- Rotation preserves the active WebView/session instead of recreating the activity.
- Touch, mouse, keyboard, and hardware game-style pointer input are handled by the web app.

The GitHub Actions workflow decodes the compressed HTML payload and builds a debug APK artifact.
