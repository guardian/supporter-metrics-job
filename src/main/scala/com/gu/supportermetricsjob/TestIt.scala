package com.gu.supportermetricsjob

import java.io.FileInputStream
import java.util.Properties

import com.gu.supportermetricsjob.SendChurnEmailsJob.Config

object TestIt extends Logging {
  def main(args: Array[String]): Unit = {
    logger.info("running in test mode")
    val config = new Properties()
    val configStream = new FileInputStream("/etc/gu/supporter-metrics-job.properties")
    try {
      config.load(configStream)
    } finally {
      configStream.close
    }
    logger.info(s"config: $config")
    println(SendChurnEmailsJob.run(Config(
      app = "supporter-metrics-job",
      stack = "memb-supporter-metrics-job",
      stage = "dev",
      emailTo = config.getProperty("email.to"),
      emailFrom = config.getProperty("email.from"),
      prestoUrl = config.getProperty("presto.url"),
      emailLine = config.getProperty("email.line"))))

  }
}
