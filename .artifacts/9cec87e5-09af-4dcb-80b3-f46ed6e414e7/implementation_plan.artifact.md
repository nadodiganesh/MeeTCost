# Implementation Plan - Visual & Functional Overhaul

Overhaul the MeeTCost app to feature creative "Money Sandglass" animations, advanced currency support, and a refined "Simple & Sleek" user interface.

## User Review Required

> [!IMPORTANT]
> **Custom Animation:** The "Money Sandglass" will be implemented using Compose `Canvas`. It will feature falling dollar signs instead of sand particles, with the speed of flow correlating to the "burn rate" (Attendees × Hourly Rate).

> [!NOTE]
> **Reset Logic:** Following the request, when the tracker is paused, the "End" button will transform into "Reset". Resetting will clear all progress and return the app to its initial state.

## Proposed Changes

### [Component] Domain & Data Layer

#### [NEW] [CurrencyService.kt](file:///C:/Users/nadod/AndroidStudioProjects/MeeTCost/app/src/main/java/com/example/meetcost/data/CurrencyService.kt)
Define a Retrofit interface to fetch exchange rates from a free API (e.g., `exchangerate-api.com`).

#### [NEW] [CurrencyRepository.kt](file:///C:/Users/nadod/AndroidStudioProjects/MeeTCost/app/src/main/java/com/example/meetcost/data/CurrencyRepository.kt)
A repository to handle data fetching and caching for exchange rates.

---

### [Component] ViewModel & Logic

#### [MODIFY] [MeetingViewModel.kt](file:///C:/Users/nadod/AndroidStudioProjects/MeeTCost/app/src/main/java/com/example/meetcost/MeetingViewModel.kt)
- Add `isEnded: StateFlow<Boolean>` to track the finished state.
- Add `selectedCurrency: StateFlow<String>` (default: EUR or GBP).
- Add `exchangeRates: StateFlow<Map<String, Double>>`.
- Update `endMeeting()` to trigger the summary state.
- Update `reset()` to ensure a clean start from zero.

---

### [Component] UI & Animation

#### [NEW] [MoneySandglass.kt](file:///C:/Users/nadod/AndroidStudioProjects/MeeTCost/app/src/main/java/com/example/meetcost/ui/MoneySandglass.kt)
A custom Composable using `Canvas` and `Animatable` to draw a sleek sandglass where "$" symbols fall through.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/nadod/AndroidStudioProjects/MeeTCost/app/src/main/java/com/example/meetcost/MainActivity.kt)
- **Sleek Inputs:** Remove "A" and "R" labels, simplify `OutlinedTextField` styling.
- **Dynamic Buttons:**
    - `isTracking == true`: Show "Pause" and "End".
    - `isTracking == false && currentCost > 0`: Show "Resume" and "Reset".
- **Summary View:** When `isEnded == true`, display the total in USD and selected currency with a small-font summary of attendees and duration.
- **Visuals:** Integrate `MoneySandglass` at the top of the screen.

## Verification Plan

### Automated Tests
- **Unit Tests:** Verify `MeetingViewModel` logic for `recalculateCost` and `reset`.
- **UI Tests:** Verify button transitions (End -> Reset) using Compose testing library.

### Manual Verification
- Deploy to emulator.
- Start a meeting, observe "falling dollars" animation.
- Pause the meeting, verify "End" button becomes "Reset".
- End the meeting, verify the summary display with two currencies.
