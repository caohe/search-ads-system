package com.bihju;

import com.bihju.util.IndexServerTask;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Log4j
@SpringBootApplication
public class IndexServerApp implements CommandLineRunner {
    private IndexServerTask indexServerTask;

    @Autowired
    public IndexServerApp(IndexServerTask indexServerTask) {
        this.indexServerTask = indexServerTask;
    }

    public static void main(String[] args) {
        SpringApplication.run(IndexServerApp.class, args);
    }

    @Override
    public void run(String... strings) throws Exception {
        indexServerTask.start();
    }
}
