ALTER TABLE goals
    ADD COLUMN knowledge_preference_json TEXT;

ALTER TABLE knowledge_documents
    ADD COLUMN retrieval_priority INTEGER NOT NULL DEFAULT 3,
    ADD COLUMN source_category VARCHAR(30) NOT NULL DEFAULT 'NOTE',
    ADD COLUMN group_name VARCHAR(120),
    ADD COLUMN tags_text TEXT;

CREATE INDEX idx_knowledge_documents_source_category
    ON knowledge_documents(source_category);

CREATE INDEX idx_knowledge_documents_retrieval_priority
    ON knowledge_documents(retrieval_priority);
