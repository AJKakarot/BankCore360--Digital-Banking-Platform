package com.bankcore360.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

@Component
public class DatabaseLedgerImmutabilityEnforcer {

    private static final Logger log = LoggerFactory.getLogger(DatabaseLedgerImmutabilityEnforcer.class);

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public DatabaseLedgerImmutabilityEnforcer(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    /**
     * Enforces immutability at the physical database engine level (PostgreSQL).
     * Creates a database trigger that rejects any direct UPDATE or DELETE query on ledger_entries.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void enforceDatabaseLevelImmutability() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String dbProductName = metaData.getDatabaseProductName();

            if (dbProductName != null && dbProductName.toLowerCase().contains("postgres")) {
                log.info("🛡️ Enforcing database-level immutability trigger on PostgreSQL table 'ledger_entries'...");

                // 1. Create immutability prevention function
                String functionSql = """
                    CREATE OR REPLACE FUNCTION prevent_ledger_entry_mutation()
                    RETURNS TRIGGER AS $$
                    BEGIN
                        RAISE EXCEPTION 'IMMUTABLE_LEDGER_VIOLATION: Posted ledger entries cannot be modified or deleted!';
                    END;
                    $$ LANGUAGE plpgsql;
                """;
                jdbcTemplate.execute(functionSql);

                // 2. Drop existing trigger if present to ensure idempotent re-creation
                jdbcTemplate.execute("DROP TRIGGER IF EXISTS trg_prevent_ledger_entries_mutation ON ledger_entries");

                // 3. Attach BEFORE UPDATE OR DELETE trigger
                String triggerSql = """
                    CREATE TRIGGER trg_prevent_ledger_entries_mutation
                    BEFORE UPDATE OR DELETE ON ledger_entries
                    FOR EACH ROW EXECUTE FUNCTION prevent_ledger_entry_mutation();
                """;
                jdbcTemplate.execute(triggerSql);

                log.info("✅ Database-level trigger successfully applied: Direct SQL UPDATE/DELETE on 'ledger_entries' is strictly blocked.");
            } else {
                log.info("Non-PostgreSQL database detected ({}). Relying on JPA immutability annotations.", dbProductName);
            }
        } catch (Exception e) {
            log.warn("Could not attach database immutability trigger (may already exist or DB is offline): {}", e.getMessage());
        }
    }
}

