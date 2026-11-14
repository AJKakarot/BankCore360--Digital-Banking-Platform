package com.bankcore360.migration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationValidationTest {

    @Test
    @DisplayName("V1 baseline migration script must exist and contain DDL for all core tables")
    void shouldValidateV1BaselineScript() throws Exception {
        ClassPathResource resource = new ClassPathResource("db/migration/V1__baseline_schema.sql");
        assertThat(resource.exists()).isTrue();

        try (InputStream is = resource.getInputStream()) {
            String sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(sql).contains("CREATE TABLE IF NOT EXISTS app_users");
            assertThat(sql).contains("CREATE TABLE IF NOT EXISTS refresh_tokens");
            assertThat(sql).contains("CREATE TABLE IF NOT EXISTS ledger_accounts");
            assertThat(sql).contains("CREATE TABLE IF NOT EXISTS ledger_transactions");
            assertThat(sql).contains("CREATE TABLE IF NOT EXISTS ledger_entries");
            assertThat(sql).contains("CREATE TABLE IF NOT EXISTS account_balances");
            assertThat(sql).contains("CREATE TABLE IF NOT EXISTS transaction_reversals");
            assertThat(sql).contains("CREATE TABLE IF NOT EXISTS fraud_assessments");
            assertThat(sql).contains("CREATE TABLE IF NOT EXISTS bank_statements");
            assertThat(sql).contains("CREATE TABLE IF NOT EXISTS outbox_events");
        }
    }

    @Test
    @DisplayName("V2 immutability trigger script must exist and contain BEFORE UPDATE OR DELETE triggers")
    void shouldValidateV2TriggerScript() throws Exception {
        ClassPathResource resource = new ClassPathResource("db/migration/V2__ledger_immutability_trigger.sql");
        assertThat(resource.exists()).isTrue();

        try (InputStream is = resource.getInputStream()) {
            String sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(sql).contains("prevent_ledger_entry_mutation()");
            assertThat(sql).contains("trg_ledger_entries_immutable");
            assertThat(sql).contains("BEFORE UPDATE OR DELETE ON ledger_entries");
            assertThat(sql).contains("trg_transaction_reversals_immutable");
        }
    }
}

