package com.example.jms;

import com.microsoft.azure.servicebus.jms.ServiceBusJmsConnectionFactory;
import com.microsoft.azure.servicebus.jms.ServiceBusJmsConnectionFactorySettings;

import javax.jms.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JMS Test Client for Azure Service Bus Queue/Topic
 * 
 * This test client:
 * - Connects to Azure Service Bus using JMS
 * - Sends test messages to a queue or topic
 * - Receives messages from a queue or topic subscription
 * - Asserts that all sent messages are received
 */
public class ServiceBusJmsClientTest {

    private final String connectionString;
    private final String destinationName;
    private final boolean useTopic;
    private final String subscriptionName;
    private ConnectionFactory connectionFactory;
    private Connection connection;

    /**
     * Creates a new ServiceBusJmsClientTest for queues
     * 
     * @param connectionString The Azure Service Bus connection string
     * @param queueName        The name of the queue to interact with
     */
    public ServiceBusJmsClientTest(String connectionString, String queueName) {
        this(connectionString, queueName, false, null);
    }

    /**
     * Creates a new ServiceBusJmsClientTest for queues or topics
     * 
     * @param connectionString  The Azure Service Bus connection string
     * @param destinationName   The name of the queue or topic to interact with
     * @param useTopic          True to use topic, false to use queue
     * @param subscriptionName  The subscription name (required for topics)
     */
    public ServiceBusJmsClientTest(String connectionString, String destinationName, boolean useTopic, String subscriptionName) {
        this.connectionString = connectionString;
        this.destinationName = destinationName;
        this.useTopic = useTopic;
        this.subscriptionName = subscriptionName;
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
     * Sends a text message to the queue or topic
     * 
     * @param messageText The text content of the message
     */
    public void sendMessage(String messageText) throws JMSException {
        try (Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
            Destination destination = useTopic ? session.createTopic(destinationName) : session.createQueue(destinationName);
            MessageProducer producer = session.createProducer(destination);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);

            TextMessage message = session.createTextMessage(messageText);
            producer.send(message);

            System.out.println("Sent message: " + messageText);
        }
    }

    /**
     * Sends a text message with custom properties to the queue or topic
     * 
     * @param messageText The text content of the message
     * @param messageId   Custom message ID
     * @param priority    Message priority (0-9)
     */
    public void sendMessageWithProperties(String messageText, String messageId, int priority) throws JMSException {
        try (Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
            Destination destination = useTopic ? session.createTopic(destinationName) : session.createQueue(destinationName);
            MessageProducer producer = session.createProducer(destination);
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
     * Receives a single message from the queue or topic subscription (blocking with timeout)
     * 
     * @param timeoutMs Timeout in milliseconds
     * @return The received message text, or null if no message available
     */
    public String receiveMessage(long timeoutMs) throws JMSException {
        try (Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
            MessageConsumer consumer;
            if (useTopic) {
                // For Azure Service Bus, consume from subscription using path format
                String subscriptionPath = destinationName + "/Subscriptions/" + subscriptionName;
                Queue subscriptionQueue = session.createQueue(subscriptionPath);
                consumer = session.createConsumer(subscriptionQueue);
            } else {
                Queue queue = session.createQueue(destinationName);
                consumer = session.createConsumer(queue);
            }

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
        MessageConsumer consumer;
        if (useTopic) {
            // For Azure Service Bus, consume from subscription using path format
            String subscriptionPath = destinationName + "/Subscriptions/" + subscriptionName;
            Queue subscriptionQueue = session.createQueue(subscriptionPath);
            consumer = session.createConsumer(subscriptionQueue);
            System.out.println("Async message listener set up for topic: " + destinationName + " (subscription: " + subscriptionName + ")");
        } else {
            Queue queue = session.createQueue(destinationName);
            consumer = session.createConsumer(queue);
            System.out.println("Async message listener set up for queue: " + destinationName);
        }
        consumer.setMessageListener(listener);

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
        String cliTopicName = null;
        String cliSubscriptionName = null;
        
        for (int i = 0; i < args.length; i++) {
            if (("--connection-string".equals(args[i]) || "-c".equals(args[i])) && i + 1 < args.length) {
                cliConnectionString = args[++i];
            } else if (("--queue-name".equals(args[i]) || "-q".equals(args[i])) && i + 1 < args.length) {
                cliQueueName = args[++i];
            } else if (("--topic-name".equals(args[i]) || "-t".equals(args[i])) && i + 1 < args.length) {
                cliTopicName = args[++i];
            } else if (("--subscription-name".equals(args[i]) || "-s".equals(args[i])) && i + 1 < args.length) {
                cliSubscriptionName = args[++i];
            } else if ("--help".equals(args[i]) || "-h".equals(args[i])) {
                System.out.println("Usage: java -jar azure-servicebus-jms-client-test.jar [options]");
                System.out.println("Options:");
                System.out.println("  -c, --connection-string <string>   Service Bus connection string (overrides SERVICEBUS_CONNECTION_STRING env var)");
                System.out.println("  -q, --queue-name <name>            Queue name (overrides SERVICEBUS_QUEUE_NAME env var)");
                System.out.println("  -t, --topic-name <name>            Topic name (overrides SERVICEBUS_TOPIC_NAME env var)");
                System.out.println("  -s, --subscription-name <name>     Subscription name for topic (overrides SERVICEBUS_SUBSCRIPTION_NAME env var)");
                System.out.println("  -h, --help                         Show this help message");
                System.out.println("\nNote: Use either --queue-name OR --topic-name with --subscription-name, not both.");
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

        // Determine topic/queue settings
        String topicName = cliTopicName != null ? cliTopicName : System.getenv("SERVICEBUS_TOPIC_NAME");
        String subscriptionName = cliSubscriptionName != null ? cliSubscriptionName : System.getenv("SERVICEBUS_SUBSCRIPTION_NAME");
        String queueName = cliQueueName != null ? cliQueueName : System.getenv("SERVICEBUS_QUEUE_NAME");
        
        boolean hasTopic = topicName != null && !topicName.isEmpty();
        boolean hasQueue = queueName != null && !queueName.isEmpty();

        // Validate connection string
        if (connectionString == null || connectionString.isEmpty()) {
            System.err.println("Please provide the connection string via CLI argument or SERVICEBUS_CONNECTION_STRING environment variable");
            System.err.println("Usage: java -jar azure-servicebus-jms-client-test.jar -c <connection-string> [-q <queue-name>] [-t <topic-name> -s <subscription-name>]");
            System.err.println("Format: Endpoint=sb://<namespace>.servicebus.windows.net/;SharedAccessKeyName=<keyname>;SharedAccessKey=<key>");
            System.exit(1);
        }

        // Validate topic subscription
        if (hasTopic && (subscriptionName == null || subscriptionName.isEmpty())) {
            System.err.println("Subscription name is required when using topics. Use -s or set SERVICEBUS_SUBSCRIPTION_NAME.");
            System.exit(1);
        }

        // Use default queue if nothing specified
        if (!hasTopic && !hasQueue) {
            queueName = "test-queue";
            hasQueue = true;
        }

        // Log configuration (with secrets redacted)
        System.out.println("=== Configuration ===");
        System.out.println("Connection String (" + connectionStringSource + "): " + redactConnectionString(connectionString));
        if (hasQueue) {
            System.out.println("Queue Name: " + queueName);
        }
        if (hasTopic) {
            System.out.println("Topic Name: " + topicName);
            System.out.println("Subscription Name: " + subscriptionName);
        }
        System.out.println("=====================");

        boolean allTestsPassed = true;

        // Test queue if specified
        if (hasQueue) {
            System.out.println("\n########## QUEUE TEST ##########");
            allTestsPassed &= runTest(connectionString, queueName, false, null);
        }

        // Test topic if specified
        if (hasTopic) {
            System.out.println("\n########## TOPIC TEST ##########");
            allTestsPassed &= runTest(connectionString, topicName, true, subscriptionName);
        }

        // Final result
        System.out.println("\n=== FINAL RESULT ===");
        if (allTestsPassed) {
            System.out.println("ALL TESTS PASSED!");
            System.exit(0);
        } else {
            System.err.println("SOME TESTS FAILED!");
            System.exit(1);
        }
    }

    /**
     * Runs a send/receive test for a queue or topic
     * 
     * @return true if test passed, false otherwise
     */
    private static boolean runTest(String connectionString, String destinationName, boolean useTopic, String subscriptionName) {
        String destType = useTopic ? "Topic" : "Queue";
        System.out.println("Testing " + destType + ": " + destinationName);
        if (useTopic) {
            System.out.println("Subscription: " + subscriptionName);
        }

        ServiceBusJmsClientTest client = useTopic
            ? new ServiceBusJmsClientTest(connectionString, destinationName, true, subscriptionName)
            : new ServiceBusJmsClientTest(connectionString, destinationName);

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
            System.out.println("\n=== " + destType + " Test Results ===");
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
                System.out.println(destType.toUpperCase() + " TEST PASSED: All sent messages were received!");
                return true;
            } else {
                System.err.println(destType.toUpperCase() + " TEST FAILED: Not all messages were received or extra messages found");
                return false;
            }

        } catch (JMSException e) {
            System.err.println("JMS Error: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            client.close();
        }
    }
}
