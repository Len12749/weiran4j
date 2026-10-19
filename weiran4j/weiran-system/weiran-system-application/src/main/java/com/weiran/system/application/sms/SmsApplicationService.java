package com.weiran.system.application.sms;

import com.weiran.system.api.sms.SmsSendCommand;
import com.weiran.system.api.sms.SmsSendResult;
import com.weiran.system.api.sms.SmsService;
import com.weiran.system.domain.sms.SmsSendOutcome;
import com.weiran.system.domain.sms.SmsSender;

/** system 短信发送用例。 */
public class SmsApplicationService implements SmsService {

    private final SmsSender smsSender;

    public SmsApplicationService(final SmsSender smsSender) {
        this.smsSender = smsSender;
    }

    @Override
    public SmsSendResult send(final SmsSendCommand command) {
        final SmsSendOutcome outcome =
                this.smsSender.send(command.phone(), command.templateCode(), command.templateParameters());
        return SmsSendResult.valueOf(outcome.name());
    }
}
