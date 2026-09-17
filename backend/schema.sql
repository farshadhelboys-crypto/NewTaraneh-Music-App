CREATE TABLE IF NOT EXISTS songs (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  message_id INTEGER UNIQUE NOT NULL,
  file_id TEXT NOT NULL,
  file_unique_id TEXT,
  title TEXT,
  artist TEXT,
  caption TEXT,
  duration INTEGER DEFAULT 0,
  file_size INTEGER DEFAULT 0,
  mime_type TEXT DEFAULT 'audio/mpeg',
  thumbnail_file_id TEXT,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_songs_created ON songs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_songs_title ON songs(title);
CREATE INDEX IF NOT EXISTS idx_songs_artist ON songs(artist);
