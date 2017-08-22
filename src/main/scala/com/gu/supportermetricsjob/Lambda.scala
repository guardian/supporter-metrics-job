package com.gu.supportermetricsjob

import com.amazonaws.auth._
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.regions.Regions
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.simpleemail.model._
import com.amazonaws.services.simpleemail.{ AmazonSimpleEmailService, AmazonSimpleEmailServiceAsyncClientBuilder }
import org.apache.logging.log4j.{ LogManager, Logger }

import scala.util.{ Failure, Success, Try }

/**
 * This is compatible with aws' lambda JSON to POJO conversion.
 * You can test your lambda by sending it the following payload:
 * {"name": "Bob"}
 */
class LambdaInput() {
  var name: String = _
  def getName(): String = name
  def setName(theName: String): Unit = name = theName
}

case class Env(app: String, stack: String, stage: String, emailTo: String, emailFrom: String)

object Env {
  def apply(): Env = Env(
    Option(System.getenv("App")).getOrElse("DEV"),
    Option(System.getenv("Stack")).getOrElse("DEV"),
    Option(System.getenv("Stage")).getOrElse("DEV"),
    Option(System.getenv("EmailTo")).get,
    Option(System.getenv("EmailFrom")).get)
}

trait Logging {
  protected val logger: Logger = LogManager.getLogger(getClass)
}

object Lambda extends Logging {

  val credentials = new AWSCredentialsProviderChain(
    new ProfileCredentialsProvider("membership"),
    DefaultAWSCredentialsProviderChain.getInstance())

  val ses: AmazonSimpleEmailService = AmazonSimpleEmailServiceAsyncClientBuilder.standard()
    .withCredentials(credentials)
    .withRegion(Regions.EU_WEST_1)
    .build()

  /*
   * This is your lambda entry point
   */
  def handler(lambdaInput: LambdaInput, context: Context): Unit = {
    implicit val env = Env()
    logger.info(s"Starting $env")
    logger.info(process)
  }

  def sendEmail(subject: String)(implicit env: Env) = {
    logger.info(s"Sending email $subject")

    Try {
      val request = new SendEmailRequest()
        .withDestination(new Destination().withToAddresses(env.emailTo))
        .withSource(env.emailFrom)
        .withMessage(new Message()
          .withSubject(new Content(subject))
          .withBody(new Body(new Content("john test email body"))))

      ses.sendEmail(request)
    }
  }

  /*
   * I recommend to put your logic outside of the handler
   */
  def process(implicit env: Env): String = {
    sendEmail("john test email") match {
      case Failure(e) =>
        logger.error("failed to send email", e)
        "failed to send email"
      case Success(result) => s"successfully emailed: ${result}"
    }
  }
}

object TestIt extends Logging {
  def main(args: Array[String]): Unit = {
    logger.info("running in test mode")
    println(Lambda.process(Env(app = "dev", stack = "dev", stage = "dev", emailTo = "john.duffell@guardian.co.uk", emailFrom = "membership.dev@theguardian.com")))
  }
}
