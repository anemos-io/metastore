# Change Log
All notable changes to this project will be documented in this file.

## 0.8.6

## Added
- Added logging when an exception happens when sending a
  descriptor.
- Added input validation for package name and prefix.
  
## Fixed
- Delete files in the git repo when they are removed from
  the store.
- Fixed incorrect handling of scope.
- Escaping for string values in options.

## 0.8.5

### Fixed
- ProtoLanguageFileWriter writes the correct package name
  in the field.
- ProtoDomain filters google.proto package so the static 
  singleton descriptors are used.

## 0.8.4

### Fixed
- ProtoDomain findBy now works with uninterpreted options.
  
### Added
- Get FileDescriptorSet on ProtoDomain

### Changed
- Moved to minimal dependencies for putils module

## 0.8.2

### Fixed
- ProtoLanguageFileWriter now uses the real filename instead of
  inferring it from the package name.

## 0.8.1

### Added
- Diffing on Services and Methods

### Changed
- Make --comment a required field when publishing for 
  metastep
- Automatic handle all proto related imports (example:
  timestamp, duration, wrapper, ...)
- Reference to errors are provided by name

### Fixed
- Also propagate comment to shadow registry

## 0.8.0

### Added
- Diffing on Enum and EnumValues

### Changed
- Braking change: Unified the chagetype into one enum
- Braking change: Error feedback

### Fixed
- Submitting on package now works as expected

## 0.7.5

### Added
- Add use of Proto extension registry to parse options
- Add getters on ProtoDomain to get options out of the catalog
- Add find methods to search for Message, File and Service 
  descriptor by option name
- Allow to set a comment and author via the metastep. This will
  be reflected in the git commit if configured

### Fixed
- Unified unknown option parsing, making it more robust

## 0.7.4
### Added
- Added API implementation for scope: prefix and package

### Fixed
- Make ProtoLint not crash on deletion
- Make submission more rebust with better error message
  on API.
- Allow metastep to submit without setting the package
  prefix

## 0.7.2

### Added
- Ability to push a single file
- Diff on imports on a FileDescriptor

### Changed

### Fixed
- Default behaviour of pushing contracts is merge

## 0.7.1

### Added
- Selecting validation profile via API and metastep

### Changed
- Plugin updated Jib to 1.8.0
- Plugin updated spotless to 3.26.0
- Dependencies upgrade
- PContainer refactored to ProtoDomain to be more
  aligned with the version in Apache Beam

### Fixed
- MetaStep: Only list proto files

## 0.7.0

### Added
- ProtoFileWriter: Write service type
- ProtoFileWriter: Write service options
- ProtoFileWriter: Write method options
- ProtoFileWriter: Write enum options
- ProtoFileWriter: Write enum value options
- ProtoFileWriter: Partial comment write
- More tests for ProtoFileWriter
- MetaStep: Support for include and source command line

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

