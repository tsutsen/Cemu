# Bar Overlay Feature - Refactor Report

## Overview
The bar overlay feature renders custom images on the black letterbox bars (top and bottom) when a 16:9 game stream is displayed on a 4:3 screen, specifically targeting the external DRC/GamePad screen.

## Files Changed
- `src/Cafe/GameProfile/GameProfile.h` - Added BarOverlay settings members
- `src/Cafe/GameProfile/GameProfile.cpp` - Added BarOverlay section load/save
- `src/android/app/src/main/cpp/NativeGameTitles.cpp` - Added JNI functions for bar overlay settings
- `src/android/app/src/main/java/info/cemu/cemu/nativeinterface/NativeGameTitles.kt` - Added Kotlin externals
- `src/android/app/src/main/java/info/cemu/cemu/common/settings/Settings.kt` - Added BarOverlaySettings data class
- `src/android/app/src/main/java/info/cemu/cemu/emulation/BarOverlay.kt` - Composable for main screen overlay (unused)
- `src/android/app/src/main/java/info/cemu/cemu/emulation/BarOverlayView.kt` - Native View for DRC overlay
- `src/android/app/src/main/java/info/cemu/cemu/emulation/PadPresentation.kt` - Integrated BarOverlayView
- `src/android/app/src/main/java/info/cemu/cemu/emulation/EmulationScreen.kt` - UI integration
- `src/android/app/src/main/java/info/cemu/cemu/emulation/EmulationViewModel.kt` - Settings management
- `src/android/app/src/main/java/info/cemu/cemu/nativeinterface/NativeEmulation.kt` - Added getTitleIdFromPath
- `src/android/app/src/main/cpp/NativeEmulation.cpp` - Added getTitleIdFromPath native implementation
- `src/android/app/src/main/java/info/cemu/cemu/emulation/EmulationActivity.kt` - Title ID passing

---

## Refactor Opportunities

### 1. 🔴 CRITICAL: Duplicate Bitmap Loading Logic

**Problem:** Bitmap loading logic is duplicated across three files:
- `BarOverlay.kt` - `rememberBarOverlayBitmap()` composable
- `BarOverlayView.kt` - `loadBitmap()` method
- `EmulationScreen.kt` - File picker copy logic

**Impact:** Code duplication, maintenance burden, inconsistent behavior

**Recommendation:** Create a shared `BitmapLoader` utility class:
```kotlin
object BitmapLoader {
    fun loadFromPath(context: Context, path: String?): Bitmap?
    fun copyFromUriToInternal(context: Context, uri: Uri): String?
}
```

**Priority:** High - Reduces ~100 lines of duplicate code

---

### 2. 🟡 HIGH: Debug Logging in Production Code

**Problem:** Multiple `Log.d()` statements remain in production code:
- `BarOverlayView.kt` - `setSettings()` has 4 log statements
- `BarOverlay.kt` - `log()` function called throughout
- `EmulationViewModel.kt` - `saveBarOverlaySettings()` has 3 log statements

**Impact:** Performance overhead, information leakage, cluttered logs

**Recommendation:** 
- Replace with a `DEBUG_BUILD` flag that disables logging in release builds
- Or use Android's `BuildConfig.DEBUG` to conditionally compile logs

**Priority:** Medium-High - Already partially addressed in onDraw

---

### 3. 🟡 HIGH: Unused BarOverlay Composable

**Problem:** `BarOverlay.kt` contains a Composable that renders overlay on the main screen, but this is not used. The actual overlay is rendered via `BarOverlayView` in `PadPresentation`.

**Impact:** Dead code, confusion about which overlay is active

**Recommendation:** 
- Option A: Remove `BarOverlay.kt` entirely if not needed
- Option B: Use it for the main screen overlay and keep `BarOverlayView` for DRC

**Priority:** Medium - Clarifies architecture

---

### 4. 🟡 MEDIUM: Hardcoded Aspect Ratio

**Problem:** The 16:9 aspect ratio is hardcoded in multiple places:
- `BarOverlayView.kt` - `val gameAspectRatio = 16f / 9f`
- `EmulationScreen.kt` - Same calculation

**Impact:** Difficult to support other aspect ratios or configurable ratios

**Recommendation:** 
- Define as a constant: `const val GAME_ASPECT_RATIO = 16f / 9f`
- Or load from settings/configuration
- Consider making it configurable per-game

**Priority:** Low-Medium - Works for now, but limits flexibility

---

### 5. 🟡 MEDIUM: Inconsistent Error Handling

**Problem:** Error handling varies across the codebase:
- `BarOverlayView.kt` - Returns `null` on error, logs warning
- `EmulationScreen.kt` - Shows snackbar on failure
- `loadBitmap()` - Catches all exceptions generically

**Impact:** Inconsistent user experience, hard to debug issues

**Recommendation:** 
- Create a sealed result type: `sealed interface BitmapLoadResult { object Success : Result; data class Error(val message: String) : Result }`
- Centralize error handling in a helper function
- Provide consistent user feedback

**Priority:** Medium - Improves robustness

---

### 6. 🟢 LOW: Magic Numbers

**Problem:** Several magic numbers scattered throughout:
- `1` pixel shrinkage in `BarOverlayView.kt`
- `16f / 9f` aspect ratio
- Various padding values in Compose UI

**Impact:** Hard to understand intent, difficult to tweak

**Recommendation:** 
- Define constants: `const val HOLE_SHRINK_PX = 1`
- Document why each magic number exists

**Priority:** Low - Cosmetic improvement

---

### 7. 🟢 LOW: Settings Persistence Race Condition

**Problem:** Settings are saved asynchronously via `viewModelScope.launch`, but the UI updates immediately. If the save fails, the UI shows stale data.

**Impact:** User sees settings that aren't actually saved

**Recommendation:** 
- Use `StateFlow` with `catch` operator to handle errors
- Show loading/disabled state during save
- Consider using a repository pattern for settings persistence

**Priority:** Low - Edge case, but good practice

---

### 8. 🟢 LOW: No Unit Tests

**Problem:** No unit tests for:
- `calculateBarPositions()` logic
- `BitmapLoader` functionality
- Settings serialization/deserialization

**Impact:** Regressions hard to catch

**Recommendation:** 
- Add tests for `calculateBarPositions()` with various screen sizes
- Test `BitmapLoader` with mock URIs
- Test settings persistence round-trip

**Priority:** Low - Nice to have

---

### 9. 🟢 LOW: C++ Code Style

**Problem:** Inconsistent C++ code style in `GameProfile.cpp`:
- Mixed indentation (tabs vs spaces)
- Inconsistent formatting of `fmt::format` calls
- Some lines exceed 120 characters

**Impact:** Harder to maintain, doesn't match project style guide

**Recommendation:** 
- Run `clang-format` on changed files
- Follow existing C++ style conventions

**Priority:** Low - Style only

---

## Summary

| Priority | Count | Description |
|----------|-------|-------------|
| 🔴 Critical | 1 | Duplicate bitmap loading logic |
| 🟡 High | 2 | Debug logging, unused composable |
| 🟡 Medium | 2 | Hardcoded aspect ratio, error handling |
| 🟢 Low | 4 | Magic numbers, race condition, tests, C++ style |

**Total Refactor Items:** 9

## Recommended Next Steps

1. **Immediate:** Create shared `BitmapLoader` utility (saves ~100 lines)
2. **Short-term:** Remove debug logging from production builds
3. **Medium-term:** Decide on `BarOverlay.kt` fate (keep or remove)
4. **Long-term:** Add unit tests, improve error handling

## Performance Notes

The current implementation is performant in release builds due to:
- Hardware layer caching (`LAYER_TYPE_HARDWARE`)
- Drawing only bar portions (not full bitmap)
- No per-frame allocations in `onDraw`

No further performance changes needed unless profiling shows issues.
