package bg.nbu.cscb532.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for asynchronous processing.
 * By default, the application runs on Java 21 Virtual Threads (configured in application.yaml)
 * which handles 99% of I/O bound web traffic (CRUD operations) with massive scalability.
 * 
 * This dedicated thread pool is explicitly reserved for CPU-intensive tasks that would otherwise
 * "pin" a Virtual Thread and monopolize the underlying OS carrier thread.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    // TODO (FEATURE: CPU-Heavy Tasks): Implement End-of-Month PDF Invoice Generation or Bulk CSV Import.
    // Use @Async("cpuHeavyPool") on the service method to offload the heavy computation to this specific pool.
    // The REST Controller should return a 202 Accepted immediately while this pool grinds through the task.
    @Bean(name = "cpuHeavyPool")
    public Executor cpuHeavyTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // Keep core pool size small (e.g., matching CPU cores) to prevent starving the OS
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("CpuHeavy-");
        executor.initialize();
        return executor;
    }
}
