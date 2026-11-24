plugins {
    id("com.wulicode.weiran.spring-conventions")
}

description = "weiran-automation 应用层：任务查询、启停与定时执行编排。"

weiranConventions {
    jacocoVerificationEnabled = false
}

dependencies {
    api(project(":weiran-automation-api"))
    api(project(":weiran-automation-domain"))
    api(project(":weiran-framework"))
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
}
