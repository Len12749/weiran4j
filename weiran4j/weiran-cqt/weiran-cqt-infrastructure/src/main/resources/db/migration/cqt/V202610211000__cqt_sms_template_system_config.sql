insert into sys_config (
    config_key,
    config_value,
    config_type,
    description,
    is_builtin,
    group_key,
    field_type,
    required
)
values (
    'system.user.cqt_sms_template_code',
    '',
    'string',
    '常青藤前台验证码短信模板 CODE（需在短信服务商审核通过）',
    1,
    'user',
    'input',
    0
);
