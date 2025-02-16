package org.simplejavamail.email.internal;

import lombok.val;
import org.junit.Test;
import org.simplejavamail.api.email.CalendarMethod;
import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.email.EmailAssert;
import org.simplejavamail.api.email.EmailPopulatingBuilder;
import org.simplejavamail.api.email.config.DkimConfig;
import org.simplejavamail.converter.EmailConverter;
import org.simplejavamail.email.EmailBuilder;
import testutil.ConfigLoaderTestHelper;
import testutil.EmailHelper;

import java.io.File;
import java.util.Date;
import java.util.Random;

import static demo.ResourceFolderHelper.determineResourceFolder;
import static org.assertj.core.api.Assertions.assertThat;

public class EmailStartingBuilderImplTest {

	private static final String RESOURCES_PATH = determineResourceFolder("simple-java-mail") + "/test/resources";
	private static final String RESOURCES_TEST_MESSAGES = RESOURCES_PATH + "/test-messages";

	@SuppressWarnings("deprecation")
	@Test
	public void testCopying() {
		val email1 = EmailHelper.createDummyEmailBuilder("moo", true, false, true, true, true, false, true)
				.signWithDomainKey(DkimConfig.builder()
						.dkimPrivateKeyData("dummykey")
						.dkimSigningDomain("moo.com")
						.dkimSelector("selector")
						.build())
				.buildEmailCompletedWithDefaultsAndOverrides();

		((InternalEmail) email1).setUserProvidedEmail(null);

		val email2 = EmailBuilder
				.ignoringDefaults()
				.ignoringOverrides()
				.copying(email1).buildEmail();

		assertThat(email2).isEqualTo(email1);
	}

	@Test
	public void testCopyingSmimeSignedOutlookMessage() {
		final Email emailParsedFromMsg = EmailConverter.outlookMsgToEmail(new File(RESOURCES_TEST_MESSAGES + "/SMIME (signed and clear text).msg"));
		final EmailPopulatingBuilder copyingEmailBuilder = EmailBuilder.ignoringDefaults().copying(emailParsedFromMsg);
		assertThat(copyingEmailBuilder.getHeaders()).isEmpty(); // when copying S/MIME generated message id should be ignored
		copyingEmailBuilder.withHeaders(emailParsedFromMsg.getHeaders()); // but for the equals check, manually add them
		EmailAssert.assertThat(copyingEmailBuilder.buildEmail()).isEqualTo(emailParsedFromMsg);
	}

	@Test
	public void testCopyingCompleteAndEquals() {
		ConfigLoaderTestHelper.clearConfigProperties();

		Email fullyPopulatedEmail = EmailBuilder
				.startingBlank()
				.to("mr moo to", "mr@moo.com")
				.cc("mr moo cc", "mr@moo.com")
				.bcc("mr moo bcc", "mr@moo.com")
				.withAttachment("mooxt", "attachment content".getBytes(), "text/plain")
				.withEmbeddedImage("mooxt", "attachment content".getBytes(), "text/plain")
				.withBounceTo("bounce@bouncy.com")
				.withDispositionNotificationTo("noti@notify.com")
				.withHeader("header 1", "value 1")
				.withHeader("header 2", new Random())
				.withPlainText("blahblah plain text")
				.withHTMLText("blahblah dynamically embedded image <img src='/test-dynamicembedded-image/excellent.png'>")
				.withCalendarText(CalendarMethod.COUNTER, "Calendar text")
				.withReplyTo("reply@to.com")
				.withSubject("mooject")
				.withReturnReceiptTo("RECEIPT@moo.com")
				.signWithDomainKey(DkimConfig.builder()
						.dkimPrivateKeyData("dkim_key")
						.dkimSigningDomain("dkim_domain")
						.dkimSelector("dkim_selector")
						.build())
				.from("it's a me! <mario@moo.com>")
				.fixingMessageId("id123")
				.fixingSentDate(new Date())
				.notMergingSingleSMIMESignedAttachment()
				.notMergingSingleSMIMESignedAttachment()
				// auto resolution for embedded images
				.withEmbeddedImageBaseDir("moo")
				.withEmbeddedImageBaseUrl("http://moo.moomoo.shmoo")
				.withEmbeddedImageBaseClassPath("moo")
				.withEmbeddedImageAutoResolutionForClassPathResources(true)
				.withEmbeddedImageAutoResolutionForURLs(true)
				.withEmbeddedImageAutoResolutionForFiles(true)
				.allowingEmbeddedImageOutsideBaseClassPath(true)
				.allowingEmbeddedImageOutsideBaseDir(true)
				.allowingEmbeddedImageOutsideBaseUrl(true)
				.embeddedImageAutoResolutionMustBeSuccesful(true)
				.buildEmail();

		Email copiedEmail = EmailBuilder
				.copying(fullyPopulatedEmail)
				.buildEmail();

		EmailAssert.assertThat(copiedEmail).isEqualTo(fullyPopulatedEmail);
		assertThat(copiedEmail).isEqualToComparingFieldByFieldRecursively(fullyPopulatedEmail);
	}
}