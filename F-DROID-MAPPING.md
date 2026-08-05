# MyFinanceMate ↔ fdroiddata Mapping

This document maps every file in the **MyFinanceMate** repository that participates
in the F-Droid distribution flow to its counterpart — or absence — in the
**fdroiddata** repository, and vice versa.

It is intended to be the single source of truth for "if I change X here, where
does it need to change over there?".

---

## Repositories

| Role | Repo | Host | Path in this workspace |
|---|---|---|---|
| App source code | `MyFinanceMate` | github.com/Dave-1/MyFinanceMate | `MyFinanceMate/` |
| F-Droid metadata | `fdroiddata` | gitlab.com/Dave-1/fdroiddata (fork of gitlab.com/fdroid/fdroiddata) | `fdroiddata/` |

The `fdroiddata` repo is a full fork of the upstream F-Droid index (~9k+
apps). Only **one file** in it is yours:

- `metadata/com.myfinancemate.yml`

Everything else in `fdroiddata/` is upstream tooling, build extlibs, schemas,
etc. — owned by the F-Droid project, not by you.

---

## MyFinanceMate files that flow into F-Droid

| MyFinanceMate path | Purpose | fdroiddata counterpart | Notes |
|---|---|---|---|
| `metadata/com.myfinancemate.yml` | Canonical F-Droid metadata file (app entry). | **`metadata/com.myfinancemate.yml`** ← primary mapping | This is the file F-Droid actually reads. The MyFinanceMate copy is the "source-of-truth" you edit; the fdroiddata copy is what gets merged into the upstream index via your MRs to fdroid/fdroiddata. |
| `metadata/com.myfinancemate/en-US/full_description.txt` | Long app description (F-Droid style). | **Inlined into `metadata/com.myfinancemate.yml`** as `Description:` (or supplied via `description:` in the fdroiddata copy) | Currently the fdroiddata `com.myfinancemate.yml` has **no `Description:` field** — the description you maintain in MyFinanceMate is **not** mirrored into fdroiddata. Either inline it in the yml, or sync by hand. |
| `metadata/com.myfinancemate/en-US/short_description.txt` | Short app description. | Inlined as `Summary:` in the yml | Same — currently missing from fdroiddata. |
| `metadata/com.myfinancemate/en-US/changelog.txt` | Per-version changelog. | Each release gets a `changelogs/<versionCode>.txt` next to the yml. Currently only `changelogs/1.txt` exists in the `f-droid/metadata/en-US/` tree, and **nothing** is in fdroiddata yet. | |
| `metadata/com.myfinancemate/en-US/images/icon.png` | F-Droid icon (512×512 recommended). | Expected at `metadata/com.myfinancemate/en-US/icon.png` next to the yml — **currently absent in fdroiddata**. The 50 KB `icon.png` in fdroiddata root is the upstream F-Droid logo, not yours. | |
| `metadata/com.myfinancemate/en-US/phoneScreenshots/*.png` | Play-Store-style screenshots. | F-Droid does not consume these. They live only in MyFinanceMate (Play Store format). | |

---

## Auxiliary F-Droid-shaped data inside MyFinanceMate

You actually maintain **four** different F-Droid/Play-Store-style metadata
trees inside the MyFinanceMate repo (one of which, `tmp_fdroid_metadata/`, is
just a working copy of `metadata/`). They are not all in sync. Map:

| Tree | Schema | Used by | Files vs canonical |
|---|---|---|---|
| `metadata/` (canonical) | Fastlane/Play Store layout — `metadata/<pkg>/en-US/*.txt` plus `phoneScreenshots/`. Same `*.yml` shape but no F-Droid build entries. | Play Console via fastlane, F-Droid when copying | **Canonical** for descriptions, screenshots, icon. |
| `f-droid/` (legacy/bundle) | Self-contained F-Droid metadata bundle using `fdroid-server`'s old `config.yml` schema (`name`, `summary`, `description`, `apk:`). | `fdroid build` / `fdroid import` CLI (the `fdroiddata` upstream build tool) | **Duplicate** of `metadata/` with different wording. Drift risk. |
| `fastlane/metadata/android/en-US/` | Play Store fastlane layout (no `.yml`). | fastlane Play upload | **Subset** of `metadata/`. Drift risk. |
| `tmp_fdroid_metadata/` | Looks like a working/staging copy of `metadata/`. | Internal scratchpad only — not consumed by anything automated. | Drift risk. Should probably be deleted or `.gitignore`d. |

**Recommendation:** pick one tree (the canonical `metadata/` is the most
complete) and derive the other two from it on every release. Right now they
have drifted — e.g. `f-droid/config.yml` says "1.0.0 / Initial release" while
fdroiddata already publishes "1.1.0".

---

## fdroiddata fields mapped to MyFinanceMate sources

Every field in `fdroiddata/metadata/com.myfinancemate.yml` traces back to
something in MyFinanceMate:

| fdroiddata field | Source of truth in MyFinanceMate |
|---|---|
| `Categories: [Finance Manager]` | `metadata/com.myfinancemate.yml` (and `f-droid/config.yml` uses `[Finance, Money]` — **disagrees**) |
| `License: MIT` | `MyFinanceMate/LICENSE` |
| `AuthorName: Dave-1` | GitHub repo owner |
| `SourceCode:` | `metadata/com.myfinancemate.yml` |
| `IssueTracker:` | `metadata/com.myfinancemate.yml` |
| `Changelog:` | `metadata/com.myfinancemate.yml` |
| `AutoName:` | `app/src/main/res/values/strings.xml` (display app name) |
| `RepoType: git` | Hard-coded |
| `Repo:` | `metadata/com.myfinancemate.yml` |
| `Binaries:` | `.github/workflows/build-release.yml` upload step (asset name pattern: `MyFinanceMate.%v.apk` on the `v%v` tag) |
| `Builds[].versionName` | `app/build.gradle.kts` → `defaultConfig.versionName` |
| `Builds[].versionCode` | `app/build.gradle.kts` → `defaultConfig.versionCode` |
| `Builds[].commit` | The Git commit you tag for that release |
| `Builds[].subdir: app` | Single-module Android project layout |
| `Builds[].gradle: [yes]` | `app/build.gradle.kts` exists |
| `AllowedAPKSigningKeys:` | The SHA-256 fingerprint of the cert in `release.keystore` / `myfinanceMate.keystore`. **Rotating the key requires updating this field in fdroiddata.** |
| `AutoUpdateMode: Version` | Hard-coded |
| `UpdateCheckMode: Tags` | `.github/workflows/build-release.yml` triggers on `v*` tags |
| `CurrentVersion:` / `CurrentVersionCode:` | Newest entry under `Builds:` |

### Per-version drift observed

| versionCode | versionName | In MyFinanceMate HEAD? | In fdroiddata? | Build commit in fdroiddata |
|---|---|---|---|---|
| 1 | 1.0.0 | Yes (ancestor of HEAD) | Yes | `2e969ff715071644392a398104f5c3ec36e111f4` |
| 2 | 1.1.0 | Yes (== HEAD) | Yes | `e259f99a7244f79e63d03e95bc2a9c1d717eb9f6` |

**Verified 2026-08-05 — the drift described in earlier revisions is now
resolved.** The earlier claim that MyFinanceMate HEAD was
`23eee3f2225c13de0ce6311f32c7aa1c45d61d80` is stale: that commit still exists
in git object storage but is no longer the tip. HEAD is now
`e259f99a7244f79e63d03e95bc2a9c1d717eb9f6`, which is **exactly** the commit
fdroiddata's versionCode 2 (1.1.0) entry references, and fdroiddata's 1.0.0
commit `2e969ff...` is an ancestor of HEAD. Both repos now agree on both build
commits.

The stale spot has moved: `MyFinanceMate/metadata/com.myfinancemate.yml` and
`f-droid/config.yml` are **still at 1.0.0** (commit `23eee3f2...`,
`CurrentVersionCode: 1`) even though the code at HEAD is 1.1.0. fdroiddata is
now the correct/current side; those two local ymls need re-syncing.

**This table is a snapshot — it goes stale the moment either repo gets a new
commit.** Re-verify before trusting it with
`git -C MyFinanceMate rev-parse HEAD` and by reading
`fdroiddata/metadata/com.myfinancemate.yml`.

---

## Files in fdroiddata you should NOT touch

Everything else in fdroiddata is upstream infrastructure:

- `.gitlab-ci.yml`, `.gitlab/`, `.yamllint`, `.weblate`, `.well-known/` — CI, lint, translation hosting.
- `build/extlib/**` — third-party JARs the build server uses.
- `config.yml` — root config for the `fdroid` CLI tool (server-wide).
- `hooks/`, `tools/`, `schemas/`, `srclibs/`, `templates/` — tooling, JSON schemas, build-script libraries, MR templates.
- `repo/`, `tmp/` — generated output, not source-controlled content.
- `icon.png` — upstream F-Droid logo.
- All other ~9 000 entries under `metadata/` — other people's apps.

Edit only `metadata/com.myfinancemate.yml` (and add changelog files
beside it). Open a merge request against `fdroid/fdroiddata` once your
changes are ready.

---

## Release flow (how a new version travels)

1. Bump `versionName` / `versionCode` in `MyFinanceMate/app/build.gradle.kts`.
2. Update `metadata/com.myfinancemate.yml` (Play/fastlane shape) and
   `f-droid/config.yml` (legacy bundle shape).
3. Update `metadata/com.myfinancemate/en-US/changelog.txt` and create
   `metadata/com.myfinancemate/en-US/changelogs/<versionCode>.txt` for F-Droid.
4. Commit, push, tag `v<versionName>` on `MyFinanceMate`.
5. `.github/workflows/build-release.yml` builds and uploads the APK to
   the GitHub release.
6. In `fdroiddata`, append a new entry under `Builds:` for the new commit,
   bump `CurrentVersion:` / `CurrentVersionCode:`, and (if needed) add
   `metadata/com.myfinancemate/en-US/changelogs/<versionCode>.txt`.
7. Open an MR from `Dave-1/fdroiddata` → `fdroid/fdroiddata`.

### Known issue in the release flow

`.github/workflows/build-release.yml` hardcodes the upload asset name to
`MyFinanceMate.1.0.0.apk` and the release tag to `v1.0.0` on **every** tag
build. So a `v1.1.0` build silently overwrites the v1.0.0 GitHub release
asset. This affects:

- `Binaries:` in fdroiddata (which fetches
  `https://github.com/Dave-1/MyFinanceMate/releases/download/v%v/MyFinanceMate.%v.apk`)
  — the URL is correct, but the asset name needs to match the version.

Fix idea: derive both from `${{ github.ref_name }}` (the tag).

---

## Sync checklist (run this when cutting a release)

- [ ] `app/build.gradle.kts` — versionName/versionCode bumped
- [ ] `metadata/com.myfinancemate.yml` — version fields match
- [ ] `f-droid/config.yml` — `currentVersion`/`currentVersionCode` match
- [ ] `metadata/com.myfinancemate/en-US/changelogs/<versionCode>.txt` exists
- [ ] `.github/workflows/build-release.yml` — upload asset name matches `<pkg>.<versionName>.apk`
- [ ] GitHub release `v<versionName>` created with the signed APK
- [ ] `fdroiddata/metadata/com.myfinancemate.yml` — new `Builds:` entry, `CurrentVersion*` bumped, `Description:`/`Summary:` (currently absent) inlined
- [ ] MR opened against `fdroid/fdroiddata`

---

## Open questions for you

1. Do you want the `Description:` and `Summary:` from
   `metadata/com.myfinancemate/en-US/*.txt` inlined into the fdroiddata yml
   on every release? If yes, this mapping should grow a small sync script.
2. `tmp_fdroid_metadata/` and `f-droid/config.yml` look like
   carry-overs from an older `fdroid import` workflow. Are you still using
   them, or safe to delete?
3. The two-yml commit-hash drift on 1.0.0 is **resolved** — MyFinanceMate HEAD
   now matches fdroiddata for both builds (verified 2026-08-05). Remaining
   cleanup: `metadata/com.myfinancemate.yml` and `f-droid/config.yml` are
   still at 1.0.0; decide and re-sync them to 1.1.0.
