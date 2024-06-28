package com.sample.workflownotification;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.WorkflowResponse;
import org.wso2.carbon.apimgt.api.model.API;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.apimgt.impl.dao.ApiMgtDAO;
import org.wso2.carbon.apimgt.impl.dto.SubscriptionWorkflowDTO;
import org.wso2.carbon.apimgt.impl.dto.WorkflowDTO;
import org.wso2.carbon.apimgt.impl.factory.PersistenceFactory;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.apimgt.impl.workflow.SubscriptionCreationSimpleWorkflowExecutor;
import org.wso2.carbon.apimgt.impl.workflow.WorkflowException;
import org.wso2.carbon.apimgt.persistence.APIPersistence;
import org.wso2.carbon.apimgt.persistence.dto.Organization;
import org.wso2.carbon.apimgt.persistence.dto.PublisherAPI;
import org.wso2.carbon.apimgt.persistence.exceptions.APIPersistenceException;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

public class SubscriptionNotificationExecutor extends SubscriptionCreationSimpleWorkflowExecutor {

    private static final Log log = LogFactory.getLog(SubscriptionNotificationExecutor.class);
    private static final ApiMgtDAO apiMgtDAO = ApiMgtDAO.getInstance();;
    
    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getSenderPassword() {
        return senderPassword;
    }

    public void setSenderPassword(String senderPassword) {
        this.senderPassword = senderPassword;
    }

    public String sender;
    public String senderPassword;
    public String portalUrl = "https://localhost:9443/devportal";
    private final String DEFAULT_SUBJECT = "New API Subscription Notification for API {API}";
    private final String DEFAULT_PAYLOAD = 
            "<!DOCTYPE html>\n"
            + "<html>\n"
            + "<head>\n"
            + "    <title>New API Subscription Notification</title>\n"
            + "</head>\n"
            + "<body>\n"
            + "    <h1 style=\"color: #4CAF50;\">New Subscription to API <b>{API}</b></h1>\n"
            + "    <p style=\"font-size: 16px;\">Dear API Owner,</p>\n"
            + "    <p style=\"font-size: 16px;\">We are pleased to inform you that a new user has subscribed to your API <b>{API}</b> version <b>{API_VERSION}</b>.</p>\n"
            + "    <p style=\"font-size: 16px;\">User Details:</p>\n"
            + "    <ul style=\"font-size: 16px;\">\n"
            + "        <li><b>Name:</b> {USER_NAME}</li>\n"
            + "        <li><b>Business Plan:</b> {BUSINESS_PLAN}</li>\n"
            + "        <li><b>Subscription Date:</b> {SUBSCRIPTION_DATE}</li>\n"
            + "    </ul>\n"
            + "    <p style=\"font-size: 16px;\">For more details, please visit the <a href=\"" + portalUrl + "\" style=\"color: #007bff; text-decoration: none;\">Developer Portal</a>.</p>\n"
            + "    <p style=\"font-size: 16px;\">Thank you for providing valuable API services.</p>\n"
            + "    <p style=\"font-size: 16px;\">Best regards,<br>Your GOGO API Team</p>\n"
            + "</body>\n"
            + "</html>";


    
    @Override
    public WorkflowResponse execute(WorkflowDTO workflowDTO) throws WorkflowException {
        SubscriptionWorkflowDTO subscriptionWFDto = (SubscriptionWorkflowDTO) workflowDTO;
        APIIdentifier identifier = new APIIdentifier(subscriptionWFDto.getApiProvider(),
                subscriptionWFDto.getApiName(), subscriptionWFDto.getApiVersion());
        String tenantDomain = MultitenantUtils
                .getTenantDomain(APIUtil.replaceEmailDomainBack(subscriptionWFDto.getApiProvider()));
        API apiInfo;
        try {
            apiInfo = apiMgtDAO.getLightWeightAPIInfoByAPIIdentifier(identifier, tenantDomain);
            APIPersistence apiPersistenceInstance = PersistenceFactory.getAPIPersistenceInstance();

            PublisherAPI api = apiPersistenceInstance.getPublisherAPI(new Organization(tenantDomain), apiInfo.getUuid());
            String businessOwnerMail = api.getBusinessOwnerEmail();
            
            if (businessOwnerMail != null) {
                EmailSender mailSender = new EmailSender(sender, senderPassword);
                String subject = DEFAULT_SUBJECT.replace("{API}", subscriptionWFDto.getApiName());
                String payload = DEFAULT_PAYLOAD
                        .replace("{API_VERSION}", subscriptionWFDto.getApiVersion())
                        .replace("{API}", subscriptionWFDto.getApiName())
                        .replace("{USER_NAME}", subscriptionWFDto.getSubscriber())
                        .replace("{BUSINESS_PLAN}", subscriptionWFDto.getTierName())
                        .replace("{API}", subscriptionWFDto.getApiName())
                        .replace("{SUBSCRIPTION_DATE}", LocalDate.now().toString());

                Set<String> mails = new HashSet<String>();
                mails.add(businessOwnerMail);
                mailSender.sendMail(mails, payload, subject);
            }
        } catch (APIManagementException | APIPersistenceException e) {
            log.error("Error while sending notification for api owners" , e);
        }

        return super.execute(workflowDTO);
    }
 
}
