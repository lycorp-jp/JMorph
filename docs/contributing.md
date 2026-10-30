# Contributing

This document explains how to contribute to this library.

## Important Notes

To maintain compatibility with internal development and keep maintenance manageable, we may be unable to accept pull requests that include changes such as:

- Breaking changes to the public specification
- Updating the supported Java version to the latest release
- Changing the build tool
- Adding dependencies
    - Pull requests containing code that may cause licensing issues
- Pull requests that change a large number of files
- Other similar changes

## Code of Conduct

This project follows a [Code of Conduct](../CODE_OF_CONDUCT.md) to foster fair and inclusive collaboration.
We expect all community members and contributors to adhere to it.

We believe that mutual respect and cooperation are essential in building a sustainable open source ecosystem.

## Developer Certificate of Origin (DCO)

All contributions must comply with the [Developer Certificate of Origin (DCO)](../DCO.md). Please review it before contributing.

When making a commit, please include a DCO sign-off by using the following flag:

`git commit -s -m "Commit message"`


This will automatically add the Signed-off-by line to your commit message.

## Corporate Contributor License Agreement
If you are contributing on behalf of a company, organization, or institution, we require a signed Corporate Contributor License Agreement (CCLA).
This ensures that your employer or organization has authorized your contribution.

Contributions from corporate entities will only be accepted once the CCLA has been signed and submitted.

Please [contact us](mailto:ml-ospo-contact@lycorp.co.jp) if you need the CCLA (Corporate Contributor License Agreement).

## Getting Started

1. Fork this repository.
1. Clone your fork locally, create a feature branch, and make your changes.
1. Confirm that the build and tests pass.
    - `./gradlew build`
1. Commit the changed files and create a pull request.

## Reporting an Issue

If you find a bug, include the following information whenever possible:

- Library version
- Java version
- Operating system
- Detailed circumstances in which the problem occurred
    - Reproduction steps
