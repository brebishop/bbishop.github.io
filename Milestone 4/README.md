# Milestone Four - Database Enhancement

This enhancement focuses on improving the security, integrity, and reliability of the Weight Tracker application's SQLite database.

The original database worked, but several parts of the design relied heavily on application logic. For this milestone, the database was strengthened so important relationships and security rules are enforced more directly.

## Included Files

- Enhanced Weight Tracker Android project
- Milestone Four narrative

## Key Improvements

- Replaced plain-text password storage with hashed passwords
- Added foreign-key relationships
- Strengthened update and delete ownership checks
- Improved date storage and sorting
- Replaced the drop-and-recreate upgrade process with a migration approach that preserves data
