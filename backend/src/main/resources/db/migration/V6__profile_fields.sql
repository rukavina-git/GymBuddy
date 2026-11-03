-- Plan 6 step 05: profile fields for onboarding and You (api/openapi.yaml
-- UserProfile). All nullable: existing rows have none of them.
-- health_data_consent_* is a record only (owner decision 2A); whether
-- sync requires it is decided in the privacy pass (roadmap 4.6).

ALTER TABLE user_profile
    ADD COLUMN experience_level            TEXT,
    ADD COLUMN avatar_id                   TEXT,
    ADD COLUMN training_days               TEXT,
    ADD COLUMN health_data_consent_version TEXT,
    ADD COLUMN health_data_consent_at      BIGINT,
    ADD CONSTRAINT user_profile_experience_level_ck
        CHECK (experience_level IS NULL OR experience_level IN ('NEW', 'SOME', 'EXPERIENCED')),
    ADD CONSTRAINT user_profile_training_days_ck
        CHECK (training_days IS NULL OR training_days IN ('ONE_TO_THREE', 'FOUR', 'FIVE_TO_SEVEN')),
    ADD CONSTRAINT user_profile_avatar_id_ck
        CHECK (avatar_id IS NULL OR avatar_id ~ '^[a-z0-9-]{1,32}$'),
    ADD CONSTRAINT user_profile_consent_version_ck
        CHECK (health_data_consent_version IS NULL OR length(health_data_consent_version) <= 32);
