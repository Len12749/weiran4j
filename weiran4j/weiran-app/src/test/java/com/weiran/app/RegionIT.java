package com.weiran.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** 行政区划模块：真实 MySQL 迁移、权限、CRUD 与操作审计。 */
class RegionIT extends IntegrationTestSupport {

    private static final String USER_PASSWORD = "region-test-pass1";

    @Test
    @DisplayName("固定快照完整，登录用户可 lookup，无地区权限用户不能访问管理树")
    void snapshotLookupAndPermissions() {
        final Map<String, Integer> counts =
                this.jdbc.query("SELECT level, COUNT(*) AS total FROM region GROUP BY level", rows -> {
                    final Map<String, Integer> result = new HashMap<>();
                    while (rows.next()) {
                        result.put(rows.getString("level"), rows.getInt("total"));
                    }
                    return result;
                });
        assertThat(counts).containsExactlyInAnyOrderEntriesOf(Map.of("province", 31, "city", 342, "county", 2978));
        assertThat(this.jdbc.queryForObject("SELECT COUNT(DISTINCT code) FROM region", Integer.class))
                .isEqualTo(3351);

        IntegrationTestSupport.assertError(this.get("/api/regions/lookup", null), HttpStatus.UNAUTHORIZED, 40100);
        final String admin = this.adminToken();
        final JsonNode lookup = IntegrationTestSupport.data(this.get("/api/regions/lookup", admin));
        assertThat(lookup).hasSize(31);
        assertThat(lookup.findValuesAsText("code")).contains("11", "110101");

        final String username = IntegrationTestSupport.unique("region_viewer");
        final Map<String, Object> user = new HashMap<>();
        user.put("username", username);
        user.put("nickname", "行政区划只读测试");
        user.put("password", RegionIT.USER_PASSWORD);
        user.put("departmentId", 1);
        user.put("roleIds", List.of());
        this.create("/api/users", admin, user);
        final String viewer = this.tokenOf(username, RegionIT.USER_PASSWORD);

        IntegrationTestSupport.assertOk(this.get("/api/regions/lookup", viewer));
        IntegrationTestSupport.assertError(this.get("/api/regions/tree", viewer), HttpStatus.FORBIDDEN, 40300);
    }

    @Test
    @DisplayName("地区 CRUD 校验父子关系、拒绝修改 code 与删除非叶节点，并记录写操作审计")
    void regionCrudAndAudit() {
        final String admin = this.adminToken();
        final String provinceCode = RegionIT.uniqueCode();
        final long provinceId = this.create(
                "/api/regions", admin, Map.of("code", provinceCode, "name", "集成测试省", "level", "province", "sort", 99));
        final long cityId = this.create(
                "/api/regions",
                admin,
                Map.of("code", provinceCode + "01", "name", "集成测试市", "level", "city", "parentCode", provinceCode));

        IntegrationTestSupport.assertError(
                this.delete("/api/regions/" + provinceId, admin), HttpStatus.CONFLICT, 40921);
        IntegrationTestSupport.assertError(
                this.put("/api/regions/" + provinceId, admin, Map.of("code", provinceCode + "99")),
                HttpStatus.CONFLICT,
                40901);
        IntegrationTestSupport.assertOk(
                this.put("/api/regions/" + provinceId, admin, Map.of("name", "修改后的集成测试省", "status", "disabled")));
        final JsonNode updated = IntegrationTestSupport.data(this.get("/api/regions/" + provinceId, admin));
        assertThat(updated.path("code").asText()).isEqualTo(provinceCode);
        assertThat(updated.path("name").asText()).isEqualTo("修改后的集成测试省");
        assertThat(updated.path("status").asText()).isEqualTo("disabled");

        IntegrationTestSupport.assertOk(this.delete("/api/regions/" + cityId, admin));
        IntegrationTestSupport.assertOk(this.delete("/api/regions/" + provinceId, admin));
        await().atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> assertThat(this.jdbc.queryForObject(
                                "SELECT COUNT(*) FROM sys_operation_log WHERE module = ? AND path LIKE ? AND description IN (?, ?, ?)",
                                Integer.class,
                                "行政区划管理",
                                "/api/regions%",
                                "新增地区",
                                "修改地区",
                                "删除地区"))
                        .isGreaterThanOrEqualTo(5));
    }

    private static String uniqueCode() {
        return "99" + String.format("%06d", Math.floorMod(System.nanoTime(), 1_000_000L));
    }
}
