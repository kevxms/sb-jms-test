package com.example.jms;

import com.microsoft.azure.servicebus.jms.ServiceBusJmsConnectionFactory;
import com.microsoft.azure.servicebus.jms.ServiceBusJmsConnectionFactorySettings;

import javax.jms.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JMS Test Client for Azure Service Bus Queue
 * 
 * This test client:
 * - Connects to Azure Service Bus using JMS
 * - Sends test messages to a queue
 * - Receives messages from a queue
 * - Asserts that all sent messages are received
 */
public class ServiceBusJmsClientTest {

    private final String connectionString;
    private final String queueName;
    private ConnectionFactory connectionFactory;
    private Connection connection;

    /**
     * Creates a new ServiceBusJmsClientTest
     * 
     * @param connectionString The Azure Service Bus connection string
     * @param queueName        The name of the queue to interact with
     */
    public ServiceBusJmsClientTest(String connectionString, String queueName) {
        this.connectionString = connectionString;
        this.queueName = queueName;
    }

    /**
     * Initializes the JMS connection factory and connection
     */
    public void initialize() throws JMSException {
        ServiceBusJmsConnectionFactorySettings settings = new ServiceBusJmsConnectionFactorySettings();
        settings.setConnectionIdleTimeoutMS(20000);

        connectionFactory = new ServiceBusJmsConnectionFactory(connectionString, settings);
        connection = connectionFactory.createConnection();
        connection.start();

        System.out.println("Successfully connected to Azure Service Bus");
    }

    /**
     * Sends a text message to the queue
     * 
     * @param messageText The text content of the message
     */
    public void sendMessage(String messageText) throws JMSException {
        try (Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
            Queue queue = session.createQueue(queueName);
            MessageProducer producer = session.createProducer(queue);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);

            TextMessage message = session.createTextMessage(messageText);
            producer.send(message);

            System.out.println("Sent message: " + messageText);
        }
    }

    /**
     * Sends a text message with custom properties to the queue
     * 
     * @param messageText The text content of the message
     * @param messageId   Custom message ID
     * @param priority    Message priority (0-9)
     */
    public void sendMessageWithProperties(String messageText, String messageId, int priority) throws JMSException {
        try (Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
            Queue queue = session.createQueue(queueName);
            MessageProducer producer = session.createProducer(queue);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            producer.setPriority(priority);

            TextMessage message = session.createTextMessage(messageText);
            message.setJMSMessageID(messageId);
            message.setStringProperty("CustomProperty", "CustomValue");

            producer.send(message);

            System.out.println("Sent message with ID: " + messageId);
        }
    }

    /**
     * Receives a single message from the queue (blocking with timeout)
     * 
     * @param timeoutMs Timeout in milliseconds
     * @return The received message text, or null if no message available
     */
    public String receiveMessage(long timeoutMs) throws JMSException {
        try (Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
            Queue queue = session.createQueue(queueName);
            MessageConsumer consumer = session.createConsumer(queue);

            Message message = consumer.receive(timeoutMs);

            if (message instanceof TextMessage) {
                String text = ((TextMessage) message).getText();
                System.out.println("Received message: " + text);
                return text;
            } else if (message != null) {
                System.out.println("Received non-text message: " + message.getClass().getName());
                return message.toString();
            }

            System.out.println("No message received within timeout");
            return null;
        }
    }

    /**
     * Sets up an asynchronous message listener
     * 
     * @param listener The message listener to handle incoming messages
     * @return The session (keep reference to close it later)
     */
    public Session setupAsyncReceiver(MessageListener listener) throws JMSException {
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Queue queue = session.createQueue(queueName);
        MessageConsumer consumer = session.createConsumer(queue);
        consumer.setMessageListener(listener);

        System.out.println("Async message listener set up for queue: " + queueName);
        return session;
    }

    /**
     * Closes the connection and releases resources
     */
    public void close() {
        try {
            if (connection != null) {
                connection.close();
                System.out.println("Connection closed");
            }
        } catch (JMSException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }

    /**
     * Redacts sensitive information from the connection string for logging
     * 
     * @param connectionString The original connection string
     * @return The connection string with secrets redacted
     */
    private static String redactConnectionString(String connectionString) {
        if (connectionString == null) {
            return null;
        }
        // Redact SharedAccessKey value only
        return connectionString.replaceAll(
            "(SharedAccessKey=)[^;]+", 
            "$1***REDACTED***"
        );
    }

    /**
     * Main method demonstrating usage of the JMS client
     */
    public static void main(String[] args) {
        // Parse CLI arguments
        String cliConnectionString = null;
        String cliQueueName = null;
        
        for (int i = 0; i < args.length; i++) {
            if (("--connection-string".equals(args[i]) || "-c".equals(args[i])) && i + 1 < args.length) {
                cliConnectionString = args[++i];
            } else if (("--queue-name".equals(args[i]) || "-q".equals(args[i])) && i + 1 < args.length) {
                cliQueueName = args[++i];
            } else if ("--help".equals(args[i]) || "-h".equals(args[i])) {
                System.out.println("Usage: java -jar azure-servicebus-jms-client-test.jar [options]");
                System.out.println("Options:");
                System.out.println("  -c, --connection-string <string>  Service Bus connection string (overrides SERVICEBUS_CONNECTION_STRING env var)");
                System.out.println("  -q, --queue-name <name>           Queue name (overrides SERVICEBUS_QUEUE_NAME env var)");
                System.out.println("  -h, --help                        Show this help message");
                System.exit(0);
            }
        }

        // Determine connection string source
        String connectionString;
        String connectionStringSource;
        if (cliConnectionString != null && !cliConnectionString.isEmpty()) {
            connectionString = cliConnectionString;
            connectionStringSource = "CLI argument";
        } else {
            connectionString = System.getenv("SERVICEBUS_CONNECTION_STRING");
            connectionStringSource = "environment variable";
        }

        // Determine queue name source
        String queueName;
        String queueNameSource;
        if (cliQueueName != null && !cliQueueName.isEmpty()) {
            queueName = cliQueueName;
            queueNameSource = "CLI argument";
        } else {
            queueName = System.getenv("SERVICEBUS_QUEUE_NAME");
            queueNameSource = "environment variable";
        }

        // Validate connection string
        if (connectionString == null || connectionString.isEmpty()) {
            System.err.println("Please provide the connection string via CLI argument or SERVICEBUS_CONNECTION_STRING environment variable");
            System.err.println("Usage: java -jar azure-servicebus-jms-client-test.jar -c <connection-string> -q <queue-name>");
            System.err.println("Format: Endpoint=sb://<namespace>.servicebus.windows.net/;SharedAccessKeyName=<keyname>;SharedAccessKey=<key>");
            System.exit(1);
        }

        // Use default queue name if not specified
        if (queueName == null || queueName.isEmpty()) {
            queueName = "test-queue";
            queueNameSource = "default";
        }

        // Log configuration (with secrets redacted)
        System.out.println("=== Configuration ===");
        System.out.println("Connection String (" + connectionStringSource + "): " + redactConnectionString(connectionString));
        System.out.println("Queue Name (" + queueNameSource + "): " + queueName);
        System.out.println("=====================");

        ServiceBusJmsClientTest client = new ServiceBusJmsClientTest(connectionString, queueName);

        try {
            // Initialize connection
            client.initialize();

            // Track sent messages
            List<String> sentMessages = new ArrayList<>();
            
            // Send test messages
            String msg1 = "A";
            String msg2 = "B";
            
            client.sendMessage(msg1);
            sentMessages.add(msg1);
            
            client.sendMessage(msg2);
            sentMessages.add(msg2);

            System.out.println("\n--- Test Summary ---");
            System.out.println("Sent " + sentMessages.size() + " messages");

            // Receive messages
            System.out.println("\n--- Receiving Messages ---");
            List<String> receivedMessages = new ArrayList<>();
            String receivedMessage;
            while ((receivedMessage = client.receiveMessage(5000)) != null) {
                System.out.println("Processed: " + receivedMessage);
                receivedMessages.add(receivedMessage);
            }

            // Assert all sent messages were received
            System.out.println("\n=== Test Results ===");
            System.out.println("Messages Sent: " + sentMessages.size());
            System.out.println("Messages Received: " + receivedMessages.size());
            
            boolean allReceived = true;
            for (String sentMsg : sentMessages) {
                if (!receivedMessages.contains(sentMsg)) {
                    System.err.println("FAILED: Message not received: " + sentMsg);
                    allReceived = false;
                }
            }
            
            if (allReceived && receivedMessages.size() == sentMessages.size()) {
                System.out.println("TEST PASSED: All sent messages were received!");
                System.exit(0);
            } else {
                System.err.println("TEST FAILED: Not all messages were received or extra messages found");
                System.exit(1);
            }

        } catch (JMSException e) {
            System.err.println("JMS Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            client.close();
        }
    }
}
