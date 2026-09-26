-- Existing databases only. Back up the content table and confirm the index is absent
-- before executing once; schema.sql already contains the index for fresh installs.
ALTER TABLE content
  ADD INDEX idx_comment_feed (parent_id, type, status, created_at, id);
