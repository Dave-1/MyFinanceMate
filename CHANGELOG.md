# Changelog

All notable changes to MyFinanceMate are documented here.

## [1.2.0] - 2026-08-05

### Added
- **Accounts**: bank accounts are now tracked and displayed. First detected sender becomes the primary account automatically; primary account is shown on the Dashboard balance card and selectable when adding/editing transactions.
- **Account resolution from SMS**: the `A/C xxxx1234` suffix in bank SMS is extracted and stored on the account, so per-account attribution works across senders.
- **Salary detection**: income transactions from senders configured with a `SALARY` sms-rule are flagged `isSalary`.
- **Fixed-expense detection**: recurring expenses (same merchant, same account, ≥ 3 distinct months, amount within tolerance) are auto-detected and create a `MONTHLY` reminder marked `AUTO`. Detection runs after SMS import and on live SMS receipt.
- **Fixed-expense settings**: `Minimum months` and `Variance tolerance` sliders added to Settings.
- **Accounts screen**: list, add, rename, delete, and set-primary management.
- **Transaction account filter**: filter chips for accounts on the Transactions screen.
- **Room migration v2 → v3** with data preservation (`accounts`, `fixed_expense_config` tables; `accountId`, `isSalary` columns).
- **JVM unit tests** for `AccountResolutionService`, `SalaryDetector`, `FixedExpenseDetector`.

### Fixed
- **SMS parser date**: parsed transactions now use the actual SMS timestamp instead of `System.currentTimeMillis()`.
- **EditTransaction decimals**: amounts like `2435.38` are no longer truncated to `2435`.
- **Reports pie-chart donut**: center hole now follows the active theme background instead of hardcoded white.
- **Lock screen theming**: now uses theme colors consistently.
- **Backup error surfacing**: `DashboardViewModel` logs and exposes SMS-backup failures instead of swallowing them.
- **Release workflow**: old release-asset deletion no longer fails due to unexpanded `jq` variable.

### Changed
- `versionCode` 3, `versionName` `1.2.0`.
- F-Droid metadata mirrored (`metadata/com.myfinancemate.yml`, `f-droid/config.yml`, `fdroiddata`).

[1.2.0]: https://github.com/Dave-1/MyFinanceMate/compare/v1.1.0...v1.2.0
