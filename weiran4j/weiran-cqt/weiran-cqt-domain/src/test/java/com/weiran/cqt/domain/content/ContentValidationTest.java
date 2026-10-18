package com.weiran.cqt.domain.content;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.weiran.framework.error.BizException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ContentValidationTest {
    @Test
    @DisplayName("只允许本地路径或绝对HTTP地址，拒绝脚本和协议相对地址")
    void acceptsOnlySafeUrls() {
        for (final String url : new String[] {"/uploads/a.png", "https://example.com/a.png", "http://example.com/a"}) {
            assertThatCode(() -> ContentValidation.url(url, true)).doesNotThrowAnyException();
        }
        for (final String url : new String[] {
            "javascript:alert(1)",
            "//evil.test/a",
            "ftp://example.com/a",
            "https:/missing-host",
            "/bad\\path",
            " /a",
            "http://[",
            "a.png"
        }) {
            assertThatThrownBy(() -> ContentValidation.url(url, true)).isInstanceOf(BizException.class);
        }
        assertThatCode(() -> ContentValidation.url("", false)).doesNotThrowAnyException();
        assertThatThrownBy(() -> ContentValidation.url("", true)).isInstanceOf(BizException.class);
        assertThatThrownBy(() -> ContentValidation.url("/" + "a".repeat(1024), true))
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("必填和长度边界校验")
    void checksRequiredAndLength() {
        assertThatCode(() -> ContentValidation.required("x", "标题", 1)).doesNotThrowAnyException();
        assertThatThrownBy(() -> ContentValidation.required(" ", "标题", 1)).isInstanceOf(BizException.class);
        assertThatThrownBy(() -> ContentValidation.required("xx", "标题", 1)).isInstanceOf(BizException.class);
        assertThatCode(() -> ContentValidation.optional("", "作者", 1)).doesNotThrowAnyException();
        assertThatThrownBy(() -> ContentValidation.optional("xx", "作者", 1)).isInstanceOf(BizException.class);
    }
}
