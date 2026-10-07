Realtime Database Schema

Security rules live in `database.rules.json` at the repo root. Paste them into Firebase Console, Realtime Database, Rules, then Publish.

| Path | Fields |
|---|---|
| `users/{uid}` | fullName, email, username, bio, photoUrl, instruments[], genres[], createdAt |
| `songs/{songId}` | title, artist, key, capo, tempo, difficulty, genre, likeCount, saveCount, sections[] { name, lines[] } with lines in ChordPro format `[G]Hello [D]world` |
| `progressions/{id}` | title, key, bpm, genre, difficulty, chords[], authorId, authorName, description, playCount, likeCount, saveCount, createdAt |
| `likes/{targetId}/{uid}` | true |
| `comments/{targetId}/{commentId}` | authorId, authorName, text, parentId, createdAt, likes/{uid}: true |
| `saves/{uid}/{songs, chords, progressions}/{key}` | id, savedAt |
| `tunings/{uid}/{tuningId}` | name, instrument, notes[] |
