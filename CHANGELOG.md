# Change Log
All notable changes to this project will be documented in this file.

## 0.6.0

### Added

- First iteration of the Proto to JSON schema convertor.

### Changed

### Fixed

- Updated gRPC dependencies to version 1.23.0, this fixes some vulnerabilities
  in HTTP/2 implementations as detailed by CERT Vulnerability Note 
  [VU#605641](https://kb.cert.org/vuls/id/605641/)
- If scope is not set on GetSchema all file discriptors are returned.

## 0.5.0

### Added

- First testable version in *alpha* program.

