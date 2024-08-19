import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class Main {

    final static int wordCount = 100_000;
    final static String originalString = "abc";
    final static int stringLength = 10_000;

    static BlockingQueue<String> maxAQueue = new ArrayBlockingQueue<>(100);
    static BlockingQueue<String> maxBQueue = new ArrayBlockingQueue<>(100);
    static BlockingQueue<String> maxCQueue = new ArrayBlockingQueue<>(100);
    static AtomicBoolean continueLoop = new AtomicBoolean(true);

    public static void main(String[] args) {
        AtomicReference<String> maxA = new AtomicReference<>("");
        AtomicReference<String> maxB = new AtomicReference<>("");
        AtomicReference<String> maxC = new AtomicReference<>("");
        AtomicLong countA = new AtomicLong(0);
        AtomicLong countB = new AtomicLong(0);
        AtomicLong countC = new AtomicLong(0);

        Thread fillingThread = new Thread(() -> {
            for (int i = 0; i < wordCount; i++) {
                String text = generateText(originalString, stringLength);
                try {
                    maxAQueue.put(text);
                    maxBQueue.put(text);
                    maxCQueue.put(text);
                } catch (InterruptedException e) {
                    System.out.println("Работа заполняющего потока прервана.");
                    return;
                }
            }
            continueLoop.set(false);
        });
        fillingThread.start();

        Thread threadMaxA = new Thread(() -> {
            maxA.set(stringSearcher('a', maxAQueue));
            countA.set(maxA.get().chars().filter(ch -> ch == 'a').count());
        });
        threadMaxA.start();

        Thread threadMaxB = new Thread(() -> {
            maxB.set(stringSearcher('b', maxBQueue));
            countB.set(maxB.get().chars().filter(ch -> ch == 'b').count());
        });
        threadMaxB.start();

        Thread threadMaxC = new Thread(() -> {
            maxC.set(stringSearcher('c', maxCQueue));
            countC.set(maxC.get().chars().filter(ch -> ch == 'c').count());
        });
        threadMaxC.start();

        try {
            threadMaxA.join();
            threadMaxB.join();
            threadMaxC.join();
        } catch (InterruptedException e) {
            System.out.println("Работа потоков разбирающих очереди прервана.");
        }

        System.out.printf("""
                        Строки с максимальным кол-вом символов:
                        %s символов 'a': %s;
                        %s символов 'b': %s;
                        %s символов 'c': %s
                        """,
                countA, maxA, countB, maxB, countC, maxC);
    }

    public static String generateText(String letters, int length) {
        Random random = new Random();
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < length; i++) {
            text.append(letters.charAt(random.nextInt(letters.length())));
        }
        return text.toString();
    }

    public static String stringSearcher(char searchChar, BlockingQueue<String> maxQueue) {
        AtomicReference<String> str = new AtomicReference<>("");
        AtomicLong count = new AtomicLong(0);
        try {
            str.set(maxQueue.take());
            count.set(str.toString().chars().filter(ch -> ch == searchChar).count());
        } catch (InterruptedException e) {
            System.out.println("Исключение при первом извлечении строки из очереди.");
        }
        while (continueLoop.get()) {
            String estimatedMax = "";
            try {
                estimatedMax = maxQueue.take();
            } catch (InterruptedException e) {
                System.out.println("Исключение при извлечении строки из очереди в цикле.");
            }
            long estimatedMaxCount = estimatedMax.chars().filter(ch -> ch == searchChar).count();
            if (count.get() < estimatedMaxCount) {
                str.set(estimatedMax);
                count.set(estimatedMaxCount);
            }
        }
        return str.get();
    }
}