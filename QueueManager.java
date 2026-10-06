import java.util.concurrent.*;
import java.util.*;

public class QueueManager {
    private final Map<String, BlockingQueue<Message>> queues = new ConcurrentHashMap<>();

    public void createQueue(String queueName) {
        queues.putIfAbsent(queueName, new LinkedBlockingQueue<>());
    }

    public void publish(String queueName, Message message) {
        BlockingQueue<Message> queue = queues.get(queueName);

        if (queue == null) {
            throw new IllegalArgumentException("Queue does not exist: " + queueName);
        }

        queue.add(message);
    }

    public Message consume(String queueName) throws InterruptedException {
        BlockingQueue<Message> queue = queues.get(queueName);

        if (queue == null) {
            throw new IllegalArgumentException("Queue does not exist: " + queueName);
        }

        return queue.take();
    }
}
