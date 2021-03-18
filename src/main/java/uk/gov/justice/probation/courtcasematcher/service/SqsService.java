package uk.gov.justice.probation.courtcasematcher.service;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Profile("sqs-messaging")
@AllArgsConstructor
@NoArgsConstructor
@Component
public class SqsService {

    @Value("${aws_sqs_queue_name:crime-portal-gateway-queue}")
    private String queueName;

    @Autowired
    private AmazonSQSAsync amazonSQSAsync;

    public boolean isQueueAvailable() {
        try {
            GetQueueUrlResult queueUrlResult = amazonSQSAsync.getQueueUrl(queueName);
            return queueUrlResult != null && !ObjectUtils.isEmpty(queueUrlResult.getQueueUrl());
        }
        catch (QueueDoesNotExistException existException) {
            return false;
        }
    }

}
