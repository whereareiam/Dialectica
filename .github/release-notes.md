---
version: 4.0.1
---

# Dialectica 4.0.1 - Key Conflicts on Updates

An update that would give a row a unique key another row already holds now throws `KeyConflictException` on every database, SQLite included, as inserts and upserts already did. A project can tell a taken key apart from other failures of an update and answer it, for example by reporting the key as taken.
