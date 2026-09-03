package scalasql.dbzio.tests

import scalasql.query.Select

/**
 * Dirty hack, don't use this in any real code.
 *
 * This is to provide a more real-like code example by mimic-ing method available for PostgreSQL /
 * MySql dialects.
 *
 * SQLite doesn't support SELECT FOR UPDATE, but spinning up testcontainers just for this is
 * overkill.
 */
extension [Q, R](s: Select[Q, R]) def forUpdate: Select[Q, R] = s
