package com.example.demo.controller;

import com.example.demo.Greeting;
import com.example.demo.job.NicJob;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;

@RestController
public class JobSchedulerController {
    private static final Logger logger = LoggerFactory.getLogger(JobSchedulerController.class);

    @Autowired
    private Scheduler scheduler;

    @PostMapping("/scheduleJob")
    public ResponseEntity<Greeting> scheduleJob(@Valid @RequestBody String request) {
        try {
            JobDetail jobDetail = buildJobDetail(request);
            Trigger trigger = buildJobTrigger(jobDetail, ZonedDateTime.now().plusSeconds(5));
            scheduler.scheduleJob(jobDetail, trigger);

            Greeting greeting = new Greeting("Scheduled Successfully");
            return ResponseEntity.ok(greeting);
        } catch (SchedulerException ex) {
            logger.error("Error scheduling job", ex);
            Greeting greeting = new Greeting("Error scheduling job. Please try later!");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(greeting);
        }
    }

    private JobDetail buildJobDetail(String request) {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("body", request);
        return JobBuilder.newJob(NicJob.class)
                .withIdentity(UUID.randomUUID().toString(), "NIC-jobs")
                .withDescription("NIC Job")
                .usingJobData(jobDataMap)
                .storeDurably()
                .build();
    }

    private Trigger buildJobTrigger(JobDetail jobDetail, ZonedDateTime startAt) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(jobDetail.getKey().getName(), "NIC-triggers")
                .withDescription("Send NIC Trigger")
                .startAt(Date.from(startAt.toInstant()))
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
                .build();
    }
}
