// 下游（常青藤 weiran-cqt）第三方依赖版本清单（D-013，契约 §2.2）。上游永不创建本文件；存在时由 weiran-dependencies 自动 apply。
// 只登记框架 BOM 不管的业务依赖；框架已管理的依赖（Spring / Jackson 等）不得在这里约束，否则构建失败。
dependencies {
    constraints {
        // 阿里云短信官方 SDK（cqt-sms-aliyun）。传递依赖 tea / tea-openapi / tea-util 等随它解析。
        add("api", "com.aliyun:dysmsapi20170525:4.6.0")
        // 阿里云 OSS 官方 SDK（cqt-file-upload）。
        add("api", "com.aliyun.oss:aliyun-sdk-oss:3.18.5")
        // Excel 工作簿解析与模板生成（cqt-school-batch-import）。
        add("api", "org.apache.poi:poi-ooxml:5.5.1")
    }
}

extra["weiran.bizVersionDriftAllowlist"] = mapOf(
    "commons-codec:commons-codec" to
        "Apache POI 5.5.1 需要 1.20.0；模板生成与真实 XLSX 解析测试通过，接受其相对框架 BOM 1.18.0 的兼容升级",
)
