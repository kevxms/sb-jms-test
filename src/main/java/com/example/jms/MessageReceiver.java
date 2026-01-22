package com.example.jms;

import com.microsoft.azure.servicebus.jms.ServiceBusJmsConnectionFactory;
import com.microsoft.azure.servicebus.jms.ServiceBusJmsConnectionFactorySettings;

import javax.jms.*;

/**
 * Standalone message receiver for Azure Service Bus Queue
 */
public class MessageReceiver {

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

        // Receive timeout in milliseconds
        long receiveTimeout = 10000;

        ServiceBusJmsConnectionFactorySettings settings = new ServiceBusJmsConnectionFactorySettings();
        ConnectionFactory connectionFactory = new ServiceBusJmsConnectionFactory(connectionString, settings);

        try (Connection connection = connectionFactory.createConnection();
             Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {

            connection.start();

            Queue queue = session.createQueue(queueName);
            MessageConsumer consumer = session.createConsumer(queue);

            System.out.println("Waiting for messages from queue: " + queueName);
            System.out.println("Press Ctrl+C to exit\n");

            while (true) {
                Message message = consumer.receive(receiveTimeout);

                if (message instanceof TextMessage) {
                    TextMessage textMessage = (TextMessage) message;
                    System.out.println("Received: " + textMessage.getText());
                    System.out.println("  Message ID: " + message.getJMSMessageID());
                    System.out.println("  Timestamp: " + message.getJMSTimestamp());
                    System.out.println();
                } else if (message != null) {
                    System.out.println("Received non-text message: " + message.getClass().getName());
                } else {
                    System.out.println("No message received, waiting...");
                }
            }

        } catch (JMSException e) {
            System.err.println("Error receiving message: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
