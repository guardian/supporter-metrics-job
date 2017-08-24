package com.gu.supportermetricsjob

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.regions.Regions
import com.amazonaws.services.simpleemail.{ AmazonSimpleEmailService, AmazonSimpleEmailServiceAsyncClientBuilder }
import com.amazonaws.services.simpleemail.model._

import scala.util.Try

object Emailer extends Logging {

  def makeSes()(implicit credentials: AWSCredentialsProvider): AmazonSimpleEmailService = AmazonSimpleEmailServiceAsyncClientBuilder.standard()
    .withCredentials(credentials)
    .withRegion(Regions.EU_WEST_1)
    .build()

  case class Email(subject: String, body: String, from: String, to: String)

  def sendEmail(email: Email)(implicit ses: AmazonSimpleEmailService) = {
    logger.info(s"Sending email ${email.subject}")

    val body = new Body(new Content(email.body.replaceAll("<[^>]*>", "")))
    body.setHtml(new Content(email.body))
    Try {
      val request = new SendEmailRequest()
        .withDestination(new Destination().withToAddresses(email.to))
        .withSource(email.from)
        .withMessage(new Message()
          .withSubject(new Content(email.subject))
          .withBody(body))

      ses.sendEmail(request)
    }
  }

}
