# Change Log
All notable changes to this project will be documented in this file.

## unreleased

### Added
- ProtoFileWriter: Write service type
- ProtoFileWriter: Write service options
- ProtoFileWriter: Write method options
- ProtoFileWriter: Write enum options
- ProtoFileWriter: Write enum value options
- ProtoFileWriter: Partial comment write
- More tests for ProtoFileWriter
- MetaStep: Support for include and source command line

### Changed

### Fixed
- ProtoFileWriter: Writing of unknown options

## 0.6.5

### Added
- Support to connect via meta step with supplied TLS certificates

## 0.6.4
### Added
- Span and logging for git operations

### Changed

### Fixed
- Proto writer now outputs enum types.

## 0.6.3
### Added
- Port is now configured through the PORT environment variable

### Changed
- Changed to SL4J

## 0.6.2
### Fixed
- Fixed a NPE is the ProtoDiffer

## 0.6.1
### Fixed
- Renamed the service name to `grpc.registry.v1alpha1.Registry` as
it had a typo in the service name

## 0.6.0
### Added
- First iteration of the Proto to JSON schema convertor.

### Fixed
- Updated gRPC dependencies to version 1.23.0, this fixes some vulnerabilities
  in HTTP/2 implementations as detailed by CERT Vulnerability Note 
  [VU#605641](https://kb.cert.org/vuls/id/605641/)
- If scope is not set on GetSchema all file discriptors are returned.

## 0.5.0
### Added
- First testable version in *alpha* program.

