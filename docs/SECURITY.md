# Security policy

## Supported versions

NextGalleryApp is maintained by one person in their spare time. Only the latest release gets security fixes; older
versions are not patched. Until the first release is tagged, fixes go into `main`.

| Version | Supported |
|---|---|
| Latest release | Yes |
| Older releases | No |

## Reporting a vulnerability

Please **do not open a public issue** for a security problem.

Report it privately through GitHub instead: open the repository's
[Security tab](https://github.com/drehtuer/NextGalleryApp/security) and choose **Report a vulnerability**
(private vulnerability reporting). Only the maintainer can see the report.

Please include:

- what an attacker can do, and what they need for it (a malicious server, a crafted image, another app on the phone, …);
- the steps to reproduce it, and the app version or commit;
- the device and Android version, if it matters.

What happens next:

- You get an answer within a week. This is a hobby project, so there is no faster guarantee.
- Once the problem is confirmed, it is fixed in a new release. The advisory is published with the fix, and you are
  credited in it unless you would rather not be.
- If the report is not accepted as a vulnerability, you get the reason.

## Scope

In scope are the app (`app/`, `core/`, `feature/`) and the build and release setup in this repository, in particular:

- the Nextcloud sign-in and the stored app password (Login Flow v2, encryption with Tink and the Android Keystore);
- TLS handling, including accepted self-signed certificates (D32 in [DESIGN_DECISIONS.md](DESIGN_DECISIONS.md));
- parsing of untrusted data: images and their EXIF data, WebDAV responses from the server, map tiles;
- data that leaves the device or reaches other apps: shares, the hand-off to the Nextcloud app, the URI grants.

Out of scope are vulnerabilities in the Nextcloud server, the official Nextcloud Android app, OpenStreetMap or the
tile server you configure, and in Android itself. Please report those to their projects. The same goes for problems
that need a rooted or already compromised device.

## How the code is checked

- **Dependabot** reports known vulnerabilities in the dependencies and proposes updates (D57).
- **CodeQL** scans the Kotlin code and the GitHub Actions workflows on every pull request and every week. **OpenSSF
  Scorecard** rates the repository's practices (D58).
- **Dependency review** fails a pull request that adds a vulnerable dependency or a licence GPL-3.0 cannot take.
- **SonarCloud** reports code issues and coverage (D41).
- **Secret scanning** with push protection keeps credentials out of the repository.
