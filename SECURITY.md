# Security Policy

## Supported versions

Qavo is at an early stage (`0.0.1-SNAPSHOT`). Until a `1.0.0` release, only the latest commit on the
default branch receives security fixes.

## Reporting a vulnerability

**Please do not report security vulnerabilities through public GitHub issues.**

Instead, report responsibly by emailing **security@qavo.org** (or, until that mailbox is active, via
a GitHub private security advisory on the repository). Include:

- a description of the vulnerability and its impact;
- steps to reproduce or a proof of concept;
- affected module(s) and version/commit;
- any suggested remediation.

We will acknowledge receipt within a reasonable period, keep you informed of progress, and credit
reporters who wish to be credited once a fix is released.

## Scope and handling

Security-sensitive areas — authentication, authorization, cryptography, input validation, and the
secure-header/CORS configuration — receive extra review. Fixes are prioritized and released as PATCH
versions where the change is backward-compatible.

## Secrets

Never include secrets (passwords, tokens, keys) in issues, pull requests, or the repository. Publishing
credentials live only in the CI secret store. Secrets for running applications are supplied via
environment variables (architecture §5.5).
