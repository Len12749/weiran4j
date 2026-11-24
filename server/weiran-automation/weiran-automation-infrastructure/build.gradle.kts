plugins {
    id("com.wulicode.weiran.spring-conventions")
}

description = "weiran-automation 基础设施层：持久化、云效 OpenAPI 客户端与知识库评审 Agent。"

weiranConventions {
    jacocoVerificationEnabled = false
}

dependencies {
    api(project(":weiran-automation-domain"))
    implementation(project(":weiran-framework"))
    implementation("com.baomidou:mybatis-plus-spring-boot3-starter")
    implementation("org.springframework:spring-web")
    implementation("org.springframework.boot:spring-boot")
    implementation("com.fasterxml.jackson.core:jackson-databind")
}
