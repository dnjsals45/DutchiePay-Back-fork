package dutchiepay.backend.global.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class TestScheduler {
//
//    @Scheduled(cron = "0 * * * * ?")
//    @SchedulerLock(name = "TestScheduler", lockAtMostFor = "3s")
//    public void test() {
//        LockAssert.assertLocked();
//        log.info("TestScheduler");
//    }
//}
