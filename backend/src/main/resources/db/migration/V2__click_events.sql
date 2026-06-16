CREATE TABLE click_events (
   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   link_id UUID NOT NULL,
   clicked_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
   ip_hash VARCHAR(64),
   user_agent TEXT,
   referer TEXT,
   created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

   CONSTRAINT fk_click_events_link
       FOREIGN KEY (link_id)
           REFERENCES links(id)
           ON DELETE CASCADE
);

CREATE INDEX idx_click_events_link_id ON click_events(link_id);
CREATE INDEX idx_click_events_link_clicked_at ON click_events(link_id, clicked_at);
