# NextGalleryApp – working rules

Android gallery app (Kotlin, Jetpack Compose). Start with [README.md](../README.md); design docs live in [docs/](../docs/).

## Git

- Delete local branches once they are stale, i.e. merged into `main` or whose remote branch is gone
  (`git fetch --prune`, then `git branch -d <branch>`). Never delete unmerged work without asking.

## Tests

- Always write unit tests for each function you add or change.
- Keep unit tests simple: one behaviour per test, clear arrange/act/assert, no clever helpers.
- Avoid test overlap: each behaviour is covered by exactly one test at the lowest layer that can verify it.
  Don't re-test a lower layer's logic from a higher layer; mock it instead.

## Documentation

- Keep documentation and code in sync. A change that alters behaviour, architecture, modules, dependencies or
  setup updates the affected docs in the same commit.
- All documentation goes in `docs/`. Only `README.md` sits in the repository root; it is the entry point and
  links to the documents in `docs/`.
- Diagrams are written in Mermaid inside Markdown.

## Key documents

- [docs/ARCHITECTURE.md](../docs/ARCHITECTURE.md) – modules, layers, data model, key flows.
- [docs/DESIGN_DECISIONS.md](../docs/DESIGN_DECISIONS.md) – decisions taken and open questions.
- [docs/TODO.md](../docs/TODO.md) – ordered implementation plan; tick items off as they are done.
