# 配置与赛事单图字段收尾

用户2026-10-18明确追加，任务6.1–6.3。站点两图片键和赛事三模板复用SingleImageUpload；disabled为组件可选扩展，上传统一计数与保存互斥。历史值回显、移除清空、原图预览保留。POST competitions/images复用create/update ANY及FileUploadService，10MB JPG/PNG/GIF；无新迁移/依赖。local返回/uploads/采用严格路径校验，不拼localhost origin。普通文字/富文本/附件/视频字段不变。

前端由reviewer、后端由auth_audit非作者独立静态复核通过。后端JDK21 spotlessApply assemble compileTestJava checkstyleMain spotbugsMain forbiddenApisMain通过，118任务，/private/tmp/cqt-single-image-static.log；前端tsc/eslint/Vite通过，/private/tmp/cqt-single-images-{tsc,eslint,vite}.log。未新增或运行自动化测试，无完整L7通过声明；全仓openspec仅余前序review change三份证据过期，本change结构无错误，未伪造新测试证据。

浏览器在已存在的服务上只读查看新增赛事三模板上传区及站点首页素材两上传控件，布局正常；未上传文件或保存业务数据，临时检查tab已关闭。本轮未启动或重启任何服务。用户前次停端口后出现的新PID57227/57258/57321非本轮启动，未擅自停止。
