# Releasing

Gjuton publishes to [Maven Central](https://central.sonatype.com) via the
[Central Portal](https://central.sonatype.com). Releases are cut from a git tag:
pushing a `v*` tag triggers `.github/workflows/release.yml`, which builds, signs,
and uploads the artifacts. The upload is **not** published automatically — it
waits in a *pending* state on the Central Portal for a human to click **Publish**
(the plugin is configured with `autoPublish=false`). Maven Central is
**immutable**: once a version is published it can never be changed or removed, so
the manual publish step is a deliberate gate.

Versioning is manual. Between releases the POM version is `X.Y.Z-SNAPSHOT`
(Central rejects `-SNAPSHOT`); a release drops the suffix and bumps to the next
`-SNAPSHOT` in one pull request, and the tag goes on the commit in between.

## Prerequisites (one-time setup)

These must be done once before the first release. They are not part of the repo.

### 1. Central Portal account and namespace

1. Sign in at <https://central.sonatype.com> with the GitHub account that owns the
   `gjuton` org.
2. Register the namespace `io.github.gjuton` and complete the verification
   challenge the Portal issues (it confirms you control `github.com/gjuton`).
3. Under your account, generate a **user token** (Account → Generate User Token).
   It yields a username and password — these are the `CENTRAL_USERNAME` /
   `CENTRAL_PASSWORD` secrets below, not your login.

### 2. GPG signing key

Central requires every artifact to be GPG-signed and the public key to be
discoverable on a keyserver.

```bash
# Generate a key (RSA 4096, no expiry or a long one). Choose a passphrase and
# keep it secret — it becomes the GPG_PASSPHRASE secret.
gpg --full-generate-key

# Find the key id (the long hex after "sec   rsa4096/").
gpg --list-secret-keys --keyid-format=long

# Publish the public key so Central can verify signatures.
gpg --keyserver keys.openpgp.org      --send-keys <KEY_ID>
gpg --keyserver keyserver.ubuntu.com  --send-keys <KEY_ID>

# Export the private key (armored) for the GitHub secret.
gpg --armor --export-secret-keys <KEY_ID> > gjuton-signing-key.asc
```

### 3. GitHub Actions secrets

Add these to the repo (Settings → Secrets and variables → Actions), or to the
`gjuton` org. They are consumed by `release.yml`:

| Secret             | Value                                              |
| ------------------ | -------------------------------------------------- |
| `CENTRAL_USERNAME` | user-token username from the Central Portal        |
| `CENTRAL_PASSWORD` | user-token password from the Central Portal        |
| `GPG_PRIVATE_KEY`  | full contents of `gjuton-signing-key.asc`          |
| `GPG_PASSPHRASE`   | the passphrase chosen when generating the key      |

Delete the exported `gjuton-signing-key.asc` afterwards.

## Cutting a release

Assume the release is `0.0.2` and the POM currently reads `0.0.2-SNAPSHOT`.

1. **Update the changelog.** In `CHANGELOG.md`, rename `## [Unreleased]` to
   `## [0.0.2] — <today's date>` and add a fresh empty `## [Unreleased]` above it.
   Confirm the entries describe what a user gets by upgrading.
2. **Update the upgrade guide.** Add a `## 0.0.2` section to `UPGRADING.md`,
   above the previous release's. A release that breaks nothing still gets one,
   saying so — an absent section is indistinguishable from a forgotten one.
3. **Swap in the new README, if there is one.** A `README-unreleased.md` in the
   repo root means the current `README.md` describes the published version and
   goes stale the moment this release lands. Diff the two, carry over anything
   `README.md` gained since the replacement was written, set the version in it to
   the one being released, then replace `README.md` with it and delete both
   `README-unreleased.md` and the explanatory comment at its top.
4. **Drop the snapshot suffix.** Set the version to the release version across
   the reactor:
   ```bash
   mvn versions:set -DnewVersion=0.0.2 -DgenerateBackupPoms=false
   ```
5. **Verify the build** locally: `mvn clean verify`.
6. **Commit both the release and the next snapshot** on a branch. Master takes no
   direct pushes, so the release goes through a pull request like any other
   change. Keep them as two commits — the tag has to land on one whose poms read
   the release version:
   ```bash
   git checkout -b release-0.0.2
   git commit -am "chore: release 0.0.2"
   mvn versions:set -DnewVersion=0.0.3-SNAPSHOT -DgenerateBackupPoms=false
   git commit -am "chore: bump to 0.0.3-SNAPSHOT"
   git push -u origin release-0.0.2
   ```
7. **Open the pull request and merge it with rebase.** Not squash: squashing
   collapses the two commits into one reading `0.0.3-SNAPSHOT`, leaving nothing
   to tag, and Central rejects a snapshot version.
8. **Tag the release commit and push the tag.** Rebasing rewrote the SHAs, so
   take the commit from master rather than reusing a local one. Tags are not
   covered by the branch ruleset, so this pushes directly:
   ```bash
   git checkout master && git pull
   git tag v0.0.2 $(git log --format=%H --grep '^chore: release 0.0.2' -1)
   git push origin v0.0.2
   ```
   Confirm the tagged commit's `pom.xml` reads `0.0.2` first — a squashed merge
   is the way this goes wrong, and it fails late, at the Central upload. The tag
   push triggers `release.yml`, which builds, signs, and uploads to the Central
   Portal.
9. **Publish on the Portal.** Sign in at <https://central.sonatype.com>, open the
   pending deployment, inspect the staged artifacts, and click **Publish**. It
   syncs to Maven Central within a few minutes.
10. **Write the GitHub Release.** Create a release for tag `v0.0.2` and paste the
   `0.0.2` section of the changelog as the body.
11. **Tell each ticket which release it shipped in.** Someone who finds a ticket
   for the bug they hit needs to know whether the version they run contains the
   fix. Comment on every issue the release's PRs closed, once the artifact is
   actually downloadable:
   ```bash
   gh api graphql -f query='
   {
     repository(owner:"gjuton", name:"gjuton") {
       pullRequests(states:MERGED, first:50, orderBy:{field:UPDATED_AT, direction:DESC}) {
         nodes { mergedAt closingIssuesReferences(first:20){nodes{number}} }
       }
     }
   }' --jq '.data.repository.pullRequests.nodes[]
             | select(.mergedAt >= "<previous tag date, ISO 8601>")
             | .closingIssuesReferences.nodes[].number' | sort -u \
     | xargs -I{} gh issue comment {} \
         --body "Shipped in [v0.0.2](https://github.com/gjuton/gjuton/releases/tag/v0.0.2)."
   ```
   The query returns what the PRs *closed*, so a "relates to" or "caused by"
   mention is left alone. Two things it cannot see, both needing a manual pass:
   an issue closed by hand rather than by a PR keyword, and — because GitHub
   links only the first issue unless the keyword is repeated — the second and
   later issues in a body like `closes #184 #169`. Write
   `closes #184, closes #169` to avoid the second.

## Breaking changes

While pre-1.0, breaking changes are allowed in any release. The changelog entry
says what broke, in the ordinary Keep a Changelog section it belongs to; what to
do about it goes in `UPGRADING.md`, under the release's own section. Keeping the
steps out of the changelog is deliberate — the changelog summarises what changed,
and migration detail buried there is hard to find when it is actually needed.

Some breakage has no migration steps to give. Seeded output is reproducible
within a version, not across versions, so a generator change may alter what a
seed produces; the only advice possible is to re-record, which is not worth a
section. The changelog notes it and `UPGRADING.md` stays silent.
