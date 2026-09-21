DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM users WHERE role = 'CASHIER' AND (company_id IS NULL OR branch_id IS NULL)) THEN
        RAISE EXCEPTION 'No se puede migrar: todos los CASHIER requieren company_id y branch_id';
    END IF;
    IF EXISTS (
        SELECT 1 FROM users u JOIN branches b ON b.id = u.branch_id
        WHERE u.branch_id IS NOT NULL AND u.company_id IS DISTINCT FROM b.company_id
    ) THEN
        RAISE EXCEPTION 'No se puede migrar: existen usuarios con local de otra empresa';
    END IF;
    IF EXISTS (
        SELECT 1 FROM users WHERE role = 'ADMIN' AND active GROUP BY company_id HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'No se puede migrar: existe mas de un ADMIN activo por empresa';
    END IF;
END $$;

UPDATE users SET role = 'OWNER' WHERE role = 'ADMIN';

ALTER TABLE users ALTER COLUMN company_id DROP NOT NULL;
ALTER TABLE branches ADD CONSTRAINT uq_branches_id_company UNIQUE (id, company_id);
ALTER TABLE users ADD CONSTRAINT fk_users_branch_company
    FOREIGN KEY (branch_id, company_id) REFERENCES branches(id, company_id);
ALTER TABLE users ADD CONSTRAINT ck_users_role_tenant CHECK (
    (role = 'PLATFORM_ADMIN' AND company_id IS NULL AND branch_id IS NULL)
    OR (role = 'OWNER' AND company_id IS NOT NULL)
    OR (role = 'CASHIER' AND company_id IS NOT NULL AND branch_id IS NOT NULL)
);
CREATE UNIQUE INDEX uq_users_active_owner_per_company
    ON users(company_id) WHERE role = 'OWNER' AND active;
