# SelkicX Manual Booth — full scaffold (Milestones 1–8)

Photographer-operated photo booth utility (Kotlin + Jetpack Compose).
Every milestone from the original implementation plan is now built:

- **Milestone 1** — Room database schema, domain models, and the
  CameraAdapter / PrinterAdapter interfaces (mock camera + initial
  Android Print Framework printer).
- **Milestone 2** — the Home → New Session → Active Session UI flow,
  wired end-to-end against `AndroidHotFolderCameraAdapter` and `AndroidPrintAdapter`,
  including the required PRINT confirmation popup and Next Customer
  session-cycling.
- **Milestone 4** — the real rendering pipeline: crop → mask (rectangle /
  heart / unique) → Original/Black & White → composite onto the template
  artwork → Final Output JPEG, persisted and sent to the printer.
- **Milestone 6** — Admin > Templates: folder create/rename/delete,
  template creation (name → size → SAF artwork upload), and the photo-
  holder editor (add/drag/resize/delete rectangle/heart/unique masks).
- **Milestone 7** — Session History: every session listed newest-first
  with print/sync status, a detail view showing all originals and the
  Final Output, and Reprint - which resends the already-rendered file
  rather than re-rendering.
- **Milestone 8** — manual-only QR sharing (Admin toggle, a share link +
  QR code generated only on explicit tap, from both Active Session after
  printing and Session History) and a WorkManager-backed cloud sync stub
  that tracks PENDING/SYNCING/SYNCED/FAILED per session and retries
  automatically once connectivity returns.

(Milestones are numbered per the original implementation plan; Milestone
3 — selection/ordering/PRINT-gating — landed inside Milestone 2's Active
Session screen rather than as a separate pass. Milestone 5 — persisting
`FinalOutputEntity` and wiring the real path into the printer — landed
inside Milestone 4 for the same reason.)

## What's stubbed rather than real

Two things are deliberately client-side stubs, both called out in code
comments at their exact seam:

- **QR share URLs** point at a placeholder domain
  (`QrRepository.SHARE_BASE_URL`). The token/link shape matches what
  spec section 46 asks for (secure random, tied to the cloud session,
  not a sequential id), but there is no real SelkicX backend to resolve
  it yet.
- **Cloud upload** (`CloudSyncWorker.uploadSessionStub`) always returns
  `false`, so every session's sync status stays "Pending" and the worker
  keeps retrying - by design, so nothing is ever silently marked synced
  before a real upload exists (spec rule #31). Swapping in a real
  Retrofit call there is the only change needed once the SelkicX API
  exists.

The local manual-import workflow, rendering, Android print handoff,
templates, holders, sessions, history, and reprint are implemented.

## What's here

```
settings.gradle.kts, build.gradle.kts, gradle.properties  — project-level
app/build.gradle.kts, app/src/main/AndroidManifest.xml    — module-level

app/src/main/java/com/selkicx/manualbooth/
├── domain/
│   ├── model/Enums.kt              PhotoHolderShape, CropMode, PhotoMode,
│   │                                SessionState, PrintStatus, CloudSyncStatus
│   └── adapters/
│       ├── CameraAdapter.kt        CameraAdapter interface, CapturedPhoto
│       └── PrinterAdapter.kt       PrinterAdapter interface, PrintJob, PrintResult
├── data/local/
│   ├── entity/                     Room entities: PrintSize, TemplateFolder,
│   │                                Template, PhotoHolder, Session, SessionPhoto,
│   │                                FinalOutput, PrinterProfile, CloudSyncJob,
│   │                                ShareLink, Account, Device
│   ├── dao/                        One Dao file per aggregate
│   ├── Converters.kt                Room TypeConverters for the enums above
│   └── AppDatabase.kt               Room database wiring all entities/DAOs
├── camera/
│   ├── MockCameraAdapter.kt         Emits sample photos on demand — build/test
│   │                                the whole app against this first
│   ├── AndroidHotFolderCameraAdapter.kt  Watches Canon Camera Connect/NFC
│   │                                     images published to Android MediaStore
│   └── ManualImportCameraAdapter.kt      SAF-based fallback import path
└── printer/
    ├── AndroidPrintAdapter.kt       Initial PrinterAdapter using the OS Print
    │                                Framework
    └── FinalOutputPrintDocumentAdapter.kt  Streams the rendered Final Output
                                              JPEG to the print job — no preview
                                              page, only runs after confirmation

app/src/main/java/com/selkicx/manualbooth/  (Milestone 2 additions)
├── MainActivity.kt                 Single-activity host, builds AppContainer,
│                                    hosts BoothNavGraph
├── di/AppContainer.kt               Hand-rolled DI: Room DB, adapters,
│                                    repositories — no DI framework
├── data/repository/
│   ├── TemplateRepository.kt        Sizes/folders/templates/holder counts
│   └── SessionRepository.kt         Session lifecycle + live capture pipeline,
│                                     runs in its own long-lived scope so it
│                                     survives navigation between screens
├── ui/navigation/                   Routes.kt, BoothNavGraph.kt (Home ↔
│                                     New Session ↔ Active Session; Admin and
│                                     History are stubbed nav targets)
├── ui/common/ViewModelFactory.kt     Tiny factory for manual DI + viewModel()
├── ui/home/                         HomeScreen + HomeViewModel — camera/
│                                     printer status, NEW SESSION, recent list
├── ui/newsession/                   NewSessionScreen + NewSessionViewModel —
│                                     one linear wizard: size → folder →
│                                     template → photo mode → confirm/start
└── ui/activesession/
    ├── ActiveSessionScreen.kt       Live photo grid, numbered selection,
    │                                gated PRINT, Next Customer
    ├── ActiveSessionViewModel.kt    Selection/print/next-customer logic,
    │                                calls FinalOutputUseCase on confirm
    ├── PrintConfirmationDialog.kt   The one required confirmation popup
    └── PrintStatusOverlay.kt        Rendering.../Printing... status, in place

app/src/main/java/com/selkicx/manualbooth/rendering/  (Milestone 4 additions)
├── FinalOutputRenderer.kt          Pure image pipeline: downsample-decode →
│                                    ORIGINAL/B&W → center crop → shape mask
│                                    (rectangle/heart/unique) → place at holder
│                                    coords → composite template artwork → JPEG
└── FinalOutputUseCase.kt           Loads session/template/holders/print size,
                                     calls the renderer, persists FinalOutputEntity,
                                     links it back onto the session

app/src/main/java/com/selkicx/manualbooth/ui/admin/  (Milestone 6 additions)
├── AdminScreen.kt                  Admin root menu - only Templates is wired;
│                                    the rest are listed as "coming soon"
└── templates/
    ├── ArtworkImporter.kt           Copies a SAF-picked JPG/PNG into app-
    │                                private storage (renderer needs a stable path)
    ├── EditableHolder.kt            UI-local holder model + PhotoHolderEntity
    │                                mapping + slot renumbering after delete
    ├── NameInputDialog.kt           Shared folder create/rename text dialog
    ├── TemplateFoldersViewModel.kt / TemplateFoldersScreen.kt
    │                                Folder list + create/rename/delete
    ├── TemplateListViewModel.kt / TemplateListScreen.kt
    │                                Templates within one folder + ADD TEMPLATE
    ├── AddTemplateViewModel.kt / AddTemplateScreen.kt
    │                                Name → size chips → SAF artwork picker →
    │                                save, then straight into the holder editor
    └── PhotoHolderEditorViewModel.kt / PhotoHolderEditorScreen.kt
                                     Add/drag/resize/delete holders over the
                                     template artwork; required photo count is
                                     just the holder list's size, shown live

app/src/main/java/com/selkicx/manualbooth/ui/history/  (Milestone 7 additions)
├── SessionHistoryViewModel.kt / SessionHistoryScreen.kt
│                                    Every session, newest first, with print/
│                                    sync status; tap to open detail
└── SessionDetailViewModel.kt / SessionDetailScreen.kt
                                     Session info, Final Output preview, all
                                     original thumbnails, Reprint (single
                                     confirmation popup, no rerender), and QR
                                     (only when enabled)

app/src/main/java/com/selkicx/manualbooth/  (Milestone 8 additions)
├── cloud/
│   ├── CloudSyncWorker.kt          CoroutineWorker: retries PENDING/FAILED
│   │                                CloudSyncJob rows; upload call is a stub
│   │                                (always fails so nothing is marked synced
│   │                                before a real backend exists)
│   └── CloudSyncScheduler.kt        Periodic (network-constrained) + one-time
│                                     WorkManager requests
├── qr/QrCodeGenerator.kt            ZXing-based QR bitmap renderer
├── data/repository/
│   ├── QrRepository.kt              Secure-random share tokens, one per
│   │                                 session, created only on demand
│   └── AppSettingsRepository.kt     SharedPreferences-backed QR Sharing
│                                     ON/OFF flag
└── ui/
    ├── admin/QrSettingsScreen.kt     Admin > QR Sharing toggle
    └── qr/QrViewModel.kt, QrDialog.kt
                                      Reusable "tap QR" dialog: generates/
                                      fetches the share link + renders the
                                      code, used from both Active Session
                                      (after printing) and Session Detail
```

`build.gradle.kts` (root), `settings.gradle.kts`, `gradle.properties`,
`app/build.gradle.kts`, and `app/src/main/AndroidManifest.xml` make this
a complete, ready-to-open Gradle project — see "Build & run" below.

## Design notes tying this back to the spec

- **Photo mode lives on `SessionEntity`, never on `TemplateEntity`** — the
  same template can be used Original today, Black & White tomorrow.
- **`requiredPhotoCount` is never stored directly** — always derive it from
  `PhotoHolderDao.countForTemplate()`, so it can't drift from the holders.
- **`SessionPhotoEntity` stores every captured photo**, selected or not;
  `SessionEntity.selectedPhotoIdsOrdered` is a separate, ordered list —
  selection order is what determines holder assignment.
- **Camera/printer code never appears outside `camera/` and `printer/`** —
  everything else depends only on `CameraAdapter` / `PrinterAdapter`, so
  swapping in a real Canon/Nikon/Sony SDK later touches nothing else.
- **`AndroidPrintAdapter.print()` performs no confirmation of its own** —
  the confirmation popup lives in the UI layer (spec rule #17) and always
  runs before `print()` is ever called.
- **`SessionRepository` owns a long-lived `CoroutineScope`**, not a
  ViewModel-supplied one — the live capture pipeline is started from the
  New Session wizard but must keep running once the operator navigates to
  Active Session, so it can't be tied to either screen's lifecycle.
- **The PRINT button is disabled until `selectedPhotoIds.size == requiredPhotoCount`**,
  and tapping it only ever opens `PrintConfirmationDialog` — there is no
  code path that calls `printerAdapter.print()` without that popup first
  (spec rules #16, #17, #20).
- **`FinalOutputRenderer` follows spec section 35's documented order literally**:
  photos are cropped/masked/placed onto the canvas *first*, then the
  template artwork is composited on top. This means a template PNG with
  transparent "windows" over the photo areas will show the photo through
  them while any opaque decorative elements (borders, text, logos) sit
  over the photo — matching how these templates are typically designed
  in Canva/Photoshop/Illustrator (spec section 23).
- **`FinalOutputRenderer` decodes each source photo downsampled to its
  holder's actual target size** (`BitmapFactory.Options.inSampleSize`),
  never the full DSLR resolution, and recycles every intermediate bitmap
  — the memory-care instruction in spec section 35.
- **Original photo files are only ever read** by `FinalOutputUseCase`,
  never written to or modified (spec rule #15).
- **The heart and "unique" masks are hand-drawn Bezier `Path`s**, not
  images — there is exactly one of each per spec rule #12 (only 3 shapes
  exist for V1: RECTANGLE, HEART, UNIQUE); swapping in nicer curves later
  only touches `FinalOutputRenderer.heartPath()` / `.uniquePath()`.
- **`AddTemplateScreen` never asks for a required photo count** — that
  field doesn't exist as user input anywhere; it's always
  `holders.size`, computed live in `PhotoHolderEditorScreen` and derived
  again from the database via `TemplateRepository.requiredPhotoCount()`
  everywhere else (spec rule #13).
- **`PhotoHolderEditorViewModel` tracks deletions separately
  (`deletedEntityIds`)** rather than diffing on save, so a holder removed
  and never saved doesn't leave an orphaned row, and a holder added,
  moved, then removed in the same editing session never touches the
  database at all.
- **`ArtworkImporter` copies the SAF-picked file into app-private
  storage** rather than storing the picked `content://` Uri directly —
  Uris aren't guaranteed stable across app restarts without persisted
  permissions, and `FinalOutputRenderer` needs a plain file path it can
  hand to `BitmapFactory.decodeFile`.
- **`SessionDetailViewModel.confirmReprint()` calls `printerAdapter.print()`
  directly on the stored `FinalOutputEntity.filePath`** — there is no
  render step in that path at all, matching spec section 49's "do not
  unnecessarily rerender" instruction literally, and it has its own
  single confirmation popup independent of Active Session's.
- **The QR button only ever renders when `AppSettingsRepository.qrSharingEnabled`
  is true**, checked independently in both `ActiveSessionScreen` and
  `SessionDetailScreen` — there is no code path that shows QR when the
  Admin toggle is off (spec rule #24), and neither screen shows it before
  a Final Output exists (spec rule #25: QR never appears automatically,
  and here it can't appear before there's anything to share).
- **`CloudSyncScheduler`'s `Constraints.setRequiredNetworkType(CONNECTED)`
  does all the "wait for connectivity, resume when it returns" work**
  (spec rule #31) — there's no manual network-listener code anywhere;
  WorkManager itself defers the request until the constraint is met.
- **`FinalOutputUseCase.render()` enqueues the session's cloud sync job**,
  not session close (Next Customer) — a session's full package (all
  captured originals + the just-rendered Final Output) is ready to
  upload the moment printing succeeds, independent of whether the
  operator has moved on yet (spec section 44 doesn't tie sync to any
  particular UI action, just "every completed session").

## Not yet built

The remaining Admin sections are Print Sizes CRUD (A4, 4x6, and Photocard
are seeded automatically on a fresh database), Camera, Printer, SelkicX
Account, Storage, and About. Printer profiles and the SelkicX backend are
also not wired yet.

## Canon Camera Connect / NFC hot-folder workflow

`AppContainer.cameraAdapter` uses `AndroidHotFolderCameraAdapter`. During an
active session, grant full photo access and transfer a JPEG/PNG from the
Canon EOS 1300D using Canon Camera Connect or its NFC image-send flow. The
adapter watches Android MediaStore for Canon-owned folders/packages and
standard Canon `IMG_` / `_MG_` filenames, then copies each new image into
`files/sessions/<sessionId>/originals` before adding it to the live gallery.

The camera remains independently operated; SelkicX does not use EDSDK or
control the shutter. The EOS 1300D requires the operator to select/send the
captured image—its firmware does not push every shot automatically. The
**IMPORT PHOTOS** SAF picker remains available as a fallback.

`AndroidPrintAdapter` uses the real OS print dialog, so printing on an
   emulator will show Android's "Save as PDF" virtual printer, which is
   enough to confirm the confirmed-print flow end-to-end.

## Default print sizes (no Admin CRUD screen yet)

Admin > Templates now covers folders, templates, and holders end-to-end.
Print sizes are still read-only from the UI, but `AppContainer` inserts A4,
4x6, and Photocard at 300 DPI when the print-size table is empty. Existing
operator data is never overwritten. The full local loop is therefore usable
after creating a folder/template/holders in Admin: New Session → choose the
template and mode → import photos → select → PRINT → confirm.

## Build & run

This is a complete, standard Gradle project — no file-merging required.

1. Open Android Studio (Jellyfish or newer; needs Java 17, which
   Android Studio bundles) → **Open** → select the `selkicx-manual-booth`
   folder. It has `settings.gradle.kts` at the root, so Android Studio
   will recognize it immediately and sync Gradle on its own.
2. Let the sync finish (it downloads dependencies from Google's and
   Maven Central's repositories — needs internet the first time).
3. Run on an emulator or a device with **API 26+**. Use the ▶ Run button
   with the `app` configuration Android Studio creates automatically.
4. The app opens on Home. Camera status becomes connected after Android
   grants photo-library access and the hot-folder observer starts. Printer
   status uses the OS Print Framework.
   Go **Home → Admin → Templates → New Folder → Add Template** (pick the
   size, choose any JPG/PNG from the emulator's sample gallery) → add a
   couple of photo holders → **Save Template** → back at Home →
   **+ NEW SESSION** → walk through size/folder/template/photo mode →
   **START SESSION**.
5. On the Active Session screen, allow full photo access and send photos
   with Camera Connect/NFC. They should appear automatically. Select them
   in holder order and tap **PRINT**. Use **IMPORT PHOTOS** as a fallback.

GitHub Actions compiles the project with JDK 17 and Gradle 8.6 and publishes
the debug APK as `selkicx-manual-booth-debug-apk`.
