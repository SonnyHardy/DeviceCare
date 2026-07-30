-- V1__init_schema.sql
-- Initial DeviceCare schema, applied once by Flyway.
-- Neither Quarkus/Hibernate nor the Node ORM should create or alter
-- this schema: they only read it (in "validate" / introspection mode).

CREATE TABLE equipment (
                           id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                           name           VARCHAR(255) NOT NULL,
                           type           VARCHAR(100) NOT NULL,
                           serial_number  VARCHAR(100) NOT NULL UNIQUE,
                           status         VARCHAR(20) NOT NULL DEFAULT 'OPERATIONAL'
                               CHECK (status IN ('OPERATIONAL', 'IN_MAINTENANCE', 'DEFECTIVE', 'RETIRED')),
                           location       VARCHAR(255),
                           purchase_date  DATE,
                           created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
                           updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE maintenance_ticket (
                                    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                    equipment_id   UUID NOT NULL REFERENCES equipment(id) ON DELETE CASCADE,
                                    title          VARCHAR(255) NOT NULL,
                                    description    TEXT,
                                    priority       VARCHAR(20) NOT NULL DEFAULT 'MEDIUM'
                                        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
                                    status         VARCHAR(20) NOT NULL DEFAULT 'OPEN'
                                        CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED')),
                                    assigned_to    VARCHAR(255),
                                    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
                                    resolved_at    TIMESTAMPTZ
);

CREATE INDEX idx_ticket_equipment ON maintenance_ticket(equipment_id);
CREATE INDEX idx_ticket_status ON maintenance_ticket(status);