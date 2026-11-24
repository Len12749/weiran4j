plugins {
    id("com.wulicode.weiran.java-conventions")
}

description = "weiran-automation 领域层：任务定义、执行记录、评审结果与外部端口。"

weiranConventions {
    jacocoLineMinimum = "0.60"
}

dependencies {
    api(project(":weiran-framework"))
}
