---
version: 4.0.0
---

# Dialectica 4.0.0 - Safe Writes

Entities can now be updated column by column, only while conditions on the row hold, inserted only if absent, and upserted on just the columns that change, each in a way that stays correct when several instances write the same row at once. Entity fields can also declare column defaults.

## Upgrading

- `EntityAccess.update(entity)`, which rewrites every column of a row, is now `replace(entity)`. `update(key...)` starts a partial update instead.
- A third-party `StatementRenderer` renders updates from one list of conditions, each with its comparison, and returns an `Upsert` for upserts. A third-party `Dialect` implements `isKeyConflict(SQLException)`.
- An upsert that clashes on a unique key other than the primary key throws `KeyConflictException` on every database.
