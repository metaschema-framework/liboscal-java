# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build and install
mvn install

# Replicate CI/CD build (recommended before pushing)
mvn install -PCI -Prelease

# Run tests only
mvn test

# Run a single test class
mvn test -Dtest=ExamplesTest

# Run a single test method
mvn test -Dtest=ExamplesTest#testExample

# Skip tests during build
mvn install -DskipTests

# Check license headers
mvn license:check

# Auto-format source code
mvn formatter:format

# Check for Checkstyle issues
mvn checkstyle:check
```

## Project Overview

liboscal-java is a Java library for processing [OSCAL](https://pages.nist.gov/OSCAL/) content. It provides:

- Reading/writing OSCAL documents in XML, JSON, and YAML formats
- OSCAL profile resolution to produce resolved catalogs
- Validation of OSCAL content well-formedness and syntax
- Experimental Metaschema constraint validation
- Builders for programmatically creating OSCAL data elements

This library is built on the [Metaschema Java Tools](https://github.com/metaschema-framework/metaschema-java) project.

## Architecture

This is a single-module Maven project. Key packages:

- `dev.metaschema.oscal.lib` - Core library classes including `OscalBindingContext`
- `dev.metaschema.oscal.lib.model` - Generated OSCAL model classes (Catalog, Profile, SSP, etc.)
- `dev.metaschema.oscal.lib.profile.resolver` - Profile resolution implementation
- `dev.metaschema.oscal.lib.metapath.function.library` - OSCAL-specific Metapath functions

### Generated Model Classes

OSCAL model classes are generated from Metaschema definitions in the `oscal/` submodule during the build. These appear in `target/generated-sources/metaschema/` and include classes like `Catalog`, `Profile`, `SystemSecurityPlan`, `ComponentDefinition`, etc. Abstract base classes in `dev.metaschema.oscal.lib.model` provide common functionality.

### OSCAL-Specific Metapath Functions

Custom functions beyond core Metaschema (registered in `OscalFunctionLibrary`):
- `has-oscal-namespace` - Check OSCAL namespace membership
- `resolve-profile` - Resolve an OSCAL profile to a catalog
- `resolve-reference` - Resolve OSCAL internal references

## Code Style

- Java 11 target
- Uses SpotBugs annotations (`@NonNull`, `@Nullable`) for null safety
- Package structure follows `dev.metaschema.oscal.*` convention

## Git Workflow

- Repository: https://github.com/metaschema-framework/liboscal-java
- **All PRs MUST be created from a personal fork** (required by CONTRIBUTING.md)
- **All PRs MUST target the `develop` branch**
- All changes require PR review

## Git Worktrees (MANDATORY)

**All development work MUST be done in a git worktree, not in the main repository checkout.**

### Why Worktrees Are Required

- Isolates feature work from the main checkout
- Prevents accidental commits to the wrong branch
- Allows parallel work on multiple features
- Keeps the main checkout clean for reference and review

### Worktree Location

Worktrees are stored in `.worktrees/` directory (gitignored) relative to the repository root.

### Workflow

1. **Before starting any feature work**, create a worktree:

```bash
# Create worktree for a new feature branch
git worktree add .worktrees/<feature-name> -b <feature-branch> origin/develop
```

2. **Check for existing worktrees** before making changes:

```bash
git worktree list
```

3. **Switch to the appropriate worktree** if one already exists for your task

4. **Remove worktrees** after PRs are merged:

```bash
git worktree remove .worktrees/<feature-name>
```

### Red Flags (You're Working in the Wrong Directory)

- Making changes without first checking `git worktree list`
- Working in the main repository when a worktree exists for the feature
- Creating files or commits in the main checkout for feature work

Use the `superpowers:using-git-worktrees` skill for guided worktree creation and management.

## Testing

- Tests use JUnit 5
- All PRs require passing CI checks before merge
- 100% of unit tests must pass before pushing code

## Dependencies

This library depends on:
- `metaschema-java` (core Metaschema framework)
- OSCAL model bindings (generated from OSCAL Metaschema modules in `oscal/` submodule)
