package utils;

import java.util.HashMap;

public class Timer {

    private static class TimerTask {
        public final String name;

        public long lastBeginTime = -1;

        public long duration = 0;

        public TimerTask(String name) {
            this.name = name;
        }

        public void start() {
            lastBeginTime = System.currentTimeMillis();
        }

        public void pause() {
            if (lastBeginTime == -1) {
                throw new RuntimeException("The task has not been started.");
            }
            duration += System.currentTimeMillis() - this.lastBeginTime;
        }

        @Override
        public String toString() {
            if (lastBeginTime == -1) {
                return String.format("[%-15s] has not been started.", this.name);
            }
            else {
                return String.format("[%-15s]: %6d ms", this.name, this.duration);
            }
        }
    }


    private static final HashMap<String, TimerTask> tasks = new HashMap<>();

    private Timer() {
    }

    public static void startTask(String taskName) {
        if (tasks.containsKey(taskName)) {
            throw new RuntimeException(
                    String.format("Task with name %s already exists.", taskName)
            );
        }

        TimerTask newTask = new TimerTask(taskName);
        tasks.put(taskName, newTask);
        newTask.start();
    }

    public static void stopTask(String taskName) {
        if (!tasks.containsKey(taskName)) {
            throw new RuntimeException("Task " + taskName + "does not exist.");
        }

        tasks.get(taskName).pause();
    }

    public static void stopTaskWithPrint(String taskName) {
        stopTask(taskName);
        System.out.println(tasks.get(taskName));
    }
}
