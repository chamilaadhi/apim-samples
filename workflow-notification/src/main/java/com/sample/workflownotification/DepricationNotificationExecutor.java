package com.sample.workflownotification;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.WorkflowResponse;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.apimgt.api.model.Identifier;
import org.wso2.carbon.apimgt.api.model.Subscriber;
import org.wso2.carbon.apimgt.impl.dao.ApiMgtDAO;
import org.wso2.carbon.apimgt.impl.dto.WorkflowDTO;
import org.wso2.carbon.apimgt.impl.token.ClaimsRetriever;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.apimgt.impl.workflow.APIStateChangeSimpleWorkflowExecutor;
import org.wso2.carbon.apimgt.impl.workflow.APIStateWorkflowDTO;
import org.wso2.carbon.apimgt.impl.workflow.WorkflowException;

public class DepricationNotificationExecutor extends APIStateChangeSimpleWorkflowExecutor {

    private static final Log log = LogFactory.getLog(DepricationNotificationExecutor.class);

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
    private final String DEFAULT_SUBJECT = "API {API} Deprecated - Please Update";
    private final String DEFAULT_PAYLOAD = "<!DOCTYPE html>\n" + "<html>\n" + "<head>\n"
            + "    <title>API <b>{API}</b> Deprecated</title>\n" + "</head>\n" + "<body>\n"
            + "    <h1 style=\"color: #FF6347;\">API <b>{API}</b> is Deprecated</h1>\n"
            + "    <p style=\"font-size: 16px;\">Dear Developer,</p>\n"
            + "    <p style=\"font-size: 16px;\">This is to inform you that API <b>{API}</b> version <b>{API_VERSION}</b> has been deprecated. It is recommended to update to the latest version to ensure compatibility and security.</p>\n"
            + "    <p style=\"font-size: 16px;\">Please visit the <a href=\"" + portalUrl
            + "\" style=\"color: #007bff; text-decoration: none;\">Developer Portal</a> to get the latest version and migration guidelines.</p>\n"
            + "    <p style=\"font-size: 16px;\">Thank you for your attention to this matter.</p>\n"
            + "    <p style=\"font-size: 16px;\">Best regards,<br>Your GOGO API Team</p>\n" + "</body>\n" + "</html>";


    @Override
    public WorkflowResponse execute(WorkflowDTO workflowDTO) throws WorkflowException {
        Map<Integer, Integer> subscriberMap = new HashMap<Integer, Integer>();
        APIStateWorkflowDTO apiStateWorkFlowDTO = (APIStateWorkflowDTO) workflowDTO;
        Identifier identifier = new APIIdentifier(apiStateWorkFlowDTO.getApiProvider(),
                apiStateWorkFlowDTO.getApiName(), apiStateWorkFlowDTO.getApiVersion());
        EmailSender mailSender = new EmailSender(sender, senderPassword);
        if ("PUBLISHED".equals(apiStateWorkFlowDTO.getApiCurrentState())
                && "Deprecate".equals(apiStateWorkFlowDTO.getApiLCAction())) {

            try {
                Set<Subscriber> subscribersOfAPI = apiMgtDAO.getSubscribersOfAPIWithoutDuplicates(identifier,
                        subscriberMap);
                ClaimsRetriever claimsRetriever = getClaimsRetriever("org.wso2.carbon.apimgt.impl.token.DefaultClaimsRetriever");
                claimsRetriever.init();
                Set<String> emailset = new HashSet<String>();
                for (Subscriber subscriber : subscribersOfAPI) {
                    String tenantUserName = subscriber.getName();

                    String email = claimsRetriever.getClaims(tenantUserName).get("http://wso2.org/claims/emailaddress");

                    if (email != null && !email.isEmpty()) {
                        emailset.add(email);
                    }
                }
                String subject = DEFAULT_SUBJECT;
                if (portalUrl == null) {
                    portalUrl = "https://localhost:9443/devportal/apis";
                }

                String emailPayload = DEFAULT_PAYLOAD;
                subject = subject.replace("{API}", identifier.getName());
                emailPayload = emailPayload.replace("{API}", identifier.getName()).replace("{API_VERSION}",
                        identifier.getVersion());
                if (!emailset.isEmpty()) {
                    mailSender.sendMail(emailset, emailPayload, subject);
                }

            } catch (APIManagementException | IllegalAccessException | InstantiationException
                    | ClassNotFoundException e) {
                log.error("Error while sending notification to the subscribers ", e);
            }
        }

        return super.execute(workflowDTO);
    }

    protected ClaimsRetriever getClaimsRetriever(String claimsRetrieverImplClass)
            throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        return (ClaimsRetriever) APIUtil.getClassInstance(claimsRetrieverImplClass);
    }

}
