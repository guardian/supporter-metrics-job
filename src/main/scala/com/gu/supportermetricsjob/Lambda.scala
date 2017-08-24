package com.gu.supportermetricsjob

import com.amazonaws.services.lambda.runtime.Context
import com.gu.supportermetricsjob.SendChurnEmailsJob.Config
import org.apache.logging.log4j.{ LogManager, Logger }

trait Logging {
  protected val logger: Logger = LogManager.getLogger(getClass)
}

object Lambda extends Logging {

  class LambdaInput() {
  }

  def env(): Config = Config(
    Option(System.getenv("App")).getOrElse("supporter-metrics-job"),
    Option(System.getenv("Stack")).getOrElse("memb-supporter-metrics-job"),
    Option(System.getenv("Stage")).getOrElse("DEV"),
    Option(System.getenv("EmailTo")).get,
    Option(System.getenv("EmailFrom")).get,
    Option(System.getenv("PrestoUrl")).get,
    Option(System.getenv("EmailLine")).get)

  /*
   * This is your lambda entry point
   */
  def handler(lambdaInput: LambdaInput, context: Context): Unit = {
    val environment = env()
    logger.info(s"Starting $environment")

    logger.info(SendChurnEmailsJob.run(env))
  }

}
