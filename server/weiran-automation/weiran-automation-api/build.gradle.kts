plugins {
    id("com.wulicode.weiran.java-conventions")
}

description = "weiran-automation 对外契约：自动化任务、执行记录与评审评论视图。"

weiranConventions {
    jacocoVerificationEnabled = false
}

dependencies {
    api(project(":weiran-framework"))
}
