package com.example.jms;

import com.microsoft.azure.servicebus.jms.ServiceBusJmsConnectionFactory;
import com.microsoft.azure.servicebus.jms.ServiceBusJmsConnectionFactorySettings;

import javax.jms.*;

/**
 * Standalone message sender for Azure Service Bus Queue
 */
public class MessageSender {

    public static void main(String[] args) {
        String connectionString = System.getenv("SERVICEBUS_CONNECTION_STRING");
        String queueName = System.getenv("SERVICEBUS_QUEUE_NAME");

        if (connectionString == null || connectionString.isEmpty()) {
            System.err.println("Please set SERVICEBUS_CONNECTION_STRING environment variable");
            System.exit(1);
        }

        if (queueName == null || queueName.isEmpty()) {
            queueName = "test-queue";
        }

        // Get message from command line or use default
        String messageText = args.length > 0 ? args[0] : "Hello from Azure Service Bus JMS!";

        ServiceBusJmsConnectionFactorySettings settings = new ServiceBusJmsConnectionFactorySettings();
        ConnectionFactory connectionFactory = new ServiceBusJmsConnectionFactory(connectionString, settings);

        try (Connection connection = connectionFactory.createConnection();
             Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {

            connection.start();

            Queue queue = session.createQueue(queueName);
            MessageProducer producer = session.createProducer(queue);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);

            TextMessage message = session.createTextMessage(messageText);
            producer.send(message);

            System.out.println("Message sent successfully: " + messageText);

        } catch (JMSException e) {
            System.err.println("Error sending message: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
