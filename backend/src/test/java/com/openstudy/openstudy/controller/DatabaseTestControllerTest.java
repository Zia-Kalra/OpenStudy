package com.openstudy.openstudy.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;

class DatabaseTestControllerTest {

    @Test
    void verifiesConnectivityBeforeReturningTheSelectedDatabase() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
        when(jdbc.queryForObject("SELECT DATABASE()", String.class)).thenReturn("openstudy_db");

        assertThat(new DatabaseTestController(jdbc).testDatabase()).isEqualTo("openstudy_db");

        var queries = inOrder(jdbc);
        queries.verify(jdbc).queryForObject("SELECT 1", Integer.class);
        queries.verify(jdbc).queryForObject("SELECT DATABASE()", String.class);
        queries.verifyNoMoreInteractions();
    }

    @Test
    void connectionFailureCannotReturnDatabaseSuccess() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        var failure = new CannotGetJdbcConnectionException("Database unavailable");
        when(jdbc.queryForObject("SELECT 1", Integer.class)).thenThrow(failure);

        assertThatThrownBy(() -> new DatabaseTestController(jdbc).testDatabase()).isSameAs(failure);

        verify(jdbc).queryForObject("SELECT 1", Integer.class);
        verifyNoMoreInteractions(jdbc);
    }

    @Test
    void databaseSelectionFailureCannotReturnSuccessAfterConnectivityPasses() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
        var failure = new CannotGetJdbcConnectionException("Database unavailable");
        when(jdbc.queryForObject("SELECT DATABASE()", String.class)).thenThrow(failure);

        assertThatThrownBy(() -> new DatabaseTestController(jdbc).testDatabase()).isSameAs(failure);
    }
}
