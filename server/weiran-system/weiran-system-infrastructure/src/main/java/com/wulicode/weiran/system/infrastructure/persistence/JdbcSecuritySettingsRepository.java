package com.wulicode.weiran.system.infrastructure.persistence;

import com.wulicode.weiran.system.domain.user.SecuritySettings;
import com.wulicode.weiran.system.domain.user.SecuritySettingsRepository;
import org.springframework.jdbc.core.JdbcTemplate;

/** 单行全局安全设置。 */
public class JdbcSecuritySettingsRepository implements SecuritySettingsRepository {

    private final JdbcTemplate jdbc;

    public JdbcSecuritySettingsRepository(final JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public SecuritySettings load() {
        return java.util.Objects.requireNonNull(this.jdbc.queryForObject(
                "select password_min_length, password_max_length, require_lower, require_upper, require_digit, "
                        + "require_symbol, mfa_enabled, mfa_remember_days, revision from sys_security_settings where id = 1",
                (rs, row) -> new SecuritySettings(
                        rs.getInt("password_min_length"),
                        rs.getInt("password_max_length"),
                        rs.getBoolean("require_lower"),
                        rs.getBoolean("require_upper"),
                        rs.getBoolean("require_digit"),
                        rs.getBoolean("require_symbol"),
                        rs.getBoolean("mfa_enabled"),
                        rs.getInt("mfa_remember_days"),
                        rs.getLong("revision"))));
    }

    @Override
    public boolean save(final SecuritySettings settings) {
        return this.jdbc.update(
                        "update sys_security_settings set password_min_length = ?, password_max_length = ?, "
                                + "require_lower = ?, require_upper = ?, require_digit = ?, require_symbol = ?, "
                                + "mfa_enabled = ?, mfa_remember_days = ?, revision = revision + 1 "
                                + "where id = 1 and revision = ?",
                        settings.passwordMinLength(),
                        settings.passwordMaxLength(),
                        settings.requireLower(),
                        settings.requireUpper(),
                        settings.requireDigit(),
                        settings.requireSymbol(),
                        settings.mfaEnabled(),
                        settings.mfaRememberDays(),
                        settings.revision())
                == 1;
    }
}
