update sys_config
set description = '单文件上传 · file'
where config_key = 'system.demo.image';

update sys_config
set description = '多文件上传 · FileUpload'
where config_key = 'system.demo.images';
