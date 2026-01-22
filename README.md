# Azure Service Bus JMS Client Test

A Java JMS test client for interacting with Azure Service Bus queues. This client sends test messages and verifies that all sent messages are successfully received.

## Prerequisites

- Java 11 or higher
- Maven 3.6 or higher
- An Azure Service Bus namespace with a queue

## Configuration

Set the following environment variables:

```powershell
# PowerShell
$env:SERVICEBUS_CONNECTION_STRING = "Endpoint=sb://<namespace>.servicebus.windows.net/;SharedAccessKeyName=<keyname>;SharedAccessKey=<key>"
$env:SERVICEBUS_QUEUE_NAME = "your-queue-name"
```

```bash
# Bash
export SERVICEBUS_CONNECTION_STRING="Endpoint=sb://<namespace>.servicebus.windows.net/;SharedAccessKeyName=<keyname>;SharedAccessKey=<key>"
export SERVICEBUS_QUEUE_NAME="your-queue-name"
```

## Building

```bash
mvn clean package
```

This creates an executable JAR with all dependencies at `target/azure-servicebus-jms-client-test-1.0-SNAPSHOT.jar`.

## Running

### Test Client (Send, Receive, and Verify)

```bash
java -jar target/azure-servicebus-jms-client-test-1.0-SNAPSHOT.jar
```

Or with CLI arguments (overrides environment variables):

```bash
java -jar target/azure-servicebus-jms-client-test-1.0-SNAPSHOT.jar -c "Endpoint=sb://..." -q "your-queue-name"
```

The test client will:
1. Send 2 test messages ("A" and "B") to the queue
2. Receive messages from the queue
3. Assert that all sent messages were received
4. Exit with code 0 on success, 1 on failure

### Command Line Options

```bash
java -jar target/azure-servicebus-jms-client-test-1.0-SNAPSHOT.jar --help
```

Options:
- `-c, --connection-string <string>` - Service Bus connection string (overrides SERVICEBUS_CONNECTION_STRING env var)
- `-q, --queue-name <name>` - Queue name (overrides SERVICEBUS_QUEUE_NAME env var)
- `-h, --help` - Show help message

### Send a Message (Standalone)

```bash
mvn exec:java -Dexec.mainClass="com.example.jms.MessageSender" -Dexec.args="Your message here"
```

### Receive Messages (Standalone)

```bash
mvn exec:java -Dexec.mainClass="com.example.jms.MessageReceiver"
```

## Dependencies

- `azure-servicebus-jms`: Microsoft's JMS provider for Azure Service Bus
- `javax.jms-api`: JMS 2.0 API
- `slf4j-simple`: Simple logging facade

## Project Structure

```
src/main/java/com/example/jms/
├── ServiceBusJmsClientTest.java  # Test client that verifies send/receive
├── MessageSender.java             # Standalone sender utility
└── MessageReceiver.java           # Standalone receiver utility
```

## Features

- **Send Messages**: Send text messages to a Service Bus queue
- **Receive Messages**: Receive messages with configurable timeout
- **Test Verification**: Assert that all sent messages are received
- **Async Receiver**: Set up asynchronous message listeners
- **Custom Properties**: Add custom properties and priority to messages
- **CLI Arguments**: Configure via command-line or environment variables

## Example Usage

```java
ServiceBusJmsClientTest client = new ServiceBusJmsClientTest(connectionString, queueName);

try {
    client.initialize();
    
    // Send a message
    client.sendMessage("Hello, Azure Service Bus!");
    
    // Receive messages
    String message = client.receiveMessage(5000);
    System.out.println("Received: " + message);
    
} finally {
    client.close();
}
```

## Troubleshooting

### Connection Issues

- Verify your connection string is correct
- Ensure the queue exists in your Service Bus namespace
- Check that your firewall allows outbound connections to Azure

### Authentication Errors

- Verify the Shared Access Key has the correct permissions (Send/Listen)
- Check that the key hasn't expired

### Message Not Received

- Ensure messages are being sent to the correct queue
- Check if another consumer is competing for messages
- Verify the receive timeout is sufficient
