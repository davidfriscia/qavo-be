# Contributing to Qavo Backend

Thank you for your interest in contributing to Qavo. This project is open source and welcomes
contributions under the standards below.

## Ground rules

- **Own what you submit.** Every contribution — human- or AI-assisted — must be understood, tested,
  and owned by the person submitting it. Qavo is developed with AI assistance under human review and
  accountability (architecture §12); the same quality and security bar applies to everyone.
- **Licensing & provenance.** By contributing you agree your contribution is licensed under the
  project's [MIT](LICENSE) license, and you must not introduce code whose provenance or
  license is unclear.
- **Be respectful.** See the [Code of Conduct](CODE_OF_CONDUCT.md).

## Development workflow

1. Fork and create a feature branch.
2. Build and test locally: `mvn clean verify` (Docker is required for Testcontainers tests).
3. Keep changes focused; follow the [best practices](docs/best-practices.md) and the layering and
   plugin conventions.
4. Add or update tests. New behavior needs coverage; bug fixes need a regression test.
5. Update documentation (capabilities matrix, configuration reference, ADRs) when behavior changes.
6. Open a pull request with a clear description of the change and its rationale.

## Architectural expectations

- New capabilities are **plugins**, not additions to the core (see
  [ADR-0001](docs/adr/0001-modular-monolith-and-plugins.md)).
- Public, cross-cutting changes (the error contract, security context, configuration namespaces,
  any token contract) follow Semantic Versioning: breaking changes require a MAJOR bump and a
  migration note.
- Significant decisions are recorded as an [ADR](docs/adr/).

## Code style

- Java 21, constructor injection only, immutable DTOs (`record`) where appropriate.
- English everywhere: code, comments, docs, commit messages.
- Default to no comments; comment only the non-obvious "why".

## Reporting security issues

Do **not** open public issues for vulnerabilities. Follow the responsible-disclosure process in
[SECURITY.md](SECURITY.md).
