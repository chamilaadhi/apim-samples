
# Custom Notification Workflows for WSO2 API Manager

This project contains the following custom workflows to send notifications:

## Email Notification for Subscribing to an API

Configure the Subscription workflow with the sender's email and password:

<SubscriptionCreation executor="com.sample.workflownotification.SubscriptionNotificationExecutor">
    <Property name="sender">xxxxxxx</Property>
    <Property name="senderPassword">xxxxxxx</Property>
</SubscriptionCreation>

Note: Subscriber user needs to have an email address in his profile. 

## Email Notification for Deprecating an API

Configure the APIStateChange workflow with the sender's email and password:

<APIStateChange executor="com.sample.workflownotification.DepricationNotificationExecutor">
    <Property name="sender">xxxxxxx</Property>
    <Property name="senderPassword">xxxxxxx</Property>
</APIStateChange>

Note: Email is send to the api business owner email address. When creating the API, add email address to the api business owner email address.