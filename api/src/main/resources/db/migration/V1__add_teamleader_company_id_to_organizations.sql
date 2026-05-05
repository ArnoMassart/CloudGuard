-- Persists Teamleader CRM company UUID per CloudGuard organization (see Organization.teamleaderCompanyId).
-- MySQL 8.0.12+ IF NOT EXISTS: safe if Hibernate ddl-auto already added the column, or on re-runs.
ALTER TABLE tbl_organizations
    ADD COLUMN IF NOT EXISTS teamleader_company_id VARCHAR(255) NULL;
