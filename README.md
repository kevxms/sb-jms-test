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
